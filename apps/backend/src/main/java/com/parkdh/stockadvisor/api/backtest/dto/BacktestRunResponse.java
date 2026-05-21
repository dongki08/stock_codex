package com.parkdh.stockadvisor.api.backtest.dto; // 백테스트 DTO 패키지를 선언한다.

import java.time.LocalDate; // 날짜 타입을 가져온다.

public record BacktestRunResponse( // 백테스트 실행 응답 DTO를 정의한다.
        Long id, // 백테스트 실행 ID를 보관한다.
        String strategy, // 전략명을 보관한다.
        LocalDate periodFrom, // 기간 시작일을 보관한다.
        LocalDate periodTo, // 기간 종료일을 보관한다.
        String metricsJson // 지표 JSON을 보관한다.
) { // 백테스트 실행 응답 DTO 본문을 시작한다.
} // 백테스트 실행 응답 DTO를 종료한다.
