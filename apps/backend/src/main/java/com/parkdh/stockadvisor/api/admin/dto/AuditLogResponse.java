package com.parkdh.stockadvisor.api.admin.dto; // 관리자 DTO 패키지를 선언한다.

public record AuditLogResponse( // 감사 로그 응답 DTO를 정의한다.
        Long id, // 감사 로그 ID를 보관한다.
        String actor, // 수행자를 보관한다.
        String action, // 작업명을 보관한다.
        String beforeJson, // 변경 전 JSON을 보관한다.
        String afterJson // 변경 후 JSON을 보관한다.
) { // 감사 로그 응답 DTO 본문을 시작한다.
} // 감사 로그 응답 DTO를 종료한다.
