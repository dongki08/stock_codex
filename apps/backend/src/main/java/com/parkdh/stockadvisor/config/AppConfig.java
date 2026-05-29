package com.parkdh.stockadvisor.config; // 애플리케이션 설정 패키지를 선언한다.

import org.springframework.boot.context.properties.ConfigurationPropertiesScan; // 설정 속성 자동 스캔 어노테이션을 가져온다.
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration; // 스프링 설정 어노테이션을 가져온다.
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // 스케줄러 활성화 어노테이션을 가져온다.
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration // 스프링 설정 클래스로 등록한다.
@EnableScheduling // 스케줄러를 활성화한다.
@ConfigurationPropertiesScan("com.parkdh.stockadvisor.config") // 설정 속성 클래스를 자동 스캔한다.
public class AppConfig { // 애플리케이션 공통 설정 클래스를 정의한다.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler(@Value("${stock-advisor.scheduler.pool-size:8}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(poolSize, 2));
        scheduler.setThreadNamePrefix("stock-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Bean("codexTaskExecutor")
    public ThreadPoolTaskExecutor codexTaskExecutor(@Value("${stock-advisor.codex.executor.pool-size:2}") int poolSize) {
        int safePoolSize = Math.max(poolSize, 1);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(safePoolSize);
        executor.setMaxPoolSize(safePoolSize);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("codex-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
} // 애플리케이션 설정 클래스를 종료한다.
