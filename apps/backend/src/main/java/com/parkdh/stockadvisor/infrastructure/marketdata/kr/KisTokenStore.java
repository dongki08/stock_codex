package com.parkdh.stockadvisor.infrastructure.marketdata.kr; // 한국 시장 데이터 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드 타입을 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.config.KisProperties; // KIS 설정 속성을 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 공통 유틸 클래스를 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.time.Instant; // 시간 지점 타입을 가져온다.
import java.util.concurrent.atomic.AtomicReference; // 원자적 참조 타입을 가져온다.

@Slf4j // SLF4J 로거를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class KisTokenStore { // KIS OAuth 토큰 인메모리 캐시를 정의한다.

    private static final String DEV_TOKEN = "dev-token"; // 개발 모드 더미 토큰을 정의한다.
    private static final long EXPIRY_BUFFER_SECONDS = 600L; // 만료 10분 전 재발급 버퍼를 정의한다.

    private final KisProperties kisProperties; // KIS 설정 속성을 보관한다.
    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서를 보관한다.
    private final AtomicReference<String> tokenRef = new AtomicReference<>(null); // 현재 액세스 토큰을 보관한다.
    private volatile Instant tokenExpiresAt = Instant.EPOCH; // 토큰 만료 시각을 보관한다.

    public KisTokenStore(KisProperties kisProperties, ObjectMapper objectMapper) { // 생성자로 의존성을 주입한다.
        this.kisProperties = kisProperties; // KIS 설정 속성을 저장한다.
        this.objectMapper = objectMapper; // JSON 파서를 저장한다.
        this.httpClient = HttpClient.newBuilder() // HTTP 클라이언트를 생성한다.
                .connectTimeout(Duration.ofSeconds(10)) // 연결 타임아웃을 10초로 설정한다.
                .build(); // HTTP 클라이언트 생성을 완료한다.
    } // 생성자를 종료한다.

    public String getToken() { // 유효한 액세스 토큰을 반환한다.
        if (MarketUtil.isDevPlaceholder(kisProperties.appKey())) { // 개발 모드인지 확인한다.
            return DEV_TOKEN; // 개발 모드에서는 더미 토큰을 반환한다.
        } // 개발 모드 확인을 종료한다.
        if (isTokenValid()) { // 현재 토큰이 유효한지 확인한다.
            return tokenRef.get(); // 유효한 토큰을 반환한다.
        } // 토큰 유효성 확인을 종료한다.
        return refreshToken(); // 토큰을 재발급한다.
    } // 토큰 반환을 종료한다.

    private boolean isTokenValid() { // 토큰이 유효한지 확인한다.
        String token = tokenRef.get(); // 현재 토큰을 가져온다.
        if (token == null) { // 토큰이 없으면 무효로 판단한다.
            return false; // 토큰이 없으므로 false를 반환한다.
        } // 토큰 null 확인을 종료한다.
        return Instant.now().isBefore(tokenExpiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS)); // 만료 버퍼 전인지 확인한다.
    } // 토큰 유효성 확인을 종료한다.

    private synchronized String refreshToken() { // 토큰을 동기화하여 재발급한다.
        if (isTokenValid()) { // 다른 스레드가 이미 갱신했는지 재확인한다.
            return tokenRef.get(); // 이미 갱신된 토큰을 반환한다.
        } // 재확인을 종료한다.
        log.info("KIS 액세스 토큰을 재발급합니다."); // 토큰 재발급 시작 로그를 출력한다.
        String requestBody = buildTokenRequestBody(); // 토큰 요청 바디를 생성한다.
        String url = kisProperties.baseUrl() + "/oauth2/tokenP"; // 토큰 발급 URL을 구성한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                .timeout(Duration.ofSeconds(15)) // 요청 타임아웃을 15초로 설정한다.
                .header("Content-Type", "application/json") // Content-Type 헤더를 설정한다.
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) // POST 요청 바디를 설정한다.
                .build(); // HTTP 요청 생성을 완료한다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("KIS 토큰 발급에 실패했습니다. status=" + response.statusCode(), 502); // 외부 API 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            JsonNode root = objectMapper.readTree(response.body()); // 응답 JSON을 파싱한다.
            String accessToken = root.path("access_token").asText(); // 액세스 토큰을 추출한다.
            long expiresIn = root.path("expires_in").asLong(86400L); // 만료 시간을 추출한다.
            tokenRef.set(accessToken); // 새 토큰을 저장한다.
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn); // 만료 시각을 갱신한다.
            log.info("KIS 액세스 토큰 재발급이 완료되었습니다. expiresIn={}초", expiresIn); // 토큰 재발급 완료 로그를 출력한다.
            return accessToken; // 새 토큰을 반환한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("KIS 토큰 발급 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("KIS 토큰 발급이 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 토큰 재발급을 종료한다.

    private String buildTokenRequestBody() { // 토큰 요청 JSON 바디를 생성한다.
        return "{" // JSON 객체를 시작한다.
                + "\"grant_type\":\"client_credentials\"," // grant_type 필드를 추가한다.
                + "\"appkey\":\"" + kisProperties.appKey() + "\"," // appkey 필드를 추가한다.
                + "\"appsecret\":\"" + kisProperties.appSecret() + "\"" // appsecret 필드를 추가한다.
                + "}"; // JSON 객체를 종료한다.
    } // 요청 바디 생성을 종료한다.
} // KIS 토큰 저장소를 종료한다.
