package com.parkdh.stockadvisor.api.autoresearch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AutoresearchAutoRunRequest(
        String market,
        LocalDate periodFrom,
        LocalDate periodTo,
        Integer iterations,
        Integer maxTickers,
        Integer holdingDays,
        BigDecimal targetPct,
        BigDecimal stopPct
) {
}
