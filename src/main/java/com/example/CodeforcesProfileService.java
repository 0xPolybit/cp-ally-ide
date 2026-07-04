package com.example;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

class CodeforcesProfileService {

    private static final String USER_INFO_URL   = "https://codeforces.com/api/user.info?handles=%s";
    private static final String USER_STATUS_URL = "https://codeforces.com/api/user.status?handle=%s&from=1&count=10000";
    private static final int    TIMEOUT_MS      = 12000;

    private static final Pattern STRING_FIELD    = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NUMBER_FIELD    = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+)");
    private static final Pattern VERDICT_PATTERN = Pattern.compile("\"verdict\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CONTEST_ID_PAT  = Pattern.compile("\"contestId\"\\s*:\\s*(\\d+)");
    private static final Pattern INDEX_PAT       = Pattern.compile("\"index\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CREATION_PAT    = Pattern.compile("\"creationTimeSeconds\"\\s*:\\s*(\\d+)");

    UserProfile fetchProfile(String handle) throws Exception {
        String infoJson = httpGet(String.format(USER_INFO_URL, handle));
        if (infoJson == null)
            throw new IOException("No response from Codeforces API");
        if (infoJson.contains("\"status\":\"FAILED\"")) {
            String comment = extractFirstString(infoJson, "comment");
            throw new IOException(comment.isEmpty() ? "Handle not found: " + handle : comment);
        }

        // Extract first object from "result" array
        int resultIdx = infoJson.indexOf("\"result\"");
        int arrStart  = infoJson.indexOf('[', Math.max(resultIdx, 0));
        int objStart  = infoJson.indexOf('{', Math.max(arrStart, 0));
        int objEnd    = findObjectEnd(infoJson, objStart);
        if (objStart < 0 || objEnd < 0)
            throw new IOException("Unexpected API response format");
        String resultObj = infoJson.substring(objStart, objEnd + 1);

        Map<String, String> strFields = new HashMap<>();
        Matcher sm = STRING_FIELD.matcher(resultObj);
        while (sm.find()) strFields.put(sm.group(1), sm.group(2));

        Map<String, Long> numFields = new HashMap<>();
        Matcher nm = NUMBER_FIELD.matcher(resultObj);
        while (nm.find()) {
            try { numFields.put(nm.group(1), Long.parseLong(nm.group(2))); }
            catch (NumberFormatException ignored) {}
        }

        String rankStr      = strFields.getOrDefault("rank", "");
        String maxRankStr   = strFields.getOrDefault("maxRank", "");
        int    rating       = numFields.getOrDefault("rating", 0L).intValue();
        int    maxRating    = numFields.getOrDefault("maxRating", 0L).intValue();
        String country      = strFields.getOrDefault("country", "");
        String organization = strFields.getOrDefault("organization", "");
        long   regTime      = numFields.getOrDefault("registrationTimeSeconds", 0L);
        long   lastOnline   = numFields.getOrDefault("lastOnlineTimeSeconds", 0L);
        String avatar       = strFields.getOrDefault("avatar", "");

        int problemsSolved = 0, totalSubmissions = 0, currentStreak = 0, longestStreak = 0;
        try {
            String statusJson = httpGet(String.format(USER_STATUS_URL, handle));
            if (statusJson != null && !statusJson.contains("\"status\":\"FAILED\"")) {
                int[] stats  = parseSubmissionStats(statusJson);
                problemsSolved   = stats[0];
                totalSubmissions = stats[1];
                currentStreak    = stats[2];
                longestStreak    = stats[3];
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesProfileService] Status fetch failed: " + e.getMessage());
        }

        return new UserProfile(handle, rankStr, maxRankStr, rating, maxRating,
                country, organization, regTime, lastOnline, avatar,
                problemsSolved, currentStreak, longestStreak, totalSubmissions);
    }

    BufferedImage fetchAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return null;
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(avatarUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "cp-ally-ide");
            if (conn.getResponseCode() == 200) {
                try (InputStream in = conn.getInputStream()) {
                    return ImageIO.read(in);
                }
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesProfileService] Avatar fetch failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private int[] parseSubmissionStats(String json) {
        Set<String> solvedProblems = new HashSet<>();
        TreeSet<LocalDate> acDates = new TreeSet<>();
        int totalSubmissions = 0;

        int resultIdx = json.indexOf("\"result\"");
        if (resultIdx < 0) return new int[]{0, 0, 0, 0};
        int arrStart = json.indexOf('[', resultIdx);
        if (arrStart < 0) return new int[]{0, 0, 0, 0};

        int i = arrStart + 1;
        while (i < json.length()) {
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) <= ' ')) i++;
            if (i >= json.length() || json.charAt(i) == ']') break;
            if (json.charAt(i) != '{') { i++; continue; }

            int objEnd = findObjectEnd(json, i);
            if (objEnd < 0) break;

            String obj = json.substring(i, objEnd + 1);
            i = objEnd + 1;
            totalSubmissions++;

            Matcher vm = VERDICT_PATTERN.matcher(obj);
            if (vm.find() && "OK".equals(vm.group(1))) {
                Matcher cidm = CONTEST_ID_PAT.matcher(obj);
                Matcher idxm = INDEX_PAT.matcher(obj);
                if (cidm.find() && idxm.find()) {
                    solvedProblems.add(cidm.group(1) + "_" + idxm.group(1));
                }
                Matcher ctm = CREATION_PAT.matcher(obj);
                if (ctm.find()) {
                    long ts = Long.parseLong(ctm.group(1));
                    acDates.add(Instant.ofEpochSecond(ts).atZone(ZoneOffset.UTC).toLocalDate());
                }
            }
        }

        int[] streaks = computeStreaks(acDates);
        return new int[]{solvedProblems.size(), totalSubmissions, streaks[0], streaks[1]};
    }

    private int[] computeStreaks(TreeSet<LocalDate> acDates) {
        if (acDates.isEmpty()) return new int[]{0, 0};

        List<LocalDate> sorted = new ArrayList<>(acDates); // TreeSet is already sorted

        int longest = 1, run = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                if (++run > longest) longest = run;
            } else {
                run = 1;
            }
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate check = acDates.contains(today) ? today : today.minusDays(1);
        int current = 0;
        while (acDates.contains(check)) {
            current++;
            check = check.minusDays(1);
        }

        return new int[]{current, longest};
    }

    private static int findObjectEnd(String json, int start) {
        if (start < 0 || start >= json.length() || json.charAt(start) != '{') return -1;
        int depth = 0;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return j;
        }
        return -1;
    }

    private static String extractFirstString(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            InputStream in = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) return null;
            try (in) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
