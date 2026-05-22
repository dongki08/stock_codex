package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.CodexCliProperties;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExitConfirmServiceTest {
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private PriceIntradayRepository priceIntradayRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private CodexClient codexClient;

    private ExitConfirmService service;

    @BeforeEach
    void setUp() {
        when(appSettingRepository.findById(anyString())).thenReturn(Optional.empty());
        service = new ExitConfirmService(
                recommendationRepository,
                priceIntradayRepository,
                priceDailyRepository,
                appSettingRepository,
                codexClient,
                new CodexCliProperties("codex"),
                new ObjectMapper()
        );
    }

    @Test
    void confirmIgnoresActionMentionOutsideFirstLine() {
        RecommendationEntity recommendation = recommendation();
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(priceIntradayRepository.findByMarketAndTickerOrderByTickAtDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of());
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceDailyEntity("AAPL", "NASDAQ", LocalDate.now(),
                        BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.ONE, BigDecimal.TEN, "TEST")));
        when(codexClient.call(anyString(), eq("stock-advisor"), eq("exit-confirm")))
                .thenReturn(new CodexClient.CodexResult(true, "Rationale only\nDo not use ACTION: CUT here.", null, 10));

        var response = service.confirm(1L);

        assertThat(response.action()).isEqualTo("HOLD");
    }

    @Test
    void confirmAcceptsStrictActionOnFirstLine() {
        RecommendationEntity recommendation = recommendation();
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(priceIntradayRepository.findByMarketAndTickerOrderByTickAtDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of());
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceDailyEntity("AAPL", "NASDAQ", LocalDate.now(),
                        BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.valueOf(101), BigDecimal.ONE, BigDecimal.TEN, "TEST")));
        when(codexClient.call(anyString(), eq("stock-advisor"), eq("exit-confirm")))
                .thenReturn(new CodexClient.CodexResult(true, "ACTION: TIGHTEN\nMove stop tighter.", null, 10));

        var response = service.confirm(1L);

        assertThat(response.action()).isEqualTo("TIGHTEN");
    }

    @Test
    void confirmCutsWithoutCodexWhenCurrentPriceIsAtOrBelowStopPrice() {
        RecommendationEntity recommendation = recommendation();
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(priceIntradayRepository.findByMarketAndTickerOrderByTickAtDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of());
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 1)))
                .thenReturn(List.of(new PriceDailyEntity("AAPL", "NASDAQ", LocalDate.now(),
                        BigDecimal.valueOf(99), BigDecimal.valueOf(99), BigDecimal.valueOf(99), BigDecimal.valueOf(99), BigDecimal.ONE, BigDecimal.TEN, "TEST")));

        var response = service.confirm(1L);

        assertThat(response.action()).isEqualTo("CUT");
        assertThat(response.usedFallback()).isFalse();
        assertThat(response.rationale()).contains("손절가 이하");
        verifyNoInteractions(codexClient);
    }

    private RecommendationEntity recommendation() {
        return new RecommendationEntity(
                "AAPL",
                "NASDAQ",
                "SHORT",
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(100),
                LocalDate.now().plusDays(5),
                70,
                "{}",
                "dev-rule-v0",
                LocalDateTime.now(),
                "OPEN"
        );
    }
}
