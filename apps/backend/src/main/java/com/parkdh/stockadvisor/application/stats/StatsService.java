package com.parkdh.stockadvisor.application.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.stats.dto.StatsDailyResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse.PaperPosition;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse.TermStats;
import com.parkdh.stockadvisor.api.stats.dto.StatsStrategyResponse;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StatsService {
    private final EvaluationRepository evaluationRepository;
    private final RecommendationRepository recommendationRepository;
    private final PriceDailyRepository priceDailyRepository;
    private final ObjectMapper objectMapper;

    public StatsSummaryResponse getSummary() {
        List<EvaluationEntity> evals = evaluationRepository.findAll();
        List<RecommendationEntity> recs = recommendationRepository.findAll();

        int total = recs.size();
        int closed = (int) recs.stream().filter(r -> "CLOSED".equals(r.getStatus())).count();
        int open = (int) recs.stream().filter(r -> "OPEN".equals(r.getStatus())).count();
        int expired = (int) recs.stream().filter(r -> "EXPIRED".equals(r.getStatus())).count();

        if (evals.isEmpty()) {
            return new StatsSummaryResponse(total, closed, open, expired, 0.0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), Map.of());
        }

        long hitCount = evals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
        double hitRate = Math.round(hitCount * 1000.0 / evals.size()) / 10.0;

        BigDecimal totalPnl = evals.stream().map(EvaluationEntity::getPnlPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPnl = totalPnl.divide(BigDecimal.valueOf(evals.size()), 4, RoundingMode.HALF_UP);

        BigDecimal maxDrawdown = evals.stream()
                .map(e -> e.getDrawdownPct() != null ? e.getDrawdownPct() : BigDecimal.ZERO)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<Long, String> recTermMap = recs.stream()
                .collect(Collectors.toMap(RecommendationEntity::getId, RecommendationEntity::getTerm));

        Map<String, TermStats> byTerm = evals.stream()
                .collect(Collectors.groupingBy(e -> recTermMap.getOrDefault(e.getRecommendationId(), "UNKNOWN")))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> buildTermStats(entry.getValue())));

        Map<String, Integer> byExitReason = evals.stream()
                .collect(Collectors.groupingBy(EvaluationEntity::getExitReason,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        return new StatsSummaryResponse(total, closed, open, expired, hitRate, avgPnl, totalPnl, maxDrawdown, byTerm, byExitReason);
    }

    public List<StatsDailyResponse> getDaily() {
        List<EvaluationEntity> evals = evaluationRepository.findAll();
        List<DailyStatsBucket> buckets = evals.stream()
                .collect(Collectors.groupingBy(e -> e.getEvaluatedAt().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<EvaluationEntity>>comparingByKey())
                .map(entry -> {
                    List<EvaluationEntity> dayEvals = entry.getValue();
                    int count = dayEvals.size();
                    int hits = (int) dayEvals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
                    BigDecimal total = dayEvals.stream().map(EvaluationEntity::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
                    return new DailyStatsBucket(entry.getKey().toString(), count, hits, avg, total);
                })
                .toList();

        BigDecimal cumulative = BigDecimal.ZERO;
        List<StatsDailyResponse> responses = new ArrayList<>();
        for (DailyStatsBucket bucket : buckets) {
            cumulative = cumulative.add(bucket.totalPnlPct());
            responses.add(new StatsDailyResponse(
                    bucket.date(),
                    bucket.count(),
                    bucket.hitCount(),
                    bucket.avgPnlPct(),
                    bucket.totalPnlPct(),
                    cumulative
            ));
        }
        Collections.reverse(responses);
        return responses.stream().limit(30).toList();
    }

    public List<StatsStrategyResponse> getByStrategy() {
        List<EvaluationEntity> evals = evaluationRepository.findAll();
        List<RecommendationEntity> recs = recommendationRepository.findAll();

        Map<Long, String> recModelMap = recs.stream()
                .collect(Collectors.toMap(RecommendationEntity::getId, RecommendationEntity::getModelVersion));

        return evals.stream()
                .collect(Collectors.groupingBy(e -> recModelMap.getOrDefault(e.getRecommendationId(), "UNKNOWN")))
                .entrySet().stream()
                .map(entry -> {
                    List<EvaluationEntity> stratEvals = entry.getValue();
                    int count = stratEvals.size();
                    long hits = stratEvals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
                    double hitRate = Math.round(hits * 1000.0 / count) / 10.0;
                    BigDecimal avg = stratEvals.stream().map(EvaluationEntity::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
                    return new StatsStrategyResponse(entry.getKey(), count, hitRate, avg);
                })
                .sorted(Comparator.comparing(StatsStrategyResponse::count).reversed())
                .toList();
    }

    public StatsPaperTradingResponse getPaperTrading() {
        List<RecommendationEntity> openRecommendations = recommendationRepository.findByStatus("OPEN").stream()
                .sorted(Comparator.comparing(RecommendationEntity::getGeneratedAt).reversed())
                .toList();
        if (openRecommendations.isEmpty()) {
            return new StatsPaperTradingResponse(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, List.of());
        }

        BigDecimal fallbackWeight = BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(openRecommendations.size()), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(20));
        List<PaperPosition> positions = openRecommendations.stream()
                .map(recommendation -> buildPaperPosition(recommendation, fallbackWeight))
                .toList();

        List<PaperPosition> pricedPositions = positions.stream()
                .filter(position -> position.unrealizedPnlPct() != null)
                .toList();
        BigDecimal avgUnrealizedPnlPct = pricedPositions.isEmpty()
                ? BigDecimal.ZERO
                : pricedPositions.stream()
                        .map(PaperPosition::unrealizedPnlPct)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(pricedPositions.size()), 4, RoundingMode.HALF_UP);
        BigDecimal totalWeightPct = pricedPositions.stream()
                .map(PaperPosition::positionWeightPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal weightedUnrealizedPnlPct = pricedPositions.stream()
                .map(PaperPosition::weightedPnlPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        int targetTouchCount = (int) pricedPositions.stream()
                .filter(position -> position.currentPrice().compareTo(position.targetPrice()) >= 0)
                .count();
        int stopTouchCount = (int) pricedPositions.stream()
                .filter(position -> position.currentPrice().compareTo(position.stopPrice()) <= 0)
                .count();
        return new StatsPaperTradingResponse(
                openRecommendations.size(),
                pricedPositions.size(),
                avgUnrealizedPnlPct,
                weightedUnrealizedPnlPct,
                totalWeightPct,
                targetTouchCount,
                stopTouchCount,
                positions
        );
    }

    private PaperPosition buildPaperPosition(RecommendationEntity recommendation, BigDecimal fallbackWeight) {
        BigDecimal positionWeightPct = extractPositionWeightPct(recommendation.getSignalsJson()).orElse(fallbackWeight);
        return priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(recommendation.getMarket(), recommendation.getTicker(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(price -> buildPricedPaperPosition(recommendation, price, positionWeightPct))
                .orElseGet(() -> new PaperPosition(
                        recommendation.getId(),
                        recommendation.getTicker(),
                        recommendation.getMarket(),
                        recommendation.getTerm(),
                        recommendation.getEntryPrice(),
                        null,
                        null,
                        recommendation.getTargetPrice(),
                        recommendation.getStopPrice(),
                        recommendation.getConfidence(),
                        positionWeightPct,
                        null,
                        null,
                        null,
                        null,
                        "NO_PRICE"
                ));
    }

    private PaperPosition buildPricedPaperPosition(RecommendationEntity recommendation, PriceDailyEntity price, BigDecimal positionWeightPct) {
        BigDecimal currentPrice = price.getClosePrice();
        BigDecimal unrealizedPnlPct = currentPrice.subtract(recommendation.getEntryPrice())
                .divide(recommendation.getEntryPrice(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal weightedPnlPct = unrealizedPnlPct.multiply(positionWeightPct)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal distanceToTargetPct = recommendation.getTargetPrice().subtract(currentPrice)
                .divide(currentPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal distanceToStopPct = currentPrice.subtract(recommendation.getStopPrice())
                .divide(currentPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        String priceStatus = currentPrice.compareTo(recommendation.getTargetPrice()) >= 0 ? "TARGET_TOUCHED"
                : currentPrice.compareTo(recommendation.getStopPrice()) <= 0 ? "STOP_TOUCHED"
                : "OPEN";
        return new PaperPosition(
                recommendation.getId(),
                recommendation.getTicker(),
                recommendation.getMarket(),
                recommendation.getTerm(),
                recommendation.getEntryPrice(),
                currentPrice,
                price.getTradeDate(),
                recommendation.getTargetPrice(),
                recommendation.getStopPrice(),
                recommendation.getConfidence(),
                positionWeightPct,
                unrealizedPnlPct,
                weightedPnlPct,
                distanceToTargetPct,
                distanceToStopPct,
                priceStatus
        );
    }

    private Optional<BigDecimal> extractPositionWeightPct(String signalsJson) {
        try {
            JsonNode node = objectMapper.readTree(signalsJson).path("positionWeightPct");
            return node.isNumber() ? Optional.of(node.decimalValue().setScale(2, RoundingMode.HALF_UP)) : Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private TermStats buildTermStats(List<EvaluationEntity> evals) {
        int count = evals.size();
        long hits = evals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
        double hitRate = Math.round(hits * 1000.0 / count) / 10.0;
        BigDecimal avg = evals.stream().map(EvaluationEntity::getPnlPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        return new TermStats(count, hitRate, avg);
    }

    private record DailyStatsBucket(String date, int count, int hitCount, BigDecimal avgPnlPct, BigDecimal totalPnlPct) {}
}
