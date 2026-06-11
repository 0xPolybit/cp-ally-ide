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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern LATEX_COLOR_CMD = Pattern.compile("\\\\(textcolor|color)\\b");
    private static final Pattern LATEX_COLOR_WRAP = Pattern.compile("\\\\(textcolor|color)\\s*\\{\\s*([^}]+)\\s*\\}\\s*(?:\\{([^}]*)\\})?");
    private static final Pattern CSS_COLOR_DECL = Pattern.compile("color\\s*:\\s*([^;]+)", Pattern.CASE_INSENSITIVE);

    LatexImageRenderer(Path appDataDirectory) {
        this.appDataDirectory = appDataDirectory;
    }

    void renderLatexNodes(Element root, AppThemePalette activePalette) {
        for (Element script : root.select("script[type^=math/tex]")) {
            String type = script.attr("type");
            boolean display = type != null && type.contains("mode=display");
            String expression = normalizeLatexExpression(script.data().isBlank() ? script.html() : script.data());
            String src = renderLatexToImageSource(expression, display, activePalette, script);
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
            String src = renderLatexToImageSource(expression, false, activePalette, texSpan);
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

            List<Element> rendered = renderTextWithLatex(text, activePalette, (Element) parentNode);
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

    private List<Element> renderTextWithLatex(String text, AppThemePalette activePalette, Element contextElement) {
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
            String src = renderLatexToImageSource(expression, display, activePalette, contextElement);
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

    private String renderLatexToImageSource(String expression, boolean display, AppThemePalette activePalette, Element contextElement) {
        if (expression == null || expression.isBlank()) {
            return "";
        }
        String normalized = normalizeLatexExpression(expression);
        String baseKey = (display ? "d:" : "i:") + normalized;

        try {
            Path latexCacheDir = appDataDirectory.resolve("cache").resolve("latex");
            Files.createDirectories(latexCacheDir);

            // Detect inline HTML color (style="color:...") from context element, if any
            String explicitCssColor = null;
            if (contextElement != null) {
                explicitCssColor = findInlineColorForElement(contextElement);
            }

            boolean hasLatexColorCmd = containsLatexColorCommand(normalized);

            if (explicitCssColor != null) {
                // Generate a single color-specific image (explicit HTML color)
                String colorKey = baseKey + "|c:" + explicitCssColor;
                if (!latexImageCache.containsKey(colorKey)) {
                    String fileName = Integer.toHexString(colorKey.hashCode()) + ".png";
                    Path file = latexCacheDir.resolve(fileName);
                    if (!Files.exists(file)) {
                        java.awt.Color fg = parseCssColor(explicitCssColor);
                        generateLatexPng(normalized, display, fg, file);
                    }
                    latexImageCache.put(colorKey, file.toUri().toString());
                }
                return latexImageCache.getOrDefault(colorKey, "");
            }

            if (hasLatexColorCmd) {
                // Formula contains its own LaTeX color commands — render mixed segments and produce themed variants
                String mixedLightKey = baseKey + "|mixed|light";
                if (!latexImageCache.containsKey(mixedLightKey)) {
                    String fileName = Integer.toHexString(mixedLightKey.hashCode()) + "-light.png";
                    Path file = latexCacheDir.resolve(fileName);
                    if (!Files.exists(file)) {
                        List<Segment> segments = splitIntoColorSegments(normalized);
                        generateCompositeLatexPng(segments, display, AppThemePalette.light().textColor(), file);
                    }
                    latexImageCache.put(mixedLightKey, file.toUri().toString());
                }

                String mixedDarkKey = baseKey + "|mixed|dark";
                if (!latexImageCache.containsKey(mixedDarkKey)) {
                    String fileName = Integer.toHexString(mixedDarkKey.hashCode()) + "-dark.png";
                    Path file = latexCacheDir.resolve(fileName);
                    if (!Files.exists(file)) {
                        List<Segment> segments = splitIntoColorSegments(normalized);
                        generateCompositeLatexPng(segments, display, AppThemePalette.dark().textColor(), file);
                    }
                    latexImageCache.put(mixedDarkKey, file.toUri().toString());
                }

                boolean wantLightMixed = activePalette != null && activePalette.lightTheme();
                String selectedMixedKey = baseKey + (wantLightMixed ? "|mixed|light" : "|mixed|dark");
                return latexImageCache.getOrDefault(selectedMixedKey, "");
            }

            // No explicit colors — generate light & dark variants based on palette text color
            String lightKey = baseKey + "|light";
            if (!latexImageCache.containsKey(lightKey)) {
                String lightFileName = Integer.toHexString(lightKey.hashCode()) + "-light.png";
                Path lightFile = latexCacheDir.resolve(lightFileName);
                if (!Files.exists(lightFile)) {
                    generateLatexPng(normalized, display, AppThemePalette.light().textColor(), lightFile);
                }
                latexImageCache.put(lightKey, lightFile.toUri().toString());
            }

            String darkKey = baseKey + "|dark";
            if (!latexImageCache.containsKey(darkKey)) {
                String darkFileName = Integer.toHexString(darkKey.hashCode()) + "-dark.png";
                Path darkFile = latexCacheDir.resolve(darkFileName);
                if (!Files.exists(darkFile)) {
                    generateLatexPng(normalized, display, AppThemePalette.dark().textColor(), darkFile);
                }
                latexImageCache.put(darkKey, darkFile.toUri().toString());
            }

            boolean wantLight = activePalette != null && activePalette.lightTheme();
            String selectedKey = baseKey + (wantLight ? "|light" : "|dark");
            return latexImageCache.getOrDefault(selectedKey, "");
        } catch (Exception e) {
            DiagnosticLogger.error("Failed to render LaTeX: " + expression, e);
            return "";
        }
    }

    private void generateLatexPng(String normalized, boolean display, java.awt.Color fg, Path outFile) throws Exception {
        TeXFormula formula = new TeXFormula(normalized);
        float size = display ? 18f : 16f;
        int style = display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT;
        TeXIcon icon = formula.createTeXIcon(style, size);
        if (fg != null) {
            icon.setForeground(fg);
        }

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

    // Segment model for composite rendering
    private static final class Segment {
        final String latex;
        final String colorRaw; // may be null

        Segment(String latex, String colorRaw) {
            this.latex = latex;
            this.colorRaw = colorRaw;
        }
    }

    private static List<Segment> splitIntoColorSegments(String normalized) {
        List<Segment> out = new ArrayList<>();
        Matcher m = LATEX_COLOR_WRAP.matcher(normalized);
        int cursor = 0;
        while (m.find()) {
            if (m.start() > cursor) {
                out.add(new Segment(normalized.substring(cursor, m.start()), null));
            }
            String color = m.group(2);
            String inner = m.group(3);
            if (inner == null) inner = "";
            out.add(new Segment(inner, color));
            cursor = m.end();
        }
        if (cursor < normalized.length()) {
            out.add(new Segment(normalized.substring(cursor), null));
        }
        return out;
    }

    private void generateCompositeLatexPng(List<Segment> segments, boolean display, java.awt.Color plainFg, Path outFile) throws Exception {
        List<BufferedImage> imgs = new ArrayList<>();
        int totalWidth = 0;
        int maxHeight = 0;

        for (Segment s : segments) {
            String tex = s.latex.isBlank() ? "" : s.latex;
            TeXFormula formula = new TeXFormula(tex.isEmpty() ? "\\text{ }" : tex);
            float size = display ? 18f : 16f;
            int style = display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT;
            TeXIcon icon = formula.createTeXIcon(style, size);
            if (s.colorRaw != null) {
                icon.setForeground(parseCssColor(s.colorRaw));
            } else if (plainFg != null) {
                icon.setForeground(plainFg);
            }

            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            int croppedH = Math.max((int) (h * 0.75f), h - 6);
            BufferedImage img = new BufferedImage(w, croppedH, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            icon.paintIcon(null, g2, 0, -2);
            g2.dispose();

            imgs.add(img);
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }

        BufferedImage out = new BufferedImage(Math.max(1, totalWidth), Math.max(1, maxHeight), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        int x = 0;
        for (BufferedImage bi : imgs) {
            g.drawImage(bi, x, 0, null);
            x += bi.getWidth();
        }
        g.dispose();

        ImageIO.write(out, "png", outFile.toFile());
    }

    private static boolean containsLatexColorCommand(String normalized) {
        if (normalized == null) return false;
        Matcher m = LATEX_COLOR_CMD.matcher(normalized);
        return m.find();
    }

    private static String findInlineColorForElement(Element el) {
        Element cur = el;
        while (cur != null) {
            if (cur.hasAttr("style")) {
                String style = cur.attr("style");
                Matcher m = CSS_COLOR_DECL.matcher(style);
                if (m.find()) {
                    String raw = m.group(1).trim();
                    return raw;
                }
            }
            if (cur.hasAttr("color")) {
                String raw = cur.attr("color").trim();
                if (!raw.isEmpty()) return raw;
            }
            cur = cur.parent();
        }
        return null;
    }

    private static java.awt.Color parseCssColor(String raw) {
        if (raw == null) return java.awt.Color.BLACK;
        raw = raw.trim();
        try {
            if (raw.startsWith("#")) {
                return java.awt.Color.decode(raw);
            }
            // rgb(...) or rgba(...)
            if (raw.toLowerCase().startsWith("rgb")) {
                int start = raw.indexOf('(');
                int end = raw.indexOf(')');
                if (start >= 0 && end > start) {
                    String body = raw.substring(start + 1, end);
                    String[] parts = body.split(",");
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return new java.awt.Color(r, g, b);
                }
            }
            // named colors - basic support
            switch (raw.toLowerCase()) {
                case "red": return java.awt.Color.RED;
                case "green": return java.awt.Color.GREEN;
                case "blue": return java.awt.Color.BLUE;
                case "black": return java.awt.Color.BLACK;
                case "white": return java.awt.Color.WHITE;
                case "gray": case "grey": return java.awt.Color.GRAY;
            }
        } catch (Exception ignored) {
        }
        return java.awt.Color.BLACK;
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