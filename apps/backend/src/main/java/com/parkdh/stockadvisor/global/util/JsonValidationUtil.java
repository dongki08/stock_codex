package com.parkdh.stockadvisor.global.util; // 공통 유틸 패키지를 선언한다.

import com.fasterxml.jackson.core.JsonProcessingException; // JSON 처리 예외를 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱 도구를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.

public final class JsonValidationUtil { // JSON 검증 유틸 클래스를 정의한다.
    private JsonValidationUtil() { // 인스턴스 생성을 막는 생성자를 정의한다.
    } // 생성자를 종료한다.

    public static void validate(ObjectMapper objectMapper, String json, String fieldName) { // JSON 문자열 형식을 검증한다.
        try { // JSON 파싱 예외를 처리하기 위해 시도 블록을 시작한다.
            objectMapper.readTree(json); // JSON 문자열을 파싱해 형식 오류를 확인한다.
        } catch (JsonProcessingException exception) { // JSON 파싱 실패를 잡는다.
            throw new CustomException(fieldName + " 형식이 올바르지 않습니다.", 400); // 클라이언트 오류 예외를 던진다.
        } // 예외 처리 블록을 종료한다.
    } // JSON 검증 메서드를 종료한다.
} // JSON 검증 유틸 클래스를 종료한다.
