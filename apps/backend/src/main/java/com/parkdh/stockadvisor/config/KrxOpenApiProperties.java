package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.krx-openapi")
public record KrxOpenApiProperties(String authKey) {
}
