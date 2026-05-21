package com.parkdh.stockadvisor.application.evaluation; // 평가 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.evaluation.dto.EvaluationCreateRequest; // 평가 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.evaluation.dto.EvaluationResponse; // 평가 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity; // 평가 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository; // 평가 저장소를 가져온다.
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
public class EvaluationService { // 평가 서비스를 정의한다.
    private static final Set<String> EXIT_REASONS = Set.of("TARGET_HIT", "STOP_HIT", "TIME_OUT", "MANUAL_CLOSE"); // 허용 청산 사유를 정의한다.
    private final EvaluationRepository evaluationRepository; // 평가 저장소 의존성을 보관한다.
    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.

    public List<EvaluationResponse> getEvaluations(Long recommendationId) { // 평가 목록을 조회한다.
        List<EvaluationEntity> entities = recommendationId == null ? evaluationRepository.findAll() : evaluationRepository.findByRecommendationId(recommendationId); // 추천 ID 조건 여부에 따라 평가를 조회한다.
        return entities.stream().sorted(Comparator.comparing(EvaluationEntity::getEvaluatedAt).reversed()).map(this::toResponse).toList(); // 평가 일시 역순으로 정렬해 DTO로 변환한다.
    } // 평가 목록 조회를 종료한다.

    public EvaluationResponse getEvaluation(Long id) { // 평가 단건을 조회한다.
        EvaluationEntity entity = evaluationRepository.findById(id).orElseThrow(() -> new CustomException("평가를 찾을 수 없습니다.", 404)); // 평가가 없으면 404 예외를 던진다.
        return toResponse(entity); // 평가 DTO를 반환한다.
    } // 평가 단건 조회를 종료한다.

    @Transactional // 평가 생성을 쓰기 트랜잭션으로 처리한다.
    public EvaluationResponse createEvaluation(EvaluationCreateRequest request) { // 평가를 생성한다.
        if (!recommendationRepository.existsById(request.recommendationId())) { // 추천이 존재하는지 확인한다.
            throw new CustomException("평가할 추천을 찾을 수 없습니다.", 404); // 추천 미존재 예외를 던진다.
        } // 추천 존재 확인을 종료한다.
        if (!EXIT_REASONS.contains(request.exitReason())) { // 허용된 청산 사유인지 확인한다.
            throw new CustomException("청산 사유는 TARGET_HIT, STOP_HIT, TIME_OUT, MANUAL_CLOSE 중 하나여야 합니다.", 400); // 청산 사유 검증 예외를 던진다.
        } // 청산 사유 확인을 종료한다.
        LocalDateTime evaluatedAt = request.evaluatedAt() == null ? LocalDateTime.now() : request.evaluatedAt(); // 평가 일시가 없으면 현재 시각을 사용한다.
        EvaluationEntity entity = new EvaluationEntity(request.recommendationId(), request.actualExitPrice(), request.exitReason(), request.pnlPct(), request.drawdownPct(), request.hitTarget(), evaluatedAt); // 새 평가 엔티티를 생성한다.
        EvaluationEntity saved = evaluationRepository.save(entity); // 새 평가를 저장한다.
        return toResponse(saved); // 저장된 평가를 반환한다.
    } // 평가 생성을 종료한다.

    private EvaluationResponse toResponse(EvaluationEntity entity) { // 평가 엔티티를 응답 DTO로 변환한다.
        return new EvaluationResponse(entity.getId(), entity.getRecommendationId(), entity.getActualExitPrice(), entity.getExitReason(), entity.getPnlPct(), entity.getDrawdownPct(), entity.getHitTarget(), entity.getEvaluatedAt()); // 평가 응답 DTO를 생성한다.
    } // 평가 DTO 변환을 종료한다.
} // 평가 서비스를 종료한다.
