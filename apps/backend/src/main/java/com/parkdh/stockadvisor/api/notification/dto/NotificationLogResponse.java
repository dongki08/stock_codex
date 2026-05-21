package com.parkdh.stockadvisor.api.notification.dto; // 알림 로그 DTO 패키지를 선언한다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record NotificationLogResponse( // 알림 로그 응답 DTO를 정의한다.
        Long id, // 알림 로그 ID를 보관한다.
        String channel, // 알림 채널을 보관한다.
        String payloadHash, // 페이로드 해시를 보관한다.
        LocalDateTime sentAt, // 발송 일시를 보관한다.
        String status, // 발송 상태를 보관한다.
        String errorMessage // 에러 메시지를 보관한다.
) { // 알림 로그 응답 DTO 본문을 시작한다.
} // 알림 로그 응답 DTO를 종료한다.
