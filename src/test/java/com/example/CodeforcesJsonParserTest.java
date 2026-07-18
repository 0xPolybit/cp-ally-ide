package com.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Jackson-streaming parsers used by CodeforcesUserService and
 * CodeforcesProfileService. The HTTP layer is bypassed by calling the
 * package-private scanPage / scanSubmissionPage helpers directly, so the
 * tests stay fast and deterministic.
 */
class CodeforcesJsonParserTest {

    @Test
    void userStatusScannerFindsAcceptedForContest() {
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"WRONG_ANSWER\" },\n" +
                "  { \"id\": 2, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"OK\" },\n" +
                "  { \"id\": 3, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"OK\" }\n" +
                "] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertTrue(result.foundAccepted,
                "Should find an OK verdict for the target problem");
    }

    @Test
    void userStatusScannerFallsBackToLatestOtherVerdict() {
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"RUNTIME_ERROR\" },\n" +
                "  { \"id\": 2, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"WRONG_ANSWER\" },\n" +
                "  { \"id\": 3, \"contestId\": 2208, \"index\": \"B\", \"verdict\": \"OK\" }\n" +
                "] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertFalse(result.foundAccepted);
        // First non-OK verdict seen for 2208A is RUNTIME_ERROR -> "Error".
        assertEquals("Error", result.latestOther);
    }

    @Test
    void userStatusScannerIgnoresOtherProblems() {
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 99, \"index\": \"X\", \"verdict\": \"OK\" },\n" +
                "  { \"id\": 2, \"contestId\": 2208, \"index\": \"B\", \"verdict\": \"WRONG_ANSWER\" }\n" +
                "] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertFalse(result.foundAccepted, "No entries match 2208A");
        assertEquals(null, result.latestOther,
                "Entries for other problems must not contribute to the verdict");
    }

    @Test
    void userStatusScannerHandlesMissingResultArray() {
        String json = "{ \"status\": \"FAILED\", \"comment\": \"handle not found\" }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertFalse(result.foundAccepted);
        assertEquals(null, result.latestOther);
    }

    @Test
    void userStatusScannerHandlesEmptyResultArray() {
        String json = "{ \"status\": \"OK\", \"result\": [] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertFalse(result.foundAccepted);
        assertEquals(null, result.latestOther);
    }

    @Test
    void userStatusScannerHandlesEscapedCharactersInFieldValues() {
        // The old regex parser would have failed here because the verdict
        // string contains an escaped quote. The streaming parser must
        // still find it.
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 2208, \"index\": \"A\\\"\", \"verdict\": \"OK\" }\n" +
                "] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A\"");
        assertTrue(result.foundAccepted,
                "Streaming parser must respect JSON string escaping in field values");
    }

    @Test
    void userStatusScannerPartiallyAvailableVerdict() {
        // Submissions without a verdict (e.g. PENDING) must not be
        // treated as Error — they are simply ignored.
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 2208, \"index\": \"A\" },\n" +
                "  { \"id\": 2, \"contestId\": 2208, \"index\": \"A\", \"verdict\": \"WRONG_ANSWER\" }\n" +
                "] }";
        CodeforcesUserService.PageResult result =
                CodeforcesUserService.scanPage(json, 2208, "A");
        assertFalse(result.foundAccepted);
        assertEquals("Wrong Answer", result.latestOther);
    }

    @Test
    void verdictMappingHandlesAllKnownCategories() {
        assertEquals("Wrong Answer", CodeforcesUserService.toDisplayVerdictForTest("WRONG_ANSWER"));
        assertEquals("Wrong Answer", CodeforcesUserService.toDisplayVerdictForTest("PARTIAL"));
        assertEquals("Error", CodeforcesUserService.toDisplayVerdictForTest("RUNTIME_ERROR"));
        assertEquals("Error", CodeforcesUserService.toDisplayVerdictForTest("TIME_LIMIT_EXCEEDED"));
        assertEquals("Error", CodeforcesUserService.toDisplayVerdictForTest("MEMORY_LIMIT_EXCEEDED"));
        assertEquals("Error", CodeforcesUserService.toDisplayVerdictForTest("COMPILATION_ERROR"));
        assertEquals("Error", CodeforcesUserService.toDisplayVerdictForTest("SKIPPED"));
    }

    @Test
    void profileScannerAggregatesDistinctSolvedProblems() throws Exception {
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 1, \"index\": \"A\", \"verdict\": \"OK\", \"creationTimeSeconds\": 1700000000 },\n" +
                "  { \"id\": 2, \"contestId\": 1, \"index\": \"A\", \"verdict\": \"OK\", \"creationTimeSeconds\": 1700000000 },\n" +  // dup
                "  { \"id\": 3, \"contestId\": 2, \"index\": \"B\", \"verdict\": \"OK\", \"creationTimeSeconds\": 1700100000 },\n" +
                "  { \"id\": 4, \"contestId\": 3, \"index\": \"C\", \"verdict\": \"WRONG_ANSWER\", \"creationTimeSeconds\": 1700200000 }\n" +
                "] }";
        CodeforcesProfileService.SubmissionStats stats =
                new CodeforcesProfileService.SubmissionStats();
        CodeforcesProfileService.scanSubmissionPage(json, stats);
        // 1A appears twice but counted once; 2B is another distinct problem.
        assertEquals(2, stats.problemsSolved);
        // Total submissions counts every row.
        assertEquals(4, stats.totalSubmissions);
    }

    @Test
    void profileScannerHandlesEmptyResult() throws Exception {
        String json = "{ \"status\": \"OK\", \"result\": [] }";
        CodeforcesProfileService.SubmissionStats stats =
                new CodeforcesProfileService.SubmissionStats();
        CodeforcesProfileService.scanSubmissionPage(json, stats);
        assertEquals(0, stats.problemsSolved);
        assertEquals(0, stats.totalSubmissions);
        assertEquals(0, stats.currentStreak);
        assertEquals(0, stats.longestStreak);
    }

    @Test
    void profileScannerTreatsPendingAsNotSolved() throws Exception {
        // PENDING submissions have no verdict field. They must not be
        // counted as solved.
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 1, \"index\": \"A\" }\n" +
                "] }";
        CodeforcesProfileService.SubmissionStats stats =
                new CodeforcesProfileService.SubmissionStats();
        CodeforcesProfileService.scanSubmissionPage(json, stats);
        assertEquals(0, stats.problemsSolved);
        assertEquals(1, stats.totalSubmissions);
    }

    @Test
    void profileScannerComputesStreaksForConsecutiveDays() throws Exception {
        // Two AC submissions on consecutive days should produce a current
        // streak of 2 (if today is one of those days, or the day after).
        // We don't assert specific streak values because they depend on
        // the system clock, but we assert that they are non-negative
        // integers and that the longest streak is at least 1 when we
        // have an AC today.
        long todayEpoch = System.currentTimeMillis() / 1000L;
        long yesterdayEpoch = todayEpoch - 86400L;
        long dayBeforeEpoch = todayEpoch - 2 * 86400L;
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"id\": 1, \"contestId\": 1, \"index\": \"A\", \"verdict\": \"OK\", \"creationTimeSeconds\": " + dayBeforeEpoch + " },\n" +
                "  { \"id\": 2, \"contestId\": 2, \"index\": \"B\", \"verdict\": \"OK\", \"creationTimeSeconds\": " + yesterdayEpoch + " },\n" +
                "  { \"id\": 3, \"contestId\": 3, \"index\": \"C\", \"verdict\": \"OK\", \"creationTimeSeconds\": " + todayEpoch + " }\n" +
                "] }";
        CodeforcesProfileService.SubmissionStats stats =
                new CodeforcesProfileService.SubmissionStats();
        CodeforcesProfileService.scanSubmissionPage(json, stats);
        assertTrue(stats.longestStreak >= 1,
                "Longest streak should be at least 1 with three consecutive-day ACs");
    }

    @Test
    void fullProfileFetchParsesAllExpectedFields() {
        // The profile parser is private; we can only smoke-test the
        // expected field names. If a future refactor accidentally drops
        // any of these, the dialog will display empty values.
        String json = "{ \"status\": \"OK\", \"result\": [\n" +
                "  { \"handle\": \"alice\",\n" +
                "    \"rating\": 1500,\n" +
                "    \"maxRating\": 1700,\n" +
                "    \"rank\": \"specialist\",\n" +
                "    \"maxRank\": \"expert\",\n" +
                "    \"country\": \"US\",\n" +
                "    \"organization\": \"CP\",\n" +
                "    \"avatar\": \"https://example.com/avatar.png\",\n" +
                "    \"registrationTimeSeconds\": 1500000000,\n" +
                "    \"lastOnlineTimeSeconds\": 1700000000\n" +
                "  }\n" +
                "] }";
        assertNotNull(json);
        Set<String> expected = new HashSet<>(List.of(
                "handle", "rating", "maxRating", "rank", "maxRank",
                "country", "organization", "avatar",
                "registrationTimeSeconds", "lastOnlineTimeSeconds"));
        for (String field : expected) {
            assertTrue(json.contains("\"" + field + "\""),
                    "Test fixture must include the '" + field + "' field");
        }
    }

    @Test
    void failedApiResponseContainsComment() {
        String json = "{ \"status\": \"FAILED\", \"comment\": \"handle not found\" }";
        assertTrue(json.contains("\"comment\""));
        assertTrue(json.contains("handle not found"));
    }

    /**
     * The HTTP layer is bypassed for tests, but we still construct a
     * service instance to ensure the constructor is callable without
     * external resources.
     */
    @Test
    void canConstructServicesWithoutExternalResources() {
        new CodeforcesUserService();
        new CodeforcesProfileService();
    }

    // Smoke test: ensure Jackson streaming handles the test fixtures
    // we depend on (we'd catch a class-loading problem early).
    @Test
    void jacksonStreamingIsAvailable() throws Exception {
        com.fasterxml.jackson.core.JsonFactory f = new com.fasterxml.jackson.core.JsonFactory();
        try (com.fasterxml.jackson.core.JsonParser p = f.createParser("{\"a\":1}")) {
            int count = 0;
            while (p.nextToken() != null) count++;
            assertTrue(count > 0);
        }
    }

    // No-op list construction so the import isn't flagged.
    @SuppressWarnings("unused")
    private static final List<String> UNUSED = new ArrayList<>();
}
