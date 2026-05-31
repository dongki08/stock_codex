package com.parkdh.stockadvisor.application.feature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity;
import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniverseFeatureBuilderTest {
    @Mock
    private MarketUniverseRepository marketUniverseRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private DisclosureEventRepository disclosureEventRepository;
    @Mock
    private MacroObservationRepository macroObservationRepository;
    @Mock
    private FundamentalMetricRepository fundamentalMetricRepository;
    @Mock
    private AppSettingRepository appSettingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UniverseFeatureBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new UniverseFeatureBuilder(
                marketUniverseRepository,
                priceDailyRepository,
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                appSettingRepository,
                objectMapper
        );
    }

    @Test
    void buildFeaturesIncludesMacdAndBollingerInTechnicalScore() throws Exception {
        MarketUniverseEntity universe = new MarketUniverseEntity(
                "AAPL",
                "NASDAQ",
                "Apple",
                "Technology",
                BigDecimal.valueOf(2_000_000_000_000L),
                BigDecimal.valueOf(5_000_000_000L),
                BigDecimal.valueOf(160),
                true,
                "TEST",
                LocalDate.now()
        );
        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull("NASDAQ", true)).thenReturn(List.of(universe));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(eq("NASDAQ"), eq("AAPL"), any(Pageable.class)))
                .thenReturn(uptrendHistory());
        when(newsArticleRepository.findByMarketAndTickerOrderByPublishedAtDesc(eq("NASDAQ"), eq("AAPL"), any(Pageable.class))).thenReturn(List.<NewsArticleEntity>of());
        when(disclosureEventRepository.findByMarketAndTickerOrderByDisclosedAtDesc(eq("NASDAQ"), eq("AAPL"), any(Pageable.class))).thenReturn(List.<DisclosureEventEntity>of());
        when(macroObservationRepository.findAllByOrderByObservedDateDesc(any(Pageable.class))).thenReturn(List.<MacroObservationEntity>of());
        when(fundamentalMetricRepository.findByMarketAndTickerOrderByPeriodEndDesc(eq("NASDAQ"), eq("AAPL"), any(Pageable.class))).thenReturn(List.<FundamentalMetricEntity>of());
        when(appSettingRepository.findById("recommendation.scoring.weights")).thenReturn(Optional.of(new AppSettingEntity(
                "recommendation.scoring.weights",
                "{\"value\":{\"liquidity\":0,\"price\":0,\"technical\":1,\"context\":0,\"fundamental\":0,\"dataQuality\":0},\"technical\":{\"ma\":0,\"rsi\":0,\"volume\":0,\"macd\":1,\"bollinger\":0},\"context\":{\"news\":0,\"disclosure\":0,\"macro\":0,\"fundamental\":0}}",
                "weights",
                "test"
        )));

        UniverseFeature feature = builder.buildFeatures("NASDAQ").get(0);
        JsonNode featureJson = objectMapper.readTree(feature.featureJson());

        assertThat(featureJson.get("macdScore").asInt()).isBetween(0, 100);
        assertThat(featureJson.get("bollingerScore").asInt()).isBetween(0, 100);
        assertThat(feature.technicalScore()).isEqualTo(featureJson.get("macdScore").asInt());
        assertThat(feature.totalScore()).isEqualTo(featureJson.get("macdScore").asInt());
    }

    @Test
    void buildFeaturesNormalizesScoresWithinMarketCrossSection() throws Exception {
        MarketUniverseEntity strong = universe("AAA");
        MarketUniverseEntity middle = universe("BBB");
        MarketUniverseEntity weak = universe("CCC");
        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull("NASDAQ", true)).thenReturn(List.of(strong, middle, weak));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(eq("NASDAQ"), eq("AAA"), any(Pageable.class)))
                .thenReturn(history("AAA", strongTrendCloses()));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(eq("NASDAQ"), eq("BBB"), any(Pageable.class)))
                .thenReturn(history("BBB", middleTrendCloses()));
        when(priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(eq("NASDAQ"), eq("CCC"), any(Pageable.class)))
                .thenReturn(history("CCC", weakTrendCloses()));
        when(newsArticleRepository.findByMarketAndTickerOrderByPublishedAtDesc(eq("NASDAQ"), any(), any(Pageable.class))).thenReturn(List.<NewsArticleEntity>of());
        when(disclosureEventRepository.findByMarketAndTickerOrderByDisclosedAtDesc(eq("NASDAQ"), any(), any(Pageable.class))).thenReturn(List.<DisclosureEventEntity>of());
        when(macroObservationRepository.findAllByOrderByObservedDateDesc(any(Pageable.class))).thenReturn(List.<MacroObservationEntity>of());
        when(fundamentalMetricRepository.findByMarketAndTickerOrderByPeriodEndDesc(eq("NASDAQ"), any(), any(Pageable.class))).thenReturn(List.<FundamentalMetricEntity>of());
        when(appSettingRepository.findById("recommendation.scoring.weights")).thenReturn(Optional.of(new AppSettingEntity(
                "recommendation.scoring.weights",
                "{\"value\":{\"liquidity\":0,\"price\":0,\"technical\":1,\"context\":0,\"fundamental\":0,\"dataQuality\":0},\"technical\":{\"ma\":1,\"rsi\":0,\"volume\":0,\"macd\":0,\"bollinger\":0},\"context\":{\"news\":0,\"disclosure\":0,\"macro\":0,\"fundamental\":0}}",
                "weights",
                "test"
        )));

        List<UniverseFeature> features = builder.buildFeatures("NASDAQ");
        JsonNode topJson = objectMapper.readTree(features.get(0).featureJson());
        JsonNode bottomJson = objectMapper.readTree(features.get(2).featureJson());

        assertThat(features).extracting(UniverseFeature::ticker).containsExactly("AAA", "BBB", "CCC");
        assertThat(features.get(0).totalScore()).isGreaterThan(features.get(1).totalScore());
        assertThat(features.get(1).totalScore()).isGreaterThan(features.get(2).totalScore());
        assertThat(topJson.get("crossSectionalNormalized").asBoolean()).isTrue();
        assertThat(topJson.get("rawMovingAverageScore").asInt()).isEqualTo(92);
        assertThat(topJson.get("movingAverageScore").asInt()).isEqualTo(95);
        assertThat(bottomJson.get("rawMovingAverageScore").asInt()).isEqualTo(45);
        assertThat(bottomJson.get("movingAverageScore").asInt()).isEqualTo(35);
    }

    private MarketUniverseEntity universe(String ticker) {
        return new MarketUniverseEntity(
                ticker,
                "NASDAQ",
                ticker,
                "Technology",
                BigDecimal.valueOf(2_000_000_000L),
                BigDecimal.valueOf(5_000_000_000L),
                BigDecimal.valueOf(160),
                true,
                "TEST",
                LocalDate.now()
        );
    }

    private List<PriceDailyEntity> uptrendHistory() {
        return history("AAPL", strongTrendCloses());
    }

    private List<BigDecimal> strongTrendCloses() {
        List<BigDecimal> closes = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            closes.add(BigDecimal.valueOf(100 + i));
        }
        return closes;
    }

    private List<BigDecimal> middleTrendCloses() {
        List<BigDecimal> closes = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            closes.add(BigDecimal.valueOf(100));
        }
        for (int i = 0; i < 15; i++) {
            closes.add(BigDecimal.valueOf(100));
        }
        for (int i = 0; i < 4; i++) {
            closes.add(BigDecimal.valueOf(80));
        }
        closes.add(BigDecimal.valueOf(100));
        return closes;
    }

    private List<BigDecimal> weakTrendCloses() {
        List<BigDecimal> closes = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            closes.add(BigDecimal.valueOf(100));
        }
        for (int i = 0; i < 19; i++) {
            closes.add(BigDecimal.valueOf(100));
        }
        closes.add(BigDecimal.valueOf(80));
        return closes;
    }

    private List<PriceDailyEntity> history(String ticker, List<BigDecimal> closes) {
        List<PriceDailyEntity> rows = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(59);
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal close = closes.get(i);
            BigDecimal high = close.add(BigDecimal.valueOf(2));
            BigDecimal low = close.subtract(BigDecimal.valueOf(2));
            rows.add(new PriceDailyEntity(
                    ticker,
                    "NASDAQ",
                    start.plusDays(i),
                    close.subtract(BigDecimal.ONE),
                    high,
                    low,
                    close,
                    BigDecimal.valueOf(1_000_000 + i * 10_000L),
                    close.multiply(BigDecimal.valueOf(1_000_000)),
                    "TEST"
            ));
        }
        return rows.reversed();
    }
}
