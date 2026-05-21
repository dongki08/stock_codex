package com.parkdh.stockadvisor.api.stats.dto;

import java.math.BigDecimal;

public record StatsStrategyResponse(String modelVersion, int count, double hitRate, BigDecimal avgPnlPct) {}
