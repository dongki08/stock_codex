package com.parkdh.stockadvisor.api.dev.dto;

public record DevNotificationTestResponse(
        boolean sent,
        boolean devMode,
        Integer statusCode,
        String errorMessage,
        String message,
        Long logId
) {}
