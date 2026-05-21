package com.parkdh.stockadvisor.infrastructure.persistence.notification; // 알림 로그 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.notification.NotificationLogEntity; // 알림 로그 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

import java.util.List; // 목록 타입을 가져온다.

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> { // 알림 로그 저장소 인터페이스를 정의한다.
    List<NotificationLogEntity> findByStatus(String status); // 발송 상태로 알림 로그 목록을 조회한다.

    boolean existsByChannelAndPayloadHashAndStatus(String channel, String payloadHash, String status); // 채널/페이로드/상태 기준 중복 발송 여부를 확인한다.
} // 알림 로그 저장소 인터페이스를 종료한다.
