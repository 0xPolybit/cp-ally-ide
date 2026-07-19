package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stores custom (user-defined) test cases per problem code on disk and
 * debounces writes so rapid edits do not thrash the disk. Test cases are
 * kept in memory by problem code; the on-disk file is a JSON array of
 * {@link CodeExecutionService.TestCaseSpec} records.
 *
 * <p>Reads are served from the in-memory map when present, falling back to
 * disk. Writes schedule a delayed save and coalesce repeated updates for
 * the same problem onto a single disk write.</p>
 */
final class CustomTestRepository {

    private static final String TESTS_DIRECTORY = "tests";
    private static final long DEBOUNCE_MILLIS = 400L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path testsDirectory;
    private final ConcurrentHashMap<String, List<CodeExecutionService.TestCaseSpec>> cache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingSaves =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService writer = TaskCoordinator.shared().scheduler();

    CustomTestRepository(Path appDataDirectory) {
        this.testsDirectory = appDataDirectory.resolve(TESTS_DIRECTORY);
    }

    /**
     * Returns the persisted custom tests for the given problem, or an empty
     * list if none are stored. The result is a defensive copy.
     */
    List<CodeExecutionService.TestCaseSpec> load(String problemCode) {
        if (problemCode == null || problemCode.isBlank() || "__EMPTY_PROBLEM__".equals(problemCode)) {
            return List.of();
        }
        List<CodeExecutionService.TestCaseSpec> cached = cache.get(problemCode);
        if (cached != null) {
            return List.copyOf(cached);
        }
        Path file = fileFor(problemCode);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            List<CodeExecutionService.TestCaseSpec> parsed = MAPPER.readValue(
                    bytes, new TypeReference<List<CodeExecutionService.TestCaseSpec>>() {});
            List<CodeExecutionService.TestCaseSpec> onlyCustom = new ArrayList<>();
            for (CodeExecutionService.TestCaseSpec spec : parsed) {
                if (spec != null && spec.custom()) {
                    onlyCustom.add(spec);
                }
            }
            cache.put(problemCode, onlyCustom);
            return List.copyOf(onlyCustom);
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[CustomTestRepository] Failed to load tests for "
                    + problemCode + ": " + ioe.getMessage());
            return List.of();
        }
    }

    /**
     * Persists the custom tests for a problem. The in-memory cache is updated
     * immediately; the on-disk write is debounced so multiple calls in quick
     * succession (e.g. a user adding several tests in a row) result in a
     * single disk write.
     */
    void save(String problemCode, List<CodeExecutionService.TestCaseSpec> tests) {
        if (problemCode == null || problemCode.isBlank() || "__EMPTY_PROBLEM__".equals(problemCode)) {
            return;
        }
        // Filter to custom tests only — sample tests are managed elsewhere.
        List<CodeExecutionService.TestCaseSpec> onlyCustom = new ArrayList<>();
        if (tests != null) {
            for (CodeExecutionService.TestCaseSpec spec : tests) {
                if (spec != null && spec.custom()) {
                    onlyCustom.add(spec);
                }
            }
        }
        cache.put(problemCode, onlyCustom);

        // Cancel any pending save and schedule a fresh one. Using
        // schedule(() -> ..., delay) gives us per-key coalescing because
        // the latest snapshot is read when the task actually runs.
        ScheduledFuture<?> previous = pendingSaves.remove(problemCode);
        if (previous != null) {
            previous.cancel(false);
        }
        ScheduledFuture<?> next = writer.schedule(
                () -> writeToDisk(problemCode, onlyCustom),
                DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
        pendingSaves.put(problemCode, next);
    }

    /**
     * Forces any pending writes to flush to disk immediately. Call this
     * before shutdown so a user does not lose test data if the process is
     * killed.
     */
    void flush() {
        for (String key : new ArrayList<>(pendingSaves.keySet())) {
            ScheduledFuture<?> pending = pendingSaves.remove(key);
            if (pending != null) {
                pending.cancel(false);
                List<CodeExecutionService.TestCaseSpec> snapshot = cache.get(key);
                if (snapshot != null) {
                    writeToDisk(key, snapshot);
                }
            }
        }
    }

    /** Visible for tests. */
    Path fileFor(String problemCode) {
        return testsDirectory.resolve(sanitize(problemCode) + ".json");
    }

    private void writeToDisk(String problemCode, List<CodeExecutionService.TestCaseSpec> tests) {
        try {
            Files.createDirectories(testsDirectory);
            Path file = fileFor(problemCode);
            // Atomic write: write to a temp file in the same directory and
            // rename. This avoids half-written test data if the process is
            // killed mid-write.
            Path tmp = Files.createTempFile(testsDirectory, sanitize(problemCode) + "-", ".json.tmp");
            byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(tests);
            try (OutputStream out = Files.newOutputStream(tmp)) {
                out.write(bytes);
            }
            try {
                Files.move(tmp, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException atomic) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[CustomTestRepository] Failed to save tests for "
                    + problemCode + ": " + ioe.getMessage());
        }
    }

    private static String sanitize(String value) {
        // Replace any character that isn't alphanumeric, dot, dash, or
        // underscore with a single underscore. This also strips path
        // separators and Windows-reserved leading dots so the result can
        // be safely used as a filename inside the tests directory.
        String cleaned = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        // Trim leading dots/dashes so the result never begins with a path
        // component ('..' or '-' that the OS could treat as a switch).
        int start = 0;
        while (start < cleaned.length()
                && (cleaned.charAt(start) == '.' || cleaned.charAt(start) == '-')) {
            start++;
        }
        if (start > 0) {
            cleaned = "_" + cleaned.substring(start);
        }
        return cleaned;
    }
}
