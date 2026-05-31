package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FundamentalMetricRepository extends JpaRepository<FundamentalMetricEntity, String> {
    List<FundamentalMetricEntity> findByMarketAndTickerOrderByPeriodEndDesc(String market, String ticker, Pageable pageable);

    List<FundamentalMetricEntity> findByMarketOrderByPeriodEndDesc(String market, Pageable pageable);

    List<FundamentalMetricEntity> findByTickerOrderByPeriodEndDesc(String ticker, Pageable pageable);

    List<FundamentalMetricEntity> findAllByOrderByPeriodEndDesc(Pageable pageable);

    // PIT 스냅샷용: asOf 이하 결산 기간 펀더멘털만 조회 (미래참조 차단)
    List<FundamentalMetricEntity> findByMarketAndTickerAndPeriodEndLessThanEqualOrderByPeriodEndDesc(String market, String ticker, LocalDate asOf, Pageable pageable);
}
