package com.parkdh.stockadvisor.api.brief.dto; // 데일리 브리프 DTO 패키지를 선언한다.

import jakarta.validation.constraints.NotBlank; // 공백 금지 검증 어노테이션을 가져온다.
import jakarta.validation.constraints.NotNull; // null 금지 검증 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record DailyBriefCreateRequest( // 데일리 브리프 생성 요청 DTO를 정의한다.
        @NotBlank(message = "시장 트랙은 필수입니다.") String marketTrack, // 시장 트랙을 보관한다.
        @NotBlank(message = "브리프 본문은 필수입니다.") String briefMd, // 브리프 본문을 보관한다.
        @NotNull(message = "초안 번호는 필수입니다.") Integer draftNo, // 초안 번호를 보관한다.
        BigDecimal coverage, // 커버리지 점수를 보관한다.
        Integer hallucinationFlags, // 환각 플래그 수를 보관한다.
        String llmModel, // LLM 모델명을 보관한다.
        LocalDateTime generatedAt // 생성 일시를 보관한다.
) { // 데일리 브리프 생성 요청 DTO 본문을 시작한다.
} // 데일리 브리프 생성 요청 DTO를 종료한다.
