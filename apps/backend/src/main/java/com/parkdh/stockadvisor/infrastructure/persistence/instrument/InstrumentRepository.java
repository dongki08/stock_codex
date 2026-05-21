package com.parkdh.stockadvisor.infrastructure.persistence.instrument; // 종목 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.instrument.InstrumentEntity; // 종목 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, String> { // 종목 저장소 인터페이스를 정의한다.
    List<InstrumentEntity> findByMarket(String market); // 시장 구분으로 종목 목록을 조회한다.

    List<InstrumentEntity> findByEnabled(Boolean enabled); // 활성 여부로 종목 목록을 조회한다.

    List<InstrumentEntity> findByMarketAndEnabled(String market, Boolean enabled); // 시장 구분과 활성 여부로 종목 목록을 조회한다.
} // 종목 저장소 인터페이스를 종료한다.
