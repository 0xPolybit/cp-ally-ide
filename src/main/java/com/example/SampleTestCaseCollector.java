package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SampleTestCaseCollector {

    private static final Pattern INPUT_KEY_PATTERN = Pattern.compile("^sample-input-(\\d+)$");
    private static final Pattern OUTPUT_KEY_PATTERN = Pattern.compile("^sample-output-(\\d+)$");

    private SampleTestCaseCollector() {
    }

    static List<CodeExecutionService.TestCaseSpec> collect(Map<String, String> copyPayloads) {
        Map<Integer, String> inputs = new TreeMap<>();
        Map<Integer, String> outputs = new TreeMap<>();

        for (Map.Entry<String, String> entry : copyPayloads.entrySet()) {
            Matcher inputMatcher = INPUT_KEY_PATTERN.matcher(entry.getKey());
            if (inputMatcher.matches()) {
                inputs.put(Integer.parseInt(inputMatcher.group(1)), entry.getValue());
                continue;
            }

            Matcher outputMatcher = OUTPUT_KEY_PATTERN.matcher(entry.getKey());
            if (outputMatcher.matches()) {
                outputs.put(Integer.parseInt(outputMatcher.group(1)), entry.getValue());
            }
        }

        List<CodeExecutionService.TestCaseSpec> testCases = new ArrayList<>();
        for (Map.Entry<Integer, String> inputEntry : inputs.entrySet()) {
            String expectedOutput = outputs.get(inputEntry.getKey());
            if (expectedOutput != null) {
                testCases.add(new CodeExecutionService.TestCaseSpec(inputEntry.getValue(), expectedOutput, false, true));
            }
        }

        return testCases;
    }
}
