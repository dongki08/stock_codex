package com.parkdh.stockadvisor.api.codex.dto; // Codex 호출 DTO 패키지를 선언한다.

import jakarta.validation.constraints.Min; // 최소값 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record CodexCallCreateRequest( // Codex 호출 로그 생성 요청 DTO를 정의한다.
        @NotBlank(message = "호출자는 필수입니다.") String caller, // 호출자를 보관한다.
        @NotBlank(message = "프롬프트 해시는 필수입니다.") String promptHash, // 프롬프트 해시를 보관한다.
        @NotNull(message = "프롬프트 길이는 필수입니다.") @Min(value = 0, message = "프롬프트 길이는 0 이상이어야 합니다.") Integer promptLen, // 프롬프트 길이를 보관한다.
        Integer responseLen, // 응답 길이를 보관한다.
        String toolsUsedJson, // 사용 도구 JSON을 보관한다.
        Integer durationMs, // 소요 시간을 보관한다.
        @NotNull(message = "성공 여부는 필수입니다.") Boolean succeeded, // 성공 여부를 보관한다.
        String errorMessage, // 에러 메시지를 보관한다.
        LocalDateTime calledAt // 호출 일시를 보관한다.
) { // Codex 호출 로그 생성 요청 DTO 본문을 시작한다.
} // Codex 호출 로그 생성 요청 DTO를 종료한다.
