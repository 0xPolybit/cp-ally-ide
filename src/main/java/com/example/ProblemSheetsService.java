package com.example;

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
    private static final Pattern SHEET_ENTRY = Pattern.compile(
            "\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpService http = new HttpService();
    private final Map<String, List<SheetInfo>> cache =
            Collections.synchronizedMap(new HashMap<>());

    List<SheetInfo> fetchSheets(String problemCode) {
        if (cache.containsKey(problemCode)) return cache.get(problemCode);
        List<SheetInfo> result = List.of();
        try {
            result = parseSheets(http.getJson(API_BASE + java.net.URLEncoder.encode(
                    problemCode, java.nio.charset.StandardCharsets.UTF_8), TIMEOUT_MS));
        } catch (Exception e) {
            DiagnosticLogger.warn("[ProblemSheetsService] Could not fetch sheets for "
                    + problemCode + ": " + e.getMessage());
        }
        cache.put(problemCode, result);
        return result;
    }

    List<SheetInfo> getCached(String problemCode) {
        return cache.getOrDefault(problemCode, List.of());
    }

    private static List<SheetInfo> parseSheets(String json) {
        if (json == null) return List.of();
        int sheetsIdx = json.indexOf("\"sheets\"");
        if (sheetsIdx < 0) return List.of();
        int arrStart = json.indexOf('[', sheetsIdx);
        if (arrStart < 0) return List.of();
        int depth = 0, arrEnd = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']' && --depth == 0) { arrEnd = i; break; }
        }
        if (arrEnd <= arrStart) return List.of();
        Matcher m = SHEET_ENTRY.matcher(json.substring(arrStart + 1, arrEnd));
        List<SheetInfo> result = new ArrayList<>();
        while (m.find()) result.add(new SheetInfo(m.group(1), m.group(2)));
        return List.copyOf(result);
    }
}
