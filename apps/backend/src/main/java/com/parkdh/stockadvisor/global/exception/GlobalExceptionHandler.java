package com.parkdh.stockadvisor.global.exception; // 예외 처리 패키지를 선언한다.

import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import org.springframework.http.ResponseEntity; // HTTP 응답 엔티티를 가져온다.
import org.springframework.web.bind.MethodArgumentNotValidException; // 검증 실패 예외를 가져온다.
import org.springframework.web.bind.annotation.ExceptionHandler; // 예외 처리 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestControllerAdvice; // 전역 REST 예외 처리 어노테이션을 가져온다.

@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리한다.
public class GlobalExceptionHandler { // 전역 예외 처리 클래스를 정의한다.
    @ExceptionHandler(CustomException.class) // 커스텀 예외를 처리한다.
    public ResponseEntity<ResultDto<?>> handleCustomException(CustomException exception) { // 커스텀 예외 응답을 만든다.
        return ResponseEntity.status(exception.getCode()).body(ResultDto.error(exception.getCode(), exception.getMessage())); // 예외 코드와 메시지를 응답한다.
    } // 커스텀 예외 처리 메서드를 종료한다.

    @ExceptionHandler(MethodArgumentNotValidException.class) // Bean Validation 예외를 처리한다.
    public ResponseEntity<ResultDto<?>> handleValidationException(MethodArgumentNotValidException exception) { // 검증 실패 응답을 만든다.
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst().map(error -> error.getDefaultMessage()).orElse("입력값이 올바르지 않습니다."); // 첫 번째 검증 메시지를 선택한다.
        return ResponseEntity.badRequest().body(ResultDto.error(400, message)); // 400 응답과 검증 메시지를 반환한다.
    } // 검증 예외 처리 메서드를 종료한다.

    @ExceptionHandler(Exception.class) // 처리되지 않은 예외를 처리한다.
    public ResponseEntity<ResultDto<?>> handleException(Exception exception) { // 서버 오류 응답을 만든다.
        return ResponseEntity.internalServerError().body(ResultDto.error(500, "서버 처리 중 오류가 발생했습니다.")); // 500 응답과 공통 메시지를 반환한다.
    } // 서버 오류 처리 메서드를 종료한다.
} // 전역 예외 처리 클래스를 종료한다.
