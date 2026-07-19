package com.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded, atomic persistence for recently opened problem codes. */
final class RecentProblemRepository {
    private static final int MAX_ENTRIES = 15;
    private final Path file;
    private final List<RecentProblem> entries = new ArrayList<>();

    RecentProblemRepository(Path appDataDirectory) {
        file = appDataDirectory.resolve("recent-problems.txt");
        load();
    }

    synchronized List<RecentProblem> list() { return List.copyOf(entries); }

    synchronized void record(String code, String title) {
        if (code == null || code.isBlank()) return;
        entries.removeIf(item -> item.code().equalsIgnoreCase(code));
        entries.add(0, new RecentProblem(code.trim(), title == null ? "" : title.trim(), Instant.now().toEpochMilli()));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save();
    }

    synchronized void clear() { entries.clear(); save(); }

    private void load() {
        try {
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\t", -1);
                if (parts.length >= 3) {
                    try { entries.add(new RecentProblem(parts[0], parts[1], Long.parseLong(parts[2]))); }
                    catch (NumberFormatException ignored) { }
                }
            }
            entries.sort(Comparator.comparingLong(RecentProblem::lastOpened).reversed());
            while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        } catch (IOException ignored) { }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            List<String> lines = entries.stream().map(item -> item.code() + "\t"
                    + item.title().replace('\t', ' ') + "\t" + item.lastOpened()).toList();
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            try { Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".tmp")); } catch (IOException ignored) { }
        }
    }

    record RecentProblem(String code, String title, long lastOpened) { }
}
