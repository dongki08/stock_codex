package com.parkdh.stockadvisor.application.stats;

import com.parkdh.stockadvisor.api.stats.dto.StatsDailyResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse.TermStats;
import com.parkdh.stockadvisor.api.stats.dto.StatsStrategyResponse;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StatsService {
    private final EvaluationRepository evaluationRepository;
    private final RecommendationRepository recommendationRepository;

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
        return evals.stream()
                .collect(Collectors.groupingBy(e -> e.getEvaluatedAt().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<EvaluationEntity>>comparingByKey().reversed())
                .limit(30)
                .map(entry -> {
                    List<EvaluationEntity> dayEvals = entry.getValue();
                    int count = dayEvals.size();
                    int hits = (int) dayEvals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
                    BigDecimal avg = dayEvals.stream().map(EvaluationEntity::getPnlPct)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
                    return new StatsDailyResponse(entry.getKey().toString(), count, hits, avg);
                })
                .toList();
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

    private TermStats buildTermStats(List<EvaluationEntity> evals) {
        int count = evals.size();
        long hits = evals.stream().filter(e -> Boolean.TRUE.equals(e.getHitTarget())).count();
        double hitRate = Math.round(hits * 1000.0 / count) / 10.0;
        BigDecimal avg = evals.stream().map(EvaluationEntity::getPnlPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        return new TermStats(count, hitRate, avg);
    }
}
