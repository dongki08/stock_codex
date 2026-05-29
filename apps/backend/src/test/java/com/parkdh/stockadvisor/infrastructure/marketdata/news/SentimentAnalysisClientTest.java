package com.parkdh.stockadvisor.infrastructure.marketdata.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.SentimentAnalysisProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SentimentAnalysisClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void analyzePostsTitleAndSummaryAndClampsScore() throws Exception {
        AtomicReference<JsonNode> requestJson = new AtomicReference<>();
        startServer(exchange -> {
            requestJson.set(objectMapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "{\"score\":1.42}");
        });

        SentimentAnalysisClient client = new SentimentAnalysisClient(
                new SentimentAnalysisProperties(true, baseUrl()),
                objectMapper
        );

        Optional<BigDecimal> score = client.analyze("AAPL beats estimates", "Guidance raised");

        assertThat(score).hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("1.000"));
        assertThat(requestJson.get().path("title").asText()).isEqualTo("AAPL beats estimates");
        assertThat(requestJson.get().path("summary").asText()).isEqualTo("Guidance raised");
    }

    @Test
    void analyzeReturnsEmptyWhenSidecarDisabled() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, "{\"score\":0.7}");
        });
        SentimentAnalysisClient client = new SentimentAnalysisClient(
                new SentimentAnalysisProperties(false, baseUrl()),
                objectMapper
        );

        Optional<BigDecimal> score = client.analyze("AAPL beats estimates", null);

        assertThat(score).isEmpty();
        assertThat(requests).hasValue(0);
    }

    @Test
    void analyzeReturnsEmptyWhenResponseHasNoNumericScore() throws Exception {
        startServer(exchange -> respond(exchange, 200, "{\"label\":\"positive\"}"));
        SentimentAnalysisClient client = new SentimentAnalysisClient(
                new SentimentAnalysisProperties(true, baseUrl()),
                objectMapper
        );

        Optional<BigDecimal> score = client.analyze("AAPL beats estimates", null);

        assertThat(score).isEmpty();
    }

    @Test
    void analyzeReturnsEmptyWhenSidecarReturnsError() throws Exception {
        startServer(exchange -> respond(exchange, 503, "{\"error\":\"warming up\"}"));
        SentimentAnalysisClient client = new SentimentAnalysisClient(
                new SentimentAnalysisProperties(true, baseUrl()),
                objectMapper
        );

        Optional<BigDecimal> score = client.analyze("AAPL beats estimates", null);

        assertThat(score).isEmpty();
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/analyze", exchange -> {
            try {
                assertThat(exchange.getRequestMethod()).isEqualTo("POST");
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
