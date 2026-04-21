package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.Color;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

class CodeforcesService {

    ProblemDetails fetchProblemDetails(String contestId, String index) throws IOException {
        String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        Element statementRoot = document.selectFirst("div.problem-statement");
        if (statementRoot == null) {
            throw new IOException("Missing problem statement");
        }

        Element titleElement = statementRoot.selectFirst("div.header div.title");
        String title = titleElement != null ? titleElement.text() : (contestId + index);

        Element statementClone = statementRoot.clone();
        statementClone.select("style, script:not([type^=math/tex])").remove();

        return new ProblemDetails(contestId + index, title, statementClone.outerHtml());
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
            URL url = new URL("https://codeforces.com/");
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
