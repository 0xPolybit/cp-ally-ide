package com.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;

class CodeforcesProfileService {

    private static final String USER_INFO_URL_TEMPLATE =
            "https://codeforces.com/api/user.info?handles=%s";
    private static final String USER_STATUS_URL_TEMPLATE =
            "https://codeforces.com/api/user.status?handle=%s&from=%d&count=%d";
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 10;
    private static final int TIMEOUT_MS = 12000;
    private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;
    private static final long PROFILE_TTL_MILLIS = 10 * 60 * 1000L;
    private static final long AVATAR_TTL_MILLIS = 60 * 60 * 1000L;

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private final HttpService http = new HttpService();
    private final Map<String, TimedValue<UserProfile>> profileCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, TimedValue<BufferedImage>> avatarCache = new java.util.concurrent.ConcurrentHashMap<>();

    UserProfile fetchProfile(String handle) throws Exception {
        String normalizedHandle = handle == null ? "" : handle.trim().toLowerCase(java.util.Locale.ROOT);
        TimedValue<UserProfile> cached = profileCache.get(normalizedHandle);
        if (cached != null && !cached.expired(PROFILE_TTL_MILLIS)) return cached.value();
        String encodedHandle = URLEncoder.encode(handle, StandardCharsets.UTF_8);
        String infoJson = httpGet(String.format(USER_INFO_URL_TEMPLATE, encodedHandle),
                "codeforces user.info");
        if (infoJson == null) {
            throw new IOException("No response from Codeforces API");
        }
        ApiStatus status = readApiStatus(infoJson);
        if ("FAILED".equals(status.status)) {
            throw new IOException(status.comment.isEmpty() ? "Handle not found: " + handle : status.comment);
        }
        UserInfo info = parseFirstUserInfo(infoJson);
        if (info == null) {
            throw new IOException("Unexpected user.info response format");
        }

        SubmissionStats stats = new SubmissionStats();
        try {
            // Page through user.status, accumulating solved-problem and
            // streak statistics. This is much cheaper than asking for
            // count=10000 in a single request, both in terms of memory
            // and of network bytes.
            for (int page = 0; page < MAX_PAGES; page++) {
                int from = 1 + page * PAGE_SIZE;
                String statusJson = httpGet(String.format(USER_STATUS_URL_TEMPLATE,
                        encodedHandle, from, PAGE_SIZE), "codeforces user.status");
                if (statusJson == null) {
                    break;
                }
                ApiStatus pageStatus = readApiStatus(statusJson);
                if ("FAILED".equals(pageStatus.status)) {
                    break;
                }
                int processedBefore = stats.totalSubmissions;
                scanSubmissionPage(statusJson, stats);
                if (stats.totalSubmissions - processedBefore < PAGE_SIZE) {
                    break; // last page
                }
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesProfileService] Status fetch failed: " + e.getMessage());
        }

        UserProfile profile = new UserProfile(handle, info.rank, info.maxRank, info.rating, info.maxRating,
                info.country, info.organization, info.registrationTimeSeconds, info.lastOnlineTimeSeconds,
                info.avatar, stats.problemsSolved, stats.currentStreak, stats.longestStreak,
                stats.totalSubmissions);
        profileCache.put(normalizedHandle, new TimedValue<>(profile));
        return profile;
    }

