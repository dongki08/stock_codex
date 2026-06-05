package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.marketdata.dto.MarketDataCollectionSyncResponse;
import com.parkdh.stockadvisor.application.marketdata.ContextRelationAnalysisService;
import com.parkdh.stockadvisor.application.marketdata.MarketDataCollectionService;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataCollectionJobTest {
    @Mock
    private MarketDataCollectionService marketDataCollectionService;
    @Mock
    private MarketDataSyncService marketDataSyncService;
    @Mock
    private ContextRelationAnalysisService contextRelationAnalysisService;
    @Mock
    private MarketUniverseRepository marketUniverseRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private MarketDataCollectionJob job;

    @Test
    void krxCollectionAnalyzesEachMarketAfterDataCollection() {
        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(any(), eq(true))).thenReturn(List.of());
        when(marketDataCollectionService.syncDisclosureEvents(any(), eq(null), any()))
                .thenReturn(new MarketDataCollectionSyncResponse("TEST", null, null, 0, 0, List.of()));

        job.runKrxPreOpenCollection();

        verify(contextRelationAnalysisService).analyzeMarket("KOSPI", List.of());
        verify(contextRelationAnalysisService).analyzeMarket("KOSDAQ", List.of());
    }
}
