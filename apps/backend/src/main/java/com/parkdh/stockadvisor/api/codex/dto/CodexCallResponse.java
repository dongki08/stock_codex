package com.parkdh.stockadvisor.api.codex.dto; // Codex 호출 DTO 패키지를 선언한다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record CodexCallResponse( // Codex 호출 로그 응답 DTO를 정의한다.
        Long id, // Codex 호출 ID를 보관한다.
        String caller, // 호출자를 보관한다.
        String promptHash, // 프롬프트 해시를 보관한다.
        Integer promptLen, // 프롬프트 길이를 보관한다.
        Integer responseLen, // 응답 길이를 보관한다.
        String toolsUsedJson, // 사용 도구 JSON을 보관한다.
        Integer durationMs, // 소요 시간을 보관한다.
        Boolean succeeded, // 성공 여부를 보관한다.
        String errorMessage, // 에러 메시지를 보관한다.
        LocalDateTime calledAt // 호출 일시를 보관한다.
) { // Codex 호출 로그 응답 DTO 본문을 시작한다.
} // Codex 호출 로그 응답 DTO를 종료한다.
