package com.parkdh.stockadvisor.application.recommendation; // 추천 애플리케이션 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.

public record PredictedRecommendation( // 추천 가격 산출 결과를 정의한다.
        BigDecimal entryPrice, // 진입 가격을 보관한다.
        BigDecimal targetPrice, // 목표 가격을 보관한다.
        BigDecimal stopPrice, // 손절 가격을 보관한다.
        LocalDate expectedExitAt, // 예상 청산일을 보관한다.
        String pricingMethod, // 가격 산출 방식을 보관한다.
        BigDecimal volatilityPct, // 최근 변동성(%)을 보관한다.
        BigDecimal positionSizingScore // 확신도·역변동성 비중 산출용 원점수를 보관한다.
) { // 추천 가격 산출 결과 본문을 시작한다.
    public PredictedRecommendation(BigDecimal entryPrice, BigDecimal targetPrice, BigDecimal stopPrice, LocalDate expectedExitAt, String pricingMethod) { // 기존 테스트/호출 호환 생성자를 정의한다.
        this(entryPrice, targetPrice, stopPrice, expectedExitAt, pricingMethod, BigDecimal.ZERO, BigDecimal.ZERO); // 비중 입력값이 없으면 0으로 둔다.
    } // 호환 생성자를 종료한다.
} // 추천 가격 산출 결과를 종료한다.
