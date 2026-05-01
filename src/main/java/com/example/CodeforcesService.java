package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.Color;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;

class CodeforcesService {

    private final ProblemCacheRepository problemCache;

    CodeforcesService(Path appDataDirectory) {
        this.problemCache = new ProblemCacheRepository(appDataDirectory);
    }

    private static final String[] PROBLEM_HOSTS = {
            "https://codeforces.com",
            "https://m1.codeforces.com",
            "https://mirror.codeforces.com"
    };

    ProblemDetails fetchProblemDetails(String contestId, String index) throws IOException {
        String problemCode = contestId + index;
        ProblemDetails cached = problemCache.load(problemCode);
        if (cached != null) {
            return cached;
        }

        IOException lastError = null;
        for (String host : PROBLEM_HOSTS) {
            try {
                ProblemDetails fetched = fetchProblemDetailsFromHost(host, contestId, index);
                problemCache.save(fetched);
                return fetched;
            } catch (IOException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Could not fetch problem statement from available hosts");
    }

    private ProblemDetails fetchProblemDetailsFromHost(String host, String contestId, String index) throws IOException {
        String url = host + "/problemset/problem/" + contestId + "/" + index;
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .referrer("https://codeforces.com/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .followRedirects(true)
                .maxBodySize(0)
                .timeout(15000)
                .get();

        Element statementRoot = document.selectFirst("div.problem-statement");
        if (statementRoot == null) {
            throw new IOException("Missing problem statement from " + host);
        }

        Element titleElement = statementRoot.selectFirst("div.header div.title");
        String title = titleElement != null ? titleElement.text() : (contestId + index);

        Element statementClone = statementRoot.clone();
        statementClone.select("style, script:not([type^=math/tex])").remove();

        return new ProblemDetails(contestId + index, title, statementClone.outerHtml());
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
