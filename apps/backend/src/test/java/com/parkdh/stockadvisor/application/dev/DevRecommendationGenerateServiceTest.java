package com.parkdh.stockadvisor.application.dev;

import com.parkdh.stockadvisor.application.recommendation.PredictedRecommendation;
import com.parkdh.stockadvisor.application.recommendation.PricePredictor;
import com.parkdh.stockadvisor.application.recommendation.RecommendationCandidate;
import com.parkdh.stockadvisor.application.recommendation.RecommendationConfidenceService;
import com.parkdh.stockadvisor.application.recommendation.RecommendationEngine;
import com.parkdh.stockadvisor.domain.autoresearch.StrategyVersionEntity;
import com.parkdh.stockadvisor.domain.prediction.PredictionEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.global.exception.CustomException;
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.StrategyVersionRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.prediction.PredictionRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevRecommendationGenerateServiceTest {
    @Mock
    private RecommendationEngine recommendationEngine;
    @Mock
    private PricePredictor pricePredictor;
    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private RecommendationConfidenceService confidenceService;
    @Mock
    private StrategyVersionRepository strategyVersionRepository;

    private DevRecommendationGenerateService service;

    @BeforeEach
    void setUp() {
        service = new DevRecommendationGenerateService(
                recommendationEngine,
                pricePredictor,
                predictionRepository,
                recommendationRepository,
                confidenceService,
                strategyVersionRepository
        );
    }

    @Test
    void generateUsesEstimatedConfidenceInsteadOfTickerHash() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "AAPL",
                "NASDAQ",
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(1_000_000_000L),
                BigDecimal.valueOf(10_000_000L),
                "Technology",
                "market_universe",
                84,
                80,
                "{\"totalScore\":84}"
        );
        PredictedRecommendation predicted = new PredictedRecommendation(
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(130),
                BigDecimal.valueOf(118),
                LocalDate.now().plusDays(5),
                "last-price-v1"
        );
        when(recommendationEngine.selectTopCandidates("NASDAQ", 3)).thenReturn(List.of(candidate));
        when(pricePredictor.predict(any(RecommendationCandidate.class), any(String.class))).thenReturn(predicted);
        when(confidenceService.estimateConfidence("NASDAQ", "SHORT", 84)).thenReturn(62);
        when(confidenceService.estimateConfidence("NASDAQ", "LONG", 84)).thenReturn(58);
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of());
        when(predictionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate("NASDAQ", 1, 1);

        ArgumentCaptor<Iterable<RecommendationEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(recommendationRepository, org.mockito.Mockito.times(2)).saveAll(captor.capture());
        List<RecommendationEntity> saved = captor.getAllValues().stream()
                .flatMap(iterable -> {
                    java.util.ArrayList<RecommendationEntity> rows = new java.util.ArrayList<>();
                    iterable.forEach(rows::add);
                    return rows.stream();
                })
                .toList();

        assertThat(saved).extracting(RecommendationEntity::getTerm).containsExactly("SHORT", "LONG");
        assertThat(saved).extracting(RecommendationEntity::getConfidence).containsExactly(62, 58);
        assertThat(saved).allSatisfy(recommendation -> assertThat(recommendation.getSignalsJson()).contains("\"positionWeightPct\":"));
    }

    @Test
    void generateSkipsCandidateWhenPriceDataIsUnavailable() {
        RecommendationCandidate noPrice = new RecommendationCandidate(
                "NO_PRICE",
                "NASDAQ",
                null,
                null,
                null,
                "Technology",
                "market_universe",
                90,
                10,
                "{}"
        );
        RecommendationCandidate priced = new RecommendationCandidate(
                "AAPL",
                "NASDAQ",
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(1_000_000_000L),
                BigDecimal.valueOf(10_000_000L),
                "Technology",
                "market_universe",
                84,
                80,
                "{\"totalScore\":84}"
        );
        PredictedRecommendation predicted = new PredictedRecommendation(
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(130),
                BigDecimal.valueOf(118),
                LocalDate.now().plusDays(5),
                "last-price-v1"
        );
        when(recommendationEngine.selectTopCandidates("NASDAQ", 3)).thenReturn(List.of(noPrice, priced));
        when(pricePredictor.predict(noPrice, "SHORT")).thenThrow(new CustomException("가격 데이터가 없어 추천을 생성할 수 없습니다: NO_PRICE", 422));
        when(pricePredictor.predict(priced, "SHORT")).thenReturn(predicted);
        when(pricePredictor.predict(priced, "LONG")).thenReturn(predicted);
        when(confidenceService.estimateConfidence("NASDAQ", "SHORT", 84)).thenReturn(62);
        when(confidenceService.estimateConfidence("NASDAQ", "LONG", 84)).thenReturn(58);
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of());
        when(predictionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.generate("NASDAQ", 1, 1);

        assertThat(response.sourceInstrumentCount()).isEqualTo(1);
        ArgumentCaptor<Iterable<PredictionEntity>> predictionCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(predictionRepository).saveAll(predictionCaptor.capture());
        java.util.ArrayList<PredictionEntity> predictions = new java.util.ArrayList<>();
        predictionCaptor.getValue().forEach(predictions::add);
        assertThat(predictions).extracting(PredictionEntity::getTicker).containsExactly("AAPL");
    }

    @Test
    void generateUsesLatestChampionStrategyVersionWhenAvailable() {
        RecommendationCandidate candidate = new RecommendationCandidate(
                "AAPL",
                "NASDAQ",
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(1_000_000_000L),
                BigDecimal.valueOf(10_000_000L),
                "Technology",
                "market_universe",
                84,
                80,
                "{\"totalScore\":84}"
        );
        PredictedRecommendation predicted = new PredictedRecommendation(
                BigDecimal.valueOf(123),
                BigDecimal.valueOf(130),
                BigDecimal.valueOf(118),
                LocalDate.now().plusDays(5),
                "last-price-v1"
        );
        StrategyVersionEntity oldChampion = new StrategyVersionEntity("v1.0.0", "old", BigDecimal.ONE, LocalDate.now().minusDays(2).atStartOfDay(), true);
        StrategyVersionEntity latestChampion = new StrategyVersionEntity("v1.2.0", "new", BigDecimal.TEN, LocalDate.now().minusDays(1).atStartOfDay(), true);
        when(recommendationEngine.selectTopCandidates("NASDAQ", 3)).thenReturn(List.of(candidate));
        when(pricePredictor.predict(any(RecommendationCandidate.class), any(String.class))).thenReturn(predicted);
        when(confidenceService.estimateConfidence("NASDAQ", "SHORT", 84)).thenReturn(62);
        when(confidenceService.estimateConfidence("NASDAQ", "LONG", 84)).thenReturn(58);
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of(oldChampion, latestChampion));
        when(predictionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate("NASDAQ", 1, 1);

        ArgumentCaptor<Iterable<PredictionEntity>> predictionCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(predictionRepository).saveAll(predictionCaptor.capture());
        java.util.ArrayList<PredictionEntity> predictions = new java.util.ArrayList<>();
        predictionCaptor.getValue().forEach(predictions::add);
        assertThat(predictions).extracting(PredictionEntity::getModelVersion).containsExactly("v1.2.0");

        ArgumentCaptor<Iterable<RecommendationEntity>> recommendationCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(recommendationRepository, org.mockito.Mockito.times(2)).saveAll(recommendationCaptor.capture());
        List<RecommendationEntity> recommendations = recommendationCaptor.getAllValues().stream()
                .flatMap(iterable -> {
                    java.util.ArrayList<RecommendationEntity> rows = new java.util.ArrayList<>();
                    iterable.forEach(rows::add);
                    return rows.stream();
                })
                .toList();
        assertThat(recommendations).extracting(RecommendationEntity::getModelVersion).containsExactly("v1.2.0", "v1.2.0");
    }
}
