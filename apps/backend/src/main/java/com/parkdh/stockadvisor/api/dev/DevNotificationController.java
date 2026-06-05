package com.parkdh.stockadvisor.api.dev;

import com.parkdh.stockadvisor.api.dev.dto.DevNotificationTestResponse;
import com.parkdh.stockadvisor.domain.notification.NotificationLogEntity;
import com.parkdh.stockadvisor.global.dto.ResultDto;
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient;
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient.TelegramSendResult;
import com.parkdh.stockadvisor.infrastructure.persistence.notification.NotificationLogRepository;
import com.parkdh.stockadvisor.scheduler.KrxPreOpenJob;
import com.parkdh.stockadvisor.scheduler.UsCloseSummaryJob;
import com.parkdh.stockadvisor.scheduler.UsPreOpenJob;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Hidden
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dev/notifications")
public class DevNotificationController {
    private final TelegramClient telegramClient;
    private final NotificationLogRepository notificationLogRepository;
    private final KrxPreOpenJob krxPreOpenJob;
    private final UsPreOpenJob usPreOpenJob;
    private final UsCloseSummaryJob usCloseSummaryJob;

    @Operation(summary = "Telegram 알림 테스트 발송",
            description = "Telegram 봇 연결 확인용 테스트 메시지를 발송하고 notification_log에 기록한다. dev-placeholder 모드에서는 로그만 출력한다.")
    @Transactional
    @PostMapping("/test")
    public ResultDto<DevNotificationTestResponse> test(
            @RequestParam(required = false, defaultValue = "Stock Advisor 알림 테스트 메시지입니다.") String message) {
        TelegramSendResult sendResult = telegramClient.sendMessageWithResult(message);
        String status = sendResult.sent() ? "SENT" : "FAILED";
        NotificationLogEntity log = notificationLogRepository.save(
                new NotificationLogEntity("TELEGRAM", hashText(message), sendResult.sent() ? LocalDateTime.now() : null, status, sendResult.errorMessage()));
        return ResultDto.success(new DevNotificationTestResponse(
                sendResult.sent(),
                sendResult.devMode(),
                sendResult.statusCode(),
                sendResult.errorMessage(),
                message,
                log.getId()
        ));
    }

    @Operation(summary = "KRX 프리오픈 스케줄러 즉시 실행", description = "KrxPreOpenJob을 즉시 실행한다. 실제 cron 시각과 무관하게 강제 트리거.")
    @PostMapping("/trigger/krx-preopen")
    public ResultDto<String> triggerKrxPreOpen() {
        krxPreOpenJob.trigger();
        return ResultDto.success("KrxPreOpenJob triggered");
    }

    @Operation(summary = "US 프리오픈 스케줄러 즉시 실행", description = "UsPreOpenJob을 즉시 실행한다.")
    @PostMapping("/trigger/us-preopen")
    public ResultDto<String> triggerUsPreOpen() {
        usPreOpenJob.trigger();
        return ResultDto.success("UsPreOpenJob triggered");
    }

    @Operation(summary = "US 마감 요약 스케줄러 즉시 실행", description = "UsCloseSummaryJob을 즉시 실행한다.")
    @PostMapping("/trigger/us-close")
    public ResultDto<String> triggerUsClose() {
        usCloseSummaryJob.trigger();
        return ResultDto.success("UsCloseSummaryJob triggered");
    }

    @Operation(summary = "오늘 알림 로그 삭제 (dedup 리셋)", description = "오늘 발송된 notification_log를 삭제해 동일 내용 재발송을 허용한다.")
    @Transactional
    @DeleteMapping("/logs/today")
    public ResultDto<String> deleteTodayLogs() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int deleted = notificationLogRepository.deleteBysentAtGreaterThanEqual(todayStart);
        return ResultDto.success("deleted=" + deleted);
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
}
