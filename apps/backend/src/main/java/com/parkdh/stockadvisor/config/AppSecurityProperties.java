package com.parkdh.stockadvisor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "stock-advisor.security")
public record AppSecurityProperties(
        List<String> allowedOrigins,
        boolean protectDevApi
) {
    public AppSecurityProperties {
        allowedOrigins = sanitizeAllowedOrigins(allowedOrigins);
        if (allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
    }

    private static List<String> sanitizeAllowedOrigins(List<String> origins) {
        if (origins == null) {
            return List.of();
        }
        return origins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .filter(origin -> !"*".equals(origin))
                .distinct()
                .toList();
    }
}
