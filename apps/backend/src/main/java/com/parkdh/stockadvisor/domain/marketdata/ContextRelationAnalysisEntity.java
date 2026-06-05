package com.parkdh.stockadvisor.domain.marketdata;

import com.parkdh.stockadvisor.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "context_relation_analysis")
public class ContextRelationAnalysisEntity extends BaseEntity {
    @Id
    @Column(name = "analysis_key", length = 80, nullable = false)
    private String analysisKey;
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;
    @Column(name = "market", length = 20, nullable = false)
    private String market;
    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;
    @Column(name = "direction", length = 12, nullable = false)
    private String direction;
    @Column(name = "confidence", nullable = false)
    private Integer confidence;
    @Column(name = "risk_level", length = 12, nullable = false)
    private String riskLevel;
    @Column(name = "relation_score", nullable = false)
    private Integer relationScore;
    @Column(name = "summary", length = 1000, nullable = false)
    private String summary;
    @Column(name = "key_factors_json", columnDefinition = "nvarchar(max)", nullable = false)
    private String keyFactorsJson;
    @Column(name = "contradictions_json", columnDefinition = "nvarchar(max)", nullable = false)
    private String contradictionsJson;
    @Column(name = "model", length = 50, nullable = false)
    private String model;
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    public ContextRelationAnalysisEntity(String ticker, String market, LocalDate analysisDate, String direction,
                                         Integer confidence, String riskLevel, Integer relationScore, String summary,
                                         String keyFactorsJson, String contradictionsJson, String model,
                                         LocalDateTime analyzedAt) {
        this.analysisKey = buildKey(market, ticker, analysisDate);
        this.ticker = ticker;
        this.market = market;
        this.analysisDate = analysisDate;
        update(direction, confidence, riskLevel, relationScore, summary, keyFactorsJson, contradictionsJson, model, analyzedAt);
    }

    public void update(String direction, Integer confidence, String riskLevel, Integer relationScore, String summary,
                       String keyFactorsJson, String contradictionsJson, String model, LocalDateTime analyzedAt) {
        this.direction = direction;
        this.confidence = confidence;
        this.riskLevel = riskLevel;
        this.relationScore = relationScore;
        this.summary = truncate(summary, 1000);
        this.keyFactorsJson = keyFactorsJson;
        this.contradictionsJson = contradictionsJson;
        this.model = model;
        this.analyzedAt = analyzedAt;
    }

    public static String buildKey(String market, String ticker, LocalDate analysisDate) {
        return market + ":" + ticker + ":" + analysisDate;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
