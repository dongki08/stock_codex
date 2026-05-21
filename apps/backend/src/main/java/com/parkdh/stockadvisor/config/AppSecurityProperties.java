package com.parkdh.stockadvisor.config; // 설정 패키지를 선언한다.

import org.springframework.boot.context.properties.ConfigurationProperties; // 설정 속성 바인딩 어노테이션을 가져온다.

import java.util.List; // 목록 타입을 가져온다.

@ConfigurationProperties(prefix = "stock-advisor.security") // 애플리케이션 보안 설정 속성을 바인딩한다.
public record AppSecurityProperties(
        List<String> allowedOrigins, // CORS 허용 origin 목록을 보관한다.
        boolean protectDevApi // 개발용 API 보호 여부를 보관한다.
) { // 애플리케이션 보안 설정 레코드를 시작한다.
    public AppSecurityProperties { // 기본값 보정을 수행한다.
        if (allowedOrigins == null || allowedOrigins.isEmpty()) { // 허용 origin 설정이 없는지 확인한다.
            allowedOrigins = List.of("http://localhost:5173", "http://127.0.0.1:5173"); // 로컬 프론트 기본 origin을 사용한다.
        } // 허용 origin 기본값 확인을 종료한다.
    } // 기본값 보정을 종료한다.
} // 애플리케이션 보안 설정 레코드를 종료한다.
