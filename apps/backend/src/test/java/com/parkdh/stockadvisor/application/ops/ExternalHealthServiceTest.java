package com.parkdh.stockadvisor.application.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.ops.dto.ExternalHealthResponse;
import com.parkdh.stockadvisor.config.CodexCliProperties;
import com.parkdh.stockadvisor.config.DartProperties;
import com.parkdh.stockadvisor.config.KisProperties;
import com.parkdh.stockadvisor.config.SecProperties;
import com.parkdh.stockadvisor.config.SentimentAnalysisProperties;
import com.parkdh.stockadvisor.config.TelegramProperties;
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity;
import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.price.PriceIntradayEntity;
import com.parkdh.stockadvisor.infrastructure.ops.ExternalApiPingClient;
import com.parkdh.stockadvisor.infrastructure.persistence.codex.CodexCallRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalHealthServiceTest {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private CodexCallRepository codexCallRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private PriceIntradayRepository priceIntradayRepository;
    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private DisclosureEventRepository disclosureEventRepository;
    @Mock
    private MacroObservationRepository macroObservationRepository;
    @Mock
    private FundamentalMetricRepository fundamentalMetricRepository;
    @Mock
    private ExternalApiPingClient externalApiPingClient;

    private ExternalHealthService service;

    @BeforeEach
    void setUp() {
        when(appSettingRepository.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(externalApiPingClient.ping(anyString(), anyString(), anyMap()))
                .thenReturn(new ExternalApiPingClient.PingResult(true, 200, null, 20));
        service = new ExternalHealthService(
                new KisProperties("kis-key", "kis-secret", "https://kis.example"),
                new TelegramProperties("telegram-token", "chat-id"),
                new CodexCliProperties("codex"),
                new DartProperties("dart-key"),
                new SecProperties("stock-advisor@example.com"),
                new SentimentAnalysisProperties(true, "http://localhost:18080"),
                codexCallRepository,
                appSettingRepository,
                new ObjectMapper(),
                priceDailyRepository,
                priceIntradayRepository,
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                externalApiPingClient
        );
    }

    @Test
    void getExternalHealthIncludesExternalApiPingStatus() {
        when(externalApiPingClient.ping(anyString(), anyString(), anyMap()))
                .thenAnswer(invocation -> {
                    String name = invocation.getArgument(0);
                    if ("DART".equals(name)) {
                        return new ExternalApiPingClient.PingResult(false, 503, "service unavailable", 37);
                    }
                    return new ExternalApiPingClient.PingResult(true, 200, null, 21);
                });

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> component.name().startsWith("Ping "))
                .extracting(component -> component.name() + ":" + component.status())
                .containsExactly(
                        "Ping KIS:READY",
                        "Ping DART:UNREACHABLE",
                        "Ping SEC EDGAR:READY",
                        "Ping RSS:READY",
                        "Ping FRED:READY",
                        "Ping Stooq:READY",
                        "Ping KIND:READY",
                        "Ping Sentiment Sidecar:READY"
                );
    }

    @Test
    void getExternalHealthMarksSentimentSidecarMissingWhenDisabled() {
        service = new ExternalHealthService(
                new KisProperties("kis-key", "kis-secret", "https://kis.example"),
                new TelegramProperties("telegram-token", "chat-id"),
                new CodexCliProperties("codex"),
                new DartProperties("dart-key"),
                new SecProperties("stock-advisor@example.com"),
                new SentimentAnalysisProperties(false, "http://localhost:18080"),
                codexCallRepository,
                appSettingRepository,
                new ObjectMapper(),
                priceDailyRepository,
                priceIntradayRepository,
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                externalApiPingClient
        );

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> "Ping Sentiment Sidecar".equals(component.name()))
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.status()).isEqualTo("MISSING_CONFIG");
                    assertThat(component.message()).contains("비활성화");
                });
        verify(externalApiPingClient, never()).ping("Sentiment Sidecar", "http://localhost:18080/health", java.util.Map.of());
    }

    @Test
    void getExternalHealthMarksSentimentSidecarMissingWhenBaseUrlIsPlaceholder() {
        service = new ExternalHealthService(
                new KisProperties("kis-key", "kis-secret", "https://kis.example"),
                new TelegramProperties("telegram-token", "chat-id"),
                new CodexCliProperties("codex"),
                new DartProperties("dart-key"),
                new SecProperties("stock-advisor@example.com"),
                new SentimentAnalysisProperties(true, "dev-placeholder"),
                codexCallRepository,
                appSettingRepository,
                new ObjectMapper(),
                priceDailyRepository,
                priceIntradayRepository,
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                externalApiPingClient
        );

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> "Ping Sentiment Sidecar".equals(component.name()))
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.status()).isEqualTo("MISSING_CONFIG");
                    assertThat(component.message()).contains("Sentiment Sidecar 설정");
                });
        verify(externalApiPingClient, never()).ping("Sentiment Sidecar", "/health", java.util.Map.of());
    }

    @Test
    void getExternalHealthMarksSentimentSidecarUnreachableWhenHealthReturnsNotFound() {
        when(externalApiPingClient.ping(anyString(), anyString(), anyMap()))
                .thenAnswer(invocation -> {
                    String name = invocation.getArgument(0);
                    if ("Sentiment Sidecar".equals(name)) {
                        return new ExternalApiPingClient.PingResult(true, 404, null, 18);
                    }
                    return new ExternalApiPingClient.PingResult(true, 200, null, 21);
                });

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> "Ping Sentiment Sidecar".equals(component.name()))
                .singleElement()
                .satisfies(component -> {
                    assertThat(component.status()).isEqualTo("UNREACHABLE");
                    assertThat(component.message()).contains("status=404");
                });
    }

    @Test
    void getExternalHealthIncludesLatestCollectedDataStatus() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        when(priceDailyRepository.findAllByOrderByTradeDateDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceDailyEntity("AAPL", "NASDAQ", today.minusDays(1),
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "STOOQ")));
        when(priceIntradayRepository.findAllByOrderByTickAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceIntradayEntity("005930", "KOSPI", now,
                        BigDecimal.TEN, BigDecimal.ONE, "KIS")));
        when(newsArticleRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new NewsArticleEntity("news-1", "AAPL", "NASDAQ", "record profit beat",
                        "https://example.com/news", "RSS", now.minusHours(1), null, BigDecimal.ONE)));
        when(disclosureEventRepository.findAllByOrderByDisclosedAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new DisclosureEventEntity("disc-1", "AAPL", "NASDAQ", "10-K filed",
                        "https://example.com/disclosure", "SEC", "FILING", 80, now.minusDays(1), "{}")));
        when(macroObservationRepository.findAllByOrderByObservedDateDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new MacroObservationEntity("macro-1", "DGS10", "10Y Treasury",
                        today.minusDays(2), BigDecimal.valueOf(4.1), "FRED", now.minusDays(1))));
        when(fundamentalMetricRepository.findAllByOrderByPeriodEndDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new FundamentalMetricEntity("fund-1", "AAPL", "NASDAQ", "Revenue",
                        BigDecimal.valueOf(100), "USD", today.getYear(), "Q1", today.minusDays(30), "SEC", now.minusDays(1))));

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> component.name().startsWith("Data "))
                .extracting(component -> component.name() + ":" + component.status())
                .containsExactly(
                        "Data Price Daily:READY",
                        "Data Price Intraday:READY",
                        "Data News:READY",
                        "Data Disclosure:READY",
                        "Data Macro:READY",
                        "Data Fundamental:READY"
                );
    }

    @Test
    void getExternalHealthMarksOldCollectedDataAsStale() {
        LocalDateTime oldDateTime = LocalDateTime.of(2000, 1, 1, 10, 0);
        LocalDate oldDate = LocalDate.of(2000, 1, 1);
        when(priceDailyRepository.findAllByOrderByTradeDateDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceDailyEntity("AAPL", "NASDAQ", oldDate,
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "STOOQ")));
        when(priceIntradayRepository.findAllByOrderByTickAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceIntradayEntity("005930", "KOSPI", oldDateTime,
                        BigDecimal.TEN, BigDecimal.ONE, "KIS")));
        when(newsArticleRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new NewsArticleEntity("news-old", "AAPL", "NASDAQ", "old news",
                        "https://example.com/news-old", "RSS", oldDateTime, null, BigDecimal.ZERO)));
        when(disclosureEventRepository.findAllByOrderByDisclosedAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new DisclosureEventEntity("disc-old", "AAPL", "NASDAQ", "old filing",
                        "https://example.com/disclosure-old", "SEC", "FILING", 80, oldDateTime, "{}")));
        when(macroObservationRepository.findAllByOrderByObservedDateDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new MacroObservationEntity("macro-old", "DGS10", "10Y Treasury",
                        oldDate, BigDecimal.valueOf(4.1), "FRED", oldDateTime)));
        when(fundamentalMetricRepository.findAllByOrderByPeriodEndDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(new FundamentalMetricEntity("fund-old", "AAPL", "NASDAQ", "Revenue",
                        BigDecimal.valueOf(100), "USD", 2000, "Q1", oldDate, "SEC", oldDateTime)));

        ExternalHealthResponse response = service.getExternalHealth();

        assertThat(response.components())
                .filteredOn(component -> component.name().startsWith("Data "))
                .extracting(component -> component.name() + ":" + component.status())
                .containsExactly(
                        "Data Price Daily:STALE",
                        "Data Price Intraday:STALE",
                        "Data News:STALE",
                        "Data Disclosure:STALE",
                        "Data Macro:STALE",
                        "Data Fundamental:STALE"
                );
    }
}
