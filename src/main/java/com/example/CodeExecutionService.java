package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class CodeExecutionService {

    private static final long COMPILE_TIMEOUT_MILLIS = 60_000L;
    private static final long RUN_TIMEOUT_MILLIS = 2_000L;

    LanguageSupport detectSupport(String language) {
        LanguagePlan plan = resolvePlan(language);
        if (plan == null) {
            return new LanguageSupport(false, "No local runner is configured for this language.");
        }

        List<String> missingCommands = new ArrayList<>();
        for (String command : plan.requiredCommands()) {
            if (!isCommandAvailable(command)) {
                missingCommands.add(command);
            }
        }

        if (!missingCommands.isEmpty()) {
            return new LanguageSupport(false, "Missing local toolchain: " + String.join(", ", missingCommands));
        }

        return new LanguageSupport(true, "Ready to execute locally.");
    }

    ExecutionReport runSampleTests(String language, String sourceCode, List<TestCaseSpec> testCases) throws IOException, InterruptedException {
        LanguagePlan plan = resolvePlan(language);
        if (plan == null) {
            return ExecutionReport.failure("No local runner is configured for this language.");
        }

        List<String> missingCommands = new ArrayList<>();
        for (String command : plan.requiredCommands()) {
            if (!isCommandAvailable(command)) {
                missingCommands.add(command);
            }
        }
        if (!missingCommands.isEmpty()) {
            return ExecutionReport.failure("Missing local toolchain: " + String.join(", ", missingCommands));
        }

        Path workDir = Files.createTempDirectory("cpa-exec-");
        try {
            Path sourceFile = workDir.resolve(plan.sourceFileName());
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

            String compileLog = "";
            if (!plan.interpreted()) {
                ProcessResult compileResult = runProcess(plan.compileCommand().create(sourceFile, workDir), workDir, "", COMPILE_TIMEOUT_MILLIS);
                compileLog = joinNonBlank(compileResult.stdout(), compileResult.stderr());
                if (compileResult.timedOut()) {
                    return ExecutionReport.failure("Compilation timed out.\n\n" + compileLog);
                }
                if (compileResult.exitCode() != 0) {
                    return ExecutionReport.failure("Compilation failed.\n\n" + compileLog);
                }
            }

            List<TestCaseResult> results = new ArrayList<>();
            int index = 1;
            for (TestCaseSpec testCase : testCases) {
                ProcessResult runResult = runProcess(plan.runCommand().create(sourceFile, workDir), workDir, testCase.input(), RUN_TIMEOUT_MILLIS);
                String actual = normalizeOutput(runResult.stdout());
                boolean hasExpectedOutput = testCase.expectedOutputProvided() && testCase.expectedOutput() != null && !testCase.expectedOutput().isBlank();
                String expected = normalizeOutput(testCase.expectedOutput());
                boolean passed = !runResult.timedOut() && runResult.exitCode() == 0 && hasExpectedOutput && expected.equals(actual);
                boolean unknown = !runResult.timedOut() && runResult.exitCode() == 0 && !hasExpectedOutput;

                String details;
                if (runResult.timedOut()) {
                    details = "Time limit exceeded.";
                } else if (runResult.exitCode() != 0) {
                    details = joinNonBlank("Runtime error.", runResult.stderr());
                } else if (unknown) {
                    details = "Expected output not provided.";
                } else if (passed) {
                    details = "Output matched expected result.";
                } else {
                    details = joinNonBlank(
                            "Output differed from expected result.",
                            formatExpectedActual(expected, actual));
                }

                results.add(new TestCaseResult(
                        index++,
                    testCase.displayName(),
                        passed,
                        runResult.timedOut(),
                        unknown,
                        runResult.durationMillis(),
                        runResult.peakMemoryKb(),
                    testCase.input(),
                        testCase.expectedOutput(),
                        runResult.stdout(),
                        runResult.stderr(),
                        details));
            }

            return ExecutionReport.success(results, compileLog);
        } finally {
            deleteRecursively(workDir);
        }
    }

    private LanguagePlan resolvePlan(String language) {
        if (language == null) {
            return null;
        }

        String normalized = language.trim();
        if (normalized.startsWith("Python") || normalized.startsWith("PyPy")) {
            return new LanguagePlan(
                    "Main.py",
                    List.of(resolvePythonCommand()),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of(resolvePythonCommand(), sourceFile.toString()));
        }
        if (normalized.startsWith("GNU G++17")) {
            return compiledCppPlan("Main.cpp", "g++", "-std=c++17");
        }
        if (normalized.startsWith("GNU G++20")) {
            return compiledCppPlan("Main.cpp", "g++", "-std=c++20");
        }
        if (normalized.startsWith("GNU C11") || normalized.startsWith("GNU G11")) {
            return compiledCPlan("Main.c");
        }
        if (normalized.startsWith("Java ")) {
            return new LanguagePlan(
                    "Main.java",
                    List.of("javac", "java"),
                    false,
                    (sourceFile, workDir) -> List.of("javac", "-encoding", "UTF-8", sourceFile.getFileName().toString()),
                    (sourceFile, workDir) -> List.of("java", "-cp", workDir.toAbsolutePath().toString(), "Main"));
        }
        if (normalized.startsWith("Kotlin")) {
            return new LanguagePlan(
                    "Main.kt",
                    List.of("kotlinc", "java"),
                    false,
                    (sourceFile, workDir) -> List.of("kotlinc", sourceFile.getFileName().toString(), "-include-runtime", "-d", "main.jar"),
                    (sourceFile, workDir) -> List.of("java", "-jar", workDir.resolve("main.jar").toAbsolutePath().toString()));
        }
        if (normalized.startsWith("C#")) {
            return new LanguagePlan(
                    "Program.cs",
                    List.of("csc"),
                    false,
                    (sourceFile, workDir) -> List.of("csc", "/nologo", "/target:exe", "/out:Program.exe", sourceFile.getFileName().toString()),
                    (sourceFile, workDir) -> List.of(workDir.resolve("Program.exe").toAbsolutePath().toString()));
        }
        if (normalized.startsWith("Go")) {
            return new LanguagePlan(
                    "Main.go",
                    List.of("go"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("go", "run", sourceFile.getFileName().toString()));
        }
        if (normalized.startsWith("Rust")) {
            return new LanguagePlan(
                    "Main.rs",
                    List.of("rustc"),
                    false,
                    (sourceFile, workDir) -> List.of("rustc", "-O", sourceFile.getFileName().toString(), "-o", executableName()),
                    (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
        }
        if (normalized.startsWith("Node.js") || normalized.startsWith("JavaScript")) {
            return new LanguagePlan(
                    "Main.js",
                    List.of("node"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("node", sourceFile.getFileName().toString()));
        }
        if (normalized.startsWith("PHP")) {
            return new LanguagePlan(
                    "Main.php",
                    List.of("php"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("php", sourceFile.getFileName().toString()));
        }
        if (normalized.startsWith("Ruby")) {
            return new LanguagePlan(
                    "Main.rb",
                    List.of("ruby"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("ruby", sourceFile.getFileName().toString()));
        }
        if (normalized.startsWith("Perl")) {
            return new LanguagePlan(
                    "Main.pl",
                    List.of("perl"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("perl", sourceFile.getFileName().toString()));
        }
        if (normalized.startsWith("Haskell")) {
            return new LanguagePlan(
                    "Main.hs",
                    List.of("ghc"),
                    false,
                    (sourceFile, workDir) -> List.of("ghc", "-O2", sourceFile.getFileName().toString(), "-o", executableName()),
                    (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
        }
        if (normalized.startsWith("OCaml")) {
            return new LanguagePlan(
                    "Main.ml",
                    List.of("ocamlc"),
                    false,
                    (sourceFile, workDir) -> List.of("ocamlc", "-o", executableName(), sourceFile.getFileName().toString()),
                    (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
        }
        if (normalized.startsWith("Scala")) {
            return new LanguagePlan(
                    "Main.scala",
                    List.of("scalac", "scala"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("scala", "-cp", workDir.toAbsolutePath().toString(), "Main"));
        }
        if (normalized.startsWith("Pascal")) {
            return new LanguagePlan(
                    "Main.pas",
                    List.of("fpc"),
                    false,
                    (sourceFile, workDir) -> List.of("fpc", "-O2", sourceFile.getFileName().toString(), "-o" + executableName()),
                    (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
        }
        return null;
    }

    private LanguagePlan compiledCppPlan(String fileName, String compiler, String stdFlag) {
        return new LanguagePlan(
                fileName,
                List.of(compiler),
                false,
                (sourceFile, workDir) -> List.of(compiler, stdFlag, "-O2", sourceFile.getFileName().toString(), "-o", executableName()),
                (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
    }

    private LanguagePlan compiledCPlan(String fileName) {
        return new LanguagePlan(
                fileName,
                List.of("gcc"),
                false,
                (sourceFile, workDir) -> List.of("gcc", "-std=c11", "-O2", sourceFile.getFileName().toString(), "-o", executableName()),
                (sourceFile, workDir) -> List.of(workDir.resolve(executableName()).toAbsolutePath().toString()));
    }

    private String sourceFileNameArg(String value) {
        return value;
    }

    private String executableName() {
        return isWindows() ? "main.exe" : "main";
    }

    private ProcessResult runProcess(List<String> command, Path workDir, String input, long timeoutMillis) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(false);
        Process process = builder.start();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
        Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }

        long peakMemoryKb = Math.max(0L, readMemoryUsageKb(process.pid()));
        long start = System.nanoTime();
        boolean timedOut = false;
        while (true) {
            if (process.waitFor(50, TimeUnit.MILLISECONDS)) {
                break;
            }
            peakMemoryKb = Math.max(peakMemoryKb, readMemoryUsageKb(process.pid()));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (elapsedMillis >= timeoutMillis) {
                timedOut = true;
                process.destroyForcibly();
                process.waitFor(250, TimeUnit.MILLISECONDS);
                break;
            }
        }

        String stdout = readFuture(stdoutFuture);
        String stderr = readFuture(stderrFuture);
        executor.shutdownNow();

        int exitCode;
        if (timedOut) {
            exitCode = -1;
        } else {
            exitCode = process.exitValue();
        }

        return new ProcessResult(exitCode, stdout, stderr, peakMemoryKb, timedOut, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private String readFuture(Future<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private long readMemoryUsageKb(long pid) {
        try {
            if (isWindows()) {
                Process process = new ProcessBuilder("cmd", "/c", "tasklist /fi \"PID eq " + pid + "\" /fo csv /nh").start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                process.waitFor(2, TimeUnit.SECONDS);
                if (output.isBlank() || output.contains("No tasks are running")) {
                    return -1L;
                }
                String[] lines = output.split("\\R");
                for (String line : lines) {
                    String[] fields = extractCsvFields(line);
                    if (fields.length >= 5) {
                        String memory = fields[4].replaceAll("[^0-9]", "");
                        if (!memory.isBlank()) {
                            return Long.parseLong(memory);
                        }
                    }
                }
                return -1L;
            }

            Process process = new ProcessBuilder("sh", "-lc", "ps -o rss= -p " + pid).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            process.waitFor(2, TimeUnit.SECONDS);
            if (output.isBlank()) {
                return -1L;
            }
            return Long.parseLong(output.split("\\s+")[0]);
        } catch (Exception e) {
            return -1L;
        }
    }

    private String[] extractCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                insideQuotes = !insideQuotes;
                continue;
            }
            if (ch == ',' && !insideQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields.toArray(String[]::new);
    }

    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder builder;
            if (isWindows()) {
                builder = new ProcessBuilder("cmd", "/c", "where " + command);
            } else {
                builder = new ProcessBuilder("sh", "-lc", "command -v " + command);
            }
            Process process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String resolvePythonCommand() {
        if (isCommandAvailable("py")) {
            return "py";
        }
        if (isCommandAvailable("python3")) {
            return "python3";
        }
        return "python";
    }

    private String normalizeOutput(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            builder.append(rtrim(lines[i]));
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
        return rtrim(builder.toString());
    }

    private String rtrim(String text) {
        int end = text.length();
        while (end > 0) {
            char ch = text.charAt(end - 1);
            if (ch == ' ' || ch == '\t') {
                end--;
                continue;
            }
            break;
        }
        return text.substring(0, end);
    }

    private String joinNonBlank(String first, String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "\n" + second;
    }

    private String formatExpectedActual(String expected, String actual) {
        return "Expected:\n" + expected + "\n\nActual:\n" + actual;
    }

    private void deleteRecursively(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return;
            }
            Files.walk(path)
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

        private record LanguagePlan(
            String sourceFileName,
            List<String> requiredCommands,
            boolean interpreted,
            ProcessCommand compileCommand,
            ProcessCommand runCommand) {
    }

    private record ProcessResult(int exitCode, String stdout, String stderr, long peakMemoryKb, boolean timedOut, long durationMillis) {
    }

    record LanguageSupport(boolean supported, String message) {
    }

    record TestCaseSpec(String input, String expectedOutput, boolean custom, boolean expectedOutputProvided, String displayName) {
    }

    record TestCaseResult(int index, String displayName, boolean passed, boolean timedOut, boolean unknown, long durationMillis, long peakMemoryKb,
                          String input, String expectedOutput, String actualOutput, String stderrOutput, String details) {
    }

    record ExecutionReport(boolean success, String compileLog, List<TestCaseResult> results, String failureMessage) {
        static ExecutionReport success(List<TestCaseResult> results, String compileLog) {
            return new ExecutionReport(true, compileLog == null ? "" : compileLog, results, "");
        }

        static ExecutionReport failure(String failureMessage) {
            return new ExecutionReport(false, "", List.of(), failureMessage == null ? "" : failureMessage);
        }
    }

    @FunctionalInterface
    private interface ProcessCommand {
        List<String> create(Path sourceFile, Path workDir);
    }
}
