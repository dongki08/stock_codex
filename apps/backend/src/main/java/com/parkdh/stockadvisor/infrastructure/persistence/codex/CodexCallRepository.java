package com.parkdh.stockadvisor.infrastructure.persistence.codex; // Codex 호출 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.codex.CodexCallEntity; // Codex 호출 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface CodexCallRepository extends JpaRepository<CodexCallEntity, Long> { // Codex 호출 저장소 인터페이스를 정의한다.
    List<CodexCallEntity> findByCaller(String caller); // 호출자로 Codex 호출 목록을 조회한다.
} // Codex 호출 저장소 인터페이스를 종료한다.
