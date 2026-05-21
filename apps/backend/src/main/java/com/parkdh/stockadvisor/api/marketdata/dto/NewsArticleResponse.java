package com.parkdh.stockadvisor.api.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NewsArticleResponse(
        String articleKey,
        String ticker,
        String market,
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String summary,
        BigDecimal sentimentScore
) {
}
