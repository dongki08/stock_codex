package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.global.exception.CustomException;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PricePredictor {
    private static final int VOLATILITY_LOOKBACK = 20;

    private final PriceDailyRepository priceDailyRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    public PredictedRecommendation predict(RecommendationCandidate candidate, String term) {
        List<PriceDailyEntity> history = getRecentHistory(candidate);
        BigDecimal entryPrice = resolveEntryPrice(candidate, history);
        BigDecimal volatilityPct = calculateVolatilityPct(history);
        BigDecimal targetMultiplier = targetMultiplier(term, volatilityPct);
        BigDecimal stopMultiplier = stopMultiplier(term, volatilityPct);
        TradingCost cost = resolveTradingCost(candidate.market());
        BigDecimal targetPrice = costAdjustedExitPrice(entryPrice, targetMultiplier, cost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stopPrice = costAdjustedExitPrice(entryPrice, stopMultiplier, cost).setScale(4, RoundingMode.HALF_UP);
        LocalDate expectedExitAt = "SHORT".equals(term) ? LocalDate.now().plusDays(5) : LocalDate.now().plusMonths(6);
        String pricingMethod = resolvePricingMethod(candidate, history) + "-cost-adjusted";
        BigDecimal volatilityPercent = volatilityPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal positionSizingScore = calculatePositionSizingScore(candidate.score(), volatilityPct);
        return new PredictedRecommendation(entryPrice, targetPrice, stopPrice, expectedExitAt, pricingMethod, volatilityPercent, positionSizingScore);
    }

    private List<PriceDailyEntity> getRecentHistory(RecommendationCandidate candidate) {
        return priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(candidate.market(), candidate.ticker(), PageRequest.of(0, VOLATILITY_LOOKBACK)).stream()
                .sorted(Comparator.comparing(PriceDailyEntity::getTradeDate))
                .toList();
    }

    private BigDecimal resolveEntryPrice(RecommendationCandidate candidate, List<PriceDailyEntity> history) {
        if (!history.isEmpty()) {
            return history.get(history.size() - 1).getClosePrice().setScale(4, RoundingMode.HALF_UP);
        }
        if (candidate.lastPrice() != null) {
            return candidate.lastPrice().setScale(4, RoundingMode.HALF_UP);
        }
        throw new CustomException("가격 데이터가 없어 추천을 생성할 수 없습니다: " + candidate.ticker(), 422);
    }

    private BigDecimal costAdjustedExitPrice(BigDecimal rawEntryPrice, BigDecimal desiredMultiplier, TradingCost cost) {
        BigDecimal effectiveEntry = rawEntryPrice.multiply(BigDecimal.ONE.add(cost.entryCostRate()));
        BigDecimal desiredEffectiveExit = effectiveEntry.multiply(desiredMultiplier);
        return desiredEffectiveExit.divide(BigDecimal.ONE.subtract(cost.exitCostRate()), 8, RoundingMode.HALF_UP);
    }

    private TradingCost resolveTradingCost(String market) {
        BigDecimal slippagePct = readSettingDecimal("backtest.slippage.percent", "value", BigDecimal.valueOf(0.05));
        if (isKoreanMarket(market)) {
            BigDecimal taxPct = readSettingDecimal("backtest.cost.kr", "taxPercent", BigDecimal.valueOf(0.18));
            BigDecimal feePct = readSettingDecimal("backtest.cost.kr", "feePercent", BigDecimal.valueOf(0.015));
            return TradingCost.fromPercent(feePct.add(slippagePct), taxPct.add(feePct).add(slippagePct));
        }
        if (isUsMarket(market)) {
            BigDecimal fxSpreadPct = readSettingDecimal("backtest.cost.us", "fxSpreadPercent", BigDecimal.valueOf(0.5));
            BigDecimal fxSidePct = fxSpreadPct.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            return TradingCost.fromPercent(fxSidePct.add(slippagePct), fxSidePct.add(slippagePct));
        }
        return TradingCost.fromPercent(slippagePct, slippagePct);
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

    private String resolvePricingMethod(RecommendationCandidate candidate, List<PriceDailyEntity> history) {
        if (history.size() >= 5) {
            return "volatility-v1";
        }
        if (!history.isEmpty()) {
            return "recent-close-v1";
        }
        if (candidate.lastPrice() != null) {
            return "last-price-v1";
        }
        return "unavailable";
    }

    private BigDecimal calculateVolatilityPct(List<PriceDailyEntity> history) {
        if (history.size() < 5) {
            return BigDecimal.valueOf(0.02);
        }
        BigDecimal totalAbsMove = BigDecimal.ZERO;
        int count = 0;
        for (int i = 1; i < history.size(); i++) {
            BigDecimal previous = history.get(i - 1).getClosePrice();
            BigDecimal current = history.get(i).getClosePrice();
            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal move = current.subtract(previous).divide(previous, 8, RoundingMode.HALF_UP).abs();
                totalAbsMove = totalAbsMove.add(move);
                count++;
            }
        }
        if (count == 0) {
            return BigDecimal.valueOf(0.02);
        }
        return totalAbsMove.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP).max(BigDecimal.valueOf(0.01)).min(BigDecimal.valueOf(0.08));
    }

    private BigDecimal calculatePositionSizingScore(Integer score, BigDecimal volatilityPct) {
        BigDecimal safeVolatility = volatilityPct.max(BigDecimal.valueOf(0.01));
        BigDecimal scoreScale = BigDecimal.valueOf(Math.max(0, Math.min(100, score == null ? 50 : score)))
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        return scoreScale.divide(safeVolatility, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal targetMultiplier(String term, BigDecimal volatilityPct) {
        if ("SHORT".equals(term)) {
            return BigDecimal.ONE.add(volatilityPct.multiply(BigDecimal.valueOf(2.5)).max(BigDecimal.valueOf(0.035)));
        }
        return BigDecimal.ONE.add(volatilityPct.multiply(BigDecimal.valueOf(8)).max(BigDecimal.valueOf(0.12)));
    }

    private BigDecimal stopMultiplier(String term, BigDecimal volatilityPct) {
        if ("SHORT".equals(term)) {
            return BigDecimal.ONE.subtract(volatilityPct.multiply(BigDecimal.valueOf(1.5)).max(BigDecimal.valueOf(0.025)));
        }
        return BigDecimal.ONE.subtract(volatilityPct.multiply(BigDecimal.valueOf(4)).max(BigDecimal.valueOf(0.08)));
    }

    private record TradingCost(BigDecimal entryCostRate, BigDecimal exitCostRate) {
        private static TradingCost fromPercent(BigDecimal entryCostPct, BigDecimal exitCostPct) {
            BigDecimal hundred = BigDecimal.valueOf(100);
            return new TradingCost(
                    entryCostPct.divide(hundred, 8, RoundingMode.HALF_UP),
                    exitCostPct.divide(hundred, 8, RoundingMode.HALF_UP)
            );
        }
    }
}
