package com.parkdh.stockadvisor.api.prediction; // 예측 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.prediction.dto.PredictionCreateRequest; // 예측 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.prediction.PredictionService; // 예측 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import jakarta.validation.Valid; // 요청 검증 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestBody; // 요청 본문 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestParam; // 요청 파라미터 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/predictions") // 예측 API 공통 경로를 지정한다.
public class PredictionController { // 예측 컨트롤러를 정의한다.
    private final PredictionService predictionService; // 예측 서비스 의존성을 보관한다.

    public PredictionController(PredictionService predictionService) { // 생성자 주입을 정의한다.
        this.predictionService = predictionService; // 예측 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "가격 예측 목록 조회", description = """
            가격 예측 결과 목록을 조회한다.
            **사용 목적:**
            - 추천 생성 근거와 모델 산출물 확인
            **요청 파라미터:**
            - **ticker** *(String, 선택)* : 종목 코드
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 예측 ID, 종목, 예측 기간, 예측 가격, 모델 버전
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - ticker가 없으면 전체 예측을 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 예측 목록 조회 경로를 매핑한다.
    public ResultDto<?> getPredictions(@RequestParam(required = false) String ticker) { // 예측 목록 조회 API를 정의한다.
        return ResultDto.success(predictionService.getPredictions(ticker)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 예측 목록 조회 API를 종료한다.

    @Operation(summary = "가격 예측 단건 조회", description = """
            예측 ID로 가격 예측 결과를 조회한다.
            **사용 목적:**
            - 특정 예측 산출물 상세 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 예측 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 가격 예측 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 예측을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 예측은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 예측 단건 조회 경로를 매핑한다.
    public ResultDto<?> getPrediction(@PathVariable Long id) { // 예측 단건 조회 API를 정의한다.
        return ResultDto.success(predictionService.getPrediction(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 예측 단건 조회 API를 종료한다.

    @Operation(summary = "가격 예측 생성", description = """
            가격 예측 모델 산출물을 저장한다.
            **사용 목적:**
            - 추천 생성 전후 예측 결과 추적
            **요청 파라미터:**
            - **ticker** *(String, 필수)* : 등록된 종목 코드
            - **horizonDays** *(Integer, 필수)* : 예측 기간 일수
            - **predictedPrice** *(Decimal, 필수)* : 예측 가격
            - **modelVersion** *(String, 필수)* : 모델 버전
            - **generatedAt** *(DateTime, 선택)* : 생성 일시
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 예측 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 등록되지 않은 종목은 예측 저장을 거부한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 예측 생성 경로를 매핑한다.
    public ResultDto<?> createPrediction(@Valid @RequestBody PredictionCreateRequest request) { // 예측 생성 API를 정의한다.
        return ResultDto.success(predictionService.createPrediction(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 예측 생성 API를 종료한다.
} // 예측 컨트롤러를 종료한다.
