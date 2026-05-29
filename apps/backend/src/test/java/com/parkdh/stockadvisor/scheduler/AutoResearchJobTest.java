package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchAutoRunRequest;
import com.parkdh.stockadvisor.application.autoresearch.AutoresearchService;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoResearchJobTest {
    @Mock
    private AutoresearchService autoresearchService;
    @Mock
    private AppSettingRepository appSettingRepository;

    @Test
    void runUsesConfiguredBacktestParameters() {
        when(appSettingRepository.findById("autoresearch.enabled"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.enabled", "{\"value\":true}", "enabled", "test")));
        when(appSettingRepository.findById("autoresearch.targetIterations"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.targetIterations", "{\"value\":3}", "iterations", "test")));
        when(appSettingRepository.findById("autoresearch.maxTickers"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.maxTickers", "{\"value\":12}", "tickers", "test")));
        when(appSettingRepository.findById("autoresearch.holdingDays"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.holdingDays", "{\"value\":15}", "holding", "test")));
        when(appSettingRepository.findById("autoresearch.targetPct"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.targetPct", "{\"value\":4.5}", "target", "test")));
        when(appSettingRepository.findById("autoresearch.stopPct"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.stopPct", "{\"value\":2.5}", "stop", "test")));
        when(autoresearchService.runAutoResearch(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AutoResearchJob job = new AutoResearchJob(autoresearchService, appSettingRepository, new ObjectMapper());

        job.run();

        ArgumentCaptor<AutoresearchAutoRunRequest> requestCaptor = ArgumentCaptor.forClass(AutoresearchAutoRunRequest.class);
        verify(autoresearchService).runAutoResearch(requestCaptor.capture());
        AutoresearchAutoRunRequest request = requestCaptor.getValue();
        assertThat(request.iterations()).isEqualTo(3);
        assertThat(request.maxTickers()).isEqualTo(12);
        assertThat(request.holdingDays()).isEqualTo(15);
        assertThat(request.targetPct()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(request.stopPct()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
    }
}
