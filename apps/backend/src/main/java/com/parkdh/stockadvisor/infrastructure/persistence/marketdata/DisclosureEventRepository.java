package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisclosureEventRepository extends JpaRepository<DisclosureEventEntity, String> {
    List<DisclosureEventEntity> findByMarketAndTickerOrderByDisclosedAtDesc(String market, String ticker, Pageable pageable);

    List<DisclosureEventEntity> findByMarketOrderByDisclosedAtDesc(String market, Pageable pageable);

    List<DisclosureEventEntity> findByTickerOrderByDisclosedAtDesc(String ticker, Pageable pageable);

    List<DisclosureEventEntity> findAllByOrderByDisclosedAtDesc(Pageable pageable);
}
