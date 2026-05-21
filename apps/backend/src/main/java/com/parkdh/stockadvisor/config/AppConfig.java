package com.parkdh.stockadvisor.config; // 애플리케이션 설정 패키지를 선언한다.

import org.springframework.boot.context.properties.ConfigurationPropertiesScan; // 설정 속성 자동 스캔 어노테이션을 가져온다.
import org.springframework.context.annotation.Configuration; // 스프링 설정 어노테이션을 가져온다.
import org.springframework.scheduling.annotation.EnableScheduling; // 스케줄러 활성화 어노테이션을 가져온다.

@Configuration // 스프링 설정 클래스로 등록한다.
@EnableScheduling // 스케줄러를 활성화한다.
@ConfigurationPropertiesScan("com.parkdh.stockadvisor.config") // 설정 속성 클래스를 자동 스캔한다.
public class AppConfig { // 애플리케이션 공통 설정 클래스를 정의한다.
} // 애플리케이션 설정 클래스를 종료한다.
