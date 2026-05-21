package com.parkdh.stockadvisor.application.notification; // 알림 로그 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.notification.dto.NotificationLogCreateRequest; // 알림 로그 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.notification.dto.NotificationLogResponse; // 알림 로그 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.notification.NotificationLogEntity; // 알림 로그 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient; // Telegram 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.notification.NotificationLogRepository; // 알림 로그 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.nio.charset.StandardCharsets; // 문자열 인코딩을 가져온다.
import java.security.MessageDigest; // 해시 생성 도구를 가져온다.
import java.security.NoSuchAlgorithmException; // 해시 알고리즘 예외를 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.HexFormat; // 바이트 배열을 hex 문자열로 변환하는 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class NotificationLogService { // 알림 로그 서비스를 정의한다.
    private final NotificationLogRepository notificationLogRepository; // 알림 로그 저장소 의존성을 보관한다.
    private final TelegramClient telegramClient; // Telegram 클라이언트 의존성을 보관한다.

    public List<NotificationLogResponse> getNotificationLogs(String status) { // 알림 로그 목록을 조회한다.
        List<NotificationLogEntity> entities = status == null || status.isBlank() ? notificationLogRepository.findAll() : notificationLogRepository.findByStatus(status); // 상태 조건 여부에 따라 알림 로그를 조회한다.
        return entities.stream().sorted(Comparator.comparing(NotificationLogEntity::getId).reversed()).map(this::toResponse).toList(); // ID 역순으로 정렬해 DTO로 변환한다.
    } // 알림 로그 목록 조회를 종료한다.

    public NotificationLogResponse getNotificationLog(Long id) { // 알림 로그 단건을 조회한다.
        NotificationLogEntity entity = notificationLogRepository.findById(id).orElseThrow(() -> new CustomException("알림 로그를 찾을 수 없습니다.", 404)); // 알림 로그가 없으면 404 예외를 던진다.
        return toResponse(entity); // 알림 로그 DTO를 반환한다.
    } // 알림 로그 단건 조회를 종료한다.

    @Transactional // 알림 로그 생성을 쓰기 트랜잭션으로 처리한다.
    public NotificationLogResponse createNotificationLog(NotificationLogCreateRequest request) { // 알림 로그를 생성한다.
        LocalDateTime sentAt = request.sentAt() == null ? LocalDateTime.now() : request.sentAt(); // 발송 일시가 없으면 현재 시각을 사용한다.
        NotificationLogEntity entity = new NotificationLogEntity(request.channel(), request.payloadHash(), sentAt, request.status(), request.errorMessage()); // 새 알림 로그 엔티티를 생성한다.
        NotificationLogEntity saved = notificationLogRepository.save(entity); // 새 알림 로그를 저장한다.
        return toResponse(saved); // 저장된 알림 로그를 반환한다.
    } // 알림 로그 생성을 종료한다.

    @Transactional // 중복 확인과 알림 로그 저장을 하나의 쓰기 트랜잭션으로 처리한다.
    public NotificationDispatchResult sendTelegramOnce(String dedupeKey, String message) { // 같은 dedupe key의 Telegram 알림을 한 번만 전송한다.
        String payloadHash = hashText(dedupeKey); // 중복 확인용 해시를 생성한다.
        if (notificationLogRepository.existsByChannelAndPayloadHashAndStatus("TELEGRAM", payloadHash, "SENT")) { // 이미 성공 발송된 알림인지 확인한다.
            return new NotificationDispatchResult(false, true, payloadHash); // 중복으로 스킵했음을 반환한다.
        } // 중복 확인을 종료한다.
        boolean sent = telegramClient.sendMessage(message); // Telegram 메시지를 전송한다.
        notificationLogRepository.save(new NotificationLogEntity(
                "TELEGRAM",
                payloadHash,
                sent ? LocalDateTime.now() : null,
                sent ? "SENT" : "FAILED",
                sent ? null : "Telegram sendMessage returned false"
        )); // 전송 결과를 알림 로그로 저장한다.
        return new NotificationDispatchResult(sent, false, payloadHash); // 전송 결과를 반환한다.
    } // Telegram 단발 알림 전송을 종료한다.

    private NotificationLogResponse toResponse(NotificationLogEntity entity) { // 알림 로그 엔티티를 응답 DTO로 변환한다.
        return new NotificationLogResponse(entity.getId(), entity.getChannel(), entity.getPayloadHash(), entity.getSentAt(), entity.getStatus(), entity.getErrorMessage()); // 알림 로그 응답 DTO를 생성한다.
    } // 알림 로그 DTO 변환을 종료한다.

    private String hashText(String text) { // 문자열을 SHA-256 해시로 변환한다.
        try { // 해시 생성 예외를 처리한다.
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // SHA-256 해시 도구를 생성한다.
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8))); // 해시를 hex 문자열로 반환한다.
        } catch (NoSuchAlgorithmException exception) { // 알고리즘이 없는 예외를 잡는다.
            return String.format("%064x", Math.abs(text.hashCode())); // fallback 해시를 반환한다.
        } // 예외 처리를 종료한다.
    } // 문자열 해시 변환을 종료한다.

    public record NotificationDispatchResult(boolean sent, boolean duplicate, String payloadHash) { // 알림 전송 결과를 정의한다.
    } // 알림 전송 결과 정의를 종료한다.
} // 알림 로그 서비스를 종료한다.
