package com.parkdh.stockadvisor.application.recommendation; // 추천 애플리케이션 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.

public record RecommendationCandidate( // 추천 후보 값을 정의한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        BigDecimal lastPrice, // 최근 가격을 보관한다.
        String sector, // 섹터를 보관한다.
        String source, // 후보 출처를 보관한다.
        Integer score, // 종합 점수를 보관한다.
        Integer dataQualityScore, // 데이터 품질 점수를 보관한다.
        String featureJson // feature JSON을 보관한다.
) { // 추천 후보 값 본문을 시작한다.
} // 추천 후보 값을 종료한다.
