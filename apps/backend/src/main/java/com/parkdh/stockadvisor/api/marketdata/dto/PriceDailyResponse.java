package com.parkdh.stockadvisor.api.marketdata.dto; // 시장 데이터 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.

public record PriceDailyResponse( // 일봉 가격 응답 DTO를 정의한다.
        String priceKey, // 가격 키를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        LocalDate tradeDate, // 거래일을 보관한다.
        BigDecimal openPrice, // 시가를 보관한다.
        BigDecimal highPrice, // 고가를 보관한다.
        BigDecimal lowPrice, // 저가를 보관한다.
        BigDecimal closePrice, // 종가를 보관한다.
        BigDecimal volume, // 거래량을 보관한다.
        BigDecimal turnover, // 거래대금을 보관한다.
        String source // 데이터 출처를 보관한다.
) { // 일봉 가격 응답 DTO 본문을 시작한다.
} // 일봉 가격 응답 DTO를 종료한다.
