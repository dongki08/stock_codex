package com.parkdh.stockadvisor.infrastructure.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.config.CodexCliProperties;
import com.parkdh.stockadvisor.domain.codex.CodexCallEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import com.parkdh.stockadvisor.infrastructure.persistence.codex.CodexCallRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class CodexClient {
    private static final int TIMEOUT_SECONDS = 300;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

    private final CodexCliProperties codexCliProperties;
    private final CodexCallRepository codexCallRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    public CodexResult call(String prompt, String profile, String caller) {
        LocalDateTime calledAt = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        if (MarketUtil.isDevPlaceholder(codexCliProperties.command())) {
            log.info("[Codex 개발 모드] caller={} profile={} promptLen={}", caller, profile, prompt.length());
            String response = buildFallback(profile);
            long durationMs = System.currentTimeMillis() - startMs;
            saveLog(caller, prompt, response, null, true, durationMs, calledAt);
            return new CodexResult(true, response, null, durationMs);
        }

        int dailyCallLimit = resolveIntSetting("codex.daily.callLimit", 200);
        long dailyCallCount = countTodayCalls();
        if (dailyCallLimit >= 0 && dailyCallCount >= dailyCallLimit) {
            long durationMs = System.currentTimeMillis() - startMs;
            String errorMessage = "Codex 일 호출 한도를 초과했습니다. limit=" + dailyCallLimit + ", todayCalls=" + dailyCallCount;
            log.warn("[Codex] 호출 한도 초과 caller={} limit={} todayCalls={}", caller, dailyCallLimit, dailyCallCount);
            String response = buildFallback(profile);
            saveLog(caller, prompt, null, errorMessage, false, durationMs, calledAt);
            return new CodexResult(false, response, errorMessage, durationMs);
        }

        double dailyBudgetUsd = resolveDoubleSetting("codex.daily.budgetUsd", 0.0);
        if (dailyBudgetUsd > 0) {
            double estimatedUsdPer1kChars = resolveDoubleSetting("codex.estimatedUsdPer1kChars", 0.002);
            int estimatedResponseChars = resolveIntSetting("codex.estimatedResponseChars", 4000);
            long todayTextLength = sumTodaySucceededTextLength();
            long estimatedTextLength = todayTextLength + prompt.length() + Math.max(estimatedResponseChars, 0);
            double estimatedBudgetUsd = estimateBudgetUsd(estimatedTextLength, estimatedUsdPer1kChars);
            if (estimatedBudgetUsd >= dailyBudgetUsd) {
                long durationMs = System.currentTimeMillis() - startMs;
                String errorMessage = "Codex 일 예산 한도를 초과할 것으로 예상됩니다. budgetUsd=" + dailyBudgetUsd + ", estimatedUsd=" + String.format("%.4f", estimatedBudgetUsd);
                log.warn("[Codex] 예산 한도 초과 caller={} budgetUsd={} estimatedUsd={}", caller, dailyBudgetUsd, estimatedBudgetUsd);
                String response = buildFallback(profile);
                saveLog(caller, prompt, null, errorMessage, false, durationMs, calledAt);
                return new CodexResult(false, response, errorMessage, durationMs);
            }
        }

        try {
            List<String> command = List.of(codexCliProperties.command(), "exec",
                    "--dangerously-bypass-approvals-and-sandbox",
                    "--skip-git-repo-check",
                    "--ephemeral",
                    prompt);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.getOutputStream().close(); // stdin 닫아서 대기 방지

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startMs;

            if (!finished) {
                process.destroyForcibly();
                log.warn("[Codex] 타임아웃 caller={} durationMs={}", caller, durationMs);
                saveLog(caller, prompt, null, "타임아웃", false, durationMs, calledAt);
                return new CodexResult(false, buildFallback(profile), "타임아웃", durationMs);
            }

            String response = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            boolean succeeded = process.exitValue() == 0 && !response.isBlank();
            String errorMsg = succeeded ? null : "exitCode=" + process.exitValue();
            if (!succeeded) {
                log.warn("[Codex] 실패 caller={} exitCode={}", caller, process.exitValue());
            }
            saveLog(caller, prompt, succeeded ? response : null, errorMsg, succeeded, durationMs, calledAt);
            return new CodexResult(succeeded, succeeded ? response : buildFallback(profile), errorMsg, durationMs);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            long durationMs = System.currentTimeMillis() - startMs;
            log.warn("[Codex] 오류 caller={} error={}", caller, e.getMessage());
            saveLog(caller, prompt, null, e.getMessage(), false, durationMs, calledAt);
            return new CodexResult(false, buildFallback(profile), e.getMessage(), durationMs);
        }
    }

    private String buildFallback(String profile) {
        return String.format("""
                [%s 개발 모드 브리프]

                Codex CLI가 설정되지 않아 기본 템플릿을 반환합니다.
                실제 브리프를 생성하려면 CODEX_COMMAND 환경변수를 설정하세요.

                생성 시각: %s
                """, profile, LocalDateTime.now());
    }

    private long countTodayCalls() {
        LocalDate today = LocalDate.now(APP_ZONE);
        return codexCallRepository.countByCalledAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    private long sumTodaySucceededTextLength() {
        LocalDate today = LocalDate.now(APP_ZONE);
        return codexCallRepository.sumSucceededTextLengthByCalledAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    private double estimateBudgetUsd(long textLength, double usdPer1kChars) {
        if (usdPer1kChars <= 0) {
            return 0.0;
        }
        return (textLength / 1000.0) * usdPer1kChars;
    }

    private int resolveIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractIntValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private int extractIntValue(String valueJson, int defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asInt(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private double resolveDoubleSetting(String key, double defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractDoubleValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private double extractDoubleValue(String valueJson, double defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asDouble(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private void saveLog(String caller, String prompt, String response,
                         String errorMsg, boolean succeeded, long durationMs, LocalDateTime calledAt) {
        try {
            codexCallRepository.save(new CodexCallEntity(
                    caller, hashText(prompt), prompt.length(),
                    response != null ? response.length() : null,
                    null, (int) Math.min(durationMs, Integer.MAX_VALUE),
                    succeeded, errorMsg, calledAt));
        } catch (Exception e) {
            log.warn("[Codex] 로그 저장 실패: {}", e.getMessage());
        }
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.format("%064x", Math.abs(text.hashCode()));
        }
    }

    public record CodexResult(boolean succeeded, String response, String errorMessage, long durationMs) {}
}
