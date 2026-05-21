package com.parkdh.stockadvisor.domain.instrument; // 종목 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.BaseEntity; // 생성일과 수정일 공통 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "instrument") // 종목 테이블에 매핑한다.
public class InstrumentEntity extends BaseEntity { // 종목 엔티티를 정의한다.
    @Id // 티커를 기본 키로 사용한다.
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

    @Comment("활성 여부") // 활성 여부 컬럼 설명을 지정한다.
    @Column(name = "enabled", nullable = false) // 활성 여부 컬럼을 매핑한다.
    private Boolean enabled; // 추천 유니버스 포함 여부를 보관한다.

    public InstrumentEntity(String ticker, String market, String name, String sector, Boolean enabled) { // 종목 생성자를 정의한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.market = market; // 시장 구분을 저장한다.
        this.name = name; // 종목명을 저장한다.
        this.sector = sector; // 섹터명을 저장한다.
        this.enabled = enabled; // 활성 여부를 저장한다.
    } // 생성자를 종료한다.

    public void update(String market, String name, String sector, Boolean enabled) { // 종목 정보를 갱신한다.
        this.market = market; // 시장 구분을 갱신한다.
        this.name = name; // 종목명을 갱신한다.
        this.sector = sector; // 섹터명을 갱신한다.
        this.enabled = enabled; // 활성 여부를 갱신한다.
    } // 종목 정보 갱신을 종료한다.
} // 종목 엔티티를 종료한다.
