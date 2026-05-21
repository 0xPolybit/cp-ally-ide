package com.example;

import java.awt.Color;

final class ExecutionResultFormatter {

    private ExecutionResultFormatter() {
    }

    static String summary(CodeExecutionService.ExecutionReport report) {
        long passed = report.results().stream().filter(CodeExecutionService.TestCaseResult::passed).count();
        long timedOut = report.results().stream().filter(CodeExecutionService.TestCaseResult::timedOut).count();
        long unknown = report.results().stream().filter(CodeExecutionService.TestCaseResult::unknown).count();
        long failed = report.results().size() - passed - timedOut - unknown;
        return passed + " passed, " + failed + " failed, " + timedOut + " timed out, " + unknown + " unknown";
    }

    static String buildResultsHtml(String language, CodeExecutionService.ExecutionReport report, AppThemePalette palette) {
        AppThemePalette theme = palette != null ? palette : AppThemePalette.dark();
        Color frame = theme.frameBackground();
        Color panel = theme.panelBackground();
        Color border = theme.borderColor();
        Color text = theme.textColor();
        Color muted = theme.mutedTextColor();
        Color success = theme.successColor();
        Color error = theme.errorColor();
        Color warning = theme.warningColor();
        Color codeText = theme.textColor();
        Color codeBg = theme.surfaceBackground();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
                .append("body { background:").append(toHex(frame)).append("; color:").append(toHex(text)).append("; font-family:Segoe UI, Arial, sans-serif; margin:0; }")
                .append(".wrap { padding:16px; }")
                .append(".title { font-size:16px; font-weight:700; color:").append(toHex(text)).append("; margin-bottom:8px; }")
                .append(".summary { color:").append(toHex(muted)).append("; margin-bottom:14px; }")
                .append(".case { border:1px solid ").append(toHex(border)).append("; border-radius:8px; background:").append(toHex(panel)).append("; padding:12px; margin-bottom:12px; }")
                .append(".meta { color:").append(toHex(muted)).append("; margin-bottom:8px; }")
                .append(".status-pass { color:").append(toHex(success)).append("; font-weight:700; }")
                .append(".status-fail { color:").append(toHex(error)).append("; font-weight:700; }")
                .append(".status-tle { color:").append(toHex(warning)).append("; font-weight:700; }")
                .append(".status-unknown { color:").append(toHex(warning)).append("; font-weight:700; }")
                .append(".section { color:").append(toHex(text)).append("; margin-top:8px; margin-bottom:4px; font-weight:600; }")
                .append(".note { color:").append(toHex(warning)).append("; margin-top:8px; font-size:12px; font-style:italic; }")
                .append("pre { margin:0; background:").append(toHex(codeBg)).append("; color:").append(toHex(codeText)).append("; border:1px solid ").append(toHex(border)).append("; border-radius:6px; padding:10px; white-space:pre-wrap; }")
                .append("</style></head><body><div class='wrap'>");

        html.append("<div class='title'>Local execution for ").append(escape(language)).append("</div>");
        html.append("<div class='summary'>").append(escape(summary(report))).append("</div>");

        for (CodeExecutionService.TestCaseResult result : report.results()) {
            String statusClass = result.timedOut() ? "status-tle" : (result.unknown() ? "status-unknown" : (result.passed() ? "status-pass" : "status-fail"));
            String statusText = result.timedOut() ? "TIME LIMIT EXCEEDED" : (result.unknown() ? "IDK BRUH" : (result.passed() ? "PASSED" : "FAILED"));

            html.append("<div class='case'>");
            html.append("<div><strong>").append(escape(result.displayName())).append("</strong></div>");
            html.append("<div class='meta'>Status: <span class='").append(statusClass).append("'>").append(statusText)
                    .append("</span> | Time: ").append(result.durationMillis()).append(" ms")
                    .append(" | Memory: ")
                    .append(result.peakMemoryKb() >= 0 ? result.peakMemoryKb() + " KB" : "N/A")
                    .append("</div>");

            html.append("<div class='section'>Input</div>");
            html.append(codeBlock(result.input()));

            html.append("<div class='section'>Output</div>");
            html.append(codeBlock(result.actualOutput()));

            if (!result.passed() || result.timedOut()) {
                html.append("<div class='section'>Expected Output</div>");
                html.append(codeBlock(result.expectedOutput()));
            }

            if (result.stderrOutput() != null && !result.stderrOutput().isBlank()) {
                html.append("<div class='section'>Error Output</div>");
                html.append(codeBlock(result.stderrOutput()));
            }

            if (result.note() != null && !result.note().isBlank()) {
                html.append("<div class='note'>").append(escape(result.note())).append("</div>");
            }
            html.append("</div>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private static String codeBlock(String content) {
        return "<pre><code>" + escape(content == null ? "" : content) + "</code></pre>";
    }

    private static String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
