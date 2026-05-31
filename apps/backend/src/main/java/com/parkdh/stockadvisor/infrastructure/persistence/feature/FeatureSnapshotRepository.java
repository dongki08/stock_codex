package com.parkdh.stockadvisor.infrastructure.persistence.feature;

import com.parkdh.stockadvisor.domain.feature.FeatureSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeatureSnapshotRepository extends JpaRepository<FeatureSnapshotEntity, String> {
    List<FeatureSnapshotEntity> findByMarketAndTickerOrderByAsOfDateDesc(String market, String ticker);

    List<FeatureSnapshotEntity> findByAsOfDate(LocalDate asOfDate);

    List<FeatureSnapshotEntity> findByFwdRet5dIsNotNullOrFwdRet20dIsNotNull();

    // forward return 백필 대상: fwdRet5d가 null이고 기준일이 cutoff 이전인 스냅샷
    List<FeatureSnapshotEntity> findByFwdRet5dIsNullAndAsOfDateLessThanEqual(LocalDate cutoff);
}
