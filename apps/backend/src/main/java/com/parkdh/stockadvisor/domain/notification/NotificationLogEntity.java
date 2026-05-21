package com.parkdh.stockadvisor.domain.notification; // 알림 도메인 패키지를 선언한다.

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
@Table(name = "notification_log") // 알림 로그 테이블에 매핑한다.
public class NotificationLogEntity extends CreatedEntity { // 알림 로그 엔티티를 정의한다.
    @Id // 알림 로그 ID를 기본 키로 사용한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MSSQL IDENTITY 방식으로 키를 생성한다.
    @Comment("알림 로그 ID") // 알림 로그 ID 컬럼 설명을 지정한다.
    @Column(name = "id", nullable = false) // 알림 로그 ID 컬럼을 매핑한다.
    private Long id; // 알림 로그 ID를 보관한다.

    @Comment("알림 채널") // 알림 채널 컬럼 설명을 지정한다.
    @Column(name = "channel", length = 30, nullable = false) // 알림 채널 컬럼을 매핑한다.
    private String channel; // Telegram, Kakao 등 채널을 보관한다.

    @Comment("페이로드 해시") // 페이로드 해시 컬럼 설명을 지정한다.
    @Column(name = "payload_hash", length = 64, nullable = false) // 페이로드 해시 컬럼을 매핑한다.
    private String payloadHash; // 알림 페이로드 해시를 보관한다.

    @Comment("발송 일시") // 발송 일시 컬럼 설명을 지정한다.
    @Column(name = "sent_at") // 발송 일시 컬럼을 매핑한다.
    private LocalDateTime sentAt; // 발송 일시를 보관한다.

    @Comment("발송 상태") // 발송 상태 컬럼 설명을 지정한다.
    @Column(name = "status", length = 20, nullable = false) // 발송 상태 컬럼을 매핑한다.
    private String status; // SENT, FAILED, SKIPPED 상태를 보관한다.

    @Comment("에러 메시지") // 에러 메시지 컬럼 설명을 지정한다.
    @Column(name = "error_message", columnDefinition = "nvarchar(max)") // 에러 메시지 컬럼을 매핑한다.
    private String errorMessage; // 실패 사유를 보관한다.

    public NotificationLogEntity(String channel, String payloadHash, LocalDateTime sentAt, String status, String errorMessage) { // 알림 로그 생성자를 정의한다.
        this.channel = channel; // 알림 채널을 저장한다.
        this.payloadHash = payloadHash; // 페이로드 해시를 저장한다.
        this.sentAt = sentAt; // 발송 일시를 저장한다.
        this.status = status; // 발송 상태를 저장한다.
        this.errorMessage = errorMessage; // 에러 메시지를 저장한다.
    } // 생성자를 종료한다.
} // 알림 로그 엔티티를 종료한다.
