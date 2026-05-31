package com.parkdh.stockadvisor.application.dev;

import com.parkdh.stockadvisor.api.dev.dto.DevRecommendationGenerateResponse;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DevRecommendationGenerateService {
    private static final String MODEL_VERSION = "dev-rule-v0";

    private final RecommendationEngine recommendationEngine;
    private final PricePredictor pricePredictor;
    private final PredictionRepository predictionRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationConfidenceService recommendationConfidenceService;
    private final StrategyVersionRepository strategyVersionRepository;

    @Transactional
    public DevRecommendationGenerateResponse generate(String market, Integer shortCount, Integer longCount) {
        int safeShortCount = normalizeCount(shortCount, 3, "short recommendation count");
        int safeLongCount = normalizeCount(longCount, 3, "long recommendation count");
        int candidateLimit = Math.max(safeShortCount, safeLongCount) * 3;
        List<RecommendationCandidate> candidates = recommendationEngine.selectTopCandidates(market, candidateLimit);
        if (candidates.isEmpty()) {
            throw new CustomException("No recommendation candidates are available. Seed or collect universe data first.", 404);
        }
        Map<String, PredictedRecommendation> shortPredictionCache = new LinkedHashMap<>();
        List<RecommendationCandidate> pricedCandidates = candidates.stream()
                .filter(candidate -> cacheShortPrediction(candidate, shortPredictionCache))
                .toList();
        if (pricedCandidates.isEmpty()) {
            throw new CustomException("No recommendation candidates have usable price data.", 422);
        }

        String modelVersion = resolveModelVersion();
        List<PredictionEntity> predictions = predictionRepository.saveAll(
                pricedCandidates.stream().map(candidate -> createPrediction(candidate, modelVersion, shortPredictionCache.get(candidate.ticker()))).toList());
        List<RecommendationDraft> shortDrafts = normalizePositionWeights(pricedCandidates.stream()
                .limit(safeShortCount)
                .map(candidate -> createRecommendationDraft(candidate, "SHORT", modelVersion, shortPredictionCache.get(candidate.ticker())))
                .toList());
        List<RecommendationDraft> longDrafts = normalizePositionWeights(pricedCandidates.stream()
                .limit(safeLongCount)
                .map(candidate -> createRecommendationDraft(candidate, "LONG", modelVersion, null))
                .toList());
        List<RecommendationEntity> shortRecommendations = recommendationRepository.saveAll(
                shortDrafts.stream().map(this::createRecommendation).toList());
        List<RecommendationEntity> longRecommendations = recommendationRepository.saveAll(
                longDrafts.stream().map(this::createRecommendation).toList());

        List<Long> predictionIds = predictions.stream().map(PredictionEntity::getId).toList();
        List<Long> recommendationIds = joinRecommendationIds(shortRecommendations, longRecommendations);
        return new DevRecommendationGenerateResponse(
                market == null || market.isBlank() ? "ALL" : market,
                pricedCandidates.size(),
                predictionIds.size(),
                recommendationIds.size(),
                predictionIds,
                recommendationIds
        );
    }

    private int normalizeCount(Integer count, int defaultValue, String label) {
        int value = count == null ? defaultValue : count;
        if (value < 1 || value > 10) {
            throw new CustomException(label + " must be between 1 and 10.", 400);
        }
        return value;
    }

    private PredictionEntity createPrediction(RecommendationCandidate candidate, String modelVersion, PredictedRecommendation predicted) {
        return new PredictionEntity(candidate.ticker(), 5, predicted.targetPrice(), modelVersion, LocalDateTime.now());
    }

    private boolean cacheShortPrediction(RecommendationCandidate candidate, Map<String, PredictedRecommendation> shortPredictionCache) {
        try {
            shortPredictionCache.put(candidate.ticker(), pricePredictor.predict(candidate, "SHORT"));
            return true;
        } catch (CustomException exception) {
            if (exception.getCode() == 422) {
                return false;
            }
            throw exception;
        }
    }

    private RecommendationDraft createRecommendationDraft(RecommendationCandidate candidate, String term, String modelVersion, PredictedRecommendation cachedPrediction) {
        PredictedRecommendation predicted = cachedPrediction == null ? pricePredictor.predict(candidate, term) : cachedPrediction;
        Integer confidence = recommendationConfidenceService.estimateConfidence(candidate.market(), term, candidate.score());
        return new RecommendationDraft(candidate, term, modelVersion, predicted, confidence, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private RecommendationEntity createRecommendation(RecommendationDraft draft) {
        RecommendationCandidate candidate = draft.candidate();
        PredictedRecommendation predicted = draft.predicted();
        String signalsJson = buildSignalsJson(candidate, draft.term(), predicted, draft.confidence(), draft.positionWeightPct());
        return new RecommendationEntity(
                candidate.ticker(),
                candidate.market(),
                draft.term(),
                predicted.entryPrice(),
                predicted.targetPrice(),
                predicted.stopPrice(),
                predicted.expectedExitAt(),
                draft.confidence(),
                signalsJson,
                draft.modelVersion(),
                LocalDateTime.now(),
                "OPEN"
        );
    }

    private String resolveModelVersion() {
        return strategyVersionRepository.findByChampion(true).stream()
                .max(Comparator.comparing(StrategyVersionEntity::getPromotedAt))
                .map(StrategyVersionEntity::getSemver)
                .orElse(MODEL_VERSION);
    }

    private List<RecommendationDraft> normalizePositionWeights(List<RecommendationDraft> drafts) {
        BigDecimal totalRawWeight = drafts.stream()
                .map(this::rawPositionWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRawWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return drafts;
        }
        return drafts.stream()
                .map(draft -> draft.withPositionWeightPct(rawPositionWeight(draft)
                        .divide(totalRawWeight, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .min(BigDecimal.valueOf(20))
                        .setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private BigDecimal rawPositionWeight(RecommendationDraft draft) {
        BigDecimal confidenceScale = BigDecimal.valueOf(Math.max(0, Math.min(100, draft.confidence() == null ? 50 : draft.confidence())))
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        return draft.predicted().positionSizingScore().multiply(confidenceScale);
    }

    private String buildSignalsJson(RecommendationCandidate candidate, String term, PredictedRecommendation predicted, Integer confidence, BigDecimal positionWeightPct) {
        return "{\"generatedBy\":\"dev-rule-v0\",\"source\":\"" + candidate.source()
                + "\",\"ticker\":\"" + candidate.ticker()
                + "\",\"term\":\"" + term
                + "\",\"reason\":\"market_universe feature score based recommendation\""
                + ",\"featureScore\":" + candidate.score()
                + ",\"marketCap\":" + candidate.marketCap()
                + ",\"avgTurnover\":" + candidate.avgTurnover()
                + ",\"pricingMethod\":\"" + predicted.pricingMethod()
                + "\",\"volatilityPct\":" + predicted.volatilityPct()
                + ",\"positionSizingScore\":" + predicted.positionSizingScore()
                + ",\"positionWeightPct\":" + positionWeightPct
                + ",\"featureJson\":" + candidate.featureJson()
                + "}";
    }

    private List<Long> joinRecommendationIds(List<RecommendationEntity> shortRecommendations, List<RecommendationEntity> longRecommendations) {
        return java.util.stream.Stream.concat(shortRecommendations.stream(), longRecommendations.stream())
                .map(RecommendationEntity::getId)
                .toList();
    }

    private record RecommendationDraft(RecommendationCandidate candidate, String term, String modelVersion, PredictedRecommendation predicted, Integer confidence, BigDecimal positionWeightPct) {
        private RecommendationDraft withPositionWeightPct(BigDecimal positionWeightPct) {
            return new RecommendationDraft(candidate, term, modelVersion, predicted, confidence, positionWeightPct);
        }
    }
}
