package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Bounded cleanup for rendered LaTeX/icon disk caches. */
final class RenderCacheMaintenance {
    private static final int MAX_FILES_PER_NAMESPACE = 1000;
    private static final long MAX_AGE_MILLIS = 30L * 24 * 60 * 60 * 1000;

    private RenderCacheMaintenance() { }

    static void cleanup(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return;
        try (var stream = Files.list(directory)) {
            long cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS;
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                try {
                    if (Files.getLastModifiedTime(file).toMillis() < cutoff) {
                        Files.deleteIfExists(file);
                    }
                } catch (IOException ignored) { }
            }
            try (var remaining = Files.list(directory)) {
                List<Path> sorted = remaining.filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong(RenderCacheMaintenance::modified).reversed())
                        .toList();
                for (int i = MAX_FILES_PER_NAMESPACE; i < sorted.size(); i++) {
                    Files.deleteIfExists(sorted.get(i));
                }
            }
        } catch (IOException ignored) { }
    }

    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return 0L; }
    }
}
