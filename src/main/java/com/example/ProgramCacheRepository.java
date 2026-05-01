package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

final class ProgramCacheRepository {

    private static final String CACHE_DIRECTORY_NAME = "cache";
    private static final String FILE_EXTENSION = ".properties";
    private static final String KEY_PROBLEM_CODE = "problem.code";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_SOURCE_CODE = "source";
    private static final String KEY_SAVED_AT = "savedAt";

    private final Path cacheDirectory;

    ProgramCacheRepository(Path appDataDirectory) {
        this.cacheDirectory = appDataDirectory.resolve(CACHE_DIRECTORY_NAME);
    }

    void save(String problemCode, String language, String sourceCode) {
        if (isBlank(problemCode) || isBlank(language) || sourceCode == null) {
            return;
        }

        try {
            Files.createDirectories(cacheDirectory);

            long savedAt = System.currentTimeMillis();
            Properties properties = new Properties();
            properties.setProperty(KEY_PROBLEM_CODE, problemCode);
            properties.setProperty(KEY_LANGUAGE, language);
            properties.setProperty(KEY_SOURCE_CODE, sourceCode);
            properties.setProperty(KEY_SAVED_AT, Long.toString(savedAt));

            String fileName = sanitize(problemCode)
                    + "__"
                    + sanitize(language)
                    + "__"
                    + savedAt
                    + "__"
                    + UUID.randomUUID()
                    + FILE_EXTENSION;

            Path cacheFile = cacheDirectory.resolve(fileName);
            try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(cacheFile))) {
                properties.store(output, "Competitive Programming Ally cache");
            }
        } catch (IOException ignored) {
            // Cache persistence is best-effort only.
        }
    }

    String loadLatestSource(String problemCode, String language) {
        Optional<CacheEntry> cacheEntry = findLatestCacheEntry(problemCode, language);
        return cacheEntry.map(CacheEntry::sourceCode).orElse(null);
    }

    void clearAll() {
        if (!Files.isDirectory(cacheDirectory)) {
            return;
        }

        try {
            Files.walk(cacheDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    private Optional<CacheEntry> findLatestCacheEntry(String problemCode, String language) {
        if (isBlank(problemCode) || isBlank(language)) {
            return Optional.empty();
        }

        if (!Files.isDirectory(cacheDirectory)) {
            return Optional.empty();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDirectory, "*" + FILE_EXTENSION)) {
            return java.util.stream.StreamSupport.stream(stream.spliterator(), false)
                    .map(this::readCacheEntry)
                    .flatMap(Optional::stream)
                    .filter(entry -> Objects.equals(problemCode, entry.problemCode()) && Objects.equals(language, entry.language()))
                    .max(Comparator.comparingLong(CacheEntry::savedAt).thenComparing(CacheEntry::fileName));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private Optional<CacheEntry> readCacheEntry(Path cacheFile) {
        Properties properties = new Properties();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(cacheFile))) {
            properties.load(input);
            String problemCode = properties.getProperty(KEY_PROBLEM_CODE, "");
            String language = properties.getProperty(KEY_LANGUAGE, "");
            String sourceCode = properties.getProperty(KEY_SOURCE_CODE);
            long savedAt = parseLong(properties.getProperty(KEY_SAVED_AT), Files.getLastModifiedTime(cacheFile).toMillis());
            return Optional.of(new CacheEntry(cacheFile.getFileName().toString(), problemCode, language, sourceCode, savedAt));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CacheEntry(String fileName, String problemCode, String language, String sourceCode, long savedAt) {
    }
}