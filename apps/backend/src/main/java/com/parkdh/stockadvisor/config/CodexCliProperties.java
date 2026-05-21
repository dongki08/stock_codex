package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.codex")
public record CodexCliProperties(String command) {}
