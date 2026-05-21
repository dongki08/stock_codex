package com.parkdh.stockadvisor.api.marketdata.dto;

import java.time.LocalDateTime;

public record DisclosureEventResponse(
        String disclosureKey,
        String ticker,
        String market,
        String title,
        String url,
        String source,
        String disclosureType,
        Integer importanceScore,
        LocalDateTime disclosedAt
) {
}
