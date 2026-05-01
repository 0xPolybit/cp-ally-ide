package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class ProblemCacheRepository {

    private static final String PROBLEMS_DIRECTORY_NAME = "problems";
    private static final String FILE_EXTENSION = ".properties";
    private static final String KEY_CODE = "code";
    private static final String KEY_TITLE = "title";
    private static final String KEY_HTML = "html";
    private static final String KEY_SAVED_AT = "savedAt";

    private final Path problemsDirectory;

    ProblemCacheRepository(Path appDataDirectory) {
        this.problemsDirectory = appDataDirectory.resolve(PROBLEMS_DIRECTORY_NAME);
    }

    void save(ProblemDetails problemDetails) {
        if (problemDetails == null || isBlank(problemDetails.code()) || problemDetails.problemHtml() == null) {
            return;
        }

        try {
            Files.createDirectories(problemsDirectory);

            long savedAt = System.currentTimeMillis();
            Properties properties = new Properties();
            properties.setProperty(KEY_CODE, problemDetails.code());
            properties.setProperty(KEY_TITLE, problemDetails.title() != null ? problemDetails.title() : "");
            properties.setProperty(KEY_HTML, problemDetails.problemHtml());
            properties.setProperty(KEY_SAVED_AT, Long.toString(savedAt));

            String fileName = sanitize(problemDetails.code()) + FILE_EXTENSION;
            Path problemFile = problemsDirectory.resolve(fileName);
            try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(problemFile))) {
                properties.store(output, "Competitive Programming Ally problem cache");
            }
        } catch (IOException ignored) {
            // Cache persistence is best-effort only.
        }
    }

    ProblemDetails load(String problemCode) {
        if (isBlank(problemCode)) {
            return null;
        }

        if (!Files.isDirectory(problemsDirectory)) {
            return null;
        }

        try {
            String fileName = sanitize(problemCode) + FILE_EXTENSION;
            Path problemFile = problemsDirectory.resolve(fileName);
            if (!Files.exists(problemFile)) {
                return null;
            }

            Properties properties = new Properties();
            try (InputStream input = new BufferedInputStream(Files.newInputStream(problemFile))) {
                properties.load(input);
            }

            String code = properties.getProperty(KEY_CODE, "");
            String title = properties.getProperty(KEY_TITLE, "");
            String html = properties.getProperty(KEY_HTML);

            if (isBlank(code) || html == null) {
                return null;
            }

            return new ProblemDetails(code, title, html);
        } catch (IOException ignored) {
            return null;
        }
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
