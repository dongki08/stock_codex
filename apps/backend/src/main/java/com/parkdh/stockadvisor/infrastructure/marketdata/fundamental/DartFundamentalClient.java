package com.parkdh.stockadvisor.infrastructure.marketdata.fundamental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.DartProperties;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.Optional;
import java.util.zip.ZipInputStream;

@Slf4j
@RequiredArgsConstructor
@Component
public class DartFundamentalClient {
    private static final String CORP_CODE_URL = "https://opendart.fss.or.kr/api/corpCode.xml";
    private static final String SINGLE_ACCOUNT_URL = "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json";
    private static final String ANNUAL_REPORT_CODE = "11011";
    private static final Map<String, String> ACCOUNT_METRICS = new LinkedHashMap<>();

    static {
        ACCOUNT_METRICS.put("매출액", "REVENUE");
        ACCOUNT_METRICS.put("영업이익", "OPERATING_INCOME");
        ACCOUNT_METRICS.put("당기순이익", "NET_INCOME");
    }

    private final DartProperties dartProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<DartFundamentalRow> fetchFundamentals(String ticker, String market) {
        if (ticker == null || ticker.isBlank() || MarketUtil.isDevPlaceholder(dartProperties.apiKey())) {
            return List.of();
        }
        Optional<String> corpCode = findCorpCode(ticker);
        if (corpCode.isEmpty()) {
            log.info("DART corp_code not found for ticker={}", ticker);
            return List.of();
        }
        int businessYear = LocalDate.now().minusYears(1).getYear();
        return fetchSingleAccount(corpCode.get(), ticker, market, businessYear);
    }

    private Optional<String> findCorpCode(String ticker) {
        try {
            String url = CORP_CODE_URL + "?crtfc_key=" + encode(dartProperties.apiKey());
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DART corp_code fetch failed. status={}", response.statusCode());
                return Optional.empty();
            }
            return parseCorpCodeZip(response.body(), ticker);
        } catch (Exception exception) {
            log.warn("DART corp_code fetch error. ticker={}, error={}", ticker, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> parseCorpCodeZip(byte[] zippedXml, String ticker) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedXml))) {
            if (zipInputStream.getNextEntry() == null) {
                return Optional.empty();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(zipInputStream);
            NodeList rows = document.getElementsByTagName("list");
            for (int i = 0; i < rows.getLength(); i++) {
                org.w3c.dom.Element row = (org.w3c.dom.Element) rows.item(i);
                if (ticker.equals(text(row, "stock_code"))) {
                    String corpCode = text(row, "corp_code");
                    return corpCode.isBlank() ? Optional.empty() : Optional.of(corpCode);
                }
            }
            return Optional.empty();
        }
    }

    private List<DartFundamentalRow> fetchSingleAccount(String corpCode, String ticker, String market, int businessYear) {
        try {
            String url = SINGLE_ACCOUNT_URL
                    + "?crtfc_key=" + encode(dartProperties.apiKey())
                    + "&corp_code=" + encode(corpCode)
                    + "&bsns_year=" + businessYear
                    + "&reprt_code=" + ANNUAL_REPORT_CODE;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DART single account fetch failed. ticker={}, status={}", ticker, response.statusCode());
                return List.of();
            }
            return parseFinancialRows(response.body(), ticker, market);
        } catch (Exception exception) {
            log.warn("DART single account fetch error. ticker={}, error={}", ticker, exception.getMessage());
            return List.of();
        }
    }

    List<DartFundamentalRow> parseFinancialRows(String body, String ticker, String market) {
        try {
            JsonNode list = objectMapper.readTree(body).path("list");
            if (!list.isArray()) {
                return List.of();
            }
            List<DartFundamentalRow> rows = new ArrayList<>();
            for (JsonNode item : list) {
                String accountName = item.path("account_nm").asText("");
                String metricName = ACCOUNT_METRICS.get(accountName);
                if (metricName == null) {
                    continue;
                }
                Optional<BigDecimal> current = parseAmount(item.path("thstrm_amount").asText(""));
                Optional<BigDecimal> previous = parseAmount(item.path("frmtrm_amount").asText(""));
                Integer fiscalYear = item.path("bsns_year").isNumber() ? item.path("bsns_year").asInt() : parseYear(item.path("bsns_year").asText(null));
                LocalDate periodEnd = fiscalYear == null ? null : LocalDate.of(fiscalYear, 12, 31);
                current.ifPresent(value -> rows.add(new DartFundamentalRow(ticker, market, metricName, value, "KRW", fiscalYear, ANNUAL_REPORT_CODE, periodEnd, "DART_FNLTT_SINGLE")));
                if (current.isPresent() && previous.isPresent() && previous.get().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal growth = current.get().subtract(previous.get())
                            .divide(previous.get().abs(), 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(6, RoundingMode.HALF_UP);
                    rows.add(new DartFundamentalRow(ticker, market, metricName + "_GROWTH_YOY", growth, "%", fiscalYear, ANNUAL_REPORT_CODE, periodEnd, "DART_FNLTT_SINGLE"));
                }
            }
            return rows;
        } catch (Exception exception) {
            log.warn("DART financial row parse error. ticker={}, error={}", ticker, exception.getMessage());
            return List.of();
        }
    }

    private Optional<BigDecimal> parseAmount(String value) {
        String normalized = value == null ? "" : value.replace(",", "").replace(" ", "").trim();
        if (normalized.isBlank() || "-".equals(normalized)) {
            return Optional.empty();
        }
        boolean negative = normalized.startsWith("(") && normalized.endsWith(")");
        normalized = normalized.replace("(", "").replace(")", "");
        try {
            BigDecimal amount = new BigDecimal(normalized);
            return Optional.of(negative ? amount.negate() : amount);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Integer parseYear(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String text(org.w3c.dom.Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record DartFundamentalRow(String ticker, String market, String metricName, BigDecimal metricValue, String unit,
                                     Integer fiscalYear, String fiscalPeriod, LocalDate periodEnd, String source) {
    }
}
