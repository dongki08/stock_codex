package com.parkdh.stockadvisor.global.exception; // 테스트 대상 패키지를 선언한다.

import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import org.junit.jupiter.api.Test; // 테스트 어노테이션을 가져온다.
import org.springframework.http.ResponseEntity; // HTTP 응답 엔티티를 가져온다.

import static org.assertj.core.api.Assertions.assertThat; // 검증 메서드를 가져온다.

class GlobalExceptionHandlerTest { // 전역 예외 처리 테스트 클래스를 정의한다.
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(); // 테스트할 예외 처리기를 생성한다.

    @Test // 커스텀 예외 변환을 검증한다.
    void customExceptionBecomesResultDtoErrorResponse() { // 커스텀 예외 응답 테스트를 정의한다.
        ResponseEntity<ResultDto<?>> response = handler.handleCustomException(new CustomException("설정을 찾을 수 없습니다.", 404)); // 커스텀 예외를 응답으로 변환한다.
        assertThat(response.getStatusCode().value()).isEqualTo(404); // HTTP 상태 코드가 404인지 검증한다.
        assertThat(response.getBody()).isNotNull(); // 응답 본문이 존재하는지 검증한다.
        assertThat(response.getBody().code()).isEqualTo(404); // 본문 코드가 404인지 검증한다.
        assertThat(response.getBody().errorMessage()).isEqualTo("설정을 찾을 수 없습니다."); // 에러 메시지를 검증한다.
    } // 테스트 메서드를 종료한다.
} // 테스트 클래스를 종료한다.
