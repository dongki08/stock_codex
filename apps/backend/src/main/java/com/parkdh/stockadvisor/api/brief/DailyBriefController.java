package com.parkdh.stockadvisor.api.brief; // 데일리 브리프 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.brief.dto.DailyBriefCreateRequest; // 데일리 브리프 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.brief.DailyBriefService; // 데일리 브리프 서비스를 가져온다.
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
@RequestMapping("/api/briefs") // 데일리 브리프 API 공통 경로를 지정한다.
public class DailyBriefController { // 데일리 브리프 컨트롤러를 정의한다.
    private final DailyBriefService dailyBriefService; // 데일리 브리프 서비스 의존성을 보관한다.

    public DailyBriefController(DailyBriefService dailyBriefService) { // 생성자 주입을 정의한다.
        this.dailyBriefService = dailyBriefService; // 데일리 브리프 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "데일리 브리프 목록 조회", description = """
            시장별 LLM 브리프 이력을 조회한다.
            **사용 목적:**
            - KRX/US/US_CLOSE 브리프 내용과 검증 점수 확인
            **요청 파라미터:**
            - **marketTrack** *(String, 선택)* : KRX/US/US_CLOSE
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 브리프 ID, 시장 트랙, 본문, 초안 번호, 커버리지
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - marketTrack이 없으면 전체 브리프를 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 데일리 브리프 목록 조회 경로를 매핑한다.
    public ResultDto<?> getDailyBriefs(@RequestParam(required = false) String marketTrack) { // 데일리 브리프 목록 조회 API를 정의한다.
        return ResultDto.success(dailyBriefService.getDailyBriefs(marketTrack)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 데일리 브리프 목록 조회 API를 종료한다.

    @Operation(summary = "데일리 브리프 단건 조회", description = """
            데일리 브리프 ID로 브리프를 조회한다.
            **사용 목적:**
            - 특정 발송 브리프 원문 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 데일리 브리프 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 데일리 브리프 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 데일리 브리프를 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 브리프는 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 데일리 브리프 단건 조회 경로를 매핑한다.
    public ResultDto<?> getDailyBrief(@PathVariable Long id) { // 데일리 브리프 단건 조회 API를 정의한다.
        return ResultDto.success(dailyBriefService.getDailyBrief(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 데일리 브리프 단건 조회 API를 종료한다.

    @Operation(hidden = true, summary = "데일리 브리프 저장", description = """
            Codex CLI가 생성한 브리프 초안을 저장한다.
            **사용 목적:**
            - 프리오픈 브리프, 마감 요약, 초안별 품질 점수 보관
            **요청 파라미터:**
            - **marketTrack** *(String, 필수)* : KRX/US/US_CLOSE
            - **briefMd** *(String, 필수)* : 브리프 마크다운 본문
            - **draftNo** *(Integer, 필수)* : 초안 번호
            - **coverage** *(Decimal, 선택)* : 커버리지 점수
            - **hallucinationFlags** *(Integer, 선택)* : 검증 실패 플래그 수
            - **llmModel** *(String, 선택)* : 사용 모델
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 데일리 브리프
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - generatedAt이 없으면 서버 현재 시각을 사용한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 데일리 브리프 저장 경로를 매핑한다.
    public ResultDto<?> createDailyBrief(@Valid @RequestBody DailyBriefCreateRequest request) { // 데일리 브리프 저장 API를 정의한다.
        return ResultDto.success(dailyBriefService.createDailyBrief(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 데일리 브리프 저장 API를 종료한다.
} // 데일리 브리프 컨트롤러를 종료한다.
