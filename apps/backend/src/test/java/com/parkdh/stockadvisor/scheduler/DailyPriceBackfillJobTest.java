package com.parkdh.stockadvisor.scheduler;

import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyPriceBackfillJobTest {
    @Mock
    private MarketDataSyncService marketDataSyncService;
    @Mock
    private SchedulerSettingReader schedulerSettingReader;
    @Mock
    private NotificationService notificationService;

    @Test
    void runUsBackfillUsesBootstrapSyncForConfiguredMarkets() {
        when(schedulerSettingReader.getBoolean("collection.dailyPriceBackfill.enabled", true)).thenReturn(true);
        when(schedulerSettingReader.getInt("collection.dailyPriceBackfill.us.limitPerMarket", 50)).thenReturn(12);
        when(schedulerSettingReader.getInt("collection.dailyPriceBackfill.us.days", 180)).thenReturn(90);
        when(marketDataSyncService.syncDailyPrices("NASDAQ", 12, 90)).thenReturn(response("NASDAQ"));
        when(marketDataSyncService.syncDailyPrices("NYSE", 12, 90)).thenReturn(response("NYSE"));

        new DailyPriceBackfillJob(marketDataSyncService, schedulerSettingReader, notificationService).runUsBackfill();

        verify(marketDataSyncService).syncDailyPrices("NASDAQ", 12, 90);
        verify(marketDataSyncService).syncDailyPrices("NYSE", 12, 90);
        verify(notificationService).sendTelegramOnce(
                org.mockito.ArgumentMatchers.startsWith("daily-price-backfill:US:"),
                org.mockito.ArgumentMatchers.contains("requested=4")
        );
    }

    @Test
    void runKrxBackfillSkipsWhenDisabled() {
        when(schedulerSettingReader.getBoolean("collection.dailyPriceBackfill.enabled", true)).thenReturn(false);

        new DailyPriceBackfillJob(marketDataSyncService, schedulerSettingReader, notificationService).runKrxBackfill();

        verify(marketDataSyncService, never()).syncDailyPrices(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).sendTelegramOnce(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private PriceDailySyncResponse response(String market) {
        return new PriceDailySyncResponse(market, 3, 2, 1, 0, 5, 5, LocalDate.now().minusDays(1), "BOOTSTRAP_ALLOWED", List.of());
    }
}
