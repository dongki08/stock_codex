package com.parkdh.stockadvisor.infrastructure.persistence.backtest; // 백테스트 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.backtest.BacktestRunEntity; // 백테스트 실행 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

public interface BacktestRunRepository extends JpaRepository<BacktestRunEntity, Long> { // 백테스트 실행 저장소 인터페이스를 정의한다.
} // 백테스트 실행 저장소 인터페이스를 종료한다.
