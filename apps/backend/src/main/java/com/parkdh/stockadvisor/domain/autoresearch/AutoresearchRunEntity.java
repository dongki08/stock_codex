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
import java.util.UUID; // UUID 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "autoresearch_run") // AutoResearch 실행 테이블에 매핑한다.
public class AutoresearchRunEntity extends CreatedEntity { // AutoResearch 실행 엔티티를 정의한다.
    @Id // 실행 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("AutoResearch 실행 ID") // 실행 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 실행 ID 컬럼을 매핑한다.
    private Long id; // 실행 ID를 보관한다.

    @Comment("작업 실행 UUID") // 작업 실행 UUID 컬럼 설명을 지정한다.
    @Column(name = "job_run_id", nullable = false) // 작업 실행 UUID 컬럼을 매핑한다.
    private UUID jobRunId; // 야간 작업 실행 단위 UUID를 보관한다.

    @Comment("반복 번호") // 반복 번호 컬럼 설명을 지정한다.
    @Column(name = "iter_no", nullable = false) // 반복 번호 컬럼을 매핑한다.
    private Integer iterNo; // 실험 반복 번호를 보관한다.

    @Comment("부모 커밋 SHA") // 부모 커밋 SHA 컬럼 설명을 지정한다.
    @Column(name = "parent_sha", length = 64) // 부모 커밋 SHA 컬럼을 매핑한다.
    private String parentSha; // 실험 기준 커밋을 보관한다.

    @Comment("제안 커밋 SHA") // 제안 커밋 SHA 컬럼 설명을 지정한다.
    @Column(name = "proposal_sha", length = 64) // 제안 커밋 SHA 컬럼을 매핑한다.
    private String proposalSha; // 실험 제안 커밋을 보관한다.

    @Comment("변경 요약") // 변경 요약 컬럼 설명을 지정한다.
    @Column(name = "diff_summary", columnDefinition = "nvarchar(max)") // 변경 요약 컬럼을 매핑한다.
    private String diffSummary; // 에이전트 변경 요약을 보관한다.

    @Comment("지표명") // 지표명 컬럼 설명을 지정한다.
    @Column(name = "metric_name", length = 40) // 지표명 컬럼을 매핑한다.
    private String metricName; // 평가 지표명을 보관한다.

    @Comment("지표 값") // 지표 값 컬럼 설명을 지정한다.
    @Column(name = "metric_value", precision = 10, scale = 4) // 지표 값 컬럼을 매핑한다.
    private BigDecimal metricValue; // 실험 지표 값을 보관한다.

    @Comment("챔피언 지표 값") // 챔피언 지표 값 컬럼 설명을 지정한다.
    @Column(name = "champion_metric", precision = 10, scale = 4) // 챔피언 지표 값 컬럼을 매핑한다.
    private BigDecimal championMetric; // 기존 챔피언 지표 값을 보관한다.

    @Comment("실험 결정") // 실험 결정 컬럼 설명을 지정한다.
    @Column(name = "decision", length = 10, nullable = false) // 실험 결정 컬럼을 매핑한다.
    private String decision; // KEEP, DISCARD, ERROR 결정을 보관한다.

    @Comment("소요 시간 밀리초") // 소요 시간 컬럼 설명을 지정한다.
    @Column(name = "duration_ms") // 소요 시간 컬럼을 매핑한다.
    private Integer durationMs; // 실험 소요 시간을 보관한다.

    @Comment("시작 일시") // 시작 일시 컬럼 설명을 지정한다.
    @Column(name = "started_at") // 시작 일시 컬럼을 매핑한다.
    private LocalDateTime startedAt; // 실험 시작 일시를 보관한다.

    @Comment("종료 일시") // 종료 일시 컬럼 설명을 지정한다.
    @Column(name = "ended_at") // 종료 일시 컬럼을 매핑한다.
    private LocalDateTime endedAt; // 실험 종료 일시를 보관한다.

    public AutoresearchRunEntity(UUID jobRunId, Integer iterNo, String parentSha, String proposalSha, String diffSummary, String metricName, BigDecimal metricValue, BigDecimal championMetric, String decision, Integer durationMs, LocalDateTime startedAt, LocalDateTime endedAt) { // AutoResearch 실행 생성자를 정의한다.
        this.jobRunId = jobRunId; // 작업 실행 UUID를 저장한다.
        this.iterNo = iterNo; // 반복 번호를 저장한다.
        this.parentSha = parentSha; // 부모 커밋 SHA를 저장한다.
        this.proposalSha = proposalSha; // 제안 커밋 SHA를 저장한다.
        this.diffSummary = diffSummary; // 변경 요약을 저장한다.
        this.metricName = metricName; // 지표명을 저장한다.
        this.metricValue = metricValue; // 지표 값을 저장한다.
        this.championMetric = championMetric; // 챔피언 지표 값을 저장한다.
        this.decision = decision; // 실험 결정을 저장한다.
        this.durationMs = durationMs; // 소요 시간을 저장한다.
        this.startedAt = startedAt; // 시작 일시를 저장한다.
        this.endedAt = endedAt; // 종료 일시를 저장한다.
    } // 생성자를 종료한다.
} // AutoResearch 실행 엔티티를 종료한다.
