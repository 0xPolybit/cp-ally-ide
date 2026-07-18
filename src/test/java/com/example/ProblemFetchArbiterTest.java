package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemFetchArbiterTest {

    @Test
    void firstBeginReturnsIdOne() {
        ProblemFetchArbiter arbiter = new ProblemFetchArbiter();
        ProblemFetchArbiter.Request req = arbiter.begin();
        assertEquals(1L, req.requestId);
        assertTrue(arbiter.isCurrent(req.requestId));
    }

    @Test
    void subsequentBeginsReturnStrictlyIncreasingIds() {
        ProblemFetchArbiter arbiter = new ProblemFetchArbiter();
        ProblemFetchArbiter.Request a = arbiter.begin();
        ProblemFetchArbiter.Request b = arbiter.begin();
        ProblemFetchArbiter.Request c = arbiter.begin();
        assertTrue(a.requestId < b.requestId);
        assertTrue(b.requestId < c.requestId);
    }

    @Test
    void olderRequestIsNoLongerCurrentAfterNewBegin() {
        ProblemFetchArbiter arbiter = new ProblemFetchArbiter();
        ProblemFetchArbiter.Request first = arbiter.begin();
        ProblemFetchArbiter.Request second = arbiter.begin();
        assertFalse(arbiter.isCurrent(first.requestId),
                "A stale request id must be reported as not-current after a new request begins");
        assertTrue(arbiter.isCurrent(second.requestId));
    }

    @Test
    void beginReturnsDistinctTokens() {
        ProblemFetchArbiter arbiter = new ProblemFetchArbiter();
        ProblemFetchArbiter.Request a = arbiter.begin();
        ProblemFetchArbiter.Request b = arbiter.begin();
        assertNotEquals(a.token, b.token);
        a.token.cancel();
        assertFalse(b.token.isCancelled());
    }
}
