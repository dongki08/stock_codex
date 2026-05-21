package com.parkdh.stockadvisor.api.instrument.dto; // 종목 DTO 패키지를 선언한다.

public record InstrumentResponse( // 종목 응답 DTO를 정의한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        String name, // 종목명을 보관한다.
        String sector, // 섹터명을 보관한다.
        Boolean enabled // 활성 여부를 보관한다.
) { // 종목 응답 DTO 본문을 시작한다.
} // 종목 응답 DTO를 종료한다.
