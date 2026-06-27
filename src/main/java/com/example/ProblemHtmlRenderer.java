package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class ProblemHtmlRenderer {

    private static final int INLINE_ICON_SIZE = 16;

    private final Path appDataDirectory;
    private final Map<String, String> iconSourceCache = new HashMap<>();
    private final LatexImageRenderer latexImageRenderer;
    private AppThemePalette appTheme = AppThemePalette.dark();
    private double zoomFactor = 1.0;

    ProblemHtmlRenderer(Path appDataDirectory) {
        this.appDataDirectory = appDataDirectory;
        this.latexImageRenderer = new LatexImageRenderer(appDataDirectory);
    }

    void setZoomFactor(double zoom) {
        if (zoom <= 0) return;
        this.zoomFactor = zoom;
        this.iconSourceCache.clear();
    }

    void setTheme(AppThemePalette theme) {
        this.appTheme = theme != null ? theme : AppThemePalette.dark();
        this.iconSourceCache.clear();
    }

    RenderedProblemView render(ProblemDetails details) {
        Map<String, String> copyPayloads = new HashMap<>();
        String problemHtml = prepareProblemHtml(details.problemHtml(), copyPayloads, details.code());
        String css = buildThemeCss();

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
        String css = buildThemeCss();
        
        String body = "<div class='problem-statement'>" + problemHtmlWithoutTests + "</div>";
        String html = "<html><head>" + css + "</head><body>" + body + "</body></html>";
        return new RenderedProblemView(html, copyPayloads);
    }

    RenderedProblemView renderEmptyProblem() {
        String html = "<div class='header'>"
                + "<div class='title'>Empty Problem</div>"
                + "</div>"
                + "<div class='empty-problem'>"
                + "<p>Use this workspace to solve a problem without fetching from CodeForces.</p>"
                + "</div>";
        String body = "<div class='problem-statement'>" + html + "</div>";
        String rendered = "<html><head>" + buildThemeCss() + "</head><body>" + body + "</body></html>";
        return new RenderedProblemView(rendered, Map.of());
    }

    private String prepareProblemHtml(String rawProblemHtml, Map<String, String> copyPayloads, String problemCode) {
        Document doc = Jsoup.parseBodyFragment(rawProblemHtml);
        Element root = doc.body().children().isEmpty() ? doc.body() : doc.body().child(0);

        latexImageRenderer.renderLatexNodes(root, appTheme);
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
        warningBox.html("<strong>Note:</strong> LaTeX rendering is limited in this view. If mathematical formulas or complex content are not displaying correctly, please visit <a href='" + problemUrl + "' style='color:" + toHex(appTheme.textColor()) + "; text-decoration:underline;'>CodeForces.com</a> to view the problem statement with full support.");
        header.after(warningBox);
    }

    private String buildThemeCss() {
        Color frame = appTheme.frameBackground();
        Color panel = appTheme.panelBackground();
        Color text = appTheme.textColor();
        Color muted = appTheme.mutedTextColor();
        Color border = appTheme.borderColor();
        Color surface = appTheme.surfaceBackground();
        Color warningBg = appTheme.lightTheme() ? new Color(255, 245, 214) : new Color(61, 73, 73);

        // Font scale — all sizes derived from a single bodyPx anchor
        int bodyPx    = Math.max(8,  (int) Math.round(13 * zoomFactor)); // slightly compact body
        int titlePx   = Math.max(10, (int) Math.round(20 * zoomFactor)); // problem title (level 1)
        int h1        = Math.max(10, (int) Math.round(18 * zoomFactor)); // generic h1 (level 2)
        int h2        = Math.max(9,  (int) Math.round(15 * zoomFactor)); // section headings / h2 (level 3)
        int h3        = Math.max(8,  (int) Math.round(13 * zoomFactor)); // sub-section / h3 — same weight as body
        int captionPx = Math.max(7,  (int) Math.round(11 * zoomFactor)); // metric row, info boxes (level 4)
        int prePx     = Math.max(7,  (int) Math.round(12 * zoomFactor)); // code/pre blocks

        return "<style>"
            + "body { background:" + toHex(frame) + "; color:" + toHex(text) + "; font-family:Segoe UI, Arial, sans-serif; margin:12px; font-size:" + bodyPx + "px; }"
            + ".problem-statement { background:" + toHex(panel) + "; border-radius:8px; padding:14px; }"
            // Level 1 — problem title
            + ".header .title { font-size:" + titlePx + "px; font-weight:700; color:" + toHex(text) + "; margin-bottom:10px; letter-spacing:-0.2px; }"
            // Levels 2-4 — generic HTML heading elements
            + ".problem-statement h1 { font-size:" + h1 + "px; font-weight:700; margin:10px 0 8px 0; }"
            + ".problem-statement h2 { font-size:" + h2 + "px; font-weight:700; margin:10px 0 6px 0; }"
            + ".problem-statement h3 { font-size:" + h3 + "px; font-weight:700; margin:8px 0 4px 0; }"
            // Level 3 — Codeforces section titles (Input / Output / Note / Examples)
            + ".section-title { font-size:" + h2 + "px; font-weight:700; margin-top:14px; margin-bottom:5px; color:" + toHex(text) + "; }"
            // Level 4 — metric/info row (time, memory, i/o)
            + ".metrics-row { margin-top:4px; margin-bottom:12px; }"
            + ".metrics-table { border-collapse:collapse; }"
            + ".metric-item { font-size:" + captionPx + "px; color:" + toHex(muted) + "; white-space:nowrap; vertical-align:middle; line-height:1.4; }"
            + ".metric-icon { width:14px; height:14px; vertical-align:middle; image-rendering:auto; }"
            + ".latex-inline { vertical-align:middle; }"
            + ".latex-inline-fallback { vertical-align:middle; }"
            + ".sample-tests .input, .sample-tests .output { margin-top:8px; }"
            + ".empty-problem { margin-top:12px; }"
            + ".io-table { width:100%; border-collapse:collapse; margin-bottom:4px; }"
            + ".io-label-cell { text-align:left; vertical-align:middle; }"
            + ".io-copy-cell { text-align:right; vertical-align:middle; width:20px; }"
            + ".io-label { font-size:" + h2 + "px; font-weight:600; line-height:1.6; }"
            + ".copy-btn { text-decoration:none; border:none; outline:none; }"
            + ".copy-btn img { width:14px; height:14px; vertical-align:middle; opacity:0.85; border:none; image-rendering:auto; }"
            + "pre { background:" + toHex(surface) + "; color:" + toHex(text) + "; border:1px solid " + toHex(border) + "; border-radius:6px; padding:10px; white-space:pre-wrap; font-size:" + prePx + "px; }"
            + "p { color:" + toHex(text) + "; line-height:1.5; margin:6px 0; }"
            + ".tex-font-style-bf { font-weight:bold; }"
            // Level 4 — warning/info box treated as a caption-level element
            + ".latex-warning-box { background:" + toHex(warningBg) + "; border-left:4px solid " + toHex(appTheme.warningColor()) + "; border-radius:4px; padding:10px 12px; margin:12px 0; color:" + toHex(text) + "; font-size:" + captionPx + "px; line-height:1.5; }"
            + "</style>";
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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
        String cacheKey = iconFile + "|" + (appTheme != null && appTheme.lightTheme() ? "light" : "dark");
        if (iconSourceCache.containsKey(cacheKey)) {
            return iconSourceCache.get(cacheKey);
        }

        String themedFileName = UiIconLoader.themedAssetPath(iconFile, appTheme);
        java.net.URL iconUrl = ProblemHtmlRenderer.class.getResource(themedFileName);

        if (iconUrl == null) {
            int dot = iconFile.lastIndexOf('.');
            String base = dot >= 0 ? iconFile.substring(0, dot) : iconFile;
            String ext = dot >= 0 ? iconFile.substring(dot) : "";
            iconUrl = ProblemHtmlRenderer.class.getResource("/assets/" + base + "@2x" + ext);
            if (iconUrl == null) {
                iconUrl = ProblemHtmlRenderer.class.getResource("/assets/" + iconFile);
            }
        }

        if (iconUrl == null) {
            DiagnosticLogger.warn("[ProblemHtmlRenderer] Icon resource not found: " + iconFile);
            return "";
        }

        try {
            BufferedImage source = ImageIO.read(iconUrl);
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                DiagnosticLogger.warn("[ProblemHtmlRenderer] Failed to decode image: " + iconUrl);
                return "";
            }

            BufferedImage target = new BufferedImage(INLINE_ICON_SIZE, INLINE_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = target.createGraphics();
            g2.setComposite(java.awt.AlphaComposite.Clear);
            g2.fillRect(0, 0, INLINE_ICON_SIZE, INLINE_ICON_SIZE);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.drawImage(source, 0, 0, INLINE_ICON_SIZE, INLINE_ICON_SIZE, null);
            g2.dispose();

            Path iconCacheDir = appDataDirectory.resolve("cache").resolve("icons");
            Files.createDirectories(iconCacheDir);
            String sanitized = iconFile.replace('.', '_');
            String themeVariant = (appTheme != null && appTheme.lightTheme()) ? "light" : "dark";
            Path scaledFile = iconCacheDir.resolve(sanitized + "_" + INLINE_ICON_SIZE + "px_" + themeVariant + ".png");
            ImageIO.write(target, "png", scaledFile.toFile());

            String src = scaledFile.toUri().toString();
            iconSourceCache.put(cacheKey, src);
            return src;
        } catch (IOException e) {
            DiagnosticLogger.error("[ProblemHtmlRenderer] Failed to load icon: " + iconFile, e);
            return "";
        }
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
