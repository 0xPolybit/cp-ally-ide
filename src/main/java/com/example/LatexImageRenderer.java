package com.example;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LatexImageRenderer {

    private static final String LATEX_PROCESSED_ATTR = "data-cpa-latex-processed";
    private static final String LATEX_FALLBACK_COLOR = "#dfe1e5";

    private final Path appDataDirectory;
    private final Map<String, String> latexImageCache = new HashMap<>();

    LatexImageRenderer(Path appDataDirectory) {
        this.appDataDirectory = appDataDirectory;
    }

    void renderLatexNodes(Element root, AppThemePalette activePalette) {
        for (Element script : root.select("script[type^=math/tex]")) {
            String type = script.attr("type");
            boolean display = type != null && type.contains("mode=display");
            String expression = normalizeLatexExpression(script.data().isBlank() ? script.html() : script.data());
            String src = renderLatexToImageSource(expression, display, activePalette);
            if (src.isBlank()) {
                Element fallback = new Element(Tag.valueOf("span"), "");
                fallback.attr("style", "color:" + LATEX_FALLBACK_COLOR + ";");
                fallback.addClass("latex-inline-fallback");
                fallback.attr(LATEX_PROCESSED_ATTR, "1");
                fallback.text(expression);
                script.replaceWith(fallback);
                continue;
            }

            Element img = new Element(Tag.valueOf("img"), "");
            img.attr("src", src);
            img.attr("alt", "");
            if (display) {
                img.attr("style", "display:block; margin:8px 0;");
            } else {
                img.addClass("latex-inline");
            }
            script.replaceWith(img);
        }

        for (Element texSpan : root.select("span.tex-span, div.tex-span")) {
            String expression = normalizeLatexExpression(texSpan.text());
            String src = renderLatexToImageSource(expression, false, activePalette);
            if (src.isBlank()) {
                texSpan.addClass("latex-inline-fallback");
                texSpan.attr(LATEX_PROCESSED_ATTR, "1");
                continue;
            }
            Element img = new Element(Tag.valueOf("img"), "");
            img.attr("src", src);
            img.attr("alt", "");
            img.addClass("latex-inline");
            texSpan.replaceWith(img);
        }

        renderLatexInTextNodes(root, activePalette);
    }

    private void renderLatexInTextNodes(Element root, AppThemePalette activePalette) {
        for (TextNode textNode : root.textNodes()) {
            String text = textNode.getWholeText();
            if (text == null || text.isBlank() || text.indexOf('$') < 0) {
                continue;
            }

            Node parentNode = textNode.parent();
            if (!(parentNode instanceof Element parent)) {
                continue;
            }
            if (parent.hasAttr(LATEX_PROCESSED_ATTR)) {
                continue;
            }
            String parentTag = parent.tagName().toLowerCase();
            if ("pre".equals(parentTag) || "code".equals(parentTag) || "script".equals(parentTag) || "style".equals(parentTag)) {
                continue;
            }

            List<Element> rendered = renderTextWithLatex(text, activePalette);
            if (rendered.isEmpty()) {
                continue;
            }

            for (int i = rendered.size() - 1; i >= 0; i--) {
                textNode.after(rendered.get(i));
            }
            textNode.remove();
        }

        for (Element child : root.children()) {
            String tag = child.tagName().toLowerCase();
            if ("pre".equals(tag) || "code".equals(tag) || "script".equals(tag) || "style".equals(tag)) {
                continue;
            }
            renderLatexInTextNodes(child, activePalette);
        }
    }

    private List<Element> renderTextWithLatex(String text, AppThemePalette activePalette) {
        List<Element> nodes = new ArrayList<>();
        int cursor = 0;

        while (cursor < text.length()) {
            int start = text.indexOf('$', cursor);
            if (start < 0) {
                nodes.add(createProcessedTextSpan(text.substring(cursor)));
                break;
            }

            if (start > cursor) {
                nodes.add(createProcessedTextSpan(text.substring(cursor, start)));
            }

            int delimiterLength = latexDelimiterLengthAt(text, start);
            if (delimiterLength == 0) {
                nodes.add(createProcessedTextSpan("$"));
                cursor = start + 1;
                continue;
            }

            int end = findClosingDelimiter(text, start + delimiterLength, delimiterLength);
            if (end < 0) {
                nodes.add(createProcessedTextSpan(text.substring(start)));
                break;
            }

            String expression = text.substring(start + delimiterLength, end).trim();
            boolean display = delimiterLength >= 2;
            String src = renderLatexToImageSource(expression, display, activePalette);
            if (src.isBlank()) {
                Element fallback = createProcessedTextSpan(text.substring(start, end + delimiterLength));
                fallback.attr("style", "color:" + LATEX_FALLBACK_COLOR + ";");
                fallback.addClass("latex-inline-fallback");
                nodes.add(fallback);
            } else {
                Element img = new Element(Tag.valueOf("img"), "");
                img.attr("src", src);
                img.attr("alt", "");
                if (display) {
                    img.attr("style", "display:block; margin:8px 0;");
                } else {
                    img.addClass("latex-inline");
                }
                nodes.add(img);
            }

            cursor = end + delimiterLength;
        }

        return nodes;
    }

    private Element createProcessedTextSpan(String text) {
        Element span = new Element(Tag.valueOf("span"), "");
        span.attr(LATEX_PROCESSED_ATTR, "1");
        span.text(text);
        return span;
    }

    private int latexDelimiterLengthAt(String text, int index) {
        int len = 0;
        while (index + len < text.length() && text.charAt(index + len) == '$' && len < 3) {
            len++;
        }
        return len;
    }

    private int findClosingDelimiter(String text, int from, int delimiterLength) {
        String delimiter = "$".repeat(delimiterLength);
        return text.indexOf(delimiter, from);
    }

    private String renderLatexToImageSource(String expression, boolean display, AppThemePalette activePalette) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        String normalized = normalizeLatexExpression(expression);
        String baseKey = (display ? "d:" : "i:") + normalized;

        // Ensure both variants exist in cache (light & dark)
        try {
            Path latexCacheDir = appDataDirectory.resolve("cache").resolve("latex");
            Files.createDirectories(latexCacheDir);

            // Light variant
            String lightKey = baseKey + "|light";
            if (!latexImageCache.containsKey(lightKey)) {
                String lightFileName = Integer.toHexString(lightKey.hashCode()) + "-light.png";
                Path lightFile = latexCacheDir.resolve(lightFileName);
                if (!Files.exists(lightFile)) {
                    generateLatexPng(normalized, display, AppThemePalette.light().textColor(), lightFile);
                }
                latexImageCache.put(lightKey, lightFile.toUri().toString());
            }

            // Dark variant
            String darkKey = baseKey + "|dark";
            if (!latexImageCache.containsKey(darkKey)) {
                String darkFileName = Integer.toHexString(darkKey.hashCode()) + "-dark.png";
                Path darkFile = latexCacheDir.resolve(darkFileName);
                if (!Files.exists(darkFile)) {
                    generateLatexPng(normalized, display, AppThemePalette.dark().textColor(), darkFile);
                }
                latexImageCache.put(darkKey, darkFile.toUri().toString());
            }

            // Return the variant that matches the active palette
            boolean wantLight = activePalette != null && activePalette.lightTheme();
            String selectedKey = baseKey + (wantLight ? "|light" : "|dark");
            return latexImageCache.getOrDefault(selectedKey, "");
        } catch (Exception e) {
            return "";
        }
    }

    private void generateLatexPng(String normalized, boolean display, java.awt.Color fg, Path outFile) throws Exception {
        TeXFormula formula = new TeXFormula(normalized);
        float size = display ? 18f : 16f;
        int style = display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT;
        TeXIcon icon = formula.createTeXIcon(style, size);
        icon.setForeground(fg);

        int fullWidth = icon.getIconWidth();
        int fullHeight = icon.getIconHeight();
        int croppedHeight = Math.max((int) (fullHeight * 0.75f), fullHeight - 6);

        BufferedImage image = new BufferedImage(fullWidth, croppedHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        icon.paintIcon(null, g2, 0, -2);
        g2.dispose();

        ImageIO.write(image, "png", outFile.toFile());
    }

    private String normalizeLatexExpression(String raw) {
        if (raw == null) {
            return "";
        }

        String text = raw.trim();
        if (text.startsWith("$$$") && text.endsWith("$$$") && text.length() > 6) {
            return text.substring(3, text.length() - 3).trim();
        }
        if (text.startsWith("$$") && text.endsWith("$$") && text.length() > 4) {
            return text.substring(2, text.length() - 2).trim();
        }
        if (text.startsWith("$") && text.endsWith("$") && text.length() > 2) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }
}