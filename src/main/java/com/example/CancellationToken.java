package com.example;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight, thread-safe cancellation token. A producer creates the token
 * and calls {@link #cancel()} to signal cancellation; consumers poll
 * {@link #isCancelled()} (or {@link #throwIfCancelled()}) at safe points.
 *
 * <p>This is intentionally simpler than {@code java.util.concurrent.Future#cancel}:
 * a token has no result and no completion state. The intent is to short-circuit
 * long-running work (network reads, page parsing) when a newer request supersedes
 * the current one.</p>
 */
final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Returns true once {@link #cancel()} has been called. */
    boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Marks the token as cancelled. Cancellation is one-way and idempotent.
     * Returns true if this call transitioned the token from uncancelled to
     * cancelled; false if it was already cancelled.
     */
    boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    /**
     * Convenience: throws {@link InterruptedException} when cancelled, so
     * existing call sites that already use {@code InterruptedException} can
     * be guarded with a single line.
     */
    void throwIfCancelled() throws InterruptedException {
        if (cancelled.get()) {
            throw new InterruptedException("Operation cancelled");
        }
    }
}
