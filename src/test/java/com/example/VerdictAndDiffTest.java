package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerdictAndDiffTest {

    @Test
    void diffReturnsEmptyForEqualStrings() {
        assertEquals("", DiffSummary.firstDifference("a\nb\n", "a\nb\n"));
    }

    @Test
    void diffIdentifiesFirstLine() {
        String out = DiffSummary.firstDifference("a\nb\nc\n", "a\nX\nc\n");
        assertTrue(out.contains("line 2"),
                "Diff should point to the first differing line; was: " + out);
        assertTrue(out.contains("b"));
        assertTrue(out.contains("X"));
    }

    @Test
    void diffNormalizesLineEndings() {
        // CRLF vs LF should not be reported as a difference.
        assertEquals("", DiffSummary.firstDifference("a\r\nb\r\n", "a\nb\n"));
    }

    @Test
    void diffTrimsTrailingWhitespacePerLine() {
        assertEquals("", DiffSummary.firstDifference("a   \nb\n", "a\nb\n"));
    }

    @Test
    void diffReportsExtraActualLine() {
        String out = DiffSummary.firstDifference("a\nb\nc\n", "a\nb\n");
        assertTrue(out.contains("line 3"),
                "Should report the missing line 3; was: " + out);
    }

    @Test
    void diffReportsMissingActualLine() {
        String out = DiffSummary.firstDifference("a\n", "a\nb\n");
        assertTrue(out.contains("line 2"),
                "Should report the extra expected line 2; was: " + out);
    }

    @Test
    void diffHandlesNullsAsEmpty() {
        assertEquals("", DiffSummary.firstDifference(null, null));
        assertTrue(DiffSummary.firstDifference(null, "x").contains("line 1"));
    }

    @Test
    void verdictForPassedTest() {
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", true, false, false, 10L, 1024L,
                        "in", "out", "out", "", "ok", "", 0, false));
        assertEquals(Verdict.PASSED, v);
    }

    @Test
    void verdictForTimedOutTest() {
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", false, true, false, 2_000L, 0L,
                        "in", "out", "", "", "TLE", "", -1, false));
        assertEquals(Verdict.TIME_LIMIT_EXCEEDED, v);
    }

    @Test
    void verdictForOutputTruncatedTest() {
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", false, false, false, 10L, 0L,
                        "in", "out", "partial\u2026", "", "truncated", "", 0, true));
        assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, v);
    }

    @Test
    void verdictForUnknownTest() {
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", false, false, true, 10L, 0L,
                        "in", "", "out", "", "no expected", "", 0, false));
        assertEquals(Verdict.UNKNOWN_NO_EXPECTED_OUTPUT, v);
    }

    @Test
    void verdictForRuntimeError() {
        // exitCode != 0 with a non-matching, non-unknown result is a
        // runtime error rather than wrong answer.
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", false, false, false, 10L, 0L,
                        "in", "expected", "actual", "stack", "RE", "", 1, false));
        assertEquals(Verdict.RUNTIME_ERROR, v);
    }

    @Test
    void verdictForWrongAnswer() {
        Verdict v = ExecutionResultFormatter.verdictOf(
                new CodeExecutionService.TestCaseResult(
                        1, "Test 1", false, false, false, 10L, 0L,
                        "in", "expected", "actual", "", "diff", "", 0, false));
        assertEquals(Verdict.WRONG_ANSWER, v);
    }

    @Test
    void verdictLabelAndCssClass() {
        assertEquals("PASSED", ExecutionResultFormatter.verdictLabel(Verdict.PASSED));
        assertEquals("RUNTIME ERROR", ExecutionResultFormatter.verdictLabel(Verdict.RUNTIME_ERROR));
        assertEquals("status-pass", ExecutionResultFormatter.verdictCssClass(Verdict.PASSED));
        assertEquals("status-fail", ExecutionResultFormatter.verdictCssClass(Verdict.RUNTIME_ERROR));
        assertEquals("status-fail", ExecutionResultFormatter.verdictCssClass(Verdict.WRONG_ANSWER));
        assertEquals("status-tle", ExecutionResultFormatter.verdictCssClass(Verdict.TIME_LIMIT_EXCEEDED));
    }

    @Test
    void summaryGroupsVerdicts() {
        // The summary line groups by verdict label.
        CodeExecutionService.ExecutionReport report = CodeExecutionService.ExecutionReport.success(
                java.util.List.of(
                        pass(1),
                        pass(2),
                        wrong(3),
                        runtime(4),
                        tle(5),
                        truncated(6),
                        unknown(7)),
                "");
        String s = ExecutionResultFormatter.summary(report);
        assertTrue(s.contains("2 passed"), s);
        assertTrue(s.contains("1 wrong answer"), s);
        assertTrue(s.contains("1 runtime error"), s);
        assertTrue(s.contains("1 time limit"), s);
        assertTrue(s.contains("1 output limit"), s);
        assertTrue(s.contains("1 unknown"), s);
        assertFalse(s.contains("IDK BRUH"),
                "Old 'IDK BRUH' wording must be gone");
    }

    private static CodeExecutionService.TestCaseResult pass(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, true, false, false, 1L, 0L,
                "in", "out", "out", "", "", "", 0, false);
    }
    private static CodeExecutionService.TestCaseResult wrong(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, false, false, false, 1L, 0L,
                "in", "exp", "act", "", "", "", 0, false);
    }
    private static CodeExecutionService.TestCaseResult runtime(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, false, false, false, 1L, 0L,
                "in", "exp", "act", "stderr", "", "", 1, false);
    }
    private static CodeExecutionService.TestCaseResult tle(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, false, true, false, 2_000L, 0L,
                "in", "exp", "act", "", "", "", -1, false);
    }
    private static CodeExecutionService.TestCaseResult truncated(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, false, false, false, 1L, 0L,
                "in", "exp", "partial", "", "", "", 0, true);
    }
    private static CodeExecutionService.TestCaseResult unknown(int i) {
        return new CodeExecutionService.TestCaseResult(i, "T" + i, false, false, true, 1L, 0L,
                "in", "", "out", "", "", "", 0, false);
    }
}
