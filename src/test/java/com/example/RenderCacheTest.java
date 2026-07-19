package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderCacheTest {
    @Test
    void sha256IsDeterministic() {
        assertEquals(CacheKey.sha256("abc"), CacheKey.sha256("abc"));
        assertEquals(64, CacheKey.sha256("abc").length());
    }

    @Test
    void sha256SeparatesDifferentInputs() {
        assertNotEquals(CacheKey.sha256("abc"), CacheKey.sha256("abd"));
    }

    @Test
    void cleanupHandlesMissingDirectory(@TempDir Path temp) {
        RenderCacheMaintenance.cleanup(temp.resolve("missing"));
        assertTrue(Files.isDirectory(temp));
    }

    @Test
    void cleanupRemovesOldFiles(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("old.png");
        Files.writeString(file, "x");
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(0));
        RenderCacheMaintenance.cleanup(temp);
        assertTrue(!Files.exists(file));
    }
}
