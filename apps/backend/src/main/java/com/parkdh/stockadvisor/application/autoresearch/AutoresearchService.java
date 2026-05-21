package com.parkdh.stockadvisor.application.autoresearch; // AutoResearch 서비스 패키지를 선언한다.

import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchRunCreateRequest; // AutoResearch 실행 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchRunResponse; // AutoResearch 실행 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.StrategyVersionCreateRequest; // 전략 버전 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.StrategyVersionResponse; // 전략 버전 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.autoresearch.AutoresearchRunEntity; // AutoResearch 실행 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.autoresearch.StrategyVersionEntity; // 전략 버전 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.AutoresearchRunRepository; // AutoResearch 실행 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.StrategyVersionRepository; // 전략 버전 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.UUID; // UUID 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class AutoresearchService { // AutoResearch 서비스를 정의한다.
    private final AutoresearchRunRepository autoresearchRunRepository; // AutoResearch 실행 저장소 의존성을 보관한다.
    private final StrategyVersionRepository strategyVersionRepository; // 전략 버전 저장소 의존성을 보관한다.

    public List<AutoresearchRunResponse> getRuns(UUID jobRunId) { // AutoResearch 실행 목록을 조회한다.
        List<AutoresearchRunEntity> entities = jobRunId == null ? autoresearchRunRepository.findAll() : autoresearchRunRepository.findByJobRunId(jobRunId); // 작업 UUID 조건 여부에 따라 실행 목록을 조회한다.
        return entities.stream().sorted(Comparator.comparing(AutoresearchRunEntity::getId).reversed()).map(this::toRunResponse).toList(); // ID 역순으로 정렬해 DTO로 변환한다.
    } // AutoResearch 실행 목록 조회를 종료한다.

    public AutoresearchRunResponse getRun(Long id) { // AutoResearch 실행 단건을 조회한다.
        AutoresearchRunEntity entity = autoresearchRunRepository.findById(id).orElseThrow(() -> new CustomException("AutoResearch 실행을 찾을 수 없습니다.", 404)); // 실행이 없으면 404 예외를 던진다.
        return toRunResponse(entity); // AutoResearch 실행 DTO를 반환한다.
    } // AutoResearch 실행 단건 조회를 종료한다.

    @Transactional // AutoResearch 실행 생성을 쓰기 트랜잭션으로 처리한다.
    public AutoresearchRunResponse createRun(AutoresearchRunCreateRequest request) { // AutoResearch 실행을 생성한다.
        AutoresearchRunEntity entity = new AutoresearchRunEntity(request.jobRunId(), request.iterNo(), request.parentSha(), request.proposalSha(), request.diffSummary(), request.metricName(), request.metricValue(), request.championMetric(), request.decision(), request.durationMs(), request.startedAt(), request.endedAt()); // 새 AutoResearch 실행 엔티티를 생성한다.
        AutoresearchRunEntity saved = autoresearchRunRepository.save(entity); // 새 AutoResearch 실행을 저장한다.
        return toRunResponse(saved); // 저장된 AutoResearch 실행을 반환한다.
    } // AutoResearch 실행 생성을 종료한다.

    public List<StrategyVersionResponse> getStrategyVersions(Boolean champion) { // 전략 버전 목록을 조회한다.
        List<StrategyVersionEntity> entities = champion == null ? strategyVersionRepository.findAll() : strategyVersionRepository.findByChampion(champion); // 챔피언 조건 여부에 따라 전략 버전을 조회한다.
        return entities.stream().sorted(Comparator.comparing(StrategyVersionEntity::getPromotedAt).reversed()).map(this::toStrategyResponse).toList(); // 승격 일시 역순으로 정렬해 DTO로 변환한다.
    } // 전략 버전 목록 조회를 종료한다.

    public StrategyVersionResponse getStrategyVersion(Long id) { // 전략 버전 단건을 조회한다.
        StrategyVersionEntity entity = strategyVersionRepository.findById(id).orElseThrow(() -> new CustomException("전략 버전을 찾을 수 없습니다.", 404)); // 전략 버전이 없으면 404 예외를 던진다.
        return toStrategyResponse(entity); // 전략 버전 DTO를 반환한다.
    } // 전략 버전 단건 조회를 종료한다.

    @Transactional // 전략 버전 생성을 쓰기 트랜잭션으로 처리한다.
    public StrategyVersionResponse createStrategyVersion(StrategyVersionCreateRequest request) { // 전략 버전을 생성한다.
        LocalDateTime promotedAt = request.promotedAt() == null ? LocalDateTime.now() : request.promotedAt(); // 승격 일시가 없으면 현재 시각을 사용한다.
        StrategyVersionEntity entity = new StrategyVersionEntity(request.semver(), request.gitSha(), request.metricValue(), promotedAt, request.champion()); // 새 전략 버전 엔티티를 생성한다.
        StrategyVersionEntity saved = strategyVersionRepository.save(entity); // 새 전략 버전을 저장한다.
        return toStrategyResponse(saved); // 저장된 전략 버전을 반환한다.
    } // 전략 버전 생성을 종료한다.

    private AutoresearchRunResponse toRunResponse(AutoresearchRunEntity entity) { // AutoResearch 실행 엔티티를 응답 DTO로 변환한다.
        return new AutoresearchRunResponse(entity.getId(), entity.getJobRunId(), entity.getIterNo(), entity.getParentSha(), entity.getProposalSha(), entity.getDiffSummary(), entity.getMetricName(), entity.getMetricValue(), entity.getChampionMetric(), entity.getDecision(), entity.getDurationMs(), entity.getStartedAt(), entity.getEndedAt()); // AutoResearch 실행 응답 DTO를 생성한다.
    } // AutoResearch 실행 DTO 변환을 종료한다.

    private StrategyVersionResponse toStrategyResponse(StrategyVersionEntity entity) { // 전략 버전 엔티티를 응답 DTO로 변환한다.
        return new StrategyVersionResponse(entity.getId(), entity.getSemver(), entity.getGitSha(), entity.getMetricValue(), entity.getPromotedAt(), entity.getChampion()); // 전략 버전 응답 DTO를 생성한다.
    } // 전략 버전 DTO 변환을 종료한다.
} // AutoResearch 서비스를 종료한다.
