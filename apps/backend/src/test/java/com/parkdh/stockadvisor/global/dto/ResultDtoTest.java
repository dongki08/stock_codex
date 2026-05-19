package com.parkdh.stockadvisor.global.dto; // 테스트 대상 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 직렬화 도구를 가져온다.
import org.junit.jupiter.api.Test; // 테스트 어노테이션을 가져온다.

import java.util.Map; // 테스트 데이터 맵을 가져온다.

import static org.assertj.core.api.Assertions.assertThat; // 검증 메서드를 가져온다.

class ResultDtoTest { // 공통 응답 DTO 테스트 클래스를 정의한다.
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 직렬화를 수행할 ObjectMapper를 준비한다.

    @Test // 성공 응답 직렬화를 검증한다.
    void successWithDataContainsCodeAndData() throws Exception { // 데이터 포함 성공 응답 테스트를 정의한다.
        ResultDto<?> result = ResultDto.success(Map.of("name", "stock")); // 성공 응답 객체를 생성한다.
        String json = objectMapper.writeValueAsString(result); // 응답 객체를 JSON 문자열로 변환한다.
        assertThat(json).contains("\"code\":200"); // 성공 코드가 포함되는지 검증한다.
        assertThat(json).contains("\"data\":{\"name\":\"stock\"}"); // 데이터 필드가 포함되는지 검증한다.
    } // 테스트 메서드를 종료한다.

    @Test // 에러 응답 직렬화를 검증한다.
    void errorContainsCodeAndErrorMessage() throws Exception { // 에러 응답 테스트를 정의한다.
        ResultDto<?> result = ResultDto.error(404, "설정을 찾을 수 없습니다."); // 에러 응답 객체를 생성한다.
        String json = objectMapper.writeValueAsString(result); // 응답 객체를 JSON 문자열로 변환한다.
        assertThat(json).contains("\"code\":404"); // 에러 코드가 포함되는지 검증한다.
        assertThat(json).contains("\"error_message\":\"설정을 찾을 수 없습니다.\""); // 에러 메시지가 포함되는지 검증한다.
    } // 테스트 메서드를 종료한다.
} // 테스트 클래스를 종료한다.
