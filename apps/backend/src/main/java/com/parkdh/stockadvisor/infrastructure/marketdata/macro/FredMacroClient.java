package com.parkdh.stockadvisor.infrastructure.marketdata.macro;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FredMacroClient {
    private static final String FRED_CSV_URL = "https://fred.stlouisfed.org/graph/fredgraph.csv?id=%s";
    private static final Map<String, String> DEFAULT_SERIES = new LinkedHashMap<>();

    static {
        DEFAULT_SERIES.put("DGS10", "10-Year Treasury Constant Maturity Rate");
        DEFAULT_SERIES.put("FEDFUNDS", "Effective Federal Funds Rate");
        DEFAULT_SERIES.put("CPIAUCSL", "Consumer Price Index for All Urban Consumers");
        DEFAULT_SERIES.put("DCOILWTICO", "WTI Crude Oil Spot Price");
        DEFAULT_SERIES.put("DTWEXBGS", "Trade Weighted U.S. Dollar Index");
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<MacroRow> fetchSeries(String seriesId, int limit) {
        String safeSeriesId = seriesId == null || seriesId.isBlank() ? "DGS10" : seriesId;
        try {
            String url = FRED_CSV_URL.formatted(URLEncoder.encode(safeSeriesId, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("FRED CSV fetch failed. seriesId={}, status={}", safeSeriesId, response.statusCode());
                return List.of();
            }
            return parseCsv(safeSeriesId, DEFAULT_SERIES.getOrDefault(safeSeriesId, safeSeriesId), response.body(), limit);
        } catch (Exception exception) {
            log.warn("FRED CSV fetch error. seriesId={}, error={}", safeSeriesId, exception.getMessage());
            return List.of();
        }
    }

    public List<MacroRow> fetchDefaultSeries(int limitPerSeries) {
        return DEFAULT_SERIES.keySet().stream()
                .flatMap(seriesId -> fetchSeries(seriesId, limitPerSeries).stream())
                .toList();
    }

    private List<MacroRow> parseCsv(String seriesId, String seriesName, String csv, int limit) {
        String[] lines = csv.split("\\R");
        List<MacroRow> parsed = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 2 || parts[1].isBlank() || ".".equals(parts[1])) {
                continue;
            }
            try {
                parsed.add(new MacroRow(seriesId, seriesName, LocalDate.parse(parts[0]), new BigDecimal(parts[1]), "FRED"));
            } catch (Exception ignored) {
                // Skip malformed rows from the public CSV.
            }
        }
        int from = Math.max(0, parsed.size() - limit);
        return parsed.subList(from, parsed.size());
    }

    public record MacroRow(String seriesId, String seriesName, LocalDate observedDate, BigDecimal observedValue, String source) {
    }
}
