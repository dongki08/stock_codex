package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MacroObservationRepository extends JpaRepository<MacroObservationEntity, String> {
    List<MacroObservationEntity> findBySeriesIdOrderByObservedDateDesc(String seriesId, Pageable pageable);

    List<MacroObservationEntity> findAllByOrderByObservedDateDesc(Pageable pageable);

    // PIT 스냅샷용: asOf 이하 매크로 관측값만 조회 (미래참조 차단)
    List<MacroObservationEntity> findByObservedDateLessThanEqualOrderByObservedDateDesc(LocalDate asOf, Pageable pageable);
}
