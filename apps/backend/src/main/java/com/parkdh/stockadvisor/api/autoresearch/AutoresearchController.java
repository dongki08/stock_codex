package com.parkdh.stockadvisor.api.autoresearch; // AutoResearch API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchAutoRunRequest; // AutoResearch 자동 실행 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchRunCreateRequest; // AutoResearch 실행 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.StrategyVersionCreateRequest; // 전략 버전 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.autoresearch.AutoresearchService; // AutoResearch 서비스를 가져온다.
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

import java.util.UUID; // UUID 타입을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/autoresearch") // AutoResearch API 공통 경로를 지정한다.
public class AutoresearchController { // AutoResearch 컨트롤러를 정의한다.
    private final AutoresearchService autoresearchService; // AutoResearch 서비스 의존성을 보관한다.

    public AutoresearchController(AutoresearchService autoresearchService) { // 생성자 주입을 정의한다.
        this.autoresearchService = autoresearchService; // AutoResearch 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "AutoResearch 실행 목록 조회", description = """
            AutoResearch 야간 실험 실행 이력을 조회한다.
            **사용 목적:**
            - 실험 반복별 KEEP/DISCARD/ERROR 결과 확인
            **요청 파라미터:**
            - **jobRunId** *(UUID, 선택)* : 야간 작업 실행 UUID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 실행 ID, 반복 번호, 커밋 SHA, 지표, 결정
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - jobRunId가 없으면 전체 실험 이력을 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/runs") // AutoResearch 실행 목록 조회 경로를 매핑한다.
    public ResultDto<?> getRuns(@RequestParam(required = false) UUID jobRunId) { // AutoResearch 실행 목록 조회 API를 정의한다.
        return ResultDto.success(autoresearchService.getRuns(jobRunId)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // AutoResearch 실행 목록 조회 API를 종료한다.

    @Operation(summary = "AutoResearch 실행 단건 조회", description = """
            AutoResearch 실행 ID로 실험 상세를 조회한다.
            **사용 목적:**
            - 특정 실험의 커밋, 지표, 결정 근거 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : AutoResearch 실행 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : AutoResearch 실행 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : AutoResearch 실행을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 실행은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/runs/{id}") // AutoResearch 실행 단건 조회 경로를 매핑한다.
    public ResultDto<?> getRun(@PathVariable Long id) { // AutoResearch 실행 단건 조회 API를 정의한다.
        return ResultDto.success(autoresearchService.getRun(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // AutoResearch 실행 단건 조회 API를 종료한다.

    @Operation(summary = "AutoResearch 실행 저장", description = """
            AutoResearch 실험 반복 결과를 저장한다.
            **사용 목적:**
            - 야간 자율 실험의 반복별 성과와 결정 이력 적재
            **요청 파라미터:**
            - **jobRunId** *(UUID, 필수)* : 작업 실행 UUID
            - **iterNo** *(Integer, 필수)* : 반복 번호
            - **parentSha/proposalSha** *(String, 선택)* : 기준 및 제안 커밋 SHA
            - **metricName/metricValue** *(String/Decimal, 선택)* : 평가 지표
            - **decision** *(String, 필수)* : KEEP/DISCARD/ERROR
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 AutoResearch 실행
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - Codex 실제 실행은 별도 Job에서 수행하고 이 API는 결과 저장 계약이다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/runs") // AutoResearch 실행 저장 경로를 매핑한다.
    public ResultDto<?> createRun(@Valid @RequestBody AutoresearchRunCreateRequest request) { // AutoResearch 실행 저장 API를 정의한다.
        return ResultDto.success(autoresearchService.createRun(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // AutoResearch 실행 저장 API를 종료한다.

    @Operation(summary = "AutoResearch 자동 실행", description = """
            scoring weights 후보를 자동 생성해 추천 엔진 백테스트를 수행하고, 기존 champion보다 성과가 좋으면 새 champion 전략 버전을 저장한다.
            """)
    @PostMapping("/runs/auto")
    public ResultDto<?> runAutoResearch(@RequestBody(required = false) AutoresearchAutoRunRequest request) {
        return ResultDto.success(autoresearchService.runAutoResearch(request));
    }

    @Operation(summary = "전략 버전 목록 조회", description = """
            AutoResearch가 승격한 전략 버전 목록을 조회한다.
            **사용 목적:**
            - Champion/Challenger 전략 버전 관리
            **요청 파라미터:**
            - **champion** *(Boolean, 선택)* : 챔피언 여부
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 전략 버전 ID, semver, gitSha, 지표, 챔피언 여부
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - champion이 없으면 전체 전략 버전을 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/strategies") // 전략 버전 목록 조회 경로를 매핑한다.
    public ResultDto<?> getStrategyVersions(@RequestParam(required = false) Boolean champion) { // 전략 버전 목록 조회 API를 정의한다.
        return ResultDto.success(autoresearchService.getStrategyVersions(champion)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 전략 버전 목록 조회 API를 종료한다.

    @Operation(summary = "전략 버전 단건 조회", description = """
            전략 버전 ID로 전략 버전을 조회한다.
            **사용 목적:**
            - 특정 챔피언 전략의 커밋과 성과 지표 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 전략 버전 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 전략 버전 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 전략 버전을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 전략 버전은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/strategies/{id}") // 전략 버전 단건 조회 경로를 매핑한다.
    public ResultDto<?> getStrategyVersion(@PathVariable Long id) { // 전략 버전 단건 조회 API를 정의한다.
        return ResultDto.success(autoresearchService.getStrategyVersion(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 전략 버전 단건 조회 API를 종료한다.

    @Operation(summary = "전략 버전 저장", description = """
            새 전략 버전 또는 챔피언 전략을 저장한다.
            **사용 목적:**
            - AutoResearch 승격 결과와 롤백 기준 관리
            **요청 파라미터:**
            - **semver** *(String, 필수)* : 전략 버전명
            - **gitSha** *(String, 필수)* : 커밋 SHA
            - **metricValue** *(Decimal, 필수)* : 전략 지표 값
            - **promotedAt** *(DateTime, 선택)* : 승격 일시
            - **champion** *(Boolean, 필수)* : 챔피언 여부
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 전략 버전
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - promotedAt이 없으면 서버 현재 시각을 사용한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping("/strategies") // 전략 버전 저장 경로를 매핑한다.
    public ResultDto<?> createStrategyVersion(@Valid @RequestBody StrategyVersionCreateRequest request) { // 전략 버전 저장 API를 정의한다.
        return ResultDto.success(autoresearchService.createStrategyVersion(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 전략 버전 저장 API를 종료한다.
} // AutoResearch 컨트롤러를 종료한다.
