package com.parkdh.stockadvisor.application.admin; // 관리자 서비스 패키지를 선언한다.

import com.fasterxml.jackson.core.JsonProcessingException; // JSON 처리 예외를 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱 도구를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingResponse; // 설정 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingUpdateRequest; // 설정 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AuditLogResponse; // 감사 로그 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.audit.AuditLogEntity; // 감사 로그 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.audit.AuditLogRepository; // 감사 로그 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 설정 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@Service // 스프링 서비스 빈으로 등록한다.
public class AdminSettingService { // 관리자 설정 서비스 클래스를 정의한다.
    private final AppSettingRepository appSettingRepository; // 설정 저장소 의존성을 보관한다.
    private final AuditLogRepository auditLogRepository; // 감사 로그 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 검증 도구 의존성을 보관한다.

    public AdminSettingService(AppSettingRepository appSettingRepository, AuditLogRepository auditLogRepository, ObjectMapper objectMapper) { // 생성자 주입을 정의한다.
        this.appSettingRepository = appSettingRepository; // 설정 저장소를 저장한다.
        this.auditLogRepository = auditLogRepository; // 감사 로그 저장소를 저장한다.
        this.objectMapper = objectMapper; // JSON 검증 도구를 저장한다.
    } // 생성자를 종료한다.

    public ResultDto<?> getSettings() { // 전체 설정 목록을 조회한다.
        List<AdminSettingResponse> settings = appSettingRepository.findAll().stream().sorted(Comparator.comparing(AppSettingEntity::getSettingKey)).map(this::toSettingResponse).toList(); // 설정 엔티티를 키 기준으로 정렬해 DTO로 변환한다.
        return ResultDto.success(settings); // 설정 목록을 성공 응답으로 반환한다.
    } // 전체 설정 조회 메서드를 종료한다.

    public ResultDto<?> getSetting(String key) { // 단일 설정을 조회한다.
        AppSettingEntity setting = appSettingRepository.findById(key).orElseThrow(() -> new CustomException("설정을 찾을 수 없습니다.", 404)); // 설정이 없으면 404 예외를 던진다.
        return ResultDto.success(toSettingResponse(setting)); // 설정 DTO를 성공 응답으로 반환한다.
    } // 단일 설정 조회 메서드를 종료한다.

    @Transactional // 설정 수정과 감사 로그 저장을 하나의 쓰기 트랜잭션으로 처리한다.
    public ResultDto<?> updateSetting(String key, AdminSettingUpdateRequest request) { // 설정 값을 수정한다.
        validateJson(request.valueJson()); // 요청 JSON 문자열의 형식을 검증한다.
        AppSettingEntity setting = appSettingRepository.findById(key).orElseThrow(() -> new CustomException("설정을 찾을 수 없습니다.", 404)); // 설정이 없으면 404 예외를 던진다.
        String beforeJson = setting.getValueJson(); // 변경 전 JSON 값을 보관한다.
        setting.updateValue(request.valueJson(), request.actor()); // 설정 값과 수정자를 갱신한다.
        appSettingRepository.save(setting); // 변경된 설정을 저장한다.
        auditLogRepository.save(new AuditLogEntity(request.actor(), "UPDATE_SETTING:" + key, beforeJson, request.valueJson())); // 변경 이력을 감사 로그로 저장한다.
        return ResultDto.success(toSettingResponse(setting)); // 수정된 설정 DTO를 성공 응답으로 반환한다.
    } // 설정 수정 메서드를 종료한다.

    @Transactional // 기본 설정 저장과 감사 로그 저장을 하나의 쓰기 트랜잭션으로 처리한다.
    public ResultDto<?> resetSettings() { // 기본 설정 값을 저장한다.
        List<AppSettingEntity> defaults = List.of( // 기본 설정 목록을 생성한다.
                new AppSettingEntity("recommendation.short.count", "{\"value\":3}", "단기 추천 개수", "system"), // 단기 추천 개수 기본값을 정의한다.
                new AppSettingEntity("recommendation.long.count", "{\"value\":3}", "장기 추천 개수", "system"), // 장기 추천 개수 기본값을 정의한다.
                new AppSettingEntity("backtest.period.years", "{\"value\":5}", "백테스트 기본 기간", "system"), // 백테스트 기간 기본값을 정의한다.
                new AppSettingEntity("codex.daily.callLimit", "{\"value\":200}", "Codex 일 호출 한도", "system"), // Codex 호출 한도 기본값을 정의한다.
                new AppSettingEntity("autoresearch.enabled", "{\"value\":true}", "AutoResearch 활성 여부", "system") // AutoResearch 활성 기본값을 정의한다.
        ); // 기본 설정 목록 생성을 종료한다.
        appSettingRepository.saveAll(defaults); // 기본 설정 목록을 저장한다.
        auditLogRepository.save(new AuditLogEntity("system", "RESET_SETTINGS", "{}", "{\"count\":" + defaults.size() + "}")); // 기본 설정 초기화 감사 로그를 저장한다.
        return ResultDto.success(defaults.stream().map(this::toSettingResponse).toList()); // 저장된 기본 설정 목록을 성공 응답으로 반환한다.
    } // 기본 설정 저장 메서드를 종료한다.

    public ResultDto<?> getAuditLogs() { // 감사 로그 목록을 조회한다.
        List<AuditLogResponse> logs = auditLogRepository.findAll().stream().sorted(Comparator.comparing(AuditLogEntity::getId).reversed()).map(this::toAuditLogResponse).toList(); // 감사 로그를 ID 역순으로 정렬해 DTO로 변환한다.
        return ResultDto.success(logs); // 감사 로그 목록을 성공 응답으로 반환한다.
    } // 감사 로그 조회 메서드를 종료한다.

    private void validateJson(String valueJson) { // JSON 문자열 형식을 검증한다.
        try { // JSON 파싱 예외를 처리하기 위해 시도 블록을 시작한다.
            objectMapper.readTree(valueJson); // JSON 문자열을 파싱해 형식 오류를 확인한다.
        } catch (JsonProcessingException exception) { // JSON 파싱 실패를 잡는다.
            throw new CustomException("설정 JSON 형식이 올바르지 않습니다.", 400); // 클라이언트 오류 예외를 던진다.
        } // 예외 처리 블록을 종료한다.
    } // JSON 검증 메서드를 종료한다.

    private AdminSettingResponse toSettingResponse(AppSettingEntity entity) { // 설정 엔티티를 응답 DTO로 변환한다.
        return new AdminSettingResponse(entity.getSettingKey(), entity.getValueJson(), entity.getDescription(), entity.getUpdatedBy()); // 설정 응답 DTO를 생성한다.
    } // 설정 DTO 변환 메서드를 종료한다.

    private AuditLogResponse toAuditLogResponse(AuditLogEntity entity) { // 감사 로그 엔티티를 응답 DTO로 변환한다.
        return new AuditLogResponse(entity.getId(), entity.getActor(), entity.getAction(), entity.getBeforeJson(), entity.getAfterJson()); // 감사 로그 응답 DTO를 생성한다.
    } // 감사 로그 DTO 변환 메서드를 종료한다.
} // 관리자 설정 서비스 클래스를 종료한다.
