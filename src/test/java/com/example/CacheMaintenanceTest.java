package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CacheMaintenanceTest {
    @Test
    void removesStaleFiles(@TempDir Path temp) throws Exception {
        Files.createDirectories(temp);
        Path stale = temp.resolve("stale.cache");
        Files.writeString(stale, "old");
        Files.setLastModifiedTime(stale, java.nio.file.attribute.FileTime.fromMillis(0));
        CacheMaintenance.cleanup(temp);
        assertFalse(Files.exists(stale));
    }
}
