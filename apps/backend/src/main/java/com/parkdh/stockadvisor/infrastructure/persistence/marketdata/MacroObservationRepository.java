package com.parkdh.stockadvisor.infrastructure.persistence.marketdata;

import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MacroObservationRepository extends JpaRepository<MacroObservationEntity, String> {
    List<MacroObservationEntity> findBySeriesIdOrderByObservedDateDesc(String seriesId, Pageable pageable);

    List<MacroObservationEntity> findAllByOrderByObservedDateDesc(Pageable pageable);
}
