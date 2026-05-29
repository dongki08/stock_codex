package com.parkdh.stockadvisor.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {
    @Test
    void taskSchedulerUsesConfiguredPoolSizeAndThreadPrefix() {
        AppConfig config = new AppConfig();
        ThreadPoolTaskScheduler scheduler = config.taskScheduler(6);

        scheduler.initialize();
        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(6);
            assertThat(scheduler.getThreadNamePrefix()).isEqualTo("stock-scheduler-");
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void taskSchedulerKeepsAtLeastTwoThreads() {
        AppConfig config = new AppConfig();
        ThreadPoolTaskScheduler scheduler = config.taskScheduler(1);

        scheduler.initialize();
        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(2);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void codexTaskExecutorUsesSmallBoundedPool() {
        AppConfig config = new AppConfig();
        ThreadPoolTaskExecutor executor = config.codexTaskExecutor(3);

        executor.initialize();
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(3);
            assertThat(executor.getMaxPoolSize()).isEqualTo(3);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("codex-task-");
            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(20);
        } finally {
            executor.shutdown();
        }
    }
}
