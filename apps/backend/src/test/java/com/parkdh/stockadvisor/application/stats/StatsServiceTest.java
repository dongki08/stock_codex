package com.parkdh.stockadvisor.application.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(
                evaluationRepository,
                recommendationRepository,
                priceDailyRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getPaperTradingSummarizesOpenRecommendationsWithLatestDailyPriceAndWeights() throws Exception {
        RecommendationEntity targetTouched = recommendation(1L, "AAA", BigDecimal.valueOf(100), BigDecimal.valueOf(110), BigDecimal.valueOf(95), "{\"positionWeightPct\":20.00}");
        RecommendationEntity stopTouched = recommendation(2L, "BBB", BigDecimal.valueOf(100), BigDecimal.valueOf(115), BigDecimal.valueOf(92), "{\"positionWeightPct\":10.00}");
        when(recommendationRepository.findByStatus("OPEN")).thenReturn(List.of(targetTouched, stopTouched));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAA", PageRequest.of(0, 1)))
                .thenReturn(List.of(price("AAA", BigDecimal.valueOf(112))));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "BBB", PageRequest.of(0, 1)))
                .thenReturn(List.of(price("BBB", BigDecimal.valueOf(90))));

        StatsPaperTradingResponse response = statsService.getPaperTrading();

        assertThat(response.openCount()).isEqualTo(2);
        assertThat(response.pricedCount()).isEqualTo(2);
        assertThat(response.avgUnrealizedPnlPct()).isEqualByComparingTo("1.0000");
        assertThat(response.weightedUnrealizedPnlPct()).isEqualByComparingTo("1.4000");
        assertThat(response.totalWeightPct()).isEqualByComparingTo("30.00");
        assertThat(response.targetTouchCount()).isEqualTo(1);
        assertThat(response.stopTouchCount()).isEqualTo(1);
        assertThat(response.positions()).extracting(StatsPaperTradingResponse.PaperPosition::priceStatus)
                .containsExactlyInAnyOrder("TARGET_TOUCHED", "STOP_TOUCHED");
    }

    @Test
    void getPaperTradingFallsBackToEqualWeightWhenSignalWeightIsMissing() throws Exception {
        RecommendationEntity recommendation = recommendation(1L, "AAA", BigDecimal.valueOf(100), BigDecimal.valueOf(120), BigDecimal.valueOf(90), "{}");
        when(recommendationRepository.findByStatus("OPEN")).thenReturn(List.of(recommendation));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAA", PageRequest.of(0, 1)))
                .thenReturn(List.of(price("AAA", BigDecimal.valueOf(105))));

        StatsPaperTradingResponse response = statsService.getPaperTrading();

        assertThat(response.positions().get(0).positionWeightPct()).isEqualByComparingTo("20");
        assertThat(response.weightedUnrealizedPnlPct()).isEqualByComparingTo("1.0000");
    }

    private RecommendationEntity recommendation(Long id, String ticker, BigDecimal entry, BigDecimal target, BigDecimal stop, String signalsJson) throws Exception {
        RecommendationEntity entity = new RecommendationEntity(
                ticker,
                "NASDAQ",
                "SHORT",
                entry,
                target,
                stop,
                LocalDate.now().plusDays(5),
                70,
                signalsJson,
                "paper-test",
                LocalDateTime.now(),
                "OPEN"
        );
        Field idField = RecommendationEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
        return entity;
    }

    private PriceDailyEntity price(String ticker, BigDecimal close) {
        return new PriceDailyEntity(
                ticker,
                "NASDAQ",
                LocalDate.of(2026, 5, 29),
                close,
                close,
                close,
                close,
                BigDecimal.valueOf(1_000_000),
                close.multiply(BigDecimal.valueOf(1_000_000)),
                "TEST"
        );
    }
}
