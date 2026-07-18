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
import java.util.concurrent.atomic.AtomicLong;

class CodeExecutionService {

    private static final long COMPILE_TIMEOUT_MILLIS = 60_000L;
    private static final long RUN_TIMEOUT_MILLIS = 2_000L;

    // Toolchain lookups spawn `where`/`command -v` subprocesses (up to 3s each)
    // and are triggered from the EDT on every language change — memoize them.
    // Installed toolchains rarely change while the app is running.
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> commandAvailabilityCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String cachedPythonCommand;

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

    String getDetailedSupportInfo(String language) {
        LanguagePlan plan = resolvePlan(language);
        if (plan == null) {
            return "Language: " + language + "\n\nStatus: No local runner is configured for this language.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Language: ").append(language).append("\n\n");
        sb.append("Required Tools:\n");

        boolean allFound = true;
        for (String command : plan.requiredCommands()) {
            String path = getCommandPath(command);
            if (path != null && !path.isBlank()) {
                sb.append("  ✓ ").append(command).append(": ").append(path).append("\n");
            } else {
                sb.append("  ✗ ").append(command).append(": NOT FOUND\n");
                allFound = false;
            }
        }

        sb.append("\nStatus: ");
        if (allFound) {
            sb.append("Ready to execute locally.");
        } else {
            sb.append("Missing required tools.");
        }

        return sb.toString();
    }

    private String getCommandPath(String command) {
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
                return null;
            }
            if (process.exitValue() == 0) {
                String output = readStream(process.getInputStream());
                if (output == null || output.isBlank()) {
                    return null;
                }

                for (String line : output.split("\\R")) {
                    String candidate = line.trim();
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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
                boolean yesNoExpected = hasExpectedOutput && isYesNoOnlyOutput(expected);
                boolean passed = !runResult.timedOut()
                        && runResult.exitCode() == 0
                        && hasExpectedOutput
                        && (yesNoExpected ? expected.equalsIgnoreCase(actual) : expected.equals(actual));
                boolean unknown = !runResult.timedOut() && runResult.exitCode() == 0 && !hasExpectedOutput;
                boolean yesNoCaseInsensitive = yesNoExpected && !actual.equals(actual.toUpperCase(Locale.ROOT));

                String details;
                if (runResult.timedOut()) {
                    details = "Time limit exceeded.";
                } else if (runResult.exitCode() != 0) {
                    details = joinNonBlank("Runtime error.", runResult.stderr());
                } else if (unknown) {
                    details = "Expected output not provided.";
                } else if (passed) {
                    details = yesNoExpected
                            ? "Output matched expected result using case-insensitive YES/NO comparison."
                            : "Output matched expected result.";
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
                        details,
                        yesNoCaseInsensitive ? "All caps is recommended for YES/NO outputs." : ""));
            }

