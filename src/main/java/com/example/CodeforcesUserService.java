package com.example;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CodeforcesUserService {

    private static final String API_BASE =
            "https://codeforces.com/api/user.status?handle=%s&from=1&count=1000";
    private static final int TIMEOUT_MS = 7000;

    // Splits "2208A" → group 1 = "2208", group 2 = "A"
    // Handles multi-letter indices like "B1", "C2"
    private static final Pattern PROBLEM_CODE_PATTERN =
            Pattern.compile("^(\\d+)([A-Za-z]+\\d*)$");

    private static final Pattern VERDICT_PATTERN =
            Pattern.compile("\"verdict\"\\s*:\\s*\"([^\"]+)\"");

    // Session-level cache: "handle|CODE" → display string (empty = no submissions)
    private final Map<String, String> cache =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Returns the best display verdict for the given handle + problem code.
     * Returns "Accepted", "Wrong Answer", "Error", or "" (never attempted / unknown).
     * Blocks on network — always call from a background thread.
     */
    String fetchBestVerdictDisplay(String handle, String problemCode) {
        String key = handle + "|" + problemCode.toUpperCase();
        if (cache.containsKey(key)) return cache.get(key);

        Matcher cm = PROBLEM_CODE_PATTERN.matcher(problemCode.toUpperCase());
        if (!cm.matches()) return "";

        int contestId = Integer.parseInt(cm.group(1));
        String index = cm.group(2);

        String result = "";
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(String.format(API_BASE,
                    java.net.URLEncoder.encode(handle, StandardCharsets.UTF_8))).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    result = parseBestVerdict(json, contestId, index);
                }
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesUserService] Could not fetch user status: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        cache.put(key, result);
        return result;
    }

    /** Clears the in-memory verdict cache (call when the stored handle changes). */
    void clearCache() {
        cache.clear();
    }

    private String parseBestVerdict(String json, int contestId, String index) {
        // Locate the "result" array
        int resultIdx = json.indexOf("\"result\"");
        if (resultIdx < 0) return "";
        int arrStart = json.indexOf('[', resultIdx);
        if (arrStart < 0) return "";

        String latestOther = null;
        int i = arrStart + 1;

        while (i < json.length()) {
            // Skip whitespace and commas between objects
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) <= ' ')) i++;
            if (i >= json.length() || json.charAt(i) == ']') break;
            if (json.charAt(i) != '{') { i++; continue; }

            // Extract one complete submission object using bracket-depth counting
            int depth = 0, objEnd = -1;
            for (int j = i; j < json.length(); j++) {
                char c = json.charAt(j);
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) { objEnd = j; break; }
            }
            if (objEnd < 0) break;

            String obj = json.substring(i, objEnd + 1);
            i = objEnd + 1;

            // Check contestId and problem index
            if (obj.contains("\"contestId\":" + contestId)
                    && obj.contains("\"index\":\"" + index + "\"")) {
                Matcher vm = VERDICT_PATTERN.matcher(obj);
                if (vm.find()) {
                    String v = vm.group(1);
                    if ("OK".equals(v)) return "Accepted"; // short-circuit on first AC
                    if (latestOther == null) latestOther = toDisplayVerdict(v);
                }
            }
        }

        return latestOther != null ? latestOther : "";
    }

    private static String toDisplayVerdict(String cfVerdict) {
        return switch (cfVerdict) {
            case "WRONG_ANSWER", "PARTIAL" -> "Wrong Answer";
            default -> "Error"; // RE, TLE, MLE, CE, SKIPPED, etc.
        };
    }
}
