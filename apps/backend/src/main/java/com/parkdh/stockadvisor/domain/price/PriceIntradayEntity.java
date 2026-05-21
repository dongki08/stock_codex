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
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.time.temporal.ChronoUnit; // 시간 절삭 단위를 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "price_intraday") // 장중 가격 테이블에 매핑한다.
public class PriceIntradayEntity extends BaseEntity { // 장중 가격 엔티티를 정의한다.
    @Id // 가격 키를 기본 키로 사용한다.
    @Comment("장중 가격 키") // 가격 키 컬럼 설명을 지정한다.
    @Column(name = "price_key", length = 100, nullable = false) // 가격 키 컬럼을 매핑한다.
    private String priceKey; // 시장, 종목, 체결 시각을 합친 키를 보관한다.

    @Comment("종목 코드") // 종목 코드 컬럼 설명을 지정한다.
    @Column(name = "ticker", length = 20, nullable = false) // 종목 코드 컬럼을 매핑한다.
    private String ticker; // 종목 코드를 보관한다.

    @Comment("시장 구분") // 시장 구분 컬럼 설명을 지정한다.
    @Column(name = "market", length = 20, nullable = false) // 시장 구분 컬럼을 매핑한다.
    private String market; // 시장 구분을 보관한다.

    @Comment("체결 기준 시각") // 체결 기준 시각 컬럼 설명을 지정한다.
    @Column(name = "tick_at", nullable = false) // 체결 기준 시각 컬럼을 매핑한다.
    private LocalDateTime tickAt; // 스냅샷 시각을 보관한다.

    @Comment("현재가") // 현재가 컬럼 설명을 지정한다.
    @Column(name = "price", precision = 24, scale = 4, nullable = false) // 현재가 컬럼을 매핑한다.
    private BigDecimal price; // 현재가를 보관한다.

    @Comment("누적 거래량") // 누적 거래량 컬럼 설명을 지정한다.
    @Column(name = "volume", precision = 24, scale = 4) // 누적 거래량 컬럼을 매핑한다.
    private BigDecimal volume; // 누적 거래량을 보관한다.

    @Comment("데이터 출처") // 데이터 출처 컬럼 설명을 지정한다.
    @Column(name = "source", length = 50, nullable = false) // 데이터 출처 컬럼을 매핑한다.
    private String source; // KIS 같은 출처를 보관한다.

    public PriceIntradayEntity(String ticker, String market, LocalDateTime tickAt, BigDecimal price, BigDecimal volume, String source) { // 장중 가격 생성자를 정의한다.
        LocalDateTime normalizedTickAt = normalizeTickAt(tickAt); // 키와 저장 시각을 초 단위로 정규화한다.
        this.priceKey = buildKey(market, ticker, normalizedTickAt); // 가격 키를 생성한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.market = market; // 시장 구분을 저장한다.
        this.tickAt = normalizedTickAt; // 스냅샷 시각을 저장한다.
        this.price = price; // 현재가를 저장한다.
        this.volume = volume; // 누적 거래량을 저장한다.
        this.source = source; // 데이터 출처를 저장한다.
    } // 생성자를 종료한다.

    public void update(BigDecimal price, BigDecimal volume, String source) { // 장중 가격 값을 갱신한다.
        this.price = price; // 현재가를 갱신한다.
        this.volume = volume; // 누적 거래량을 갱신한다.
        this.source = source; // 데이터 출처를 갱신한다.
    } // 장중 가격 값 갱신을 종료한다.

    public static String buildKey(String market, String ticker, LocalDateTime tickAt) { // 시장, 종목, 시각으로 가격 키를 만든다.
        return market + ":" + ticker + ":" + normalizeTickAt(tickAt); // 가격 키를 반환한다.
    } // 가격 키 생성을 종료한다.

    public static LocalDateTime normalizeTickAt(LocalDateTime tickAt) { // 장중 가격 시각을 저장 단위에 맞게 정규화한다.
        return tickAt.truncatedTo(ChronoUnit.SECONDS); // 초 단위로 절삭해 같은 초의 중복 저장을 방지한다.
    } // 장중 가격 시각 정규화를 종료한다.
} // 장중 가격 엔티티를 종료한다.
