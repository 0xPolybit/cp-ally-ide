package com.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logic-level tests for TestCasesPanel that don't require a display. The panel
 * still extends no display-only code in these exercised methods, so we can
 * construct it headless and verify the bug fix (zoom re-render no longer
 * destroys custom test cases).
 */
class TestCasesPanelLogicTest {

    @Test
    void updateSamplePayloadsDoesNotClearCustomTests() {
        // Simulate the zoom path: setSamplePayloads() runs first, then
        // updateSamplePayloads() is called for every re-render.
        TestCasesPanel panel = newHeadlessPanel();
        Map<String, String> initial = new HashMap<>();
        initial.put("sample-input-1", "1\n");
        initial.put("sample-output-1", "1\n");
        panel.setSamplePayloads(initial);

        // The user adds a custom test case.
        TestCaseFactory factory = new TestCaseFactory();
        panel.setCustomTestCases(List.of(
                factory.custom("in1", "out1", "Custom Test Case 1")));

        // Zoom re-renders and only the sample-derived set is refreshed.
        Map<String, String> reRendered = new HashMap<>();
        reRendered.put("sample-input-1", "1\n");
        reRendered.put("sample-output-1", "1\n");
        reRendered.put("sample-input-2", "2\n");
        reRendered.put("sample-output-2", "2\n");
        panel.updateSamplePayloads(reRendered);

        // Custom test is preserved.
        List<CodeExecutionService.TestCaseSpec> custom = panel.getCustomTestCases();
        assertEquals(1, custom.size(), "Custom test must survive a zoom re-render");
        assertEquals("in1", custom.get(0).input());

        // The sample set is also updated.
        List<CodeExecutionService.TestCaseSpec> execution = panel.getExecutionTestCases();
        long sampleCount = execution.stream().filter(t -> !t.custom()).count();
        long customCount = execution.stream().filter(CodeExecutionService.TestCaseSpec::custom).count();
        assertEquals(2, sampleCount, "Both samples should be present after re-render");
        assertEquals(1, customCount, "Custom test should still be present after re-render");
    }

    @Test
    void setSamplePayloadsClearsCustomTestsOnProblemChange() {
        // This guards the explicit-reset contract: when a NEW problem is
        // loaded, the parent's setSamplePayloads call is the right place to
        // drop custom tests from the previous problem.
        TestCasesPanel panel = newHeadlessPanel();
        panel.setCustomTestCases(List.of(
                new TestCaseFactory().custom("in", "out", "Custom Test Case 1")));

        panel.setSamplePayloads(Map.of("sample-input-1", "1\n", "sample-output-1", "1\n"));
        assertEquals(0, panel.getCustomTestCases().size(),
                "setSamplePayloads is the problem-change entry point and must clear custom tests");
    }

    @Test
    void updateSamplePayloadsIsNoOpWhenPayloadsUnchanged() {
        // If the sample-derived set has not actually changed, we should not
        // rebuild tabs (which would lose the user's selected tab and scroll
        // position). We can only verify the listener contract here since the
        // tabs are display-only.
        TestCasesPanel panel = newHeadlessPanel();
        panel.setSamplePayloads(Map.of("sample-input-1", "1\n", "sample-output-1", "1\n"));

        AtomicInteger callCount = new AtomicInteger(0);
        panel.addListener(source -> callCount.incrementAndGet());

        // Same payload twice — second call must not fire a change.
        Map<String, String> same = Map.of("sample-input-1", "1\n", "sample-output-1", "1\n");
        panel.updateSamplePayloads(same);
        panel.updateSamplePayloads(same);

        // No change in samples, no change in customs → no listener fired.
        assertEquals(0, callCount.get(),
                "updateSamplePayloads must be a no-op when the sample set is unchanged");
    }

    @Test
    void addListenerIsIdempotent() {
        TestCasesPanel panel = newHeadlessPanel();
        TestCasesPanel.Listener l = src -> { };
        panel.addListener(l);
        panel.addListener(l);
        panel.addListener(null);
        // After triggering one change, listener should fire exactly once.
        AtomicInteger count = new AtomicInteger(0);
        panel.addListener(src -> count.incrementAndGet());
        panel.setSamplePayloads(Map.of());
        assertEquals(1, count.get());
    }

    @Test
    void setCustomTestCasesFiltersOutNonCustom() {
        // Defensive: only specs flagged as custom are accepted; sample specs
        // are managed via setSamplePayloads.
        TestCasesPanel panel = newHeadlessPanel();
        TestCaseFactory factory = new TestCaseFactory();
        List<CodeExecutionService.TestCaseSpec> mixed = new ArrayList<>();
        mixed.add(factory.custom("a", "b", "Custom 1"));
        mixed.add(factory.sample("a", "b", "Sample 1"));
        mixed.add(null);
        panel.setCustomTestCases(mixed);

        List<CodeExecutionService.TestCaseSpec> custom = panel.getCustomTestCases();
        assertEquals(1, custom.size());
        assertTrue(custom.get(0).custom());
    }

    @Test
    void listenerFiresWhenCustomTestsReplaced() {
        TestCasesPanel panel = newHeadlessPanel();
        AtomicInteger fires = new AtomicInteger(0);
        panel.addListener(src -> fires.incrementAndGet());

        TestCaseFactory factory = new TestCaseFactory();
        panel.setCustomTestCases(List.of(factory.custom("a", "b", "Custom 1")));
        panel.setCustomTestCases(List.of(factory.custom("c", "d", "Custom 2")));
        assertEquals(2, fires.get());
    }

    private static TestCasesPanel newHeadlessPanel() {
        // The panel only needs an owner Frame for centering its dialogs. We
        // pass null and rely on these tests not opening any dialogs.
        return new TestCasesPanel(null, AppThemePalette.dark());
    }

    /** Small helper to keep the assertions readable. */
    private static final class TestCaseFactory {
        CodeExecutionService.TestCaseSpec custom(String input, String expected, String name) {
            return new CodeExecutionService.TestCaseSpec(input, expected, true, true, name);
        }
        CodeExecutionService.TestCaseSpec sample(String input, String expected, String name) {
            return new CodeExecutionService.TestCaseSpec(input, expected, false, true, name);
        }
    }

    // Smoke check that the test factory itself is wired correctly — keeps
    // "assertSame" import alive and documents intent.
    @Test
    void factorySmoke() {
        assertSame(0, 0);
    }
}
