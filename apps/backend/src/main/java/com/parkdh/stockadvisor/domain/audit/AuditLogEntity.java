package com.parkdh.stockadvisor.domain.audit; // 감사 로그 도메인 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.common.CreatedEntity; // 생성일 기본 엔티티를 가져온다.
import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.Entity; // 엔티티 어노테이션을 가져온다.
import jakarta.persistence.GeneratedValue; // 자동 생성 어노테이션을 가져온다.
import jakarta.persistence.GenerationType; // 기본 키 생성 전략을 가져온다.
import jakarta.persistence.Id; // 기본 키 어노테이션을 가져온다.
import jakarta.persistence.Table; // 테이블 매핑 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@Entity // JPA 엔티티로 등록한다.
@Table(name = "audit_log") // 감사 로그 테이블에 매핑한다.
public class AuditLogEntity extends CreatedEntity { // 감사 로그 엔티티를 정의한다.
    @Id // 감사 로그 ID를 기본 키로 지정한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("감사 로그 ID") // 감사 로그 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // ID 컬럼을 매핑한다.
    private Long id; // 감사 로그 ID를 보관한다.

    @Comment("수행자") // 수행자 컬럼 설명을 지정한다.
    @Column(name = "actor", length = 40, nullable = false) // 수행자 컬럼을 매핑한다.
    private String actor; // 수행자 값을 보관한다.

    @Comment("작업명") // 작업명 컬럼 설명을 지정한다.
    @Column(name = "action", length = 60, nullable = false) // 작업명 컬럼을 매핑한다.
    private String action; // 작업명을 보관한다.

    @Comment("변경 전 JSON") // 변경 전 JSON 컬럼 설명을 지정한다.
    @Column(name = "before_json", nullable = false, columnDefinition = "nvarchar(max)") // 변경 전 JSON 컬럼을 매핑한다.
    private String beforeJson; // 변경 전 JSON 문자열을 보관한다.

    @Comment("변경 후 JSON") // 변경 후 JSON 컬럼 설명을 지정한다.
    @Column(name = "after_json", nullable = false, columnDefinition = "nvarchar(max)") // 변경 후 JSON 컬럼을 매핑한다.
    private String afterJson; // 변경 후 JSON 문자열을 보관한다.

    public AuditLogEntity(String actor, String action, String beforeJson, String afterJson) { // 감사 로그 생성자를 정의한다.
        this.actor = actor; // 수행자를 저장한다.
        this.action = action; // 작업명을 저장한다.
        this.beforeJson = beforeJson; // 변경 전 JSON을 저장한다.
        this.afterJson = afterJson; // 변경 후 JSON을 저장한다.
    } // 생성자를 종료한다.
} // 감사 로그 엔티티를 종료한다.
