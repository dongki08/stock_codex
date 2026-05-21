package com.parkdh.stockadvisor.api.instrument.dto; // 종목 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

public record InstrumentCreateRequest( // 종목 생성 요청 DTO를 정의한다.
        @NotBlank(message = "종목 코드는 필수입니다.") String ticker, // 종목 코드를 보관한다.
        @NotBlank(message = "시장 구분은 필수입니다.") String market, // 시장 구분을 보관한다.
        @NotBlank(message = "종목명은 필수입니다.") String name, // 종목명을 보관한다.
        String sector, // 섹터명을 보관한다.
        @NotNull(message = "활성 여부는 필수입니다.") Boolean enabled // 활성 여부를 보관한다.
) { // 종목 생성 요청 DTO 본문을 시작한다.
} // 종목 생성 요청 DTO를 종료한다.
