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
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "news_article")
public class NewsArticleEntity extends CreatedEntity {
    @Id
    @Comment("News article key")
    @Column(name = "article_key", length = 80, nullable = false)
    private String articleKey;

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
    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    @Comment("Source")
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Comment("Published at")
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Comment("Summary")
    @Column(name = "summary", columnDefinition = "nvarchar(max)")
    private String summary;

    @Comment("Sentiment score")
    @Column(name = "sentiment_score", precision = 6, scale = 3)
    private BigDecimal sentimentScore;

    public NewsArticleEntity(String articleKey, String ticker, String market, String title, String url, String source,
                             LocalDateTime publishedAt, String summary, BigDecimal sentimentScore) {
        this.articleKey = articleKey;
        this.ticker = ticker;
        this.market = market;
        this.title = title;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.sentimentScore = sentimentScore;
    }

    public void update(String title, LocalDateTime publishedAt, String summary, BigDecimal sentimentScore) {
        this.title = title;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.sentimentScore = sentimentScore;
    }
}
