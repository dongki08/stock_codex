package com.parkdh.stockadvisor.api.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatsPaperTradingResponse(
        int openCount,
        int pricedCount,
        BigDecimal avgUnrealizedPnlPct,
        BigDecimal weightedUnrealizedPnlPct,
        BigDecimal totalWeightPct,
        int targetTouchCount,
        int stopTouchCount,
        List<PaperPosition> positions
) {
    public record PaperPosition(
            Long recommendationId,
            String ticker,
            String market,
            String term,
            BigDecimal entryPrice,
            BigDecimal currentPrice,
            LocalDate currentTradeDate,
            BigDecimal targetPrice,
            BigDecimal stopPrice,
            Integer confidence,
            BigDecimal positionWeightPct,
            BigDecimal unrealizedPnlPct,
            BigDecimal weightedPnlPct,
            BigDecimal distanceToTargetPct,
            BigDecimal distanceToStopPct,
            String priceStatus
    ) {
    }
}
