package com.parkdh.stockadvisor.global.exception; // 예외 패키지를 선언한다.

public class CustomException extends RuntimeException { // 비즈니스 예외 클래스를 정의한다.
    private final int code; // 클라이언트에 전달할 에러 코드를 보관한다.

    public CustomException(String message, int code) { // 메시지와 코드로 예외를 생성한다.
        super(message); // 상위 RuntimeException에 메시지를 전달한다.
        this.code = code; // 에러 코드를 필드에 저장한다.
    } // 생성자를 종료한다.

    public int getCode() { // 에러 코드를 반환한다.
        return code; // 저장된 에러 코드를 반환한다.
    } // getter를 종료한다.
} // 예외 클래스를 종료한다.
