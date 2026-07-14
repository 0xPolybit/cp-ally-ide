package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiViewStateTest {

    @Test
    void problemStateDistinguishesLoadingLoadedErrorAndUsableEmpty() {
        assertFalse(ProblemViewState.loading("1A").usable());
        assertTrue(ProblemViewState.empty().usable());
        assertTrue(ProblemViewState.loaded("1A", null).usable());
        assertFalse(ProblemViewState.error("1A", "failed").usable());
    }

    @Test
    void runtimeStateMapsSupportMessagesToTypedStates() {
        RuntimeViewState ready = RuntimeViewState.fromSupport(
                "Python 3", new CodeExecutionService.LanguageSupport(true, "Ready"));
        RuntimeViewState missing = RuntimeViewState.fromSupport(
                "C++", new CodeExecutionService.LanguageSupport(false, "Missing local toolchain: g++"));
        RuntimeViewState unsupported = RuntimeViewState.fromSupport(
                "Unknown", new CodeExecutionService.LanguageSupport(false, "No local runner is configured"));

        assertEquals(RuntimeState.READY, ready.state());
        assertTrue(ready.ready());
        assertEquals(RuntimeState.MISSING, missing.state());
        assertEquals(RuntimeState.UNSUPPORTED, unsupported.state());
    }

    @Test
    void executionStateHelpersExposeRunningAndTerminalStates() {
        assertTrue(ExecutionViewState.running("Java 21").running());
        assertFalse(ExecutionViewState.complete("Java 21").running());
        assertEquals(ExecutionState.FAILED, ExecutionViewState.failed("Java 21").state());
    }
}
