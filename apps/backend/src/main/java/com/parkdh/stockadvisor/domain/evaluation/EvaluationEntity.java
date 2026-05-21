package com.parkdh.stockadvisor.domain.evaluation; // 평가 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.CreatedEntity; // 생성일 공통 엔티티를 가져온다.
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
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "evaluation") // 평가 테이블에 매핑한다.
public class EvaluationEntity extends CreatedEntity { // 추천 평가 엔티티를 정의한다.
    @Id // 평가 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("평가 ID") // 평가 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 평가 ID 컬럼을 매핑한다.
    private Long id; // 평가 ID를 보관한다.

    @Comment("추천 ID") // 추천 ID 컬럼 설명을 지정한다.
    @Column(name = "recommendation_id", nullable = false) // 추천 ID 컬럼을 매핑한다.
    private Long recommendationId; // 평가 대상 추천 ID를 보관한다.

    @Comment("실제 매도 가격") // 실제 매도 가격 컬럼 설명을 지정한다.
    @Column(name = "actual_exit_price", precision = 18, scale = 4) // 실제 매도 가격 컬럼을 매핑한다.
    private BigDecimal actualExitPrice; // 실제 매도 가격을 보관한다.

    @Comment("청산 사유") // 청산 사유 컬럼 설명을 지정한다.
    @Column(name = "exit_reason", length = 20, nullable = false) // 청산 사유 컬럼을 매핑한다.
    private String exitReason; // TARGET_HIT, STOP_HIT, TIME_OUT 값을 보관한다.

    @Comment("손익률") // 손익률 컬럼 설명을 지정한다.
    @Column(name = "pnl_pct", precision = 8, scale = 4, nullable = false) // 손익률 컬럼을 매핑한다.
    private BigDecimal pnlPct; // 손익률을 보관한다.

    @Comment("최대 낙폭") // 최대 낙폭 컬럼 설명을 지정한다.
    @Column(name = "drawdown_pct", precision = 8, scale = 4) // 최대 낙폭 컬럼을 매핑한다.
    private BigDecimal drawdownPct; // 최대 낙폭을 보관한다.

    @Comment("목표가 적중 여부") // 목표가 적중 여부 컬럼 설명을 지정한다.
    @Column(name = "hit_target", nullable = false) // 목표가 적중 여부 컬럼을 매핑한다.
    private Boolean hitTarget; // 목표가 적중 여부를 보관한다.

    @Comment("평가 일시") // 평가 일시 컬럼 설명을 지정한다.
    @Column(name = "evaluated_at", nullable = false) // 평가 일시 컬럼을 매핑한다.
    private LocalDateTime evaluatedAt; // 평가 일시를 보관한다.

    public EvaluationEntity(Long recommendationId, BigDecimal actualExitPrice, String exitReason, BigDecimal pnlPct, BigDecimal drawdownPct, Boolean hitTarget, LocalDateTime evaluatedAt) { // 평가 생성자를 정의한다.
        this.recommendationId = recommendationId; // 추천 ID를 저장한다.
        this.actualExitPrice = actualExitPrice; // 실제 매도 가격을 저장한다.
        this.exitReason = exitReason; // 청산 사유를 저장한다.
        this.pnlPct = pnlPct; // 손익률을 저장한다.
        this.drawdownPct = drawdownPct; // 최대 낙폭을 저장한다.
        this.hitTarget = hitTarget; // 목표가 적중 여부를 저장한다.
        this.evaluatedAt = evaluatedAt; // 평가 일시를 저장한다.
    } // 생성자를 종료한다.
} // 평가 엔티티를 종료한다.
