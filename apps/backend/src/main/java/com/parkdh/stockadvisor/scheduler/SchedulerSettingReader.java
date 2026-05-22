package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SchedulerSettingReader {
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    public int getInt(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractIntValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractBooleanValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    public boolean getBooleanField(String key, String fieldName, boolean defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractBooleanField(valueJson, fieldName, defaultValue))
                .orElse(defaultValue);
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractDecimalValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    public String getStringField(String key, String fieldName, String defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractStringField(valueJson, fieldName, defaultValue))
                .orElse(defaultValue);
    }

    public boolean containsDate(String key, LocalDate date) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(this::extractStringListValue)
                .orElse(List.of())
                .contains(date.toString());
    }

    private int extractIntValue(String valueJson, int defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asInt(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String valueJson, boolean defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asBoolean(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private boolean extractBooleanField(String valueJson, String fieldName, boolean defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path(fieldName).asBoolean(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private BigDecimal extractDecimalValue(String valueJson, BigDecimal defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            JsonNode value = node.path("value");
            if (value.isNumber() || value.isTextual()) {
                return new BigDecimal(value.asText());
            }
            return defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private String extractStringField(String valueJson, String fieldName, String defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            String value = node.path(fieldName).asText(defaultValue);
            return value == null || value.isBlank() ? defaultValue : value;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private List<String> extractStringListValue(String valueJson) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            JsonNode value = node.path("value");
            if (!value.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            value.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            });
            return values;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
