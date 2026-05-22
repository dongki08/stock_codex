package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.application.feature.UniverseFeature;
import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineTest {
    @Mock
    private UniverseFeatureBuilder universeFeatureBuilder;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;

    private RecommendationEngine recommendationEngine;

    @BeforeEach
    void setUp() {
        lenient().when(appSettingRepository.findById(anyString())).thenReturn(Optional.empty());
        recommendationEngine = new RecommendationEngine(
                universeFeatureBuilder,
                instrumentRepository,
                appSettingRepository,
                priceDailyRepository,
                new ObjectMapper()
        );
    }

    @Test
    void selectTopCandidatesLimitsCandidatesPerSector() {
        when(appSettingRepository.findById("recommendation.sector.max"))
                .thenReturn(Optional.of(new AppSettingEntity("recommendation.sector.max", "{\"value\":2}", "sector max", "test")));
        when(universeFeatureBuilder.buildFeatures("NASDAQ"))
                .thenReturn(List.of(
                        feature("AAPL", "Technology", 95),
                        feature("MSFT", "Technology", 94),
                        feature("NVDA", "Technology", 93),
                        feature("LLY", "Healthcare", 92)
                ));

        List<RecommendationCandidate> selected = recommendationEngine.selectTopCandidates("NASDAQ", 4);

        assertThat(selected).extracting(RecommendationCandidate::ticker)
                .containsExactly("AAPL", "MSFT", "LLY");
    }

    @Test
    void buildCandidatesReturnsEmptyWhenMarketRegimeIsBelowMa200() {
        when(appSettingRepository.findById("recommendation.regime.filter.enabled"))
                .thenReturn(Optional.of(new AppSettingEntity("recommendation.regime.filter.enabled", "{\"value\":true}", "regime", "test")));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc("NASDAQ", "SPY", PageRequest.of(0, 200)))
                .thenReturn(indexHistoryBelowMa200());
        List<RecommendationCandidate> candidates = recommendationEngine.buildCandidates("NASDAQ");

        assertThat(candidates).isEmpty();
    }

    private UniverseFeature feature(String ticker, String sector, int totalScore) {
        MarketUniverseEntity entity = new MarketUniverseEntity(
                ticker,
                "NASDAQ",
                ticker,
                sector,
                BigDecimal.valueOf(2_000_000_000L),
                BigDecimal.valueOf(20_000_000L),
                BigDecimal.valueOf(100),
                true,
                "TEST",
                LocalDate.now()
        );
        return new UniverseFeature(entity, 90, 90, 90, 90, 90, 90, 90, 20, totalScore, "{\"totalScore\":" + totalScore + "}");
    }

    private List<PriceDailyEntity> indexHistoryBelowMa200() {
        List<PriceDailyEntity> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        rows.add(price("SPY", today, BigDecimal.valueOf(80)));
        for (int i = 1; i < 200; i++) {
            rows.add(price("SPY", today.minusDays(i), BigDecimal.valueOf(100)));
        }
        return rows;
    }

    private PriceDailyEntity price(String ticker, LocalDate date, BigDecimal close) {
        return new PriceDailyEntity(
                ticker,
                "NASDAQ",
                date,
                close,
                close,
                close,
                close,
                BigDecimal.ONE,
                close,
                "TEST"
        );
    }
}
