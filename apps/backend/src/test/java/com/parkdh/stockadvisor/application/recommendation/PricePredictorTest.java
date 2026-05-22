package com.parkdh.stockadvisor.application.recommendation;

import com.parkdh.stockadvisor.global.exception.CustomException;
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
    }

    @Test
    void predictAdjustsTargetAndStopForTradingCostAndSlippage() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "AAPL",
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
}
