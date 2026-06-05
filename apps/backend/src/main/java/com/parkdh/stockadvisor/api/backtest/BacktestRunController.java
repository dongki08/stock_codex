package com.parkdh.stockadvisor.api.backtest; // 백테스트 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.backtest.dto.BacktestRunCreateRequest; // 백테스트 실행 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.backtest.dto.BacktestSimulationRequest;
import com.parkdh.stockadvisor.application.backtest.BacktestRunService; // 백테스트 실행 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import jakarta.validation.Valid; // 요청 검증 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestBody; // 요청 본문 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/backtests") // 백테스트 API 공통 경로를 지정한다.
public class BacktestRunController { // 백테스트 실행 컨트롤러를 정의한다.
    private final BacktestRunService backtestRunService; // 백테스트 실행 서비스 의존성을 보관한다.

    public BacktestRunController(BacktestRunService backtestRunService) { // 생성자 주입을 정의한다.
        this.backtestRunService = backtestRunService; // 백테스트 실행 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "백테스트 실행 목록 조회", description = """
            백테스트 실행 이력을 조회한다.
            **사용 목적:**
            - 전략 검증 결과와 관리자 백테스트 UI 연동
            **요청 파라미터:**
            - 없음
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 실행 ID, 전략명, 기간, 지표 JSON
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 최근 실행이 먼저 오도록 ID 역순으로 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 백테스트 실행 목록 조회 경로를 매핑한다.
    public ResultDto<?> getBacktestRuns() { // 백테스트 실행 목록 조회 API를 정의한다.
        return ResultDto.success(backtestRunService.getBacktestRuns()); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 백테스트 실행 목록 조회 API를 종료한다.

    @Operation(summary = "백테스트 실행 단건 조회", description = """
            백테스트 실행 ID로 결과를 조회한다.
            **사용 목적:**
            - 특정 전략 검증 결과 상세 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 백테스트 실행 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 백테스트 실행 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 백테스트 실행을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 실행은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 백테스트 실행 단건 조회 경로를 매핑한다.
    public ResultDto<?> getBacktestRun(@PathVariable Long id) { // 백테스트 실행 단건 조회 API를 정의한다.
        return ResultDto.success(backtestRunService.getBacktestRun(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 백테스트 실행 단건 조회 API를 종료한다.

    @Operation(hidden = true, summary = "백테스트 실행 저장", description = """
            백테스트 엔진이 계산한 실행 결과를 저장한다.
            **사용 목적:**
            - 전략별 과거 성과와 AutoResearch 평가 지표 기록
            **요청 파라미터:**
            - **strategy** *(String, 필수)* : 전략명
            - **periodFrom** *(Date, 필수)* : 기간 시작일
            - **periodTo** *(Date, 필수)* : 기간 종료일
            - **metricsJson** *(String, 필수)* : ROI, HitRate, MDD 등 지표 JSON
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 백테스트 실행 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 시작일이 종료일보다 늦으면 400으로 응답한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 백테스트 실행 저장 경로를 매핑한다.
    public ResultDto<?> createBacktestRun(@Valid @RequestBody BacktestRunCreateRequest request) { // 백테스트 실행 저장 API를 정의한다.
        return ResultDto.success(backtestRunService.createBacktestRun(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 백테스트 실행 저장 API를 종료한다.

    @Operation(summary = "일봉 기반 백테스트 실행", description = """
            저장된 price_daily 일봉 데이터로 간단한 룰 기반 백테스트를 실행하고 결과를 저장한다.
            **전략 기본값:**
            - 20일 이동평균 이상이면 다음 거래일 종가 진입
            - targetPct 도달, stopPct 이탈, holdingDays 만료 중 먼저 발생한 조건으로 청산
            **요청 필드:**
            - **strategy** *(String, 선택)* : 전략명. 기본 ma20-breakout-v0
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NASDAQ/NYSE/ALL. 기본 ALL
            - **periodFrom / periodTo** *(Date, 필수)* : 백테스트 기간
            - **maxTickers** *(Integer, 선택)* : 최대 종목 수. 기본 30, 최대 300
            - **holdingDays** *(Integer, 선택)* : 최대 보유일. 기본 20
            - **targetPct / stopPct** *(Decimal, 선택)* : 목표/손절 퍼센트. 기본 3.0 / 2.0
            """)
    @PostMapping("/simulate")
    public ResultDto<?> simulateBacktest(@Valid @RequestBody BacktestSimulationRequest request) {
        return ResultDto.success(backtestRunService.simulateBacktest(request));
    }
} // 백테스트 실행 컨트롤러를 종료한다.
