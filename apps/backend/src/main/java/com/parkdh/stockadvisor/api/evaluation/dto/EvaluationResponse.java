package com.parkdh.stockadvisor.api.evaluation.dto; // 평가 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record EvaluationResponse( // 평가 응답 DTO를 정의한다.
        Long id, // 평가 ID를 보관한다.
        Long recommendationId, // 추천 ID를 보관한다.
        BigDecimal actualExitPrice, // 실제 매도 가격을 보관한다.
        String exitReason, // 청산 사유를 보관한다.
        BigDecimal pnlPct, // 손익률을 보관한다.
        BigDecimal drawdownPct, // 최대 낙폭을 보관한다.
        Boolean hitTarget, // 목표가 적중 여부를 보관한다.
        LocalDateTime evaluatedAt // 평가 일시를 보관한다.
) { // 평가 응답 DTO 본문을 시작한다.
} // 평가 응답 DTO를 종료한다.
