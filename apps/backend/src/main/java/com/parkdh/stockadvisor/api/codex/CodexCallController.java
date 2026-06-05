package com.parkdh.stockadvisor.api.codex; // Codex 호출 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.codex.dto.CodexCallCreateRequest; // Codex 호출 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.codex.CodexCallService; // Codex 호출 서비스를 가져온다.
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
@RequestMapping("/api/codex/calls") // Codex 호출 API 공통 경로를 지정한다.
public class CodexCallController { // Codex 호출 컨트롤러를 정의한다.
    private final CodexCallService codexCallService; // Codex 호출 서비스 의존성을 보관한다.

    public CodexCallController(CodexCallService codexCallService) { // 생성자 주입을 정의한다.
        this.codexCallService = codexCallService; // Codex 호출 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "Codex 호출 로그 목록 조회", description = """
            Codex CLI 호출 감사 로그를 조회한다.
            **사용 목적:**
            - Daily Brief, Exit Confirm, AutoResearch 호출량과 실패 추적
            **요청 파라미터:**
            - **caller** *(String, 선택)* : BRIEF_KR/BRIEF_US/EXIT_CONFIRM 등 호출자
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 호출 ID, 호출자, 프롬프트 해시, 길이, 성공 여부
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 프롬프트 원문은 저장하지 않고 해시와 길이만 저장한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // Codex 호출 로그 목록 조회 경로를 매핑한다.
    public ResultDto<?> getCodexCalls(@RequestParam(required = false) String caller) { // Codex 호출 로그 목록 조회 API를 정의한다.
        return ResultDto.success(codexCallService.getCodexCalls(caller)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // Codex 호출 로그 목록 조회 API를 종료한다.

    @Operation(summary = "Codex 호출 로그 단건 조회", description = """
            Codex 호출 로그 ID로 호출 기록을 조회한다.
            **사용 목적:**
            - 특정 LLM 호출 실패 원인 및 지연 시간 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : Codex 호출 로그 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : Codex 호출 로그 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : Codex 호출 로그를 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 호출 로그는 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // Codex 호출 로그 단건 조회 경로를 매핑한다.
    public ResultDto<?> getCodexCall(@PathVariable Long id) { // Codex 호출 로그 단건 조회 API를 정의한다.
        return ResultDto.success(codexCallService.getCodexCall(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // Codex 호출 로그 단건 조회 API를 종료한다.

    @Operation(hidden = true, summary = "Codex 호출 로그 저장", description = """
            Codex CLI 호출 결과를 저장한다.
            **사용 목적:**
            - LLM 호출 한도, 지연, 실패율, 도구 사용 여부 감사
            **요청 파라미터:**
            - **caller** *(String, 필수)* : 호출자
            - **promptHash** *(String, 필수)* : 프롬프트 해시
            - **promptLen** *(Integer, 필수)* : 프롬프트 길이
            - **responseLen** *(Integer, 선택)* : 응답 길이
            - **toolsUsedJson** *(String, 선택)* : 사용 도구 JSON
            - **durationMs** *(Integer, 선택)* : 소요 시간
            - **succeeded** *(Boolean, 필수)* : 성공 여부
            - **errorMessage** *(String, 선택)* : 실패 메시지
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 Codex 호출 로그
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - toolsUsedJson이 입력되면 JSON 형식을 검증한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // Codex 호출 로그 저장 경로를 매핑한다.
    public ResultDto<?> createCodexCall(@Valid @RequestBody CodexCallCreateRequest request) { // Codex 호출 로그 저장 API를 정의한다.
        return ResultDto.success(codexCallService.createCodexCall(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // Codex 호출 로그 저장 API를 종료한다.
} // Codex 호출 컨트롤러를 종료한다.
