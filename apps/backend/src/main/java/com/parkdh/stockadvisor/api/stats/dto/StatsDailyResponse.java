package com.parkdh.stockadvisor.api.stats.dto;

import java.math.BigDecimal;

public record StatsDailyResponse(
        String date,
        int count,
        int hitCount,
        BigDecimal avgPnlPct,
        BigDecimal totalPnlPct,
        BigDecimal cumulativePnlPct
) {}
