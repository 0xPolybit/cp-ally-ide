package com.example;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestCasesPanelFeaturesTest {

    @Test
    void importsJsonTestsFromText() {
        TestCasesPanel panel = new TestCasesPanel(null, AppThemePalette.dark());
        String json = "[{\"input\":\"1\\n\",\"expectedOutput\":\"2\\n\",\"custom\":true,\"expectedOutputProvided\":true,\"displayName\":\"old\"}]";
        assertEquals(1, panel.importTestsFromText(json, "test"));
        assertEquals(1, panel.getCustomTestCases().size());
        assertEquals("1\n", panel.getTestCase(1).input());
    }

    @Test
    void importsPlainTextTestsFromText() {
        TestCasesPanel panel = new TestCasesPanel(null, AppThemePalette.dark());
        String text = "Input:\n1 2\nExpected:\n3\n\nInput:\n4 5\nExpected:\n9\n";
        assertEquals(2, panel.importTestsFromText(text, "test"));
        assertEquals(2, panel.getCustomTestCases().size());
        assertEquals("1 2\n", panel.getTestCase(1).input());
        assertEquals("9\n", panel.getTestCase(2).expectedOutput());
    }

    @Test
    void invalidImportDoesNotAddTests() {
        TestCasesPanel panel = new TestCasesPanel(null, AppThemePalette.dark());
        assertEquals(0, panel.importTestsFromText("", "test"));
        assertEquals(0, panel.getCustomTestCases().size());
    }

    @Test
    void invalidTestIndexReturnsNull() {
        TestCasesPanel panel = new TestCasesPanel(null, AppThemePalette.dark());
        assertNull(panel.getTestCase(1));
        assertNull(panel.getTestCase(0));
    }

    @Test
    void sampleAndCustomIndicesAreUnified() {
        TestCasesPanel panel = new TestCasesPanel(null, AppThemePalette.dark());
        panel.setSamplePayloads(Map.of("sample-input-1", "a\n", "sample-output-1", "b\n"));
        panel.setCustomTestCases(java.util.List.of(new CodeExecutionService.TestCaseSpec("c\n", "d\n", true, true, "custom")));
        assertNotNull(panel.getTestCase(1));
        assertNotNull(panel.getTestCase(2));
        assertEquals("c\n", panel.getTestCase(2).input());
    }
}
