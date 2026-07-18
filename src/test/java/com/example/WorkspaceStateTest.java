package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceStateTest {

    @Test
    void fetchDisabledDuringLoading() {
        WorkspaceState s = new WorkspaceState(true, false, false, false);
        assertFalse(s.fetchEnabled());
        assertFalse(s.refreshEnabled());
    }

    @Test
    void fetchEnabledWhenIdle() {
        WorkspaceState s = new WorkspaceState(false, false, false, false);
        assertTrue(s.fetchEnabled());
    }

    @Test
    void refreshEnabledOnlyAfterProblemLoaded() {
        WorkspaceState sLoading = new WorkspaceState(true, false, false, false);
        assertFalse(sLoading.refreshEnabled());
        WorkspaceState sLoaded = new WorkspaceState(false, true, false, false);
        assertTrue(sLoaded.refreshEnabled());
    }

    @Test
    void refreshDisabledForEmptyProblem() {
        WorkspaceState s = new WorkspaceState(false, true, false, false);
        // The base refreshEnabled() is true for a loaded problem, but
        // MainWindow passes currentProblemIsEmpty=true to suppress
        // refresh on the empty-problem placeholder.
        assertTrue(s.refreshEnabled());
        assertFalse(s.refreshEnabled(true),
                "Refresh must be disabled for the empty-problem placeholder");
    }

    @Test
    void addTestCaseRequiresLoadedProblemAndIdle() {
        WorkspaceState noProblem = new WorkspaceState(false, false, false, false);
        assertFalse(noProblem.addTestCaseEnabled());
        WorkspaceState loaded = new WorkspaceState(false, true, false, false);
        assertTrue(loaded.addTestCaseEnabled());
        WorkspaceState running = new WorkspaceState(false, true, true, false);
        assertFalse(running.addTestCaseEnabled(),
                "Add Test Case must be disabled while a run is in progress");
    }

    @Test
    void runEnabledWhileExecutingSoStopIsAvailable() {
        // The Run button stays enabled while a run is in progress so
        // the user can hit it again to act as Stop (or the button is
        // relabeled by applyWorkspaceState). The model considers the
        // workspace "run-enabled" in both cases.
        WorkspaceState idle = new WorkspaceState(false, true, false, false);
        WorkspaceState running = new WorkspaceState(false, true, true, false);
        assertTrue(idle.runEnabled());
        assertTrue(running.runEnabled());
    }

    @Test
    void showProfileRequiresCodeforcesUsername() {
        WorkspaceState noUser = new WorkspaceState(false, true, false, false);
        assertFalse(noUser.showProfileEnabled());
        WorkspaceState withUser = new WorkspaceState(false, true, false, true);
        assertTrue(withUser.showProfileEnabled());
    }

    @Test
    void addTestCaseBlockedDuringLoading() {
        WorkspaceState loading = new WorkspaceState(true, true, false, false);
        assertFalse(loading.addTestCaseEnabled(),
                "Add Test Case must be blocked while a problem is loading");
    }
}
