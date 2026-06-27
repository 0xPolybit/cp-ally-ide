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

class ProblemSheetsService {

    private static final String API_BASE = "https://cp-ally-ide.vercel.app/api/problem-sheets?code=";
    private static final int TIMEOUT_MS = 6000;

    // Matches {"name":"...","url":"..."} entries inside the sheets array.
    private static final Pattern SHEET_ENTRY = Pattern.compile(
            "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final Map<String, List<SheetInfo>> cache =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Fetches which practice sheets contain {@code problemCode} (e.g. "2208A").
     * Results are cached in-memory for the lifetime of the service instance.
     * Returns an empty list on any error so callers never need to handle exceptions.
     */
    List<SheetInfo> fetchSheets(String problemCode) {
        if (cache.containsKey(problemCode)) {
            return cache.get(problemCode);
        }
        List<SheetInfo> result = List.of();
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(API_BASE + problemCode).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    result = parseSheets(json);
                }
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[ProblemSheetsService] Could not fetch sheets for "
                    + problemCode + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        cache.put(problemCode, result);
        return result;
    }

    /**
     * Returns cached sheets without making a network call.
     * Safe to call on the EDT (e.g. during zoom re-render).
     */
    List<SheetInfo> getCached(String problemCode) {
        return cache.getOrDefault(problemCode, List.of());
    }

    private static List<SheetInfo> parseSheets(String json) {
        int sheetsIdx = json.indexOf("\"sheets\"");
        if (sheetsIdx < 0) return List.of();
        int arrStart = json.indexOf('[', sheetsIdx);
        if (arrStart < 0) return List.of();
        int depth = 0;
        int arrEnd = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                if (--depth == 0) { arrEnd = i; break; }
            }
        }
        if (arrEnd <= arrStart) return List.of();

        String arr = json.substring(arrStart + 1, arrEnd);
        List<SheetInfo> result = new ArrayList<>();
        Matcher m = SHEET_ENTRY.matcher(arr);
        while (m.find()) {
            result.add(new SheetInfo(m.group(1), m.group(2)));
        }
        return List.copyOf(result);
    }
}
