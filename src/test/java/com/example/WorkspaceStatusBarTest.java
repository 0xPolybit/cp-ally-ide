package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkspaceStatusBarTest {

    @Test
    void workspaceBarCanRenderProblemAndConnectivityStates() {
        ActionRegistry actions = registeredActions();
        WorkspaceBar bar = new WorkspaceBar(actions, AppThemePalette.dark());

        bar.setProblemState(ProblemViewState.loading("2208A"));
        bar.setConnectivity(ConnectivityState.ONLINE, "Codeforces online");

        assertNotNull(bar.getAccessibleContext().getAccessibleName());
        assertNotNull(bar.getComponent(0));
    }

    @Test
    void applicationStatusBarAcceptsAllTrackedStates() {
        ApplicationStatusBar bar = new ApplicationStatusBar(AppThemePalette.light());

        bar.setConnectivity(ConnectivityState.DEGRADED, "Degraded");
        bar.setSaveState(SaveState.DIRTY);
        bar.setExecutionState(ExecutionState.RUNNING);
        bar.setZoomText("E 100% | P 125%");

        assertNotNull(bar.getAccessibleContext().getAccessibleName());
        assertNotNull(bar.getComponent(0));
    }

    private static ActionRegistry registeredActions() {
        ActionRegistry actions = new ActionRegistry();
        for (ActionRegistry.Id id : new ActionRegistry.Id[]{
                ActionRegistry.Id.CHOOSE_PROBLEM,
                ActionRegistry.Id.OPEN_EMPTY_PROBLEM,
                ActionRegistry.Id.REFRESH_PROBLEM,
                ActionRegistry.Id.PREFERENCES}) {
            actions.register(id, id.name(), null, () -> {});
        }
        return actions;
    }
}
