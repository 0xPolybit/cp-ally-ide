package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancellationTokenTest {

    @Test
    void startsNotCancelled() {
        CancellationToken token = new CancellationToken();
        assertFalse(token.isCancelled());
    }

    @Test
    void cancelTransitionsOnceAndIsIdempotent() {
        CancellationToken token = new CancellationToken();
        assertTrue(token.cancel(), "first cancel should return true");
        assertFalse(token.cancel(), "second cancel should return false");
        assertTrue(token.isCancelled());
    }

    @Test
    void throwIfCancelledThrowsWhenCancelled() {
        CancellationToken token = new CancellationToken();
        token.cancel();
        assertThrows(InterruptedException.class, token::throwIfCancelled);
    }

    @Test
    void throwIfCancelledDoesNotThrowWhenNotCancelled() throws InterruptedException {
        CancellationToken token = new CancellationToken();
        token.throwIfCancelled();
    }
}
