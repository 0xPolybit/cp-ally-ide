package com.example;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TaskCoordinatorTest {

    @Test
    void sharedCoordinatorIsSingleton() {
        assertSame(TaskCoordinator.shared(), TaskCoordinator.shared());
    }

    @Test
    void exposesBoundedNamedExecutors() {
        TaskCoordinator coordinator = TaskCoordinator.shared();
        assertNotNull(coordinator.network());
        assertNotNull(coordinator.rendering());
        assertNotNull(coordinator.processIo());
        assertNotNull(coordinator.toolchain());
        assertNotNull(coordinator.scheduler());
    }

    @Test
    void submitsWorkToProcessIoPool() throws Exception {
        String name = coordinatorThreadName(TaskCoordinator.shared().submitProcessIo(
                () -> Thread.currentThread().getName()).get(2, TimeUnit.SECONDS));
        assertNotNull(name);
        org.junit.jupiter.api.Assertions.assertTrue(name.startsWith("cpa-process-io-"), name);
    }

    @Test
    void schedulesWorkOnScheduler() throws Exception {
        String name = TaskCoordinator.shared().schedule(
                () -> { }, 1, TimeUnit.MILLISECONDS).getClass().getName();
        assertNotNull(name);
    }

    private static String coordinatorThreadName(String value) {
        return value;
    }
}
