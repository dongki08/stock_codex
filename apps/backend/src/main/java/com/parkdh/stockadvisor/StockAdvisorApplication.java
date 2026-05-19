package com.parkdh.stockadvisor; // 애플리케이션 기본 패키지를 선언한다.

import org.springframework.boot.SpringApplication; // 스프링 부트 실행 도구를 가져온다.
import org.springframework.boot.autoconfigure.SpringBootApplication; // 스프링 부트 애플리케이션 어노테이션을 가져온다.

@SpringBootApplication // 자동 설정과 컴포넌트 스캔을 활성화한다.
public class StockAdvisorApplication { // 애플리케이션 시작 클래스를 정의한다.
    public static void main(String[] args) { // JVM 실행 진입점을 정의한다.
        SpringApplication.run(StockAdvisorApplication.class, args); // 스프링 부트 애플리케이션을 실행한다.
    } // main 메서드를 종료한다.
} // 시작 클래스를 종료한다.
