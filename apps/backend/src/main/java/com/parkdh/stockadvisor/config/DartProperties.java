package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.dart")
public record DartProperties(String apiKey) {
}
