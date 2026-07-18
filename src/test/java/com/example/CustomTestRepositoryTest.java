package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomTestRepositoryTest {

    @Test
    void saveThenLoadRoundtrips(@TempDir Path tempDir) throws Exception {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        List<CodeExecutionService.TestCaseSpec> tests = List.of(
                new CodeExecutionService.TestCaseSpec("in1", "out1", true, true, "Custom 1"),
                new CodeExecutionService.TestCaseSpec("in2", "",      true, false, "Custom 2"));
        repo.save("2208A", tests);
        repo.flush();

        List<CodeExecutionService.TestCaseSpec> loaded = repo.load("2208A");
        assertEquals(2, loaded.size());
        assertEquals("in1", loaded.get(0).input());
        assertEquals("out1", loaded.get(0).expectedOutput());
        assertEquals("in2", loaded.get(1).input());
        assertEquals(false, loaded.get(1).expectedOutputProvided());
    }

    @Test
    void emptyProblemIsNotPersisted(@TempDir Path tempDir) {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        repo.save("__EMPTY_PROBLEM__", List.of(
                new CodeExecutionService.TestCaseSpec("in", "out", true, true, "x")));
        repo.flush();
        // No file should be created.
        assertFalse(java.nio.file.Files.exists(repo.fileFor("__EMPTY_PROBLEM__")));
    }

    @Test
    void blankProblemCodeIsIgnored(@TempDir Path tempDir) {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        repo.save(null, List.of(
                new CodeExecutionService.TestCaseSpec("in", "out", true, true, "x")));
        repo.save("",   List.of(
                new CodeExecutionService.TestCaseSpec("in", "out", true, true, "x")));
        repo.flush();
        // Nothing should be written.
        assertEquals(0, listJsonFiles(tempDir).size());
    }

    @Test
    void nonCustomEntriesAreFiltered(@TempDir Path tempDir) throws Exception {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        List<CodeExecutionService.TestCaseSpec> mixed = new ArrayList<>();
        mixed.add(new CodeExecutionService.TestCaseSpec("a", "b", true, true, "Custom 1"));
        mixed.add(new CodeExecutionService.TestCaseSpec("a", "b", false, true, "Sample 1"));
        mixed.add(null);
        repo.save("2208A", mixed);
        repo.flush();

        List<CodeExecutionService.TestCaseSpec> loaded = repo.load("2208A");
        assertEquals(1, loaded.size());
        assertTrue(loaded.get(0).custom());
    }

    @Test
    void loadReturnsEmptyForUnknownProblem(@TempDir Path tempDir) {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        List<CodeExecutionService.TestCaseSpec> loaded = repo.load("DOES_NOT_EXIST");
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void loadIgnoresNullAndMalformedFile(@TempDir Path tempDir) throws Exception {
        // Manually write a malformed file to ensure load() doesn't throw.
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        java.nio.file.Files.createDirectories(tempDir.resolve("tests"));
        java.nio.file.Files.writeString(repo.fileFor("2208A"), "not valid json");

        // load() must return an empty list and log a warning, not throw.
        List<CodeExecutionService.TestCaseSpec> loaded = repo.load("2208A");
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void debouncedSaveCoalescesMultipleUpdates(@TempDir Path tempDir) throws Exception {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        // Three rapid saves; only the last should be on disk.
        repo.save("2208A", List.of(
                new CodeExecutionService.TestCaseSpec("in1", "out1", true, true, "Custom 1")));
        repo.save("2208A", List.of(
                new CodeExecutionService.TestCaseSpec("in1", "out1", true, true, "Custom 1"),
                new CodeExecutionService.TestCaseSpec("in2", "out2", true, true, "Custom 2")));
        repo.save("2208A", List.of(
                new CodeExecutionService.TestCaseSpec("in-final", "out-final", true, true, "Custom final")));
        // Force the writer to drain the pending tasks.
        repo.flush();
        List<CodeExecutionService.TestCaseSpec> loaded = repo.load("2208A");
        assertEquals(1, loaded.size());
        assertEquals("in-final", loaded.get(0).input());
    }

    @Test
    void saveWritesJsonFile(@TempDir Path tempDir) throws Exception {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        repo.save("2208A", List.of(
                new CodeExecutionService.TestCaseSpec("in", "out", true, true, "x")));
        repo.flush();
        assertTrue(java.nio.file.Files.exists(repo.fileFor("2208A")));
        // File should be valid JSON.
        String content = java.nio.file.Files.readString(repo.fileFor("2208A"));
        assertTrue(content.contains("\"input\""));
        assertTrue(content.contains("\"in\""));
    }

    @Test
    void fileNameSanitizationStripsPathSeparators(@TempDir Path tempDir) {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        // Problem code with characters that are unsafe for filenames must be
        // sanitized so the resulting file lives inside the tests directory.
        repo.save("../escape/attack", List.of(
                new CodeExecutionService.TestCaseSpec("in", "out", true, true, "x")));
        repo.flush();
        // The file should be inside the tests directory, not outside.
        // Sanitization collapses "../" to "__" (each '/' -> '_' and the
        // leading ".." is replaced with "_" to avoid a path traversal).
        Path expected = tempDir.resolve("tests").resolve("__escape_attack.json");
        assertTrue(java.nio.file.Files.exists(expected));
        assertFalse(java.nio.file.Files.exists(tempDir.resolve("escape")));
    }

    @Test
    void flushIsIdempotent(@TempDir Path tempDir) {
        CustomTestRepository repo = new CustomTestRepository(tempDir);
        repo.flush();
        repo.flush();
        // No exception means it ran.
    }

    private static List<Path> listJsonFiles(Path dir) {
        List<Path> out = new ArrayList<>();
        try (var stream = java.nio.file.Files.list(dir)) {
            stream.forEach(p -> {
                if (p.getFileName().toString().endsWith(".json")) out.add(p);
            });
        } catch (java.io.IOException ignored) { }
        return out;
    }
}
