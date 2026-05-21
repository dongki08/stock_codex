package com.parkdh.stockadvisor.application.recommendation; // 추천 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 검증 도구를 가져온다.
import com.parkdh.stockadvisor.api.recommendation.dto.RecommendationCreateRequest; // 추천 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.recommendation.dto.RecommendationResponse; // 추천 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.recommendation.dto.RecommendationStatusUpdateRequest; // 추천 상태 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.JsonValidationUtil; // JSON 검증 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository; // 종목 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Set; // 집합 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class RecommendationService { // 추천 서비스를 정의한다.
    private static final Set<String> TERMS = Set.of("SHORT", "LONG"); // 허용 보유 기간 구분을 정의한다.
    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED", "EXPIRED"); // 허용 추천 상태를 정의한다.
    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.
    private final InstrumentRepository instrumentRepository; // 종목 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 검증 도구 의존성을 보관한다.

    public List<RecommendationResponse> getRecommendations(String status, String ticker) { // 추천 목록을 조회한다.
        List<RecommendationEntity> entities = ticker != null && !ticker.isBlank() ? recommendationRepository.findByTicker(ticker) : status != null && !status.isBlank() ? recommendationRepository.findByStatus(status) : recommendationRepository.findAll(); // 조건 우선순위에 따라 추천을 조회한다.
        return entities.stream().sorted(Comparator.comparing(RecommendationEntity::getGeneratedAt).reversed()).map(this::toResponse).toList(); // 생성 일시 역순으로 정렬해 DTO로 변환한다.
    } // 추천 목록 조회를 종료한다.

    public RecommendationResponse getRecommendation(Long id) { // 추천 단건을 조회한다.
        RecommendationEntity entity = recommendationRepository.findById(id).orElseThrow(() -> new CustomException("추천을 찾을 수 없습니다.", 404)); // 추천이 없으면 404 예외를 던진다.
        return toResponse(entity); // 추천 DTO를 반환한다.
    } // 추천 단건 조회를 종료한다.

    @Transactional // 추천 생성을 쓰기 트랜잭션으로 처리한다.
    public RecommendationResponse createRecommendation(RecommendationCreateRequest request) { // 추천을 생성한다.
        validateCreateRequest(request); // 추천 생성 요청의 비즈니스 규칙을 검증한다.
        LocalDateTime generatedAt = request.generatedAt() == null ? LocalDateTime.now() : request.generatedAt(); // 생성 일시가 없으면 현재 시각을 사용한다.
        RecommendationEntity entity = new RecommendationEntity(request.ticker(), request.market(), request.term(), request.entryPrice(), request.targetPrice(), request.stopPrice(), request.expectedExitAt(), request.confidence(), request.signalsJson(), request.modelVersion(), generatedAt, "OPEN"); // 새 추천 엔티티를 생성한다.
        RecommendationEntity saved = recommendationRepository.save(entity); // 새 추천을 저장한다.
        return toResponse(saved); // 저장된 추천을 반환한다.
    } // 추천 생성을 종료한다.

    @Transactional // 추천 상태 수정을 쓰기 트랜잭션으로 처리한다.
    public RecommendationResponse updateStatus(Long id, RecommendationStatusUpdateRequest request) { // 추천 상태를 수정한다.
        if (!STATUSES.contains(request.status())) { // 허용된 상태인지 확인한다.
            throw new CustomException("추천 상태는 OPEN, CLOSED, EXPIRED 중 하나여야 합니다.", 400); // 상태 검증 예외를 던진다.
        } // 상태 확인을 종료한다.
        RecommendationEntity entity = recommendationRepository.findById(id).orElseThrow(() -> new CustomException("추천을 찾을 수 없습니다.", 404)); // 추천이 없으면 404 예외를 던진다.
        entity.updateStatus(request.status()); // 추천 상태를 갱신한다.
        RecommendationEntity saved = recommendationRepository.save(entity); // 수정된 추천을 저장한다.
        return toResponse(saved); // 수정된 추천을 반환한다.
    } // 추천 상태 수정을 종료한다.

    private void validateCreateRequest(RecommendationCreateRequest request) { // 추천 생성 요청을 검증한다.
        if (!TERMS.contains(request.term())) { // 보유 기간 구분이 허용 값인지 확인한다.
            throw new CustomException("보유 기간 구분은 SHORT 또는 LONG이어야 합니다.", 400); // 보유 기간 검증 예외를 던진다.
        } // 보유 기간 확인을 종료한다.
        if (!instrumentRepository.existsById(request.ticker())) { // 등록된 종목인지 확인한다.
            throw new CustomException("등록되지 않은 종목은 추천할 수 없습니다.", 404); // 종목 미등록 예외를 던진다.
        } // 종목 확인을 종료한다.
        JsonValidationUtil.validate(objectMapper, request.signalsJson(), "시그널 JSON"); // 시그널 JSON 형식을 검증한다.
    } // 추천 생성 요청 검증을 종료한다.

    private RecommendationResponse toResponse(RecommendationEntity entity) { // 추천 엔티티를 응답 DTO로 변환한다.
        return new RecommendationResponse(entity.getId(), entity.getTicker(), entity.getMarket(), entity.getTerm(), entity.getEntryPrice(), entity.getTargetPrice(), entity.getStopPrice(), entity.getExpectedExitAt(), entity.getConfidence(), entity.getSignalsJson(), entity.getModelVersion(), entity.getGeneratedAt(), entity.getStatus()); // 추천 응답 DTO를 생성한다.
    } // 추천 DTO 변환을 종료한다.
} // 추천 서비스를 종료한다.
