package com.parkdh.stockadvisor.api.feature.dto; // 후보군 feature DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.

public record UniverseFeatureResponse( // 후보군 feature 응답 DTO를 정의한다.
        String universeKey, // 유니버스 키를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        String name, // 종목명을 보관한다.
        BigDecimal lastPrice, // 최근 가격을 보관한다.
        BigDecimal avgTurnover, // 평균 거래대금을 보관한다.
        Integer liquidityScore, // 유동성 점수를 보관한다.
        Integer priceScore, // 가격 점수를 보관한다.
        Integer movingAverageScore, // 이동평균 추세 점수를 보관한다.
        Integer rsiScore, // RSI 점수를 보관한다.
        Integer volumeScore, // 거래량 점수를 보관한다.
        Integer technicalScore, // 기술적 종합 점수를 보관한다.
        Integer dataQualityScore, // 데이터 품질 점수를 보관한다.
        Integer priceHistoryCount, // 가격 히스토리 개수를 보관한다.
        Integer totalScore, // 종합 점수를 보관한다.
        String featureJson // feature 원본 JSON 문자열을 보관한다.
) { // 후보군 feature 응답 DTO 본문을 시작한다.
} // 후보군 feature 응답 DTO를 종료한다.
