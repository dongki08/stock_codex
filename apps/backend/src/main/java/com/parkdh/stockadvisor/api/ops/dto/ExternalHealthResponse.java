package com.parkdh.stockadvisor.api.ops.dto; // 운영 DTO 패키지를 선언한다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

public record ExternalHealthResponse( // 외부 연동 상태 응답 DTO를 정의한다.
        LocalDateTime checkedAt, // 점검 시각을 보관한다.
        List<ComponentHealthResponse> components // 구성 요소별 상태를 보관한다.
) { // 외부 연동 상태 응답 DTO 본문을 시작한다.
} // 외부 연동 상태 응답 DTO를 종료한다.
