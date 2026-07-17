package com.example;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight metadata for native problem presentation. */
final class ProblemStatementMetadata {

    private static final Pattern TIME_LIMIT_PATTERN = Pattern.compile(
            "(\\d+)\\s*seconds?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEMORY_LIMIT_PATTERN = Pattern.compile(
            "(\\d+)\\s*(?:mb|megabytes|kb|kilobytes)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IO_MODE_PATTERN = Pattern.compile(
            "(standard|file)\\s*input.*?(standard|file)\\s*output",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    final String problemCode;
    final String title;
    final String timeLimit;
    final String memoryLimit;
    final String ioMode;
    final List<String> sheetNames;
    final List<String> sheetUrls;
    final String difficulty;
    final List<String> tags;

    private ProblemStatementMetadata(
            String problemCode,
            String title,
            String timeLimit,
            String memoryLimit,
            String ioMode,
            List<String> sheetNames,
            List<String> sheetUrls,
            String difficulty,
            List<String> tags) {
        this.problemCode = problemCode;
        this.title = title;
        this.timeLimit = timeLimit;
        this.memoryLimit = memoryLimit;
        this.ioMode = ioMode;
        this.sheetNames = sheetNames;
        this.sheetUrls = sheetUrls;
        this.difficulty = difficulty;
        this.tags = tags;
    }

    static ProblemStatementMetadata from(
            ProblemDetails details, RenderedProblemView full, List<SheetInfo> sheets, String contestId) {
        Document document = org.jsoup.Jsoup.parseBodyFragment(full.html());
        Element root = document.body();
        String title = firstText(root, "div.header div.title");
        String timeLimit = firstMatchingText(root, "div.header div.time-limit", TIME_LIMIT_PATTERN);
        String memoryLimit = firstMatchingText(root, "div.header div.memory-limit", MEMORY_LIMIT_PATTERN);
        String ioMode = firstIoMode(root);
        List<String> tagList = textList(root, "div.header div.tags a[href*=problemTag]");
        String difficulty = textOf(root, "div.header div.difficulty");
        return new ProblemStatementMetadata(
                details != null ? details.code() : (contestId == null ? "" : contestId),
                title,
                timeLimit,
                memoryLimit,
                ioMode,
                sheetNames(sheets),
                sheetUrls(sheets),
                difficulty,
                tagList);
    }

    private static String firstText(Element root, String selector) {
        if (root == null) {
            return "";
        }
        Elements matches = root.select(selector);
        return matches.isEmpty() ? "" : matches.first().text().trim();
    }

    private static String firstMatchingText(Element root, String selector, Pattern pattern) {
        String text = firstText(root, selector);
        if (text.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return text;
        }
        String value = matcher.group(1);
        String suffix = matcher.group().substring(value.length()).trim();
        return value + " " + suffix;
    }

    private static String firstIoMode(Element root) {
        if (root == null) {
            return "";
        }
        Element header = root.selectFirst("div.header");
        if (header == null) {
            return "";
        }
        String headerText = header.text();
        Matcher matcher = IO_MODE_PATTERN.matcher(headerText);
        return matcher.find() ? matcher.group().trim() : "";
    }

    private static List<String> textList(Element root, String selector) {
        if (root == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Element element : root.select(selector)) {
            String value = element.text().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String textOf(Element root, String selector) {
        return firstText(root, selector);
    }

    private static List<String> sheetNames(List<SheetInfo> sheets) {
        if (sheets == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (SheetInfo sheet : sheets) {
            if (sheet.name() != null) {
                names.add(sheet.name());
            }
        }
        return names;
    }

    private static List<String> sheetUrls(List<SheetInfo> sheets) {
        if (sheets == null) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (SheetInfo sheet : sheets) {
            if (sheet.url() != null) {
                urls.add(sheet.url());
            }
        }
        return urls;
    }
}
