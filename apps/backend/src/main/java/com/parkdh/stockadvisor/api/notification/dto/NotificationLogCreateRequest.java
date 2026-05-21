package com.parkdh.stockadvisor.api.notification.dto; // 알림 로그 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record NotificationLogCreateRequest( // 알림 로그 생성 요청 DTO를 정의한다.
        @NotBlank(message = "알림 채널은 필수입니다.") String channel, // 알림 채널을 보관한다.
        @NotBlank(message = "페이로드 해시는 필수입니다.") String payloadHash, // 페이로드 해시를 보관한다.
        LocalDateTime sentAt, // 발송 일시를 보관한다.
        @NotBlank(message = "발송 상태는 필수입니다.") String status, // 발송 상태를 보관한다.
        String errorMessage // 에러 메시지를 보관한다.
) { // 알림 로그 생성 요청 DTO 본문을 시작한다.
} // 알림 로그 생성 요청 DTO를 종료한다.
