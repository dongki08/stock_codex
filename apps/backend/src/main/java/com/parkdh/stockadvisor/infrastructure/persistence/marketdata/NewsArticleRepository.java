package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, String> {
    List<NewsArticleEntity> findByMarketAndTickerOrderByPublishedAtDesc(String market, String ticker, Pageable pageable);

    List<NewsArticleEntity> findByMarketOrderByPublishedAtDesc(String market, Pageable pageable);

    List<NewsArticleEntity> findByTickerOrderByPublishedAtDesc(String ticker, Pageable pageable);

    List<NewsArticleEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);

    List<NewsArticleEntity> findByMarketAndTickerAndPublishedAtBetweenOrderByPublishedAtDesc(String market, String ticker, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<NewsArticleEntity> findByMarketAndPublishedAtBetweenOrderByPublishedAtDesc(String market, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<NewsArticleEntity> findByTickerAndPublishedAtBetweenOrderByPublishedAtDesc(String ticker, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<NewsArticleEntity> findByPublishedAtBetweenOrderByPublishedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
