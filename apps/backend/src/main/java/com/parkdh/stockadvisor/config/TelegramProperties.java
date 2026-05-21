package com.parkdh.stockadvisor.config; // Telegram 설정 패키지를 선언한다.

import org.springframework.boot.context.properties.ConfigurationProperties; // 설정 속성 바인딩 어노테이션을 가져온다.

@ConfigurationProperties(prefix = "stock-advisor.telegram") // Telegram 설정 속성을 바인딩한다.
public record TelegramProperties(
        String botToken, // Telegram 봇 토큰을 보관한다.
        String chatId    // Telegram 채팅 ID를 보관한다.
) {} // Telegram 설정 레코드를 종료한다.
