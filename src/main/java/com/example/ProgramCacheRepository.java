package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-(problem, language) program source storage.
 *
 * <p>Layout:</p>
 * <pre>
 *   &lt;appData&gt;/cache/source/
 *     &lt;problem&gt;/&lt;language&gt;/current.txt     (deterministic current file)
 *     &lt;problem&gt;/&lt;language&gt;/snapshots/...  (bounded snapshot history, opt-in)
 * </pre>
 *
 * <p>Autosave writes the deterministic current file atomically. The
 * snapshot history is only updated when the user explicitly switches
 * problem or language, so autosave does not have to write a new file
 * every interval. Loads are O(1) by path.</p>
 */
final class ProgramCacheRepository {

    private static final String CURRENT_FILE_NAME = "current.txt";
    private static final String SNAPSHOTS_DIR = "snapshots";
    private static final int MAX_ENTRIES_PER_KEY = 3;

    private final Path cacheDirectory;
    private final ReentrantLock migrationLock = new ReentrantLock();
    private final ConcurrentHashMap<String, Boolean> migratedKeys =
            new ConcurrentHashMap<>();
    private volatile boolean legacyMigrationDone = false;

    ProgramCacheRepository(Path appDataDirectory) {
        this.cacheDirectory = CacheNames.sourceDir(appDataDirectory);
    }

    /**
     * Writes the source for the given problem and language to a
     * deterministic file under cache/source/&lt;problem&gt;/&lt;language&gt;/current.txt.
     * The write is atomic: a temporary file is written and renamed into
     * place, so a power loss mid-write cannot corrupt the saved source.
     */
    void save(String problemCode, String language, String sourceCode) {
        if (isBlank(problemCode) || isBlank(language) || sourceCode == null) {
            return;
        }
        if (EMPTY_PROBLEM_CODE.equals(problemCode)) {
            // The empty problem slot is ephemeral; never persist it.
            return;
        }
        migrateLegacyIfNeeded(problemCode, language);
        Path current = currentFileFor(problemCode, language);
        Path tmp = null;
        try {
            Files.createDirectories(current.getParent());
            tmp = Files.createTempFile(current.getParent(),
                    "current-", ".tmp");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                out.write(sourceCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            try {
                Files.move(tmp, current,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException atomic) {
                Files.move(tmp, current, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[ProgramCacheRepository] Failed to save source for "
                    + problemCode + "/" + language + ": " + ioe.getMessage());
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Captures a bounded snapshot of the current source under
     * snapshots/&lt;timestamp&gt;.txt. The autosave loop never calls this;
     * it is intended for problem/language changes or explicit "snapshot"
     * actions. The total snapshot count per (problem, language) is capped
     * at {@link #MAX_ENTRIES_PER_KEY}.
     */
    void takeSnapshot(String problemCode, String language) {
        if (isBlank(problemCode) || isBlank(language) || EMPTY_PROBLEM_CODE.equals(problemCode)) {
            return;
        }
        Path current = currentFileFor(problemCode, language);
        if (!Files.exists(current)) {
            return;
        }
        try {
            Path snapshots = cacheDirectory.resolve(safeName(problemCode))
                    .resolve(safeName(language))
                    .resolve(SNAPSHOTS_DIR);
            Files.createDirectories(snapshots);
            String name = System.currentTimeMillis() + ".txt";
            Files.copy(current, snapshots.resolve(name),
                    StandardCopyOption.REPLACE_EXISTING);
            pruneSnapshots(snapshots);
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[ProgramCacheRepository] Snapshot failed for "
                    + problemCode + "/" + language + ": " + ioe.getMessage());
        }
    }

    String loadLatestSource(String problemCode, String language) {
        if (isBlank(problemCode) || isBlank(language) || EMPTY_PROBLEM_CODE.equals(problemCode)) {
            return null;
        }
        // Legacy data is read first so users upgrading from 0.2.x keep
        // their code. Once read, it is migrated to the new layout.
        migrateLegacyIfNeeded(problemCode, language);
        Path current = currentFileFor(problemCode, language);
        if (!Files.exists(current)) {
            return null;
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(current))) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            return null;
        }
    }

    void clearAll() {
        if (!Files.isDirectory(cacheDirectory)) {
            return;
        }
        try {
            Files.walk(cacheDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) { }
                    });
        } catch (IOException ignored) { }
        migratedKeys.clear();
        legacyMigrationDone = false;
    }

    Path currentFileFor(String problemCode, String language) {
        return cacheDirectory.resolve(safeName(problemCode))
                .resolve(safeName(language))
                .resolve(CURRENT_FILE_NAME);
    }

    /**
     * One-time, per-(problem, language) migration from the legacy flat
     * file layout to the new directory layout. Subsequent calls are
     * no-ops. Idempotent across restarts.
     */
    private void migrateLegacyIfNeeded(String problemCode, String language) {
        String key = problemCode + "|" + language;
        if (migratedKeys.containsKey(key)) {
            return;
        }
        migrationLock.lock();
        try {
            if (migratedKeys.containsKey(key)) {
                return;
            }
            doLegacyMigration();
            migratedKeys.put(key, Boolean.TRUE);
        } finally {
            migrationLock.unlock();
        }
    }

    private void doLegacyMigration() {
        if (legacyMigrationDone) {
            return;
        }
        // The legacy layout is a flat directory of *.properties files
        // at cache/source/ itself (not under <problem>/<language>/). We
        // iterate once, group by (problem, language), and pick the most
        // recent entry for each key to migrate.
        if (!Files.isDirectory(cacheDirectory)) {
            legacyMigrationDone = true;
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                cacheDirectory, "*.properties")) {
            java.util.Map<String, LegacyEntry> latest = new java.util.HashMap<>();
            for (Path file : stream) {
                LegacyEntry entry = readLegacyEntry(file);
                if (entry == null) continue;
                String k = entry.problemCode + "|" + entry.language;
                LegacyEntry existing = latest.get(k);
                if (existing == null || entry.savedAt > existing.savedAt) {
                    latest.put(k, entry);
                }
            }
            for (LegacyEntry entry : latest.values()) {
                Path current = currentFileFor(entry.problemCode, entry.language);
                if (Files.exists(current)) {
                    // Already migrated (race with a concurrent save).
                    continue;
                }
                Files.createDirectories(current.getParent());
                Files.writeString(current, entry.sourceCode,
                        java.nio.charset.StandardCharsets.UTF_8);
                // Keep the original file for now; CacheManager.clearAll or
                // the next "Clear All Cache" will remove orphans.
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[ProgramCacheRepository] Legacy migration failed: "
                    + ioe.getMessage());
        } finally {
            legacyMigrationDone = true;
        }
    }

