package com.parkdh.stockadvisor.application.prediction; // 예측 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.prediction.dto.PredictionCreateRequest; // 예측 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.prediction.dto.PredictionResponse; // 예측 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.prediction.PredictionEntity; // 예측 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository; // 종목 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.prediction.PredictionRepository; // 예측 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class PredictionService { // 예측 서비스를 정의한다.
    private final PredictionRepository predictionRepository; // 예측 저장소 의존성을 보관한다.
    private final InstrumentRepository instrumentRepository; // 종목 저장소 의존성을 보관한다.

    public List<PredictionResponse> getPredictions(String ticker) { // 예측 목록을 조회한다.
        List<PredictionEntity> entities = ticker == null || ticker.isBlank() ? predictionRepository.findAll() : predictionRepository.findByTicker(ticker); // 종목 조건 여부에 따라 예측을 조회한다.
        return entities.stream().sorted(Comparator.comparing(PredictionEntity::getGeneratedAt).reversed()).map(this::toResponse).toList(); // 생성 일시 역순으로 정렬해 DTO로 변환한다.
    } // 예측 목록 조회를 종료한다.

    public PredictionResponse getPrediction(Long id) { // 예측 단건을 조회한다.
        PredictionEntity entity = predictionRepository.findById(id).orElseThrow(() -> new CustomException("예측을 찾을 수 없습니다.", 404)); // 예측이 없으면 404 예외를 던진다.
        return toResponse(entity); // 예측 DTO를 반환한다.
    } // 예측 단건 조회를 종료한다.

    @Transactional // 예측 생성을 쓰기 트랜잭션으로 처리한다.
    public PredictionResponse createPrediction(PredictionCreateRequest request) { // 예측을 생성한다.
        if (!instrumentRepository.existsById(request.ticker())) { // 등록된 종목인지 확인한다.
            throw new CustomException("등록되지 않은 종목은 예측할 수 없습니다.", 404); // 종목 미등록 예외를 던진다.
        } // 종목 확인을 종료한다.
        LocalDateTime generatedAt = request.generatedAt() == null ? LocalDateTime.now() : request.generatedAt(); // 생성 일시가 없으면 현재 시각을 사용한다.
        PredictionEntity entity = new PredictionEntity(request.ticker(), request.horizonDays(), request.predictedPrice(), request.modelVersion(), generatedAt); // 새 예측 엔티티를 생성한다.
        PredictionEntity saved = predictionRepository.save(entity); // 새 예측을 저장한다.
        return toResponse(saved); // 저장된 예측을 반환한다.
    } // 예측 생성을 종료한다.

    private PredictionResponse toResponse(PredictionEntity entity) { // 예측 엔티티를 응답 DTO로 변환한다.
        return new PredictionResponse(entity.getId(), entity.getTicker(), entity.getHorizonDays(), entity.getPredictedPrice(), entity.getModelVersion(), entity.getGeneratedAt()); // 예측 응답 DTO를 생성한다.
    } // 예측 DTO 변환을 종료한다.
} // 예측 서비스를 종료한다.
