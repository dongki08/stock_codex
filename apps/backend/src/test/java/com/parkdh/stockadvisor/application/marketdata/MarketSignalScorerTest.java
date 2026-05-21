package com.parkdh.stockadvisor.application.marketdata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSignalScorerTest {
    private final MarketSignalScorer scorer = new MarketSignalScorer();

    @Test
    void scoreNewsSentimentReturnsPositiveForStrongPositiveTitle() {
        assertThat(scorer.scoreNewsSentiment("AAPL shares surge after record profit beat", null)).isPositive();
    }

    @Test
    void scoreNewsSentimentReturnsNegativeForRiskTitle() {
        assertThat(scorer.scoreNewsSentiment("Company shares plunge after lawsuit warning", null)).isNegative();
    }

    @Test
    void scoreDisclosureImportanceMarksMaterialEventsHigh() {
        assertThat(scorer.scoreDisclosureImportance("주요사항보고서 유상증자 결정", null)).isGreaterThanOrEqualTo(85);
    }

    @Test
    void classifyDisclosureTypeDetectsEarnings() {
        assertThat(scorer.classifyDisclosureType("잠정 실적 공시", null)).isEqualTo("EARNINGS");
    }
}
