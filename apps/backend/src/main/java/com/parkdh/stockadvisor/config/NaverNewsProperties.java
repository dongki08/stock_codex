package com.parkdh.stockadvisor.config;

import com.parkdh.stockadvisor.global.util.MarketUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.naver-news")
public record NaverNewsProperties(String clientId, String clientSecret) {
    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && !MarketUtil.isDevPlaceholder(clientId)
                && !MarketUtil.isDevPlaceholder(clientSecret);
    }
}
