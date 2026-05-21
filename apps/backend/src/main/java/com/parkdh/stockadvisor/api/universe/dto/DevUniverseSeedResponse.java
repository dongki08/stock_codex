package com.parkdh.stockadvisor.api.universe.dto; // 시장 유니버스 DTO 패키지를 선언한다.

import java.util.List; // 목록 타입을 가져온다.

public record DevUniverseSeedResponse( // 개발용 유니버스 seed 응답 DTO를 정의한다.
        String market, // seed 대상 시장을 보관한다.
        Integer upsertedCount, // 저장 또는 갱신된 후보 수를 보관한다.
        List<String> universeKeys // 저장 또는 갱신된 유니버스 키 목록을 보관한다.
) { // 개발용 유니버스 seed 응답 DTO 본문을 시작한다.
} // 개발용 유니버스 seed 응답 DTO를 종료한다.
