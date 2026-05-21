package com.parkdh.stockadvisor.domain.codex; // Codex 호출 도메인 패키지를 선언한다.

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

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "codex_call") // Codex 호출 로그 테이블에 매핑한다.
public class CodexCallEntity extends CreatedEntity { // Codex 호출 로그 엔티티를 정의한다.
    @Id // Codex 호출 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("Codex 호출 ID") // Codex 호출 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // Codex 호출 ID 컬럼을 매핑한다.
    private Long id; // Codex 호출 ID를 보관한다.

    @Comment("호출자") // 호출자 컬럼 설명을 지정한다.
    @Column(name = "caller", length = 40, nullable = false) // 호출자 컬럼을 매핑한다.
    private String caller; // BRIEF_KR, EXIT_CONFIRM 같은 호출 위치를 보관한다.

    @Comment("프롬프트 해시") // 프롬프트 해시 컬럼 설명을 지정한다.
    @Column(name = "prompt_hash", length = 64, nullable = false) // 프롬프트 해시 컬럼을 매핑한다.
    private String promptHash; // 프롬프트 SHA-256 해시를 보관한다.

    @Comment("프롬프트 길이") // 프롬프트 길이 컬럼 설명을 지정한다.
    @Column(name = "prompt_len", nullable = false) // 프롬프트 길이 컬럼을 매핑한다.
    private Integer promptLen; // 프롬프트 길이를 보관한다.

    @Comment("응답 길이") // 응답 길이 컬럼 설명을 지정한다.
    @Column(name = "response_len") // 응답 길이 컬럼을 매핑한다.
    private Integer responseLen; // 응답 길이를 보관한다.

    @Comment("사용 도구 JSON") // 사용 도구 JSON 컬럼 설명을 지정한다.
    @Column(name = "tools_used_json", columnDefinition = "nvarchar(max)") // 사용 도구 JSON 컬럼을 매핑한다.
    private String toolsUsedJson; // 사용 도구 JSON 문자열을 보관한다.

    @Comment("소요 시간 밀리초") // 소요 시간 컬럼 설명을 지정한다.
    @Column(name = "duration_ms") // 소요 시간 컬럼을 매핑한다.
    private Integer durationMs; // 호출 소요 시간을 보관한다.

    @Comment("성공 여부") // 성공 여부 컬럼 설명을 지정한다.
    @Column(name = "succeeded", nullable = false) // 성공 여부 컬럼을 매핑한다.
    private Boolean succeeded; // 호출 성공 여부를 보관한다.

    @Comment("에러 메시지") // 에러 메시지 컬럼 설명을 지정한다.
    @Column(name = "error_message", columnDefinition = "nvarchar(max)") // 에러 메시지 컬럼을 매핑한다.
    private String errorMessage; // 호출 실패 사유를 보관한다.

    @Comment("호출 일시") // 호출 일시 컬럼 설명을 지정한다.
    @Column(name = "called_at", nullable = false) // 호출 일시 컬럼을 매핑한다.
    private LocalDateTime calledAt; // 호출 일시를 보관한다.

    public CodexCallEntity(String caller, String promptHash, Integer promptLen, Integer responseLen, String toolsUsedJson, Integer durationMs, Boolean succeeded, String errorMessage, LocalDateTime calledAt) { // Codex 호출 로그 생성자를 정의한다.
        this.caller = caller; // 호출자를 저장한다.
        this.promptHash = promptHash; // 프롬프트 해시를 저장한다.
        this.promptLen = promptLen; // 프롬프트 길이를 저장한다.
        this.responseLen = responseLen; // 응답 길이를 저장한다.
        this.toolsUsedJson = toolsUsedJson; // 사용 도구 JSON을 저장한다.
        this.durationMs = durationMs; // 소요 시간을 저장한다.
        this.succeeded = succeeded; // 성공 여부를 저장한다.
        this.errorMessage = errorMessage; // 에러 메시지를 저장한다.
        this.calledAt = calledAt; // 호출 일시를 저장한다.
    } // 생성자를 종료한다.
} // Codex 호출 로그 엔티티를 종료한다.
