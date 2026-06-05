package com.parkdh.stockadvisor.api.instrument; // 종목 API 패키지를 선언한다.

import com.parkdh.stockadvisor.api.instrument.dto.InstrumentCreateRequest; // 종목 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.instrument.dto.InstrumentUpdateRequest; // 종목 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.instrument.InstrumentService; // 종목 서비스를 가져온다.
import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import io.swagger.v3.oas.annotations.Hidden;
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

@Hidden
@RestController // REST API 컨트롤러로 등록한다.
@RequestMapping("/api/instruments") // 종목 API 공통 경로를 지정한다.
public class InstrumentController { // 종목 컨트롤러를 정의한다.
    private final InstrumentService instrumentService; // 종목 서비스 의존성을 보관한다.

    public InstrumentController(InstrumentService instrumentService) { // 생성자 주입을 정의한다.
        this.instrumentService = instrumentService; // 종목 서비스를 저장한다.
    } // 생성자를 종료한다.

    @Operation(summary = "종목 목록 조회", description = """
            종목 마스터 목록을 조회한다.
            **사용 목적:**
            - 추천·예측·백테스트 대상 유니버스 확인
            **요청 파라미터:**
            - **market** *(String, 선택)* : KOSPI/KOSDAQ/NYSE/NASDAQ 등 시장 구분
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Array)* : 종목 코드, 시장, 종목명, 섹터, 활성 여부
            **반환 필드 (에러):**
            - **code** *(Integer)* : 커스텀 에러 코드
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - market이 없으면 전체 종목을 티커 오름차순으로 반환한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping // 종목 목록 조회 경로를 매핑한다.
    public ResultDto<?> getInstruments(@RequestParam(required = false) String market) { // 종목 목록 조회 API를 정의한다.
        return ResultDto.success(instrumentService.getInstruments(market)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 종목 목록 조회 API를 종료한다.

    @Operation(summary = "종목 단건 조회", description = """
            종목 코드로 종목 마스터를 조회한다.
            **사용 목적:**
            - 추천 생성 전 종목 등록 상태 확인
            **요청 파라미터:**
            - **ticker** *(String, 필수)* : 종목 코드
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 종목 코드, 시장, 종목명, 섹터, 활성 여부
            **반환 필드 (에러):**
            - **code** *(Integer)* : 404
            - **error_message** *(String)* : 종목을 찾을 수 없습니다.
            **특징:**
            - 존재하지 않는 종목은 404로 응답한다.
            """) // Swagger 문서를 정의한다.
    @GetMapping("/{ticker}") // 종목 단건 조회 경로를 매핑한다.
    public ResultDto<?> getInstrument(@PathVariable String ticker) { // 종목 단건 조회 API를 정의한다.
        return ResultDto.success(instrumentService.getInstrument(ticker)); // 서비스 조회 결과를 성공 응답으로 래핑해 반환한다.
    } // 종목 단건 조회 API를 종료한다.

    @Operation(summary = "종목 등록", description = """
            추천 유니버스에 사용할 종목을 등록한다.
            **사용 목적:**
            - KOSPI/KOSDAQ/NYSE/NASDAQ 종목 마스터 구성
            **요청 파라미터:**
            - **ticker** *(String, 필수)* : 종목 코드
            - **market** *(String, 필수)* : 시장 구분
            - **name** *(String, 필수)* : 종목명
            - **sector** *(String, 선택)* : 섹터
            - **enabled** *(Boolean, 필수)* : 활성 여부
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 등록된 종목 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 409
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - 이미 등록된 ticker는 409로 응답한다.
            """) // Swagger 문서를 정의한다.
    @PostMapping // 종목 등록 경로를 매핑한다.
    public ResultDto<?> createInstrument(@Valid @RequestBody InstrumentCreateRequest request) { // 종목 등록 API를 정의한다.
        return ResultDto.success(instrumentService.createInstrument(request)); // 서비스 생성 결과를 성공 응답으로 래핑해 반환한다.
    } // 종목 등록 API를 종료한다.

    @Operation(summary = "종목 수정", description = """
            등록된 종목의 시장, 이름, 섹터, 활성 여부를 수정한다.
            **사용 목적:**
            - 종목 마스터 정정 및 추천 유니버스 ON/OFF 제어
            **요청 파라미터:**
            - **ticker** *(String, 필수)* : 종목 코드
            - **market** *(String, 필수)* : 시장 구분
            - **name** *(String, 필수)* : 종목명
            - **sector** *(String, 선택)* : 섹터
            - **enabled** *(Boolean, 필수)* : 활성 여부
            **반환 필드 (성공):**
            - **code** *(Integer)* : 200 (성공)
            - **data** *(Object)* : 수정된 종목 정보
            **반환 필드 (에러):**
            - **code** *(Integer)* : 400 또는 404
            - **error_message** *(String)* : 에러 상세 메시지
            **특징:**
            - ticker 자체는 변경하지 않는다.
            """) // Swagger 문서를 정의한다.
    @PutMapping("/{ticker}") // 종목 수정 경로를 매핑한다.
    public ResultDto<?> updateInstrument(@PathVariable String ticker, @Valid @RequestBody InstrumentUpdateRequest request) { // 종목 수정 API를 정의한다.
        return ResultDto.success(instrumentService.updateInstrument(ticker, request)); // 서비스 수정 결과를 성공 응답으로 래핑해 반환한다.
    } // 종목 수정 API를 종료한다.
} // 종목 컨트롤러를 종료한다.
