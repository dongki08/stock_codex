package com.parkdh.stockadvisor.api.recommendation.dto; // 추천 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.

public record RecommendationStatusUpdateRequest( // 추천 상태 수정 요청 DTO를 정의한다.
        @NotBlank(message = "추천 상태는 필수입니다.") String status // 추천 상태를 보관한다.
) { // 추천 상태 수정 요청 DTO 본문을 시작한다.
} // 추천 상태 수정 요청 DTO를 종료한다.
