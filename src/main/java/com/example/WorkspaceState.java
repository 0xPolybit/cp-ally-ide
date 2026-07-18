package com.example;

/**
 * High-level state of the main workspace, used to drive the enablement
 * and label state of every interactive control in the main window.
 *
 * <p>The state is derived from a few booleans on the controller and is
 * applied to the UI in one place ({@code MainWindow.applyWorkspaceState}).
 * This replaces the scattered {@code setEnabled} calls that were spread
 * across problem-change, run, and add-test code paths.</p>
 */
final class WorkspaceState {

    private final boolean problemLoading;
    private final boolean problemLoaded;
    private final boolean executionRunning;
    private final boolean codeforcesUsernameSet;

    WorkspaceState(boolean problemLoading, boolean problemLoaded,
                   boolean executionRunning, boolean codeforcesUsernameSet) {
        this.problemLoading = problemLoading;
        this.problemLoaded = problemLoaded;
        this.executionRunning = executionRunning;
        this.codeforcesUsernameSet = codeforcesUsernameSet;
    }

    /** True while a problem is being fetched/parsed/rendered. */
    boolean problemLoading() { return problemLoading; }

    /** True when a problem is loaded and the editor is editable. */
    boolean problemLoaded() { return problemLoaded; }

    /** True while the code is being compiled/run. */
    boolean executionRunning() { return executionRunning; }

    /** True if the user has set a Codeforces handle. */
    boolean codeforcesUsernameSet() { return codeforcesUsernameSet; }

    /** The 'Fetch from CodeForces' button is enabled whenever no fetch is in flight. */
    boolean fetchEnabled() {
        return !problemLoading;
    }

    /** Refresh / Choose Problem are only meaningful after a problem is loaded. */
    boolean refreshEnabled() {
        return problemLoaded && !problemLoading;
    }

    /** Refresh on the empty-problem placeholder is not meaningful. */
    boolean refreshEnabled(boolean isEmptyProblem) {
        return refreshEnabled() && !isEmptyProblem;
    }

    /** Add Test Case requires a problem to attach the test to. */
    boolean addTestCaseEnabled() {
        return problemLoaded && !problemLoading && !executionRunning;
    }

    /** Run / Stop follows the executionRunning flag itself. */
    boolean runEnabled() {
        return executionRunning || (problemLoaded && !problemLoading);
    }

    /** Show Profile is meaningful only when a handle is set. */
    boolean showProfileEnabled() {
        return codeforcesUsernameSet;
    }
}
