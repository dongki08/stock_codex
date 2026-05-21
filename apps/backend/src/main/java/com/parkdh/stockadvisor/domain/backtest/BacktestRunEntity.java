package com.parkdh.stockadvisor.domain.backtest; // 백테스트 도메인 패키지를 선언한다.

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

import java.time.LocalDate; // 날짜 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "backtest_run") // 백테스트 실행 테이블에 매핑한다.
public class BacktestRunEntity extends CreatedEntity { // 백테스트 실행 엔티티를 정의한다.
    @Id // 백테스트 실행 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("백테스트 실행 ID") // 백테스트 실행 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 백테스트 실행 ID 컬럼을 매핑한다.
    private Long id; // 백테스트 실행 ID를 보관한다.

    @Comment("전략명") // 전략명 컬럼 설명을 지정한다.
    @Column(name = "strategy", length = 100, nullable = false) // 전략명 컬럼을 매핑한다.
    private String strategy; // 전략명을 보관한다.

    @Comment("기간 시작일") // 기간 시작일 컬럼 설명을 지정한다.
    @Column(name = "period_from", nullable = false) // 기간 시작일 컬럼을 매핑한다.
    private LocalDate periodFrom; // 백테스트 시작일을 보관한다.

    @Comment("기간 종료일") // 기간 종료일 컬럼 설명을 지정한다.
    @Column(name = "period_to", nullable = false) // 기간 종료일 컬럼을 매핑한다.
    private LocalDate periodTo; // 백테스트 종료일을 보관한다.

    @Comment("지표 JSON") // 지표 JSON 컬럼 설명을 지정한다.
    @Column(name = "metrics_json", nullable = false, columnDefinition = "nvarchar(max)") // 지표 JSON 컬럼을 매핑한다.
    private String metricsJson; // 백테스트 지표 JSON 문자열을 보관한다.

    public BacktestRunEntity(String strategy, LocalDate periodFrom, LocalDate periodTo, String metricsJson) { // 백테스트 실행 생성자를 정의한다.
        this.strategy = strategy; // 전략명을 저장한다.
        this.periodFrom = periodFrom; // 기간 시작일을 저장한다.
        this.periodTo = periodTo; // 기간 종료일을 저장한다.
        this.metricsJson = metricsJson; // 지표 JSON을 저장한다.
    } // 생성자를 종료한다.
} // 백테스트 실행 엔티티를 종료한다.
