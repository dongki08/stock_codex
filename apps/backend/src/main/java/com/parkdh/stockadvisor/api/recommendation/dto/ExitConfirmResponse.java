package com.parkdh.stockadvisor.api.recommendation.dto; // 추천 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record ExitConfirmResponse( // Exit Confirm 응답 DTO를 정의한다.
        Long recommendationId, // 추천 ID를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        String market, // 시장 구분을 보관한다.
        BigDecimal currentPrice, // 판단 기준 현재가를 보관한다.
        BigDecimal stopPrice, // 손절가를 보관한다.
        BigDecimal distancePct, // 손절가 대비 이격률을 보관한다.
        String action, // HOLD, CUT, TIGHTEN, DATA_REQUIRED를 보관한다.
        String rationale, // 판단 근거를 보관한다.
        boolean usedFallback, // 로컬 fallback 사용 여부를 보관한다.
        String codexError, // Codex 오류 메시지를 보관한다.
        LocalDateTime confirmedAt // 판단 시각을 보관한다.
) { // Exit Confirm 응답 DTO 본문을 시작한다.
} // Exit Confirm 응답 DTO를 종료한다.
