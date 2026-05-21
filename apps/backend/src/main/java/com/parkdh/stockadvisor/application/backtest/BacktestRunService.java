package com.parkdh.stockadvisor.application.backtest; // 백테스트 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 검증 도구를 가져온다.
import com.parkdh.stockadvisor.api.backtest.dto.BacktestRunCreateRequest; // 백테스트 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.backtest.dto.BacktestRunResponse; // 백테스트 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.backtest.BacktestRunEntity; // 백테스트 실행 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.JsonValidationUtil; // JSON 검증 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.backtest.BacktestRunRepository; // 백테스트 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class BacktestRunService { // 백테스트 실행 서비스를 정의한다.
    private final BacktestRunRepository backtestRunRepository; // 백테스트 실행 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 검증 도구 의존성을 보관한다.

    public List<BacktestRunResponse> getBacktestRuns() { // 백테스트 실행 목록을 조회한다.
        return backtestRunRepository.findAll().stream().sorted(Comparator.comparing(BacktestRunEntity::getId).reversed()).map(this::toResponse).toList(); // ID 역순으로 정렬해 DTO로 변환한다.
    } // 백테스트 실행 목록 조회를 종료한다.

    public BacktestRunResponse getBacktestRun(Long id) { // 백테스트 실행 단건을 조회한다.
        BacktestRunEntity entity = backtestRunRepository.findById(id).orElseThrow(() -> new CustomException("백테스트 실행을 찾을 수 없습니다.", 404)); // 백테스트 실행이 없으면 404 예외를 던진다.
        return toResponse(entity); // 백테스트 실행 DTO를 반환한다.
    } // 백테스트 실행 단건 조회를 종료한다.

    @Transactional // 백테스트 실행 생성을 쓰기 트랜잭션으로 처리한다.
    public BacktestRunResponse createBacktestRun(BacktestRunCreateRequest request) { // 백테스트 실행을 생성한다.
        if (request.periodFrom().isAfter(request.periodTo())) { // 시작일이 종료일 이후인지 확인한다.
            throw new CustomException("백테스트 시작일은 종료일보다 늦을 수 없습니다.", 400); // 기간 검증 예외를 던진다.
        } // 기간 확인을 종료한다.
        JsonValidationUtil.validate(objectMapper, request.metricsJson(), "지표 JSON"); // 지표 JSON 형식을 검증한다.
        BacktestRunEntity entity = new BacktestRunEntity(request.strategy(), request.periodFrom(), request.periodTo(), request.metricsJson()); // 새 백테스트 실행 엔티티를 생성한다.
        BacktestRunEntity saved = backtestRunRepository.save(entity); // 새 백테스트 실행을 저장한다.
        return toResponse(saved); // 저장된 백테스트 실행을 반환한다.
    } // 백테스트 실행 생성을 종료한다.

    private BacktestRunResponse toResponse(BacktestRunEntity entity) { // 백테스트 실행 엔티티를 응답 DTO로 변환한다.
        return new BacktestRunResponse(entity.getId(), entity.getStrategy(), entity.getPeriodFrom(), entity.getPeriodTo(), entity.getMetricsJson()); // 백테스트 실행 응답 DTO를 생성한다.
    } // 백테스트 실행 DTO 변환을 종료한다.
} // 백테스트 실행 서비스를 종료한다.
