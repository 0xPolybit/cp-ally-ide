package com.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-wide bounded executors for blocking background work.
 *
 * <p>The coordinator prevents each feature from creating its own
 * unbounded collection of pools and threads. Pools are daemon-backed so
 * a failed UI shutdown cannot keep the JVM alive; {@link #shutdownNow()}
 * is still called during application disposal when available.</p>
 */
final class TaskCoordinator {

    private static final TaskCoordinator SHARED = new TaskCoordinator();

    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(
            4, namedFactory("cpa-network"));
    private final ExecutorService renderExecutor = Executors.newFixedThreadPool(
            2, namedFactory("cpa-render"));
    private final ExecutorService processIoExecutor = Executors.newFixedThreadPool(
            6, namedFactory("cpa-process-io"));
    private final ExecutorService toolchainExecutor = Executors.newFixedThreadPool(
            2, namedFactory("cpa-toolchain"));
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2, namedFactory("cpa-scheduler"));

    static TaskCoordinator shared() {
        return SHARED;
    }

    ExecutorService network() {
        return networkExecutor;
    }

    ExecutorService rendering() {
        return renderExecutor;
    }

    ExecutorService processIo() {
        return processIoExecutor;
    }

    ExecutorService toolchain() {
        return toolchainExecutor;
    }

    ScheduledExecutorService scheduler() {
        return scheduler;
    }

    <T> Future<T> submitNetwork(java.util.concurrent.Callable<T> task) {
        return networkExecutor.submit(task);
    }

    <T> Future<T> submitRendering(java.util.concurrent.Callable<T> task) {
        return renderExecutor.submit(task);
    }

    <T> Future<T> submitProcessIo(java.util.concurrent.Callable<T> task) {
        return processIoExecutor.submit(task);
    }

    ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(task, delay, unit);
    }

    void shutdownNow() {
        networkExecutor.shutdownNow();
        renderExecutor.shutdownNow();
        processIoExecutor.shutdownNow();
        toolchainExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable,
                    prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
