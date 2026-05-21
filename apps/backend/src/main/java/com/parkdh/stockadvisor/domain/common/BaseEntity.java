package com.parkdh.stockadvisor.domain.common; // 공통 엔티티 패키지를 선언한다.

import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.MappedSuperclass; // 매핑 상위 클래스 어노테이션을 가져온다.
import jakarta.persistence.PrePersist; // 저장 전 콜백 어노테이션을 가져온다.
import jakarta.persistence.PreUpdate; // 수정 전 콜백 어노테이션을 가져온다.
import lombok.AccessLevel; // 접근 제한 레벨을 가져온다.
import lombok.Getter; // Getter 어노테이션을 가져온다.
import lombok.NoArgsConstructor; // 기본 생성자 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@Getter // 모든 필드의 Getter를 자동 생성한다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자를 protected로 자동 생성한다.
@MappedSuperclass // 하위 엔티티에 공통 매핑 필드를 제공한다.
public abstract class BaseEntity { // 생성일과 수정일을 가진 기본 엔티티를 정의한다.
    @Comment("생성 일시") // 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "created_at", nullable = false, updatable = false) // 생성 일시 컬럼을 매핑한다.
    private LocalDateTime createdAt; // 생성 일시 값을 보관한다.

    @Comment("수정 일시") // 수정 일시 컬럼 설명을 지정한다.
    @Column(name = "updated_at", nullable = false) // 수정 일시 컬럼을 매핑한다.
    private LocalDateTime updatedAt; // 수정 일시 값을 보관한다.

    @PrePersist // 최초 저장 전에 실행한다.
    protected void onCreate() { // 생성 콜백 메서드를 정의한다.
        LocalDateTime now = LocalDateTime.now(); // 현재 시간을 구한다.
        this.createdAt = now; // 생성 일시를 현재 시간으로 설정한다.
        this.updatedAt = now; // 수정 일시를 현재 시간으로 설정한다.
    } // 생성 콜백을 종료한다.

    @PreUpdate // 수정 전에 실행한다.
    protected void onUpdate() { // 수정 콜백 메서드를 정의한다.
        this.updatedAt = LocalDateTime.now(); // 수정 일시를 현재 시간으로 갱신한다.
    } // 수정 콜백을 종료한다.
} // 기본 엔티티를 종료한다.
