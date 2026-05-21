package com.parkdh.stockadvisor.api.dev; // 개발용 API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.dev.DevRecommendationGenerateService; // 개발용 추천 생성 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/dev/recommendations") // 개발용 추천 API 공통 경로를 지정한다.
public class DevRecommendationGenerateController { // 개발용 추천 생성 컨트롤러를 정의한다.
    private final DevRecommendationGenerateService devRecommendationGenerateService; // 개발용 추천 생성 서비스 의존성을 보관한다.

    public DevRecommendationGenerateController(DevRecommendationGenerateService devRecommendationGenerateService) { // 생성자 주입을 정의한다.
        this.devRecommendationGenerateService = devRecommendationGenerateService; // 개발용 추천 생성 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "개발용 추천 자동 생성", description = """
            등록된 활성 종목을 기준으로 개발용 가격 예측과 추천을 자동 생성한다.
            **사용 목적:**
            - 실제 시세 수집과 추천 엔진 구현 전 프론트 추천 화면과 API 흐름 테스트
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NYSE/NASDAQ 등 시장 구분. 없으면 전체 활성 종목 사용
            - **shortCount** *(Integer, 선택)* : 단기 추천 생성 개수. 기본 3, 범위 1~10
            - **longCount** *(Integer, 선택)* : 장기 추천 생성 개수. 기본 3, 범위 1~10
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 대상 시장, 원천 종목 수, 생성된 예측/추천 수와 ID 목록
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 실제 투자 로직이 아닌 dev-rule-v0 개발용 더미 룰이다.
            - 먼저 /api/instruments에 enabled=true 종목이 등록되어 있어야 한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/generate") // 개발용 추천 자동 생성 경로를 매핑한다.
    public ResultDto<?> generate(@RequestParam(required = false) String market, @RequestParam(required = false) Integer shortCount, @RequestParam(required = false) Integer longCount) { // 개발용 추천 자동 생성 API를 정의한다.
        return ResultDto.success(devRecommendationGenerateService.generate(market, shortCount, longCount)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 개발용 추천 자동 생성 API를 종료한다.
} // 개발용 추천 생성 컨트롤러를 종료한다.
