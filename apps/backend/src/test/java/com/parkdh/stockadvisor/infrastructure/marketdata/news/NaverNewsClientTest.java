package com.parkdh.stockadvisor.infrastructure.marketdata.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.NaverNewsProperties;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsClientTest {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private final NaverNewsClient client = new NaverNewsClient(
            new NaverNewsProperties("client-id", "client-secret"),
            new ObjectMapper()
    );

    @Test
    void buildUrlUsesCompanyNameTickerAndDateSort() {
        String url = URLDecoder.decode(client.buildUrl("005930", "삼성전자", 5), StandardCharsets.UTF_8);

        assertThat(url).contains("query=삼성전자 005930 주식");
        assertThat(url).contains("display=5");
        assertThat(url).contains("sort=date");
    }

    @Test
    void buildUrlWithoutTickerUsesKoreanMarketQuery() {
        String url = URLDecoder.decode(client.buildUrl(null, null, 5), StandardCharsets.UTF_8);

        assertThat(url).contains("query=한국 증시");
    }

    @Test
    void parseResponseReturnsOnlyTodayAndCleansHtml() throws Exception {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        String todayValue = today.atTime(9, 0).atZone(SEOUL_ZONE).format(formatter);
        String yesterdayValue = today.minusDays(1).atTime(9, 0).atZone(SEOUL_ZONE).format(formatter);
        String json = """
                {
                  "items": [
                    {
                      "title": "<b>삼성전자</b> &amp; 반도체 상승",
                      "originalLink": "https://example.com/original",
                      "link": "https://n.news.naver.com/today",
                      "description": "<b>실적</b> 개선",
                      "pubDate": "%s"
                    },
                    {
                      "title": "어제 뉴스",
                      "link": "https://n.news.naver.com/yesterday",
                      "description": "어제",
                      "pubDate": "%s"
                    }
                  ]
                }
                """.formatted(todayValue, yesterdayValue);

        var rows = client.parseResponse(json, "KOSPI", "005930", 5);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).title()).isEqualTo("삼성전자 & 반도체 상승");
        assertThat(rows.get(0).summary()).isEqualTo("실적 개선");
        assertThat(rows.get(0).url()).isEqualTo("https://example.com/original");
        assertThat(rows.get(0).source()).isEqualTo("NAVER_NEWS_API");
        assertThat(rows.get(0).publishedAt().toLocalDate()).isEqualTo(today);
    }
}
