package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationConfidenceServiceTest {
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private RecommendationRepository recommendationRepository;

    private RecommendationConfidenceService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationConfidenceService(evaluationRepository, recommendationRepository, new ObjectMapper());
    }

    @Test
    void estimateConfidenceReturnsNeutralWhenSampleIsTooSmall() {
        when(evaluationRepository.findAll()).thenReturn(List.of(evaluation(1L, true)));
        when(recommendationRepository.findAll()).thenReturn(List.of(recommendation(1L, "NASDAQ", "SHORT", 82)));

        int confidence = service.estimateConfidence("NASDAQ", "SHORT", 80);

        assertThat(confidence).isEqualTo(50);
    }

    @Test
    void estimateConfidenceUsesHitRateForSameMarketTermAndScoreBand() {
        List<EvaluationEntity> evaluations = new ArrayList<>();
        List<RecommendationEntity> recommendations = new ArrayList<>();
        for (long id = 1; id <= 30; id++) {
            boolean hit = id <= 21;
            evaluations.add(evaluation(id, hit));
            recommendations.add(recommendation(id, "NASDAQ", "SHORT", 82));
        }

        when(evaluationRepository.findAll()).thenReturn(evaluations);
        when(recommendationRepository.findAll()).thenReturn(recommendations);

        int confidence = service.estimateConfidence("NASDAQ", "SHORT", 80);

        assertThat(confidence).isEqualTo(70);
    }

    private EvaluationEntity evaluation(Long recommendationId, boolean hitTarget) {
        return new EvaluationEntity(
                recommendationId,
                BigDecimal.TEN,
                hitTarget ? "TARGET_HIT" : "STOP_HIT",
                hitTarget ? BigDecimal.ONE : BigDecimal.valueOf(-1),
                null,
                hitTarget,
                LocalDateTime.now()
        );
    }

    private RecommendationEntity recommendation(Long id, String market, String term, int featureScore) {
        RecommendationEntity recommendation = new RecommendationEntity(
                "AAPL",
                market,
                term,
                BigDecimal.TEN,
                BigDecimal.valueOf(11),
                BigDecimal.valueOf(9),
                LocalDate.now().plusDays(5),
                70,
                "{\"featureScore\":" + featureScore + "}",
                "dev-rule-v0",
                LocalDateTime.now(),
                "CLOSED"
        );
        ReflectionTestUtils.setField(recommendation, "id", id);
        return recommendation;
    }
}
