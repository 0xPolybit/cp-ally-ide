package com.example;

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

    static String buildResultsHtml(String language, CodeExecutionService.ExecutionReport report) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
                .append("body { background:#1e1f22; color:#dfe1e5; font-family:Segoe UI, Arial, sans-serif; margin:0; }")
                .append(".wrap { padding:16px; }")
                .append(".title { font-size:16px; font-weight:700; color:#eceff4; margin-bottom:8px; }")
                .append(".summary { color:#a9b0bc; margin-bottom:14px; }")
                .append(".case { border:1px solid #43474c; border-radius:8px; background:#2b2d30; padding:12px; margin-bottom:12px; }")
                .append(".meta { color:#b8bec8; margin-bottom:8px; }")
                .append(".status-pass { color:#61d66e; font-weight:700; }")
                .append(".status-fail { color:#f65656; font-weight:700; }")
                .append(".status-tle { color:#f7d71a; font-weight:700; }")
                .append(".status-unknown { color:#f7d71a; font-weight:700; }")
                .append(".section { color:#cfd4dd; margin-top:8px; margin-bottom:4px; font-weight:600; }")
                .append("pre { margin:0; background:#24262a; color:#d9dde4; border:1px solid #43474c; border-radius:6px; padding:10px; white-space:pre-wrap; }")
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
}
