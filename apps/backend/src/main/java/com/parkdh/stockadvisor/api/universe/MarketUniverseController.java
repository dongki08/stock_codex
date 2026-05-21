package com.parkdh.stockadvisor.api.universe; // 시장 유니버스 API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.universe.MarketUniverseService; // 시장 유니버스 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/universe") // 시장 유니버스 API 공통 경로를 지정한다.
public class MarketUniverseController { // 시장 유니버스 컨트롤러를 정의한다.
    private final MarketUniverseService marketUniverseService; // 시장 유니버스 서비스 의존성을 보관한다.

    public MarketUniverseController(MarketUniverseService marketUniverseService) { // 생성자 주입을 정의한다.
        this.marketUniverseService = marketUniverseService; // 시장 유니버스 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "시장 유니버스 조회", description = """
            자동 추천 엔진이 스캔할 시장 후보군을 조회한다.
            **사용 목적:**
            - 사용자가 종목을 직접 등록하지 않아도 시장 후보군을 확인한다.
            - 실제 수집기 또는 개발용 seed가 저장한 KOSPI/KOSDAQ/NYSE/NASDAQ 후보를 필터링한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NYSE/NASDAQ
            - **tradable** *(Boolean, 선택)* : 거래 가능 여부
            - **minMarketCap** *(Decimal, 선택)* : 시가총액 하한
            - **minAvgTurnover** *(Decimal, 선택)* : 평균 거래대금 하한
            - **minLastPrice** *(Decimal, 선택)* : 최근 가격 하한
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 후보 종목 코드, 시장, 이름, 섹터, 시총, 거래대금, 최근 가격, 거래 가능 여부
            **특징:**
            - 최종 추천 플로우는 이 후보군을 기반으로 feature를 만들고 Top-N 추천을 생성한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 시장 유니버스 목록 조회 경로를 매핑한다.
    public ResultDto<?> getUniverse(@RequestParam(required = false) String market, @RequestParam(required = false) Boolean tradable, @RequestParam(required = false) BigDecimal minMarketCap, @RequestParam(required = false) BigDecimal minAvgTurnover, @RequestParam(required = false) BigDecimal minLastPrice) { // 시장 유니버스 조회 API를 정의한다.
        return ResultDto.success(marketUniverseService.getUniverse(market, tradable, minMarketCap, minAvgTurnover, minLastPrice)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 시장 유니버스 조회 API를 종료한다.

    @Operation(summary = "미국 공개 심볼 동기화", description = """
            NASDAQ Trader 공개 심볼 파일을 읽어 미국 시장 후보군을 저장한다.
            **사용 목적:**
            - API 키 없이 NASDAQ/NYSE 상장 종목 후보군을 자동 구성한다.
            - 이후 가격/거래량 수집 전 기본 ticker universe를 만든다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : NASDAQ/NYSE/ALL. 기본 ALL
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 소스, 시장, 조회 수, 저장 수, 샘플 키
            **주의:**
            - 외부 네트워크가 막혀 있으면 502로 실패한다.
            - 이 단계는 가격/시총/거래대금이 아니라 심볼 목록만 채운다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/sync/us-symbols") // 미국 공개 심볼 동기화 경로를 매핑한다.
    public ResultDto<?> syncUsSymbols(@RequestParam(required = false) String market) { // 미국 공개 심볼 동기화 API를 정의한다.
        return ResultDto.success(marketUniverseService.syncUsSymbols(market)); // 서비스 동기화 결과를 성공 응답으로 래핑해 반환한다.
    } // 미국 공개 심볼 동기화 API를 종료한다.

    @Operation(summary = "한국 공개 심볼 동기화", description = """
            KIND 상장법인 공개 다운로드를 읽어 KOSPI/KOSDAQ 시장 후보군을 저장한다.
            **사용 목적:**
            - 개발용 seed 없이 한국 시장 추천 후보군을 자동 구성한다.
            - 이후 KIS 일봉 동기화와 추천 feature 계산 대상이 된다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/ALL. 기본 ALL
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 소스, 시장, 조회 수, 저장 수, 샘플 키
            **주의:**
            - 외부 네트워크가 막혀 있으면 502로 실패한다.
            - 스팩/우선주로 추정되는 항목은 tradable=false로 저장한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/sync/kr-symbols") // 한국 공개 심볼 동기화 경로를 매핑한다.
    public ResultDto<?> syncKrSymbols(@RequestParam(required = false) String market) { // 한국 공개 심볼 동기화 API를 정의한다.
        return ResultDto.success(marketUniverseService.syncKrSymbols(market)); // 서비스 동기화 결과를 성공 응답으로 래핑해 반환한다.
    } // 한국 공개 심볼 동기화 API를 종료한다.

    @Operation(summary = "미국 시세 동기화", description = """
            Stooq 공개 CSV quote를 사용해 미국 후보군의 최근 가격과 거래대금을 갱신한다.
            **사용 목적:**
            - 심볼 목록만 있는 NASDAQ/NYSE 후보군에 추천 필터용 가격과 거래대금을 채운다.
            - API 키 없이 미국 추천 후보군의 기본 가격 데이터를 구성한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : NASDAQ/NYSE/ALL. 기본 ALL
            - **limit** *(Integer, 선택)* : 동기화할 최대 후보 수. 기본 50, 범위 1~500
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 소스, 시장, 조회 대상 수, 갱신 수, 샘플 키
            **주의:**
            - 이 API는 후보군이 먼저 있어야 하므로 `/api/universe/sync/us-symbols` 이후 실행한다.
            - 현재는 최근 종가와 당일 거래대금을 저장하며, 시가총액은 별도 수집 단계에서 채운다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/sync/us-prices") // 미국 시세 동기화 경로를 매핑한다.
    public ResultDto<?> syncUsPrices(@RequestParam(required = false) String market, @RequestParam(required = false) Integer limit) { // 미국 시세 동기화 API를 정의한다.
        return ResultDto.success(marketUniverseService.syncUsPrices(market, limit)); // 서비스 시세 동기화 결과를 성공 응답으로 래핑해 반환한다.
    } // 미국 시세 동기화 API를 종료한다.
} // 시장 유니버스 컨트롤러를 종료한다.
