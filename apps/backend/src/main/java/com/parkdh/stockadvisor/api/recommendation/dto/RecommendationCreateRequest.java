package com.parkdh.stockadvisor.api.recommendation.dto; // 추천 DTO 패키지를 선언한다.

import jakarta.validation.constraints.DecimalMin; // 최소 숫자 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.Max; // 최대값 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.Min; // 최소값 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record RecommendationCreateRequest( // 추천 생성 요청 DTO를 정의한다.
        @NotBlank(message = "종목 코드는 필수입니다.") String ticker, // 종목 코드를 보관한다.
        @NotBlank(message = "시장 구분은 필수입니다.") String market, // 시장 구분을 보관한다.
        @NotBlank(message = "보유 기간 구분은 필수입니다.") String term, // 보유 기간 구분을 보관한다.
        @NotNull(message = "진입 가격은 필수입니다.") @DecimalMin(value = "0.0001", message = "진입 가격은 0보다 커야 합니다.") BigDecimal entryPrice, // 진입 가격을 보관한다.
        @NotNull(message = "목표 가격은 필수입니다.") @DecimalMin(value = "0.0001", message = "목표 가격은 0보다 커야 합니다.") BigDecimal targetPrice, // 목표 가격을 보관한다.
        @NotNull(message = "손절 가격은 필수입니다.") @DecimalMin(value = "0.0001", message = "손절 가격은 0보다 커야 합니다.") BigDecimal stopPrice, // 손절 가격을 보관한다.
        @NotNull(message = "예상 매도일은 필수입니다.") LocalDate expectedExitAt, // 예상 매도일을 보관한다.
        @NotNull(message = "신뢰도는 필수입니다.") @Min(value = 0, message = "신뢰도는 0 이상이어야 합니다.") @Max(value = 100, message = "신뢰도는 100 이하여야 합니다.") Integer confidence, // 신뢰도를 보관한다.
        @NotBlank(message = "시그널 JSON은 필수입니다.") String signalsJson, // 시그널 JSON 문자열을 보관한다.
        @NotBlank(message = "모델 버전은 필수입니다.") String modelVersion, // 모델 버전을 보관한다.
        LocalDateTime generatedAt // 추천 생성 일시를 보관한다.
) { // 추천 생성 요청 DTO 본문을 시작한다.
} // 추천 생성 요청 DTO를 종료한다.
