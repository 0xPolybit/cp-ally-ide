package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class ProblemHtmlRenderer {

    private final Path appDataDirectory;
    private final Map<String, String> iconSourceCache = new HashMap<>();

    ProblemHtmlRenderer(Path appDataDirectory) {
        this.appDataDirectory = appDataDirectory;
    }

    RenderedProblemView render(ProblemDetails details) {
        Map<String, String> copyPayloads = new HashMap<>();
        String problemHtml = prepareProblemHtml(details.problemHtml(), copyPayloads);
        String css = """
                <style>
                body { background:#1e1f22; color:#dfe1e5; font-family:Segoe UI, Arial, sans-serif; margin:12px; }
                .problem-statement { background:#2b2d30; border-radius:8px; padding:14px; }
                .header .title { font-size:18px; font-weight:700; color:#eceff4; margin-bottom:10px; }
                .metrics-row { margin-top:4px; margin-bottom:12px; }
                .metrics-table { border-collapse:collapse; }
                .metric-item { color:#b8bec8; white-space:nowrap; vertical-align:middle; line-height:16px; }
                .metric-icon { width:14px; height:14px; vertical-align:middle; }
                .section-title { font-weight:700; margin-top:12px; margin-bottom:6px; color:#e8ebf0; }
                .sample-tests .input, .sample-tests .output { margin-top:8px; }
                .io-table { width:100%; border-collapse:collapse; margin-bottom:4px; }
                .io-label-cell { text-align:left; vertical-align:middle; }
                .io-copy-cell { text-align:right; vertical-align:middle; width:20px; }
                .io-label { line-height:18px; }
                .copy-btn { text-decoration:none; border:none; outline:none; }
                .copy-btn img { width:14px; height:14px; vertical-align:middle; opacity:0.92; border:none; }
                pre { background:#24262a; color:#d9dde4; border:1px solid #43474c; border-radius:6px; padding:10px; white-space:pre-wrap; }
                p { color:#d3d7de; line-height:1.45; }
                .tex-font-style-bf { font-weight:bold; }
                </style>
                """;

        String body = "<div class='problem-statement'>" + problemHtml + "</div>";
        String html = "<html><head>" + css + "</head><body>" + body + "</body></html>";
        return new RenderedProblemView(html, copyPayloads);
    }

    private String prepareProblemHtml(String rawProblemHtml, Map<String, String> copyPayloads) {
        Document doc = Jsoup.parseBodyFragment(rawProblemHtml);
        Element root = doc.body().children().isEmpty() ? doc.body() : doc.body().child(0);

        enhanceHeaderMetrics(root);
        enhanceSampleTestsWithCopy(root, copyPayloads);
        return root.outerHtml();
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

        String iconSrc = loadIconSource(iconFile, 14);
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
        String copyIcon = loadIconSource("copy.png", 14);
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
            copyPayloads.put(key, pre.text());
            decorateIoBlockHeader(input, "Input", key, copyIcon);
        }

        int outputCounter = 1;
        for (Element output : root.select("div.sample-tests div.output")) {
            Element pre = output.selectFirst("pre");
            if (pre == null) {
                continue;
            }
            String key = "sample-output-" + outputCounter++;
            copyPayloads.put(key, pre.text());
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

    private String loadIconSource(String iconFile, int size) {
        String cacheKey = iconFile + "@" + size;
        if (iconSourceCache.containsKey(cacheKey)) {
            return iconSourceCache.get(cacheKey);
        }

        try {
            Path iconPath = Path.of("assets", iconFile);
            if (!Files.exists(iconPath)) {
                return "";
            }

            BufferedImage source = ImageIO.read(iconPath.toFile());
            if (source == null) {
                return "";
            }

            BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = target.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, 0, 0, size, size, null);
            g2.dispose();

            Path iconCacheDir = appDataDirectory.resolve("cache").resolve("icons");
            Files.createDirectories(iconCacheDir);
            String sanitized = iconFile.replace('.', '_');
            Path scaledFile = iconCacheDir.resolve(sanitized + "_" + size + "px.png");
            ImageIO.write(target, "png", scaledFile.toFile());

            String src = scaledFile.toUri().toString();
            iconSourceCache.put(cacheKey, src);
            return src;
        } catch (IOException e) {
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
