package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentProblemRepositoryTest {
    @Test
    void recordsMostRecentFirstAndPersists(@TempDir Path temp) {
        RecentProblemRepository repository = new RecentProblemRepository(temp);
        repository.record("1A", "First");
        repository.record("2B", "Second");
        repository.record("1A", "Updated");
        assertEquals("1A", repository.list().get(0).code());
        assertEquals("Updated", new RecentProblemRepository(temp).list().get(0).title());
    }

    @Test
    void keepsHistoryBounded(@TempDir Path temp) {
        RecentProblemRepository repository = new RecentProblemRepository(temp);
        for (int i = 0; i < 30; i++) repository.record(i + "A", "Problem");
        assertEquals(15, repository.list().size());
    }
}
