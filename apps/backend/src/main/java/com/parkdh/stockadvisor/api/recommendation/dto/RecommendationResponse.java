package com.parkdh.stockadvisor.api.recommendation.dto; // 추천 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record RecommendationResponse( // 추천 응답 DTO를 정의한다.
        Long id, // 추천 ID를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        String term, // 보유 기간 구분을 보관한다.
        BigDecimal entryPrice, // 진입 가격을 보관한다.
        BigDecimal targetPrice, // 목표 가격을 보관한다.
        BigDecimal stopPrice, // 손절 가격을 보관한다.
        LocalDate expectedExitAt, // 예상 매도일을 보관한다.
        Integer confidence, // 신뢰도를 보관한다.
        String signalsJson, // 시그널 JSON을 보관한다.
        String modelVersion, // 모델 버전을 보관한다.
        LocalDateTime generatedAt, // 추천 생성 일시를 보관한다.
        String status // 추천 상태를 보관한다.
) { // 추천 응답 DTO 본문을 시작한다.
} // 추천 응답 DTO를 종료한다.
