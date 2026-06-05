package com.parkdh.stockadvisor.application.brief;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse;
import com.parkdh.stockadvisor.application.stats.StatsService;
import com.parkdh.stockadvisor.config.CodexCliProperties;
import com.parkdh.stockadvisor.domain.brief.DailyBriefEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient;
import com.parkdh.stockadvisor.infrastructure.persistence.brief.DailyBriefRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyBriefServiceTest {
    @Mock
    private DailyBriefRepository dailyBriefRepository;
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private MarketUniverseRepository marketUniverseRepository;
    @Mock
    private PriceDailyRepository priceDailyRepository;
    @Mock
    private NewsArticleRepository newsArticleRepository;
    @Mock
    private DisclosureEventRepository disclosureEventRepository;
    @Mock
    private MacroObservationRepository macroObservationRepository;
    @Mock
    private FundamentalMetricRepository fundamentalMetricRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private CodexClient codexClient;

    private DailyBriefService service;

    @BeforeEach
    void setUp() {
        service = new DailyBriefService(
                dailyBriefRepository,
                recommendationRepository,
                marketUniverseRepository,
                priceDailyRepository,
                newsArticleRepository,
                disclosureEventRepository,
                macroObservationRepository,
                fundamentalMetricRepository,
                appSettingRepository,
                statsService,
                codexClient,
                new CodexCliProperties("codex"),
                new ObjectMapper()
        );
    }

    @Test
    void generateDailyBriefCapsPromptAndIncludesOperationalContract() {
        when(recommendationRepository.findByStatus("OPEN")).thenReturn(List.of());
        when(marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(any(), eq(true))).thenReturn(List.of());
        when(priceDailyRepository.findByMarketOrderByTradeDateDesc(any(), any(Pageable.class))).thenReturn(List.of());
        when(newsArticleRepository.findByMarketAndPublishedAtBetweenOrderByPublishedAtDesc(any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(disclosureEventRepository.findByMarketOrderByDisclosedAtDesc(any(), any(Pageable.class))).thenReturn(List.of());
        when(macroObservationRepository.findAllByOrderByObservedDateDesc(any(Pageable.class))).thenReturn(List.of());
        when(fundamentalMetricRepository.findByMarketOrderByPeriodEndDesc(any(), any(Pageable.class))).thenReturn(List.of());
        when(statsService.getSummary()).thenReturn(new StatsSummaryResponse(
                0,
                0,
                0,
                0,
                0.0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Map.of(),
                Map.of()
        ));
        when(appSettingRepository.findById("codex.profile")).thenReturn(Optional.of(new AppSettingEntity("codex.profile", "{\"value\":\"stock-advisor\"}", "profile", "test")));
        when(appSettingRepository.findById("dailybrief.prompt.maxChars")).thenReturn(Optional.of(new AppSettingEntity("dailybrief.prompt.maxChars", "{\"value\":1200}", "max chars", "test")));
        when(codexClient.call(any(String.class), eq("stock-advisor"), eq("daily-brief")))
                .thenReturn(new CodexClient.CodexResult(true, "# brief", null, 10));
        when(dailyBriefRepository.save(any(DailyBriefEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generateDailyBrief("KRX", "추가요청 ".repeat(500));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(codexClient).call(promptCaptor.capture(), eq("stock-advisor"), eq("daily-brief"));
        String prompt = promptCaptor.getValue();
        assertThat(prompt.length()).isLessThanOrEqualTo(1200);
        assertThat(prompt).contains("OPERATING_BRIEF_CONTRACT");
        assertThat(prompt).contains("데이터 없음");
        assertThat(prompt).contains("TRUNCATED_TO_MAX_CHARS");

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        verify(newsArticleRepository).findByMarketAndPublishedAtBetweenOrderByPublishedAtDesc(
                eq("KOSPI"),
                eq(today.atStartOfDay()),
                eq(today.atTime(LocalTime.MAX)),
                any(Pageable.class)
        );
    }
}
