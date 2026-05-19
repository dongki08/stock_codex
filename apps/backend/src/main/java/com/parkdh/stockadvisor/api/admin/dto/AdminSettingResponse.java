package com.parkdh.stockadvisor.api.admin.dto; // 관리자 DTO 패키지를 선언한다.

public record AdminSettingResponse( // 관리자 설정 응답 DTO를 정의한다.
        String key, // 설정 키를 보관한다.
        String valueJson, // 설정 값 JSON을 보관한다.
        String description, // 설정 설명을 보관한다.
        String updatedBy // 마지막 수정자를 보관한다.
) { // 관리자 설정 응답 DTO 본문을 시작한다.
} // 관리자 설정 응답 DTO를 종료한다.
