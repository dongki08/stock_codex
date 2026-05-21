package com.parkdh.stockadvisor.infrastructure.marketdata.disclosure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.DartProperties;
import com.parkdh.stockadvisor.config.SecProperties;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DisclosureClient {
    private static final String DART_LIST_URL = "https://opendart.fss.or.kr/api/list.json";
    private static final String SEC_CURRENT_URL = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=8-K&owner=include&start=0&count=%d&output=atom";
    private static final String SEC_COMPANY_URL = "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=%s&type=8-K&owner=include&count=%d&output=atom";

    private final DartProperties dartProperties;
    private final SecProperties secProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<DisclosureRow> fetchDisclosures(String market, String ticker, int limit) {
        if ("KOSPI".equals(market) || "KOSDAQ".equals(market)) {
            return fetchDart(market, ticker, limit);
        }
        if ("NASDAQ".equals(market) || "NYSE".equals(market)) {
            return fetchSec(market, ticker, limit);
        }
        List<DisclosureRow> rows = new ArrayList<>();
        rows.addAll(fetchDart("KOSPI", ticker, limit));
        rows.addAll(fetchSec("NASDAQ", ticker, Math.max(1, limit - rows.size())));
        return rows.stream().limit(limit).toList();
    }

    private List<DisclosureRow> fetchDart(String market, String ticker, int limit) {
        if (MarketUtil.isDevPlaceholder(dartProperties.apiKey())) {
            log.info("DART api key is not configured; skipping DART disclosure sync.");
            return List.of();
        }
        try {
            String from = LocalDate.now().minusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);
            String url = DART_LIST_URL + "?crtfc_key=" + encode(dartProperties.apiKey())
                    + "&bgn_de=" + from
                    + "&page_count=" + Math.min(limit, 100);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DART disclosure fetch failed. status={}", response.statusCode());
                return List.of();
            }
            return parseDart(response.body(), market, ticker, limit);
        } catch (Exception exception) {
            log.warn("DART disclosure fetch error. error={}", exception.getMessage());
            return List.of();
        }
    }

    private List<DisclosureRow> parseDart(String body, String market, String ticker, int limit) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode list = root.path("list");
        if (!list.isArray()) {
            return List.of();
        }
        List<DisclosureRow> rows = new ArrayList<>();
        for (JsonNode item : list) {
            if (rows.size() >= limit) {
                break;
            }
            String stockCode = item.path("stock_code").asText(null);
            if (ticker != null && !ticker.isBlank() && !ticker.equals(stockCode)) {
                continue;
            }
            String receiptNo = item.path("rcept_no").asText();
            String reportName = item.path("report_nm").asText();
            if (receiptNo.isBlank() || reportName.isBlank()) {
                continue;
            }
            LocalDateTime disclosedAt = parseDartDate(item.path("rcept_dt").asText());
            rows.add(new DisclosureRow(
                    stockCode,
                    market,
                    reportName,
                    "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + receiptNo,
                    "DART",
                    item.path("corp_name").asText(null),
                    disclosedAt,
                    objectMapper.writeValueAsString(item),
                    "DART:" + receiptNo
            ));
        }
        return rows;
    }

    private List<DisclosureRow> fetchSec(String market, String ticker, int limit) {
        try {
            String url = ticker == null || ticker.isBlank()
                    ? SEC_CURRENT_URL.formatted(Math.min(limit, 100))
                    : SEC_COMPANY_URL.formatted(encode(ticker), Math.min(limit, 100));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", resolveSecUserAgent())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("SEC disclosure fetch failed. status={}", response.statusCode());
                return List.of();
            }
            return parseSec(response.body(), market, ticker, limit);
        } catch (Exception exception) {
            log.warn("SEC disclosure fetch error. error={}", exception.getMessage());
            return List.of();
        }
    }

    private List<DisclosureRow> parseSec(String xml, String market, String ticker, int limit) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList entries = document.getElementsByTagName("entry");
        List<DisclosureRow> rows = new ArrayList<>();
        for (int i = 0; i < entries.getLength() && rows.size() < limit; i++) {
            Element entry = (Element) entries.item(i);
            String title = text(entry, "title");
            String link = atomLink(entry);
            if (title.isBlank() || link.isBlank()) {
                continue;
            }
            rows.add(new DisclosureRow(ticker, market, title, link, "SEC_EDGAR", "8-K", parseSecDate(text(entry, "updated")), null, "SEC:" + stableKey(link)));
        }
        return rows;
    }

    private LocalDateTime parseDartDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
        } catch (Exception exception) {
            return null;
        }
    }

    private LocalDateTime parseSecDate(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception exception) {
            return null;
        }
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String atomLink(Element entry) {
        NodeList links = entry.getElementsByTagName("link");
        if (links.getLength() == 0) {
            return "";
        }
        Element link = (Element) links.item(0);
        String href = link.getAttribute("href");
        return href == null || href.isBlank() ? link.getTextContent() : href;
    }

    private String resolveSecUserAgent() {
        String configured = secProperties.userAgent();
        return configured == null || configured.isBlank() || MarketUtil.isDevPlaceholder(configured)
                ? "StockAdvisor/1.0 contact@example.com"
                : configured;
    }

    private String stableKey(String value) {
        return Integer.toUnsignedString(value.hashCode());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record DisclosureRow(String ticker, String market, String title, String url, String source, String disclosureType,
                                LocalDateTime disclosedAt, String rawJson, String externalKey) {
    }
}
