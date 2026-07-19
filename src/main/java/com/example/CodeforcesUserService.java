package com.example;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CodeforcesUserService {

    private static final String API_BASE_TEMPLATE =
            "https://codeforces.com/api/user.status?handle=%s&from=%d&count=%d";
    // Codeforces allows up to count=1000 per call. We page through older
    // submissions until we either find an Accepted for the requested problem
    // (success) or the contestId drops below the requested one (give up).
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 10; // 10 * 1000 = 10 000 submissions
    private static final int TIMEOUT_MS = 7000;

    // Splits "2208A" → group 1 = "2208", group 2 = "A"
    // Handles multi-letter indices like "B1", "C2"
    private static final Pattern PROBLEM_CODE_PATTERN =
            Pattern.compile("^(\\d+)([A-Za-z]+\\d*)$");

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    // Session-level cache: "handle|CODE" → display string (empty = no submissions)
    private static final long VERDICT_TTL_MILLIS = 5 * 60 * 1000L;
    private final Map<String, TimedValue> cache =
            Collections.synchronizedMap(new HashMap<>());
    private final HttpService http = new HttpService();

    /**
     * Returns the best display verdict for the given handle + problem code.
     * Returns "Accepted", "Wrong Answer", "Error", or "" (never attempted / unknown).
     * Blocks on network — always call from a background thread.
     */
    String fetchBestVerdictDisplay(String handle, String problemCode) {
        String key = handle.trim().toLowerCase(java.util.Locale.ROOT) + "|" + problemCode.toUpperCase();
        TimedValue cached = cache.get(key);
        if (cached != null && !cached.expired()) return cached.value();

        Matcher cm = PROBLEM_CODE_PATTERN.matcher(problemCode.toUpperCase());
        if (!cm.matches()) return "";

        int contestId = Integer.parseInt(cm.group(1));
        String index = cm.group(2);

        String result = "";
        // Page through the user's submissions from most recent to older. We
        // can stop as soon as we find an Accepted verdict for our problem,
        // or when a submission's contestId is older than the requested
        // contest — that means there's no further relevant data to scan.
        for (int page = 0; page < MAX_PAGES; page++) {
            int from = 1 + page * PAGE_SIZE;
            String json = httpGetJson(String.format(API_BASE_TEMPLATE,
                    URLEncoder.encode(handle, StandardCharsets.UTF_8), from, PAGE_SIZE));
            if (json == null) {
                break;
            }
            PageResult pr = scanPage(json, contestId, index);
            if (pr.foundAccepted) {
                result = "Accepted";
                break;
            }
            if (pr.latestOther != null && result.isEmpty()) {
                result = pr.latestOther;
            }
            if (pr.stop) {
                break;
            }
            if (pr.entriesInPage < PAGE_SIZE) {
                // We exhausted the user's submission history.
                break;
            }
        }

        cache.put(key, new TimedValue(result));
        return result;
    }

    private static final class TimedValue {
        private final String value;
        private final long createdAt = System.currentTimeMillis();
        TimedValue(String value) { this.value = value; }
        String value() { return value; }
        boolean expired() { return System.currentTimeMillis() - createdAt > VERDICT_TTL_MILLIS; }
    }

    /** Clears the in-memory verdict cache (call when the stored handle changes). */
    void clearCache() {
        cache.clear();
    }

    /**
     * Streams one page of the user.status response, looking only for entries
     * whose contestId and index match {@code contestId}/{@code index}.
     * Returns the most recent non-OK verdict seen (if any) and whether to
     * stop paging. Package-private for testing.
     */
    static PageResult scanPage(String json, int contestId, String index) {
        PageResult result = new PageResult();
        int minContestIdSeen = Integer.MAX_VALUE;
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            boolean inResultArray = false;
            int objectDepth = 0;
            String currentVerdict = null;
            int currentContestId = Integer.MIN_VALUE;
            String currentIndex = null;
            while (parser.nextToken() != null) {
                JsonToken token = parser.currentToken();
                if (token == JsonToken.START_ARRAY && "result".equals(parser.currentName())) {
                    inResultArray = true;
                    continue;
                }
                if (!inResultArray) {
                    continue;
                }
                if (token == JsonToken.END_ARRAY) {
                    inResultArray = false;
                    break;
                }
                if (token == JsonToken.START_OBJECT) {
                    objectDepth++;
                    currentVerdict = null;
                    currentContestId = Integer.MIN_VALUE;
                    currentIndex = null;
                    continue;
                }
                if (token == JsonToken.END_OBJECT) {
                    objectDepth--;
                    if (objectDepth == 0) {
                        result.entriesInPage++;
                        if (currentContestId == contestId && index.equals(currentIndex)) {
                            if ("OK".equals(currentVerdict)) {
                                result.foundAccepted = true;
                                return result;
                            }
                            if (result.latestOther == null && currentVerdict != null) {
                                result.latestOther = toDisplayVerdict(currentVerdict);
                            }
                        }
                        if (currentContestId != Integer.MIN_VALUE && currentContestId < minContestIdSeen) {
                            minContestIdSeen = currentContestId;
                        }
                    }
                    continue;
                }
                if (token == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    JsonParser p = parser;
                    // Read the value token that follows the field name.
                    JsonToken value = p.nextToken();
                    if ("verdict".equals(name) && value == JsonToken.VALUE_STRING) {
                        currentVerdict = p.getValueAsString();
                    } else if ("contestId".equals(name) && value == JsonToken.VALUE_NUMBER_INT) {
                        currentContestId = p.getValueAsInt();
                    } else if ("index".equals(name) && value == JsonToken.VALUE_STRING) {
                        currentIndex = p.getValueAsString();
                    }
                }
            }
        } catch (IOException ioe) {
            DiagnosticLogger.warn("[CodeforcesUserService] Failed to parse user status JSON: " + ioe.getMessage());
        }
        // If we've already scanned submissions older than the requested
        // contest, the user has no further history relevant to us.
        if (minContestIdSeen < contestId) {
            result.stop = true;
        }
        return result;
    }

    /**
     * Performs a GET and returns the response body, or {@code null} on any
     * network/parse failure or non-2xx response. Caps the body at 2 MiB.
     */
    private String httpGetJson(String urlStr) {
        try {
            return http.get(urlStr, TIMEOUT_MS, 2 * 1024 * 1024,
                    Map.of("Accept", "application/json"));
        } catch (Exception e) {
            DiagnosticLogger.warn("[CodeforcesUserService] HTTP GET failed for " + urlStr + ": " + e.getMessage());
            return null;
        }
    }

    /** Visible for tests. */
    static String toDisplayVerdictForTest(String cfVerdict) {
        return toDisplayVerdict(cfVerdict);
    }

    private static String toDisplayVerdict(String cfVerdict) {
        return switch (cfVerdict) {
            case "WRONG_ANSWER", "PARTIAL" -> "Wrong Answer";
            default -> "Error"; // RE, TLE, MLE, CE, SKIPPED, etc.
        };
    }

    /** Aggregated result of scanning one user.status page. Visible for tests. */
    static final class PageResult {
        boolean foundAccepted;
        String latestOther;
        boolean stop;
        int entriesInPage;
    }
}