    BufferedImage fetchAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return null;
        TimedValue<BufferedImage> cached = avatarCache.get(avatarUrl);
        if (cached != null && !cached.expired(AVATAR_TTL_MILLIS)) return cached.value();
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
                    BufferedImage image = ImageIO.read(in);
                    if (image != null) avatarCache.put(avatarUrl, new TimedValue<>(image));
                    return image;
                }
            }
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesProfileService] Avatar fetch failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    void clearCache() {
        profileCache.clear();
        avatarCache.clear();
    }

    private record TimedValue<T>(T value, long createdAt) {
        TimedValue(T value) { this(value, System.currentTimeMillis()); }
        boolean expired(long ttl) { return System.currentTimeMillis() - createdAt > ttl; }
    }

    /**
     * Streams one user.status page and accumulates per-user statistics: total
     * submission count, distinct solved problems, and AC date set for streak
     * computation. The full UserProfile object is built from these counters
     * plus the user.info response.
     */
    /**
     * Visible for testing. Streams one user.status page into the supplied
     * {@link SubmissionStats} accumulator.
     */
    static void scanSubmissionPage(String json, SubmissionStats stats) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            boolean inResultArray = false;
            int objectDepth = 0;
            String currentVerdict = null;
            int currentContestId = Integer.MIN_VALUE;
            String currentIndex = null;
            long currentCreation = 0L;
            while (parser.nextToken() != null) {
                JsonToken token = parser.currentToken();
                if (token == JsonToken.START_ARRAY && "result".equals(parser.currentName())) {
                    inResultArray = true;
                    continue;
                }
                if (!inResultArray) continue;
                if (token == JsonToken.END_ARRAY) {
                    break;
                }
                if (token == JsonToken.START_OBJECT) {
                    objectDepth++;
                    currentVerdict = null;
                    currentContestId = Integer.MIN_VALUE;
                    currentIndex = null;
                    currentCreation = 0L;
                    continue;
                }
                if (token == JsonToken.END_OBJECT) {
                    objectDepth--;
                    if (objectDepth == 0) {
                        stats.totalSubmissions++;
                        if ("OK".equals(currentVerdict)
                                && currentContestId != Integer.MIN_VALUE
                                && currentIndex != null) {
                            stats.solvedProblems.add(currentContestId + "_" + currentIndex);
                            if (currentCreation > 0) {
                                stats.acDates.add(Instant.ofEpochSecond(currentCreation)
                                        .atZone(ZoneOffset.UTC).toLocalDate());
                            }
                        }
                    }
                    continue;
                }
                if (token == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    JsonToken value = parser.nextToken();
                    if ("verdict".equals(name) && value == JsonToken.VALUE_STRING) {
                        currentVerdict = parser.getValueAsString();
                    } else if ("contestId".equals(name) && value == JsonToken.VALUE_NUMBER_INT) {
                        currentContestId = parser.getValueAsInt();
                    } else if ("index".equals(name) && value == JsonToken.VALUE_STRING) {
                        currentIndex = parser.getValueAsString();
                    } else if ("creationTimeSeconds".equals(name) && value == JsonToken.VALUE_NUMBER_INT) {
                        currentCreation = parser.getValueAsLong();
                    }
                }
            }
        }
        stats.problemsSolved = stats.solvedProblems.size();
        int[] streaks = computeStreaks(stats.acDates);
        stats.currentStreak = streaks[0];
        stats.longestStreak = streaks[1];
    }

    private static int[] computeStreaks(TreeSet<LocalDate> acDates) {
        if (acDates.isEmpty()) return new int[]{0, 0};
        List<LocalDate> sorted = new ArrayList<>(acDates);

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

    private static ApiStatus readApiStatus(String json) {
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    JsonToken value = parser.nextToken();
                    if ("status".equals(name) && value == JsonToken.VALUE_STRING) {
                        String s = parser.getValueAsString();
                        parser.nextToken(); // skip "comment" key
                        if (parser.currentToken() == JsonToken.FIELD_NAME
                                && "comment".equals(parser.currentName())) {
                            parser.nextToken();
                            String comment = parser.getValueAsString();
                            return new ApiStatus(s, comment == null ? "" : comment);
                        }
                        return new ApiStatus(s, "");
                    }
                }
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[CodeforcesProfileService] Failed to read API status: " + ioe.getMessage());
        }
        return new ApiStatus("", "");
    }

    private static UserInfo parseFirstUserInfo(String json) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            boolean inResultArray = false;
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.START_ARRAY && "result".equals(parser.currentName())) {
                    inResultArray = true;
                    continue;
                }
                if (!inResultArray) continue;
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    return readUserInfoObject(parser);
                }
                if (parser.currentToken() == JsonToken.END_ARRAY) {
                    return null;
                }
            }
        }
        return null;
    }

    private static UserInfo readUserInfoObject(JsonParser parser) throws IOException {
        UserInfo info = new UserInfo();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.FIELD_NAME) continue;
            String name = parser.currentName();
            JsonToken value = parser.nextToken();
            if (value == JsonToken.VALUE_STRING) {
                String s = parser.getValueAsString();
                switch (name) {
                    case "rank" -> info.rank = s;
                    case "maxRank" -> info.maxRank = s;
                    case "country" -> info.country = s;
                    case "organization" -> info.organization = s;
                    case "avatar" -> info.avatar = s;
                    default -> { /* ignore unknown field */ }
                }
            } else if (value == JsonToken.VALUE_NUMBER_INT) {
                long n = parser.getValueAsLong();
                switch (name) {
                    case "rating" -> info.rating = (int) n;
                    case "maxRating" -> info.maxRating = (int) n;
                    case "registrationTimeSeconds" -> info.registrationTimeSeconds = n;
                    case "lastOnlineTimeSeconds" -> info.lastOnlineTimeSeconds = n;
                    default -> { /* ignore */ }
                }
            }
            // Other value types (booleans, null, nested objects/arrays) are
            // silently skipped — we only need the fields listed above.
        }
        return info;
    }

    private String httpGet(String urlStr, String source) throws IOException {
        try {
            return http.get(urlStr, TIMEOUT_MS, MAX_BODY_BYTES,
                    Map.of("Accept", "application/json"));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + source, ie);
        }
    }

    /** Result of a Codeforces API call. */
    private static final class ApiStatus {
        final String status;
        final String comment;
        ApiStatus(String status, String comment) {
            this.status = status;
            this.comment = comment;
        }
    }

    /** Subset of user.info that the profile dialog actually uses. */
    private static final class UserInfo {
        String rank = "";
        String maxRank = "";
        String country = "";
        String organization = "";
        String avatar = "";
        int rating;
        int maxRating;
        long registrationTimeSeconds;
        long lastOnlineTimeSeconds;
    }

    /** Mutable accumulator for streaming stats from paged user.status calls. Visible for tests. */
    static final class SubmissionStats {
        int problemsSolved;
        int totalSubmissions;
        int currentStreak;
        int longestStreak;
        final Set<String> solvedProblems = new HashSet<>();
        final TreeSet<LocalDate> acDates = new TreeSet<>();
    }
}
