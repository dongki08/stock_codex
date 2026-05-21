package com.parkdh.stockadvisor.application.instrument; // 종목 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.instrument.dto.InstrumentCreateRequest; // 종목 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.instrument.dto.InstrumentResponse; // 종목 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.instrument.dto.InstrumentUpdateRequest; // 종목 수정 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.instrument.InstrumentEntity; // 종목 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository; // 종목 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class InstrumentService { // 종목 서비스를 정의한다.
    private final InstrumentRepository instrumentRepository; // 종목 저장소 의존성을 보관한다.

    public List<InstrumentResponse> getInstruments(String market) { // 종목 목록을 조회한다.
        List<InstrumentEntity> entities = market == null || market.isBlank() ? instrumentRepository.findAll() : instrumentRepository.findByMarket(market); // 시장 조건 여부에 따라 목록을 조회한다.
        return entities.stream().sorted(Comparator.comparing(InstrumentEntity::getTicker)).map(this::toResponse).toList(); // 종목을 티커 기준으로 정렬해 DTO로 변환한다.
    } // 종목 목록 조회를 종료한다.

    public InstrumentResponse getInstrument(String ticker) { // 종목 단건을 조회한다.
        InstrumentEntity entity = instrumentRepository.findById(ticker).orElseThrow(() -> new CustomException("종목을 찾을 수 없습니다.", 404)); // 종목이 없으면 404 예외를 던진다.
        return toResponse(entity); // 종목 DTO를 반환한다.
    } // 종목 단건 조회를 종료한다.

    @Transactional // 종목 생성을 쓰기 트랜잭션으로 처리한다.
    public InstrumentResponse createInstrument(InstrumentCreateRequest request) { // 종목을 생성한다.
        if (instrumentRepository.existsById(request.ticker())) { // 이미 존재하는 종목인지 확인한다.
            throw new CustomException("이미 등록된 종목입니다.", 409); // 중복 등록 예외를 던진다.
        } // 중복 확인을 종료한다.
        InstrumentEntity entity = new InstrumentEntity(request.ticker(), request.market(), request.name(), request.sector(), request.enabled()); // 새 종목 엔티티를 생성한다.
        InstrumentEntity saved = instrumentRepository.save(entity); // 새 종목을 저장한다.
        return toResponse(saved); // 저장된 종목을 반환한다.
    } // 종목 생성을 종료한다.

    @Transactional // 종목 수정을 쓰기 트랜잭션으로 처리한다.
    public InstrumentResponse updateInstrument(String ticker, InstrumentUpdateRequest request) { // 종목을 수정한다.
        InstrumentEntity entity = instrumentRepository.findById(ticker).orElseThrow(() -> new CustomException("종목을 찾을 수 없습니다.", 404)); // 종목이 없으면 404 예외를 던진다.
        entity.update(request.market(), request.name(), request.sector(), request.enabled()); // 종목 정보를 갱신한다.
        InstrumentEntity saved = instrumentRepository.save(entity); // 수정된 종목을 저장한다.
        return toResponse(saved); // 수정된 종목을 반환한다.
    } // 종목 수정을 종료한다.

    private InstrumentResponse toResponse(InstrumentEntity entity) { // 종목 엔티티를 응답 DTO로 변환한다.
        return new InstrumentResponse(entity.getTicker(), entity.getMarket(), entity.getName(), entity.getSector(), entity.getEnabled()); // 종목 응답 DTO를 생성한다.
    } // 종목 DTO 변환을 종료한다.
} // 종목 서비스를 종료한다.
