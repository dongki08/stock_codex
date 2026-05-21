package com.parkdh.stockadvisor.infrastructure.persistence.recommendation;

import com.parkdh.stockadvisor.domain.recommendation.ExitConfirmLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ExitConfirmLogRepository extends JpaRepository<ExitConfirmLogEntity, Long> {
    long countByMarketAndTickerAndConfirmedAtBetween(String market, String ticker, LocalDateTime from, LocalDateTime to);

    boolean existsByRecommendationIdAndConfirmedAtAfter(Long recommendationId, LocalDateTime confirmedAfter);

    boolean existsByNotifyKey(String notifyKey);
}
