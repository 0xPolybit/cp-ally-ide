package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Whole-cache operations. Splits the on-disk cache into source, latex, and
 * icons subdirectories so a single "Clear All Cache" can wipe each one
 * independently, but a "Clear Source" call cannot accidentally nuke the
 * LaTeX and icon caches that take a long time to rebuild.
 */
final class CacheManager {

    private CacheManager() {
    }

    /**
     * Removes all cache subdirectories (source, latex, icons) and any
     * legacy top-level cache files that predate the namespace split.
     */
    static void clearAll(Path appDataDirectory) {
        deleteRecursively(CacheNames.sourceDir(appDataDirectory));
        deleteRecursively(CacheNames.latexDir(appDataDirectory));
        deleteRecursively(CacheNames.iconsDir(appDataDirectory));
        // Clean up any legacy files that lived directly under cache/
        // before the namespace split. After this point only the named
        // subdirectories should exist under cache/.
        Path legacy = CacheNames.legacyCacheDir(appDataDirectory);
        if (Files.isDirectory(legacy)) {
            try (var stream = Files.list(legacy)) {
                stream.forEach(child -> {
                    // Skip the subdirectories we just cleared (they may
                    // have been recreated above).
                    String name = child.getFileName().toString();
                    if ("source".equals(name) || "latex".equals(name) || "icons".equals(name)) {
                        return;
                    }
                    deleteRecursively(child);
                });
            } catch (IOException ioe) {
                DiagnosticLogger.warn("[CacheManager] Legacy cleanup failed: " + ioe.getMessage());
            }
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try {
            Files.walk(root)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
