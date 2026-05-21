package com.parkdh.stockadvisor.application.ops; // 운영 애플리케이션 패키지를 선언한다.

import com.parkdh.stockadvisor.api.ops.dto.ComponentHealthResponse; // 개별 외부 연동 상태 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.ops.dto.ExternalHealthResponse; // 외부 연동 상태 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.config.CodexCliProperties; // Codex CLI 설정을 가져온다.
import com.parkdh.stockadvisor.config.DartProperties; // DART 설정을 가져온다.
import com.parkdh.stockadvisor.config.KisProperties; // KIS 설정을 가져온다.
import com.parkdh.stockadvisor.config.SecProperties; // SEC 설정을 가져온다.
import com.parkdh.stockadvisor.config.TelegramProperties; // Telegram 설정을 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 시장 공통 유틸을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class ExternalHealthService { // 외부 연동 상태 서비스를 정의한다.
    private final KisProperties kisProperties; // KIS 설정 의존성을 보관한다.
    private final TelegramProperties telegramProperties; // Telegram 설정 의존성을 보관한다.
    private final CodexCliProperties codexCliProperties; // Codex CLI 설정 의존성을 보관한다.
    private final DartProperties dartProperties; // DART 설정 의존성을 보관한다.
    private final SecProperties secProperties; // SEC 설정 의존성을 보관한다.

    public ExternalHealthResponse getExternalHealth() { // 외부 연동 상태를 조회한다.
        List<ComponentHealthResponse> components = List.of(
                configured("KIS", kisProperties.appKey(), "KIS API 키가 설정되어 있습니다.", "KIS API 키가 dev-placeholder입니다."),
                configured("Telegram", telegramProperties.botToken(), "Telegram Bot Token이 설정되어 있습니다.", "Telegram Bot Token이 dev-placeholder입니다."),
                configured("Codex CLI", codexCliProperties.command(), "Codex CLI 명령이 설정되어 있습니다.", "Codex CLI 명령이 dev-placeholder입니다."),
                configured("DART", dartProperties.apiKey(), "DART API 키가 설정되어 있습니다.", "DART API 키가 dev-placeholder입니다."),
                configured("SEC EDGAR", secProperties.userAgent(), "SEC User-Agent가 설정되어 있습니다.", "SEC User-Agent가 dev-placeholder입니다."),
                publicSource("RSS", "Google News/Yahoo Finance RSS 공개 피드를 사용합니다."),
                publicSource("FRED", "FRED 공개 CSV 소스를 사용하며 기본 지표는 별도 키가 필요 없습니다."),
                publicSource("Stooq", "공개 CSV 소스를 사용하며 별도 키가 필요 없습니다."),
                publicSource("KIND", "KRX KIND 상장법인 공개 목록을 사용하며 별도 키가 필요 없습니다.")
        ); // 구성 요소 상태 목록을 만든다.
        return new ExternalHealthResponse(LocalDateTime.now(), components); // 점검 시각과 상태 목록을 반환한다.
    } // 외부 연동 상태 조회를 종료한다.

    private ComponentHealthResponse configured(String name, String value, String readyMessage, String missingMessage) { // 설정 기반 외부 연동 상태를 만든다.
        if (value == null || value.isBlank() || MarketUtil.isDevPlaceholder(value)) { // 설정값이 없거나 개발용 placeholder인지 확인한다.
            return new ComponentHealthResponse(name, "MISSING_CONFIG", missingMessage); // 설정 누락 상태를 반환한다.
        } // 설정값 확인을 종료한다.
        return new ComponentHealthResponse(name, "READY", readyMessage); // 준비 완료 상태를 반환한다.
    } // 설정 기반 외부 연동 상태 생성을 종료한다.

    private ComponentHealthResponse publicSource(String name, String message) { // 공개 소스 상태를 만든다.
        return new ComponentHealthResponse(name, "PUBLIC_SOURCE", message); // 공개 소스 상태를 반환한다.
    } // 공개 소스 상태 생성을 종료한다.
} // 외부 연동 상태 서비스를 종료한다.
