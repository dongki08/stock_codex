package com.parkdh.stockadvisor.api.dev.dto;

public record DevNotificationTestResponse(boolean sent, boolean devMode, String message, Long logId) {}
