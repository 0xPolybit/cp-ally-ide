package com.example;

/** Typed UI state shared by the workspace and action orchestration. */
enum ProblemLoadState {
    EMPTY,
    LOADING,
    LOADED,
    ERROR
}

record ProblemViewState(
        ProblemLoadState state,
        String problemCode,
        ProblemDetails details,
        String errorMessage) {

    static ProblemViewState empty() {
        return new ProblemViewState(ProblemLoadState.EMPTY, null, null, "");
    }

    static ProblemViewState loading(String problemCode) {
        return new ProblemViewState(ProblemLoadState.LOADING, problemCode, null, "");
    }

    static ProblemViewState loaded(String problemCode, ProblemDetails details) {
        return new ProblemViewState(ProblemLoadState.LOADED, problemCode, details, "");
    }

    static ProblemViewState error(String problemCode, String errorMessage) {
        return new ProblemViewState(
                ProblemLoadState.ERROR,
                problemCode,
                null,
                errorMessage == null ? "" : errorMessage);
    }

    boolean usable() {
        return state == ProblemLoadState.LOADED || state == ProblemLoadState.EMPTY;
    }
}

enum RuntimeState {
    CHECKING,
    READY,
    MISSING,
    UNSUPPORTED
}

record RuntimeViewState(RuntimeState state, String language, String message) {

    static RuntimeViewState checking(String language) {
        return new RuntimeViewState(RuntimeState.CHECKING, language, "Checking runtime support...");
    }

    static RuntimeViewState fromSupport(
            String language, CodeExecutionService.LanguageSupport support) {
        if (support == null) {
            return new RuntimeViewState(RuntimeState.UNSUPPORTED, language, "No runtime support information.");
        }
        RuntimeState state;
        if (support.supported()) {
            state = RuntimeState.READY;
        } else if (support.message() != null && support.message().startsWith("Missing local toolchain")) {
            state = RuntimeState.MISSING;
        } else {
            state = RuntimeState.UNSUPPORTED;
        }
        return new RuntimeViewState(state, language, support.message());
    }

    boolean ready() {
        return state == RuntimeState.READY;
    }
}

enum ExecutionState {
    IDLE,
    RUNNING,
    COMPLETE,
    FAILED
}

record ExecutionViewState(ExecutionState state, String language) {

    static ExecutionViewState idle(String language) {
        return new ExecutionViewState(ExecutionState.IDLE, language);
    }

    static ExecutionViewState running(String language) {
        return new ExecutionViewState(ExecutionState.RUNNING, language);
    }

    static ExecutionViewState complete(String language) {
        return new ExecutionViewState(ExecutionState.COMPLETE, language);
    }

    static ExecutionViewState failed(String language) {
        return new ExecutionViewState(ExecutionState.FAILED, language);
    }

    boolean running() {
        return state == ExecutionState.RUNNING;
    }
}

enum SaveState {
    DISABLED,
    CLEAN,
    DIRTY,
    SAVING
}

enum ConnectivityState {
    CHECKING,
    ONLINE,
    DEGRADED,
    OFFLINE
}
