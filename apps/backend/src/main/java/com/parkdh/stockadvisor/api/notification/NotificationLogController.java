package com.parkdh.stockadvisor.api.notification; // 알림 로그 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.notification.dto.NotificationLogCreateRequest; // 알림 로그 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.notification.NotificationLogService; // 알림 로그 서비스를 가져온다.
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
@RequestMapping("/api/notifications/logs") // 알림 로그 API 공통 경로를 지정한다.
public class NotificationLogController { // 알림 로그 컨트롤러를 정의한다.
    private final NotificationLogService notificationLogService; // 알림 로그 서비스 의존성을 보관한다.

    public NotificationLogController(NotificationLogService notificationLogService) { // 생성자 주입을 정의한다.
        this.notificationLogService = notificationLogService; // 알림 로그 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "알림 로그 목록 조회", description = """
            Telegram/Kakao 등 알림 발송 로그를 조회한다.
            **사용 목적:**
            - 알림 발송 신뢰성 확인 및 운영 감사
            **요청 파라미터:**
            - **status** *(String, 선택)* : SENT/FAILED/SKIPPED 등 발송 상태
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 알림 로그 ID, 채널, 페이로드 해시, 발송 일시, 상태
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - status가 없으면 전체 로그를 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 알림 로그 목록 조회 경로를 매핑한다.
    public ResultDto<?> getNotificationLogs(@RequestParam(required = false) String status) { // 알림 로그 목록 조회 API를 정의한다.
        return ResultDto.success(notificationLogService.getNotificationLogs(status)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 알림 로그 목록 조회 API를 종료한다.

    @Operation(summary = "알림 로그 단건 조회", description = """
            알림 로그 ID로 발송 기록을 조회한다.
            **사용 목적:**
            - 특정 알림 발송 실패 원인 확인
            **요청 파라미터:**
            - **id** *(Long, 필수)* : 알림 로그 ID
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 알림 로그 상세 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 알림 로그를 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 알림 로그는 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{id}") // 알림 로그 단건 조회 경로를 매핑한다.
    public ResultDto<?> getNotificationLog(@PathVariable Long id) { // 알림 로그 단건 조회 API를 정의한다.
        return ResultDto.success(notificationLogService.getNotificationLog(id)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 알림 로그 단건 조회 API를 종료한다.

    @Operation(summary = "알림 로그 저장", description = """
            알림 발송 시도 결과를 저장한다.
            **사용 목적:**
            - Telegram/Kakao 발송 결과와 실패 사유 기록
            **요청 파라미터:**
            - **channel** *(String, 필수)* : 알림 채널
            - **payloadHash** *(String, 필수)* : 페이로드 해시
            - **sentAt** *(DateTime, 선택)* : 발송 일시
            - **status** *(String, 필수)* : SENT/FAILED/SKIPPED 등 상태
            - **errorMessage** *(String, 선택)* : 실패 메시지
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 저장된 알림 로그
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - sentAt이 없으면 서버 현재 시각을 사용한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 알림 로그 저장 경로를 매핑한다.
    public ResultDto<?> createNotificationLog(@Valid @RequestBody NotificationLogCreateRequest request) { // 알림 로그 저장 API를 정의한다.
        return ResultDto.success(notificationLogService.createNotificationLog(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 알림 로그 저장 API를 종료한다.
} // 알림 로그 컨트롤러를 종료한다.
