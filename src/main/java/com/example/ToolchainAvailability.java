package com.example;

import javax.swing.SwingUtilities;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Asynchronous wrapper around {@link CodeExecutionService#detectSupport(String)}.
 *
 * <p>{@code detectSupport} can take a few seconds the first time it runs for a
 * given language because it spawns {@code where} / {@code command -v} subprocesses
 * for each required toolchain. Calling it on the Event Dispatch Thread would
 * freeze the UI during language selection.</p>
 *
 * <p>This class hands every probe to a single background {@link ExecutorService}
 * and delivers the result to a {@link Consumer} on the EDT. Results are cached
 * in memory, so subsequent language switches that revisit a previously-checked
 * language complete instantly.</p>
 *
 * <p>The probe executor is shared across instances of this class so even
 * multiple components of the UI can request the same language at once without
 * triggering duplicate subprocesses.</p>
 */
final class ToolchainAvailability {

    private final CodeExecutionService codeExecutionService;
    private final TaskCoordinator coordinator = TaskCoordinator.shared();
    private final java.util.Map<String, CodeExecutionService.LanguageSupport> cache =
            new ConcurrentHashMap<>();
    private final java.util.Set<String> inFlight =
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    ToolchainAvailability(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    /**
     * Submits an asynchronous probe. The {@code onComplete} consumer is
     * always invoked on the EDT. The cache short-circuits the probe for any
     * language we've already checked, so the consumer is called synchronously
     * in that case.
     */
    Future<?> probe(String language, Consumer<CodeExecutionService.LanguageSupport> onComplete) {
        if (language == null || onComplete == null) {
            return null;
        }
        CodeExecutionService.LanguageSupport cached = cache.get(language);
        if (cached != null) {
            deliver(onComplete, cached);
            return null;
        }
        if (!inFlight.add(language)) {
            // Another caller is already probing; the new consumer will be
            // notified when the in-flight probe completes.
            pendingListeners
                    .computeIfAbsent(language, key -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(onComplete);
            return null;
        }
        pendingListeners
                .computeIfAbsent(language, key -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(onComplete);
        return coordinator.toolchain().submit(() -> {
            CodeExecutionService.LanguageSupport result;
            try {
                result = codeExecutionService.detectSupport(language);
            } catch (Throwable t) {
                DiagnosticLogger.error("[ToolchainAvailability] Probe failed for " + language, t);
                result = new CodeExecutionService.LanguageSupport(false, "Probe failed: " + t.getMessage());
            }
            cache.put(language, result);
            inFlight.remove(language);
            java.util.List<Consumer<CodeExecutionService.LanguageSupport>> waiters =
                    pendingListeners.remove(language);
            if (waiters != null) {
                for (Consumer<CodeExecutionService.LanguageSupport> waiter : waiters) {
                    deliver(waiter, result);
                }
            }
        });
    }

    /** Returns true if a probe result is already cached for the given language. */
    boolean isKnown(String language) {
        return language != null && cache.containsKey(language);
    }

    /** Returns the cached result, or null if nothing is cached. */
    CodeExecutionService.LanguageSupport cached(String language) {
        return language == null ? null : cache.get(language);
    }

    private static void deliver(Consumer<CodeExecutionService.LanguageSupport> consumer,
                                CodeExecutionService.LanguageSupport result) {
        if (SwingUtilities.isEventDispatchThread()) {
            consumer.accept(result);
        } else {
            SwingUtilities.invokeLater(() -> consumer.accept(result));
        }
    }

    private final java.util.Map<String, java.util.List<Consumer<CodeExecutionService.LanguageSupport>>> pendingListeners =
            new ConcurrentHashMap<>();
}
