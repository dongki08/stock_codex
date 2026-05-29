package com.parkdh.stockadvisor.application.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.infrastructure.marketdata.disclosure.DisclosureClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.fundamental.DartFundamentalClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.fundamental.SecFundamentalClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.macro.FredMacroClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.news.RssNewsClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.news.SentimentAnalysisClient;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataCollectionServiceTest {
    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private DisclosureEventRepository disclosureEventRepository;
    @Mock
    private MacroObservationRepository macroObservationRepository;
    @Mock
    private FundamentalMetricRepository fundamentalMetricRepository;
    @Mock
    private RssNewsClient rssNewsClient;
    @Mock
    private DisclosureClient disclosureClient;
    @Mock
    private FredMacroClient fredMacroClient;
    @Mock
    private SecFundamentalClient secFundamentalClient;
    @Mock
    private DartFundamentalClient dartFundamentalClient;
    @Mock
    private KisApiClient kisApiClient;
    @Mock
    private SentimentAnalysisClient sentimentAnalysisClient;

    private MarketDataCollectionService service;

    @BeforeEach
    void setUp() {
        service = new MarketDataCollectionService(
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                rssNewsClient,
                disclosureClient,
                fredMacroClient,
                secFundamentalClient,
                dartFundamentalClient,
                kisApiClient,
                new MarketSignalScorer(),
                sentimentAnalysisClient
        );
    }

    @Test
    void syncNewsArticlesUsesMlSentimentWhenSidecarReturnsScore() {
        when(rssNewsClient.fetchNews("NASDAQ", "AAPL", 1))
                .thenReturn(List.of(new RssNewsClient.NewsRow(
                        "AAPL",
                        "NASDAQ",
                        "AAPL shares surge after record profit beat",
                        "https://example.com/aapl",
                        "RSS",
                        LocalDateTime.now(),
                        "strong demand"
                )));
        when(sentimentAnalysisClient.analyze("AAPL shares surge after record profit beat", "strong demand"))
                .thenReturn(Optional.of(BigDecimal.valueOf(0.870)));
        when(newsArticleRepository.findById(any(String.class))).thenReturn(Optional.empty());
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncNewsArticles("NASDAQ", "AAPL", 1);

        ArgumentCaptor<NewsArticleEntity> captor = ArgumentCaptor.forClass(NewsArticleEntity.class);
        verify(newsArticleRepository).save(captor.capture());
        assertThat(captor.getValue().getSentimentScore()).isEqualByComparingTo("0.870");
    }

    @Test
    void syncNewsArticlesFallsBackToRuleSentimentWhenSidecarHasNoScore() {
        when(rssNewsClient.fetchNews("NASDAQ", "AAPL", 1))
                .thenReturn(List.of(new RssNewsClient.NewsRow(
                        "AAPL",
                        "NASDAQ",
                        "AAPL shares plunge after lawsuit warning",
                        "https://example.com/aapl-risk",
                        "RSS",
                        LocalDateTime.now(),
                        null
                )));
        when(sentimentAnalysisClient.analyze("AAPL shares plunge after lawsuit warning", null))
                .thenReturn(Optional.empty());
        when(newsArticleRepository.findById(any(String.class))).thenReturn(Optional.empty());
        when(newsArticleRepository.save(any(NewsArticleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncNewsArticles("NASDAQ", "AAPL", 1);

        ArgumentCaptor<NewsArticleEntity> captor = ArgumentCaptor.forClass(NewsArticleEntity.class);
        verify(newsArticleRepository).save(captor.capture());
        assertThat(captor.getValue().getSentimentScore()).isNegative();
    }

    @Test
    void syncFundamentalMetricsUsesKisForKoreanMarkets() {
        LocalDate periodEnd = LocalDate.of(2026, 5, 26);
        when(kisApiClient.fetchFundamentalMetrics("005930", "KOSPI"))
                .thenReturn(List.of(
                        new KisApiClient.KisFundamentalMetric("005930", "KOSPI", "PER", BigDecimal.valueOf(12.3), "x", periodEnd, "KIS_INQUIRE_PRICE"),
                        new KisApiClient.KisFundamentalMetric("005930", "KOSPI", "PBR", BigDecimal.valueOf(1.2), "x", periodEnd, "KIS_INQUIRE_PRICE"),
                        new KisApiClient.KisFundamentalMetric("005930", "KOSPI", "ROE", BigDecimal.valueOf(9.5), "%", periodEnd, "KIS_INQUIRE_PRICE")
                ));
        when(dartFundamentalClient.fetchFundamentals("005930", "KOSPI"))
                .thenReturn(List.of(
                        new DartFundamentalClient.DartFundamentalRow("005930", "KOSPI", "REVENUE_GROWTH_YOY", BigDecimal.valueOf(12.5), "%", 2025, "11011", periodEnd, "DART_FNLTT_SINGLE")
                ));
        when(fundamentalMetricRepository.findById(any(String.class))).thenReturn(Optional.empty());
        when(fundamentalMetricRepository.save(any(FundamentalMetricEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.syncFundamentalMetrics("KOSPI", "005930");

        assertThat(response.source()).isEqualTo("KIS_INQUIRE_PRICE+DART_FNLTT_SINGLE");
        assertThat(response.market()).isEqualTo("KOSPI");
        assertThat(response.fetchedCount()).isEqualTo(4);
        assertThat(response.savedCount()).isEqualTo(4);
        verify(secFundamentalClient, never()).fetchFundamentals(any(), any());

        ArgumentCaptor<FundamentalMetricEntity> captor = ArgumentCaptor.forClass(FundamentalMetricEntity.class);
        verify(fundamentalMetricRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(FundamentalMetricEntity::getMetricName)
                .containsExactly("PER", "PBR", "ROE", "REVENUE_GROWTH_YOY");
        assertThat(captor.getAllValues()).extracting(FundamentalMetricEntity::getSource)
                .containsExactly("KIS_INQUIRE_PRICE", "KIS_INQUIRE_PRICE", "KIS_INQUIRE_PRICE", "DART_FNLTT_SINGLE");
    }
}
