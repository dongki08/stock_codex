package com.parkdh.stockadvisor.infrastructure.persistence.autoresearch; // 전략 버전 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.autoresearch.StrategyVersionEntity; // 전략 버전 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface StrategyVersionRepository extends JpaRepository<StrategyVersionEntity, Long> { // 전략 버전 저장소 인터페이스를 정의한다.
    List<StrategyVersionEntity> findByChampion(Boolean champion); // 챔피언 여부로 전략 버전 목록을 조회한다.
} // 전략 버전 저장소 인터페이스를 종료한다.
