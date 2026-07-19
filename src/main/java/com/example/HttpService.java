package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Shared bounded HTTP policy for API and metadata requests. */
final class HttpService {

    static final int DEFAULT_MAX_BODY_BYTES = 5 * 1024 * 1024;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2)
            .build();

    String get(String url, int timeoutMillis, int maxBodyBytes,
               Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(100, timeoutMillis)))
                .GET()
                .header("User-Agent", "cp-ally-ide")
                .header("Accept", "application/json");
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (key != null && value != null) builder.header(key, value);
            });
        }
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        byte[] body = response.body();
        if (body.length > maxBodyBytes) {
            throw new IOException("Response exceeded " + maxBodyBytes + " bytes from " + url);
        }
        return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }

    String getJson(String url, int timeoutMillis) throws IOException, InterruptedException {
        return get(url, timeoutMillis, DEFAULT_MAX_BODY_BYTES,
                Map.of("Accept", "application/json"));
    }
}
