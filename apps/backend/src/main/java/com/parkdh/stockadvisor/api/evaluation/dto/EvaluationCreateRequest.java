package com.parkdh.stockadvisor.api.evaluation.dto; // 평가 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record EvaluationCreateRequest( // 평가 생성 요청 DTO를 정의한다.
        @NotNull(message = "추천 ID는 필수입니다.") Long recommendationId, // 추천 ID를 보관한다.
        BigDecimal actualExitPrice, // 실제 매도 가격을 보관한다.
        @NotBlank(message = "청산 사유는 필수입니다.") String exitReason, // 청산 사유를 보관한다.
        @NotNull(message = "손익률은 필수입니다.") BigDecimal pnlPct, // 손익률을 보관한다.
        BigDecimal drawdownPct, // 최대 낙폭을 보관한다.
        @NotNull(message = "목표가 적중 여부는 필수입니다.") Boolean hitTarget, // 목표가 적중 여부를 보관한다.
        LocalDateTime evaluatedAt // 평가 일시를 보관한다.
) { // 평가 생성 요청 DTO 본문을 시작한다.
} // 평가 생성 요청 DTO를 종료한다.
