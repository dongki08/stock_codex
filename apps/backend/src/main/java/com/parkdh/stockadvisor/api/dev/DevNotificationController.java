package com.parkdh.stockadvisor.api.dev;

import com.parkdh.stockadvisor.api.dev.dto.DevNotificationTestResponse;
import com.parkdh.stockadvisor.config.TelegramProperties;
import com.parkdh.stockadvisor.domain.notification.NotificationLogEntity;
import com.parkdh.stockadvisor.global.dto.ResultDto;
import com.parkdh.stockadvisor.global.util.MarketUtil;
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient;
import com.parkdh.stockadvisor.infrastructure.persistence.notification.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dev/notifications")
public class DevNotificationController {
    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;
    private final NotificationLogRepository notificationLogRepository;

    @Operation(summary = "Telegram 알림 테스트 발송",
            description = "Telegram 봇 연결 확인용 테스트 메시지를 발송하고 notification_log에 기록한다. dev-placeholder 모드에서는 로그만 출력한다.")
    @Transactional
    @PostMapping("/test")
    public ResultDto<DevNotificationTestResponse> test(
            @RequestParam(required = false, defaultValue = "Stock Advisor 알림 테스트 메시지입니다.") String message) {
        boolean devMode = MarketUtil.isDevPlaceholder(telegramProperties.botToken());
        boolean sent = telegramClient.sendMessage(message);
        String status = sent ? "SENT" : "FAILED";
        String errorMessage = sent ? null : "전송 실패";
        NotificationLogEntity log = notificationLogRepository.save(
                new NotificationLogEntity("TELEGRAM", hashText(message), LocalDateTime.now(), status, errorMessage));
        return ResultDto.success(new DevNotificationTestResponse(sent, devMode, message, log.getId()));
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
