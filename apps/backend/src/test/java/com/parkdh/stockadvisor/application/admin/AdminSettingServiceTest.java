package com.parkdh.stockadvisor.application.admin; // 테스트 대상 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 검증 도구를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingResponse; // 설정 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingUpdateRequest; // 설정 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.audit.AuditLogEntity; // 감사 로그 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.audit.AuditLogRepository; // 감사 로그 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 설정 저장소를 가져온다.
import org.junit.jupiter.api.BeforeEach; // 각 테스트 전 실행 어노테이션을 가져온다.
import org.junit.jupiter.api.Test; // 테스트 어노테이션을 가져온다.
import org.junit.jupiter.api.extension.ExtendWith; // 확장 어노테이션을 가져온다.
import org.mockito.ArgumentCaptor; // 인자 캡처 도구를 가져온다.
import org.mockito.Mock; // 목 객체 어노테이션을 가져온다.
import org.mockito.junit.jupiter.MockitoExtension; // Mockito 확장을 가져온다.

import java.util.List; // 목록 타입을 가져온다.
import java.util.Optional; // Optional 타입을 가져온다.

import static org.assertj.core.api.Assertions.assertThat; // 검증 메서드를 가져온다.
import static org.assertj.core.api.Assertions.assertThatThrownBy; // 예외 검증 메서드를 가져온다.
import static org.mockito.ArgumentMatchers.any; // 임의 인자 매처를 가져온다.
import static org.mockito.Mockito.verify; // 호출 검증 메서드를 가져온다.
import static org.mockito.Mockito.when; // 목 동작 지정 메서드를 가져온다.

@ExtendWith(MockitoExtension.class) // Mockito 확장을 활성화한다.
class AdminSettingServiceTest { // 관리자 설정 서비스 테스트 클래스를 정의한다.
    @Mock // 설정 저장소 목 객체를 생성한다.
    private AppSettingRepository appSettingRepository; // 설정 저장소 의존성을 선언한다.

    @Mock // 감사 로그 저장소 목 객체를 생성한다.
    private AuditLogRepository auditLogRepository; // 감사 로그 저장소 의존성을 선언한다.

    private final ObjectMapper objectMapper = new ObjectMapper(); // 실제 JSON 검증 도구를 생성한다.

    private AdminSettingService adminSettingService; // 테스트 대상 서비스를 보관한다.

    @BeforeEach // 각 테스트 실행 전에 호출한다.
    void setUp() { // 테스트 대상 준비 메서드를 정의한다.
        adminSettingService = new AdminSettingService(appSettingRepository, auditLogRepository, objectMapper); // 목 저장소와 실제 ObjectMapper로 서비스를 생성한다.
    } // 테스트 대상 준비 메서드를 종료한다.

    @Test // 설정 목록 조회 정렬을 검증한다.
    void getSettingsReturnsAllSettingsSortedByKey() { // 설정 목록 정렬 테스트를 정의한다.
        when(appSettingRepository.findAll()).thenReturn(List.of(new AppSettingEntity("z", "{\"value\":2}", "뒤 설정", "admin"), new AppSettingEntity("a", "{\"value\":1}", "앞 설정", "admin"))); // 저장소가 정렬되지 않은 설정 목록을 반환하게 한다.
        List<AdminSettingResponse> result = adminSettingService.getSettings(); // 설정 목록을 조회한다.
        assertThat(result).extracting(AdminSettingResponse::key).containsExactly("a", "z"); // 키 기준 오름차순 정렬을 검증한다.
    } // 테스트 메서드를 종료한다.

    @Test // 존재하지 않는 설정 조회를 검증한다.
    void getSettingThrows404WhenMissing() { // 누락 설정 조회 테스트를 정의한다.
        when(appSettingRepository.findById("missing")).thenReturn(Optional.empty()); // 저장소가 빈 결과를 반환하게 한다.
        assertThatThrownBy(() -> adminSettingService.getSetting("missing")).isInstanceOf(CustomException.class).hasMessage("설정을 찾을 수 없습니다."); // 404 커스텀 예외를 검증한다.
    } // 테스트 메서드를 종료한다.

    @Test // 잘못된 JSON 수정 요청을 검증한다.
    void updateSettingRejectsInvalidJson() { // JSON 검증 실패 테스트를 정의한다.
        AdminSettingUpdateRequest request = new AdminSettingUpdateRequest("{invalid", "admin"); // 잘못된 JSON 요청을 생성한다.
        assertThatThrownBy(() -> adminSettingService.updateSetting("short.count", request)).isInstanceOf(CustomException.class).hasMessage("설정 JSON 형식이 올바르지 않습니다."); // 400 커스텀 예외를 검증한다.
    } // 테스트 메서드를 종료한다.

    @Test // 설정 수정 감사 로그 저장을 검증한다.
    void updateSettingSavesAuditLog() { // 감사 로그 저장 테스트를 정의한다.
        AppSettingEntity entity = new AppSettingEntity("short.count", "{\"value\":3}", "단기 추천 개수", "admin"); // 기존 설정 엔티티를 생성한다.
        when(appSettingRepository.findById("short.count")).thenReturn(Optional.of(entity)); // 저장소가 기존 설정을 반환하게 한다.
        when(appSettingRepository.save(any(AppSettingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0)); // 설정 저장소가 입력 엔티티를 반환하게 한다.
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0)); // 감사 로그 저장소가 입력 엔티티를 반환하게 한다.
        adminSettingService.updateSetting("short.count", new AdminSettingUpdateRequest("{\"value\":4}", "admin")); // 설정을 수정한다.
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class); // 감사 로그 인자 캡처를 준비한다.
        verify(auditLogRepository).save(captor.capture()); // 감사 로그 저장 호출을 검증하고 인자를 캡처한다.
        assertThat(captor.getValue().getAction()).isEqualTo("UPDATE_SETTING:short.count"); // 감사 로그 작업명을 검증한다.
        assertThat(captor.getValue().getBeforeJson()).isEqualTo("{\"value\":3}"); // 변경 전 JSON을 검증한다.
        assertThat(captor.getValue().getAfterJson()).isEqualTo("{\"value\":4}"); // 변경 후 JSON을 검증한다.
    } // 테스트 메서드를 종료한다.
} // 테스트 클래스를 종료한다.
