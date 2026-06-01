package com.parkdh.stockadvisor.infrastructure.marketdata.kr; // 한국 시장 데이터 클라이언트 패키지를 선언한다.

import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.nio.charset.Charset; // 문자셋 타입을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Optional; // 선택 값 타입을 가져온다.
import java.util.regex.Matcher; // 정규식 매처를 가져온다.
import java.util.regex.Pattern; // 정규식 패턴을 가져온다.

@Component // 스프링 컴포넌트로 등록한다.
public class KrxSymbolClient { // KRX/KIND 한국 상장 종목 클라이언트를 정의한다.
    private static final String KIND_CORP_LIST_URL = "https://kind.krx.co.kr/corpgeneral/corpList.do"; // KIND 상장법인 목록 URL을 정의한다.
    private static final Pattern ROW_PATTERN = Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>"); // HTML 행 패턴을 정의한다.
    private static final Pattern CELL_PATTERN = Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>"); // HTML 셀 패턴을 정의한다.

    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.

    public KrxSymbolClient() { // 기본 생성자를 정의한다.
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(); // 타임아웃이 있는 HTTP 클라이언트를 만든다.
    } // 기본 생성자를 종료한다.

    public List<KrxSymbol> fetchKospiListed() { // KOSPI 상장 종목을 조회한다.
        return fetchListed("KOSPI", "stockMkt"); // KOSPI 시장 목록을 반환한다.
    } // KOSPI 상장 종목 조회를 종료한다.

    public List<KrxSymbol> fetchKosdaqListed() { // KOSDAQ 상장 종목을 조회한다.
        return fetchListed("KOSDAQ", "kosdaqMkt"); // KOSDAQ 시장 목록을 반환한다.
    } // KOSDAQ 상장 종목 조회를 종료한다.

    private List<KrxSymbol> fetchListed(String market, String marketType) { // 시장별 상장 종목을 조회한다.
        String url = KIND_CORP_LIST_URL + "?method=download&searchType=13&marketType=" + marketType; // KIND 다운로드 URL을 구성한다.
        String body = fetchHtml(url); // HTML 본문을 조회한다.
        List<KrxSymbol> symbols = parseSymbols(body, market); // HTML 본문을 종목 목록으로 파싱한다.
        if (symbols.isEmpty()) { // 파싱된 종목이 없는지 확인한다.
            throw new CustomException("KRX 상장 종목 목록을 파싱하지 못했습니다. market=" + market, 502); // 파싱 실패 예외를 던진다.
        } // 종목 확인을 종료한다.
        return symbols; // 종목 목록을 반환한다.
    } // 시장별 상장 종목 조회를 종료한다.

