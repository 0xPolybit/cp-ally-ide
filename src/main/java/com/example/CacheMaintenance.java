package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Incremental cleanup for stale rendered and API cache entries. */
final class CacheMaintenance {
    private static final long MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(30);
    private static final int MAX_FILES = 1500;

    private CacheMaintenance() { }

    static void schedule(Path appDataDirectory) {
        TaskCoordinator.shared().scheduler().execute(() -> {
            cleanup(CacheNames.latexDir(appDataDirectory));
            cleanup(CacheNames.iconsDir(appDataDirectory));
            cleanup(appDataDirectory.resolve("problems"));
        });
    }

    static void cleanup(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return;
        try (var stream = Files.list(directory)) {
            long cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS;
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                try {
                    if (Files.getLastModifiedTime(path).toMillis() < cutoff) Files.deleteIfExists(path);
                } catch (IOException ignored) { }
            }
        } catch (IOException ignored) { return; }
        try (var stream = Files.list(directory)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong(CacheMaintenance::modified).reversed()).toList();
            for (int i = MAX_FILES; i < files.size(); i++) Files.deleteIfExists(files.get(i));
        } catch (IOException ignored) { }
    }

    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return 0L; }
    }
}
