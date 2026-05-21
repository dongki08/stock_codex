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

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "disclosure_event")
public class DisclosureEventEntity extends CreatedEntity {
    @Id
    @Comment("Disclosure key")
    @Column(name = "disclosure_key", length = 100, nullable = false)
    private String disclosureKey;

    @Comment("Ticker")
    @Column(name = "ticker", length = 20)
    private String ticker;

    @Comment("Market")
    @Column(name = "market", length = 20)
    private String market;

    @Comment("Title")
    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Comment("URL")
    @Column(name = "url", length = 1000)
    private String url;

    @Comment("Source")
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Comment("Disclosure type")
    @Column(name = "disclosure_type", length = 60)
    private String disclosureType;

    @Comment("Importance score")
    @Column(name = "importance_score")
    private Integer importanceScore;

    @Comment("Disclosed at")
    @Column(name = "disclosed_at")
    private LocalDateTime disclosedAt;

    @Comment("Raw payload JSON")
    @Column(name = "raw_json", columnDefinition = "nvarchar(max)")
    private String rawJson;

    public DisclosureEventEntity(String disclosureKey, String ticker, String market, String title, String url, String source,
                                 String disclosureType, Integer importanceScore, LocalDateTime disclosedAt, String rawJson) {
        this.disclosureKey = disclosureKey;
        this.ticker = ticker;
        this.market = market;
        this.title = title;
        this.url = url;
        this.source = source;
        this.disclosureType = disclosureType;
        this.importanceScore = importanceScore;
        this.disclosedAt = disclosedAt;
        this.rawJson = rawJson;
    }

    public void update(String title, String url, String disclosureType, Integer importanceScore, LocalDateTime disclosedAt, String rawJson) {
        this.title = title;
        this.url = url;
        this.disclosureType = disclosureType;
        this.importanceScore = importanceScore;
        this.disclosedAt = disclosedAt;
        this.rawJson = rawJson;
    }
}
