package com.example;

import java.awt.Color;

final class ExecutionResultFormatter {

    private ExecutionResultFormatter() {
    }

    static String summary(CodeExecutionService.ExecutionReport report) {
        long passed = count(report, r -> verdictOf(r) == Verdict.PASSED);
        long wrong = count(report, r -> verdictOf(r) == Verdict.WRONG_ANSWER);
        long runtime = count(report, r -> verdictOf(r) == Verdict.RUNTIME_ERROR);
        long tle = count(report, r -> verdictOf(r) == Verdict.TIME_LIMIT_EXCEEDED);
        long outLimit = count(report, r -> verdictOf(r) == Verdict.OUTPUT_LIMIT_EXCEEDED);
        long unknown = count(report, r -> verdictOf(r) == Verdict.UNKNOWN_NO_EXPECTED_OUTPUT);
        long canceled = count(report, r -> verdictOf(r) == Verdict.CANCELED);
        return passed + " passed, " + wrong + " wrong answer, "
                + runtime + " runtime error, " + tle + " time limit, "
                + outLimit + " output limit, " + unknown + " unknown, "
                + canceled + " canceled";
    }

    private static long count(CodeExecutionService.ExecutionReport report,
                              java.util.function.Predicate<CodeExecutionService.TestCaseResult> pred) {
        return report.results().stream().filter(pred).count();
    }

    /**
     * Map a single {@link CodeExecutionService.TestCaseResult} to the
     * structured {@link Verdict} that the UI uses to label it. The
     * mapping is deterministic so the same result always produces the
     * same label.
     */
    static Verdict verdictOf(CodeExecutionService.TestCaseResult r) {
        if (r.timedOut()) {
            return Verdict.TIME_LIMIT_EXCEEDED;
        }
        if (r.outputTruncated()) {
            return Verdict.OUTPUT_LIMIT_EXCEEDED;
        }
        if (r.passed()) {
            return Verdict.PASSED;
        }
        if (r.unknown()) {
            return Verdict.UNKNOWN_NO_EXPECTED_OUTPUT;
        }
        if (r.exitCode() != 0) {
            return Verdict.RUNTIME_ERROR;
        }
        return Verdict.WRONG_ANSWER;
    }

    static String verdictLabel(Verdict v) {
        return switch (v) {
            case PASSED -> "PASSED";
            case WRONG_ANSWER -> "WRONG ANSWER";
            case RUNTIME_ERROR -> "RUNTIME ERROR";
            case TIME_LIMIT_EXCEEDED -> "TIME LIMIT EXCEEDED";
            case OUTPUT_LIMIT_EXCEEDED -> "OUTPUT LIMIT EXCEEDED";
            case CANCELED -> "CANCELED";
            case UNKNOWN_NO_EXPECTED_OUTPUT -> "NO EXPECTED OUTPUT";
        };
    }

