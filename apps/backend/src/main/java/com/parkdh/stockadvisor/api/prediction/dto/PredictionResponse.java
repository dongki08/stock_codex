package com.parkdh.stockadvisor.api.prediction.dto; // 예측 DTO 패키지를 선언한다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

public record PredictionResponse( // 예측 응답 DTO를 정의한다.
        Long id, // 예측 ID를 보관한다.
        String ticker, // 종목 코드를 보관한다.
        Integer horizonDays, // 예측 기간 일수를 보관한다.
        BigDecimal predictedPrice, // 예측 가격을 보관한다.
        String modelVersion, // 모델 버전을 보관한다.
        LocalDateTime generatedAt // 예측 생성 일시를 보관한다.
) { // 예측 응답 DTO 본문을 시작한다.
} // 예측 응답 DTO를 종료한다.
