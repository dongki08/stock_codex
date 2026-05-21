package com.parkdh.stockadvisor.infrastructure.persistence.prediction; // 예측 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.prediction.PredictionEntity; // 예측 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface PredictionRepository extends JpaRepository<PredictionEntity, Long> { // 예측 저장소 인터페이스를 정의한다.
    List<PredictionEntity> findByTicker(String ticker); // 종목 코드로 예측 목록을 조회한다.
} // 예측 저장소 인터페이스를 종료한다.
