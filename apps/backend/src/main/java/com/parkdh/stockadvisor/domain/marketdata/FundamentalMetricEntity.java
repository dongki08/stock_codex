package com.parkdh.stockadvisor.domain.marketdata;

import com.parkdh.stockadvisor.domain.common.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fundamental_metric")
public class FundamentalMetricEntity extends CreatedEntity {
    @Id
    @Comment("Fundamental metric key")
    @Column(name = "metric_key", length = 120, nullable = false)
    private String metricKey;

    @Comment("Ticker")
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Comment("Market")
    @Column(name = "market", length = 20, nullable = false)
    private String market;

    @Comment("Metric name")
    @Column(name = "metric_name", length = 80, nullable = false)
    private String metricName;

    @Comment("Metric value")
    @Column(name = "metric_value", precision = 24, scale = 6)
    private BigDecimal metricValue;

    @Comment("Unit")
    @Column(name = "unit", length = 30)
    private String unit;

    @Comment("Fiscal year")
    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Comment("Fiscal period")
    @Column(name = "fiscal_period", length = 10)
    private String fiscalPeriod;

    @Comment("Period end")
    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Comment("Source")
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Comment("Fetched at")
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    public FundamentalMetricEntity(String metricKey, String ticker, String market, String metricName, BigDecimal metricValue,
                                   String unit, Integer fiscalYear, String fiscalPeriod, LocalDate periodEnd, String source,
                                   LocalDateTime fetchedAt) {
        this.metricKey = metricKey;
        this.ticker = ticker;
        this.market = market;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
        this.periodEnd = periodEnd;
        this.source = source;
        this.fetchedAt = fetchedAt;
    }

    public void update(BigDecimal metricValue, String unit, Integer fiscalYear, String fiscalPeriod, LocalDate periodEnd, LocalDateTime fetchedAt) {
        this.metricValue = metricValue;
        this.unit = unit;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
        this.periodEnd = periodEnd;
        this.fetchedAt = fetchedAt;
    }
}