    private String fetchHtml(String url) { // HTML 응답을 조회한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                .timeout(Duration.ofSeconds(20)) // 요청 타임아웃을 20초로 설정한다.
                .header("User-Agent", "Mozilla/5.0 StockAdvisor/1.0") // KIND 차단 가능성을 줄이기 위해 User-Agent를 설정한다.
                .GET() // GET 메서드를 사용한다.
                .build(); // HTTP 요청 생성을 완료한다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray()); // 요청을 보내고 바이트 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("KRX 상장 종목 조회에 실패했습니다. status=" + response.statusCode(), 502); // 외부 조회 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return decodeBody(response); // 응답 본문을 문자열로 디코딩한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("KRX 상장 종목 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("KRX 상장 종목 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // HTML 응답 조회를 종료한다.

    private String decodeBody(HttpResponse<byte[]> response) { // 응답 본문을 디코딩한다.
        Optional<String> contentType = response.headers().firstValue("Content-Type"); // Content-Type 헤더를 가져온다.
        Charset charset = contentType.filter(value -> value.toLowerCase().contains("utf-8")).map(value -> Charset.forName("UTF-8")).orElse(Charset.forName("EUC-KR")); // 헤더를 기준으로 문자셋을 선택한다.
        return new String(response.body(), charset); // 선택한 문자셋으로 본문을 반환한다.
    } // 응답 본문 디코딩을 종료한다.

    private List<KrxSymbol> parseSymbols(String body, String market) { // HTML 본문을 종목 목록으로 파싱한다.
        java.util.ArrayList<KrxSymbol> symbols = new java.util.ArrayList<>(); // 종목 목록을 생성한다.
        Matcher rowMatcher = ROW_PATTERN.matcher(body); // 행 매처를 생성한다.
        while (rowMatcher.find()) { // HTML 행을 순회한다.
            parseRow(rowMatcher.group(1), market).ifPresent(symbols::add); // 파싱된 종목을 목록에 추가한다.
        } // 행 순회를 종료한다.
        return symbols; // 종목 목록을 반환한다.
    } // HTML 본문 파싱을 종료한다.

    private Optional<KrxSymbol> parseRow(String rowHtml, String market) { // HTML 행을 단일 종목으로 파싱한다.
        java.util.ArrayList<String> cells = new java.util.ArrayList<>(); // 셀 목록을 생성한다.
        Matcher cellMatcher = CELL_PATTERN.matcher(rowHtml); // 셀 매처를 생성한다.
        while (cellMatcher.find()) { // 셀을 순회한다.
            cells.add(cleanCell(cellMatcher.group(1))); // 셀 텍스트를 정리해 추가한다.
        } // 셀 순회를 종료한다.
        if (cells.size() < 2) { // 회사명과 종목코드가 있는지 확인한다.
            return Optional.empty(); // 부족하면 빈 값을 반환한다.
        } // 셀 개수 확인을 종료한다.
        String name = cells.get(0); // 회사명을 가져온다.
        // KIND 테이블 컬럼 순서가 변경될 수 있으므로 6자리 숫자 셀을 탐색한다.
        String ticker = cells.stream()
                .map(this::normalizeTicker)
                .filter(t -> t.length() == 6)
                .findFirst()
                .orElse("");
        if (name.isBlank() || ticker.isBlank()) { // 유효한 종목인지 확인한다.
            return Optional.empty(); // 유효하지 않으면 빈 값을 반환한다.
        } // 종목 유효성 확인을 종료한다.
        return Optional.of(new KrxSymbol(ticker, market, name, tradable(name, ticker))); // 종목 정보를 반환한다.
    } // HTML 행 파싱을 종료한다.

    private String cleanCell(String html) { // HTML 셀 내용을 텍스트로 정리한다.
        return html.replaceAll("(?is)<[^>]+>", "") // HTML 태그를 제거한다.
                .replace("&amp;", "&") // amp 엔티티를 치환한다.
                .replace("&nbsp;", " ") // 공백 엔티티를 치환한다.
                .replace("&#40;", "(") // 여는 괄호 엔티티를 치환한다.
                .replace("&#41;", ")") // 닫는 괄호 엔티티를 치환한다.
                .trim(); // 앞뒤 공백을 제거한다.
    } // 셀 텍스트 정리를 종료한다.

    private String normalizeTicker(String rawTicker) { // 종목코드를 6자리로 정규화한다.
        String digits = rawTicker.replaceAll("[^0-9]", ""); // 숫자만 남긴다.
        if (digits.isBlank()) { // 숫자가 없는지 확인한다.
            return ""; // 빈 값을 반환한다.
        } // 숫자 확인을 종료한다.
        return String.format("%06d", Integer.parseInt(digits)); // 6자리 종목코드로 반환한다.
    } // 종목코드 정규화를 종료한다.

    private boolean tradable(String name, String ticker) { // 추천 후보 거래 가능 여부를 판단한다.
        if (ticker.isBlank()) { // 종목코드가 없는지 확인한다.
            return false; // 거래 불가로 반환한다.
        } // 종목코드 확인을 종료한다.
        return !(name.contains("스팩") || name.contains("기업인수목적") || name.endsWith("우") || name.contains("우선주")); // 스팩과 우선주는 제외한다.
    } // 거래 가능 여부 판단을 종료한다.

    public record KrxSymbol(String ticker, String market, String name, boolean tradable) { // KRX 종목 정보를 정의한다.
    } // KRX 종목 정보를 종료한다.
} // KRX/KIND 한국 상장 종목 클라이언트를 종료한다.
