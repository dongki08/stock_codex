package com.parkdh.stockadvisor.domain.recommendation; // 추천 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.BaseEntity; // 생성일과 수정일 공통 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.GeneratedValue; // 기본 키 자동 생성 어노테이션을 가져온다.
import jakarta.persistence.GenerationType; // 기본 키 생성 전략을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "recommendation") // 추천 테이블에 매핑한다.
public class RecommendationEntity extends BaseEntity { // 추천 엔티티를 정의한다.
    @Id // 추천 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("추천 ID") // 추천 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 추천 ID 컬럼을 매핑한다.
    private Long id; // 추천 ID를 보관한다.

    @Comment("종목 코드") // 종목 코드 컬럼 설명을 지정한다.
    @Column(name = "ticker", length = 20, nullable = false) // 종목 코드 컬럼을 매핑한다.
    private String ticker; // 종목 코드를 보관한다.

    @Comment("시장 구분") // 시장 구분 컬럼 설명을 지정한다.
    @Column(name = "market", length = 20, nullable = false) // 시장 구분 컬럼을 매핑한다.
    private String market; // 시장 구분을 보관한다.

    @Comment("보유 기간 구분") // 보유 기간 구분 컬럼 설명을 지정한다.
    @Column(name = "term", length = 10, nullable = false) // 보유 기간 구분 컬럼을 매핑한다.
    private String term; // SHORT 또는 LONG 값을 보관한다.

    @Comment("진입 가격") // 진입 가격 컬럼 설명을 지정한다.
    @Column(name = "entry_price", precision = 18, scale = 4, nullable = false) // 진입 가격 컬럼을 매핑한다.
    private BigDecimal entryPrice; // 진입 가격을 보관한다.

    @Comment("목표 가격") // 목표 가격 컬럼 설명을 지정한다.
    @Column(name = "target_price", precision = 18, scale = 4, nullable = false) // 목표 가격 컬럼을 매핑한다.
    private BigDecimal targetPrice; // 목표 가격을 보관한다.

    @Comment("손절 가격") // 손절 가격 컬럼 설명을 지정한다.
    @Column(name = "stop_price", precision = 18, scale = 4, nullable = false) // 손절 가격 컬럼을 매핑한다.
    private BigDecimal stopPrice; // 손절 가격을 보관한다.

    @Comment("예상 매도일") // 예상 매도일 컬럼 설명을 지정한다.
    @Column(name = "expected_exit_at", nullable = false) // 예상 매도일 컬럼을 매핑한다.
    private LocalDate expectedExitAt; // 예상 매도일을 보관한다.

    @Comment("신뢰도") // 신뢰도 컬럼 설명을 지정한다.
    @Column(name = "confidence", nullable = false) // 신뢰도 컬럼을 매핑한다.
    private Integer confidence; // 0부터 100까지의 신뢰도를 보관한다.

    @Comment("시그널 JSON") // 시그널 JSON 컬럼 설명을 지정한다.
    @Column(name = "signals_json", nullable = false, columnDefinition = "nvarchar(max)") // 시그널 JSON 컬럼을 매핑한다.
    private String signalsJson; // 시그널 원본 JSON 문자열을 보관한다.

    @Comment("모델 버전") // 모델 버전 컬럼 설명을 지정한다.
    @Column(name = "model_version", length = 40, nullable = false) // 모델 버전 컬럼을 매핑한다.
    private String modelVersion; // 모델 버전을 보관한다.

    @Comment("추천 생성 일시") // 추천 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "generated_at", nullable = false) // 추천 생성 일시 컬럼을 매핑한다.
    private LocalDateTime generatedAt; // 추천 생성 일시를 보관한다.

    @Comment("추천 상태") // 추천 상태 컬럼 설명을 지정한다.
    @Column(name = "status", length = 20, nullable = false) // 추천 상태 컬럼을 매핑한다.
    private String status; // OPEN, CLOSED, EXPIRED 상태를 보관한다.

    public RecommendationEntity(String ticker, String market, String term, BigDecimal entryPrice, BigDecimal targetPrice, BigDecimal stopPrice, LocalDate expectedExitAt, Integer confidence, String signalsJson, String modelVersion, LocalDateTime generatedAt, String status) { // 추천 생성자를 정의한다.
        this.ticker = ticker; // 종목 코드를 저장한다.
        this.market = market; // 시장 구분을 저장한다.
        this.term = term; // 보유 기간 구분을 저장한다.
        this.entryPrice = entryPrice; // 진입 가격을 저장한다.
        this.targetPrice = targetPrice; // 목표 가격을 저장한다.
        this.stopPrice = stopPrice; // 손절 가격을 저장한다.
        this.expectedExitAt = expectedExitAt; // 예상 매도일을 저장한다.
        this.confidence = confidence; // 신뢰도를 저장한다.
        this.signalsJson = signalsJson; // 시그널 JSON을 저장한다.
        this.modelVersion = modelVersion; // 모델 버전을 저장한다.
        this.generatedAt = generatedAt; // 생성 일시를 저장한다.
        this.status = status; // 추천 상태를 저장한다.
    } // 생성자를 종료한다.

    public void updateStatus(String status) { // 추천 상태를 갱신한다.
        this.status = status; // 새 추천 상태를 저장한다.
    } // 추천 상태 갱신을 종료한다.
} // 추천 엔티티를 종료한다.
