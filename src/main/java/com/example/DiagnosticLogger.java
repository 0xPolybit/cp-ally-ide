package com.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DiagnosticLogger {
    private static Path logFile = null;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private DiagnosticLogger() {
    }

    private static final long MAX_LOG_SIZE_BYTES = 1_000_000L;

    public static void initialize(Path appDataDirectory) {
        try {
            Files.createDirectories(appDataDirectory);
            Path candidate = appDataDirectory.resolve("diagnostics.log");
            rotateIfOversized(candidate);
            logFile = candidate;
            info("Diagnostic logger initialized. Log file: " + logFile.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to initialize DiagnosticLogger: " + e.getMessage());
        }
    }

    /** Keeps the log bounded: when it grows past the limit, roll it to *.old (replacing any previous roll). */
    private static void rotateIfOversized(Path candidate) {
        try {
            if (Files.exists(candidate) && Files.size(candidate) > MAX_LOG_SIZE_BYTES) {
                Path rolled = candidate.resolveSibling("diagnostics.log.old");
                Files.deleteIfExists(rolled);
                Files.move(candidate, rolled);
            }
        } catch (IOException ignored) {
            // Rotation is best-effort; logging continues on the existing file.
        }
    }

    public static void info(String message) {
        log("INFO", message, null);
    }

    public static void warn(String message) {
        log("WARN", message, null);
    }

    public static void error(String message) {
        log("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    private static synchronized void log(String level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(formatter);
        StringBuilder logEntry = new StringBuilder();
        logEntry.append(String.format("[%s] [%s] %s\n", timestamp, level, message));
        System.err.print(String.format("[%s] [%s] %s\n", timestamp, level, message));

        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            logEntry.append(sw.toString()).append("\n");
            throwable.printStackTrace(System.err);
        }

        if (logFile != null) {
            try {
                Files.writeString(logFile, logEntry.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Failed to write to log file: " + e.getMessage());
            }
        }
    }
}
