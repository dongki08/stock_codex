package com.parkdh.stockadvisor.infrastructure.persistence.brief; // 데일리 브리프 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.brief.DailyBriefEntity; // 데일리 브리프 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface DailyBriefRepository extends JpaRepository<DailyBriefEntity, Long> { // 데일리 브리프 저장소 인터페이스를 정의한다.
    List<DailyBriefEntity> findByMarketTrack(String marketTrack); // 시장 트랙으로 브리프 목록을 조회한다.
} // 데일리 브리프 저장소 인터페이스를 종료한다.