    private LegacyEntry readLegacyEntry(Path cacheFile) {
        Properties properties = new Properties();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(cacheFile))) {
            properties.load(input);
            String problemCode = properties.getProperty("problem.code", "");
            String language = properties.getProperty("language", "");
            String sourceCode = properties.getProperty("source");
            if (problemCode.isBlank() || language.isBlank() || sourceCode == null) {
                return null;
            }
            long savedAt;
            try {
                savedAt = Long.parseLong(properties.getProperty("savedAt", "0"));
            } catch (NumberFormatException nfe) {
                savedAt = 0L;
            }
            return new LegacyEntry(problemCode, language, sourceCode, savedAt);
        } catch (IOException ioe) {
            return null;
        }
    }

    private void pruneSnapshots(Path snapshotsDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(snapshotsDir, "*.txt")) {
            java.util.List<Path> all = new java.util.ArrayList<>();
            stream.forEach(all::add);
            all.sort(Comparator.comparingLong(p -> parseLong(p.getFileName().toString(), 0L)));
            // Keep the most recent MAX_ENTRIES_PER_KEY snapshots.
            int toDelete = all.size() - MAX_ENTRIES_PER_KEY;
            for (int i = 0; i < toDelete; i++) {
                try {
                    Files.deleteIfExists(all.get(i));
                } catch (IOException ignored) { }
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[ProgramCacheRepository] Snapshot prune failed: "
                    + ioe.getMessage());
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String safeName(String value) {
        // Same rules as CustomTestRepository: drop path separators and
        // Windows-reserved leading dots so the result is a safe directory
        // name on every supported platform.
        String cleaned = value.replaceAll("[^a-zA-Z0-9._-]+", "_");
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final String EMPTY_PROBLEM_CODE = "__EMPTY_PROBLEM__";

    /** Internal record used only while migrating the legacy flat layout. */
    private record LegacyEntry(String problemCode, String language, String sourceCode, long savedAt) {}
}
