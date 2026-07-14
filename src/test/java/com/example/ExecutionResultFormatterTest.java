package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionResultFormatterTest {

    @Test
    void summarySeparatesPassedFailedTimeoutAndUnknownCases() {
        var report = CodeExecutionService.ExecutionReport.success(List.of(
                result(1, true, false, false),
                result(2, false, false, false),
                result(3, false, true, false),
                result(4, false, false, true)), "");

        assertEquals("1 passed, 1 failed, 1 timed out, 1 unknown",
                ExecutionResultFormatter.summary(report));
    }

    @Test
    void resultsHtmlEscapesUserControlledText() {
        var report = CodeExecutionService.ExecutionReport.success(List.of(
                new CodeExecutionService.TestCaseResult(
                        1,
                        "<custom>",
                        false,
                        false,
                        false,
                        12,
                        1024,
                        "<input>",
                        "<expected>",
                        "<actual>",
                        "",
                        "details",
                        "")), "");

        String html = ExecutionResultFormatter.buildResultsHtml(
                "Language <X>", report, AppThemePalette.dark());

        assertTrue(html.contains("Language &lt;X&gt;"));
        assertTrue(html.contains("&lt;custom&gt;"));
        assertTrue(html.contains("&lt;input&gt;"));
        assertTrue(html.contains("&lt;actual&gt;"));
        assertFalse(html.contains("<custom>"));
    }

    private static CodeExecutionService.TestCaseResult result(
            int index, boolean passed, boolean timedOut, boolean unknown) {
        return new CodeExecutionService.TestCaseResult(
                index,
                "Test Case " + index,
                passed,
                timedOut,
                unknown,
                10,
                512,
                "input",
                "expected",
                "actual",
                "",
                "details",
                "");
    }
}
