package com.parkdh.stockadvisor.infrastructure.persistence.recommendation; // 추천 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, Long> { // 추천 저장소 인터페이스를 정의한다.
    List<RecommendationEntity> findByStatus(String status); // 추천 상태로 추천 목록을 조회한다.

    List<RecommendationEntity> findByTicker(String ticker); // 종목 코드로 추천 목록을 조회한다.

    List<RecommendationEntity> findByModelVersionAndGeneratedAtBetween(String modelVersion, LocalDateTime from, LocalDateTime to); // 모델 버전과 생성 기간으로 추천을 조회한다.
} // 추천 저장소 인터페이스를 종료한다.
