package com.parkdh.stockadvisor.infrastructure.persistence.price; // 가격 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.price.PriceIntradayEntity; // 장중 가격 엔티티를 가져온다.
import org.springframework.data.domain.Pageable; // 페이징 요청 타입을 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface PriceIntradayRepository extends JpaRepository<PriceIntradayEntity, String> { // 장중 가격 저장소 인터페이스를 정의한다.
    List<PriceIntradayEntity> findByMarketOrderByTickAtDesc(String market, Pageable pageable); // 시장별 장중 가격을 최근 시각 순으로 조회한다.

    List<PriceIntradayEntity> findByTickerOrderByTickAtDesc(String ticker, Pageable pageable); // 종목별 장중 가격을 최근 시각 순으로 조회한다.

    List<PriceIntradayEntity> findByMarketAndTickerOrderByTickAtDesc(String market, String ticker, Pageable pageable); // 시장과 종목별 장중 가격을 최근 시각 순으로 조회한다.

    List<PriceIntradayEntity> findAllByOrderByTickAtDesc(Pageable pageable); // 전체 장중 가격을 최근 시각 순으로 조회한다.
} // 장중 가격 저장소 인터페이스를 종료한다.
