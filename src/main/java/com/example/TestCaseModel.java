package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test case data separated from the Swing view. */
final class TestCaseModel {

    enum Kind { SAMPLE, CUSTOM }

    record Entry(
            Kind kind,
            int index,
            String displayName,
            String input,
            String expectedOutput,
            boolean expectedOutputProvided) {
    }

    private final List<Entry> entries = new ArrayList<>();

    List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    void setSamples(List<CodeExecutionService.TestCaseSpec> samples) {
        // Remove existing SAMPLE entries but keep CUSTOM entries intact.
        int write = 0;
        for (int read = 0; read < entries.size(); read++) {
            Entry entry = entries.get(read);
            if (entry.kind() == Kind.CUSTOM) {
                entries.set(write++, entry);
            }
        }
        while (entries.size() > write) {
            entries.remove(entries.size() - 1);
        }
        // SAMPLE entries go first, CUSTOM entries follow.
        for (int i = 0; i < samples.size(); i++) {
            CodeExecutionService.TestCaseSpec spec = samples.get(i);
            Entry sample = new Entry(
                    Kind.SAMPLE,
                    i + 1,
                    "Test Case " + (i + 1),
                    safe(spec.input()),
                    safe(spec.expectedOutput()),
                    spec.expectedOutputProvided());
            entries.add(0, sample);
        }
        renumberCustomEntries();
    }

    Entry addCustom(String input, String expectedOutput, boolean expectedOutputProvided) {
        int nextIndex = nextCustomIndex();
        String name = "Custom Test Case " + nextIndex;
        Entry entry = new Entry(
                Kind.CUSTOM,
                nextIndex,
                name,
                normalize(input),
                normalize(expectedOutput),
                expectedOutputProvided);
        entries.add(entry);
        return entry;
    }

    void removeCustom(int customIndex) {
        int customCount = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).kind() != Kind.CUSTOM) {
                continue;
            }
            if (customCount == customIndex) {
                entries.remove(i);
                renumberCustomEntries();
                return;
            }
            customCount++;
        }
    }

    int customCount() {
        int count = 0;
        for (Entry entry : entries) {
            if (entry.kind() == Kind.CUSTOM) {
                count++;
            }
        }
        return count;
    }

    int sampleCount() {
        int count = 0;
        for (Entry entry : entries) {
            if (entry.kind() == Kind.SAMPLE) {
                count++;
            }
        }
        return count;
    }

    List<CodeExecutionService.TestCaseSpec> toExecutionSpecs() {
        List<CodeExecutionService.TestCaseSpec> specs = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            specs.add(new CodeExecutionService.TestCaseSpec(
                    ensureTrailingNewline(entry.input()),
                    ensureTrailingNewline(entry.expectedOutput()),
                    entry.kind() == Kind.CUSTOM,
                    entry.expectedOutputProvided(),
                    entry.displayName()));
        }
        return specs;
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    void clear() {
        entries.clear();
    }

    private int indexOfFirstCustom() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).kind() == Kind.CUSTOM) {
                return i;
            }
        }
        return -1;
    }

    private int nextCustomIndex() {
        return customCount() + 1;
    }

    private void renumberCustomEntries() {
        int next = 1;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.kind() != Kind.CUSTOM) {
                continue;
            }
            String newName = "Custom Test Case " + next++;
            if (!newName.equals(entry.displayName())) {
                entries.set(i, new Entry(
                        entry.kind(), next - 1, newName, entry.input(), entry.expectedOutput(), entry.expectedOutputProvided()));
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return ensureTrailingNewline(value);
    }

    private static String ensureTrailingNewline(String value) {
        if (value == null || value.isEmpty()) {
            return "\n";
        }
        return value.endsWith("\n") ? value : value + "\n";
    }
}
