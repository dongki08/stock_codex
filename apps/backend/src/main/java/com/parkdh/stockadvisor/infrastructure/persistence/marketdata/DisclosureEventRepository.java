package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DisclosureEventRepository extends JpaRepository<DisclosureEventEntity, String> {
    List<DisclosureEventEntity> findByMarketAndTickerOrderByDisclosedAtDesc(String market, String ticker, Pageable pageable);

    List<DisclosureEventEntity> findByMarketOrderByDisclosedAtDesc(String market, Pageable pageable);

    List<DisclosureEventEntity> findByTickerOrderByDisclosedAtDesc(String ticker, Pageable pageable);

    List<DisclosureEventEntity> findAllByOrderByDisclosedAtDesc(Pageable pageable);

    List<DisclosureEventEntity> findByMarketAndTickerAndDisclosedAtBetweenOrderByDisclosedAtDesc(String market, String ticker, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // PIT 스냅샷용: asOf 이하 공시만 조회 (미래참조 차단)
    List<DisclosureEventEntity> findByMarketAndTickerAndDisclosedAtLessThanEqualOrderByDisclosedAtDesc(String market, String ticker, LocalDateTime asOf, Pageable pageable);
}
