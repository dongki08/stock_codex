package com.parkdh.stockadvisor.application.backtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.backtest.dto.BacktestSimulationRequest;
import com.parkdh.stockadvisor.domain.backtest.BacktestRunEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.backtest.BacktestRunRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BacktestRunServiceTest {
    @Mock
    private BacktestRunRepository backtestRunRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private AppSettingRepository appSettingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BacktestRunService service;

    @BeforeEach
    void setUp() {
        service = new BacktestRunService(backtestRunRepository, priceDailyRepository, appSettingRepository, objectMapper);
    }

    @Test
    void simulateBacktestAppliesTradingCostAndSlippageToPnl() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        List<PriceDailyEntity> prices = buildMomentumPrices(from);
        when(priceDailyRepository.findByMarketAndTradeDateBetweenOrderByTickerAscTradeDateAsc("NASDAQ", from, from.plusDays(prices.size() - 1)))
                .thenReturn(prices);
        when(appSettingRepository.findById("backtest.slippage.percent"))
                .thenReturn(Optional.of(new AppSettingEntity("backtest.slippage.percent", "{\"value\":0.05}", "slippage", "test")));
        when(appSettingRepository.findById("backtest.cost.us"))
                .thenReturn(Optional.of(new AppSettingEntity("backtest.cost.us", "{\"secFeeEnabled\":true,\"fxSpreadPercent\":0.5}", "us cost", "test")));
        when(backtestRunRepository.save(any(BacktestRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.simulateBacktest(new BacktestSimulationRequest(
                "ma20-breakout-v0",
                "NASDAQ",
                from,
                from.plusDays(prices.size() - 1),
                1,
                1,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(50)
        ));

        JsonNode metrics = objectMapper.readTree(response.metricsJson());

        assertThat(metrics.get("roundTripCostPct").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(metrics.get("slippagePct").decimalValue()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void simulateBacktestWithRecommendationEngineV1UsesScoreBasedEntry() throws Exception {
        // TASK-1: recommendation-engine-v1도 score 기반 진입으로 동작 — selectTopCandidates 호출 없음
        LocalDate from = LocalDate.of(2025, 1, 1);
        List<PriceDailyEntity> prices = buildMomentumPrices(from);
        when(priceDailyRepository.findByMarketAndTradeDateBetweenOrderByTickerAscTradeDateAsc("NASDAQ", from, from.plusDays(prices.size() - 1)))
                .thenReturn(prices);
        when(backtestRunRepository.save(any(BacktestRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.simulateBacktest(new BacktestSimulationRequest(
                "recommendation-engine-v1",
                "NASDAQ",
                from,
                from.plusDays(prices.size() - 1),
                1,
                1,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(50)
        ));

        JsonNode metrics = objectMapper.readTree(response.metricsJson());
        assertThat(metrics.get("strategy").asText()).isEqualTo("recommendation-engine-v1");
        // 종목 선별은 이제 score 기반이라 selectedCandidateCount=0
        assertThat(metrics.get("selectedCandidateCount").asInt()).isEqualTo(0);
    }

    /**
     * 상승 모멘텀 가격 시계열 생성.
     * 초반 20봉 = 90 (낮은 점수), 21봉부터 꾸준히 상승 → score >= 60 진입 유도.
     */
    private List<PriceDailyEntity> buildMomentumPrices(LocalDate from) {
        List<PriceDailyEntity> rows = new ArrayList<>();
        // 초반 20봉: 일정 상승 (50→90) → MA 추세 형성
        for (int i = 0; i < 20; i++) {
            BigDecimal close = BigDecimal.valueOf(50 + i * 2L); // 50, 52, ..., 88
            rows.add(price(from.plusDays(i), close));
        }
        // bar 20: 명확히 MA 상회 → score >= 60
        rows.add(price(from.plusDays(20), BigDecimal.valueOf(100)));
        // 청산 바
        rows.add(price(from.plusDays(21), BigDecimal.valueOf(100)));
        rows.add(price(from.plusDays(22), BigDecimal.valueOf(110)));
        return rows;
    }

    private PriceDailyEntity price(LocalDate date, BigDecimal close) {
        return new PriceDailyEntity(
                "AAPL",
                "NASDAQ",
                date,
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
