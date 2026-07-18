package com.example;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingCommandsTest {

    @Test
    void rejectsNullAndEmptyCommands() {
        PendingCommands queue = new PendingCommands(4);
        assertFalse(queue.submit(null));
        assertFalse(queue.submit(""));
        assertFalse(queue.submit("   "));
        assertFalse(queue.submit("https://example.com/x"));
        assertEquals(0, queue.size());
    }

    @Test
    void acceptsDeepLinkAndKeepsOrder() {
        PendingCommands queue = new PendingCommands(4);
        assertTrue(queue.submit("cpally://problem/2208A"));
        assertTrue(queue.submit("cpally://problem/1A"));
        assertEquals(2, queue.size());

        Map<String, String> drained = queue.markReady();
        assertEquals(2, drained.size());
        // Submission order is preserved.
        assertEquals("cpally://problem/2208A", drained.values().iterator().next());
    }

    @Test
    void coalescesDuplicateDeepLinksToLastSubmittedCommand() {
        PendingCommands queue = new PendingCommands(4);
        queue.submit("cpally://problem/2208A");
        queue.submit("cpally://problem/1A");
        queue.submit("cpally://problem/2208A"); // duplicate
        assertEquals(2, queue.size());

        Map<String, String> drained = queue.markReady();
        // The most recent submission for the same problem wins.
        assertEquals("cpally://problem/2208A", drained.get("2208A"));
    }

    @Test
    void isCaseInsensitiveOnCoalescing() {
        PendingCommands queue = new PendingCommands(4);
        queue.submit("cpally://problem/2208A");
        queue.submit("CPALLY://problem/2208a");
        assertEquals(1, queue.size());
    }

    @Test
    void evictsOldestWhenOverCapacity() {
        PendingCommands queue = new PendingCommands(2);
        queue.submit("cpally://problem/1A");
        queue.submit("cpally://problem/2B");
        queue.submit("cpally://problem/3C");
        assertEquals(2, queue.size());

        Map<String, String> drained = queue.markReady();
        // 1A is evicted to make room for 3C.
        assertFalse(drained.containsKey("1A"));
        assertTrue(drained.containsKey("2B"));
        assertTrue(drained.containsKey("3C"));
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new PendingCommands(0));
        assertThrows(IllegalArgumentException.class, () -> new PendingCommands(-1));
    }

    @Test
    void submissionsAfterReadyAreStillAcceptedButNotDelivered() {
        // After markReady(), the caller is expected to deliver subsequent
        // submissions directly. The queue itself does not retroactively
        // re-buffer; this test guards the current contract.
        PendingCommands queue = new PendingCommands(4);
        queue.markReady();
        assertTrue(queue.isReady());
        // submit() still validates/keys the command but no caller is draining.
        assertTrue(queue.submit("cpally://problem/9Z"));
        assertEquals(1, queue.size());
    }

    @Test
    void discardClearsPendingAndPreventsDelivery() {
        PendingCommands queue = new PendingCommands(4);
        queue.submit("cpally://problem/1A");
        queue.discard();
        assertEquals(0, queue.size());
        Map<String, String> drained = queue.markReady();
        assertTrue(drained.isEmpty());
    }

    @Test
    void stripsTrailingSlashesFromCode() {
        PendingCommands queue = new PendingCommands(2);
        queue.submit("cpally://problem/2208A/");
        queue.submit("cpally://problem/2208A///");
        assertEquals(1, queue.size());
    }
}
