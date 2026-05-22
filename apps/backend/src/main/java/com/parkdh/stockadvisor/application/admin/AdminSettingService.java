package com.parkdh.stockadvisor.application.admin; // 관리자 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱 도구를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingResponse; // 설정 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AdminSettingUpdateRequest; // 설정 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.admin.dto.AuditLogResponse; // 감사 로그 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.audit.AuditLogEntity; // 감사 로그 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.JsonValidationUtil; // JSON 검증 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.audit.AuditLogRepository; // 감사 로그 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 설정 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.security.core.Authentication; // 현재 인증 정보를 가져온다.
import org.springframework.security.core.context.SecurityContextHolder; // 보안 컨텍스트 접근 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.util.ArrayList; // 배열 기반 목록 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Set; // 집합 타입을 가져온다.
import java.util.stream.Collectors; // 스트림 수집 도구를 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class AdminSettingService { // 관리자 설정 서비스 클래스를 정의한다.
    private final AppSettingRepository appSettingRepository; // 설정 저장소 의존성을 보관한다.
    private final AuditLogRepository auditLogRepository; // 감사 로그 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 검증 도구 의존성을 보관한다.

    public List<AdminSettingResponse> getSettings() { // 전체 설정 목록을 조회한다.
        return appSettingRepository.findAll().stream().sorted(Comparator.comparing(AppSettingEntity::getSettingKey)).map(this::toSettingResponse).toList(); // 설정 엔티티를 키 기준으로 정렬해 DTO로 변환한다.
    } // 전체 설정 조회 메서드를 종료한다.

    public AdminSettingResponse getSetting(String key) { // 단일 설정을 조회한다.
        AppSettingEntity setting = appSettingRepository.findById(key).orElseThrow(() -> new CustomException("설정을 찾을 수 없습니다.", 404)); // 설정이 없으면 404 예외를 던진다.
        return toSettingResponse(setting); // 설정 DTO를 반환한다.
    } // 단일 설정 조회 메서드를 종료한다.

    @Transactional // 설정 수정과 감사 로그 저장을 하나의 쓰기 트랜잭션으로 처리한다.
    public AdminSettingResponse updateSetting(String key, AdminSettingUpdateRequest request) { // 설정 값을 수정한다.
        validateJson(request.valueJson()); // 요청 JSON 문자열의 형식을 검증한다.
        AppSettingEntity setting = appSettingRepository.findById(key).orElseThrow(() -> new CustomException("설정을 찾을 수 없습니다.", 404)); // 설정이 없으면 404 예외를 던진다.
        String beforeJson = setting.getValueJson(); // 변경 전 JSON 값을 보관한다.
        String actor = resolveActor(request.actor()); // 인증된 사용자명을 우선 사용해 감사 주체를 결정한다.
        setting.updateValue(request.valueJson(), actor); // 설정 값과 수정자를 갱신한다.
        appSettingRepository.save(setting); // 변경된 설정을 저장한다.
        auditLogRepository.save(new AuditLogEntity(actor, "UPDATE_SETTING:" + key, beforeJson, request.valueJson())); // 변경 이력을 감사 로그로 저장한다.
        return toSettingResponse(setting); // 수정된 설정 DTO를 반환한다.
    } // 설정 수정 메서드를 종료한다.

    @Transactional // 기본 설정 저장과 감사 로그 저장을 하나의 쓰기 트랜잭션으로 처리한다.
    public List<AdminSettingResponse> resetSettings() { // 기본 설정 값을 저장한다.
        List<AppSettingEntity> defaults = List.of( // 기본 설정 목록을 생성한다.
                new AppSettingEntity("recommendation.short.count", "{\"value\":3,\"min\":1,\"max\":10}", "단기 추천 개수", "system"), // 단기 추천 개수 기본값을 정의한다.
                new AppSettingEntity("recommendation.long.count", "{\"value\":3,\"min\":1,\"max\":10}", "장기 추천 개수", "system"), // 장기 추천 개수 기본값을 정의한다.
                new AppSettingEntity("recommendation.market.enabled", "{\"kr\":true,\"us\":true}", "시장 활성화", "system"), // 시장 활성화 기본값을 정의한다.
                new AppSettingEntity("recommendation.marketcap.kr.min", "{\"value\":300000000000,\"currency\":\"KRW\"}", "한국 시총 하한", "system"), // 한국 시총 하한 기본값을 정의한다.
                new AppSettingEntity("recommendation.marketcap.us.min", "{\"value\":1000000000,\"currency\":\"USD\"}", "미국 시총 하한", "system"), // 미국 시총 하한 기본값을 정의한다.
                new AppSettingEntity("recommendation.turnover.kr.min", "{\"value\":10000000000,\"currency\":\"KRW\"}", "한국 거래대금 하한", "system"), // 한국 거래대금 하한 기본값을 정의한다.
                new AppSettingEntity("recommendation.turnover.us.min", "{\"value\":10000000,\"currency\":\"USD\"}", "미국 거래대금 하한", "system"), // 미국 거래대금 하한 기본값을 정의한다.
                new AppSettingEntity("recommendation.feature.minTotalScore", "{\"value\":0,\"min\":0,\"max\":100}", "추천 후보 최소 feature 점수", "system"), // 추천 후보 최소 점수 기본값을 정의한다.
                new AppSettingEntity("recommendation.feature.minDataQualityScore", "{\"value\":0,\"min\":0,\"max\":100}", "추천 후보 최소 데이터 품질 점수", "system"), // 추천 후보 최소 데이터 품질 점수 기본값을 정의한다.
                new AppSettingEntity("recommendation.scoring.weights", "{\"value\":{\"liquidity\":0.20,\"price\":0.10,\"technical\":0.30,\"context\":0.15,\"fundamental\":0.10,\"dataQuality\":0.15},\"technical\":{\"ma\":0.40,\"rsi\":0.35,\"volume\":0.25},\"context\":{\"news\":0.40,\"disclosure\":0.18,\"macro\":0.25,\"fundamental\":0.17}}", "추천 feature 점수 가중치", "system"), // 추천 feature 점수 가중치 기본값을 정의한다.
                new AppSettingEntity("recommendation.excluded.sectors", "{\"value\":[]}", "제외 섹터", "system"), // 제외 섹터 기본값을 정의한다.
                new AppSettingEntity("recommendation.watchlist", "{\"include\":[],\"exclude\":[]}", "보유 및 제외 종목", "system"), // 보유 및 제외 종목 기본값을 정의한다.
                new AppSettingEntity("notification.krx.preopen.offsetMinutes", "{\"value\":-30,\"displayTime\":\"08:30\"}", "KRX 프리오픈 알림 오프셋", "system"), // KRX 프리오픈 알림 기본값을 정의한다.
                new AppSettingEntity("notification.us.preopen.offsetMinutes", "{\"value\":-30,\"dstTime\":\"22:00\",\"standardTime\":\"23:00\"}", "US 프리오픈 알림 오프셋", "system"), // US 프리오픈 알림 기본값을 정의한다.
                new AppSettingEntity("notification.us.close.offsetMinutes", "{\"value\":30,\"dstTime\":\"05:30\",\"standardTime\":\"06:30\"}", "US 마감 결산 알림 오프셋", "system"), // US 마감 결산 알림 기본값을 정의한다.
                new AppSettingEntity("notification.holiday.enabled", "{\"value\":true}", "휴장일 알림 발송 여부", "system"), // 휴장일 알림 기본값을 정의한다.
                new AppSettingEntity("notification.holiday.kr.closedDates", "{\"value\":[]}", "한국 휴장일 날짜 목록", "system"), // 한국 휴장일 목록 기본값을 정의한다.
                new AppSettingEntity("notification.holiday.us.closedDates", "{\"value\":[]}", "미국 휴장일 날짜 목록", "system"), // 미국 휴장일 목록 기본값을 정의한다.
                new AppSettingEntity("notification.channel.priority", "{\"value\":[\"TELEGRAM\",\"KAKAO\"]}", "알림 채널 우선순위", "system"), // 알림 채널 우선순위 기본값을 정의한다.
                new AppSettingEntity("collection.enabled", "{\"value\":true}", "뉴스/공시/매크로 자동 수집 활성 여부", "system"), // 수집 스케줄 활성 기본값을 정의한다.
                new AppSettingEntity("collection.news.tickersPerMarket", "{\"value\":5}", "시장별 뉴스 수집 후보 수", "system"), // 시장별 뉴스 후보 수 기본값을 정의한다.
                new AppSettingEntity("collection.news.limitPerTicker", "{\"value\":5}", "종목별 뉴스 수집 개수", "system"), // 종목별 뉴스 수집 개수 기본값을 정의한다.
                new AppSettingEntity("collection.disclosure.limit", "{\"value\":20}", "시장별 공시 수집 개수", "system"), // 공시 수집 개수 기본값을 정의한다.
                new AppSettingEntity("collection.macro.limitPerSeries", "{\"value\":5}", "매크로 지표별 수집 개수", "system"), // 매크로 수집 개수 기본값을 정의한다.
                new AppSettingEntity("collection.fundamental.tickersPerMarket", "{\"value\":3}", "시장별 펀더멘털 수집 후보 수", "system"), // 펀더멘털 수집 후보 수 기본값을 정의한다.
                new AppSettingEntity("ops.health.priceDaily.maxAgeDays", "{\"value\":3}", "일봉 데이터 헬스체크 허용 지연일", "system"), // 일봉 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("ops.health.priceIntraday.maxAgeMinutes", "{\"value\":120}", "장중 데이터 헬스체크 허용 지연분", "system"), // 장중 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("ops.health.news.maxAgeHours", "{\"value\":48}", "뉴스 데이터 헬스체크 허용 지연시간", "system"), // 뉴스 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("ops.health.disclosure.maxAgeDays", "{\"value\":14}", "공시 데이터 헬스체크 허용 지연일", "system"), // 공시 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("ops.health.macro.maxAgeDays", "{\"value\":14}", "매크로 데이터 헬스체크 허용 지연일", "system"), // 매크로 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("ops.health.fundamental.maxAgeDays", "{\"value\":120}", "펀더멘털 데이터 헬스체크 허용 지연일", "system"), // 펀더멘털 최신성 기준 기본값을 정의한다.
                new AppSettingEntity("exit.polling.intervalMinutes", "{\"value\":5,\"options\":[1,3,5,10,30]}", "손절 모니터링 폴링 주기", "system"), // 손절 모니터링 폴링 기본값을 정의한다.
                new AppSettingEntity("exit.riskBand.percent", "{\"value\":2.0}", "손절 위험 구간 폭", "system"), // 손절 위험 구간 기본값을 정의한다.
                new AppSettingEntity("exit.codex.confirmLimitPerTickerDaily", "{\"value\":3}", "종목당 일 Codex 컨펌 한도", "system"), // 종목당 Codex 컨펌 한도 기본값을 정의한다.
                new AppSettingEntity("exit.codex.confirmLimitPerRun", "{\"value\":5}", "스케줄 1회당 Codex 컨펌 한도", "system"), // 스케줄 실행당 Codex 컨펌 한도 기본값을 정의한다.
                new AppSettingEntity("exit.codex.confirmCooldownMinutes", "{\"value\":60}", "추천별 Codex 컨펌 쿨다운", "system"), // 추천별 Codex 컨펌 쿨다운 기본값을 정의한다.
                new AppSettingEntity("exit.intraday.enabled", "{\"value\":true}", "장중 즉시 손절 알림", "system"), // 장중 즉시 손절 알림 기본값을 정의한다.
                new AppSettingEntity("exit.extendedHours.enabled", "{\"value\":false}", "시간외 모니터링", "system"), // 시간외 모니터링 기본값을 정의한다.
                new AppSettingEntity("backtest.period.years", "{\"value\":5,\"options\":[1,3,5,10]}", "백테스트 기본 기간", "system"), // 백테스트 기간 기본값을 정의한다.
                new AppSettingEntity("backtest.walkForward.days", "{\"value\":180}", "Walk-forward 윈도우", "system"), // Walk-forward 윈도우 기본값을 정의한다.
                new AppSettingEntity("backtest.slippage.percent", "{\"value\":0.05}", "슬리피지 가정", "system"), // 슬리피지 기본값을 정의한다.
                new AppSettingEntity("backtest.cost.kr", "{\"taxPercent\":0.18,\"feePercent\":0.015}", "한국 거래비용 가정", "system"), // 한국 거래비용 기본값을 정의한다.
                new AppSettingEntity("backtest.cost.us", "{\"secFeeEnabled\":true,\"fxSpreadPercent\":0.5}", "미국 거래비용 가정", "system"), // 미국 거래비용 기본값을 정의한다.
                new AppSettingEntity("autoresearch.enabled", "{\"value\":true}", "AutoResearch 활성 여부", "system"), // AutoResearch 활성 기본값을 정의한다.
                new AppSettingEntity("autoresearch.targetIterations", "{\"value\":80}", "야간 실험 목표 횟수", "system"), // 야간 실험 목표 횟수 기본값을 정의한다.
                new AppSettingEntity("autoresearch.rollbackValidationDays", "{\"value\":7}", "Champion 롤백 검증 기간", "system"), // Champion 롤백 검증 기간 기본값을 정의한다.
                new AppSettingEntity("codex.daily.callLimit", "{\"value\":200}", "Codex 일 호출 한도", "system"), // Codex 호출 한도 기본값을 정의한다.
                new AppSettingEntity("codex.daily.budgetUsd", "{\"value\":0}", "Codex 일 예산", "system"), // Codex 예산 기본값을 정의한다.
                new AppSettingEntity("codex.estimatedUsdPer1kChars", "{\"value\":0.002}", "Codex 문자 1천자당 예상 비용", "system"), // Codex 예상 비용 단가 기본값을 정의한다.
                new AppSettingEntity("codex.estimatedResponseChars", "{\"value\":4000}", "Codex 응답 예상 문자 수", "system"), // Codex 응답 예상 길이 기본값을 정의한다.
                new AppSettingEntity("codex.profile", "{\"value\":\"stock-advisor\"}", "Codex profile", "system"), // Codex profile 기본값을 정의한다.
                new AppSettingEntity("operation.dbBackup.enabled", "{\"value\":true}", "DB 백업 스케줄 활성 여부", "system"), // DB 백업 활성 기본값을 정의한다.
                new AppSettingEntity("operation.dbBackup.cron", "{\"value\":\"0 0 3 * * *\"}", "DB 백업 스케줄", "system") // DB 백업 스케줄 기본값을 정의한다.
        ); // 기본 설정 목록 생성을 종료한다.
        Set<String> existingKeys = appSettingRepository.findAll().stream() // 기존 설정 키를 한 번에 조회한다.
                .map(AppSettingEntity::getSettingKey)
                .collect(Collectors.toSet()); // 기존 설정 키 집합을 생성한다.
        List<AppSettingEntity> toSave = defaults.stream()
                .filter(d -> !existingKeys.contains(d.getSettingKey())) // 존재하지 않는 키만 필터링한다.
                .toList(); // 신규 저장 대상 목록을 수집한다.
        List<AppSettingEntity> newlySaved = appSettingRepository.saveAll(toSave); // 신규 설정을 한 번에 저장한다.
        List<AppSettingEntity> existing = appSettingRepository.findAll().stream()
                .filter(e -> existingKeys.contains(e.getSettingKey())) // 기존 설정 엔티티를 필터링한다.
                .toList(); // 기존 설정 목록을 수집한다.
        List<AppSettingEntity> allResult = new ArrayList<>(newlySaved); // 신규 저장 결과를 포함한 전체 목록을 구성한다.
        allResult.addAll(existing); // 기존 설정도 응답에 포함한다.
        auditLogRepository.save(new AuditLogEntity("system", "RESET_SETTINGS", "{}", "{\"count\":" + allResult.size() + "}")); // 기본 설정 초기화 감사 로그를 저장한다.
        return allResult.stream().map(this::toSettingResponse).toList(); // 전체 기본 설정 목록을 반환한다.
    } // 기본 설정 저장 메서드를 종료한다.

    public List<AuditLogResponse> getAuditLogs() { // 감사 로그 목록을 조회한다.
        return auditLogRepository.findTop50ByOrderByIdDesc().stream().map(this::toAuditLogResponse).toList(); // 최근 감사 로그 50건을 DB에서 정렬해 DTO로 변환한다.
    } // 감사 로그 조회 메서드를 종료한다.

    private void validateJson(String valueJson) { // JSON 문자열 형식을 검증한다.
        JsonValidationUtil.validate(objectMapper, valueJson, "설정 JSON"); // 공통 JSON 검증 유틸로 형식을 확인한다.
    } // JSON 검증 메서드를 종료한다.

    private String resolveActor(String requestActor) { // 감사 로그 주체를 결정한다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 현재 인증 정보를 조회한다.
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) { // 인증된 사용자가 있으면 요청 바디보다 우선한다.
            return authentication.getName(); // 인증 사용자명을 반환한다.
        } // 인증 사용자 확인을 종료한다.
        return requestActor == null || requestActor.isBlank() ? "unknown" : requestActor; // 인증 정보가 없으면 기존 요청 값을 호환용으로 사용한다.
    } // 감사 로그 주체 결정 메서드를 종료한다.

    private AdminSettingResponse toSettingResponse(AppSettingEntity entity) { // 설정 엔티티를 응답 DTO로 변환한다.
        return new AdminSettingResponse(entity.getSettingKey(), entity.getValueJson(), entity.getDescription(), entity.getUpdatedBy()); // 설정 응답 DTO를 생성한다.
    } // 설정 DTO 변환 메서드를 종료한다.

    private AuditLogResponse toAuditLogResponse(AuditLogEntity entity) { // 감사 로그 엔티티를 응답 DTO로 변환한다.
        return new AuditLogResponse(entity.getId(), entity.getActor(), entity.getAction(), entity.getBeforeJson(), entity.getAfterJson()); // 감사 로그 응답 DTO를 생성한다.
    } // 감사 로그 DTO 변환 메서드를 종료한다.
} // 관리자 설정 서비스 클래스를 종료한다.
