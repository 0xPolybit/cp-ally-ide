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
import java.util.HashMap;
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
        String problemCode = contestId + index;
        ProblemDetails cached = problemCache.load(problemCode);
        if (cached != null) {
            return cached;
        }

        IOException lastError = null;
        for (String host : PROBLEM_HOSTS) {
            try {
                System.err.println("[CodeforcesService] Fetching " + problemCode + " from " + host);
                ProblemDetails fetched = fetchProblemDetailsFromHost(host, contestId, index);
                problemCache.save(fetched);
                return fetched;
            } catch (IOException ex) {
                System.err.println("[CodeforcesService] Failed from " + host + ": " + ex.getMessage());
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Could not fetch problem statement from available hosts");
    }

    private ProblemDetails fetchProblemDetailsFromHost(String host, String contestId, String index) throws IOException {
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
                if (statementRoot == null) {
                    int statusCode = responseStatusCode(document);
                    String statusSuffix = statusCode > 0 ? " (HTTP " + statusCode + ")" : "";
                    throw new IOException("Missing problem statement from " + url + statusSuffix);
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

    private Map<String, String> bootstrapCookies(String host) throws IOException {
        Connection.Response response = Jsoup.connect(host)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer("https://codeforces.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0)
                .timeout(FETCH_TIMEOUT_MS)
                .execute();

        if (response.statusCode() >= 400) {
            throw new IOException("Failed to bootstrap Codeforces cookies from " + host + " (HTTP " + response.statusCode() + ")");
        }

        return new HashMap<>(response.cookies());
    }

    private Document fetchDocument(String url, Map<String, String> cookies) throws IOException {
        try {
            return fetchDocumentWithCurl(url);
        } catch (IOException curlError) {
            System.err.println("[CodeforcesService] curl fetch failed for " + url + ": " + curlError.getMessage());
        }

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer("https://codeforces.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Upgrade-Insecure-Requests", "1")
                .cookies(cookies)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0)
                .timeout(FETCH_TIMEOUT_MS)
                .get();
    }

    private Document fetchDocumentWithCurl(String url) throws IOException {
        String curlCommand = isWindows() ? "curl.exe" : "curl";
        Process process = new ProcessBuilder(
                curlCommand,
                "--silent",
                "--show-error",
                "--location",
                "--compressed",
                "--user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "--referer",
                "https://codeforces.com/",
                "--header",
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "--header",
                "Accept-Language: en-US,en;q=0.9",
                "--header",
                "Upgrade-Insecure-Requests: 1",
                url)
                .redirectErrorStream(true)
                .start();

        byte[] output;
        try (InputStream input = process.getInputStream()) {
            output = input.readAllBytes();
        }

        try {
            int exitCode = process.waitFor();
            String html = new String(output, StandardCharsets.UTF_8);
            if (exitCode != 0) {
                throw new IOException("curl exited with code " + exitCode + (html.isBlank() ? "" : ": " + html.trim()));
            }
            if (html.isBlank()) {
                throw new IOException("curl returned empty body for " + url);
            }
            return Jsoup.parse(html, url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
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
        String text = document.text();
        if (text == null) {
            return false;
        }

        String lowered = text.toLowerCase();
        return lowered.contains("browser is being checked")
                || lowered.contains("please wait")
                || lowered.contains("security check");
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
        try {
            URL url = URI.create("https://codeforces.com/").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int code = connection.getResponseCode();
            return code >= 200 && code < 500;
        } catch (IOException e) {
            return false;
        }
    }
}
