package com.example;

/**
 * Whitespace-aware first-difference finder for the execution results
 * dialog. Both inputs are normalized to LF and trailing whitespace is
 * trimmed per line before comparison, which matches the rules used by
 * {@link CodeExecutionService#normalizeOutput}.
 */
final class DiffSummary {

    private DiffSummary() {
    }

    /**
     * Returns a short human-readable description of the first line
     * where {@code actual} and {@code expected} differ after
     * normalization. If the strings are equal after normalization,
     * returns an empty string. The summary is bounded to a single
     * line and at most {@code maxSnippetChars} characters of context
     * on each side.
     */
    static String firstDifference(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        String[] actualLines = normalize(actual);
        String[] expectedLines = normalize(expected);
        int n = Math.min(actualLines.length, expectedLines.length);
        for (int i = 0; i < n; i++) {
            if (!actualLines[i].equals(expectedLines[i])) {
                return describe(i, actualLines[i], expectedLines[i]);
            }
        }
        if (actualLines.length != expectedLines.length) {
            int first = n;
            String a = first < actualLines.length ? actualLines[first] : "<end of output>";
            String e = first < expectedLines.length ? expectedLines[first] : "<end of output>";
            return describe(first, a, e);
        }
        return "";
    }

    private static String describe(int line, String actual, String expected) {
        int snippet = 40;
        String a = trim(actual, snippet);
        String e = trim(expected, snippet);
        return "line " + (line + 1) + ": expected \u201c" + e + "\u201d, got \u201c" + a + "\u201d";
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "\u2026";
    }

    private static String[] normalize(String text) {
        if (text == null) return new String[0];
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = rtrim(lines[i]);
        }
        // Drop a single trailing empty line that came from a final \n.
        if (lines.length > 1 && lines[lines.length - 1].isEmpty()) {
            String[] trimmed = new String[lines.length - 1];
            System.arraycopy(lines, 0, trimmed, 0, trimmed.length);
            lines = trimmed;
        }
        return lines;
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0) {
            char ch = s.charAt(end - 1);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
                end--;
                continue;
            }
            break;
        }
        return s.substring(0, end);
    }
}
