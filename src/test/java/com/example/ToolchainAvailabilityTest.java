package com.example;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolchainAvailabilityTest {

    /** A stub CodeExecutionService whose detectSupport returns whatever we configure. */
    static class StubService extends CodeExecutionService {
        private final CodeExecutionService.LanguageSupport result;
        private final long delayMillis;
        volatile int callCount = 0;

        StubService(CodeExecutionService.LanguageSupport result) {
            this(result, 0L);
        }

        StubService(CodeExecutionService.LanguageSupport result, long delayMillis) {
            super();
            this.result = result;
            this.delayMillis = delayMillis;
        }

        @Override
        CodeExecutionService.LanguageSupport detectSupport(String language) {
            callCount++;
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return result;
        }
    }

    @Test
    void cachesResultAfterFirstProbe() throws Exception {
        StubService stub = new StubService(
                new CodeExecutionService.LanguageSupport(true, "ready"));
        ToolchainAvailability cache = new ToolchainAvailability(stub);

        CountDownLatch first = new CountDownLatch(1);
        AtomicReference<CodeExecutionService.LanguageSupport> firstResult = new AtomicReference<>();
        cache.probe("Python 3", s -> {
            firstResult.set(s);
            first.countDown();
        });
        assertTrue(first.await(2, TimeUnit.SECONDS), "first probe should complete");
        assertNotNull(firstResult.get());
        assertTrue(firstResult.get().supported());

        // Second probe for the same language should reuse the cache and not
        // re-invoke the underlying service.
        CountDownLatch second = new CountDownLatch(1);
        AtomicReference<CodeExecutionService.LanguageSupport> secondResult = new AtomicReference<>();
        cache.probe("Python 3", s -> {
            secondResult.set(s);
            second.countDown();
        });
        assertTrue(second.await(2, TimeUnit.SECONDS), "second probe should complete");
        assertNotNull(secondResult.get());
        assertEquals(1, stub.callCount, "DetectSupport should be called exactly once for cached languages");
    }

    @Test
    void unknownLanguageProbeRunsDetect() throws Exception {
        StubService stub = new StubService(
                new CodeExecutionService.LanguageSupport(false, "missing toolchain"));
        ToolchainAvailability cache = new ToolchainAvailability(stub);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CodeExecutionService.LanguageSupport> result = new AtomicReference<>();
        cache.probe("GNU C11 5.1.0", s -> {
            result.set(s);
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertEquals(false, result.get().supported());
        assertEquals(1, stub.callCount);
    }

    @Test
    void cachesPerLanguageIndependently() throws Exception {
        StubService stub = new StubService(
                new CodeExecutionService.LanguageSupport(true, "ready"));
        ToolchainAvailability cache = new ToolchainAvailability(stub);

        CountDownLatch a = new CountDownLatch(1);
        CountDownLatch b = new CountDownLatch(1);
        cache.probe("Python 3", s -> a.countDown());
        cache.probe("Java 21", s -> b.countDown());
        assertTrue(a.await(2, TimeUnit.SECONDS));
        assertTrue(b.await(2, TimeUnit.SECONDS));
        assertEquals(2, stub.callCount);
    }

    @Test
    void deliverOnEdtWhenCalledFromBackground() throws Exception {
        StubService stub = new StubService(
                new CodeExecutionService.LanguageSupport(true, "ready"));
        ToolchainAvailability cache = new ToolchainAvailability(stub);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> wasEdt = new AtomicReference<>();
        cache.probe("Python 3", s -> {
            wasEdt.set(SwingUtilities.isEventDispatchThread());
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(Boolean.TRUE, wasEdt.get(),
                "probe completion callback must run on the EDT");
    }

    @Test
    void nullInputsAreNoOp() {
        StubService stub = new StubService(
                new CodeExecutionService.LanguageSupport(true, "ready"));
        ToolchainAvailability cache = new ToolchainAvailability(stub);

        cache.probe(null, s -> { });
        cache.probe("Python 3", null);
        assertEquals(0, stub.callCount);
        assertNull(cache.cached(null));
    }
}
