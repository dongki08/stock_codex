package com.parkdh.stockadvisor.domain.universe; // 시장 유니버스 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.BaseEntity; // 생성일과 수정일 공통 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "market_universe") // 시장 유니버스 테이블에 매핑한다.
public class MarketUniverseEntity extends BaseEntity { // 자동 추천 후보군 엔티티를 정의한다.
    @Id // 유니버스 키를 기본 키로 사용한다.
    @Comment("유니버스 키") // 유니버스 키 컬럼 설명을 지정한다.
    @Column(name = "universe_key", length = 60, nullable = false) // 유니버스 키 컬럼을 매핑한다.
    private String universeKey; // 시장과 종목 코드를 합친 키를 보관한다.

    @Comment("종목 코드") // 종목 코드 컬럼 설명을 지정한다.
    @Column(name = "ticker", length = 20, nullable = false) // 종목 코드 컬럼을 매핑한다.
    private String ticker; // 종목 코드를 보관한다.

    @Comment("시장 구분") // 시장 구분 컬럼 설명을 지정한다.
    @Column(name = "market", length = 20, nullable = false) // 시장 구분 컬럼을 매핑한다.
    private String market; // KOSPI, KOSDAQ, NYSE, NASDAQ 값을 보관한다.

    @Comment("종목명") // 종목명 컬럼 설명을 지정한다.
    @Column(name = "name", length = 200, nullable = false) // 종목명 컬럼을 매핑한다.
    private String name; // 종목명을 보관한다.

    @Comment("섹터") // 섹터 컬럼 설명을 지정한다.
    @Column(name = "sector", length = 100) // 섹터 컬럼을 매핑한다.
    private String sector; // 섹터명을 보관한다.

    @Comment("시가총액") // 시가총액 컬럼 설명을 지정한다.
    @Column(name = "market_cap", precision = 24, scale = 4) // 시가총액 컬럼을 매핑한다.
    private BigDecimal marketCap; // 시가총액을 보관한다.

    @Comment("평균 거래대금") // 평균 거래대금 컬럼 설명을 지정한다.
    @Column(name = "avg_turnover", precision = 24, scale = 4) // 평균 거래대금 컬럼을 매핑한다.
    private BigDecimal avgTurnover; // 평균 거래대금을 보관한다.

    @Comment("최근 가격") // 최근 가격 컬럼 설명을 지정한다.
    @Column(name = "last_price", precision = 24, scale = 4) // 최근 가격 컬럼을 매핑한다.
    private BigDecimal lastPrice; // 최근 가격을 보관한다.

    @Comment("거래 가능 여부") // 거래 가능 여부 컬럼 설명을 지정한다.
    @Column(name = "tradable", nullable = false) // 거래 가능 여부 컬럼을 매핑한다.
    private Boolean tradable; // 추천 후보 포함 가능 여부를 보관한다.

    @Comment("데이터 출처") // 데이터 출처 컬럼 설명을 지정한다.
    @Column(name = "source", length = 50, nullable = false) // 데이터 출처 컬럼을 매핑한다.
    private String source; // KIS, KRX, NASDAQ, DEV 같은 출처를 보관한다.

    @Comment("마지막 동기화일") // 마지막 동기화일 컬럼 설명을 지정한다.
    @Column(name = "last_synced_at") // 마지막 동기화일 컬럼을 매핑한다.
    private LocalDate lastSyncedAt; // 마지막 동기화일을 보관한다.

    public MarketUniverseEntity(String ticker, String market, String name, String sector, BigDecimal marketCap, BigDecimal avgTurnover, BigDecimal lastPrice, Boolean tradable, String source, LocalDate lastSyncedAt) { // 시장 유니버스 생성자를 정의한다.
        this.universeKey = buildKey(market, ticker); // 유니버스 키를 생성한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.market = market; // 시장 구분을 저장한다.
        this.name = name; // 종목명을 저장한다.
        this.sector = sector; // 섹터명을 저장한다.
        this.marketCap = marketCap; // 시가총액을 저장한다.
        this.avgTurnover = avgTurnover; // 평균 거래대금을 저장한다.
        this.lastPrice = lastPrice; // 최근 가격을 저장한다.
        this.tradable = tradable; // 거래 가능 여부를 저장한다.
        this.source = source; // 데이터 출처를 저장한다.
        this.lastSyncedAt = lastSyncedAt; // 마지막 동기화일을 저장한다.
    } // 생성자를 종료한다.

    public void update(String name, String sector, BigDecimal marketCap, BigDecimal avgTurnover, BigDecimal lastPrice, Boolean tradable, String source, LocalDate lastSyncedAt) { // 시장 유니버스 정보를 갱신한다.
        this.name = name; // 종목명을 갱신한다.
        this.sector = sector; // 섹터명을 갱신한다.
        this.marketCap = marketCap; // 시가총액을 갱신한다.
        this.avgTurnover = avgTurnover; // 평균 거래대금을 갱신한다.
        this.lastPrice = lastPrice; // 최근 가격을 갱신한다.
        this.tradable = tradable; // 거래 가능 여부를 갱신한다.
        this.source = source; // 데이터 출처를 갱신한다.
        this.lastSyncedAt = lastSyncedAt; // 마지막 동기화일을 갱신한다.
    } // 시장 유니버스 정보 갱신을 종료한다.

    public static String buildKey(String market, String ticker) { // 시장과 종목 코드로 유니버스 키를 만든다.
        return market + ":" + ticker; // 유니버스 키를 반환한다.
    } // 유니버스 키 생성을 종료한다.
} // 시장 유니버스 엔티티를 종료한다.
