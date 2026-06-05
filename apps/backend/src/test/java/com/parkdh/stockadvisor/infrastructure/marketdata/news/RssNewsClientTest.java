package com.parkdh.stockadvisor.infrastructure.marketdata.news;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class RssNewsClientTest {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private final RssNewsClient client = new RssNewsClient();

    @Test
    void koreanNewsQueryIncludesCompanyNameAndTicker() {
        String url = URLDecoder.decode(
                client.buildGoogleUrl("KOSPI", "005930", "삼성전자"),
                StandardCharsets.UTF_8
        );

        assertThat(url).contains("q=삼성전자 005930 주식");
    }

    @Test
    void usGoogleNewsQueryIncludesCompanyNameAndTicker() {
        String url = URLDecoder.decode(
                client.buildGoogleUrl("NASDAQ", "AAPL", "Apple"),
                StandardCharsets.UTF_8
        );

        assertThat(url).contains("q=Apple AAPL stock");
        assertThat(url).contains("hl=en-US");
    }

    @Test
    void yahooNewsUrlUsesTicker() {
        String url = URLDecoder.decode(client.buildYahooUrl("AAPL"), StandardCharsets.UTF_8);

        assertThat(url).contains("headline?s=AAPL");
    }

    @Test
    void parseFeedReturnsOnlyNewsPublishedTodayInSeoul() throws Exception {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        String todayRfc1123 = today.atTime(9, 0).atZone(SEOUL_ZONE)
                .withZoneSameInstant(ZoneId.of("GMT"))
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String yesterdayRfc1123 = today.minusDays(1).atTime(9, 0).atZone(SEOUL_ZONE)
                .withZoneSameInstant(ZoneId.of("GMT"))
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String xml = """
                <rss><channel>
                  <item><title>오늘 뉴스</title><link>https://example.com/today</link><pubDate>%s</pubDate><description>today</description></item>
                  <item><title>어제 뉴스</title><link>https://example.com/yesterday</link><pubDate>%s</pubDate><description>yesterday</description></item>
                </channel></rss>
                """.formatted(todayRfc1123, yesterdayRfc1123);

        var rows = client.parseFeed(xml, "KOSPI", "005930", 5, "GOOGLE_NEWS_RSS");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).title()).isEqualTo("오늘 뉴스");
        assertThat(rows.get(0).publishedAt().toLocalDate()).isEqualTo(today);
    }
}
