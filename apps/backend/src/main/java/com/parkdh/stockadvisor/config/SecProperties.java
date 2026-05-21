package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.sec")
public record SecProperties(String userAgent) {
}
