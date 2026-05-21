package com.parkdh.stockadvisor.api.evaluation; // 평가 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.evaluation.dto.EvaluationCreateRequest; // 평가 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.evaluation.EvaluationService; // 평가 서비스를 가져온다.
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
@RequestMapping("/api/evaluations") // 평가 API 공통 경로를 지정한다.
public class EvaluationController { // 평가 컨트롤러를 정의한다.
    private final EvaluationService evaluationService; // 평가 서비스 의존성을 보관한다.

    public EvaluationController(EvaluationService evaluationService) { // 생성자 주입을 정의한다.
        this.evaluationService = evaluationService; // 평가 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "평가 목록 조회", description = """
            추천 평가 결과 목록을 조회한다.
            **사용 목적:**
            - 추천 사후 검증, 통계 대시보드, 손익 분석
            **요청 파라미터:**
            - **recommendationId** *(Long, 선택)* : 추천 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 평가 ID, 추천 ID, 매도 가격, 청산 사유, 손익률
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - recommendationId가 없으면 전체 평가를 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 평가 목록 조회 경로를 매핑한다.
    public ResultDto<?> getEvaluations(@RequestParam(required = false) Long recommendationId) { // 평가 목록 조회 API를 정의한다.
        return ResultDto.success(evaluationService.getEvaluations(recommendationId)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 평가 목록 조회 API를 종료한다.

    @Operation(summary = "평가 단건 조회", description = """
            평가 ID로 평가 결과를 조회한다.
            **사용 목적:**
            - 추천 상세의 사후 평가 정보 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 평가 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 평가 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 평가를 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 평가는 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 평가 단건 조회 경로를 매핑한다.
    public ResultDto<?> getEvaluation(@PathVariable Long id) { // 평가 단건 조회 API를 정의한다.
        return ResultDto.success(evaluationService.getEvaluation(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 평가 단건 조회 API를 종료한다.

    @Operation(summary = "평가 생성", description = """
            추천의 실제 결과를 평가 테이블에 저장한다.
            **사용 목적:**
            - 목표가 도달, 손절, 시간 만료 결과 기록
            **요청 파라미터:**
            - **recommendationId** *(Long, 필수)* : 추천 ID
            - **actualExitPrice** *(Decimal, 선택)* : 실제 매도 가격
            - **exitReason** *(String, 필수)* : TARGET_HIT/STOP_HIT/TIME_OUT/MANUAL_CLOSE
            - **pnlPct** *(Decimal, 필수)* : 손익률
            - **drawdownPct** *(Decimal, 선택)* : 최대 낙폭
            - **hitTarget** *(Boolean, 필수)* : 목표가 적중 여부
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 평가 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 추천이 존재하지 않으면 평가를 생성하지 않는다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 평가 생성 경로를 매핑한다.
    public ResultDto<?> createEvaluation(@Valid @RequestBody EvaluationCreateRequest request) { // 평가 생성 API를 정의한다.
        return ResultDto.success(evaluationService.createEvaluation(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 평가 생성 API를 종료한다.
} // 평가 컨트롤러를 종료한다.
