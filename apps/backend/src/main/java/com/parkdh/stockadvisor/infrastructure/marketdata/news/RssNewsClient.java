package com.parkdh.stockadvisor.infrastructure.marketdata.news;

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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RssNewsClient {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<NewsRow> fetchNews(String market, String ticker, int limit) {
        return fetchNews(market, ticker, null, limit);
    }

    public List<NewsRow> fetchNews(String market, String ticker, String companyName, int limit) {
        return isKoreanMarket(market)
                ? fetchGoogleNews(market, ticker, companyName, limit)
                : fetchYahooNews(market, ticker, limit);
    }

    public List<NewsRow> fetchGoogleNews(String market, String ticker, String companyName, int limit) {
        return fetch(buildGoogleUrl(market, ticker, companyName), market, ticker, limit, "GOOGLE_NEWS_RSS");
    }

    public List<NewsRow> fetchYahooNews(String market, String ticker, int limit) {
        return fetch(buildYahooUrl(ticker), market, ticker, limit, "YAHOO_FINANCE_RSS");
    }

    private List<NewsRow> fetch(String url, String market, String ticker, int limit, String source) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "StockAdvisor/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("RSS news fetch failed. status={}, url={}", response.statusCode(), url);
                return List.of();
            }
            return parseFeed(response.body(), market, ticker, limit, source);
        } catch (Exception exception) {
            log.warn("RSS news fetch error. url={}, error={}", url, exception.getMessage());
            return List.of();
        }
    }

    String buildGoogleUrl(String market, String ticker, String companyName) {
        String query;
        if (isKoreanMarket(market)) {
            if (ticker == null || ticker.isBlank()) {
                query = "한국 증시";
            } else {
                query = companyName == null || companyName.isBlank()
                        ? ticker + " 주식"
                        : companyName + " " + ticker + " 주식";
            }
            return "https://news.google.com/rss/search?q=" + encode(query) + "&hl=ko&gl=KR&ceid=KR:ko";
        }
        query = ticker == null || ticker.isBlank()
                ? "stock market"
                : companyName == null || companyName.isBlank()
                ? ticker + " stock"
                : companyName + " " + ticker + " stock";
        return "https://news.google.com/rss/search?q=" + encode(query) + "&hl=en-US&gl=US&ceid=US:en";
    }

    String buildYahooUrl(String ticker) {
        return "https://feeds.finance.yahoo.com/rss/2.0/headline?s=" + encode(ticker == null ? "" : ticker) + "&region=US&lang=en-US";
    }

    List<NewsRow> parseFeed(String xml, String market, String ticker, int limit, String source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList items = document.getElementsByTagName("item");
        if (items.getLength() > 0) {
            return parseRssItems(items, market, ticker, limit, source);
        }
        return parseAtomEntries(document.getElementsByTagName("entry"), market, ticker, limit, source);
    }

    private List<NewsRow> parseRssItems(NodeList items, String market, String ticker, int limit, String source) {
        List<NewsRow> rows = new ArrayList<>();
        for (int i = 0; i < items.getLength() && rows.size() < limit; i++) {
            Element item = (Element) items.item(i);
            String title = text(item, "title");
            String link = text(item, "link");
            LocalDateTime publishedAt = parseDate(text(item, "pubDate"));
            if (title.isBlank() || link.isBlank() || !isPublishedToday(publishedAt)) {
                continue;
            }
            rows.add(new NewsRow(ticker, market, title, link, source, publishedAt, text(item, "description")));
        }
        return rows;
    }

    private List<NewsRow> parseAtomEntries(NodeList entries, String market, String ticker, int limit, String source) {
        List<NewsRow> rows = new ArrayList<>();
        for (int i = 0; i < entries.getLength() && rows.size() < limit; i++) {
            Element entry = (Element) entries.item(i);
            String title = text(entry, "title");
            String link = atomLink(entry);
            LocalDateTime publishedAt = parseDate(text(entry, "updated"));
            if (title.isBlank() || link.isBlank() || !isPublishedToday(publishedAt)) {
                continue;
            }
            rows.add(new NewsRow(ticker, market, title, link, source, publishedAt, text(entry, "summary")));
        }
        return rows;
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

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private boolean isPublishedToday(LocalDateTime publishedAt) {
        return publishedAt != null && publishedAt.toLocalDate().equals(LocalDate.now(SEOUL_ZONE));
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(SEOUL_ZONE)
                    .toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value)
                        .atZoneSameInstant(SEOUL_ZONE)
                        .toLocalDateTime();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private boolean isKoreanMarket(String market) {
        return "KOSPI".equals(market) || "KOSDAQ".equals(market);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record NewsRow(String ticker, String market, String title, String url, String source, LocalDateTime publishedAt, String summary) {
    }
}
