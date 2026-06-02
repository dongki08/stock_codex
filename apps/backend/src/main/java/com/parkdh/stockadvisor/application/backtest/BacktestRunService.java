package com.parkdh.stockadvisor.application.backtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.backtest.dto.BacktestRunCreateRequest;
import com.parkdh.stockadvisor.api.backtest.dto.BacktestRunResponse;
import com.parkdh.stockadvisor.api.backtest.dto.BacktestSimulationRequest;
import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder;
import com.parkdh.stockadvisor.domain.backtest.BacktestRunEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.domain.feature.FeatureSnapshotEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.global.exception.CustomException;
import com.parkdh.stockadvisor.global.util.JsonValidationUtil;
import com.parkdh.stockadvisor.infrastructure.persistence.backtest.BacktestRunRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.feature.FeatureSnapshotRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BacktestRunService {
    private final BacktestRunRepository backtestRunRepository;
    private final PriceDailyRepository priceDailyRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    private final UniverseFeatureBuilder universeFeatureBuilder;
    private final MarketUniverseRepository marketUniverseRepository;
    private final FeatureSnapshotRepository featureSnapshotRepository;

    public List<BacktestRunResponse> getBacktestRuns() {
        return backtestRunRepository.findAll().stream()
                .sorted(Comparator.comparing(BacktestRunEntity::getId).reversed())
                .map(this::toResponse)
                .toList();
    }

    public BacktestRunResponse getBacktestRun(Long id) {
        BacktestRunEntity entity = backtestRunRepository.findById(id)
                .orElseThrow(() -> new CustomException("Backtest run not found.", 404));
        return toResponse(entity);
    }

    @Transactional
    public BacktestRunResponse createBacktestRun(BacktestRunCreateRequest request) {
        if (request.periodFrom().isAfter(request.periodTo())) {
            throw new CustomException("Backtest start date cannot be after end date.", 400);
        }
        JsonValidationUtil.validate(objectMapper, request.metricsJson(), "metricsJson");
        BacktestRunEntity saved = backtestRunRepository.save(new BacktestRunEntity(
                request.strategy(),
                request.periodFrom(),
                request.periodTo(),
                request.metricsJson()
        ));
        return toResponse(saved);
    }

    @Transactional
    public BacktestRunResponse simulateBacktest(BacktestSimulationRequest request) {
        if (request.periodFrom().isAfter(request.periodTo())) {
            throw new CustomException("Backtest start date cannot be after end date.", 400);
        }

        String strategy = request.strategy() == null || request.strategy().isBlank() ? "ma20-breakout-v0" : request.strategy();
        String market = request.market() == null || request.market().isBlank() ? "ALL" : request.market();
        int maxTickers = normalizeInt(request.maxTickers(), 30, 1, 300, "maxTickers");
        int holdingDays = normalizeInt(request.holdingDays(), 20, 1, 120, "holdingDays");
        BigDecimal targetPct = normalizePct(request.targetPct(), BigDecimal.valueOf(3.0), "targetPct");
        BigDecimal stopPct = normalizePct(request.stopPct(), BigDecimal.valueOf(2.0), "stopPct");

        BacktestEvaluation evaluation = evaluateStrategy(strategy, market, request, maxTickers, holdingDays, targetPct, stopPct);
        BacktestRunEntity saved = backtestRunRepository.save(new BacktestRunEntity(strategy, request.periodFrom(), request.periodTo(), evaluation.metricsJson()));
        return toResponse(saved);
    }

    public BacktestEvaluation evaluateRecommendationEngine(BacktestSimulationRequest request) {
        if (request.periodFrom().isAfter(request.periodTo())) {
            throw new CustomException("Backtest start date cannot be after end date.", 400);
        }
        String market = request.market() == null || request.market().isBlank() ? "ALL" : request.market();
        int maxTickers = normalizeInt(request.maxTickers(), 30, 1, 300, "maxTickers");
        int holdingDays = normalizeInt(request.holdingDays(), 20, 1, 120, "holdingDays");
        BigDecimal targetPct = normalizePct(request.targetPct(), BigDecimal.valueOf(3.0), "targetPct");
        BigDecimal stopPct = normalizePct(request.stopPct(), BigDecimal.valueOf(2.0), "stopPct");
        return evaluateStrategy("recommendation-engine-v1", market, request, maxTickers, holdingDays, targetPct, stopPct);
    }

    private BacktestEvaluation evaluateStrategy(String strategy, String market, BacktestSimulationRequest request, int maxTickers, int holdingDays, BigDecimal targetPct, BigDecimal stopPct) {
        // TASK-1: selectTopCandidates(현재 시점) 제거 → score 기반 진입으로 교체해 미래참조 차단
        int minScore = readSettingInt("backtest.entry.minScore", 60);
        List<PriceDailyEntity> prices = findSimulationPrices(market, request);
        Map<String, List<PriceDailyEntity>> byTicker = prices.stream()
                .collect(Collectors.groupingBy(row -> row.getMarket() + ":" + row.getTicker(), LinkedHashMap::new, Collectors.toList()));
        List<SimulatedTrade> trades = byTicker.values().stream()
                .limit(maxTickers)
                .map(rows -> {
                    PriceDailyEntity head = rows.get(0);
                    MarketUniverseEntity universeEntity = marketUniverseRepository
                            .findById(MarketUniverseEntity.buildKey(head.getMarket(), head.getTicker()))
                            .orElse(null);
                    if (universeEntity == null) {
                        return List.<SimulatedTrade>of(); // 유니버스에 없는 종목은 시뮬에서 제외한다.
                    }
                    return simulateTicker(universeEntity, rows, holdingDays, targetPct, stopPct, resolveBacktestCost(head.getMarket()), minScore);
                })
                .flatMap(List::stream)
                .toList();
        if (trades.isEmpty()) {
            throw new CustomException("Not enough price data to simulate backtest.", 404);
        }

        String metricsJson = buildMetricsJson(strategy, market, request, maxTickers, holdingDays, targetPct, stopPct, prices.size(), 0, trades);
        BigDecimal metricValue = extractMetric(metricsJson, "avgPnlPct");
        return new BacktestEvaluation(strategy, request.periodFrom(), request.periodTo(), metricsJson, metricValue);
    }

    private List<PriceDailyEntity> findSimulationPrices(String market, BacktestSimulationRequest request) {
        if ("ALL".equals(market)) {
            return priceDailyRepository.findByTradeDateBetweenOrderByTickerAscTradeDateAsc(request.periodFrom(), request.periodTo());
        }
        return priceDailyRepository.findByMarketAndTradeDateBetweenOrderByTickerAscTradeDateAsc(market, request.periodFrom(), request.periodTo());
    }

    private List<SimulatedTrade> simulateTicker(MarketUniverseEntity universeEntity, List<PriceDailyEntity> rows, int holdingDays, BigDecimal targetPct, BigDecimal stopPct, BacktestCost cost, int minScore) {
        // TASK-1: 진입 score를 라이브 추천 점수기(buildFeatureAsOf)로 산출 → AutoResearch 가중치가 metric에 반영된다. asOf 이하 데이터만 쓰므로 미래참조 없음.
        if (rows.size() < 21) {
            return List.of();
        }

        PriceDailyEntity entry = null;
        for (int i = 20; i < rows.size() - 1; i++) {
            int score = resolveScore(universeEntity, rows.get(i).getTradeDate()); // 스냅샷 우선 조회, 없으면 buildFeatureAsOf 재계산
            if (score >= minScore) {
                entry = rows.get(i + 1);
                break;
            }
        }
        if (entry == null) {
            return List.of();
        }

        BigDecimal entryPrice = entry.getClosePrice();
        BigDecimal targetPrice = entryPrice.multiply(BigDecimal.ONE.add(targetPct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
        BigDecimal stopPrice = entryPrice.multiply(BigDecimal.ONE.subtract(stopPct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
        int entryIndex = rows.indexOf(entry);
        int endExclusive = Math.min(rows.size(), entryIndex + holdingDays + 1);
        PriceDailyEntity exit = rows.get(endExclusive - 1);
        String exitReason = "TIME";

        for (int i = entryIndex + 1; i < endExclusive; i++) {
            PriceDailyEntity row = rows.get(i);
            if (row.getLowPrice().compareTo(stopPrice) <= 0) {
                exit = row;
                exitReason = "STOP";
                break;
            }
            if (row.getHighPrice().compareTo(targetPrice) >= 0) {
                exit = row;
                exitReason = "TARGET";
                break;
            }
        }

        BigDecimal effectiveEntryPrice = entryPrice.multiply(BigDecimal.ONE.add(cost.entryCostRate())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal effectiveExitPrice = exit.getClosePrice().multiply(BigDecimal.ONE.subtract(cost.exitCostRate())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal pnlPct = effectiveExitPrice.subtract(effectiveEntryPrice)
                .divide(effectiveEntryPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        return List.of(new SimulatedTrade(
                entry.getTicker(),
                entry.getMarket(),
                entry.getTradeDate(),
                exit.getTradeDate(),
                entryPrice,
                exit.getClosePrice(),
                effectiveEntryPrice,
                effectiveExitPrice,
                pnlPct,
                exitReason,
                cost.entryCostPct(),
                cost.exitCostPct(),
                cost.slippagePct(),
                cost.roundTripCostPct()
        ));
    }

    private BacktestCost resolveBacktestCost(String market) {
        BigDecimal slippagePct = readSettingDecimal("backtest.slippage.percent", "value", BigDecimal.valueOf(0.05));
        if (isKoreanMarket(market)) {
            BigDecimal taxPct = readSettingDecimal("backtest.cost.kr", "taxPercent", BigDecimal.valueOf(0.18));
            BigDecimal feePct = readSettingDecimal("backtest.cost.kr", "feePercent", BigDecimal.valueOf(0.015));
            return BacktestCost.fromPercent(feePct.add(slippagePct), taxPct.add(feePct).add(slippagePct), slippagePct);
        }
        if (isUsMarket(market)) {
            BigDecimal fxSpreadPct = readSettingDecimal("backtest.cost.us", "fxSpreadPercent", BigDecimal.valueOf(0.5));
            BigDecimal fxSidePct = fxSpreadPct.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            return BacktestCost.fromPercent(fxSidePct.add(slippagePct), fxSidePct.add(slippagePct), slippagePct);
        }
        return BacktestCost.fromPercent(slippagePct, slippagePct, slippagePct);
    }

    private boolean isKoreanMarket(String market) {
        return "KR".equalsIgnoreCase(market)
                || "KOSPI".equalsIgnoreCase(market)
                || "KOSDAQ".equalsIgnoreCase(market)
                || "KONEX".equalsIgnoreCase(market);
    }

    private boolean isUsMarket(String market) {
        return "US".equalsIgnoreCase(market)
                || "NASDAQ".equalsIgnoreCase(market)
                || "NYSE".equalsIgnoreCase(market)
                || "AMEX".equalsIgnoreCase(market);
    }

    private int resolveScore(MarketUniverseEntity entity, java.time.LocalDate date) {
        // 스냅샷이 있으면 이미 저장된 totalScore 반환 — 다회 DB 조회 불필요, IC와 일관성 보장.
        // 없으면 buildFeatureAsOf 재계산(폴백) — 스냅샷 적재 전 구간 지원.
        String key = FeatureSnapshotEntity.buildKey(entity.getMarket(), entity.getTicker(), date);
        return featureSnapshotRepository.findById(key)
                .map(FeatureSnapshotEntity::getTotalScore)
                .orElseGet(() -> universeFeatureBuilder.buildFeatureAsOf(entity, date).totalScore());
    }

    private int readSettingInt(String key, int defaultValue) {
        try {
            Optional<AppSettingEntity> setting = appSettingRepository.findById(key);
            if (setting.isEmpty()) return defaultValue;
            JsonNode value = objectMapper.readTree(setting.get().getValueJson()).path("value");
            return value.isInt() || value.isLong() ? value.intValue() : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private BigDecimal readSettingDecimal(String key, String field, BigDecimal defaultValue) {
        try {
            Optional<AppSettingEntity> setting = appSettingRepository.findById(key);
            if (setting.isEmpty()) {
                return defaultValue;
            }
            JsonNode value = objectMapper.readTree(setting.get().getValueJson()).get(field);
            if (value == null || !value.isNumber()) {
                return defaultValue;
            }
            return value.decimalValue();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private String buildMetricsJson(String strategy, String market, BacktestSimulationRequest request, int maxTickers, int holdingDays, BigDecimal targetPct, BigDecimal stopPct, int priceRows, int selectedCandidateCount, List<SimulatedTrade> trades) {
        try {
            BigDecimal totalPnl = trades.stream().map(SimulatedTrade::pnlPct).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
            BigDecimal avgPnl = totalPnl.divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);
            long wins = trades.stream().filter(trade -> trade.pnlPct().compareTo(BigDecimal.ZERO) > 0).count();
            long targetHits = trades.stream().filter(trade -> "TARGET".equals(trade.exitReason())).count();
            long stopHits = trades.stream().filter(trade -> "STOP".equals(trade.exitReason())).count();
            long timeExits = trades.stream().filter(trade -> "TIME".equals(trade.exitReason())).count();
            BigDecimal hitRate = BigDecimal.valueOf(wins)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(trades.size()), 2, RoundingMode.HALF_UP);
            BigDecimal maxDrawdown = calculateMaxDrawdown(trades);

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("strategy", strategy);
            metrics.put("market", market);
            metrics.put("periodFrom", request.periodFrom().toString());
            metrics.put("periodTo", request.periodTo().toString());
            metrics.put("maxTickers", maxTickers);
            metrics.put("holdingDays", holdingDays);
            metrics.put("targetPct", targetPct);
            metrics.put("stopPct", stopPct);
            metrics.put("selectedCandidateCount", selectedCandidateCount);
            metrics.put("entryCostPct", averageTradeMetric(trades.stream().map(SimulatedTrade::entryCostPct).toList()));
            metrics.put("exitCostPct", averageTradeMetric(trades.stream().map(SimulatedTrade::exitCostPct).toList()));
            metrics.put("slippagePct", averageTradeMetric(trades.stream().map(SimulatedTrade::slippagePct).toList()));
            metrics.put("roundTripCostPct", averageTradeMetric(trades.stream().map(SimulatedTrade::roundTripCostPct).toList()));
            metrics.put("priceRows", priceRows);
            metrics.put("tradeCount", trades.size());
            metrics.put("wins", wins);
            metrics.put("losses", trades.size() - wins);
            metrics.put("targetHits", targetHits);
            metrics.put("stopHits", stopHits);
            metrics.put("timeExits", timeExits);
            metrics.put("hitRate", hitRate);
            metrics.put("avgPnlPct", avgPnl);
            metrics.put("totalPnlPct", totalPnl);
            metrics.put("maxDrawdownPct", maxDrawdown);
            metrics.put("sampleTrades", trades.stream().limit(20).map(this::toTradeMap).toList());
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception exception) {
            throw new CustomException("Failed to create backtest metrics JSON: " + exception.getMessage(), 500);
        }
    }

    private BigDecimal extractMetric(String metricsJson, String fieldName) {
        try {
            JsonNode value = objectMapper.readTree(metricsJson).path(fieldName);
            return value.isNumber() ? value.decimalValue() : BigDecimal.ZERO;
        } catch (Exception exception) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> toTradeMap(SimulatedTrade trade) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ticker", trade.ticker());
        row.put("market", trade.market());
        row.put("entryDate", trade.entryDate().toString());
        row.put("exitDate", trade.exitDate().toString());
        row.put("entryPrice", trade.entryPrice());
        row.put("exitPrice", trade.exitPrice());
        row.put("effectiveEntryPrice", trade.effectiveEntryPrice());
        row.put("effectiveExitPrice", trade.effectiveExitPrice());
        row.put("pnlPct", trade.pnlPct());
        row.put("exitReason", trade.exitReason());
        row.put("entryCostPct", trade.entryCostPct());
        row.put("exitCostPct", trade.exitCostPct());
        row.put("slippagePct", trade.slippagePct());
        row.put("roundTripCostPct", trade.roundTripCostPct());
        return row;
    }

    private BigDecimal calculateMaxDrawdown(List<SimulatedTrade> trades) {
        BigDecimal equity = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        for (SimulatedTrade trade : trades) {
            equity = equity.add(trade.pnlPct());
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            BigDecimal drawdown = equity.subtract(peak);
            if (drawdown.compareTo(maxDrawdown) < 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageTradeMetric(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private int normalizeInt(Integer value, int defaultValue, int min, int max, String label) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < min || normalized > max) {
            throw new CustomException(label + " must be between " + min + " and " + max + ".", 400);
        }
        return normalized;
    }

    private BigDecimal normalizePct(BigDecimal value, BigDecimal defaultValue, String label) {
        BigDecimal normalized = value == null ? defaultValue : value;
        if (normalized.compareTo(BigDecimal.valueOf(0.1)) < 0 || normalized.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new CustomException(label + " must be between 0.1 and 50.", 400);
        }
        return normalized;
    }

    private BacktestRunResponse toResponse(BacktestRunEntity entity) {
        return new BacktestRunResponse(entity.getId(), entity.getStrategy(), entity.getPeriodFrom(), entity.getPeriodTo(), entity.getMetricsJson());
    }

    public record BacktestEvaluation(
            String strategy,
            java.time.LocalDate periodFrom,
            java.time.LocalDate periodTo,
            String metricsJson,
            BigDecimal metricValue
    ) {
    }

    private record BacktestCost(
            BigDecimal entryCostRate,
            BigDecimal exitCostRate,
            BigDecimal entryCostPct,
            BigDecimal exitCostPct,
            BigDecimal slippagePct,
            BigDecimal roundTripCostPct
    ) {
        private static BacktestCost fromPercent(BigDecimal entryCostPct, BigDecimal exitCostPct, BigDecimal slippagePct) {
            BigDecimal hundred = BigDecimal.valueOf(100);
            BigDecimal normalizedEntryCostPct = entryCostPct.setScale(4, RoundingMode.HALF_UP);
            BigDecimal normalizedExitCostPct = exitCostPct.setScale(4, RoundingMode.HALF_UP);
            return new BacktestCost(
                    normalizedEntryCostPct.divide(hundred, 8, RoundingMode.HALF_UP),
                    normalizedExitCostPct.divide(hundred, 8, RoundingMode.HALF_UP),
                    normalizedEntryCostPct,
                    normalizedExitCostPct,
                    slippagePct.setScale(4, RoundingMode.HALF_UP),
                    normalizedEntryCostPct.add(normalizedExitCostPct).setScale(4, RoundingMode.HALF_UP)
            );
        }
    }

    private record SimulatedTrade(
            String ticker,
            String market,
            java.time.LocalDate entryDate,
            java.time.LocalDate exitDate,
            BigDecimal entryPrice,
            BigDecimal exitPrice,
            BigDecimal effectiveEntryPrice,
            BigDecimal effectiveExitPrice,
            BigDecimal pnlPct,
            String exitReason,
            BigDecimal entryCostPct,
            BigDecimal exitCostPct,
            BigDecimal slippagePct,
            BigDecimal roundTripCostPct
    ) {
    }
}
