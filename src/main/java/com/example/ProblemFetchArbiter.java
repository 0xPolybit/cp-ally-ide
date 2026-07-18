package com.example;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks a monotonically increasing "fetch request id" so asynchronous fetch
 * results can be discarded when a newer request has been started. Encapsulated
 * here so the bookkeeping can be unit-tested without a SwingWorker.
 */
final class ProblemFetchArbiter {

    private final AtomicLong currentRequestId = new AtomicLong(0L);

    /**
     * Starts a new request, returning a token pair: the new request id and
     * a cancellation token. The caller is expected to also cancel any
     * previously returned token.
     */
    Request begin() {
        long id = currentRequestId.incrementAndGet();
        return new Request(id, new CancellationToken());
    }

    /**
     * Returns true if {@code requestId} is still the most recent request.
     * If false, the caller should drop its result without touching UI state.
     */
    boolean isCurrent(long requestId) {
        return currentRequestId.get() == requestId;
    }

    /** Visible for tests. */
    long currentId() {
        return currentRequestId.get();
    }

    /** A fetch request token: the request id and a cancel handle. */
    static final class Request {
        final long requestId;
        final CancellationToken token;

        Request(long requestId, CancellationToken token) {
            this.requestId = requestId;
            this.token = token;
        }
    }
}
