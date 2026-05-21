package com.parkdh.stockadvisor.api.stats.dto;

import java.math.BigDecimal;
import java.util.Map;

public record StatsSummaryResponse(
        int total,
        int closed,
        int open,
        int expired,
        double hitRate,
        BigDecimal avgPnlPct,
        BigDecimal totalPnlPct,
        BigDecimal maxDrawdownPct,
        Map<String, TermStats> byTerm,
        Map<String, Integer> byExitReason
) {
    public record TermStats(int count, double hitRate, BigDecimal avgPnlPct) {}
}
