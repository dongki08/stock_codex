package com.parkdh.stockadvisor.api.feature; // 후보군 feature API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder; // 후보군 feature 빌더를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/features") // feature API 공통 경로를 지정한다.
public class UniverseFeatureController { // 후보군 feature 컨트롤러를 정의한다.
    private final UniverseFeatureBuilder universeFeatureBuilder; // 후보군 feature 빌더 의존성을 보관한다.

    public UniverseFeatureController(UniverseFeatureBuilder universeFeatureBuilder) { // 생성자 주입을 정의한다.
        this.universeFeatureBuilder = universeFeatureBuilder; // 후보군 feature 빌더를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "시장 후보군 feature 조회", description = """
            시장 후보군을 단순 룰 기반 feature 점수로 변환해 조회한다.
            **사용 목적:**
            - 추천 엔진이 어떤 후보를 우선 선택하는지 확인한다.
            - 가격/거래대금 데이터가 추천 점수에 반영되는지 검증한다.
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE/ALL
            - **limit** *(Integer, 선택)* : 최대 응답 수. 기본 50
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 후보 종목, 가격, 거래대금, 유동성 점수, 가격 점수, 데이터 품질 점수, 종합 점수
            **특징:**
            - 현재는 feature-rule-v0 개발용 룰이며, 이후 RSI/MACD/뉴스/펀더멘털 feature로 확장한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/universe") // 후보군 feature 조회 경로를 매핑한다.
    public ResultDto<?> getUniverseFeatures(@RequestParam(required = false) String market, @RequestParam(required = false) Integer limit) { // 후보군 feature 조회 API를 정의한다.
        return ResultDto.success(universeFeatureBuilder.getFeatures(market, limit)); // feature 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 후보군 feature 조회 API를 종료한다.
} // 후보군 feature 컨트롤러를 종료한다.
