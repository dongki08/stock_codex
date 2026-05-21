package com.parkdh.stockadvisor.api.marketdata; // 시장 데이터 API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService; // 시장 데이터 동기화 서비스를 가져온다.
import com.parkdh.stockadvisor.application.marketdata.MarketDataCollectionService; // 시장 데이터 수집 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/market-data") // 시장 데이터 API 공통 경로를 지정한다.
public class MarketDataSyncController { // 시장 데이터 동기화 컨트롤러를 정의한다.
    private final MarketDataSyncService marketDataSyncService; // 시장 데이터 동기화 서비스 의존성을 보관한다.
    private final MarketDataCollectionService marketDataCollectionService; // 시장 데이터 수집 서비스 의존성을 보관한다.

    @Operation(summary = "일봉 가격 조회", description = """
            저장된 일봉 가격 히스토리를 조회한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE/ALL
            - **ticker** *(String, 선택)* : 종목 코드
            - **limit** *(Integer, 선택)* : 최대 조회 수. 기본 100, 최대 1000
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200
            - **data** *(Array)* : ticker, market, tradeDate, OHLCV, turnover, source
            """) // Swagger 문서를 정의한다.
    @GetMapping("/daily-prices") // 일봉 가격 조회 경로를 매핑한다.
    public ResultDto<?> getDailyPrices(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 일봉 가격 조회 API를 정의한다.
        return ResultDto.success(marketDataSyncService.getDailyPrices(market, ticker, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 일봉 가격 조회 API를 종료한다.

    @Operation(summary = "장중 가격 조회", description = """
            저장된 장중 가격 스냅샷을 조회한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE/ALL
            - **ticker** *(String, 선택)* : 종목 코드
            - **limit** *(Integer, 선택)* : 최대 조회 수. 기본 100, 최대 1000
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200
            - **data** *(Array)* : ticker, market, tickAt, price, volume, source
            """) // Swagger 문서를 정의한다.
    @GetMapping("/intraday-prices") // 장중 가격 조회 경로를 매핑한다.
    public ResultDto<?> getIntradayPrices(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 장중 가격 조회 API를 정의한다.
        return ResultDto.success(marketDataSyncService.getIntradayPrices(market, ticker, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 장중 가격 조회 API를 종료한다.

    @Operation(summary = "일봉 가격 동기화", description = """
            시장 후보군을 순회하며 KIS 또는 Stooq에서 일봉 가격을 조회해 price_daily에 저장한다.
            **사용 전제:**
            - `/api/dev/universe/seed` 또는 `/api/universe/sync/us-symbols`로 시장 후보군이 있어야 한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE/ALL. 기본 ALL
            - **limit** *(Integer, 선택)* : 동기화할 후보 수. 기본 20, 최대 500
            - **days** *(Integer, 선택)* : 조회 기간 일수. 기본 120, 최대 2000
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200
            - **data** *(Object)* : 대상 시장, 후보 수, 수집/저장 일봉 수, 샘플 키
            """) // Swagger 문서를 정의한다.
    @PostMapping("/daily-prices/sync") // 일봉 가격 동기화 경로를 매핑한다.
    public ResultDto<?> syncDailyPrices(@RequestParam(required = false) String market, @RequestParam(required = false) Integer limit, @RequestParam(required = false) Integer days) { // 일봉 가격 동기화 API를 정의한다.
        return ResultDto.success(marketDataSyncService.syncDailyPrices(market, limit, days)); // 서비스 동기화 결과를 성공 응답으로 반환한다.
    } // 일봉 가격 동기화 API를 종료한다.

    @Operation(summary = "뉴스 조회", description = "RSS 기반으로 저장된 최근 뉴스 제목/링크를 조회한다.") // Swagger 문서를 정의한다.
    @GetMapping("/news") // 뉴스 조회 경로를 매핑한다.
    public ResultDto<?> getNews(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 뉴스 조회 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.getNewsArticles(market, ticker, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 뉴스 조회 API를 종료한다.

    @Operation(summary = "뉴스 RSS 동기화", description = "Google News/Yahoo Finance RSS에서 제목, 링크, 발행 시각을 수집해 저장한다.") // Swagger 문서를 정의한다.
    @PostMapping("/news/sync") // 뉴스 동기화 경로를 매핑한다.
    public ResultDto<?> syncNews(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 뉴스 동기화 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.syncNewsArticles(market, ticker, limit)); // 서비스 동기화 결과를 성공 응답으로 반환한다.
    } // 뉴스 동기화 API를 종료한다.

    @Operation(summary = "공시 조회", description = "DART/SEC EDGAR에서 저장된 공시 메타데이터를 조회한다.") // Swagger 문서를 정의한다.
    @GetMapping("/disclosures") // 공시 조회 경로를 매핑한다.
    public ResultDto<?> getDisclosures(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 공시 조회 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.getDisclosureEvents(market, ticker, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 공시 조회 API를 종료한다.

    @Operation(summary = "공시 동기화", description = "한국은 DART 키가 있을 때, 미국은 SEC EDGAR Atom feed에서 공시 메타데이터를 수집한다.") // Swagger 문서를 정의한다.
    @PostMapping("/disclosures/sync") // 공시 동기화 경로를 매핑한다.
    public ResultDto<?> syncDisclosures(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 공시 동기화 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.syncDisclosureEvents(market, ticker, limit)); // 서비스 동기화 결과를 성공 응답으로 반환한다.
    } // 공시 동기화 API를 종료한다.

    @Operation(summary = "매크로 지표 조회", description = "FRED 기반으로 저장된 매크로 관측값을 조회한다.") // Swagger 문서를 정의한다.
    @GetMapping("/macro-observations") // 매크로 조회 경로를 매핑한다.
    public ResultDto<?> getMacroObservations(@RequestParam(required = false) String seriesId, @RequestParam(required = false) Integer limit) { // 매크로 조회 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.getMacroObservations(seriesId, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 매크로 조회 API를 종료한다.

    @Operation(summary = "매크로 지표 동기화", description = "FRED 공개 CSV에서 주요 지표를 수집해 저장한다. seriesId가 없으면 기본 핵심 지표 묶음을 수집한다.") // Swagger 문서를 정의한다.
    @PostMapping("/macro-observations/sync") // 매크로 동기화 경로를 매핑한다.
    public ResultDto<?> syncMacroObservations(@RequestParam(required = false) String seriesId, @RequestParam(required = false) Integer limit) { // 매크로 동기화 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.syncMacroObservations(seriesId, limit)); // 서비스 동기화 결과를 성공 응답으로 반환한다.
    } // 매크로 동기화 API를 종료한다.

    @Operation(summary = "펀더멘털 지표 조회", description = "SEC Company Facts 기반으로 저장된 미국 종목 펀더멘털 지표를 조회한다.") // Swagger 문서를 정의한다.
    @GetMapping("/fundamentals") // 펀더멘털 조회 경로를 매핑한다.
    public ResultDto<?> getFundamentals(@RequestParam(required = false) String market, @RequestParam(required = false) String ticker, @RequestParam(required = false) Integer limit) { // 펀더멘털 조회 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.getFundamentalMetrics(market, ticker, limit)); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 펀더멘털 조회 API를 종료한다.

    @Operation(summary = "펀더멘털 지표 동기화", description = "미국 종목 ticker를 SEC Company Facts에서 조회해 주요 펀더멘털 지표를 저장한다.") // Swagger 문서를 정의한다.
    @PostMapping("/fundamentals/sync") // 펀더멘털 동기화 경로를 매핑한다.
    public ResultDto<?> syncFundamentals(@RequestParam(required = false) String market, @RequestParam String ticker) { // 펀더멘털 동기화 API를 정의한다.
        return ResultDto.success(marketDataCollectionService.syncFundamentalMetrics(market, ticker)); // 서비스 동기화 결과를 성공 응답으로 반환한다.
    } // 펀더멘털 동기화 API를 종료한다.
} // 시장 데이터 동기화 컨트롤러를 종료한다.
