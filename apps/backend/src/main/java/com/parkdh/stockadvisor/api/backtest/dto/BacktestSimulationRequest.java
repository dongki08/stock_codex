package com.parkdh.stockadvisor.api.backtest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestSimulationRequest(
        String strategy,
        String market,
        @NotNull(message = "기간 시작일은 필수입니다.") LocalDate periodFrom,
        @NotNull(message = "기간 종료일은 필수입니다.") LocalDate periodTo,
        Integer maxTickers,
        Integer holdingDays,
        BigDecimal targetPct,
        BigDecimal stopPct
) {
}
