package com.parkdh.stockadvisor.api.autoresearch.dto; // AutoResearch DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.UUID; // UUID 타입을 가져온다.

public record AutoresearchRunCreateRequest( // AutoResearch 실행 생성 요청 DTO를 정의한다.
        @NotNull(message = "작업 실행 UUID는 필수입니다.") UUID jobRunId, // 작업 실행 UUID를 보관한다.
        @NotNull(message = "반복 번호는 필수입니다.") Integer iterNo, // 반복 번호를 보관한다.
        String parentSha, // 부모 커밋 SHA를 보관한다.
        String proposalSha, // 제안 커밋 SHA를 보관한다.
        String diffSummary, // 변경 요약을 보관한다.
        String metricName, // 지표명을 보관한다.
        BigDecimal metricValue, // 지표 값을 보관한다.
        BigDecimal championMetric, // 챔피언 지표 값을 보관한다.
        @NotBlank(message = "실험 결정은 필수입니다.") String decision, // 실험 결정을 보관한다.
        Integer durationMs, // 소요 시간을 보관한다.
        LocalDateTime startedAt, // 시작 일시를 보관한다.
        LocalDateTime endedAt // 종료 일시를 보관한다.
) { // AutoResearch 실행 생성 요청 DTO 본문을 시작한다.
} // AutoResearch 실행 생성 요청 DTO를 종료한다.
