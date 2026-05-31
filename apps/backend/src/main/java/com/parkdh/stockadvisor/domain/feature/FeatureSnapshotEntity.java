package com.parkdh.stockadvisor.domain.feature;

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
@Table(name = "feature_snapshot")
public class FeatureSnapshotEntity {
    @Id
    @Comment("스냅샷 키 ({market}:{ticker}:{yyyyMMdd})")
    @Column(name = "snapshot_key", length = 80, nullable = false)
    private String snapshotKey;

    @Comment("시장 구분")
    @Column(name = "market", length = 20, nullable = false)
    private String market;

    @Comment("종목 코드")
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Comment("기준일 (그날 장마감 기준 PIT)")
    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Comment("종합 점수 (0-100)")
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Comment("피처 JSON (UniverseFeatureBuilder.buildFeatureJson 결과)")
    @Column(name = "feature_json", columnDefinition = "nvarchar(max)", nullable = false)
    private String featureJson;

    @Comment("진입+5거래일 수익률(%) — 사후 백필")
    @Column(name = "fwd_ret_5d", precision = 12, scale = 6)
    private BigDecimal fwdRet5d;

    @Comment("진입+20거래일 수익률(%) — 사후 백필")
    @Column(name = "fwd_ret_20d", precision = 12, scale = 6)
    private BigDecimal fwdRet20d;

    @Comment("생성 일시")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FeatureSnapshotEntity(String market, String ticker, LocalDate asOfDate, int totalScore, String featureJson) {
        this.snapshotKey = buildKey(market, ticker, asOfDate);
        this.market = market;
        this.ticker = ticker;
        this.asOfDate = asOfDate;
        this.totalScore = totalScore;
        this.featureJson = featureJson;
        this.createdAt = LocalDateTime.now();
    }

    public void updateFeature(int totalScore, String featureJson) {
        this.totalScore = totalScore;
        this.featureJson = featureJson;
    }

    public void updateForwardReturns(BigDecimal fwdRet5d, BigDecimal fwdRet20d) {
        this.fwdRet5d = fwdRet5d;
        this.fwdRet20d = fwdRet20d;
    }

    public static String buildKey(String market, String ticker, LocalDate asOfDate) {
        return market + ":" + ticker + ":" + asOfDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }
}
