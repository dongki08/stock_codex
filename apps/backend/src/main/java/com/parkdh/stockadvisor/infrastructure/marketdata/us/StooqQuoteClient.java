package com.parkdh.stockadvisor.infrastructure.marketdata.us; // 미국 시장 데이터 클라이언트 패키지를 선언한다.

import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.URLEncoder; // URL 인코더를 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.nio.charset.StandardCharsets; // 문자셋을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.format.DateTimeFormatter; // 날짜 포맷 도구를 가져온다.
import java.util.Arrays; // 배열 유틸을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Optional; // 선택 값 타입을 가져온다.

@Component // 스프링 컴포넌트로 등록한다.
public class StooqQuoteClient { // Stooq 시세 클라이언트를 정의한다.
    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.

    public StooqQuoteClient() { // 기본 생성자를 정의한다.
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(); // 타임아웃이 있는 HTTP 클라이언트를 만든다.
    } // 기본 생성자를 종료한다.

    public Optional<StooqQuote> fetchQuote(String ticker) { // 단일 미국 종목 시세를 조회한다.
        String stooqSymbol = ticker.toLowerCase() + ".us"; // Stooq 미국 심볼 형식으로 변환한다.
        String encodedSymbol = URLEncoder.encode(stooqSymbol, StandardCharsets.UTF_8); // 심볼을 URL 인코딩한다.
        String url = "https://stooq.com/q/l/?s=" + encodedSymbol + "&f=sd2t2ohlcv&h&e=csv"; // Stooq CSV 조회 URL을 만든다.
        String body = fetchText(url); // CSV 본문을 조회한다.
        return parseQuote(body, ticker); // CSV 본문을 시세로 파싱한다.
    } // 단일 미국 종목 시세 조회를 종료한다.

    public List<StooqDailyPrice> fetchDailyPrices(String ticker, LocalDate from, LocalDate to, int maxRows) { // 단일 미국 종목 일봉을 조회한다.
        String stooqSymbol = ticker.toLowerCase() + ".us"; // Stooq 미국 심볼 형식으로 변환한다.
        return fetchDailyPricesByStooqSymbol(stooqSymbol, ticker, from, to, maxRows); // 심볼로 일봉을 조회한다.
    } // 단일 미국 종목 일봉 조회를 종료한다.

    // TASK-8: 지수 등 커스텀 Stooq 심볼(^ks11, spy.us 등)로 일봉을 조회한다.
    public List<StooqDailyPrice> fetchDailyPricesByStooqSymbol(String stooqSymbol, String storageTicker, LocalDate from, LocalDate to, int maxRows) { // 지정 Stooq 심볼로 일봉을 조회한다.
        String encodedSymbol = URLEncoder.encode(stooqSymbol, StandardCharsets.UTF_8); // 심볼을 URL 인코딩한다.
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE; // Stooq 날짜 파라미터 포맷을 준비한다.
        String url = "https://stooq.com/q/d/l/?s=" + encodedSymbol + "&d1=" + formatter.format(from) + "&d2=" + formatter.format(to) + "&i=d"; // Stooq 일봉 CSV URL을 만든다.
        String body = fetchText(url); // CSV 본문을 조회한다.
        return parseDailyPrices(body, storageTicker).stream()
                .map(p -> new StooqDailyPrice(storageTicker, p.tradeDate(), p.openPrice(), p.highPrice(), p.lowPrice(), p.closePrice(), p.volume(), p.turnover())) // 저장용 티커로 교체한다.
                .sorted(Comparator.comparing(StooqDailyPrice::tradeDate).reversed()) // 최근 거래일 순으로 정렬한다.
                .limit(maxRows) // 요청한 최대 개수만 남긴다.
                .sorted(Comparator.comparing(StooqDailyPrice::tradeDate)) // 저장 전 오래된 거래일부터 정렬한다.
                .toList(); // 목록으로 수집한다.
    } // 지정 Stooq 심볼 일봉 조회를 종료한다.

