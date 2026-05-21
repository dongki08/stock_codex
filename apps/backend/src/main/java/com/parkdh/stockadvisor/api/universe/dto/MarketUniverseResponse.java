package com.parkdh.stockadvisor.api.universe.dto; // 시장 유니버스 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.

public record MarketUniverseResponse( // 시장 유니버스 응답 DTO를 정의한다.
        String universeKey, // 유니버스 키를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        String name, // 종목명을 보관한다.
        String sector, // 섹터명을 보관한다.
        BigDecimal marketCap, // 시가총액을 보관한다.
        BigDecimal avgTurnover, // 평균 거래대금을 보관한다.
        BigDecimal lastPrice, // 최근 가격을 보관한다.
        Boolean tradable, // 거래 가능 여부를 보관한다.
        String source, // 데이터 출처를 보관한다.
        LocalDate lastSyncedAt // 마지막 동기화일을 보관한다.
) { // 시장 유니버스 응답 DTO 본문을 시작한다.
} // 시장 유니버스 응답 DTO를 종료한다.
