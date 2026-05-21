package com.parkdh.stockadvisor.api.marketdata.dto; // 시장 데이터 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record PriceIntradayResponse( // 장중 가격 응답 DTO를 정의한다.
        String priceKey, // 가격 키를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        LocalDateTime tickAt, // 스냅샷 시각을 보관한다.
        BigDecimal price, // 현재가를 보관한다.
        BigDecimal volume, // 누적 거래량을 보관한다.
        String source // 데이터 출처를 보관한다.
) { // 장중 가격 응답 DTO 본문을 시작한다.
} // 장중 가격 응답 DTO를 종료한다.
