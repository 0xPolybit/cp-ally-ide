package com.example;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleTestCaseCollectorTest {

    @Test
    void collectsPairedSamplesInNumericOrder() {
        Map<String, String> payloads = Map.of(
                "sample-input-10", "input ten",
                "sample-output-10", "output ten",
                "sample-input-2", "input two",
                "sample-output-2", "output two");

        var cases = SampleTestCaseCollector.collect(payloads);

        assertEquals(2, cases.size());
        assertEquals("input two", cases.get(0).input());
        assertEquals("output two", cases.get(0).expectedOutput());
        assertEquals("Test Case 2", cases.get(0).displayName());
        assertEquals("Test Case 10", cases.get(1).displayName());
        assertFalse(cases.get(0).custom());
        assertTrue(cases.get(0).expectedOutputProvided());
    }

    @Test
    void ignoresInputsWithoutMatchingOutputs() {
        Map<String, String> payloads = Map.of(
                "sample-input-1", "input one",
                "sample-output-2", "output two",
                "unrelated", "ignored");

        assertTrue(SampleTestCaseCollector.collect(payloads).isEmpty());
    }
}
