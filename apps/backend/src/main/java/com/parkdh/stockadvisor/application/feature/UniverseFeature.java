package com.parkdh.stockadvisor.application.feature; // 후보군 feature 애플리케이션 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.

public record UniverseFeature( // 후보군 feature 값을 정의한다.
        MarketUniverseEntity entity, // 원본 시장 유니버스 엔티티를 보관한다.
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
) { // 후보군 feature 본문을 시작한다.
    public String ticker() { return entity.getTicker(); } // 종목 코드를 반환한다.

    public String market() { return entity.getMarket(); } // 시장 구분을 반환한다.

    public BigDecimal lastPrice() { return entity.getLastPrice(); } // 최근 가격을 반환한다.
} // 후보군 feature를 종료한다.
