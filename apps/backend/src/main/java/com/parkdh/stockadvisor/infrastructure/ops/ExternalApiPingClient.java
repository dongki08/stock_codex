package com.parkdh.stockadvisor.infrastructure.ops;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class ExternalApiPingClient {
    private final HttpClient httpClient;

    public ExternalApiPingClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public PingResult ping(String name, String url, Map<String, String> headers) {
        long startedAt = System.nanoTime();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(7))
                    .GET();
            if (headers != null) {
                headers.forEach(builder::header);
            }
            HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            long elapsedMillis = elapsedMillis(startedAt);
            int statusCode = response.statusCode();
            boolean reachable = statusCode >= 200 && statusCode < 500;
            String errorMessage = reachable ? null : "HTTP status=" + statusCode;
            return new PingResult(reachable, statusCode, errorMessage, elapsedMillis);
        } catch (Exception exception) {
            return new PingResult(false, null, exception.getMessage(), elapsedMillis(startedAt));
        }
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    public record PingResult(boolean reachable, Integer statusCode, String errorMessage, long elapsedMillis) {
    }
}
