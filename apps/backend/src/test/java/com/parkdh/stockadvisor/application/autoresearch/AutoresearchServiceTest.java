package com.parkdh.stockadvisor.application.autoresearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchAutoRunRequest;
import com.parkdh.stockadvisor.application.backtest.BacktestRunService;
import com.parkdh.stockadvisor.domain.autoresearch.AutoresearchRunEntity;
import com.parkdh.stockadvisor.domain.autoresearch.StrategyVersionEntity;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.AutoresearchRunRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.StrategyVersionRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.audit.AuditLogRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoresearchServiceTest {
    @Mock
    private AutoresearchRunRepository autoresearchRunRepository;
    @Mock
    private StrategyVersionRepository strategyVersionRepository;
    @Mock
    private AppSettingRepository appSettingRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private BacktestRunService backtestRunService;

    private AutoresearchService service;

    @BeforeEach
    void setUp() {
        service = new AutoresearchService(
                autoresearchRunRepository,
                strategyVersionRepository,
                appSettingRepository,
                auditLogRepository,
                recommendationRepository,
                evaluationRepository,
                backtestRunService,
                new ObjectMapper()
        );
    }

    @Test
    void runAutoResearchPromotesImprovedWeightsAsChampion() {
        AppSettingEntity weights = new AppSettingEntity(
                "recommendation.scoring.weights",
                "{\"value\":{\"liquidity\":0.20,\"price\":0.10,\"technical\":0.30,\"context\":0.15,\"fundamental\":0.10,\"dataQuality\":0.15},\"technical\":{\"ma\":0.40,\"rsi\":0.35,\"volume\":0.25},\"context\":{\"news\":0.40,\"disclosure\":0.18,\"macro\":0.25,\"fundamental\":0.17}}",
                "weights",
                "test"
        );
        when(appSettingRepository.findById(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return "recommendation.scoring.weights".equals(key) ? Optional.of(weights) : Optional.empty();
        });
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of());
        when(backtestRunService.evaluateRecommendationEngine(any())).thenReturn(new BacktestRunService.BacktestEvaluation(
                "recommendation-engine-v1",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "{\"avgPnlPct\":1.25}",
                BigDecimal.valueOf(1.25)
        ));
        when(autoresearchRunRepository.save(any(AutoresearchRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(strategyVersionRepository.save(any(StrategyVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var runs = service.runAutoResearch(new AutoresearchAutoRunRequest(
                "NASDAQ",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                1,
                1,
                20,
                null,
                null
        ));

        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).decision()).isEqualTo("KEEP");
        ArgumentCaptor<StrategyVersionEntity> strategyCaptor = ArgumentCaptor.forClass(StrategyVersionEntity.class);
        verify(strategyVersionRepository).save(strategyCaptor.capture());
        assertThat(strategyCaptor.getValue().getChampion()).isTrue();
        assertThat(strategyCaptor.getValue().getMetricValue()).isEqualByComparingTo("1.25");
    }

    @Test
    void runAutoResearchStoresStrategyYamlSnapshotsForProposalAndChampion() {
        AppSettingEntity weights = new AppSettingEntity(
                "recommendation.scoring.weights",
                "{\"value\":{\"liquidity\":0.20,\"price\":0.10,\"technical\":0.30,\"context\":0.15,\"fundamental\":0.10,\"dataQuality\":0.15},\"technical\":{\"ma\":0.30,\"rsi\":0.25,\"volume\":0.20,\"macd\":0.15,\"bollinger\":0.10},\"context\":{\"news\":0.40,\"disclosure\":0.18,\"macro\":0.25,\"fundamental\":0.17}}",
                "weights",
                "test"
        );
        when(appSettingRepository.findById(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return "recommendation.scoring.weights".equals(key) ? Optional.of(weights) : Optional.empty();
        });
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of());
        when(backtestRunService.evaluateRecommendationEngine(any())).thenReturn(new BacktestRunService.BacktestEvaluation(
                "recommendation-engine-v1",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "{\"avgPnlPct\":1.25,\"tradeCount\":5}",
                BigDecimal.valueOf(1.25)
        ));
        when(autoresearchRunRepository.save(any(AutoresearchRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(strategyVersionRepository.save(any(StrategyVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.runAutoResearch(new AutoresearchAutoRunRequest(
                "NASDAQ",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                1,
                10,
                20,
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(2)
        ));

        ArgumentCaptor<AppSettingEntity> settingCaptor = ArgumentCaptor.forClass(AppSettingEntity.class);
        verify(appSettingRepository, atLeastOnce()).save(settingCaptor.capture());
        assertThat(settingCaptor.getAllValues())
                .filteredOn(setting -> setting.getSettingKey().startsWith("autoresearch.strategyYaml."))
                .extracting(AppSettingEntity::getValueJson)
                .anySatisfy(valueJson -> assertThat(valueJson)
                        .contains("strategy: recommendation-engine-v1")
                        .contains("market: NASDAQ")
                        .contains("avgPnlPct")
                        .contains("weights:"));
    }

    @Test
    void validationRollsBackChampionWhenLiveMetricDropsBelowHalf() throws Exception {
        StrategyVersionEntity oldStrategy = strategy("ar-old", BigDecimal.valueOf(2.0), LocalDateTime.now().minusDays(20), false, 1L);
        StrategyVersionEntity champion = strategy("ar-new", BigDecimal.valueOf(4.0), LocalDateTime.now().minusDays(8), true, 2L);
        RecommendationEntity recommendation = recommendation("AAPL", "NASDAQ", "ar-new", 10L, LocalDateTime.now().minusDays(7));
        when(strategyVersionRepository.findByChampion(true)).thenReturn(List.of(champion));
        when(strategyVersionRepository.findAll()).thenReturn(List.of(oldStrategy, champion));
        when(appSettingRepository.findById("autoresearch.rollbackValidationDays"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.rollbackValidationDays", "{\"value\":7}", "rollback days", "test")));
        when(appSettingRepository.findById("autoresearch.weights.ar-old"))
                .thenReturn(Optional.of(new AppSettingEntity("autoresearch.weights.ar-old", "{\"value\":{\"technical\":1.0}}", "snapshot", "test")));
        when(appSettingRepository.findById("recommendation.scoring.weights"))
                .thenReturn(Optional.of(new AppSettingEntity("recommendation.scoring.weights", "{\"value\":{\"technical\":1.0}}", "weights", "test")));
        when(recommendationRepository.findByModelVersionAndGeneratedAtBetween(anyString(), any(), any()))
                .thenReturn(List.of(recommendation));
        when(evaluationRepository.findByRecommendationIdIn(List.of(10L)))
                .thenReturn(List.of(new EvaluationEntity(10L, BigDecimal.valueOf(90), "STOP_HIT", BigDecimal.valueOf(1.0), BigDecimal.valueOf(-2), false, LocalDateTime.now())));

        Method method = AutoresearchService.class.getDeclaredMethod("validateChampionRollback");
        method.setAccessible(true);
        method.invoke(service);

        assertThat(champion.getChampion()).isFalse();
        assertThat(oldStrategy.getChampion()).isTrue();
        verify(strategyVersionRepository).saveAll(List.of(champion, oldStrategy));
    }

    private StrategyVersionEntity strategy(String semver, BigDecimal metric, LocalDateTime promotedAt, boolean champion, Long id) throws Exception {
        StrategyVersionEntity entity = new StrategyVersionEntity(semver, "sha-" + semver, metric, promotedAt, champion);
        Field idField = StrategyVersionEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
        return entity;
    }

    private RecommendationEntity recommendation(String ticker, String market, String modelVersion, Long id, LocalDateTime generatedAt) throws Exception {
        RecommendationEntity entity = new RecommendationEntity(
                ticker,
                market,
                "SHORT",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(95),
                LocalDate.now().plusDays(5),
                70,
                "{}",
                modelVersion,
                generatedAt,
                "CLOSED"
        );
        Field idField = RecommendationEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
        return entity;
    }
}
