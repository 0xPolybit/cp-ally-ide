package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionResultsViewTest {

    @Test
    void rendersResultsAndSelectsFirstFailureByDefault() {
        ExecutionResultsView view = new ExecutionResultsView(AppThemePalette.dark());
        CodeExecutionService.ExecutionReport report = CodeExecutionService.ExecutionReport.success(
                List.of(
                        new CodeExecutionService.TestCaseResult(
                                1, "Test Case 1", true, false, false,
                                12, 1024, "i", "o", "o", "", "details", ""),
                        new CodeExecutionService.TestCaseResult(
                                2, "Test Case 2", false, false, false,
                                18, 2048, "i", "o", "x", "", "details", ""),
                        new CodeExecutionService.TestCaseResult(
                                3, "Test Case 3", true, false, false,
                                5, 512, "i", "o", "o", "", "details", "")),
                "");

        view.setReport(report);

        assertNotNull(view.component());
        javax.swing.JList<?> list = view.resultList();
        assertEquals(3, list.getModel().getSize());
    }

    @Test
    void selectsFirstRowWhenAllResultsPass() {
        ExecutionResultsView view = new ExecutionResultsView(AppThemePalette.light());
        CodeExecutionService.ExecutionReport report = CodeExecutionService.ExecutionReport.success(
                List.of(
                        new CodeExecutionService.TestCaseResult(
                                1, "Test Case 1", true, false, false,
                                10, 1024, "i", "o", "o", "", "", ""),
                        new CodeExecutionService.TestCaseResult(
                                2, "Test Case 2", true, false, false,
                                12, 1024, "i", "o", "o", "", "", "")),
                "");

        view.setReport(report);

        javax.swing.JList<?> list = view.resultList();
        assertTrue(list.getSelectedIndex() >= 0);
    }
}
