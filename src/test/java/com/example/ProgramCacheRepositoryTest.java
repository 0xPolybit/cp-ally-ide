package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramCacheRepositoryTest {

    @Test
    void saveCreatesDeterministicCurrentFile(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "print(1)\n");
        Path expected = repo.currentFileFor("2208A", "Python 3");
        assertTrue(Files.exists(expected), "save must create the deterministic current file");
    }

    @Test
    void loadReturnsTheSameSourceThatWasSaved(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        String source = "def solve():\n    return 42\n";
        repo.save("1A", "Python 3", source);
        assertEquals(source, repo.loadLatestSource("1A", "Python 3"));
    }

    @Test
    void loadReturnsNullForUnknownProblem(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        assertNull(repo.loadLatestSource("NOPE", "Python 3"));
    }

    @Test
    void saveIsAtomic(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "v1");
        // Save again; the temp file must be gone (rename succeeded).
        repo.save("2208A", "Python 3", "v2");
        Path current = repo.currentFileFor("2208A", "Python 3");
        assertEquals("v2", repo.loadLatestSource("2208A", "Python 3"));
        Path parent = current.getParent();
        try (var stream = Files.list(parent)) {
            assertTrue(stream.allMatch(p -> p.getFileName().toString().equals("current.txt")),
                    "No leftover temp files should remain in the source directory");
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Test
    void emptyProblemIsNotPersisted(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("__EMPTY_PROBLEM__", "Python 3", "transient");
        assertNull(repo.loadLatestSource("__EMPTY_PROBLEM__", "Python 3"));
    }

    @Test
    void saveRefusesBlankProblemOrLanguage(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save(null, "Python 3", "x");
        repo.save("",   "Python 3", "x");
        repo.save("2208A", null, "x");
        repo.save("2208A", "",   "x");
        repo.save("2208A", "Python 3", null);
        // No files should have been written.
        assertFalse(Files.exists(repo.currentFileFor("", "Python 3")));
    }

    @Test
    void takeSnapshotCreatesBoundedHistory(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        for (int i = 0; i < 5; i++) {
            repo.save("2208A", "Python 3", "v" + i);
            repo.takeSnapshot("2208A", "Python 3");
        }
        Path snapshots = CacheNames.sourceDir(tempDir)
                .resolve("2208A")
                .resolve("Python_3")
                .resolve("snapshots");
        assertTrue(Files.isDirectory(snapshots));
        // At most 3 snapshots retained.
        long count;
        try (var stream = Files.list(snapshots)) {
            count = stream.count();
        } catch (IOException ioe) {
            count = -1;
        }
        assertTrue(count <= 3, "snapshot count must be bounded; got " + count);
    }

    @Test
    void legacyFlatFileIsMigratedToNewLayout(@TempDir Path tempDir) throws Exception {
        // Simulate the 0.2.x layout: a single *.properties file in the
        // cache/source/ root.
        Path sourceDir = CacheNames.sourceDir(tempDir);
        Files.createDirectories(sourceDir);
        Path legacy = sourceDir.resolve("2208A__Python_3__1700000000__legacy.properties");
        Properties properties = new Properties();
        properties.setProperty("problem.code", "2208A");
        properties.setProperty("language", "Python 3");
        properties.setProperty("source", "legacy source\n");
        properties.setProperty("savedAt", "1700000000");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(legacy))) {
            properties.store(out, "legacy");
        }

        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        // The migration runs on the first load or save for that key.
        assertEquals("legacy source\n", repo.loadLatestSource("2208A", "Python 3"));

        // The new layout must contain the current file with the legacy
        // contents; the legacy file itself can be cleaned up later.
        Path current = repo.currentFileFor("2208A", "Python 3");
        assertTrue(Files.exists(current));
    }

    @Test
    void legacyMigrationPicksMostRecentEntryPerKey(@TempDir Path tempDir) throws Exception {
        // Two legacy entries for the same key: only the most recent must
        // be migrated into the new layout.
        Path sourceDir = CacheNames.sourceDir(tempDir);
        Files.createDirectories(sourceDir);
        writeLegacy(sourceDir, "2208A", "Python 3", "old", 100L);
        writeLegacy(sourceDir, "2208A", "Python 3", "new", 999L);

        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        assertEquals("new", repo.loadLatestSource("2208A", "Python 3"));
    }

    @Test
    void clearAllRemovesAllSourceFiles(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "x");
        repo.save("1A",    "Java 21",   "y");
        repo.clearAll();
        assertNull(repo.loadLatestSource("2208A", "Python 3"));
        assertNull(repo.loadLatestSource("1A", "Java 21"));
    }

    @Test
    void saveOverwritesPreviousContent(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "first");
        repo.save("2208A", "Python 3", "second");
        assertEquals("second", repo.loadLatestSource("2208A", "Python 3"));
    }

    @Test
    void takeSnapshotDoesNotChangeCurrentSource(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "v1");
        repo.takeSnapshot("2208A", "Python 3");
        assertEquals("v1", repo.loadLatestSource("2208A", "Python 3"));
    }

    @Test
    void takeSnapshotIsNoOpWhenCurrentMissing(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        // Should not throw even when the current file does not exist.
        repo.takeSnapshot("NOPE", "Python 3");
        assertNull(repo.loadLatestSource("NOPE", "Python 3"));
    }

    @Test
    void distinctLanguagesHaveIndependentSlots(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3",  "py");
        repo.save("2208A", "Java 21",   "java");
        assertEquals("py",   repo.loadLatestSource("2208A", "Python 3"));
        assertEquals("java", repo.loadLatestSource("2208A", "Java 21"));
    }

    @Test
    void currentFileForReturnsExpectedPath(@TempDir Path tempDir) {
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        Path p = repo.currentFileFor("2208A", "Python 3");
        assertNotNull(p);
        assertTrue(p.toString().endsWith("cache" + java.io.File.separator
                + "source" + java.io.File.separator
                + "2208A" + java.io.File.separator
                + "Python_3" + java.io.File.separator
                + "current.txt"));
    }

    private static void writeLegacy(Path dir, String problem, String language,
                                     String source, long savedAt) throws IOException {
        Path file = dir.resolve(problem + "__" + language + "__" + savedAt + "__legacy.properties");
        Properties properties = new Properties();
        properties.setProperty("problem.code", problem);
        properties.setProperty("language", language);
        properties.setProperty("source", source);
        properties.setProperty("savedAt", Long.toString(savedAt));
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            properties.store(out, "legacy");
        }
    }
}
