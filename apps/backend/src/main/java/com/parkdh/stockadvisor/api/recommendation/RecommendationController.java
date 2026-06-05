package com.parkdh.stockadvisor.api.recommendation; // 추천 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.recommendation.dto.RecommendationCreateRequest; // 추천 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.recommendation.dto.RecommendationStatusUpdateRequest; // 추천 상태 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.recommendation.RecommendationService; // 추천 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import jakarta.validation.Valid; // 요청 검증 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PutMapping; // PUT 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestBody; // 요청 본문 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/recommendations") // 추천 API 공통 경로를 지정한다.
public class RecommendationController { // 추천 컨트롤러를 정의한다.
    private final RecommendationService recommendationService; // 추천 서비스 의존성을 보관한다.

    public RecommendationController(RecommendationService recommendationService) { // 생성자 주입을 정의한다.
        this.recommendationService = recommendationService; // 추천 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "추천 목록 조회", description = """
            추천 결과 목록을 조회한다.
            **사용 목적:**
            - 오늘의 추천, 추천 이력, 상태별 추천 관리
            **요청 파라미터:**
            - **status** *(String, 선택)* : OPEN/CLOSED/EXPIRED
            - **ticker** *(String, 선택)* : 종목 코드
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 추천 ID, 종목, 가격, 신뢰도, 시그널 JSON, 상태
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - ticker 조건이 status 조건보다 우선 적용된다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 추천 목록 조회 경로를 매핑한다.
    public ResultDto<?> getRecommendations(@RequestParam(required = false) String status, @RequestParam(required = false) String ticker) { // 추천 목록 조회 API를 정의한다.
        return ResultDto.success(recommendationService.getRecommendations(status, ticker)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 추천 목록 조회 API를 종료한다.

    @Operation(summary = "추천 단건 조회", description = """
            추천 ID로 추천 결과를 조회한다.
            **사용 목적:**
            - 추천 상세 화면 및 평가 연결
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 추천 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 추천 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 추천을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 추천은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 추천 단건 조회 경로를 매핑한다.
    public ResultDto<?> getRecommendation(@PathVariable Long id) { // 추천 단건 조회 API를 정의한다.
        return ResultDto.success(recommendationService.getRecommendation(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 추천 단건 조회 API를 종료한다.

    @Operation(hidden = true, summary = "추천 생성", description = """
            추천 엔진 또는 운영자가 추천 결과를 저장한다.
            **사용 목적:**
            - KRX/US 프리오픈 추천 결과 DB 적재
            **요청 파라미터:**
            - **ticker** *(String, 필수)* : 등록된 종목 코드
            - **market** *(String, 필수)* : 시장 구분
            - **term** *(String, 필수)* : SHORT 또는 LONG
            - **entryPrice/targetPrice/stopPrice** *(Decimal, 필수)* : 진입·목표·손절 가격
            - **expectedExitAt** *(Date, 필수)* : 예상 매도일
            - **confidence** *(Integer, 필수)* : 0~100 신뢰도
            - **signalsJson** *(String, 필수)* : 시그널 원본 JSON
            - **modelVersion** *(String, 필수)* : 모델 버전
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 추천 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 추천 상태는 생성 시 OPEN으로 저장한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 추천 생성 경로를 매핑한다.
    public ResultDto<?> createRecommendation(@Valid @RequestBody RecommendationCreateRequest request) { // 추천 생성 API를 정의한다.
        return ResultDto.success(recommendationService.createRecommendation(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 추천 생성 API를 종료한다.

    @Operation(hidden = true, summary = "추천 상태 수정", description = """
            추천의 상태를 OPEN/CLOSED/EXPIRED 중 하나로 변경한다.
            **사용 목적:**
            - 목표가 도달, 손절, 시간 만료 평가 후 추천 상태 반영
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 추천 ID
            - **status** *(String, 필수)* : OPEN/CLOSED/EXPIRED
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 수정된 추천 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 허용되지 않은 상태는 400으로 응답한다.
            """) // Swagger 문서를 정의한다.
    @PutMapping("/{id}/status") // 추천 상태 수정 경로를 매핑한다.
    public ResultDto<?> updateStatus(@PathVariable Long id, @Valid @RequestBody RecommendationStatusUpdateRequest request) { // 추천 상태 수정 API를 정의한다.
        return ResultDto.success(recommendationService.updateStatus(id, request)); // 서비스 수정 결과를 성공 응답으로 래핑해 반환한다.
    } // 추천 상태 수정 API를 종료한다.

} // 추천 컨트롤러를 종료한다.
