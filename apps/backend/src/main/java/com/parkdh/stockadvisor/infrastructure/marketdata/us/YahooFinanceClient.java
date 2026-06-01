package com.parkdh.stockadvisor.infrastructure.marketdata.us;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YahooFinanceClient {

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YahooFinanceClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<YahooDailyPrice> fetchDailyPrices(String ticker, LocalDate from, LocalDate to, int maxRows) {
        long period1 = from.atStartOfDay(NY_ZONE).toEpochSecond();
        long period2 = to.plusDays(1).atStartOfDay(NY_ZONE).toEpochSecond();
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker
                + "?interval=1d&period1=" + period1 + "&period2=" + period2;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 StockAdvisor/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Yahoo Finance 일봉 조회 실패. ticker={}, status={}", ticker, response.statusCode());
                return List.of();
            }
            return parse(ticker, response.body(), maxRows);
        } catch (Exception e) {
            log.warn("Yahoo Finance 일봉 조회 오류. ticker={}, error={}", ticker, e.getMessage());
            return List.of();
        }
    }

    private List<YahooDailyPrice> parse(String ticker, String body, int maxRows) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return List.of();

            JsonNode item = result.get(0);
            JsonNode timestamps = item.path("timestamp");
            JsonNode quote = item.path("indicators").path("quote").get(0);

            if (!timestamps.isArray() || quote == null) return List.of();

            List<YahooDailyPrice> prices = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                try {
                    LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong()).atZone(NY_ZONE).toLocalDate();
                    BigDecimal open = toBd(quote.path("open").get(i));
                    BigDecimal high = toBd(quote.path("high").get(i));
                    BigDecimal low = toBd(quote.path("low").get(i));
                    BigDecimal close = toBd(quote.path("close").get(i));
                    BigDecimal volume = toBd(quote.path("volume").get(i));
                    if (open == null || high == null || low == null || close == null || volume == null) continue;
                    BigDecimal turnover = close.multiply(volume);
                    prices.add(new YahooDailyPrice(ticker, date, open, high, low, close, volume, turnover));
                } catch (Exception ignored) {}
            }

            int from = Math.max(0, prices.size() - maxRows);
            return prices.subList(from, prices.size());
        } catch (Exception e) {
            log.warn("Yahoo Finance 응답 파싱 오류. ticker={}, error={}", ticker, e.getMessage());
            return List.of();
        }
    }

    private BigDecimal toBd(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        try {
            return BigDecimal.valueOf(node.asDouble()).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    public record YahooDailyPrice(
            String ticker, LocalDate tradeDate,
            BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
            BigDecimal closePrice, BigDecimal volume, BigDecimal turnover) {}
}
