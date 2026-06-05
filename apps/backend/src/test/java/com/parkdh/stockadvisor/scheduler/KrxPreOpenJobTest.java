package com.parkdh.stockadvisor.scheduler;

import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse;
import com.parkdh.stockadvisor.application.dev.DevRecommendationGenerateService;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.application.universe.MarketUniverseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KrxPreOpenJobTest {
    @Mock
    private MarketUniverseService marketUniverseService;
    @Mock
    private MarketDataSyncService marketDataSyncService;
    @Mock
    private DevRecommendationGenerateService devRecommendationGenerateService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerSettingReader schedulerSettingReader;
    @InjectMocks
    private KrxPreOpenJob job;

    @Test
    void preOpenDailySyncWalksBackToLatestTradingDayWhenHolidayReturnsNoRows() {
        LocalDate today = LocalDate.of(2026, 6, 4);
        LocalDate holiday = LocalDate.of(2026, 6, 3);
        LocalDate latestTradingDay = LocalDate.of(2026, 6, 2);
        when(marketDataSyncService.syncKrxDailyPricesForDate("KOSPI", holiday))
                .thenReturn(response(holiday, 0));
        when(marketDataSyncService.syncKrxDailyPricesForDate("KOSPI", latestTradingDay))
                .thenReturn(response(latestTradingDay, 948));

        PriceDailySyncResponse response = job.syncKrxDailyForPreOpen(today, "KOSPI");

        assertThat(response.targetDate()).isEqualTo(latestTradingDay);
        assertThat(response.fetchedCount()).isEqualTo(948);
        verify(marketDataSyncService).syncKrxDailyPricesForDate("KOSPI", holiday);
        verify(marketDataSyncService).syncKrxDailyPricesForDate("KOSPI", latestTradingDay);
    }

    private PriceDailySyncResponse response(LocalDate date, int count) {
        return new PriceDailySyncResponse("KOSPI", count, 1, 0, 0, count, count, date, "KRX_OPENAPI_DAILY", List.of());
    }
}