    private String fetchText(String url) { // 텍스트 응답을 조회한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build(); // GET 요청을 만든다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 문자열 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("Stooq 시세 조회에 실패했습니다. status=" + response.statusCode(), 502); // 외부 조회 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return response.body(); // 응답 본문을 반환한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("Stooq 시세 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("Stooq 시세 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 텍스트 응답 조회를 종료한다.

    private Optional<StooqQuote> parseQuote(String body, String fallbackTicker) { // CSV 본문을 시세로 파싱한다.
        String[] lines = body.split("\\R"); // 줄 단위로 나눈다.
        if (lines.length < 2) { // 데이터 행이 있는지 확인한다.
            return Optional.empty(); // 데이터가 없으면 빈 값을 반환한다.
        } // 데이터 행 확인을 종료한다.
        String[] values = lines[1].split(",", -1); // 데이터 행을 쉼표로 나눈다.
        if (values.length < 8 || Arrays.stream(values).anyMatch(value -> "N/D".equalsIgnoreCase(value) || "--".equals(value))) { // 유효하지 않은 값이 있는지 확인한다.
            return Optional.empty(); // 유효하지 않으면 빈 값을 반환한다.
        } // 값 유효성 확인을 종료한다.
        String ticker = values[0].replace(".US", "").replace(".us", "").toUpperCase(); // 응답 심볼을 표준 티커로 변환한다.
        LocalDate quoteDate = LocalDate.parse(values[1]); // 시세 날짜를 파싱한다.
        BigDecimal close = new BigDecimal(values[6]); // 종가를 파싱한다.
        BigDecimal volume = new BigDecimal(values[7]); // 거래량을 파싱한다.
        BigDecimal turnover = close.multiply(volume); // 거래대금을 계산한다.
        return Optional.of(new StooqQuote(ticker.isBlank() ? fallbackTicker : ticker, quoteDate, close, volume, turnover)); // 시세 정보를 반환한다.
    } // CSV 시세 파싱을 종료한다.

    private List<StooqDailyPrice> parseDailyPrices(String body, String fallbackTicker) { // CSV 본문을 일봉 목록으로 파싱한다.
        return Arrays.stream(body.split("\\R"))
                .skip(1) // 헤더 행을 제외한다.
                .filter(line -> !line.isBlank()) // 빈 줄을 제외한다.
                .map(line -> parseDailyPriceLine(line, fallbackTicker)) // 각 행을 일봉으로 파싱한다.
                .flatMap(Optional::stream) // 파싱 성공 행만 남긴다.
                .toList(); // 목록으로 수집한다.
    } // CSV 일봉 목록 파싱을 종료한다.

    private Optional<StooqDailyPrice> parseDailyPriceLine(String line, String fallbackTicker) { // CSV 단일 행을 일봉으로 파싱한다.
        String[] values = line.split(",", -1); // 데이터 행을 쉼표로 나눈다.
        if (values.length < 6 || Arrays.stream(values).anyMatch(value -> "N/D".equalsIgnoreCase(value) || "--".equals(value))) { // 유효하지 않은 값이 있는지 확인한다.
            return Optional.empty(); // 유효하지 않으면 빈 값을 반환한다.
        } // 값 유효성 확인을 종료한다.
        try { // 숫자와 날짜 파싱 예외를 처리한다.
            LocalDate tradeDate = LocalDate.parse(values[0]); // 거래일을 파싱한다.
            BigDecimal open = new BigDecimal(values[1]); // 시가를 파싱한다.
            BigDecimal high = new BigDecimal(values[2]); // 고가를 파싱한다.
            BigDecimal low = new BigDecimal(values[3]); // 저가를 파싱한다.
            BigDecimal close = new BigDecimal(values[4]); // 종가를 파싱한다.
            BigDecimal volume = new BigDecimal(values[5]); // 거래량을 파싱한다.
            BigDecimal turnover = close.multiply(volume); // 거래대금을 계산한다.
            return Optional.of(new StooqDailyPrice(fallbackTicker, tradeDate, open, high, low, close, volume, turnover)); // 일봉 정보를 반환한다.
        } catch (Exception exception) { // 파싱 실패를 처리한다.
            return Optional.empty(); // 파싱 실패 시 빈 값을 반환한다.
        } // 예외 처리를 종료한다.
    } // CSV 단일 일봉 행 파싱을 종료한다.

    public record StooqQuote(String ticker, LocalDate quoteDate, BigDecimal close, BigDecimal volume, BigDecimal turnover) { // Stooq 시세 정보를 정의한다.
    } // Stooq 시세 정보를 종료한다.

    public record StooqDailyPrice(String ticker, LocalDate tradeDate, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice, BigDecimal volume, BigDecimal turnover) { // Stooq 일봉 정보를 정의한다.
    } // Stooq 일봉 정보를 종료한다.
} // Stooq 시세 클라이언트를 종료한다.