    static String verdictCssClass(Verdict v) {
        return switch (v) {
            case PASSED -> "status-pass";
            case WRONG_ANSWER, RUNTIME_ERROR -> "status-fail";
            case TIME_LIMIT_EXCEEDED, OUTPUT_LIMIT_EXCEEDED, CANCELED -> "status-tle";
            case UNKNOWN_NO_EXPECTED_OUTPUT -> "status-unknown";
        };
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
                .append(".diff { background:").append(toHex(codeBg)).append("; color:").append(toHex(text)).append("; border:1px solid ").append(toHex(border)).append("; border-radius:6px; padding:10px; white-space:pre-wrap; font-family:monospace; }")
                .append("pre { margin:0; background:").append(toHex(codeBg)).append("; color:").append(toHex(codeText)).append("; border:1px solid ").append(toHex(border)).append("; border-radius:6px; padding:10px; white-space:pre-wrap; }")
                .append("</style></head><body><div class='wrap'>");

        html.append("<div class='title'>Local execution for ").append(escape(language)).append("</div>");
        html.append("<div class='summary'>").append(escape(summary(report))).append("</div>");

        for (CodeExecutionService.TestCaseResult result : report.results()) {
            Verdict v = verdictOf(result);
            String statusClass = verdictCssClass(v);
            String statusText = verdictLabel(v);

            html.append("<div class='case'>");
            html.append("<div><strong>").append(escape(result.displayName())).append("</strong></div>");
            html.append("<div class='meta'>Status: <span class='").append(statusClass).append("'>").append(statusText)
                    .append("</span> | Time: ").append(result.durationMillis()).append(" ms")
                    .append(" | Memory: ")
                    .append(result.peakMemoryKb() >= 0 ? formatMemory(result.peakMemoryKb()) : "N/A")
                    .append("</div>");

            // Show input always.
            html.append("<div class='section'>Input</div>");
            html.append(codeBlock(result.input()));

            // Output. Mark truncation in the meta line if it happened.
            html.append("<div class='section'>Output</div>");
            html.append(codeBlock(result.actualOutput()));
            if (result.outputTruncated()) {
                html.append("<div class='note'>Output truncated: the program produced more than the configured output cap. The remaining bytes were discarded.</div>");
            }

            // Expected output: always when there is one. For
            // UNKNOWN_NO_EXPECTED_OUTPUT, we still render the block
            // but label it clearly.
            boolean hasExpected = result.expectedOutput() != null
                    && !result.expectedOutput().isEmpty();
            if (hasExpected) {
                html.append("<div class='section'>Expected Output</div>");
                html.append(codeBlock(result.expectedOutput()));
                if (v == Verdict.WRONG_ANSWER) {
                    html.append("<div class='section'>First Difference</div>");
                    html.append("<div class='diff'>")
                            .append(escape(DiffSummary.firstDifference(result.actualOutput(), result.expectedOutput())))
                            .append("</div>");
                }
            } else if (v == Verdict.UNKNOWN_NO_EXPECTED_OUTPUT) {
                html.append("<div class='section'>Expected Output</div>");
                html.append("<div class='note'>No expected output was provided for this test.</div>");
            }

            if (result.stderrOutput() != null && !result.stderrOutput().isBlank()) {
                html.append("<div class='section'>Error Output</div>");
                html.append(codeBlock(result.stderrOutput()));
            }

            if (result.details() != null && !result.details().isBlank()) {
                html.append("<div class='note'>").append(escape(result.details())).append("</div>");
            }
            if (result.note() != null && !result.note().isBlank()) {
                html.append("<div class='note'>").append(escape(result.note())).append("</div>");
            }
            html.append("</div>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Returns a plain-text representation of the report. Used by the
     * "Copy report" and "Save report" actions on the results dialog.
     */
    static String buildResultsText(String language, CodeExecutionService.ExecutionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Local execution for ").append(language).append('\n');
        sb.append(summary(report)).append("\n\n");
        for (CodeExecutionService.TestCaseResult r : report.results()) {
            Verdict v = verdictOf(r);
            sb.append(r.displayName()).append("  [").append(verdictLabel(v)).append("]  ");
            sb.append("time=").append(r.durationMillis()).append("ms");
            if (r.peakMemoryKb() >= 0) {
                sb.append(" mem=").append(formatMemory(r.peakMemoryKb()));
            }
            sb.append('\n');
            sb.append("  Input:\n").append(indentBlock(r.input())).append('\n');
            sb.append("  Output:\n").append(indentBlock(r.actualOutput())).append('\n');
            if (r.expectedOutput() != null && !r.expectedOutput().isEmpty()) {
                sb.append("  Expected:\n").append(indentBlock(r.expectedOutput())).append('\n');
            }
            if (r.stderrOutput() != null && !r.stderrOutput().isBlank()) {
                sb.append("  Stderr:\n").append(indentBlock(r.stderrOutput())).append('\n');
            }
            if (r.details() != null && !r.details().isBlank()) {
                sb.append("  Details: ").append(r.details()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String indentBlock(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            sb.append("    ").append(line).append('\n');
        }
        return sb.toString();
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

    private static String formatMemory(long kb) {
        if (kb >= 1024) {
            return String.format("%.1f MB", kb / 1024.0);
        }
        return kb + " KB";
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
