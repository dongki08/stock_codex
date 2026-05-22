package com.parkdh.stockadvisor.application.backtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.backtest.dto.BacktestSimulationRequest;
import com.parkdh.stockadvisor.application.recommendation.RecommendationCandidate;
import com.parkdh.stockadvisor.application.recommendation.RecommendationEngine;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BacktestRunServiceTest {
    @Mock
    private BacktestRunRepository backtestRunRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private RecommendationEngine recommendationEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BacktestRunService service;

    @BeforeEach
    void setUp() {
        service = new BacktestRunService(backtestRunRepository, priceDailyRepository, appSettingRepository, recommendationEngine, objectMapper);
    }

    @Test
    void simulateBacktestAppliesTradingCostAndSlippageToPnl() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        List<PriceDailyEntity> prices = buildBreakoutPrices(from);
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

        assertThat(metrics.get("totalPnlPct").decimalValue()).isLessThan(BigDecimal.valueOf(10));
        assertThat(metrics.get("roundTripCostPct").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(metrics.get("slippagePct").decimalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(metrics.get("sampleTrades").get(0).get("pnlPct").decimalValue()).isEqualByComparingTo(metrics.get("totalPnlPct").decimalValue());
    }

    @Test
    void simulateBacktestCanUseRecommendationEngineSelectedTickers() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        List<PriceDailyEntity> selectedPrices = buildBreakoutPrices(from);
        List<PriceDailyEntity> otherPrices = buildBreakoutPrices(from).stream()
                .map(row -> new PriceDailyEntity("MSFT", "NASDAQ", row.getTradeDate(), row.getOpenPrice(), row.getHighPrice(), row.getLowPrice(), row.getClosePrice(), row.getVolume(), row.getTurnover(), "TEST"))
                .toList();
        List<PriceDailyEntity> allPrices = new ArrayList<>();
        allPrices.addAll(selectedPrices);
        allPrices.addAll(otherPrices);
        when(recommendationEngine.selectTopCandidates("NASDAQ", 1)).thenReturn(List.of(candidate("AAPL")));
        when(priceDailyRepository.findByMarketAndTradeDateBetweenOrderByTickerAscTradeDateAsc("NASDAQ", from, from.plusDays(selectedPrices.size() - 1)))
                .thenReturn(allPrices);
        when(backtestRunRepository.save(any(BacktestRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.simulateBacktest(new BacktestSimulationRequest(
                "recommendation-engine-v1",
                "NASDAQ",
                from,
                from.plusDays(selectedPrices.size() - 1),
                1,
                1,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(50)
        ));

        JsonNode metrics = objectMapper.readTree(response.metricsJson());
        assertThat(metrics.get("strategy").asText()).isEqualTo("recommendation-engine-v1");
        assertThat(metrics.get("selectedCandidateCount").asInt()).isEqualTo(1);
        assertThat(metrics.get("sampleTrades").get(0).get("ticker").asText()).isEqualTo("AAPL");
        verify(recommendationEngine).selectTopCandidates("NASDAQ", 1);
    }

    private List<PriceDailyEntity> buildBreakoutPrices(LocalDate from) {
        List<PriceDailyEntity> rows = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rows.add(price(from.plusDays(i), BigDecimal.valueOf(90)));
        }
        rows.add(price(from.plusDays(20), BigDecimal.valueOf(100)));
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

    private RecommendationCandidate candidate(String ticker) {
        return new RecommendationCandidate(
                ticker,
                "NASDAQ",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1_000_000_000L),
                BigDecimal.valueOf(10_000_000L),
                "Technology",
                "test",
                90,
                90,
                "{\"totalScore\":90}"
        );
    }
}
