package com.parkdh.stockadvisor.application.codex; // Codex 호출 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 검증 도구를 가져온다.
import com.parkdh.stockadvisor.api.codex.dto.CodexCallCreateRequest; // Codex 호출 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.codex.dto.CodexCallResponse; // Codex 호출 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.codex.CodexCallEntity; // Codex 호출 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.JsonValidationUtil; // JSON 검증 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.codex.CodexCallRepository; // Codex 호출 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class CodexCallService { // Codex 호출 로그 서비스를 정의한다.
    private final CodexCallRepository codexCallRepository; // Codex 호출 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 검증 도구 의존성을 보관한다.

    public List<CodexCallResponse> getCodexCalls(String caller) { // Codex 호출 로그 목록을 조회한다.
        List<CodexCallEntity> entities = caller == null || caller.isBlank() ? codexCallRepository.findAll() : codexCallRepository.findByCaller(caller); // 호출자 조건 여부에 따라 호출 로그를 조회한다.
        return entities.stream().sorted(Comparator.comparing(CodexCallEntity::getCalledAt).reversed()).map(this::toResponse).toList(); // 호출 일시 역순으로 정렬해 DTO로 변환한다.
    } // Codex 호출 로그 목록 조회를 종료한다.

    public CodexCallResponse getCodexCall(Long id) { // Codex 호출 로그 단건을 조회한다.
        CodexCallEntity entity = codexCallRepository.findById(id).orElseThrow(() -> new CustomException("Codex 호출 로그를 찾을 수 없습니다.", 404)); // 호출 로그가 없으면 404 예외를 던진다.
        return toResponse(entity); // Codex 호출 로그 DTO를 반환한다.
    } // Codex 호출 로그 단건 조회를 종료한다.

    @Transactional // Codex 호출 로그 생성을 쓰기 트랜잭션으로 처리한다.
    public CodexCallResponse createCodexCall(CodexCallCreateRequest request) { // Codex 호출 로그를 생성한다.
        if (request.toolsUsedJson() != null && !request.toolsUsedJson().isBlank()) { // 사용 도구 JSON이 입력됐는지 확인한다.
            JsonValidationUtil.validate(objectMapper, request.toolsUsedJson(), "사용 도구 JSON"); // 사용 도구 JSON 형식을 검증한다.
        } // 사용 도구 JSON 확인을 종료한다.
        LocalDateTime calledAt = request.calledAt() == null ? LocalDateTime.now() : request.calledAt(); // 호출 일시가 없으면 현재 시각을 사용한다.
        CodexCallEntity entity = new CodexCallEntity(request.caller(), request.promptHash(), request.promptLen(), request.responseLen(), request.toolsUsedJson(), request.durationMs(), request.succeeded(), request.errorMessage(), calledAt); // 새 Codex 호출 엔티티를 생성한다.
        CodexCallEntity saved = codexCallRepository.save(entity); // 새 Codex 호출 로그를 저장한다.
        return toResponse(saved); // 저장된 Codex 호출 로그를 반환한다.
    } // Codex 호출 로그 생성을 종료한다.

    private CodexCallResponse toResponse(CodexCallEntity entity) { // Codex 호출 엔티티를 응답 DTO로 변환한다.
        return new CodexCallResponse(entity.getId(), entity.getCaller(), entity.getPromptHash(), entity.getPromptLen(), entity.getResponseLen(), entity.getToolsUsedJson(), entity.getDurationMs(), entity.getSucceeded(), entity.getErrorMessage(), entity.getCalledAt()); // Codex 호출 응답 DTO를 생성한다.
    } // Codex 호출 DTO 변환을 종료한다.
} // Codex 호출 로그 서비스를 종료한다.
