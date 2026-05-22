package com.parkdh.stockadvisor.infrastructure.persistence.universe; // 시장 유니버스 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface MarketUniverseRepository extends JpaRepository<MarketUniverseEntity, String> { // 시장 유니버스 저장소 인터페이스를 정의한다.
    List<MarketUniverseEntity> findByMarket(String market); // 시장 구분으로 후보군을 조회한다.

    List<MarketUniverseEntity> findByTradable(Boolean tradable); // 거래 가능 여부로 후보군을 조회한다.

    List<MarketUniverseEntity> findByMarketAndTradable(String market, Boolean tradable); // 시장 구분과 거래 가능 여부로 후보군을 조회한다.

    List<MarketUniverseEntity> findByTradableAndDelistedAtIsNull(Boolean tradable); // 현재 상장된 거래 가능 후보군을 조회한다.

    List<MarketUniverseEntity> findByMarketAndTradableAndDelistedAtIsNull(String market, Boolean tradable); // 시장별 현재 상장된 거래 가능 후보군을 조회한다.
} // 시장 유니버스 저장소 인터페이스를 종료한다.
