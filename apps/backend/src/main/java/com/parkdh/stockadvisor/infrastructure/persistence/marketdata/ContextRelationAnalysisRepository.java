package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.ContextRelationAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ContextRelationAnalysisRepository extends JpaRepository<ContextRelationAnalysisEntity, String> {
    Optional<ContextRelationAnalysisEntity> findByMarketAndTickerAndAnalysisDate(String market, String ticker, LocalDate analysisDate);
}
