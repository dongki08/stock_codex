package com.parkdh.stockadvisor.infrastructure.persistence.audit; // 감사 로그 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.audit.AuditLogEntity; // 감사 로그 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> { // 감사 로그 저장소 인터페이스를 정의한다.
    List<AuditLogEntity> findTop50ByOrderByIdDesc(); // 최근 감사 로그 50건을 ID 역순으로 조회한다.
} // 감사 로그 저장소 인터페이스를 종료한다.
