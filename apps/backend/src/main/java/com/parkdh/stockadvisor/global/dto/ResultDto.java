package com.parkdh.stockadvisor.global.dto; // 공통 DTO 패키지를 선언한다.

import com.fasterxml.jackson.annotation.JsonInclude; // null 필드 제외 어노테이션을 가져온다.
import com.fasterxml.jackson.annotation.JsonProperty; // JSON 필드명 지정 어노테이션을 가져온다.

@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 필드는 응답 JSON에서 제외한다.
public record ResultDto<T>( // 공통 API 응답 레코드를 정의한다.
        Integer code, // 응답 코드를 보관한다.
        T data, // 성공 응답 데이터를 보관한다.
        @JsonProperty("error_message") String errorMessage // 에러 메시지 JSON 필드를 보관한다.
) { // 레코드 본문을 시작한다.
    public static <T> ResultDto<T> success(T data) { // 데이터가 있는 성공 응답을 생성한다.
        return new ResultDto<>(200, data, null); // 성공 코드와 데이터를 담아 반환한다.
    } // 데이터 포함 성공 메서드를 종료한다.

    public static ResultDto<?> success() { // 데이터가 없는 성공 응답을 생성한다.
        return new ResultDto<>(200, null, null); // 성공 코드만 담아 반환한다.
    } // 데이터 없는 성공 메서드를 종료한다.

    public static ResultDto<?> error(Integer code, String errorMessage) { // 에러 응답을 생성한다.
        return new ResultDto<>(code, null, errorMessage); // 에러 코드와 메시지를 담아 반환한다.
    } // 에러 메서드를 종료한다.
} // 응답 레코드를 종료한다.
