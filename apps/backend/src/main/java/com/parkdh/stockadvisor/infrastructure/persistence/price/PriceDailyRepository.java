package com.parkdh.stockadvisor.infrastructure.persistence.price; // 가격 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import org.springframework.data.domain.Pageable; // 페이징 요청 타입을 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

public interface PriceDailyRepository extends JpaRepository<PriceDailyEntity, String> { // 일봉 가격 저장소 인터페이스를 정의한다.
    List<PriceDailyEntity> findByMarketOrderByTradeDateDesc(String market, Pageable pageable); // 시장별 일봉을 최근 거래일 순으로 조회한다.

    List<PriceDailyEntity> findByTickerOrderByTradeDateDesc(String ticker, Pageable pageable); // 종목별 일봉을 최근 거래일 순으로 조회한다.

    List<PriceDailyEntity> findByMarketAndTickerOrderByTradeDateDesc(String market, String ticker, Pageable pageable); // 시장과 종목별 일봉을 최근 거래일 순으로 조회한다.

    List<PriceDailyEntity> findAllByOrderByTradeDateDesc(Pageable pageable); // 전체 일봉을 최근 거래일 순으로 조회한다.

    List<PriceDailyEntity> findByMarketAndTicker(String market, String ticker, Pageable pageable); // 시장과 종목별 일봉을 조회한다.

    List<PriceDailyEntity> findByTradeDateBetweenOrderByTickerAscTradeDateAsc(LocalDate from, LocalDate to); // 기간 내 전체 일봉을 종목/거래일 순으로 조회한다.

    List<PriceDailyEntity> findByMarketAndTradeDateBetweenOrderByTickerAscTradeDateAsc(String market, LocalDate from, LocalDate to); // 시장과 기간으로 일봉을 종목/거래일 순으로 조회한다.

    // PIT 스냅샷용: asOf 이하 일봉만 조회 (미래참조 차단)
    List<PriceDailyEntity> findByMarketAndTickerAndTradeDateLessThanEqualOrderByTradeDateDesc(String market, String ticker, LocalDate asOf, Pageable pageable);

    // forward return 계산용: asOf 이후 일봉 오름차순
    List<PriceDailyEntity> findByMarketAndTickerAndTradeDateGreaterThanOrderByTradeDateAsc(String market, String ticker, LocalDate asOf, Pageable pageable);

    // as_of_date 정확히 일치하는 단건
    java.util.Optional<PriceDailyEntity> findByMarketAndTickerAndTradeDate(String market, String ticker, LocalDate tradeDate);
} // 일봉 가격 저장소 인터페이스를 종료한다.
