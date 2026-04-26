package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ProblemHtmlRenderer {

    private static final String LATEX_PROCESSED_ATTR = "data-cpa-latex-processed";
    private static final String LATEX_FALLBACK_COLOR = "#dfe1e5";
    private static final int INLINE_ICON_SIZE = 16;

    private final Path appDataDirectory;
    private final Map<String, String> iconSourceCache = new HashMap<>();
    private final Map<String, String> latexImageCache = new HashMap<>();

    ProblemHtmlRenderer(Path appDataDirectory) {
        this.appDataDirectory = appDataDirectory;
    }

    RenderedProblemView render(ProblemDetails details) {
        Map<String, String> copyPayloads = new HashMap<>();
        String problemHtml = prepareProblemHtml(details.problemHtml(), copyPayloads, details.code());
        String css = """
                <style>
                body { background:#1e1f22; color:#dfe1e5; font-family:Segoe UI, Arial, sans-serif; margin:12px; }
                .problem-statement { background:#2b2d30; border-radius:8px; padding:14px; }
                .header .title { font-size:18px; font-weight:700; color:#eceff4; margin-bottom:10px; }
                .metrics-row { margin-top:4px; margin-bottom:12px; }
                .metrics-table { border-collapse:collapse; }
                .metric-item { color:#b8bec8; white-space:nowrap; vertical-align:middle; line-height:18px; }
                .metric-icon { width:16px; height:16px; vertical-align:middle; image-rendering:auto; }
                .latex-inline { vertical-align:-0.18em; }
                .latex-inline-fallback { vertical-align:-0.08em; }
                .section-title { font-weight:700; margin-top:12px; margin-bottom:6px; color:#e8ebf0; }
                .sample-tests .input, .sample-tests .output { margin-top:8px; }
                .io-table { width:100%; border-collapse:collapse; margin-bottom:4px; }
                .io-label-cell { text-align:left; vertical-align:middle; }
                .io-copy-cell { text-align:right; vertical-align:middle; width:20px; }
                .io-label { line-height:18px; }
                .copy-btn { text-decoration:none; border:none; outline:none; }
                .copy-btn img { width:16px; height:16px; vertical-align:middle; opacity:0.92; border:none; image-rendering:auto; }
                pre { background:#24262a; color:#d9dde4; border:1px solid #43474c; border-radius:6px; padding:10px; white-space:pre-wrap; }
                p { color:#d3d7de; line-height:1.45; }
                .tex-font-style-bf { font-weight:bold; }
                .latex-warning-box { background:#3d4949; border-left:4px solid #f7d71a; border-radius:4px; padding:12px; margin:12px 0; color:#e8ebf0; font-size:inherit; line-height:1.5; }
                </style>
                """;

        String body = "<div class='problem-statement'>" + problemHtml + "</div>";
        String html = "<html><head>" + css + "</head><body>" + body + "</body></html>";
        return new RenderedProblemView(html, copyPayloads);
    }

    RenderedProblemView renderStatementOnly(ProblemDetails details) {
        Map<String, String> copyPayloads = new HashMap<>();
        String problemHtml = prepareProblemHtml(details.problemHtml(), copyPayloads, details.code());
        
        Document doc = Jsoup.parseBodyFragment(problemHtml);
        Element root = doc.body();
        
        for (Element sampleTests : root.select("div.sample-tests")) {
            sampleTests.remove();
        }
        
        String problemHtmlWithoutTests = root.outerHtml();
        String css = """
                <style>
                body { background:#1e1f22; color:#dfe1e5; font-family:Segoe UI, Arial, sans-serif; margin:12px; }
                .problem-statement { background:#2b2d30; border-radius:8px; padding:14px; }
                .header .title { font-size:18px; font-weight:700; color:#eceff4; margin-bottom:10px; }
                .metrics-row { margin-top:4px; margin-bottom:12px; }
                .metrics-table { border-collapse:collapse; }
                .metric-item { color:#b8bec8; white-space:nowrap; vertical-align:middle; line-height:18px; }
                .metric-icon { width:16px; height:16px; vertical-align:middle; image-rendering:auto; }
                .latex-inline { vertical-align:-0.18em; }
                .latex-inline-fallback { vertical-align:-0.08em; }
                .section-title { font-weight:700; margin-top:12px; margin-bottom:6px; color:#e8ebf0; }
                .io-table { width:100%; border-collapse:collapse; margin-bottom:4px; }
                .io-label-cell { text-align:left; vertical-align:middle; }
                .io-copy-cell { text-align:right; vertical-align:middle; width:20px; }
                .io-label { line-height:18px; }
                .copy-btn { text-decoration:none; border:none; outline:none; }
                .copy-btn img { width:16px; height:16px; vertical-align:middle; opacity:0.92; border:none; image-rendering:auto; }
                pre { background:#24262a; color:#d9dde4; border:1px solid #43474c; border-radius:6px; padding:10px; white-space:pre-wrap; }
                p { color:#d3d7de; line-height:1.45; }
                .tex-font-style-bf { font-weight:bold; }
                .latex-warning-box { background:#3d4949; border-left:4px solid #f7d71a; border-radius:4px; padding:12px; margin:12px 0; color:#e8ebf0; font-size:inherit; line-height:1.5; }
                </style>
                """;
        
        String body = "<div class='problem-statement'>" + problemHtmlWithoutTests + "</div>";
        String html = "<html><head>" + css + "</head><body>" + body + "</body></html>";
        return new RenderedProblemView(html, copyPayloads);
    }

    private String prepareProblemHtml(String rawProblemHtml, Map<String, String> copyPayloads, String problemCode) {
        Document doc = Jsoup.parseBodyFragment(rawProblemHtml);
        Element root = doc.body().children().isEmpty() ? doc.body() : doc.body().child(0);

        renderLatexNodes(root);
        enhanceHeaderMetrics(root);
        insertLatexWarningBox(root, problemCode);
        enhanceSampleTestsWithCopy(root, copyPayloads);
        normalizePreBlocks(root);
        return root.outerHtml();
    }

    private void insertLatexWarningBox(Element root, String problemCode) {
        Element header = root.selectFirst("div.header");
        if (header == null) {
            return;
        }
        String problemUrl = buildProblemUrl(problemCode);
        Element warningBox = new Element(Tag.valueOf("div"), "");
        warningBox.addClass("latex-warning-box");
        warningBox.html("<strong>Note:</strong> LaTeX rendering is limited in this view. If mathematical formulas or complex content are not displaying correctly, please visit <a href='" + problemUrl + "' style='color:#dfe1e5; text-decoration:underline;'>CodeForces.com</a> to view the problem statement with full support.");
        header.after(warningBox);
    }

    private String buildProblemUrl(String problemCode) {
        if (problemCode == null || problemCode.isBlank()) {
            return "https://codeforces.com";
        }

        String trimmed = problemCode.trim();
        int split = 0;
        while (split < trimmed.length() && Character.isDigit(trimmed.charAt(split))) {
            split++;
        }

        if (split == 0 || split >= trimmed.length()) {
            return "https://codeforces.com";
        }

        String contestId = trimmed.substring(0, split);
        String index = trimmed.substring(split).toUpperCase();
        return "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
    }

    private void renderLatexNodes(Element root) {
        for (Element script : root.select("script[type^=math/tex]")) {
            String type = script.attr("type");
            boolean display = type != null && type.contains("mode=display");
            String expression = normalizeLatexExpression(script.data().isBlank() ? script.html() : script.data());
            String src = renderLatexToImageSource(expression, display);
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
            String src = renderLatexToImageSource(expression, false);
            if (src.isBlank()) {
                texSpan.attr("style", appendStyle(texSpan.attr("style"), "color:" + LATEX_FALLBACK_COLOR + ";"));
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

        renderLatexInTextNodes(root);
    }

    private void renderLatexInTextNodes(Element root) {
        for (TextNode textNode : root.textNodes()) {
            String text = textNode.getWholeText();
            if (text == null || text.isBlank()) {
                continue;
            }
            if (!containsLatexDelimiter(text)) {
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

            List<Element> rendered = renderTextWithLatex(text);
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
            renderLatexInTextNodes(child);
        }
    }

    private List<Element> renderTextWithLatex(String text) {
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
            String src = renderLatexToImageSource(expression, display);
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

    private boolean containsLatexDelimiter(String text) {
        return text.indexOf('$') >= 0;
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
        int idx = text.indexOf(delimiter, from);
        return idx;
    }

    private void enhanceHeaderMetrics(Element root) {
        Element header = root.selectFirst("div.header");
        if (header == null) {
            return;
        }

        Element timeLimit = header.selectFirst("div.time-limit");
        Element memoryLimit = header.selectFirst("div.memory-limit");
        Element inputFile = header.selectFirst("div.input-file");
        Element outputFile = header.selectFirst("div.output-file");

        if (timeLimit == null && memoryLimit == null && inputFile == null && outputFile == null) {
            return;
        }

        String timeText = metricValueOnly(timeLimit != null ? timeLimit.text() : "", "time limit per test");
        String memoryText = metricValueOnly(memoryLimit != null ? memoryLimit.text() : "", "memory limit per test");
        String inputText = metricValueOnly(inputFile != null ? inputFile.text() : "", "input", "input file");
        String outputText = metricValueOnly(outputFile != null ? outputFile.text() : "", "output", "output file");

        if (timeLimit != null) {
            timeLimit.remove();
        }
        if (memoryLimit != null) {
            memoryLimit.remove();
        }
        if (inputFile != null) {
            inputFile.remove();
        }
        if (outputFile != null) {
            outputFile.remove();
        }

        Element row = new Element(Tag.valueOf("div"), "");
        row.addClass("metrics-row");

        Element table = new Element(Tag.valueOf("table"), "");
        table.addClass("metrics-table");
        Element tr = new Element(Tag.valueOf("tr"), "");
        table.appendChild(tr);

        addMetricCell(tr, "time.png", timeText);
        addMetricCell(tr, "memory.png", memoryText);
        String ioMetric = combineInputOutputMetric(inputText, outputText);
        addMetricCell(tr, "input.png", ioMetric);

        row.appendChild(table);
        header.appendChild(row);
    }

    private void addMetricCell(Element tr, String iconFile, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (!tr.children().isEmpty()) {
            Element spacer = new Element(Tag.valueOf("td"), "");
            spacer.attr("width", "24");
            spacer.attr("style", "vertical-align:middle;");
            spacer.html("&nbsp;");
            tr.appendChild(spacer);
        }

        String iconSrc = loadIconSource(iconFile);
        Element cell = new Element(Tag.valueOf("td"), "");
        cell.attr("style", "vertical-align:middle;");
        Element item = new Element(Tag.valueOf("span"), "").addClass("metric-item");
        if (!iconSrc.isBlank()) {
            Element icon = new Element(Tag.valueOf("img"), "");
            icon.addClass("metric-icon");
            icon.attr("src", iconSrc);
            icon.attr("alt", "");
            item.appendChild(icon);
            item.append("&nbsp;");
        }
        item.appendText(text);
        cell.appendChild(item);
        tr.appendChild(cell);
    }

    private void enhanceSampleTestsWithCopy(Element root, Map<String, String> copyPayloads) {
        String copyIcon = loadIconSource("copy.png");
        if (copyIcon.isBlank()) {
            return;
        }

        int inputCounter = 1;
        for (Element input : root.select("div.sample-tests div.input")) {
            Element pre = input.selectFirst("pre");
            if (pre == null) {
                continue;
            }
            String key = "sample-input-" + inputCounter++;
            copyPayloads.put(key, extractPreText(pre));
            decorateIoBlockHeader(input, "Input", key, copyIcon);
        }

        int outputCounter = 1;
        for (Element output : root.select("div.sample-tests div.output")) {
            Element pre = output.selectFirst("pre");
            if (pre == null) {
                continue;
            }
            String key = "sample-output-" + outputCounter++;
            copyPayloads.put(key, extractPreText(pre));
            decorateIoBlockHeader(output, "Output", key, copyIcon);
        }
    }

    private void decorateIoBlockHeader(Element ioBlock, String fallbackLabel, String key, String copyIcon) {
        Element existingTitle = ioBlock.selectFirst("div.title");
        String label = existingTitle != null ? existingTitle.text() : fallbackLabel;
        if (existingTitle != null) {
            existingTitle.remove();
        }
        ioBlock.prepend(createCopyHeaderHtml(label, key, copyIcon));
    }

    private void normalizePreBlocks(Element root) {
        for (Element pre : root.select("pre")) {
            String original = extractPreText(pre);
            String wrapped = wrapLongPreLines(original, 84);
            pre.empty();
            pre.append(buildPreHtml(wrapped));
        }
    }

    private String extractPreText(Element pre) {
        Element clone = pre.clone();
        for (Element br : clone.select("br")) {
            br.after("\n");
            br.remove();
        }

        StringBuilder text = new StringBuilder();
        for (Node node : clone.childNodes()) {
            appendNodeText(node, text);
        }
        return text.toString().replace("\u00a0", " ");
    }

    private void appendNodeText(Node node, StringBuilder out) {
        if (node instanceof TextNode textNode) {
            out.append(textNode.getWholeText());
            return;
        }

        if (!(node instanceof Element element)) {
            return;
        }

        if ("br".equalsIgnoreCase(element.tagName())) {
            out.append('\n');
            return;
        }

        for (Node child : element.childNodes()) {
            appendNodeText(child, out);
        }

        String tag = element.tagName().toLowerCase();
        if (isLineBreakElement(tag) && !endsWithNewline(out)) {
            out.append('\n');
        }
    }

    private boolean isLineBreakElement(String tag) {
        return "div".equals(tag)
                || "p".equals(tag)
                || "li".equals(tag)
                || "tr".equals(tag)
                || "td".equals(tag);
    }

    private boolean endsWithNewline(StringBuilder out) {
        return out.length() > 0 && out.charAt(out.length() - 1) == '\n';
    }

    private String wrapLongPreLines(String source, int maxTokenLength) {
        String normalized = source.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String[] chunks = lines[i].split("(?<=\\s)|(?=\\s)");
            for (String chunk : chunks) {
                if (chunk.isBlank() || chunk.length() <= maxTokenLength) {
                    out.append(chunk);
                    continue;
                }
                int idx = 0;
                while (idx < chunk.length()) {
                    int end = Math.min(chunk.length(), idx + maxTokenLength);
                    out.append(chunk, idx, end);
                    idx = end;
                    if (idx < chunk.length()) {
                        out.append('\n');
                    }
                }
            }
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    private String buildPreHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return escaped.replace("\n", "<br>");
    }

    private String createCopyHeaderHtml(String label, String key, String copyIconDataUri) {
        return "<table class='io-table'><tr>"
                + "<td class='io-label-cell'><span class='io-label'>" + label + "</span></td>"
                + "<td class='io-copy-cell'><a class='copy-btn' href='copy:" + key + "'><img src='" + copyIconDataUri + "' alt='' border='0' hspace='0' vspace='0'/></a></td>"
                + "</tr></table>";
    }

    private String combineInputOutputMetric(String inputText, String outputText) {
        if ((inputText == null || inputText.isBlank()) && (outputText == null || outputText.isBlank())) {
            return "";
        }
        if (inputText == null || inputText.isBlank()) {
            return outputText;
        }
        if (outputText == null || outputText.isBlank()) {
            return inputText;
        }
        return inputText + " / " + outputText;
    }

    private String loadIconSource(String iconFile) {
        if (iconSourceCache.containsKey(iconFile)) {
            return iconSourceCache.get(iconFile);
        }

        Path iconPath = resolveInlineIconPath(iconFile);
        if (!Files.exists(iconPath)) {
            return "";
        }

        try {
            BufferedImage source = ImageIO.read(iconPath.toFile());
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                return "";
            }

            if (source.getWidth() == INLINE_ICON_SIZE && source.getHeight() == INLINE_ICON_SIZE) {
                String src = iconPath.toAbsolutePath().toUri().toString();
                iconSourceCache.put(iconFile, src);
                return src;
            }

            BufferedImage target = new BufferedImage(INLINE_ICON_SIZE, INLINE_ICON_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
            java.awt.Graphics2D g2 = target.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(source, 0, 0, INLINE_ICON_SIZE, INLINE_ICON_SIZE, null);
            g2.dispose();

            Path iconCacheDir = appDataDirectory.resolve("cache").resolve("icons");
            Files.createDirectories(iconCacheDir);
            String sanitized = iconFile.replace('.', '_');
            Path scaledFile = iconCacheDir.resolve(sanitized + "_" + INLINE_ICON_SIZE + "px.png");
            ImageIO.write(target, "png", scaledFile.toFile());

            String src = scaledFile.toUri().toString();
            iconSourceCache.put(iconFile, src);
            return src;
        } catch (IOException e) {
            return "";
        }
    }

    private Path resolveInlineIconPath(String iconFile) {
        int dot = iconFile.lastIndexOf('.');
        String base = dot >= 0 ? iconFile.substring(0, dot) : iconFile;
        String ext = dot >= 0 ? iconFile.substring(dot) : "";

        Path hiDpiPath = Path.of("assets", base + "@2x" + ext);
        if (Files.exists(hiDpiPath)) {
            return hiDpiPath;
        }
        return Path.of("assets", iconFile);
    }

    private String renderLatexToImageSource(String expression, boolean display) {
        if (expression == null || expression.isBlank()) {
            return "";
        }

        String normalized = expression.trim();
        String cacheKey = (display ? "d:" : "i:") + normalized;
        if (latexImageCache.containsKey(cacheKey)) {
            return latexImageCache.get(cacheKey);
        }

        try {
            TeXFormula formula = new TeXFormula(normalized);
            float size = display ? 18f : 16f;
            int style = display ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT;
            TeXIcon icon = formula.createTeXIcon(style, size);
            icon.setForeground(new java.awt.Color(223, 225, 229));

            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            icon.paintIcon(null, g2, 0, 0);
            g2.dispose();

            Path latexCacheDir = appDataDirectory.resolve("cache").resolve("latex");
            Files.createDirectories(latexCacheDir);
            String fileName = Integer.toHexString(cacheKey.hashCode()) + ".png";
            Path file = latexCacheDir.resolve(fileName);
            ImageIO.write(image, "png", file.toFile());

            String src = file.toUri().toString();
            latexImageCache.put(cacheKey, src);
            return src;
        } catch (Exception e) {
            return "";
        }
    }

    private String appendStyle(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        if (existing.endsWith(";")) {
            return existing + addition;
        }
        return existing + ";" + addition;
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

    private String metricValueOnly(String text, String... prefixes) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();
        String lowered = cleaned.toLowerCase();

        for (String prefix : prefixes) {
            String lowerPrefix = prefix.toLowerCase();
            if (lowered.startsWith(lowerPrefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                if (cleaned.startsWith(":")) {
                    cleaned = cleaned.substring(1).trim();
                }
                return cleaned;
            }
        }

        int colon = cleaned.indexOf(':');
        if (colon >= 0 && colon + 1 < cleaned.length()) {
            return cleaned.substring(colon + 1).trim();
        }
        return cleaned;
    }
}
