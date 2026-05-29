package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.sentiment")
public record SentimentAnalysisProperties(
        boolean enabled,
        String baseUrl
) {
}
