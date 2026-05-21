package com.parkdh.stockadvisor.domain.brief; // 데일리 브리프 도메인 패키지를 선언한다.

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
@Table(name = "daily_brief") // 데일리 브리프 테이블에 매핑한다.
public class DailyBriefEntity extends CreatedEntity { // 데일리 브리프 엔티티를 정의한다.
    @Id // 데일리 브리프 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("데일리 브리프 ID") // 데일리 브리프 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 데일리 브리프 ID 컬럼을 매핑한다.
    private Long id; // 데일리 브리프 ID를 보관한다.

    @Comment("시장 트랙") // 시장 트랙 컬럼 설명을 지정한다.
    @Column(name = "market_track", length = 20, nullable = false) // 시장 트랙 컬럼을 매핑한다.
    private String marketTrack; // KRX, US, US_CLOSE 값을 보관한다.

    @Comment("브리프 마크다운") // 브리프 마크다운 컬럼 설명을 지정한다.
    @Column(name = "brief_md", nullable = false, columnDefinition = "nvarchar(max)") // 브리프 마크다운 컬럼을 매핑한다.
    private String briefMd; // 브리프 본문을 보관한다.

    @Comment("초안 번호") // 초안 번호 컬럼 설명을 지정한다.
    @Column(name = "draft_no", nullable = false) // 초안 번호 컬럼을 매핑한다.
    private Integer draftNo; // 초안 번호를 보관한다.

    @Comment("커버리지 점수") // 커버리지 점수 컬럼 설명을 지정한다.
    @Column(name = "coverage", precision = 4, scale = 3) // 커버리지 점수 컬럼을 매핑한다.
    private BigDecimal coverage; // 필수 정보 커버리지 점수를 보관한다.

    @Comment("환각 플래그 수") // 환각 플래그 수 컬럼 설명을 지정한다.
    @Column(name = "hallucination_flags") // 환각 플래그 수 컬럼을 매핑한다.
    private Integer hallucinationFlags; // 검증 실패 플래그 수를 보관한다.

    @Comment("LLM 모델") // LLM 모델 컬럼 설명을 지정한다.
    @Column(name = "llm_model", length = 40) // LLM 모델 컬럼을 매핑한다.
    private String llmModel; // 사용 모델명을 보관한다.

    @Comment("생성 일시") // 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "generated_at", nullable = false) // 생성 일시 컬럼을 매핑한다.
    private LocalDateTime generatedAt; // 브리프 생성 일시를 보관한다.

    public DailyBriefEntity(String marketTrack, String briefMd, Integer draftNo, BigDecimal coverage, Integer hallucinationFlags, String llmModel, LocalDateTime generatedAt) { // 데일리 브리프 생성자를 정의한다.
        this.marketTrack = marketTrack; // 시장 트랙을 저장한다.
        this.briefMd = briefMd; // 브리프 본문을 저장한다.
        this.draftNo = draftNo; // 초안 번호를 저장한다.
        this.coverage = coverage; // 커버리지 점수를 저장한다.
        this.hallucinationFlags = hallucinationFlags; // 환각 플래그 수를 저장한다.
        this.llmModel = llmModel; // LLM 모델명을 저장한다.
        this.generatedAt = generatedAt; // 생성 일시를 저장한다.
    } // 생성자를 종료한다.
} // 데일리 브리프 엔티티를 종료한다.
