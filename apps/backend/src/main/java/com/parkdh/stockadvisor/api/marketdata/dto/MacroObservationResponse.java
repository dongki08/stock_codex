package com.parkdh.stockadvisor.api.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MacroObservationResponse(
        String observationKey,
        String seriesId,
        String seriesName,
        LocalDate observedDate,
        BigDecimal observedValue,
        String source,
        LocalDateTime fetchedAt
) {
}
