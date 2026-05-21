package com.parkdh.stockadvisor.infrastructure.marketdata.fundamental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.SecProperties;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class SecFundamentalClient {
    private static final String COMPANY_TICKERS_URL = "https://www.sec.gov/files/company_tickers.json";
    private static final String COMPANY_FACTS_URL = "https://data.sec.gov/api/xbrl/companyfacts/CIK%s.json";
    private static final Map<String, String> TAGS = new LinkedHashMap<>();

    static {
        TAGS.put("Revenues", "REVENUE");
        TAGS.put("NetIncomeLoss", "NET_INCOME");
        TAGS.put("OperatingIncomeLoss", "OPERATING_INCOME");
        TAGS.put("Assets", "ASSETS");
        TAGS.put("Liabilities", "LIABILITIES");
        TAGS.put("StockholdersEquity", "EQUITY");
        TAGS.put("EarningsPerShareDiluted", "EPS_DILUTED");
    }

    private final SecProperties secProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<FundamentalRow> fetchFundamentals(String ticker, String market) {
        if (ticker == null || ticker.isBlank()) {
            return List.of();
        }
        Optional<String> cik = findCik(ticker);
        if (cik.isEmpty()) {
            log.info("SEC CIK not found for ticker={}", ticker);
            return List.of();
        }
        return fetchCompanyFacts(cik.get(), ticker.toUpperCase(), market);
    }

    private Optional<String> findCik(String ticker) {
        try {
            HttpRequest request = baseRequest(COMPANY_TICKERS_URL).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("SEC ticker map fetch failed. status={}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String normalizedTicker = ticker.toUpperCase();
            for (JsonNode node : root) {
                if (normalizedTicker.equals(node.path("ticker").asText("").toUpperCase())) {
                    int cik = node.path("cik_str").asInt();
                    return Optional.of("%010d".formatted(cik));
                }
            }
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("SEC ticker map fetch error. ticker={}, error={}", ticker, exception.getMessage());
            return Optional.empty();
        }
    }

    private List<FundamentalRow> fetchCompanyFacts(String cik, String ticker, String market) {
        try {
            HttpRequest request = baseRequest(COMPANY_FACTS_URL.formatted(cik)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("SEC companyfacts fetch failed. ticker={}, status={}", ticker, response.statusCode());
                return List.of();
            }
            JsonNode facts = objectMapper.readTree(response.body()).path("facts").path("us-gaap");
            List<FundamentalRow> rows = new ArrayList<>();
            for (Map.Entry<String, String> entry : TAGS.entrySet()) {
                latestFact(facts.path(entry.getKey()), ticker, market, entry.getValue()).ifPresent(rows::add);
            }
            return rows;
        } catch (Exception exception) {
            log.warn("SEC companyfacts fetch error. ticker={}, error={}", ticker, exception.getMessage());
            return List.of();
        }
    }

    private Optional<FundamentalRow> latestFact(JsonNode factNode, String ticker, String market, String metricName) {
        JsonNode units = factNode.path("units");
        if (units.isMissingNode()) {
            return Optional.empty();
        }
        String unit = units.has("USD") ? "USD" : units.has("USD/shares") ? "USD/shares" : units.fieldNames().hasNext() ? units.fieldNames().next() : null;
        if (unit == null) {
            return Optional.empty();
        }
        JsonNode values = units.path(unit);
        if (!values.isArray()) {
            return Optional.empty();
        }
        List<JsonNode> candidates = new ArrayList<>();
        values.forEach(value -> {
            if (value.hasNonNull("val") && value.hasNonNull("end")) {
                candidates.add(value);
            }
        });
        return candidates.stream()
                .max(Comparator.comparing(value -> value.path("filed").asText(value.path("end").asText(""))))
                .map(value -> new FundamentalRow(
                        ticker,
                        market,
                        metricName,
                        value.path("val").decimalValue(),
                        unit,
                        value.path("fy").isNumber() ? value.path("fy").asInt() : null,
                        value.path("fp").asText(null),
                        parseDate(value.path("end").asText(null)),
                        "SEC_COMPANYFACTS"
                ));
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", resolveSecUserAgent())
                .header("Accept", "application/json")
                .GET();
    }

    private String resolveSecUserAgent() {
        String configured = secProperties.userAgent();
        return configured == null || configured.isBlank() || MarketUtil.isDevPlaceholder(configured)
                ? "StockAdvisor/1.0 contact@example.com"
                : configured;
    }

    public record FundamentalRow(String ticker, String market, String metricName, BigDecimal metricValue, String unit,
                                 Integer fiscalYear, String fiscalPeriod, LocalDate periodEnd, String source) {
    }
}
