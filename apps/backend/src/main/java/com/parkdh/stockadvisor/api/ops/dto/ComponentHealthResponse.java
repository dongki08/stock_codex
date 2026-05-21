package com.parkdh.stockadvisor.api.ops.dto; // 운영 DTO 패키지를 선언한다.

public record ComponentHealthResponse( // 개별 외부 연동 상태 응답 DTO를 정의한다.
        String name, // 구성 요소 이름을 보관한다.
        String status, // READY, MISSING_CONFIG, PUBLIC_SOURCE 같은 상태를 보관한다.
        String message // 상태 설명을 보관한다.
) { // 개별 외부 연동 상태 응답 DTO 본문을 시작한다.
} // 개별 외부 연동 상태 응답 DTO를 종료한다.
