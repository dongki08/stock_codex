package com.parkdh.stockadvisor.application.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.ContextRelationAnalysisRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextRelationAnalysisServiceTest {
    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private DisclosureEventRepository disclosureEventRepository;
    @Mock
    private ContextRelationAnalysisRepository contextRelationAnalysisRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private CodexClient codexClient;

    private ContextRelationAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new ContextRelationAnalysisService(
                newsArticleRepository,
                disclosureEventRepository,
                contextRelationAnalysisRepository,
                appSettingRepository,
                codexClient,
                new ObjectMapper()
        );
    }

    @Test
    void analyzeMarketCallsCodexOnceAndSavesStructuredTickerResults() {
        MarketUniverseEntity samsung = universe("005930", "Samsung Electronics");
        when(appSettingRepository.findById("context.relation.codex.enabled")).thenReturn(Optional.empty());
        when(newsArticleRepository.findByMarketAndTickerAndPublishedAtBetweenOrderByPublishedAtDesc(
                eq("KOSPI"), eq("005930"), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(new NewsArticleEntity(
                        "n1", "005930", "KOSPI", "Samsung raises chip investment",
                        "https://example.com/news", "TEST", LocalDateTime.now(), "investment summary", BigDecimal.valueOf(0.4)
                )));
        when(disclosureEventRepository.findByMarketAndTickerAndDisclosedAtBetweenOrderByDisclosedAtDesc(
                eq("KOSPI"), eq("005930"), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(new DisclosureEventEntity(
                        "d1", "005930", "KOSPI", "Capital expenditure decision",
                        "https://example.com/disclosure", "DART", "INVESTMENT", 85, LocalDateTime.now(), "{}"
                )));
        when(codexClient.call(any(), eq("context-relation"), eq("context-relation-KOSPI")))
                .thenReturn(new CodexClient.CodexResult(true, """
                        ```json
                        [{"ticker":"005930","direction":"POSITIVE","confidence":82,"riskLevel":"MEDIUM","score":78,
                          "summary":"Investment disclosure supports the news catalyst.",
                          "keyFactors":["capacity expansion"],"contradictions":["near-term cost pressure"]}]
                        ```
                        """, null, 10));
        when(contextRelationAnalysisRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int saved = service.analyzeMarket("KOSPI", List.of(samsung));

        assertThat(saved).isEqualTo(1);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(codexClient).call(prompt.capture(), eq("context-relation"), eq("context-relation-KOSPI"));
        assertThat(prompt.getValue()).contains("005930", "Samsung raises chip investment", "Capital expenditure decision");
        assertThat(prompt.getValue()).doesNotContain("https://example.com");
        verify(contextRelationAnalysisRepository).saveAll(any());
    }

    @Test
    void analyzeMarketDoesNotSaveWhenCodexFails() {
        when(appSettingRepository.findById("context.relation.codex.enabled")).thenReturn(Optional.empty());
        when(newsArticleRepository.findByMarketAndTickerAndPublishedAtBetweenOrderByPublishedAtDesc(
                eq("NASDAQ"), eq("AAPL"), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(new NewsArticleEntity(
                        "n1", "AAPL", "NASDAQ", "Apple news", "https://example.com",
                        "TEST", LocalDateTime.now(), null, null
                )));
        when(disclosureEventRepository.findByMarketAndTickerAndDisclosedAtBetweenOrderByDisclosedAtDesc(
                eq("NASDAQ"), eq("AAPL"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(codexClient.call(any(), eq("context-relation"), eq("context-relation-NASDAQ")))
                .thenReturn(new CodexClient.CodexResult(false, "fallback", "timeout", 300_000));

        int saved = service.analyzeMarket("NASDAQ", List.of(universe("AAPL", "Apple")));

        assertThat(saved).isZero();
        verify(contextRelationAnalysisRepository, never()).saveAll(any());
    }

    private MarketUniverseEntity universe(String ticker, String name) {
        return new MarketUniverseEntity(
                ticker, ticker.equals("005930") ? "KOSPI" : "NASDAQ", name, "Technology",
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000), BigDecimal.valueOf(100),
                true, "TEST", LocalDate.now()
        );
    }
}
