package com.parkdh.stockadvisor.infrastructure.persistence.codex; // Codex 호출 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.codex.CodexCallEntity; // Codex 호출 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.
import org.springframework.data.jpa.repository.Query; // JPQL 쿼리 어노테이션을 가져온다.
import org.springframework.data.repository.query.Param; // 쿼리 파라미터 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

public interface CodexCallRepository extends JpaRepository<CodexCallEntity, Long> { // Codex 호출 저장소 인터페이스를 정의한다.
    List<CodexCallEntity> findByCaller(String caller); // 호출자로 Codex 호출 목록을 조회한다.

    long countByCalledAtBetween(LocalDateTime from, LocalDateTime to); // 기간 내 Codex 호출 수를 조회한다.

    @Query("select coalesce(sum(c.promptLen + coalesce(c.responseLen, 0)), 0) from CodexCallEntity c where c.succeeded = true and c.calledAt between :from and :to")
    long sumSucceededTextLengthByCalledAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to); // 기간 내 성공한 Codex 입출력 문자 수 합계를 조회한다.
} // Codex 호출 저장소 인터페이스를 종료한다.
