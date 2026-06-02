package com.parkdh.stockadvisor.application.recommendation;

import com.parkdh.stockadvisor.global.exception.CustomException;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PricePredictorTest {
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private AppSettingRepository appSettingRepository;

    private PricePredictor pricePredictor;

    @BeforeEach
    void setUp() {
        lenient().when(appSettingRepository.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        pricePredictor = new PricePredictor(priceDailyRepository, appSettingRepository, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void predictRejectsCandidateWithoutDailyHistoryOrLastPrice() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "NO_PRICE",
                "NoPrice Inc.",
                "NASDAQ",
                null,
                null,
                null,
                "Technology",
                "market_universe",
                80,
                20,
                "{}"
        );
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "NO_PRICE", PageRequest.of(0, 20)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> pricePredictor.predict(candidate, "SHORT"))
                .isInstanceOf(CustomException.class)
                .hasMessage("가격 데이터가 없어 추천을 생성할 수 없습니다: NO_PRICE")
                .extracting("code")
                .isEqualTo(422);
    }

    @Test
    void predictUsesCandidateLastPriceWhenDailyHistoryIsMissing() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                BigDecimal.valueOf(123.45678),
                null,
                null,
                "Technology",
                "market_universe",
                80,
                60,
                "{}"
        );
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 20)))
                .thenReturn(List.of());

        PredictedRecommendation predicted = pricePredictor.predict(candidate, "SHORT");

        assertThat(predicted.entryPrice()).isEqualByComparingTo("123.4568");
        assertThat(predicted.pricingMethod()).isEqualTo("last-price-v1-cost-adjusted");
        assertThat(predicted.volatilityPct()).isEqualByComparingTo("2.0000");
        assertThat(predicted.positionSizingScore()).isEqualByComparingTo("40.00000000");
    }

    @Test
    void predictGivesHigherSizingScoreToLowerVolatilityCandidate() {
        RecommendationCandidate lowVolatility = new RecommendationCandidate(
                "LOW",
                "Low Vol Inc.",
                "NASDAQ",
                BigDecimal.valueOf(100),
                null,
                null,
                "Technology",
                "market_universe",
                80,
                60,
                "{}"
        );
        RecommendationCandidate highVolatility = new RecommendationCandidate(
                "HIGH",
                "High Vol Inc.",
                "NASDAQ",
                BigDecimal.valueOf(100),
                null,
                null,
                "Technology",
                "market_universe",
                80,
                60,
                "{}"
        );
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "LOW", PageRequest.of(0, 20)))
                .thenReturn(history("LOW", List.of(100, 101, 100, 101, 100, 101)));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "HIGH", PageRequest.of(0, 20)))
                .thenReturn(history("HIGH", List.of(100, 108, 100, 108, 100, 108)));

        PredictedRecommendation low = pricePredictor.predict(lowVolatility, "SHORT");
        PredictedRecommendation high = pricePredictor.predict(highVolatility, "SHORT");

        assertThat(low.volatilityPct()).isLessThan(high.volatilityPct());
        assertThat(low.positionSizingScore()).isGreaterThan(high.positionSizingScore());
    }

    @Test
    void predictAdjustsTargetAndStopForTradingCostAndSlippage() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                BigDecimal.valueOf(100),
                null,
                null,
                "Technology",
                "market_universe",
                80,
                60,
                "{}"
        );
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "AAPL", PageRequest.of(0, 20)))
                .thenReturn(List.of());
        when(appSettingRepository.findById("backtest.slippage.percent"))
                .thenReturn(Optional.of(new AppSettingEntity("backtest.slippage.percent", "{\"value\":0.05}", "slippage", "test")));
        when(appSettingRepository.findById("backtest.cost.us"))
                .thenReturn(Optional.of(new AppSettingEntity("backtest.cost.us", "{\"secFeeEnabled\":true,\"fxSpreadPercent\":0.5}", "us cost", "test")));

        PredictedRecommendation predicted = pricePredictor.predict(candidate, "SHORT");

        assertThat(predicted.targetPrice()).isGreaterThan(BigDecimal.valueOf(105));
        assertThat(predicted.stopPrice()).isGreaterThan(BigDecimal.valueOf(97));
        assertThat(predicted.pricingMethod()).isEqualTo("last-price-v1-cost-adjusted");
    }

    private List<PriceDailyEntity> history(String ticker, List<Integer> closes) {
        List<PriceDailyEntity> rows = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = closes.size() - 1; i >= 0; i--) {
            BigDecimal close = BigDecimal.valueOf(closes.get(i));
            rows.add(new PriceDailyEntity(
                    ticker,
                    "NASDAQ",
                    start.plusDays(i),
                    close,
                    close.add(BigDecimal.ONE),
                    close.subtract(BigDecimal.ONE),
                    close,
                    BigDecimal.valueOf(1_000_000),
                    close.multiply(BigDecimal.valueOf(1_000_000)),
                    "TEST"
            ));
        }
        return rows;
    }
}
