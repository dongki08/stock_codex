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

    // PIT 스냅샷용: asOf 이하 발행 뉴스만 조회 (미래참조 차단)
    List<NewsArticleEntity> findByMarketAndTickerAndPublishedAtLessThanEqualOrderByPublishedAtDesc(String market, String ticker, LocalDateTime asOf, Pageable pageable);
}
