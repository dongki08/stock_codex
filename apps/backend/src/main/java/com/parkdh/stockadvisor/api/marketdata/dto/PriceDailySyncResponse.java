package com.parkdh.stockadvisor.api.marketdata.dto; // 시장 데이터 DTO 패키지를 선언한다.

import java.util.List; // 목록 타입을 가져온다.

public record PriceDailySyncResponse( // 일봉 가격 동기화 응답 DTO를 정의한다.
        String market, // 동기화 대상 시장을 보관한다.
        Integer candidateCount, // 동기화 대상 후보 수를 보관한다.
        Integer fetchedCount, // 외부 소스에서 읽은 일봉 수를 보관한다.
        Integer upsertedCount, // 저장 또는 갱신한 일봉 수를 보관한다.
        List<String> samplePriceKeys // 샘플 가격 키 목록을 보관한다.
) { // 일봉 가격 동기화 응답 DTO 본문을 시작한다.
} // 일봉 가격 동기화 응답 DTO를 종료한다.
