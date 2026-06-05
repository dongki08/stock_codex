package com.parkdh.stockadvisor.infrastructure.marketdata.kr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.KrxOpenApiProperties;
import com.parkdh.stockadvisor.global.exception.CustomException;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class KrxOpenApiClient {
    private static final String BASE_URL = "https://data-dbg.krx.co.kr/svc/apis/sto";
    private static final String KOSPI_DAILY_PATH = "/stk_bydd_trd";
    private static final String KOSDAQ_DAILY_PATH = "/ksq_bydd_trd";
    private static final DateTimeFormatter BAS_DD_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final KrxOpenApiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return properties.authKey() != null
                && !properties.authKey().isBlank()
                && !MarketUtil.isDevPlaceholder(properties.authKey());
    }

    public List<KrxDailyTradeRow> fetchDailyTrades(String market, LocalDate tradeDate) {
        if (!isConfigured()) {
            throw new CustomException("KRX OpenAPI auth key is not configured.", 503);
        }
        String normalizedMarket = normalizeMarket(market);
        String path = switch (normalizedMarket) {
            case "KOSPI" -> KOSPI_DAILY_PATH;
            case "KOSDAQ" -> KOSDAQ_DAILY_PATH;
            default -> throw new CustomException("KRX OpenAPI supports only KOSPI/KOSDAQ daily prices.", 400);
        };
        try {
            String url = BASE_URL + path + "?basDd=" + tradeDate.format(BAS_DD_FORMATTER);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("AUTH_KEY", properties.authKey())
                    .header("Accept", "application/json")
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CustomException("KRX OpenAPI daily price fetch failed. status=" + response.statusCode(), response.statusCode());
            }
            return parseDailyTradeResponse(response.body(), normalizedMarket);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("KRX OpenAPI daily price fetch error. market={}, tradeDate={}, error={}", normalizedMarket, tradeDate, exception.getMessage());
            throw new CustomException("KRX OpenAPI daily price fetch error: " + exception.getMessage(), 502);
        }
    }

    List<KrxDailyTradeRow> parseDailyTradeResponse(String body, String market) {
        try {
            JsonNode rowsNode = objectMapper.readTree(body).path("OutBlock_1");
            if (!rowsNode.isArray()) {
                return List.of();
            }
            List<KrxDailyTradeRow> rows = new ArrayList<>();
            for (JsonNode row : rowsNode) {
                KrxDailyTradeRow parsed = parseRow(row, market);
                if (parsed != null) {
                    rows.add(parsed);
                }
            }
            return rows;
        } catch (Exception exception) {
            throw new CustomException("KRX OpenAPI response parse failed: " + exception.getMessage(), 502);
        }
    }

    private KrxDailyTradeRow parseRow(JsonNode row, String market) {
        String ticker = row.path("ISU_CD").asText("").trim();
        String name = row.path("ISU_NM").asText("").trim();
        LocalDate tradeDate = parseDate(row.path("BAS_DD").asText(""));
        BigDecimal open = parseDecimal(row.path("TDD_OPNPRC").asText(""));
        BigDecimal high = parseDecimal(row.path("TDD_HGPRC").asText(""));
        BigDecimal low = parseDecimal(row.path("TDD_LWPRC").asText(""));
        BigDecimal close = parseDecimal(row.path("TDD_CLSPRC").asText(""));
        BigDecimal volume = parseDecimal(row.path("ACC_TRDVOL").asText(""));
        BigDecimal turnover = parseDecimal(row.path("ACC_TRDVAL").asText(""));
        BigDecimal marketCap = parseDecimal(row.path("MKTCAP").asText(""));
        if (ticker.isBlank() || tradeDate == null || open == null || high == null || low == null || close == null || volume == null) {
            return null;
        }
        return new KrxDailyTradeRow(ticker, market, name, tradeDate, open, high, low, close, volume, turnover, marketCap);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, BAS_DD_FORMATTER);
        } catch (Exception exception) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace(",", "").trim();
        if (normalized.isBlank() || "-".equals(normalized) || "--".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeMarket(String market) {
        return market == null ? "" : market.trim().toUpperCase();
    }

    public record KrxDailyTradeRow(
            String ticker,
            String market,
            String name,
            LocalDate tradeDate,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal volume,
            BigDecimal turnover,
            BigDecimal marketCap
    ) {
    }
}
