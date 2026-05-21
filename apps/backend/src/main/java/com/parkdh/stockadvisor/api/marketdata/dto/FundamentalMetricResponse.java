package com.parkdh.stockadvisor.api.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FundamentalMetricResponse(
        String metricKey,
        String ticker,
        String market,
        String metricName,
        BigDecimal metricValue,
        String unit,
        Integer fiscalYear,
        String fiscalPeriod,
        LocalDate periodEnd,
        String source,
        LocalDateTime fetchedAt
) {
}
