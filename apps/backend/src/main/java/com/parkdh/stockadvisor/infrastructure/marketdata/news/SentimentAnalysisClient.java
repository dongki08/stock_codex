package com.parkdh.stockadvisor.infrastructure.marketdata.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.SentimentAnalysisProperties;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class SentimentAnalysisClient {
    private final SentimentAnalysisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SentimentAnalysisClient(SentimentAnalysisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Optional<BigDecimal> analyze(String title, String summary) {
        if (!properties.enabled() || properties.baseUrl() == null || properties.baseUrl().isBlank() || MarketUtil.isDevPlaceholder(properties.baseUrl())) {
            return Optional.empty();
        }
        try {
            String body = objectMapper.writeValueAsString(new SentimentRequest(title, summary));
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl(properties.baseUrl()) + "/analyze"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Sentiment sidecar request failed. status={}", response.statusCode());
                return Optional.empty();
            }
            JsonNode scoreNode = objectMapper.readTree(response.body()).path("score");
            if (!scoreNode.isNumber()) {
                return Optional.empty();
            }
            return Optional.of(clamp(scoreNode.decimalValue()));
        } catch (Exception exception) {
            log.warn("Sentiment sidecar request error. error={}", exception.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private BigDecimal clamp(BigDecimal value) {
        BigDecimal clamped = value.max(BigDecimal.valueOf(-1)).min(BigDecimal.ONE);
        return clamped.setScale(3, RoundingMode.HALF_UP);
    }

    private record SentimentRequest(String title, String summary) {
    }
}
