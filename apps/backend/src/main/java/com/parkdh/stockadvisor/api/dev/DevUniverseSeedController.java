package com.parkdh.stockadvisor.api.dev; // 개발용 API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.universe.MarketUniverseService; // 시장 유니버스 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/dev/universe") // 개발용 시장 유니버스 API 공통 경로를 지정한다.
public class DevUniverseSeedController { // 개발용 시장 유니버스 seed 컨트롤러를 정의한다.
    private final MarketUniverseService marketUniverseService; // 시장 유니버스 서비스 의존성을 보관한다.

    public DevUniverseSeedController(MarketUniverseService marketUniverseService) { // 생성자 주입을 정의한다.
        this.marketUniverseService = marketUniverseService; // 시장 유니버스 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "개발용 시장 유니버스 seed", description = """
            실제 KIS/yfinance 수집기가 붙기 전 개발용 시장 후보군을 저장한다.
            **사용 목적:**
            - 종목 수동 등록 없이 추천 후보군 자동 구성 플로우를 검증한다.
            - 오늘의 추천 화면과 개발용 추천 생성 API를 빠르게 테스트한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE. 없으면 전체 개발용 후보군 저장
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장 또는 갱신한 후보 수와 유니버스 키 목록
            """) // Swagger 문서를 정의한다.
    @PostMapping("/seed") // 개발용 시장 유니버스 seed 경로를 매핑한다.
    public ResultDto<?> seed(@RequestParam(required = false) String market) { // 개발용 시장 유니버스 seed API를 정의한다.
        return ResultDto.success(marketUniverseService.seedDevUniverse(market)); // 서비스 seed 결과를 성공 응답으로 래핑해 반환한다.
    } // 개발용 시장 유니버스 seed API를 종료한다.
} // 개발용 시장 유니버스 seed 컨트롤러를 종료한다.