            return ExecutionReport.success(results, compileLog);
        } finally {
            deleteRecursively(workDir);
        }
    }

    ExecutionReport runProgramOnce(String language, String sourceCode) throws IOException, InterruptedException {
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

        Path workDir = Files.createTempDirectory("cpa-run-");
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

            ProcessResult runResult = runProcess(plan.runCommand().create(sourceFile, workDir), workDir, "", RUN_TIMEOUT_MILLIS);
            String details;
            boolean passed = !runResult.timedOut() && runResult.exitCode() == 0;
            if (runResult.timedOut()) {
                details = "Time limit exceeded.";
            } else if (runResult.exitCode() != 0) {
                details = joinNonBlank("Runtime error.", runResult.stderr());
            } else {
                details = "Executed with empty input.";
            }

            List<TestCaseResult> results = List.of(new TestCaseResult(
                    1,
                    "Empty input run",
                    passed,
                    runResult.timedOut(),
                    false,
                    runResult.durationMillis(),
                    runResult.peakMemoryKb(),
                    "",
                    "",
                    runResult.stdout(),
                    runResult.stderr(),
                    details,
                    ""));

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
            String pythonCommand = resolvePythonCommand();
            return new LanguagePlan(
                    "Main.py",
                    List.of(pythonCommand),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of(pythonCommand, sourceFile.toString()));
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
            // `scala Main.scala` compiles and runs the file's main object directly;
            // running `scala -cp <dir> Main` would require a separate compile step.
            return new LanguagePlan(
                    "Main.scala",
                    List.of("scala"),
                    true,
                    (sourceFile, workDir) -> List.of(),
                    (sourceFile, workDir) -> List.of("scala", sourceFile.getFileName().toString()));
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

    private String executableName() {
        return isWindows() ? "main.exe" : "main";
    }

    /**
     * Hard cap on the combined stdout+stderr that one process run is allowed
     * to produce. Prevents a child program that prints forever from filling
     * the heap, and bounds the memory we copy out of the OS pipe.
     */
    private static final int MAX_PROCESS_OUTPUT_BYTES = 1 * 1024 * 1024; // 1 MiB

    /**
     * Grace period after {@link Process#destroyForcibly()} before we walk the
     * process tree and force-kill any surviving descendants.
     */
    private static final long PROCESS_TREE_DESTROY_GRACE_MILLIS = 250L;

    private ProcessResult runProcess(List<String> command, Path workDir, String input, long timeoutMillis) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(false);
        Process process = builder.start();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<byte[]> stdoutFuture = executor.submit(() ->
                BoundedStreams.read(process.getInputStream(), MAX_PROCESS_OUTPUT_BYTES, "stdout"));
        Future<byte[]> stderrFuture = executor.submit(() ->
                BoundedStreams.read(process.getErrorStream(), MAX_PROCESS_OUTPUT_BYTES, "stderr"));

        // Feed stdin on its own thread: a large input can exceed the pipe buffer
        // and block, and a program that exits without reading everything causes
        // a broken-pipe IOException — neither should stall or abort the run.
        executor.submit(() -> {
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            } catch (IOException ignored) {
                // Process exited without consuming its input.
            }
            return null;
        });

        // Memory sampler runs on its own daemon thread so OS queries do not
        // inflate the wall-clock time measured in the loop below.
        AtomicLong peakMemoryKb = new AtomicLong(0L);
        long pid = process.pid();
        Thread memSampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long mem = readMemoryUsageKb(pid);
                if (mem > 0) peakMemoryKb.updateAndGet(cur -> Math.max(cur, mem));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "mem-sampler");
        memSampler.setDaemon(true);
        memSampler.start();

        long start = System.nanoTime();
        long endNano = start;
        boolean timedOut = false;
        try {
            while (true) {
                if (process.waitFor(50, TimeUnit.MILLISECONDS)) {
                    endNano = System.nanoTime(); // captured before stdout/stderr drain
                    break;
                }
                if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= timeoutMillis) {
                    timedOut = true;
                    destroyProcessTree(process);
                    process.waitFor(PROCESS_TREE_DESTROY_GRACE_MILLIS, TimeUnit.MILLISECONDS);
                    endNano = System.nanoTime();
                    break;
                }
            }
        } finally {
            // Always stop the sampler and try to drain pipes so we don't leak
            // a daemon thread even if the wait above threw.
            memSampler.interrupt();
        }
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endNano - start);

        byte[] stdoutBytes = readFutureBytes(stdoutFuture);
        byte[] stderrBytes = readFutureBytes(stderrFuture);
        executor.shutdownNow();

        int exitCode = timedOut ? -1 : safeExitValue(process);
        return new ProcessResult(exitCode,
                new String(stdoutBytes, StandardCharsets.UTF_8),
                new String(stderrBytes, StandardCharsets.UTF_8),
                peakMemoryKb.get(), timedOut, durationMillis);
    }

    /**
     * Best-effort destruction of the process and any descendants. Uses
     * {@link ProcessHandle#descendants()} when available (Java 9+), and falls
     * back to destroying only the direct process on older runtimes. The
     * intent is to ensure that killing a compiler or runner does not leave
     * a forked child still consuming CPU and memory.
     */
    private static void destroyProcessTree(Process process) {
        try {
            ProcessHandle handle = process.toHandle();
            if (handle != null) {
                handle.descendants().forEach(child -> {
                    try {
                        child.destroyForcibly();
                    } catch (Exception ignored) {
                        // Best-effort only.
                    }
                });
            }
        } catch (Exception ignored) {
            // Process.toHandle() can throw on some platforms; fall through.
        }
        try {
            process.destroyForcibly();
        } catch (Exception ignored) {
            // Best-effort only.
        }
    }

    private static int safeExitValue(Process process) {
        try {
            return process.exitValue();
        } catch (IllegalThreadStateException stillRunning) {
            return -1;
        }
    }

    private byte[] readFutureBytes(Future<byte[]> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new byte[0];
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

            // Linux/macOS: read /proc/<pid>/status directly — no subprocess needed
            Path statusFile = Path.of("/proc/" + pid + "/status");
            if (Files.notExists(statusFile)) return -1L;
            for (String line : Files.readAllLines(statusFile, StandardCharsets.UTF_8)) {
                if (line.startsWith("VmRSS:")) {
                    // Format: "VmRSS:   12345 kB"
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) return Long.parseLong(parts[1]);
                }
            }
            return -1L;
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
        return commandAvailabilityCache.computeIfAbsent(command, this::probeCommandAvailable);
    }

    private boolean probeCommandAvailable(String command) {
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
        String cached = cachedPythonCommand;
        if (cached != null) {
            return cached;
        }
        String resolved;
        if (isCommandAvailable("py")) {
            resolved = "py";
        } else if (isCommandAvailable("python3")) {
            resolved = "python3";
        } else {
            resolved = "python";
        }
        cachedPythonCommand = resolved;
        return resolved;
    }

    private String normalizeOutput(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        int lastSignificantLine = lines.length - 1;
        while (lastSignificantLine >= 0 && rtrim(lines[lastSignificantLine]).isEmpty()) {
            lastSignificantLine--;
        }

        if (lastSignificantLine < 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= lastSignificantLine; i++) {
            builder.append(rtrim(lines[i]));
            if (i < lastSignificantLine) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private String rtrim(String text) {
        int end = text.length();
        while (end > 0) {
            char ch = text.charAt(end - 1);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
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

    private boolean isYesNoOnlyOutput(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.trim();
        String[] tokens = normalized.split("\\s+");
        boolean sawToken = false;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            sawToken = true;
            String upper = token.toUpperCase(Locale.ROOT);
            if (!"YES".equals(upper) && !"NO".equals(upper)) {
                return false;
            }
        }

        return sawToken;
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
                          String input, String expectedOutput, String actualOutput, String stderrOutput, String details, String note) {
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
