package com.parkdh.stockadvisor.api.autoresearch.dto; // AutoResearch DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record StrategyVersionResponse( // 전략 버전 응답 DTO를 정의한다.
        Long id, // 전략 버전 ID를 보관한다.
        String semver, // 전략 버전명을 보관한다.
        String gitSha, // 커밋 SHA를 보관한다.
        BigDecimal metricValue, // 지표 값을 보관한다.
        LocalDateTime promotedAt, // 승격 일시를 보관한다.
        Boolean champion // 챔피언 여부를 보관한다.
) { // 전략 버전 응답 DTO 본문을 시작한다.
} // 전략 버전 응답 DTO를 종료한다.
