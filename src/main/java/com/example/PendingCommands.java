package com.example;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded, coalescing command queue for application-level events that must be
 * delivered to the UI after the main window reports {@link #markReady() ready}.
 *
 * <p>Background producers (e.g. the {@link InstanceServer} IPC listener, the
 * command-line deep-link parser) can call {@link #submit(String)} from any
 * thread. The {@link MainWindow} drains the queue on the EDT once it is
 * fully initialized.</p>
 *
 * <p>Duplicate commands are coalesced so only the most recent is delivered —
 * this is the right behavior for deep links, where a flurry of
 * {@code cpally://problem/...} events should always converge on the last
 * requested problem rather than queue a sequence of fetch attempts.</p>
 *
 * <p>The queue is bounded to prevent unbounded growth if the UI is never
 * ready. Once full, the oldest command is evicted (the new request wins).</p>
 */
final class PendingCommands {

    static final String COMMAND_OPEN_PROBLEM_PREFIX = "cpally://problem/";

    private final int capacity;
    private final Set<String> deepLinkKeys = new LinkedHashSet<>();
    private final Map<String, String> deepLinks = new LinkedHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);
    private final Object lock = new Object();

    PendingCommands(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
    }

    /**
     * Submits a command. Returns {@code true} if the command was accepted.
     * Returns {@code false} if the queue is full AND the command is a duplicate
     * of an already-queued command (in which case the existing entry is kept
     * and no eviction happens).
     */
    boolean submit(String command) {
        String key = keyOf(command);
        if (key == null) {
            return false;
        }
        synchronized (lock) {
            if (deepLinks.containsKey(key)) {
                // Already queued; keep the original ordering.
                return true;
            }
            if (deepLinkKeys.size() >= capacity) {
                // Evict the oldest entry to make room.
                String oldestKey = deepLinkKeys.iterator().next();
                deepLinkKeys.remove(oldestKey);
                deepLinks.remove(oldestKey);
            }
            deepLinkKeys.add(key);
            deepLinks.put(key, command);
            return true;
        }
    }

    /**
     * Marks the UI as ready and returns any commands that had been buffered.
     * Subsequent {@link #submit(String)} calls are no-ops — the caller should
     * invoke them directly on the live UI.
     */
    Map<String, String> markReady() {
        synchronized (lock) {
            state.set(State.READY);
            Map<String, String> drained = new LinkedHashMap<>(deepLinks);
            deepLinks.clear();
            deepLinkKeys.clear();
            return drained;
        }
    }

    /**
     * Discards any pending commands without delivering them. Called when the
     * application is shutting down before reaching the ready state.
     */
    void discard() {
        synchronized (lock) {
            state.set(State.DISCARDED);
            deepLinks.clear();
            deepLinkKeys.clear();
        }
    }

    boolean isReady() {
        return state.get() == State.READY;
    }

    int size() {
        synchronized (lock) {
            return deepLinks.size();
        }
    }

    private static String keyOf(String command) {
        if (command == null) {
            return null;
        }
        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith(COMMAND_OPEN_PROBLEM_PREFIX)) {
            return null;
        }
        String code = trimmed.substring(COMMAND_OPEN_PROBLEM_PREFIX.length()).trim();
        while (code.endsWith("/")) {
            code = code.substring(0, code.length() - 1).trim();
        }
        if (code.isEmpty()) {
            return null;
        }
        return code.toUpperCase();
    }

    private enum State { PENDING, READY, DISCARDED }
}
