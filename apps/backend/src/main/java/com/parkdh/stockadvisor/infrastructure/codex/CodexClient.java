package com.parkdh.stockadvisor.infrastructure.codex;

import com.parkdh.stockadvisor.config.CodexCliProperties;
import com.parkdh.stockadvisor.domain.codex.CodexCallEntity;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import com.parkdh.stockadvisor.infrastructure.persistence.codex.CodexCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class CodexClient {
    private static final int TIMEOUT_SECONDS = 120;

    private final CodexCliProperties codexCliProperties;
    private final CodexCallRepository codexCallRepository;

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

        try {
            List<String> command = List.of(codexCliProperties.command(), "--profile", profile, "--print", prompt);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

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
