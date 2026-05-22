package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RecommendationConfidenceService {
    private static final int MIN_SAMPLE_SIZE = 30;
    private static final int SCORE_BAND_WIDTH = 10;

    private final EvaluationRepository evaluationRepository;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public int estimateConfidence(String market, String term, Integer featureScore) {
        if (market == null || term == null || featureScore == null) {
            return 50;
        }

        int targetBand = scoreBand(featureScore);
        Map<Long, RecommendationEntity> recommendations = recommendationRepository.findAll().stream()
                .collect(Collectors.toMap(RecommendationEntity::getId, recommendation -> recommendation, (left, right) -> left));

        Sample sample = evaluationRepository.findAll().stream()
                .map(evaluation -> new EvaluatedRecommendation(evaluation, recommendations.get(evaluation.getRecommendationId())))
                .filter(row -> row.recommendation() != null)
                .filter(row -> market.equals(row.recommendation().getMarket()))
                .filter(row -> term.equals(row.recommendation().getTerm()))
                .filter(row -> scoreBand(extractFeatureScore(row.recommendation().getSignalsJson())) == targetBand)
                .reduce(new Sample(0, 0), (sampleAcc, row) -> sampleAcc.add(row.evaluation().getHitTarget()), Sample::combine);

        if (sample.count() < MIN_SAMPLE_SIZE) {
            return 50;
        }
        return Math.toIntExact(Math.round(sample.hits() * 100.0 / sample.count()));
    }

    private int extractFeatureScore(String signalsJson) {
        try {
            JsonNode root = objectMapper.readTree(signalsJson);
            return root.path("featureScore").asInt(-1);
        } catch (Exception exception) {
            return -1;
        }
    }

    private int scoreBand(int score) {
        if (score < 0) {
            return -1;
        }
        return score / SCORE_BAND_WIDTH;
    }

    private record EvaluatedRecommendation(EvaluationEntity evaluation, RecommendationEntity recommendation) {
    }

    private record Sample(int count, int hits) {
        private Sample add(Boolean hitTarget) {
            return new Sample(count + 1, hits + (Boolean.TRUE.equals(hitTarget) ? 1 : 0));
        }

        private Sample combine(Sample other) {
            return new Sample(count + other.count, hits + other.hits);
        }
    }
}
