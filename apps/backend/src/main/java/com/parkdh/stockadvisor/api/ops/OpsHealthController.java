package com.parkdh.stockadvisor.api.ops; // 운영 API 패키지를 선언한다.

import com.parkdh.stockadvisor.application.ops.ExternalHealthService; // 외부 연동 상태 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Operation; // Swagger Operation 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RequestMapping; // 공통 경로 매핑 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 어노테이션을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/ops") // 운영 API 공통 경로를 지정한다.
public class OpsHealthController { // 운영 상태 컨트롤러를 정의한다.
    private final ExternalHealthService externalHealthService; // 외부 연동 상태 서비스 의존성을 보관한다.

    @Operation(summary = "외부 연동 상태 조회", description = """
            KIS, Telegram, Codex, Stooq, KIND 연동 설정 상태를 조회한다.
            이 API는 `/api/ops/**` 보호 정책에 따라 BasicAuth가 필요하다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/external-health") // 외부 연동 상태 조회 경로를 매핑한다.
    public ResultDto<?> getExternalHealth() { // 외부 연동 상태 조회 API를 정의한다.
        return ResultDto.success(externalHealthService.getExternalHealth()); // 서비스 조회 결과를 성공 응답으로 반환한다.
    } // 외부 연동 상태 조회 API를 종료한다.
} // 운영 상태 컨트롤러를 종료한다.
