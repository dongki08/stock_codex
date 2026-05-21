package com.parkdh.stockadvisor.infrastructure.persistence.autoresearch; // AutoResearch 실행 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.autoresearch.AutoresearchRunEntity; // AutoResearch 실행 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.
import java.util.UUID; // UUID 타입을 가져온다.

public interface AutoresearchRunRepository extends JpaRepository<AutoresearchRunEntity, Long> { // AutoResearch 실행 저장소 인터페이스를 정의한다.
    List<AutoresearchRunEntity> findByJobRunId(UUID jobRunId); // 작업 실행 UUID로 실험 목록을 조회한다.
} // AutoResearch 실행 저장소 인터페이스를 종료한다.
