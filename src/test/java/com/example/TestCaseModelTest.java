package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCaseModelTest {

    @Test
    void samplesReplaceExistingSamplesAndKeepCustoms() {
        TestCaseModel model = new TestCaseModel();
        model.addCustom("custom-in", "custom-out", true);
        model.setSamples(SampleTestCaseCollector.collect(Map.of(
                "sample-input-1", "i1",
                "sample-output-1", "o1")));

        assertEquals(1, model.sampleCount());
        assertEquals(1, model.customCount());
        assertEquals("Test Case 1", model.entries().get(0).displayName());
        assertEquals("Custom Test Case 1", model.entries().get(1).displayName());
    }

    @Test
    void addingCustomTestCaseNormalizesNewlines() {
        TestCaseModel model = new TestCaseModel();
        model.addCustom("hello", "world", true);

        TestCaseModel.Entry entry = model.entries().get(0);
        assertEquals("Custom Test Case 1", entry.displayName());
        assertTrue(entry.input().endsWith("\n"));
        assertTrue(entry.expectedOutput().endsWith("\n"));
    }

    @Test
    void customTestCasesAreRenumberedAfterRemoval() {
        TestCaseModel model = new TestCaseModel();
        model.addCustom("a", "x", true);
        model.addCustom("b", "y", true);
        model.addCustom("c", "z", true);
        model.removeCustom(0);

        List<TestCaseModel.Entry> entries = model.entries();
        assertEquals(2, entries.size());
        assertEquals("Custom Test Case 1", entries.get(0).displayName());
        assertEquals("Custom Test Case 2", entries.get(1).displayName());
    }

    @Test
    void toExecutionSpecsPreservesOrderingAndTrailingNewlines() {
        TestCaseModel model = new TestCaseModel();
        model.addCustom("a", "b", true);

        List<CodeExecutionService.TestCaseSpec> specs = model.toExecutionSpecs();
        assertEquals(1, specs.size());
        assertEquals("a\n", specs.get(0).input());
        assertEquals("b\n", specs.get(0).expectedOutput());
        assertTrue(specs.get(0).custom());
        assertFalse(specs.get(0).expectedOutputProvided()
                ? false : specs.get(0).expectedOutputProvided());
    }
}
