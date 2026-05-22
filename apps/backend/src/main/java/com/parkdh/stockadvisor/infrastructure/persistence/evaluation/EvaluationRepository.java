package com.parkdh.stockadvisor.infrastructure.persistence.evaluation; // 평가 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity; // 평가 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> { // 평가 저장소 인터페이스를 정의한다.
    List<EvaluationEntity> findByRecommendationId(Long recommendationId); // 추천 ID로 평가 목록을 조회한다.

    List<EvaluationEntity> findByRecommendationIdIn(List<Long> recommendationIds); // 추천 ID 목록으로 평가 목록을 조회한다.

    boolean existsByRecommendationId(Long recommendationId); // 추천 ID로 평가 존재 여부를 조회한다.
} // 평가 저장소 인터페이스를 종료한다.
