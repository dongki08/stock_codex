package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-advisor.admin")
public record AdminProperties(String username, String password) {}
