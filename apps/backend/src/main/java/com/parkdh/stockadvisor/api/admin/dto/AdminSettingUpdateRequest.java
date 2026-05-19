package com.parkdh.stockadvisor.api.admin.dto; // 관리자 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.

public record AdminSettingUpdateRequest( // 관리자 설정 수정 요청 DTO를 정의한다.
        @NotBlank(message = "설정 값 JSON은 필수입니다.") String valueJson, // 설정 값 JSON 입력값을 보관한다.
        @NotBlank(message = "수정자는 필수입니다.") String actor // 수정자 입력값을 보관한다.
) { // 관리자 설정 수정 요청 DTO 본문을 시작한다.
} // 관리자 설정 수정 요청 DTO를 종료한다.
