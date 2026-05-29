package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.price.PriceIntradayEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExitMonitorJobTest {
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private KisApiClient kisApiClient;
    @Mock
    private StooqQuoteClient stooqQuoteClient;
    @Mock
    private MarketDataSyncService marketDataSyncService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private PriceIntradayRepository priceIntradayRepository;

    private ExitMonitorJob newJob() {
        return new ExitMonitorJob(
                recommendationRepository,
                evaluationRepository,
                kisApiClient,
                stooqQuoteClient,
                marketDataSyncService,
                notificationService,
                appSettingRepository,
                priceIntradayRepository,
                new ObjectMapper()
        );
    }

    @Test
    void eventDedupeKeyUsesEventAndPriceBucketInsteadOfDateOnly() throws Exception {
        ExitMonitorJob job = newJob();
        RecommendationEntity recommendation = new RecommendationEntity(
                "AAPL",
                "NASDAQ",
                "SHORT",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(95),
                LocalDate.now().plusDays(5),
                70,
                "{}",
                "dev-rule-v0",
                LocalDateTime.now(),
                "OPEN"
        );
        Field idField = RecommendationEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(recommendation, 7L);

        Method method = ExitMonitorJob.class.getDeclaredMethod("buildEventKey", RecommendationEntity.class, String.class, BigDecimal.class);
        method.setAccessible(true);

        String key = (String) method.invoke(job, recommendation, "STOP", BigDecimal.valueOf(94.90));

        assertThat(key).isEqualTo("exit-monitor:7:STOP:0.95");
    }

    @Test
    void closeRecommendationWithEvaluationStoresDrawdownFromIntradayLow() throws Exception {
        ExitMonitorJob job = newJob();
        RecommendationEntity recommendation = new RecommendationEntity(
                "005930",
                "KOSPI",
                "SHORT",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(95),
                LocalDate.now().plusDays(5),
                70,
                "{}",
                "dev-rule-v0",
                LocalDateTime.now().minusHours(2),
                "OPEN"
        );
        Field idField = RecommendationEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(recommendation, 8L);
        when(evaluationRepository.existsByRecommendationId(8L)).thenReturn(false);
        when(priceIntradayRepository.findByMarketAndTickerAndTickAtBetweenOrderByTickAtAsc(any(), any(), any(), any()))
                .thenReturn(List.of(
                        new PriceIntradayEntity("005930", "KOSPI", LocalDateTime.now().minusHours(1), BigDecimal.valueOf(98), BigDecimal.ONE, "KIS"),
                        new PriceIntradayEntity("005930", "KOSPI", LocalDateTime.now().minusMinutes(30), BigDecimal.valueOf(92), BigDecimal.ONE, "KIS")
                ));

        Method method = ExitMonitorJob.class.getDeclaredMethod("closeRecommendationWithEvaluation", RecommendationEntity.class, BigDecimal.class, String.class, boolean.class, String.class);
        method.setAccessible(true);
        method.invoke(job, recommendation, BigDecimal.valueOf(106), "TARGET_HIT", true, "CLOSED");

        ArgumentCaptor<EvaluationEntity> captor = ArgumentCaptor.forClass(EvaluationEntity.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getDrawdownPct()).isEqualByComparingTo("-8.0000");
        assertThat(recommendation.getStatus()).isEqualTo("CLOSED");
    }
}
