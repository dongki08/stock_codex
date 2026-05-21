package com.parkdh.stockadvisor.api.admin; // 관리자 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.admin.dto.AdminSettingUpdateRequest; // 설정 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.admin.AdminSettingService; // 관리자 설정 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import jakarta.validation.Valid; // 요청 검증 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.PutMapping; // PUT 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestBody; // 요청 본문 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/admin") // 관리자 API 공통 경로를 지정한다.
public class AdminSettingController { // 관리자 설정 컨트롤러를 정의한다.
    private final AdminSettingService adminSettingService; // 관리자 설정 서비스 의존성을 보관한다.

    public AdminSettingController(AdminSettingService adminSettingService) { // 생성자 주입을 정의한다.
        this.adminSettingService = adminSettingService; // 관리자 설정 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation( // Swagger 문서 정보를 정의한다.
            summary = "관리자 설정 목록 조회", // API 요약을 작성한다.
            description = """
                    관리자 페이지에서 수정 가능한 전체 설정 목록을 조회한다.
                    **사용 목적:**
                    - 프론트엔드 관리자 화면의 설정 폼 렌더링
                    **요청 파라미터:**
                    - 없음
                    **반환 필드 (성공):**
                    - **code** *(Integer)* : 200 (성공)
                    - **data** *(Array)* : 설정 키, 설정 값 JSON, 설명, 수정자 목록
                    **반환 필드 (에러):**
                    - **code** *(Integer)* : 커스텀 에러 코드
                    - **error_message** *(String)* : 에러 상세 메시지
                    **특징:**
                    - 설정 키 기준 오름차순으로 반환한다.
                    """
    ) // Swagger 문서 정의를 종료한다.
    @GetMapping("/settings") // 설정 목록 조회 경로를 매핑한다.
    public ResultDto<?> getSettings() { // 설정 목록 조회 API를 정의한다.
        return ResultDto.success(adminSettingService.getSettings()); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 설정 목록 조회 API를 종료한다.

    @Operation( // Swagger 문서 정보를 정의한다.
            summary = "관리자 설정 단건 조회", // API 요약을 작성한다.
            description = """
                    설정 키로 단일 설정을 조회한다.
                    **사용 목적:**
                    - 특정 설정 편집 전 현재 값 확인
                    **요청 파라미터:**
                    - **key** *(String, 필수)* : 설정 키
                    **반환 필드 (성공):**
                    - **code** *(Integer)* : 200 (성공)
                    - **data** *(Object)* : 설정 키, 설정 값 JSON, 설명, 수정자
                    **반환 필드 (에러):**
                    - **code** *(Integer)* : 404
                    - **error_message** *(String)* : 설정을 찾을 수 없습니다.
                    **특징:**
                    - 존재하지 않는 키는 404로 응답한다.
                    """
    ) // Swagger 문서 정의를 종료한다.
    @GetMapping("/settings/{key}") // 설정 단건 조회 경로를 매핑한다.
    public ResultDto<?> getSetting(@PathVariable String key) { // 설정 단건 조회 API를 정의한다.
        return ResultDto.success(adminSettingService.getSetting(key)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 설정 단건 조회 API를 종료한다.

    @Operation( // Swagger 문서 정보를 정의한다.
            summary = "관리자 설정 수정", // API 요약을 작성한다.
            description = """
                    설정 키에 해당하는 JSON 값을 수정하고 감사 로그를 저장한다.
                    **사용 목적:**
                    - 관리자 페이지에서 런타임 설정 변경
                    **요청 파라미터:**
                    - **key** *(String, 필수)* : 설정 키
                    - **valueJson** *(String, 필수)* : 저장할 JSON 문자열
                    - **actor** *(String, 필수)* : 수정자
                    **반환 필드 (성공):**
                    - **code** *(Integer)* : 200 (성공)
                    - **data** *(Object)* : 수정된 설정 정보
                    **반환 필드 (에러):**
                    - **code** *(Integer)* : 400 또는 404
                    - **error_message** *(String)* : 에러 상세 메시지
                    **특징:**
                    - JSON 형식이 올바르지 않으면 400으로 응답한다.
                    - 존재하지 않는 키는 404로 응답한다.
                    """
    ) // Swagger 문서 정의를 종료한다.
    @PutMapping("/settings/{key}") // 설정 수정 경로를 매핑한다.
    public ResultDto<?> updateSetting(@PathVariable String key, @Valid @RequestBody AdminSettingUpdateRequest request) { // 설정 수정 API를 정의한다.
        return ResultDto.success(adminSettingService.updateSetting(key, request)); // 서비스 수정 결과를 성공 응답으로 래핑해 반환한다.
    } // 설정 수정 API를 종료한다.

    @Operation( // Swagger 문서 정보를 정의한다.
            summary = "관리자 기본 설정 초기화", // API 요약을 작성한다.
            description = """
                    초기 운영에 필요한 기본 설정 값을 저장한다.
                    **사용 목적:**
                    - 최초 실행 또는 설정 복구 시 기본값 생성
                    **요청 파라미터:**
                    - 없음
                    **반환 필드 (성공):**
                    - **code** *(Integer)* : 200 (성공)
                    - **data** *(Array)* : 저장된 기본 설정 목록
                    **반환 필드 (에러):**
                    - **code** *(Integer)* : 커스텀 에러 코드
                    - **error_message** *(String)* : 에러 상세 메시지
                    **특징:**
                    - 기본 설정 저장 후 감사 로그를 남긴다.
                    """
    ) // Swagger 문서 정의를 종료한다.
    @PostMapping("/settings/reset") // 기본 설정 초기화 경로를 매핑한다.
    public ResultDto<?> resetSettings() { // 기본 설정 초기화 API를 정의한다.
        return ResultDto.success(adminSettingService.resetSettings()); // 서비스 초기화 결과를 성공 응답으로 래핑해 반환한다.
    } // 기본 설정 초기화 API를 종료한다.

    @Operation( // Swagger 문서 정보를 정의한다.
            summary = "관리자 감사 로그 조회", // API 요약을 작성한다.
            description = """
                    관리자 설정 변경 이력을 조회한다.
                    **사용 목적:**
                    - 설정 변경 추적 및 운영 감사
                    **요청 파라미터:**
                    - 없음
                    **반환 필드 (성공):**
                    - **code** *(Integer)* : 200 (성공)
                    - **data** *(Array)* : 감사 로그 ID, 수행자, 작업명, 변경 전 JSON, 변경 후 JSON
                    **반환 필드 (에러):**
                    - **code** *(Integer)* : 커스텀 에러 코드
                    - **error_message** *(String)* : 에러 상세 메시지
                    **특징:**
                    - 최신 감사 로그가 먼저 오도록 반환한다.
                    """
    ) // Swagger 문서 정의를 종료한다.
    @GetMapping("/audit-logs") // 감사 로그 조회 경로를 매핑한다.
    public ResultDto<?> getAuditLogs() { // 감사 로그 조회 API를 정의한다.
        return ResultDto.success(adminSettingService.getAuditLogs()); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 감사 로그 조회 API를 종료한다.
} // 관리자 설정 컨트롤러를 종료한다.
