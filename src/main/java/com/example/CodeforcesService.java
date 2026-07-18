package com.example;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CodeforcesService {

    private final ProblemCacheRepository problemCache;

    CodeforcesService(Path appDataDirectory) {
        this.problemCache = new ProblemCacheRepository(appDataDirectory);
    }

        private static final String[] PROBLEM_HOSTS = {
            "https://codeforces.com"
        };

        private static final String[] PROBLEM_PATH_TEMPLATES = {
            "/problemset/problem/%s/%s",
            "/contest/%s/problem/%s"
        };

        private static final int FETCH_TIMEOUT_MS = 25000;

    ProblemDetails fetchProblemDetails(String contestId, String index) throws IOException {
        return fetchProblemDetails(contestId, index, new CancellationToken());
    }

    ProblemDetails fetchProblemDetails(String contestId, String index, CancellationToken cancellation) throws IOException {
        if (cancellation == null) {
            cancellation = new CancellationToken();
        }
        String problemCode = contestId + index;
        ProblemDetails cached = problemCache.load(problemCode);
        if (cached != null) {
            if (!looksLikeBotCheckHtml(cached.problemHtml())) {
                return cached;
            }
            DiagnosticLogger.info("[CodeforcesService] Cached HTML for " + problemCode + " looks like a bot-check page, re-fetching");
        }

        IOException lastError = null;
        for (String host : PROBLEM_HOSTS) {
            if (cancellation.isCancelled()) {
                throw new IOException("Fetch cancelled before trying host " + host);
            }
            try {
                DiagnosticLogger.info("[CodeforcesService] Fetching " + problemCode + " from " + host);
                ProblemDetails fetched = fetchProblemDetailsFromHost(host, contestId, index, cancellation);
                if (cancellation.isCancelled()) {
                    // Do not persist a result the caller no longer wants.
                    throw new IOException("Fetch cancelled after retrieving " + problemCode);
                }
                problemCache.save(fetched);
                return fetched;
            } catch (IOException ex) {
                DiagnosticLogger.error("[CodeforcesService] Failed from " + host, ex);
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Could not fetch problem statement from available hosts");
    }

    private ProblemDetails fetchProblemDetailsFromHost(String host, String contestId, String index, CancellationToken cancellation) throws IOException {
        // Cookie bootstrap is best-effort: a transient 4xx/5xx or network blip on the
        // root should NOT abort the whole fetch — the problem page can still be tried
        // with empty cookies. This was previously the single biggest point of failure
        // (PROBLEM_HOSTS has only one entry, so a hard fail here short-circuited everything).
        Map<String, String> cookies = bootstrapCookies(host);
        IOException lastError = null;

        for (String pathTemplate : PROBLEM_PATH_TEMPLATES) {
            String url = host + String.format(pathTemplate, contestId, index);
            try {
                Document document = fetchDocument(url, cookies);

                if (looksLikeBotCheck(document)) {
                    throw new IOException("Codeforces returned a bot-check page from " + host);
                }

                Element statementRoot = extractStatementRoot(document, host);
                if (!looksLikeValidProblem(statementRoot)) {
                    int statusCode = responseStatusCode(document);
                    String statusSuffix = statusCode > 0 ? " (HTTP " + statusCode + ")" : "";
                    String detail = statementRoot == null
                            ? "Missing problem statement from " + url + statusSuffix
                            : "Problem markup at " + url + statusSuffix + " lacked a problem header";
                    throw new IOException(detail);
                }

                Element titleElement = statementRoot.selectFirst("div.header div.title");
                String title = titleElement != null ? titleElement.text() : (contestId + index);

                Element statementClone = statementRoot.clone();
                statementClone.select("style, script:not([type^=math/tex])").remove();

                return new ProblemDetails(contestId + index, title, statementClone.outerHtml());
            } catch (IOException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Could not fetch problem statement from " + host);
    }

    /**
     * Best-effort session cookie bootstrap. Returns an empty map on any failure
     * (network error, non-2xx response, bot-check page) so the caller can still
     * try the actual problem page.
     */
    private Map<String, String> bootstrapCookies(String host) {
        try {
            Connection.Response response = Jsoup.connect(host)
                    .userAgent(BROWSER_USER_AGENT)
                    .referrer("https://codeforces.com/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("DNT", "1")
                    .header("Sec-CH-UA", SEC_CH_UA)
                    .header("Sec-CH-UA-Mobile", SEC_CH_UA_MOBILE)
                    .header("Sec-CH-UA-Platform", SEC_CH_UA_PLATFORM)
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(0)
                    .timeout(FETCH_TIMEOUT_MS)
                    .execute();

            if (response.statusCode() >= 400) {
                DiagnosticLogger.warn("[CodeforcesService] Cookie bootstrap returned HTTP "
                        + response.statusCode() + " from " + host + " — continuing without session cookies");
                return Map.of();
            }

            String body = response.body();
            if (looksLikeBotCheckHtml(body)) {
                DiagnosticLogger.warn("[CodeforcesService] Cookie bootstrap got a bot-check page from "
                        + host + " — continuing without session cookies");
                return Map.of();
            }

            return new HashMap<>(response.cookies());
        } catch (IOException e) {
            DiagnosticLogger.warn("[CodeforcesService] Cookie bootstrap failed for " + host
                    + " — continuing without session cookies: " + e.getMessage());
            return Map.of();
        }
    }

    private Document fetchDocument(String url, Map<String, String> cookies) throws IOException {
        return fetchDocument(url, cookies, new CancellationToken());
    }

    private Document fetchDocument(String url, Map<String, String> cookies, CancellationToken cancellation) throws IOException {
        if (cancellation == null) {
            cancellation = new CancellationToken();
        }
        try {
            return fetchDocumentWithCurl(url, cookies, cancellation);
        } catch (IOException curlError) {
            if (cancellation.isCancelled()) {
                throw new IOException("Fetch cancelled while using curl for " + url, curlError);
            }
            DiagnosticLogger.error("[CodeforcesService] curl fetch failed for " + url, curlError);
        }

        return Jsoup.connect(url)
                .userAgent(BROWSER_USER_AGENT)
                .referrer("https://codeforces.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("DNT", "1")
                .header("Sec-CH-UA", SEC_CH_UA)
                .header("Sec-CH-UA-Mobile", SEC_CH_UA_MOBILE)
                .header("Sec-CH-UA-Platform", SEC_CH_UA_PLATFORM)
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .cookies(cookies)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0)
                .timeout(FETCH_TIMEOUT_MS)
                .get();
    }

    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // User-Agent Client Hints (UA-CH). Codeforces sits behind Cloudflare and now
    // gates requests on these — without them Cloudflare returns HTTP 403 to every
    // request, regardless of User-Agent. Values mirror a real Chrome 124 on Windows.
    private static final String SEC_CH_UA =
            "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"";
    private static final String SEC_CH_UA_MOBILE = "?0";
    private static final String SEC_CH_UA_PLATFORM = "\"Windows\"";

    private Document fetchDocumentWithCurl(String url, Map<String, String> cookies) throws IOException {
        return fetchDocumentWithCurl(url, cookies, new CancellationToken());
    }

    private Document fetchDocumentWithCurl(String url, Map<String, String> cookies, CancellationToken cancellation) throws IOException {
        String curlCommand = isWindows() ? "curl.exe" : "curl";
        List<String> command = new ArrayList<>();
        command.add(curlCommand);
        command.add("--silent");
        command.add("--show-error");
        command.add("--location");
        command.add("--compressed");
        command.add("--user-agent");
        command.add(BROWSER_USER_AGENT);
        command.add("--referer");
        command.add("https://codeforces.com/");
        command.add("--header");
        command.add("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        command.add("--header");
        command.add("Accept-Language: en-US,en;q=0.9");
        // Note: do NOT pass an explicit Accept-Encoding here — let curl's
        // --compressed flag advertise only the encodings the bundled curl
        // can actually decode. Listing "br" breaks against curl builds that
        // don't ship brotli (exit code 61, "Unrecognized content encoding").
        command.add("--header");
        command.add("DNT: 1");
        command.add("--header");
        command.add("Sec-CH-UA: " + SEC_CH_UA);
        command.add("--header");
        command.add("Sec-CH-UA-Mobile: " + SEC_CH_UA_MOBILE);
        command.add("--header");
        command.add("Sec-CH-UA-Platform: " + SEC_CH_UA_PLATFORM);
        command.add("--header");
        command.add("Sec-Fetch-Dest: document");
        command.add("--header");
        command.add("Sec-Fetch-Mode: navigate");
        command.add("--header");
        command.add("Sec-Fetch-Site: none");
        command.add("--header");
        command.add("Sec-Fetch-User: ?1");
        command.add("--header");
        command.add("Upgrade-Insecure-Requests: 1");
        String cookieHeader = formatCookieHeader(cookies);
        if (cookieHeader != null) {
            command.add("--cookie");
            command.add(cookieHeader);
        }
        command.add(url);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        // Watcher thread: if the cancellation token is signalled while curl is
        // still running, destroy the process so the call returns quickly.
        Thread cancellationWatcher = null;
        if (cancellation != null) {
            cancellationWatcher = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    if (cancellation.isCancelled()) {
                        process.destroyForcibly();
                        return;
                    }
                    if (!process.isAlive()) {
                        return;
                    }
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "cpa-fetch-cancel");
            cancellationWatcher.setDaemon(true);
            cancellationWatcher.start();
        }

        byte[] output;
        try (InputStream input = process.getInputStream()) {
            output = input.readAllBytes();
        }

        try {
            int exitCode = process.waitFor();
            String html = new String(output, StandardCharsets.UTF_8);
            if (cancellation != null && cancellation.isCancelled()) {
                throw new IOException("Fetch cancelled for " + url);
            }
            if (exitCode != 0) {
                throw new IOException("curl exited with code " + exitCode + (html.isBlank() ? "" : ": " + html.trim()));
            }
            if (html.isBlank()) {
                throw new IOException("curl returned empty body for " + url);
            }
            return Jsoup.parse(html, url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroy();
            throw new IOException("Interrupted while fetching " + url, e);
        } finally {
            if (cancellationWatcher != null) {
                cancellationWatcher.interrupt();
            }
        }
    }

    /**
     * Renders the cookie map as a single {@code "name=value; name2=value2"} string
     * suitable for {@code curl --cookie}. Returns {@code null} if there are no usable
     * cookies (so the caller can skip the flag entirely).
     */
    private static String formatCookieHeader(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || name.isEmpty() || value == null) {
                continue;
            }
            if (!first) {
                sb.append("; ");
            }
            sb.append(name).append('=').append(value);
            first = false;
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase().contains("win");
    }

    private Element extractStatementRoot(Document document, String host) {
        Element statementRoot = document.selectFirst("div.problem-statement");
        if (statementRoot != null) {
            return statementRoot;
        }

        Element fallbackRoot = document.selectFirst("div.ttypography");
        if (fallbackRoot != null) {
            Element wrapper = new Element("div");
            wrapper.addClass("problem-statement");
            wrapper.appendChild(fallbackRoot.clone());
            return wrapper;
        }

        Element pageContent = document.selectFirst("div#pageContent");
        if (pageContent != null) {
            Element wrapper = new Element("div");
            wrapper.addClass("problem-statement");
            wrapper.appendChild(pageContent.clone());
            return wrapper;
        }

        if (document.title() != null && document.title().toLowerCase().contains("codeforces")) {
            return null;
        }

        return null;
    }

    private boolean looksLikeBotCheck(Document document) {
        if (document == null) {
            return false;
        }
        // Check the visible text (what the user would see) AND the raw HTML
        // (some Cloudflare challenge pages put the markers in <script> blocks
        // or attributes that .text() strips out).
        if (looksLikeBotCheckHtml(document.text())) {
            return true;
        }
        Element head = document.head();
        String outerHtml = head != null ? head.outerHtml() : "";
        return looksLikeBotCheckHtml(outerHtml);
    }

    private boolean looksLikeBotCheckHtml(String html) {
        if (html == null || html.isEmpty()) {
            return false;
        }
        String lowered = html.toLowerCase();
        // Legacy Codeforces anti-bot markers
        if (lowered.contains("browser is being checked")
                || lowered.contains("please wait")
                || lowered.contains("security check")) {
            return true;
        }
        // Cloudflare challenge markers (modern UA-gated challenges)
        if (lowered.contains("cf-mitigated")
                || lowered.contains("cf-chl")
                || lowered.contains("challenge-running")
                || lowered.contains("challenge-stage")
                || lowered.contains("cf_chl_opt")
                || lowered.contains("just a moment")
                || lowered.contains("checking your browser before accessing")
                || lowered.contains("attention required")
                || lowered.contains("access denied")
                || lowered.contains("verify you are human")) {
            return true;
        }
        return false;
    }

    /**
     * A real Codeforces problem statement always contains a {@code div.header} block
     * with the title, time/memory limits, etc. If our heuristic-matched root element
     * lacks it (e.g. we matched a generic {@code div#pageContent} on an error page
     * or a redirect), treat the response as a failed fetch and try the next template.
     */
    private boolean looksLikeValidProblem(Element statementRoot) {
        if (statementRoot == null) {
            return false;
        }
        return statementRoot.selectFirst("div.header") != null;
    }

    private int responseStatusCode(Document document) {
        try {
            if (document.connection() != null && document.connection().response() != null) {
                return document.connection().response().statusCode();
            }
        } catch (Exception ignored) {
            // Best effort only.
        }

        return -1;
    }

    void clearProblemCache() {
        problemCache.clearAll();
    }

    void clearProblemCache(String problemCode) {
        problemCache.clear(problemCode);
    }

    ConnectivityResult evaluateConnectivity() {
        try {
            InetAddress address = InetAddress.getByName("codeforces.com");
            boolean pingReachable = address.isReachable(2500);
            boolean httpReachable = isHttpResponsive();

            if (pingReachable || httpReachable) {
                return new ConnectivityResult("CodeForces online and responsive", new Color(97, 214, 110));
            }
            return new ConnectivityResult("CodeForces unresponsive", new Color(246, 86, 86));
        } catch (UnknownHostException e) {
            return new ConnectivityResult("CodeForces offline", new Color(246, 86, 86));
        } catch (IOException e) {
            return new ConnectivityResult("CodeForces unresponsive", new Color(246, 86, 86));
        }
    }

    private boolean isHttpResponsive() {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create("https://codeforces.com/").toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int code = connection.getResponseCode();
            return code >= 200 && code < 500;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
