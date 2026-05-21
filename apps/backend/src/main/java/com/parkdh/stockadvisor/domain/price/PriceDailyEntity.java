package com.parkdh.stockadvisor.domain.price; // 가격 도메인 패키지를 선언한다.

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
@Table(name = "price_daily") // 일봉 가격 테이블에 매핑한다.
public class PriceDailyEntity extends BaseEntity { // 일봉 가격 엔티티를 정의한다.
    @Id // 가격 키를 기본 키로 사용한다.
    @Comment("가격 일봉 키") // 가격 키 컬럼 설명을 지정한다.
    @Column(name = "price_key", length = 80, nullable = false) // 가격 키 컬럼을 매핑한다.
    private String priceKey; // 시장, 종목, 거래일을 합친 키를 보관한다.

    @Comment("종목 코드") // 종목 코드 컬럼 설명을 지정한다.
    @Column(name = "ticker", length = 20, nullable = false) // 종목 코드 컬럼을 매핑한다.
    private String ticker; // 종목 코드를 보관한다.

    @Comment("시장 구분") // 시장 구분 컬럼 설명을 지정한다.
    @Column(name = "market", length = 20, nullable = false) // 시장 구분 컬럼을 매핑한다.
    private String market; // 시장 구분을 보관한다.

    @Comment("거래일") // 거래일 컬럼 설명을 지정한다.
    @Column(name = "trade_date", nullable = false) // 거래일 컬럼을 매핑한다.
    private LocalDate tradeDate; // 거래일을 보관한다.

    @Comment("시가") // 시가 컬럼 설명을 지정한다.
    @Column(name = "open_price", precision = 24, scale = 4, nullable = false) // 시가 컬럼을 매핑한다.
    private BigDecimal openPrice; // 시가를 보관한다.

    @Comment("고가") // 고가 컬럼 설명을 지정한다.
    @Column(name = "high_price", precision = 24, scale = 4, nullable = false) // 고가 컬럼을 매핑한다.
    private BigDecimal highPrice; // 고가를 보관한다.

    @Comment("저가") // 저가 컬럼 설명을 지정한다.
    @Column(name = "low_price", precision = 24, scale = 4, nullable = false) // 저가 컬럼을 매핑한다.
    private BigDecimal lowPrice; // 저가를 보관한다.

    @Comment("종가") // 종가 컬럼 설명을 지정한다.
    @Column(name = "close_price", precision = 24, scale = 4, nullable = false) // 종가 컬럼을 매핑한다.
    private BigDecimal closePrice; // 종가를 보관한다.

    @Comment("거래량") // 거래량 컬럼 설명을 지정한다.
    @Column(name = "volume", precision = 24, scale = 4, nullable = false) // 거래량 컬럼을 매핑한다.
    private BigDecimal volume; // 거래량을 보관한다.

    @Comment("거래대금") // 거래대금 컬럼 설명을 지정한다.
    @Column(name = "turnover", precision = 24, scale = 4) // 거래대금 컬럼을 매핑한다.
    private BigDecimal turnover; // 거래대금을 보관한다.

    @Comment("데이터 출처") // 데이터 출처 컬럼 설명을 지정한다.
    @Column(name = "source", length = 50, nullable = false) // 데이터 출처 컬럼을 매핑한다.
    private String source; // KIS, STOOQ 같은 출처를 보관한다.

    public PriceDailyEntity(String ticker, String market, LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice, BigDecimal volume, BigDecimal turnover, String source) { // 일봉 가격 생성자를 정의한다.
        this.priceKey = buildKey(market, ticker, tradeDate); // 가격 키를 생성한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.market = market; // 시장 구분을 저장한다.
        this.tradeDate = tradeDate; // 거래일을 저장한다.
        this.openPrice = openPrice; // 시가를 저장한다.
        this.highPrice = highPrice; // 고가를 저장한다.
        this.lowPrice = lowPrice; // 저가를 저장한다.
        this.closePrice = closePrice; // 종가를 저장한다.
        this.volume = volume; // 거래량을 저장한다.
        this.turnover = turnover; // 거래대금을 저장한다.
        this.source = source; // 데이터 출처를 저장한다.
    } // 생성자를 종료한다.

    public void update(BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice, BigDecimal volume, BigDecimal turnover, String source) { // 일봉 가격 값을 갱신한다.
        this.openPrice = openPrice; // 시가를 갱신한다.
        this.highPrice = highPrice; // 고가를 갱신한다.
        this.lowPrice = lowPrice; // 저가를 갱신한다.
        this.closePrice = closePrice; // 종가를 갱신한다.
        this.volume = volume; // 거래량을 갱신한다.
        this.turnover = turnover; // 거래대금을 갱신한다.
        this.source = source; // 데이터 출처를 갱신한다.
    } // 일봉 가격 값 갱신을 종료한다.

    public static String buildKey(String market, String ticker, LocalDate tradeDate) { // 시장, 종목, 거래일로 가격 키를 만든다.
        return market + ":" + ticker + ":" + tradeDate; // 가격 키를 반환한다.
    } // 가격 키 생성을 종료한다.
} // 일봉 가격 엔티티를 종료한다.
