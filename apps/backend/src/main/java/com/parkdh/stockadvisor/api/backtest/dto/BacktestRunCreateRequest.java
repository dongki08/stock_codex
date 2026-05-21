package com.parkdh.stockadvisor.api.backtest.dto; // 백테스트 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.time.LocalDate; // 날짜 타입을 가져온다.

public record BacktestRunCreateRequest( // 백테스트 실행 생성 요청 DTO를 정의한다.
        @NotBlank(message = "전략명은 필수입니다.") String strategy, // 전략명을 보관한다.
        @NotNull(message = "기간 시작일은 필수입니다.") LocalDate periodFrom, // 기간 시작일을 보관한다.
        @NotNull(message = "기간 종료일은 필수입니다.") LocalDate periodTo, // 기간 종료일을 보관한다.
        @NotBlank(message = "지표 JSON은 필수입니다.") String metricsJson // 지표 JSON 문자열을 보관한다.
) { // 백테스트 실행 생성 요청 DTO 본문을 시작한다.
} // 백테스트 실행 생성 요청 DTO를 종료한다.
