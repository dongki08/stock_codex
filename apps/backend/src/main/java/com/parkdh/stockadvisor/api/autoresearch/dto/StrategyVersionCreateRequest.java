package com.parkdh.stockadvisor.api.autoresearch.dto; // AutoResearch DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record StrategyVersionCreateRequest( // 전략 버전 생성 요청 DTO를 정의한다.
        @NotBlank(message = "전략 버전명은 필수입니다.") String semver, // 전략 버전명을 보관한다.
        @NotBlank(message = "커밋 SHA는 필수입니다.") String gitSha, // 커밋 SHA를 보관한다.
        @NotNull(message = "지표 값은 필수입니다.") BigDecimal metricValue, // 지표 값을 보관한다.
        LocalDateTime promotedAt, // 승격 일시를 보관한다.
        @NotNull(message = "챔피언 여부는 필수입니다.") Boolean champion // 챔피언 여부를 보관한다.
) { // 전략 버전 생성 요청 DTO 본문을 시작한다.
} // 전략 버전 생성 요청 DTO를 종료한다.
