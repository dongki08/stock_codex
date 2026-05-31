package com.parkdh.stockadvisor.application.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.feature.FeatureSnapshotEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.feature.FeatureSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureICServiceTest {
    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    private FeatureICService service;

    @BeforeEach
    void setUp() {
        service = new FeatureICService(featureSnapshotRepository, new ObjectMapper());
    }

    @Test
    void measureLatestCalculatesSpearmanIcFromFeatureSnapshots() {
        when(featureSnapshotRepository.findByFwdRet5dIsNotNullOrFwdRet20dIsNotNull()).thenReturn(List.of(
                snapshot("AAA", 10, 1),
                snapshot("BBB", 20, 2),
                snapshot("CCC", 30, 3)
        ));

        FeatureICService.FeatureICReport report = service.measureLatest();

        assertThat(report.hasSignal()).isTrue();
        assertThat(report.ics())
                .filteredOn(ic -> ic.weightPath().equals("technical.ma"))
                .singleElement()
                .satisfies(ic -> assertThat(ic.ic5d()).isEqualByComparingTo("1.000000"));
    }

    @Test
    void guideForIterationUsesHighestIcFeatureAndIncreasesItsWeight() {
        when(featureSnapshotRepository.findByFwdRet5dIsNotNullOrFwdRet20dIsNotNull()).thenReturn(List.of(
                snapshot("AAA", 10, 1),
                snapshot("BBB", 20, 2),
                snapshot("CCC", 30, 3)
        ));

        FeatureICService.MutationGuide guide = service.guideForIteration(1, List.of("value.liquidity"));

        assertThat(guide.icGuided()).isTrue();
        assertThat(guide.weightPath()).isEqualTo("technical.ma");
        assertThat(guide.factor()).isEqualByComparingTo("1.10");
        assertThat(guide.summary()).contains("IC-guided").contains("technical.ma").contains("icGuide");
    }

    private FeatureSnapshotEntity snapshot(String ticker, int movingAverageScore, int fwdRet5d) {
        String featureJson = """
                {"ticker":"%s","market":"NASDAQ","liquidityScore":50,"priceScore":50,"movingAverageScore":%d,"rsiScore":50,"volumeScore":50,"macdScore":50,"bollingerScore":50,"technicalScore":%d,"newsScore":50,"disclosureScore":50,"macroScore":50,"fundamentalScore":50,"contextScore":50,"dataQualityScore":50,"totalScore":%d}
                """.formatted(ticker, movingAverageScore, movingAverageScore, movingAverageScore);
        FeatureSnapshotEntity snapshot = new FeatureSnapshotEntity("NASDAQ", ticker, LocalDate.of(2026, 1, 1), movingAverageScore, featureJson);
        snapshot.updateForwardReturns(BigDecimal.valueOf(fwdRet5d), null);
        return snapshot;
    }
}
