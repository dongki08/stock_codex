package com.parkdh.stockadvisor.infrastructure.marketdata.kr; // 한국 시장 데이터 클라이언트 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드 타입을 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.config.KisProperties; // KIS 설정 속성을 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 공통 유틸 클래스를 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.io.IOException; // 입출력 예외를 가져온다.
import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.net.URI; // URI 타입을 가져온다.
import java.net.http.HttpClient; // HTTP 클라이언트를 가져온다.
import java.net.http.HttpRequest; // HTTP 요청 타입을 가져온다.
import java.net.http.HttpResponse; // HTTP 응답 타입을 가져온다.
import java.time.Duration; // 시간 간격 타입을 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.format.DateTimeFormatter; // 날짜 포맷 도구를 가져온다.
import java.util.ArrayList; // 배열 목록 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Optional; // 선택 값 타입을 가져온다.

@Slf4j // SLF4J 로거를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class KisApiClient { // 한국투자증권 API 클라이언트를 정의한다.

    private static final String INQUIRE_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price"; // 국내 주식 현재가 조회 경로를 정의한다.
    private static final String INQUIRE_DAILY_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"; // 국내 주식 일봉 조회 경로를 정의한다.

    private final KisProperties kisProperties; // KIS 설정 속성을 보관한다.
    private final KisTokenStore kisTokenStore; // KIS 토큰 저장소를 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서를 보관한다.
    private final HttpClient httpClient; // HTTP 클라이언트를 보관한다.

    public KisApiClient(KisProperties kisProperties, KisTokenStore kisTokenStore, ObjectMapper objectMapper) { // 생성자로 의존성을 주입한다.
        this.kisProperties = kisProperties; // KIS 설정 속성을 저장한다.
        this.kisTokenStore = kisTokenStore; // KIS 토큰 저장소를 저장한다.
        this.objectMapper = objectMapper; // JSON 파서를 저장한다.
        this.httpClient = HttpClient.newBuilder() // HTTP 클라이언트를 생성한다.
                .connectTimeout(Duration.ofSeconds(10)) // 연결 타임아웃을 10초로 설정한다.
                .build(); // HTTP 클라이언트 생성을 완료한다.
    } // 생성자를 종료한다.

    public Optional<KisCurrentPrice> fetchCurrentPrice(String ticker) { // 국내 주식 현재가를 조회한다.
        if (MarketUtil.isDevPlaceholder(kisProperties.appKey())) { // 개발 모드인지 확인한다.
            log.debug("KIS 개발 모드: 현재가 조회를 건너뜁니다. ticker={}", ticker); // 개발 모드 스킵 로그를 출력한다.
            return Optional.empty(); // 개발 모드에서는 빈 값을 반환한다.
        } // 개발 모드 확인을 종료한다.
        String token = kisTokenStore.getToken(); // 유효한 액세스 토큰을 가져온다.
        String url = buildPriceUrl(ticker); // 현재가 조회 URL을 구성한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                .timeout(Duration.ofSeconds(15)) // 요청 타임아웃을 15초로 설정한다.
                .header("authorization", "Bearer " + token) // 인증 헤더를 설정한다.
                .header("appkey", kisProperties.appKey()) // appkey 헤더를 설정한다.
                .header("appsecret", kisProperties.appSecret()) // appsecret 헤더를 설정한다.
                .header("tr_id", "FHKST01010100") // 거래 ID 헤더를 설정한다.
                .GET() // GET 메서드를 사용한다.
                .build(); // HTTP 요청 생성을 완료한다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("KIS 현재가 조회에 실패했습니다. ticker=" + ticker + ", status=" + response.statusCode(), 502); // 외부 API 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return parseCurrentPrice(response.body(), ticker); // 응답 본문을 현재가로 파싱한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("KIS 현재가 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("KIS 현재가 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 현재가 조회를 종료한다.

    public List<KisFundamentalMetric> fetchFundamentalMetrics(String ticker, String market) { // 국내 주식 펀더멘털 지표를 조회한다.
        if (MarketUtil.isDevPlaceholder(kisProperties.appKey())) { // 개발 모드인지 확인한다.
            log.debug("KIS 개발 모드: 펀더멘털 조회를 건너뜁니다. ticker={}", ticker); // 개발 모드 스킵 로그를 출력한다.
            return List.of(); // 개발 모드에서는 빈 목록을 반환한다.
        } // 개발 모드 확인을 종료한다.
        String token = kisTokenStore.getToken(); // 유효한 액세스 토큰을 가져온다.
        String url = buildPriceUrl(ticker); // 현재가 조회 URL을 구성한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                .timeout(Duration.ofSeconds(15)) // 요청 타임아웃을 15초로 설정한다.
                .header("authorization", "Bearer " + token) // 인증 헤더를 설정한다.
                .header("appkey", kisProperties.appKey()) // appkey 헤더를 설정한다.
                .header("appsecret", kisProperties.appSecret()) // appsecret 헤더를 설정한다.
                .header("tr_id", "FHKST01010100") // 거래 ID 헤더를 설정한다.
                .GET() // GET 메서드를 사용한다.
                .build(); // HTTP 요청 생성을 완료한다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                throw new CustomException("KIS 펀더멘털 조회에 실패했습니다. ticker=" + ticker + ", status=" + response.statusCode(), 502); // 외부 API 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return parseFundamentalMetrics(response.body(), ticker, market); // 응답 본문을 펀더멘털 지표로 파싱한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("KIS 펀더멘털 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("KIS 펀더멘털 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 국내 주식 펀더멘털 조회를 종료한다.

    public List<KisDailyPrice> fetchDailyPrices(String ticker, LocalDate from, LocalDate to) { // 국내 주식 일봉을 조회한다.
        if (MarketUtil.isDevPlaceholder(kisProperties.appKey())) { // 개발 모드인지 확인한다.
            log.debug("KIS 개발 모드: 일봉 조회를 건너뜁니다. ticker={}", ticker); // 개발 모드 스킵 로그를 출력한다.
            return List.of(); // 개발 모드에서는 빈 목록을 반환한다.
        } // 개발 모드 확인을 종료한다.
        return fetchDailyPricesWithRetry(ticker, from, to, 3); // 재시도 로직을 포함해 일봉을 조회한다.
    } // 국내 주식 일봉 조회를 종료한다.

    private List<KisDailyPrice> fetchDailyPricesWithRetry(String ticker, LocalDate from, LocalDate to, int remaining) { // EGW00201 재시도 포함 일봉 조회를 수행한다.
        String token = kisTokenStore.getToken(); // 유효한 액세스 토큰을 가져온다.
        String url = buildDailyUrl(ticker, from, to); // 일봉 조회 URL을 구성한다.
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)) // HTTP 요청을 생성한다.
                .timeout(Duration.ofSeconds(20)) // 요청 타임아웃을 20초로 설정한다.
                .header("authorization", "Bearer " + token) // 인증 헤더를 설정한다.
                .header("appkey", kisProperties.appKey()) // appkey 헤더를 설정한다.
                .header("appsecret", kisProperties.appSecret()) // appsecret 헤더를 설정한다.
                .header("tr_id", "FHKST03010100") // 일봉 조회 거래 ID 헤더를 설정한다.
                .GET() // GET 메서드를 사용한다.
                .build(); // HTTP 요청 생성을 완료한다.
        try { // 네트워크 예외를 처리한다.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // 요청을 보내고 응답을 받는다.
            if (response.statusCode() < 200 || response.statusCode() >= 300) { // 성공 상태 코드인지 확인한다.
                String body = response.body();
                log.warn("KIS 일봉 조회 실패. ticker={}, status={}, body={}", ticker, response.statusCode(), body.substring(0, Math.min(300, body.length())));
                if (remaining > 0 && body.contains("EGW00201")) { // 초당 거래 한도 초과 시 재시도한다.
                    log.info("KIS 초당 한도 초과. 1초 후 재시도. ticker={}, remaining={}", ticker, remaining);
                    Thread.sleep(1000);
                    return fetchDailyPricesWithRetry(ticker, from, to, remaining - 1);
                }
                throw new CustomException("KIS 일봉 조회에 실패했습니다. ticker=" + ticker + ", status=" + response.statusCode(), 502); // 외부 API 실패 예외를 던진다.
            } // 상태 코드 확인을 종료한다.
            return parseDailyPrices(response.body(), ticker); // 응답 본문을 일봉 목록으로 파싱한다.
        } catch (IOException exception) { // 입출력 예외를 잡는다.
            throw new CustomException("KIS 일봉 네트워크 오류: " + exception.getMessage(), 502); // 네트워크 오류 예외를 던진다.
        } catch (InterruptedException exception) { // 인터럽트 예외를 잡는다.
            Thread.currentThread().interrupt(); // 인터럽트 상태를 복원한다.
            throw new CustomException("KIS 일봉 조회가 중단되었습니다.", 502); // 중단 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 재시도 포함 일봉 조회를 종료한다.

    private String buildPriceUrl(String ticker) { // 현재가 조회 URL을 구성한다.
        return kisProperties.baseUrl() + INQUIRE_PRICE_PATH // 기본 URL과 경로를 결합한다.
                + "?fid_cond_mrkt_div_code=J" // 시장 구분 코드를 추가한다.
                + "&fid_input_iscd=" + ticker; // 종목 코드를 추가한다.
    } // URL 구성을 종료한다.

    private String buildDailyUrl(String ticker, LocalDate from, LocalDate to) { // 일봉 조회 URL을 구성한다.
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE; // KIS 날짜 파라미터 포맷을 준비한다.
        return kisProperties.baseUrl() + INQUIRE_DAILY_PATH // 기본 URL과 경로를 결합한다.
                + "?FID_COND_MRKT_DIV_CODE=J" // 시장 구분 코드를 추가한다.
                + "&FID_INPUT_ISCD=" + ticker // 종목 코드를 추가한다.
                + "&FID_INPUT_DATE_1=" + formatter.format(from) // 시작일을 추가한다.
                + "&FID_INPUT_DATE_2=" + formatter.format(to) // 종료일을 추가한다.
                + "&FID_PERIOD_DIV_CODE=D" // 일봉 조회를 지정한다.
                + "&FID_ORG_ADJ_PRC=1"; // 수정주가 기준을 지정한다.
    } // 일봉 URL 구성을 종료한다.

    private Optional<KisCurrentPrice> parseCurrentPrice(String body, String ticker) { // 응답 JSON을 현재가로 파싱한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode root = objectMapper.readTree(body); // 응답 JSON을 파싱한다.
            JsonNode output = root.path("output"); // output 노드를 가져온다.
            if (output.isMissingNode() || output.isNull()) { // output 노드가 없으면 빈 값을 반환한다.
                log.warn("KIS 현재가 응답에 output이 없습니다. ticker={}", ticker); // 응답 이상 경고 로그를 출력한다.
                return Optional.empty(); // output이 없으면 빈 값을 반환한다.
            } // output 노드 확인을 종료한다.
            String priceStr = output.path("stck_prpr").asText("0"); // 현재가 문자열을 추출한다.
            String changeRateStr = output.path("prdy_ctrt").asText("0"); // 전일대비율 문자열을 추출한다.
            String volumeStr = output.path("acml_vol").asText("0"); // 누적거래량 문자열을 추출한다.
            BigDecimal currentPrice = new BigDecimal(priceStr.isBlank() ? "0" : priceStr); // 현재가를 숫자로 변환한다.
            BigDecimal changeRate = new BigDecimal(changeRateStr.isBlank() ? "0" : changeRateStr); // 전일대비율을 숫자로 변환한다.
            BigDecimal volume = new BigDecimal(volumeStr.isBlank() ? "0" : volumeStr); // 누적거래량을 숫자로 변환한다.
            return Optional.of(new KisCurrentPrice(ticker, currentPrice, changeRate, volume)); // 현재가 정보를 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            log.warn("KIS 현재가 응답 파싱에 실패했습니다. ticker={}, error={}", ticker, exception.getMessage()); // 파싱 실패 경고 로그를 출력한다.
            return Optional.empty(); // 파싱 실패 시 빈 값을 반환한다.
        } // 파싱 예외 처리를 종료한다.
    } // 현재가 파싱을 종료한다.

    private List<KisFundamentalMetric> parseFundamentalMetrics(String body, String ticker, String market) { // 응답 JSON을 펀더멘털 지표로 파싱한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode root = objectMapper.readTree(body); // 응답 JSON을 파싱한다.
            JsonNode output = root.path("output"); // output 노드를 가져온다.
            if (output.isMissingNode() || output.isNull()) { // output 노드가 없으면 빈 값을 반환한다.
                log.warn("KIS 펀더멘털 응답에 output이 없습니다. ticker={}", ticker); // 응답 이상 경고 로그를 출력한다.
                return List.of(); // output이 없으면 빈 목록을 반환한다.
            } // output 노드 확인을 종료한다.
            LocalDate periodEnd = LocalDate.now(); // 현재가 기반 지표는 조회일을 기준일로 둔다.
            ArrayList<KisFundamentalMetric> metrics = new ArrayList<>(); // 지표 목록을 생성한다.
            parseDecimal(output, "per").ifPresent(value -> metrics.add(new KisFundamentalMetric(ticker, market, "PER", value, "x", periodEnd, "KIS_INQUIRE_PRICE"))); // PER을 추가한다.
            parseDecimal(output, "pbr").ifPresent(value -> metrics.add(new KisFundamentalMetric(ticker, market, "PBR", value, "x", periodEnd, "KIS_INQUIRE_PRICE"))); // PBR을 추가한다.
            Optional<BigDecimal> eps = parseDecimal(output, "eps"); // EPS를 파싱한다.
            Optional<BigDecimal> bps = parseDecimal(output, "bps"); // BPS를 파싱한다.
            eps.ifPresent(value -> metrics.add(new KisFundamentalMetric(ticker, market, "EPS", value, "KRW/share", periodEnd, "KIS_INQUIRE_PRICE"))); // EPS를 추가한다.
            bps.ifPresent(value -> metrics.add(new KisFundamentalMetric(ticker, market, "BPS", value, "KRW/share", periodEnd, "KIS_INQUIRE_PRICE"))); // BPS를 추가한다.
            if (eps.isPresent() && bps.isPresent() && bps.get().compareTo(BigDecimal.ZERO) != 0) { // ROE 계산 가능 여부를 확인한다.
                BigDecimal roe = eps.get().divide(bps.get(), 6, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)); // ROE를 계산한다.
                metrics.add(new KisFundamentalMetric(ticker, market, "ROE", roe, "%", periodEnd, "KIS_INQUIRE_PRICE")); // ROE를 추가한다.
            } // ROE 계산을 종료한다.
            return metrics; // 지표 목록을 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            log.warn("KIS 펀더멘털 응답 파싱에 실패했습니다. ticker={}, error={}", ticker, exception.getMessage()); // 파싱 실패 경고 로그를 출력한다.
            return List.of(); // 파싱 실패 시 빈 목록을 반환한다.
        } // 파싱 예외 처리를 종료한다.
    } // 펀더멘털 파싱을 종료한다.

    private Optional<BigDecimal> parseDecimal(JsonNode node, String fieldName) { // JSON 숫자 문자열을 BigDecimal로 파싱한다.
        String value = node.path(fieldName).asText(""); // 필드 값을 문자열로 읽는다.
        if (value.isBlank() || "0".equals(value)) { // 빈 값이나 0 값은 저장하지 않는다.
            return Optional.empty(); // 빈 값을 반환한다.
        } // 값 확인을 종료한다.
        try { // 숫자 파싱 예외를 처리한다.
            return Optional.of(new BigDecimal(value.replace(",", ""))); // 쉼표를 제거하고 숫자로 반환한다.
        } catch (NumberFormatException exception) { // 숫자 형식 오류를 잡는다.
            return Optional.empty(); // 파싱 실패 시 빈 값을 반환한다.
        } // 예외 처리를 종료한다.
    } // BigDecimal 파싱을 종료한다.

    private List<KisDailyPrice> parseDailyPrices(String body, String ticker) { // 응답 JSON을 일봉 목록으로 파싱한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode root = objectMapper.readTree(body); // 응답 JSON을 파싱한다.
            JsonNode output = root.path("output2"); // 일봉 목록 노드를 가져온다.
            if (!output.isArray()) { // 일봉 목록이 배열인지 확인한다.
                log.warn("KIS 일봉 응답에 output2 배열이 없습니다. ticker={}", ticker); // 응답 이상 경고 로그를 출력한다.
                return List.of(); // 배열이 아니면 빈 목록을 반환한다.
            } // output2 확인을 종료한다.
            java.util.ArrayList<KisDailyPrice> prices = new java.util.ArrayList<>(); // 일봉 목록을 생성한다.
            for (JsonNode row : output) { // 각 일봉 행을 순회한다.
                parseDailyPriceRow(row, ticker).ifPresent(prices::add); // 파싱된 일봉을 목록에 추가한다.
            } // 일봉 행 순회를 종료한다.
            return prices.stream().sorted(java.util.Comparator.comparing(KisDailyPrice::tradeDate)).toList(); // 거래일 오름차순으로 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            log.warn("KIS 일봉 응답 파싱에 실패했습니다. ticker={}, error={}", ticker, exception.getMessage()); // 파싱 실패 경고 로그를 출력한다.
            return List.of(); // 파싱 실패 시 빈 목록을 반환한다.
        } // 파싱 예외 처리를 종료한다.
    } // 일봉 목록 파싱을 종료한다.

    private Optional<KisDailyPrice> parseDailyPriceRow(JsonNode row, String ticker) { // 일봉 단일 행을 파싱한다.
        try { // 날짜와 숫자 파싱 예외를 처리한다.
            LocalDate tradeDate = LocalDate.parse(row.path("stck_bsop_date").asText(), DateTimeFormatter.BASIC_ISO_DATE); // 거래일을 파싱한다.
            BigDecimal open = new BigDecimal(row.path("stck_oprc").asText("0")); // 시가를 파싱한다.
            BigDecimal high = new BigDecimal(row.path("stck_hgpr").asText("0")); // 고가를 파싱한다.
            BigDecimal low = new BigDecimal(row.path("stck_lwpr").asText("0")); // 저가를 파싱한다.
            BigDecimal close = new BigDecimal(row.path("stck_clpr").asText("0")); // 종가를 파싱한다.
            BigDecimal volume = new BigDecimal(row.path("acml_vol").asText("0")); // 누적 거래량을 파싱한다.
            BigDecimal turnover = new BigDecimal(row.path("acml_tr_pbmn").asText("0")); // 누적 거래대금을 파싱한다.
            return Optional.of(new KisDailyPrice(ticker, tradeDate, open, high, low, close, volume, turnover)); // 일봉 정보를 반환한다.
        } catch (Exception exception) { // 파싱 실패를 처리한다.
            return Optional.empty(); // 파싱 실패 시 빈 값을 반환한다.
        } // 예외 처리를 종료한다.
    } // 일봉 단일 행 파싱을 종료한다.

    public record KisCurrentPrice( // 한국 주식 현재가 정보를 정의한다.
            String ticker,          // 종목 코드를 보관한다.
            BigDecimal currentPrice, // 현재가를 보관한다.
            BigDecimal changeRate,   // 전일대비율을 보관한다.
            BigDecimal volume        // 누적거래량을 보관한다.
    ) {} // 현재가 레코드를 종료한다.

    public record KisDailyPrice( // 한국 주식 일봉 정보를 정의한다.
            String ticker,           // 종목 코드를 보관한다.
            LocalDate tradeDate,     // 거래일을 보관한다.
            BigDecimal openPrice,    // 시가를 보관한다.
            BigDecimal highPrice,    // 고가를 보관한다.
            BigDecimal lowPrice,     // 저가를 보관한다.
            BigDecimal closePrice,   // 종가를 보관한다.
            BigDecimal volume,       // 거래량을 보관한다.
            BigDecimal turnover      // 거래대금을 보관한다.
    ) {} // 일봉 레코드를 종료한다.

    public record KisFundamentalMetric( // 한국 주식 펀더멘털 지표를 정의한다.
            String ticker,             // 종목 코드를 보관한다.
            String market,             // 시장 구분을 보관한다.
            String metricName,         // 지표 이름을 보관한다.
            BigDecimal metricValue,    // 지표 값을 보관한다.
            String unit,               // 단위를 보관한다.
            LocalDate periodEnd,       // 기준일을 보관한다.
            String source              // 데이터 출처를 보관한다.
    ) {} // 한국 펀더멘털 지표 레코드를 종료한다.
} // KIS API 클라이언트를 종료한다.
