package com.parkdh.stockadvisor.api.dev.dto; // 개발용 DTO 패키지를 선언한다.

import java.util.List; // 목록 타입을 가져온다.

public record DevRecommendationGenerateResponse( // 개발용 추천 생성 응답 DTO를 정의한다.
        String market, // 생성 대상 시장을 보관한다.
        Integer sourceInstrumentCount, // 추천 생성에 사용한 종목 수를 보관한다.
        Integer generatedPredictionCount, // 생성된 가격 예측 수를 보관한다.
        Integer generatedRecommendationCount, // 생성된 추천 수를 보관한다.
        List<Long> predictionIds, // 생성된 가격 예측 ID 목록을 보관한다.
        List<Long> recommendationIds, // 생성된 추천 ID 목록을 보관한다.
        List<RecommendationSummary> recommendations // 생성된 추천 요약 목록을 보관한다.
) { // 개발용 추천 생성 응답 DTO 본문을 시작한다.
    public record RecommendationSummary( // 추천 요약 값을 정의한다.
            Long id, // 추천 ID를 보관한다.
            String ticker, // 종목 코드를 보관한다.
            String name, // 종목명을 보관한다.
            String market, // 시장 구분을 보관한다.
            String term, // 추천 기간을 보관한다.
            java.math.BigDecimal entryPrice, // 진입 가격을 보관한다.
            java.math.BigDecimal targetPrice, // 목표 가격을 보관한다.
            java.math.BigDecimal stopPrice, // 손절 가격을 보관한다.
            java.time.LocalDate expectedExitAt // 예상 청산일을 보관한다.
    ) { // 추천 요약 본문을 시작한다.
    } // 추천 요약을 종료한다.
} // 개발용 추천 생성 응답 DTO를 종료한다.
