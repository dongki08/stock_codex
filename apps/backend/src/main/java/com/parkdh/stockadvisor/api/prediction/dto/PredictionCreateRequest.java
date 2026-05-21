package com.parkdh.stockadvisor.api.prediction.dto; // 예측 DTO 패키지를 선언한다.

import jakarta.validation.constraints.DecimalMin; // 최소 숫자 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.Min; // 최소값 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record PredictionCreateRequest( // 예측 생성 요청 DTO를 정의한다.
        @NotBlank(message = "종목 코드는 필수입니다.") String ticker, // 종목 코드를 보관한다.
        @NotNull(message = "예측 기간 일수는 필수입니다.") @Min(value = 1, message = "예측 기간 일수는 1 이상이어야 합니다.") Integer horizonDays, // 예측 기간 일수를 보관한다.
        @NotNull(message = "예측 가격은 필수입니다.") @DecimalMin(value = "0.0001", message = "예측 가격은 0보다 커야 합니다.") BigDecimal predictedPrice, // 예측 가격을 보관한다.
        @NotBlank(message = "모델 버전은 필수입니다.") String modelVersion, // 모델 버전을 보관한다.
        LocalDateTime generatedAt // 예측 생성 일시를 보관한다.
) { // 예측 생성 요청 DTO 본문을 시작한다.
} // 예측 생성 요청 DTO를 종료한다.
