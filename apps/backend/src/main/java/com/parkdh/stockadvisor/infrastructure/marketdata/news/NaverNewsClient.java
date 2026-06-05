package com.parkdh.stockadvisor.infrastructure.marketdata.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.NaverNewsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class NaverNewsClient {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter NAVER_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final NaverNewsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public NaverNewsClient(NaverNewsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<RssNewsClient.NewsRow> fetchNews(String market, String ticker, String companyName, int limit) {
        if (!properties.configured()) {
            return List.of();
        }
        String url = buildUrl(ticker, companyName, limit);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Naver-Client-Id", properties.clientId())
                    .header("X-Naver-Client-Secret", properties.clientSecret())
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Naver news fetch failed. status={}, ticker={}", response.statusCode(), ticker);
                return List.of();
            }
            return parseResponse(response.body(), market, ticker, limit);
        } catch (Exception exception) {
            log.warn("Naver news fetch error. ticker={}, error={}", ticker, exception.getMessage());
            return List.of();
        }
    }

    String buildUrl(String ticker, String companyName, int limit) {
        String query = ticker == null || ticker.isBlank()
                ? "한국 증시"
                : companyName == null || companyName.isBlank()
                ? ticker + " 주식"
                : companyName + " " + ticker + " 주식";
        return "https://openapi.naver.com/v1/search/news.json?query=" + encode(query)
                + "&display=" + limit + "&start=1&sort=date";
    }

    List<RssNewsClient.NewsRow> parseResponse(String json, String market, String ticker, int limit) throws Exception {
        JsonNode items = objectMapper.readTree(json).path("items");
        if (!items.isArray()) {
            return List.of();
        }
        List<RssNewsClient.NewsRow> rows = new ArrayList<>();
        for (JsonNode item : items) {
            if (rows.size() >= limit) {
                break;
            }
            LocalDateTime publishedAt = parseDate(item.path("pubDate").asText());
            if (publishedAt == null || !publishedAt.toLocalDate().equals(LocalDate.now(SEOUL_ZONE))) {
                continue;
            }
            String title = clean(item.path("title").asText());
            String url = item.path("originalLink").asText();
            if (url.isBlank()) {
                url = item.path("link").asText();
            }
            if (title.isBlank() || url.isBlank()) {
                continue;
            }
            rows.add(new RssNewsClient.NewsRow(
                    ticker,
                    market,
                    title,
                    url,
                    "NAVER_NEWS_API",
                    publishedAt,
                    clean(item.path("description").asText())
            ));
        }
        return rows;
    }

    private LocalDateTime parseDate(String value) {
        try {
            return OffsetDateTime.parse(value, NAVER_DATE_FORMATTER)
                    .atZoneSameInstant(SEOUL_ZONE)
                    .toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String clean(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value.replaceAll("<[^>]+>", "")).trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
