package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheNamesTest {

    @Test
    void sourceDirIsUnderCache(@TempDir Path tempDir) {
        assertEquals(tempDir.resolve("cache").resolve("source"),
                CacheNames.sourceDir(tempDir));
    }

    @Test
    void latexDirIsUnderCache(@TempDir Path tempDir) {
        assertEquals(tempDir.resolve("cache").resolve("latex"),
                CacheNames.latexDir(tempDir));
    }

    @Test
    void iconsDirIsUnderCache(@TempDir Path tempDir) {
        assertEquals(tempDir.resolve("cache").resolve("icons"),
                CacheNames.iconsDir(tempDir));
    }

    @Test
    void legacyCacheDirIsCache(@TempDir Path tempDir) {
        assertEquals(tempDir.resolve("cache"),
                CacheNames.legacyCacheDir(tempDir));
    }

    @Test
    void cacheManagerRemovesAllThreeSubdirs(@TempDir Path tempDir) throws IOException {
        Path source = CacheNames.sourceDir(tempDir);
        Path latex = CacheNames.latexDir(tempDir);
        Path icons = CacheNames.iconsDir(tempDir);
        Files.createDirectories(source);
        Files.createDirectories(latex);
        Files.createDirectories(icons);
        Files.writeString(source.resolve("a.properties"), "x");
        Files.writeString(latex.resolve("a.png"), "x");
        Files.writeString(icons.resolve("a.png"), "x");

        CacheManager.clearAll(tempDir);

        assertFalse(Files.exists(source.resolve("a.properties")));
        assertFalse(Files.exists(latex.resolve("a.png")));
        assertFalse(Files.exists(icons.resolve("a.png")));
    }

    @Test
    void cacheManagerRemovesLegacyTopLevelFiles(@TempDir Path tempDir) throws IOException {
        // Simulate an old installation where source files lived directly
        // under cache/ instead of cache/source/.
        Path legacy = CacheNames.legacyCacheDir(tempDir);
        Files.createDirectories(legacy);
        Path orphan = legacy.resolve("old.properties");
        Files.writeString(orphan, "x");
        assertTrue(Files.exists(orphan));

        CacheManager.clearAll(tempDir);

        assertFalse(Files.exists(orphan),
                "Legacy top-level files must be removed on Clear All");
    }

    @Test
    void cacheManagerIsIdempotent(@TempDir Path tempDir) {
        // Calling clearAll on a directory that does not have a cache/ must
        // not throw.
        CacheManager.clearAll(tempDir);
        CacheManager.clearAll(tempDir);
    }

    @Test
    void programCacheRepositoryWritesOnlyToSourceSubdir(@TempDir Path tempDir) throws Exception {
        // Roundtrip: saving a program through ProgramCacheRepository must
        // place files under cache/source/, not the legacy cache/ root.
        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "print('hello')");
        Path sourceDir = CacheNames.sourceDir(tempDir);
        try (var stream = Files.list(sourceDir)) {
            List<Path> files = stream.toList();
            assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".properties")),
                    "Source files must live under cache/source/");
        }
        // The legacy cache/ root should not contain any source files after
        // ProgramCacheRepository has run.
        Path legacy = CacheNames.legacyCacheDir(tempDir);
        if (Files.isDirectory(legacy)) {
            try (var stream = Files.list(legacy)) {
                assertTrue(stream.allMatch(p -> p.getFileName().toString().equals("source")
                                || p.getFileName().toString().equals("latex")
                                || p.getFileName().toString().equals("icons")),
                        "Legacy cache/ may only contain the three namespace subdirs");
            }
        }
    }

    @Test
    void programCacheRepositoryClearAllLeavesLatexAndIconsIntact(@TempDir Path tempDir) throws Exception {
        // Place a synthetic LaTeX cache file, then clearAll() the program
        // cache. The LaTeX file must survive because it lives in a separate
        // namespace.
        Path latex = CacheNames.latexDir(tempDir);
        Files.createDirectories(latex);
        Path latexFile = latex.resolve("abc123.png");
        Files.writeString(latexFile, "binary-png-stub", StandardCharsets.US_ASCII);
        assertTrue(Files.exists(latexFile));

        ProgramCacheRepository repo = new ProgramCacheRepository(tempDir);
        repo.save("2208A", "Python 3", "x");
        repo.clearAll();

        assertTrue(Files.exists(latexFile),
                "clearAll on the source cache must not touch the LaTeX cache");
    }
}
