package com.parkdh.stockadvisor.infrastructure.notification; // 알림 인프라 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.config.TelegramProperties; // Telegram 설정 속성을 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 공통 유틸 클래스를 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.util.Map; // Map 타입을 가져온다.

@Slf4j // SLF4J 로거를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class TelegramClient { // Telegram 알림 클라이언트를 정의한다.

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot"; // Telegram API 기본 URL을 정의한다.

    private final TelegramProperties telegramProperties; // Telegram 설정 속성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서를 보관한다.
    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.

    public TelegramClient(TelegramProperties telegramProperties, ObjectMapper objectMapper) { // 생성자로 의존성을 주입한다.
        this.telegramProperties = telegramProperties; // Telegram 설정 속성을 저장한다.
        this.objectMapper = objectMapper; // JSON 파서를 저장한다.
        this.httpClient = HttpClient.newBuilder() // HTTP 클라이언트를 생성한다.
                .connectTimeout(Duration.ofSeconds(10)) // 연결 타임아웃을 10초로 설정한다.
                .build(); // HTTP 클라이언트 생성을 완료한다.
    } // 생성자를 종료한다.

    public boolean sendMessage(String text) { // Telegram 봇으로 메시지를 전송한다.
        return sendMessageWithResult(text).sent(); // 상세 결과 중 성공 여부만 반환한다.
    } // 메시지 전송을 종료한다.

    public TelegramSendResult sendMessageWithResult(String text) { // Telegram 봇 전송 결과를 상세 정보와 함께 반환한다.
        if (MarketUtil.isDevPlaceholder(telegramProperties.botToken())) { // 개발 모드인지 확인한다.
            log.info("[Telegram 개발 모드] 메시지 출력: {}", text); // 개발 모드에서는 로그만 출력한다.
            return new TelegramSendResult(true, true, null, null); // 개발 모드에서는 성공으로 반환한다.
        } // 개발 모드 확인을 종료한다.
        if (isBlank(telegramProperties.botToken())) { // 봇 토큰 설정 여부를 확인한다.
            return new TelegramSendResult(false, false, null, "Telegram bot token is missing"); // 설정 누락 결과를 반환한다.
        } // 봇 토큰 확인을 종료한다.
        if (isBlank(telegramProperties.chatId())) { // 채팅 ID 설정 여부를 확인한다.
            return new TelegramSendResult(false, false, null, "Telegram chat id is missing"); // 설정 누락 결과를 반환한다.
        } // 채팅 ID 확인을 종료한다.
        try { // 전송 예외를 처리한다.
            String requestBody = buildMessageBody(text); // 요청 바디를 생성한다.
            String url = TELEGRAM_API_BASE + telegramProperties.botToken() + "/sendMessage"; // 메시지 전송 URL을 구성한다.
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                    .timeout(Duration.ofSeconds(15)) // 요청 타임아웃을 15초로 설정한다.
                    .header("Content-Type", "application/json") // Content-Type 헤더를 설정한다.
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)) // POST 요청 바디를 설정한다.
                    .build(); // HTTP 요청 생성을 완료한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                log.warn("Telegram 메시지 전송에 실패했습니다. status={}, body={}", response.statusCode(), response.body()); // 전송 실패 경고 로그를 출력한다.
                return new TelegramSendResult(false, false, response.statusCode(), "Telegram API returned HTTP " + response.statusCode() + ": " + truncate(response.body())); // 전송 실패 결과를 반환한다.
            } // 상태 코드 확인을 종료한다.
            return new TelegramSendResult(true, false, response.statusCode(), null); // 전송 성공 결과를 반환한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            log.warn("Telegram 메시지 전송 중 네트워크 오류가 발생했습니다. error={}", exception.getMessage()); // 네트워크 오류 경고 로그를 출력한다.
            return new TelegramSendResult(false, false, null, "Telegram network error: " + exception.getMessage()); // 네트워크 오류 결과를 반환한다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            log.warn("Telegram 메시지 전송이 중단되었습니다."); // 중단 경고 로그를 출력한다.
            return new TelegramSendResult(false, false, null, "Telegram send interrupted"); // 중단 결과를 반환한다.
        } // 예외 처리를 종료한다.
    } // 상세 메시지 전송을 종료한다.

    private String buildMessageBody(String text) throws IOException { // 메시지 요청 JSON 바디를 생성한다.
        Map<String, String> body = Map.of( // 요청 바디 Map을 생성한다.
                "chat_id", telegramProperties.chatId(), // 채팅 ID를 추가한다.
                "text", text, // 메시지 텍스트를 추가한다.
                "parse_mode", "HTML" // HTML 파싱 모드를 추가한다.
        ); // Map 생성을 종료한다.
        return objectMapper.writeValueAsString(body); // Map을 JSON 문자열로 변환한다.
    } // 요청 바디 생성을 종료한다.

    private boolean isBlank(String value) { // 문자열 공백 여부를 확인한다.
        return value == null || value.isBlank(); // null 또는 공백이면 true를 반환한다.
    } // 문자열 공백 확인을 종료한다.

    private String truncate(String value) { // 긴 응답 본문을 로그/DB 저장용으로 줄인다.
        if (value == null) { // 값이 없으면 빈 문자열을 반환한다.
            return ""; // 빈 문자열을 반환한다.
        } // null 확인을 종료한다.
        return value.length() <= 300 ? value : value.substring(0, 300); // 300자까지만 반환한다.
    } // 문자열 축약을 종료한다.

    public record TelegramSendResult(boolean sent, boolean devMode, Integer statusCode, String errorMessage) {
    } // Telegram 전송 상세 결과를 정의한다.
} // Telegram 클라이언트를 종료한다.
