package com.parkdh.stockadvisor.infrastructure.marketdata.us; // 미국 시장 데이터 클라이언트 패키지를 선언한다.

import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.util.Arrays; // 배열 유틸을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@Component // 스프링 컴포넌트로 등록한다.
public class NasdaqTraderSymbolClient { // NASDAQ Trader 심볼 클라이언트를 정의한다.
    private static final String NASDAQ_LISTED_URL = "https://www.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt"; // NASDAQ 상장 심볼 파일 URL을 정의한다.
    private static final String OTHER_LISTED_URL = "https://www.nasdaqtrader.com/dynamic/SymDir/otherlisted.txt"; // NYSE 등 기타 상장 심볼 파일 URL을 정의한다.
    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.

    public NasdaqTraderSymbolClient() { // 기본 생성자를 정의한다.
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(); // 타임아웃이 있는 HTTP 클라이언트를 만든다.
    } // 기본 생성자를 종료한다.

    public List<UsSymbol> fetchNasdaqListed() { // NASDAQ 상장 심볼을 조회한다.
        String body = fetchText(NASDAQ_LISTED_URL); // NASDAQ 상장 심볼 파일을 가져온다.
        return Arrays.stream(body.split("\\R"))
                .skip(1) // 헤더 행을 제외한다.
                .filter(line -> !line.startsWith("File Creation Time")) // 꼬리 메타데이터를 제외한다.
                .map(this::parseNasdaqLine) // NASDAQ 행을 파싱한다.
                .filter(UsSymbol::tradable) // 거래 가능 후보만 남긴다.
                .toList(); // 목록으로 수집한다.
    } // NASDAQ 상장 심볼 조회를 종료한다.

    public List<UsSymbol> fetchNyseListed() { // NYSE 상장 심볼을 조회한다.
        String body = fetchText(OTHER_LISTED_URL); // 기타 상장 심볼 파일을 가져온다.
        return Arrays.stream(body.split("\\R"))
                .skip(1) // 헤더 행을 제외한다.
                .filter(line -> !line.startsWith("File Creation Time")) // 꼬리 메타데이터를 제외한다.
                .map(this::parseOtherLine) // 기타 상장 행을 파싱한다.
                .filter(symbol -> "N".equals(symbol.exchangeCode())) // NYSE 상장 종목만 남긴다.
                .filter(UsSymbol::tradable) // 거래 가능 후보만 남긴다.
                .toList(); // 목록으로 수집한다.
    } // NYSE 상장 심볼 조회를 종료한다.

    private String fetchText(String url) { // 텍스트 파일을 가져온다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(); // GET 요청을 만든다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 문자열 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("미국 심볼 파일 조회에 실패했습니다. status=" + response.statusCode(), 502); // 외부 조회 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return response.body(); // 응답 본문을 반환한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("미국 심볼 파일 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("미국 심볼 파일 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 텍스트 파일 조회를 종료한다.

    private UsSymbol parseNasdaqLine(String line) { // NASDAQ 파일 행을 파싱한다.
        String[] parts = line.split("\\|", -1); // 파이프 구분자로 행을 나눈다.
        String symbol = parts.length > 0 ? parts[0].trim() : ""; // 심볼을 추출한다.
        String name = parts.length > 1 ? parts[1].trim() : symbol; // 이름을 추출한다.
        boolean testIssue = parts.length > 3 && "Y".equals(parts[3]); // 테스트 종목 여부를 확인한다.
        boolean etf = parts.length > 6 && "Y".equals(parts[6]); // ETF 여부를 확인한다.
        return new UsSymbol(symbol, "NASDAQ", "Q", name, !symbol.isBlank() && !testIssue && !etf); // NASDAQ 심볼을 반환한다.
    } // NASDAQ 행 파싱을 종료한다.

    private UsSymbol parseOtherLine(String line) { // 기타 상장 파일 행을 파싱한다.
        String[] parts = line.split("\\|", -1); // 파이프 구분자로 행을 나눈다.
        String symbol = parts.length > 0 ? parts[0].trim() : ""; // ACT 심볼을 추출한다.
        String name = parts.length > 2 ? parts[2].trim() : symbol; // 이름을 추출한다.
        String exchange = parts.length > 4 ? parts[4].trim() : ""; // 거래소 코드를 추출한다.
        boolean etf = parts.length > 5 && "Y".equals(parts[5]); // ETF 여부를 확인한다.
        boolean testIssue = parts.length > 6 && "Y".equals(parts[6]); // 테스트 종목 여부를 확인한다.
        return new UsSymbol(symbol, exchangeToMarket(exchange), exchange, name, !symbol.isBlank() && !testIssue && !etf); // 기타 상장 심볼을 반환한다.
    } // 기타 상장 행 파싱을 종료한다.

    private String exchangeToMarket(String exchange) { // 거래소 코드를 시장명으로 변환한다.
        if ("N".equals(exchange)) { // NYSE 코드인지 확인한다.
            return "NYSE"; // NYSE를 반환한다.
        } // NYSE 코드 확인을 종료한다.
        return exchange; // 알 수 없는 거래소 코드는 그대로 반환한다.
    } // 거래소 코드 변환을 종료한다.

    public record UsSymbol(String ticker, String market, String exchangeCode, String name, boolean tradable) { // 미국 심볼 정보를 정의한다.
    } // 미국 심볼 정보를 종료한다.
} // NASDAQ Trader 심볼 클라이언트를 종료한다.
