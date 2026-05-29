package com.parkdh.stockadvisor.scheduler;

import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchAutoRunRequest;
import com.parkdh.stockadvisor.application.autoresearch.AutoresearchService;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Component
public class AutoResearchJob {
    private final AutoresearchService autoresearchService;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 20 2 * * TUE-SAT", zone = "Asia/Seoul")
    public void run() {
        if (!getBooleanSetting("autoresearch.enabled", true)) {
            log.info("AutoResearchJob 비활성화. setting=autoresearch.enabled");
            return;
        }
        LocalDate periodTo = LocalDate.now().minusDays(1);
        LocalDate periodFrom = periodTo.minusDays(365);
        int iterations = getIntSetting("autoresearch.targetIterations", 8);
        int maxTickers = getIntSetting("autoresearch.maxTickers", 30);
        int holdingDays = getIntSetting("autoresearch.holdingDays", 20);
        BigDecimal targetPct = getDecimalSetting("autoresearch.targetPct", null);
        BigDecimal stopPct = getDecimalSetting("autoresearch.stopPct", null);
        log.info("AutoResearchJob 시작. periodFrom={}, periodTo={}, iterations={}", periodFrom, periodTo, iterations);
        autoresearchService.runAutoResearch(new AutoresearchAutoRunRequest(
                "ALL",
                periodFrom,
                periodTo,
                iterations,
                maxTickers,
                holdingDays,
                targetPct,
                stopPct
        ));
        log.info("AutoResearchJob 완료");
    }

    private boolean getBooleanSetting(String key, boolean defaultValue) {
        try {
            return appSettingRepository.findById(key)
                    .map(AppSettingEntity::getValueJson)
                    .map(valueJson -> {
                        try {
                            JsonNode node = objectMapper.readTree(valueJson);
                            return node.path("value").asBoolean(defaultValue);
                        } catch (Exception exception) {
                            return defaultValue;
                        }
                    })
                    .orElse(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private int getIntSetting(String key, int defaultValue) {
        try {
            return appSettingRepository.findById(key)
                    .map(AppSettingEntity::getValueJson)
                    .map(valueJson -> {
                        try {
                            JsonNode node = objectMapper.readTree(valueJson);
                            return node.path("value").asInt(defaultValue);
                        } catch (Exception exception) {
                            return defaultValue;
                        }
                    })
                    .orElse(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private BigDecimal getDecimalSetting(String key, BigDecimal defaultValue) {
        try {
            return appSettingRepository.findById(key)
                    .map(AppSettingEntity::getValueJson)
                    .map(valueJson -> {
                        try {
                            JsonNode node = objectMapper.readTree(valueJson);
                            return node.path("value").isNumber() ? node.path("value").decimalValue() : defaultValue;
                        } catch (Exception exception) {
                            return defaultValue;
                        }
                    })
                    .orElse(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }
}
