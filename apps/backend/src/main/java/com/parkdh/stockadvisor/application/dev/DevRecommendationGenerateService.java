package com.parkdh.stockadvisor.application.dev; // 개발용 추천 생성 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.dev.dto.DevRecommendationGenerateResponse; // 개발용 추천 생성 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.application.recommendation.PredictedRecommendation; // 추천 가격 산출 결과를 가져온다.
import com.parkdh.stockadvisor.application.recommendation.PricePredictor; // 추천 가격 산출기를 가져온다.
import com.parkdh.stockadvisor.application.recommendation.RecommendationCandidate; // 추천 후보 값을 가져온다.
import com.parkdh.stockadvisor.application.recommendation.RecommendationEngine; // 추천 후보 선별 엔진을 가져온다.
import com.parkdh.stockadvisor.domain.prediction.PredictionEntity; // 예측 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.prediction.PredictionRepository; // 예측 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class DevRecommendationGenerateService { // 개발용 추천 생성 서비스를 정의한다.
    private static final String MODEL_VERSION = "dev-rule-v0"; // 개발용 모델 버전을 정의한다.
    private final RecommendationEngine recommendationEngine; // 추천 후보 선별 엔진 의존성을 보관한다.
    private final PricePredictor pricePredictor; // 추천 가격 산출기 의존성을 보관한다.
    private final PredictionRepository predictionRepository; // 예측 저장소 의존성을 보관한다.
    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.

    @Transactional // 예측과 추천 생성을 쓰기 트랜잭션으로 처리한다.
    public DevRecommendationGenerateResponse generate(String market, Integer shortCount, Integer longCount) { // 개발용 예측과 추천을 생성한다.
        int safeShortCount = normalizeCount(shortCount, 3, "단기 추천 개수"); // 단기 추천 개수를 검증하고 기본값을 적용한다.
        int safeLongCount = normalizeCount(longCount, 3, "장기 추천 개수"); // 장기 추천 개수를 검증하고 기본값을 적용한다.
        List<RecommendationCandidate> candidates = recommendationEngine.buildCandidates(market); // 자동 후보군 또는 개발용 수동 종목 목록을 조회한다.
        if (candidates.isEmpty()) { // 추천 생성 대상 후보군이 없는지 확인한다.
            throw new CustomException("추천을 생성할 후보 종목이 없습니다. 먼저 /api/dev/universe/seed를 호출하세요.", 404); // 후보군 없음 예외를 던진다.
        } // 활성 종목 확인을 종료한다.
        List<RecommendationCandidate> sorted = candidates.stream().toList(); // 엔진에서 정렬된 후보 목록을 사용한다.
        List<PredictionEntity> predictions = predictionRepository.saveAll( // 예측을 일괄 저장한다.
                sorted.stream().map(this::createPrediction).toList()); // 종목별 개발용 예측을 생성한다.
        List<RecommendationEntity> shortRecommendations = recommendationRepository.saveAll( // 단기 추천을 일괄 저장한다.
                sorted.stream().limit(safeShortCount).map(candidate -> createRecommendation(candidate, "SHORT")).toList()); // 단기 추천을 생성한다.
        List<RecommendationEntity> longRecommendations = recommendationRepository.saveAll( // 장기 추천을 일괄 저장한다.
                sorted.stream().limit(safeLongCount).map(candidate -> createRecommendation(candidate, "LONG")).toList()); // 장기 추천을 생성한다.
        List<Long> predictionIds = predictions.stream().map(PredictionEntity::getId).toList(); // 생성된 예측 ID 목록을 만든다.
        List<Long> recommendationIds = joinRecommendationIds(shortRecommendations, longRecommendations); // 생성된 추천 ID 목록을 만든다.
        return new DevRecommendationGenerateResponse(market == null || market.isBlank() ? "ALL" : market, sorted.size(), predictionIds.size(), recommendationIds.size(), predictionIds, recommendationIds); // 생성 결과를 반환한다.
    } // 개발용 추천 생성을 종료한다.

    private int normalizeCount(Integer count, int defaultValue, String label) { // 추천 개수를 정규화한다.
        int value = count == null ? defaultValue : count; // 값이 없으면 기본값을 사용한다.
        if (value < 1 || value > 10) { // 허용 범위를 벗어났는지 확인한다.
            throw new CustomException(label + "는 1~10 사이여야 합니다.", 400); // 추천 개수 검증 예외를 던진다.
        } // 범위 확인을 종료한다.
        return value; // 정규화된 값을 반환한다.
    } // 추천 개수 정규화를 종료한다.

    private PredictionEntity createPrediction(RecommendationCandidate candidate) { // 개발용 예측 엔티티를 생성한다.
        PredictedRecommendation predicted = pricePredictor.predict(candidate, "SHORT"); // 단기 기준 예측 가격을 산출한다.
        return new PredictionEntity(candidate.ticker(), 5, predicted.targetPrice(), MODEL_VERSION, LocalDateTime.now()); // 예측 엔티티를 반환한다.
    } // 개발용 예측 엔티티 생성을 종료한다.

    private RecommendationEntity createRecommendation(RecommendationCandidate candidate, String term) { // 개발용 추천 엔티티를 생성한다.
        PredictedRecommendation predicted = pricePredictor.predict(candidate, term); // 추천 기간에 맞는 가격을 산출한다.
        Integer confidence = 70 + Math.abs(candidate.ticker().hashCode() % 20); // 티커 기반 개발용 신뢰도를 계산한다.
        String signalsJson = buildSignalsJson(candidate, term, predicted); // 개발용 시그널 JSON 문자열을 만든다.
        return new RecommendationEntity(candidate.ticker(), candidate.market(), term, predicted.entryPrice(), predicted.targetPrice(), predicted.stopPrice(), predicted.expectedExitAt(), confidence, signalsJson, MODEL_VERSION, LocalDateTime.now(), "OPEN"); // 추천 엔티티를 반환한다.
    } // 개발용 추천 엔티티 생성을 종료한다.

    private String buildSignalsJson(RecommendationCandidate candidate, String term, PredictedRecommendation predicted) { // 개발용 시그널 JSON 문자열을 만든다.
        return "{\"generatedBy\":\"dev-rule-v0\",\"source\":\"" + candidate.source() + "\",\"ticker\":\"" + candidate.ticker() + "\",\"term\":\"" + term + "\",\"reason\":\"시장 후보군 feature 점수 기반 개발용 자동 생성\",\"featureScore\":" + candidate.score() + ",\"pricingMethod\":\"" + predicted.pricingMethod() + "\",\"featureJson\":" + candidate.featureJson() + "}"; // 개발용 시그널 JSON을 반환한다.
    } // 개발용 시그널 JSON 생성을 종료한다.

    private List<Long> joinRecommendationIds(List<RecommendationEntity> shortRecommendations, List<RecommendationEntity> longRecommendations) { // 추천 ID 목록을 합친다.
        return java.util.stream.Stream.concat(shortRecommendations.stream(), longRecommendations.stream()).map(RecommendationEntity::getId).toList(); // 단기와 장기 추천 ID를 합쳐 반환한다.
    } // 추천 ID 목록 병합을 종료한다.
} // 개발용 추천 생성 서비스를 종료한다.
