package com.parkdh.stockadvisor.api.dev.dto; // 개발용 DTO 패키지를 선언한다.

import java.util.List; // 목록 타입을 가져온다.

public record DevRecommendationGenerateResponse( // 개발용 추천 생성 응답 DTO를 정의한다.
        String market, // 생성 대상 시장을 보관한다.
        Integer sourceInstrumentCount, // 추천 생성에 사용한 종목 수를 보관한다.
        Integer generatedPredictionCount, // 생성된 가격 예측 수를 보관한다.
        Integer generatedRecommendationCount, // 생성된 추천 수를 보관한다.
        List<Long> predictionIds, // 생성된 가격 예측 ID 목록을 보관한다.
        List<Long> recommendationIds // 생성된 추천 ID 목록을 보관한다.
) { // 개발용 추천 생성 응답 DTO 본문을 시작한다.
} // 개발용 추천 생성 응답 DTO를 종료한다.
