package com.parkdh.stockadvisor.config; // KIS 설정 패키지를 선언한다.

import org.springframework.boot.context.properties.ConfigurationProperties; // 설정 속성 바인딩 어노테이션을 가져온다.

@ConfigurationProperties(prefix = "stock-advisor.kis") // KIS API 설정 속성을 바인딩한다.
public record KisProperties(
        String appKey,   // KIS API 앱 키를 보관한다.
        String appSecret, // KIS API 앱 시크릿을 보관한다.
        String baseUrl   // KIS API 기본 URL을 보관한다.
) {} // KIS 설정 레코드를 종료한다.
