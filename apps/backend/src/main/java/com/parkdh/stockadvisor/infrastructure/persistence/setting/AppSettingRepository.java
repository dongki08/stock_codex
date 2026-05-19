package com.parkdh.stockadvisor.infrastructure.persistence.setting; // 설정 저장소 패키지를 선언한다.

import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 앱 설정 엔티티를 가져온다.
import org.springframework.data.jpa.repository.JpaRepository; // JPA 저장소 인터페이스를 가져온다.

public interface AppSettingRepository extends JpaRepository<AppSettingEntity, String> { // 앱 설정 저장소 인터페이스를 정의한다.
} // 앱 설정 저장소 인터페이스를 종료한다.
