package com.parkdh.stockadvisor.domain.autoresearch; // AutoResearch 도메인 패키지를 선언한다.

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
@Table(name = "strategy_version") // 전략 버전 테이블에 매핑한다.
public class StrategyVersionEntity extends CreatedEntity { // 전략 버전 엔티티를 정의한다.
    @Id // 전략 버전 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("전략 버전 ID") // 전략 버전 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 전략 버전 ID 컬럼을 매핑한다.
    private Long id; // 전략 버전 ID를 보관한다.

    @Comment("전략 버전명") // 전략 버전명 컬럼 설명을 지정한다.
    @Column(name = "semver", length = 20, nullable = false) // 전략 버전명 컬럼을 매핑한다.
    private String semver; // v24.0.0 같은 전략 버전명을 보관한다.

    @Comment("커밋 SHA") // 커밋 SHA 컬럼 설명을 지정한다.
    @Column(name = "git_sha", length = 64, nullable = false) // 커밋 SHA 컬럼을 매핑한다.
    private String gitSha; // 채택된 전략 커밋 SHA를 보관한다.

    @Comment("지표 값") // 지표 값 컬럼 설명을 지정한다.
    @Column(name = "metric_value", precision = 10, scale = 4, nullable = false) // 지표 값 컬럼을 매핑한다.
    private BigDecimal metricValue; // 전략 평가 지표 값을 보관한다.

    @Comment("승격 일시") // 승격 일시 컬럼 설명을 지정한다.
    @Column(name = "promoted_at", nullable = false) // 승격 일시 컬럼을 매핑한다.
    private LocalDateTime promotedAt; // 전략 승격 일시를 보관한다.

    @Comment("챔피언 여부") // 챔피언 여부 컬럼 설명을 지정한다.
    @Column(name = "is_champion", nullable = false) // 챔피언 여부 컬럼을 매핑한다.
    private Boolean champion; // 현재 챔피언 여부를 보관한다.

    public StrategyVersionEntity(String semver, String gitSha, BigDecimal metricValue, LocalDateTime promotedAt, Boolean champion) { // 전략 버전 생성자를 정의한다.
        this.semver = semver; // 전략 버전명을 저장한다.
        this.gitSha = gitSha; // 커밋 SHA를 저장한다.
        this.metricValue = metricValue; // 지표 값을 저장한다.
        this.promotedAt = promotedAt; // 승격 일시를 저장한다.
        this.champion = champion; // 챔피언 여부를 저장한다.
    } // 생성자를 종료한다.
} // 전략 버전 엔티티를 종료한다.
