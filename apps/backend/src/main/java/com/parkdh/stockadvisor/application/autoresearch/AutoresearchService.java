package com.parkdh.stockadvisor.application.autoresearch; // AutoResearch 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드를 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 도구를 가져온다.
import com.fasterxml.jackson.databind.node.ObjectNode; // JSON 객체 노드를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchAutoRunRequest; // AutoResearch 자동 실행 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchRunCreateRequest; // AutoResearch 실행 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.AutoresearchRunResponse; // AutoResearch 실행 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.StrategyVersionCreateRequest; // 전략 버전 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.autoresearch.dto.StrategyVersionResponse; // 전략 버전 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.backtest.dto.BacktestSimulationRequest; // 백테스트 시뮬레이션 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.application.backtest.BacktestRunService; // 백테스트 서비스를 가져온다.
import com.parkdh.stockadvisor.application.research.FeatureICService; // 피처 IC 측정 서비스를 가져온다.
import com.parkdh.stockadvisor.domain.autoresearch.AutoresearchRunEntity; // AutoResearch 실행 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.autoresearch.StrategyVersionEntity; // 전략 버전 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.audit.AuditLogEntity; // 감사 로그 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity; // 평가 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 앱 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.AutoresearchRunRepository; // AutoResearch 실행 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.autoresearch.StrategyVersionRepository; // 전략 버전 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.audit.AuditLogRepository; // 감사 로그 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository; // 평가 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 앱 설정 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.math.RoundingMode; // 반올림 모드를 가져온다.
import java.nio.charset.StandardCharsets; // 문자열 인코딩을 가져온다.
import java.security.MessageDigest; // 해시 도구를 가져온다.
import java.time.Duration; // 기간 계산 도구를 가져온다.
import java.time.LocalDate; // 날짜 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.time.format.DateTimeFormatter; // 날짜 포맷 도구를 가져온다.
import java.util.ArrayList; // 배열 목록 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Objects; // 객체 비교 유틸을 가져온다.
import java.util.Optional; // Optional 타입을 가져온다.
import java.util.UUID; // UUID 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class AutoresearchService { // AutoResearch 서비스를 정의한다.
    private static final String SCORING_WEIGHTS_SETTING_KEY = "recommendation.scoring.weights"; // 점수 가중치 설정 키를 정의한다.
    private static final String DEFAULT_WEIGHTS_JSON = "{\"value\":{\"liquidity\":0.20,\"price\":0.10,\"technical\":0.30,\"context\":0.15,\"fundamental\":0.10,\"dataQuality\":0.15},\"technical\":{\"ma\":0.30,\"rsi\":0.25,\"volume\":0.20,\"macd\":0.15,\"bollinger\":0.10},\"context\":{\"news\":0.40,\"disclosure\":0.18,\"macro\":0.25,\"fundamental\":0.17}}"; // 기본 점수 가중치를 정의한다.
    private static final List<String> MUTATION_PATHS = List.of(
            "value.technical",
            "value.context",
            "value.liquidity",
            "value.dataQuality",
            "technical.ma",
            "technical.rsi",
            "technical.volume",
            "technical.macd",
            "technical.bollinger",
            "context.news",
            "context.disclosure",
            "context.macro",
            "context.fundamental"
    ); // AutoResearch가 변형할 가중치 경로를 정의한다.

    private final AutoresearchRunRepository autoresearchRunRepository; // AutoResearch 실행 저장소 의존성을 보관한다.
    private final StrategyVersionRepository strategyVersionRepository; // 전략 버전 저장소 의존성을 보관한다.
    private final AppSettingRepository appSettingRepository; // 앱 설정 저장소 의존성을 보관한다.
    private final AuditLogRepository auditLogRepository; // 감사 로그 저장소 의존성을 보관한다.
    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.
    private final EvaluationRepository evaluationRepository; // 평가 저장소 의존성을 보관한다.
    private final BacktestRunService backtestRunService; // 백테스트 서비스 의존성을 보관한다.
    private final FeatureICService featureICService; // 피처 IC 측정 서비스 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 도구 의존성을 보관한다.

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
        if (Boolean.TRUE.equals(request.champion())) { // 새 챔피언이면 기존 챔피언을 해제한다.
            demoteCurrentChampions(); // 기존 챔피언 상태를 false로 바꾼다.
        } // 챔피언 요청 확인을 종료한다.
        StrategyVersionEntity entity = new StrategyVersionEntity(request.semver(), request.gitSha(), request.metricValue(), promotedAt, request.champion()); // 새 전략 버전 엔티티를 생성한다.
        StrategyVersionEntity saved = strategyVersionRepository.save(entity); // 새 전략 버전을 저장한다.
        return toStrategyResponse(saved); // 저장된 전략 버전을 반환한다.
    } // 전략 버전 생성을 종료한다.

    public List<AutoresearchRunResponse> runAutoResearch(AutoresearchAutoRunRequest request) { // AutoResearch 자동 실행을 수행한다.
        AutoresearchAutoRunRequest safeRequest = request == null ? new AutoresearchAutoRunRequest(null, null, null, null, null, null, null, null) : request; // null 요청을 기본 요청으로 보정한다.
        validateChampionRollback(); // 검증 기간이 지난 챔피언은 라이브 성과 기준으로 롤백 여부를 먼저 확인한다.
        UUID jobRunId = UUID.randomUUID(); // 이번 실행 UUID를 생성한다.
        String originalWeightsJson = currentWeightsJson(); // 원래 가중치 JSON을 보관한다.
        BigDecimal championMetric = latestChampion().map(StrategyVersionEntity::getMetricValue).orElse(null); // 기존 챔피언 지표를 조회한다.
        String parentSha = latestChampion().map(StrategyVersionEntity::getGitSha).orElse(hash(originalWeightsJson)); // 기존 챔피언 해시를 조회한다.
        int iterations = normalizeIterations(safeRequest.iterations()); // 반복 횟수를 정규화한다.
        LocalDate periodTo = safeRequest.periodTo() == null ? LocalDate.now().minusDays(1) : safeRequest.periodTo(); // 백테스트 종료일을 결정한다.
        LocalDate periodFrom = safeRequest.periodFrom() == null ? periodTo.minusDays(365) : safeRequest.periodFrom(); // 백테스트 시작일을 결정한다.
        BacktestSimulationRequest simulationRequest = new BacktestSimulationRequest(
                "recommendation-engine-v1",
                safeRequest.market() == null || safeRequest.market().isBlank() ? "ALL" : safeRequest.market(),
                periodFrom,
                periodTo,
                safeRequest.maxTickers(),
                safeRequest.holdingDays(),
                safeRequest.targetPct(),
                safeRequest.stopPct()
        ); // 추천 엔진 백테스트 요청을 생성한다.

        List<AutoresearchRunEntity> savedRuns = new ArrayList<>(); // 저장된 실행 목록을 보관한다.
        String bestWeightsJson = originalWeightsJson; // 최고 가중치 JSON을 초기화한다.
        BigDecimal bestMetric = championMetric; // 최고 지표를 기존 챔피언으로 초기화한다.
        boolean promoted = false; // 승격 여부를 보관한다.
        for (int iterNo = 1; iterNo <= iterations; iterNo++) { // 요청된 반복 횟수만큼 실행한다.
            LocalDateTime startedAt = LocalDateTime.now(); // 시작 시각을 기록한다.
            FeatureICService.MutationGuide mutationGuide = featureICService.guideForIteration(iterNo, MUTATION_PATHS); // IC 기반 변형 가이드를 생성한다.
            String proposalJson = mutateWeights(originalWeightsJson, mutationGuide); // 후보 가중치를 생성한다.
            String proposalSha = hash(proposalJson); // 후보 해시를 생성한다.
            try { // 단일 실험 실패를 기록하고 다음 실험으로 진행한다.
                saveWeights(proposalJson); // 후보 가중치를 적용한다.
                BacktestRunService.BacktestEvaluation evaluation = backtestRunService.evaluateRecommendationEngine(simulationRequest); // 추천 엔진 백테스트를 수행한다.
                BigDecimal metricValue = evaluation.metricValue(); // 평가 지표를 가져온다.
                boolean keep = bestMetric == null || metricValue.compareTo(bestMetric) > 0; // 기존 최고보다 개선됐는지 판단한다.
                if (keep) { // 개선된 후보면 최고 후보를 갱신한다.
                    bestMetric = metricValue; // 최고 지표를 갱신한다.
                    bestWeightsJson = proposalJson; // 최고 가중치를 갱신한다.
                    promoted = true; // 승격 후보가 있음을 표시한다.
                } // 개선 후보 처리를 종료한다.
                saveStrategyYaml(proposalSha, buildStrategyYaml("proposal-" + iterNo, simulationRequest, proposalJson, evaluation.metricsJson(), metricValue, keep ? "KEEP" : "DISCARD")); // 후보 전략 YAML 스냅샷을 저장한다.
                savedRuns.add(autoresearchRunRepository.save(new AutoresearchRunEntity(
                        jobRunId,
                        iterNo,
                        parentSha,
                        proposalSha,
                        mutationGuide.summary(),
                        "avgPnlPct",
                        metricValue,
                        championMetric,
                        keep ? "KEEP" : "DISCARD",
                        durationMs(startedAt),
                        startedAt,
                        LocalDateTime.now()
                ))); // 실험 결과를 저장한다.
            } catch (Exception exception) { // 실험 실패를 기록한다.
                savedRuns.add(autoresearchRunRepository.save(new AutoresearchRunEntity(
                        jobRunId,
                        iterNo,
                        parentSha,
                        proposalSha,
                        exception.getMessage(),
                        "avgPnlPct",
                        null,
                        championMetric,
                        "ERROR",
                        durationMs(startedAt),
                        startedAt,
                        LocalDateTime.now()
                ))); // 오류 실험 결과를 저장한다.
            } // 단일 실험 처리를 종료한다.
        } // 반복 실행을 종료한다.

        if (promoted) { // 개선 후보가 있으면 챔피언으로 승격한다.
            saveWeights(bestWeightsJson); // 최고 가중치를 운영 설정으로 유지한다.
            demoteCurrentChampions(); // 기존 챔피언을 해제한다.
            String semver = "ar-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // 새 전략 버전을 생성한다.
            strategyVersionRepository.save(new StrategyVersionEntity(
                    semver,
                    hash(bestWeightsJson),
                    bestMetric,
                    LocalDateTime.now(),
                    true
            )); // 새 챔피언 전략 버전을 저장한다.
            saveStrategyWeights(semver, bestWeightsJson); // 롤백을 위해 전략별 가중치 스냅샷을 저장한다.
            saveStrategyYaml(semver, buildStrategyYaml(semver, simulationRequest, bestWeightsJson, null, bestMetric, "CHAMPION")); // 챔피언 전략 YAML 스냅샷을 저장한다.
        } else { // 개선 후보가 없으면 원래 가중치를 복구한다.
            saveWeights(originalWeightsJson); // 원래 가중치를 복구한다.
        } // 승격 여부 처리를 종료한다.
        return savedRuns.stream().map(this::toRunResponse).toList(); // 실행 결과를 반환한다.
    } // AutoResearch 자동 실행을 종료한다.

    private int normalizeIterations(Integer requestedIterations) { // 반복 횟수를 정규화한다.
        int defaultIterations = readIntSetting("autoresearch.targetIterations", 8); // 설정 기반 기본 반복 횟수를 읽는다.
        int iterations = requestedIterations == null ? defaultIterations : requestedIterations; // 요청값 또는 기본값을 사용한다.
        if (iterations < 1 || iterations > 80) { // 허용 범위를 확인한다.
            throw new CustomException("AutoResearch iterations must be between 1 and 80.", 400); // 범위 예외를 던진다.
        } // 범위 확인을 종료한다.
        return iterations; // 정규화된 반복 횟수를 반환한다.
    } // 반복 횟수 정규화를 종료한다.

    private int readIntSetting(String key, int defaultValue) { // 정수 설정을 읽는다.
        try { // JSON 파싱 실패를 처리한다.
            Optional<AppSettingEntity> setting = appSettingRepository.findById(key); // 설정을 조회한다.
            if (setting.isEmpty()) { // 설정이 없으면 확인한다.
                return defaultValue; // 기본값을 반환한다.
            } // 설정 없음 확인을 종료한다.
            JsonNode value = objectMapper.readTree(setting.get().getValueJson()).path("value"); // value 필드를 읽는다.
            return value.isNumber() ? value.asInt(defaultValue) : defaultValue; // 숫자면 설정값을 반환한다.
        } catch (Exception exception) { // 파싱 실패를 처리한다.
            return defaultValue; // 기본값을 반환한다.
        } // 예외 처리를 종료한다.
    } // 정수 설정 읽기를 종료한다.

    private Optional<StrategyVersionEntity> latestChampion() { // 최신 챔피언 전략을 조회한다.
        return strategyVersionRepository.findByChampion(true).stream()
                .max(Comparator.comparing(StrategyVersionEntity::getPromotedAt)); // 승격일이 가장 최근인 챔피언을 반환한다.
    } // 최신 챔피언 조회를 종료한다.

    private void demoteCurrentChampions() { // 현재 챔피언 전략들을 해제한다.
        List<StrategyVersionEntity> champions = strategyVersionRepository.findByChampion(true); // 기존 챔피언을 조회한다.
        champions.forEach(champion -> champion.updateChampion(false)); // 챔피언 플래그를 해제한다.
        strategyVersionRepository.saveAll(champions); // 변경사항을 저장한다.
    } // 기존 챔피언 해제를 종료한다.

    private void validateChampionRollback() { // 챔피언 라이브 검증 기간 경과 후 롤백 여부를 확인한다.
        Optional<StrategyVersionEntity> championOptional = latestChampion(); // 최신 챔피언을 조회한다.
        if (championOptional.isEmpty()) { // 챔피언이 없으면 종료한다.
            return; // 롤백할 대상이 없다.
        } // 챔피언 없음 확인을 종료한다.
        StrategyVersionEntity champion = championOptional.get(); // 챔피언 엔티티를 가져온다.
        int validationDays = readIntSetting("autoresearch.rollbackValidationDays", 7); // 검증 기간 설정을 읽는다.
        if (validationDays <= 0 || LocalDateTime.now().isBefore(champion.getPromotedAt().plusDays(validationDays))) { // 검증 기간이 아직 지나지 않았는지 확인한다.
            return; // 아직 롤백 판단을 하지 않는다.
        } // 검증 기간 확인을 종료한다.
        BigDecimal liveMetric = liveMetricForChampion(champion, validationDays); // 라이브 평가 지표를 계산한다.
        if (liveMetric == null) { // 평가 데이터가 없으면 확인한다.
            return; // 평가 없이 롤백하지 않는다.
        } // 평가 데이터 확인을 종료한다.
        BigDecimal rollbackThreshold = champion.getMetricValue().multiply(BigDecimal.valueOf(0.5)).setScale(4, RoundingMode.HALF_UP); // 챔피언 지표의 50% 기준을 계산한다.
        if (liveMetric.compareTo(rollbackThreshold) >= 0) { // 기준 이상이면 유지한다.
            return; // 롤백하지 않는다.
        } // 롤백 기준 확인을 종료한다.
        Optional<StrategyVersionEntity> rollbackTarget = strategyVersionRepository.findAll().stream()
                .filter(strategy -> !Objects.equals(strategy.getId(), champion.getId()))
                .max(Comparator.comparing(StrategyVersionEntity::getPromotedAt)); // 직전 전략을 찾는다.
        if (rollbackTarget.isEmpty()) { // 롤백 대상이 없으면 확인한다.
            return; // 복구할 전략이 없어 유지한다.
        } // 롤백 대상 확인을 종료한다.
        champion.updateChampion(false); // 현재 챔피언을 해제한다.
        StrategyVersionEntity target = rollbackTarget.get(); // 롤백 대상 전략을 가져온다.
        target.updateChampion(true); // 롤백 대상을 챔피언으로 승격한다.
        strategyVersionRepository.saveAll(List.of(champion, target)); // 챔피언 변경을 저장한다.
        loadStrategyWeights(target.getSemver()).ifPresent(this::saveWeights); // 저장된 가중치 스냅샷이 있으면 복구한다.
        auditLogRepository.save(new AuditLogEntity(
                "autoresearch",
                "ROLLBACK_CHAMPION:" + champion.getSemver(),
                "{\"liveMetric\":" + liveMetric + ",\"threshold\":" + rollbackThreshold + "}",
                "{\"champion\":\"" + target.getSemver() + "\"}"
        )); // 롤백 감사 로그를 저장한다.
    } // 챔피언 롤백 검증을 종료한다.

    private BigDecimal liveMetricForChampion(StrategyVersionEntity champion, int validationDays) { // 챔피언 라이브 평균 손익률을 계산한다.
        LocalDateTime from = champion.getPromotedAt(); // 검증 시작일을 승격 시각으로 둔다.
        LocalDateTime to = champion.getPromotedAt().plusDays(validationDays); // 검증 종료일을 계산한다.
        List<RecommendationEntity> recommendations = recommendationRepository.findByModelVersionAndGeneratedAtBetween(champion.getSemver(), from, to); // 해당 챔피언 추천을 조회한다.
        List<Long> recommendationIds = recommendations.stream().map(RecommendationEntity::getId).toList(); // 추천 ID 목록을 만든다.
        if (recommendationIds.isEmpty()) { // 추천이 없으면 확인한다.
            return null; // 평가 불가로 반환한다.
        } // 추천 없음 확인을 종료한다.
        List<BigDecimal> pnlValues = evaluationRepository.findByRecommendationIdIn(recommendationIds).stream()
                .map(EvaluationEntity::getPnlPct)
                .filter(value -> value != null)
                .toList(); // 평가 손익률 목록을 만든다.
        if (pnlValues.isEmpty()) { // 평가가 없으면 확인한다.
            return null; // 평가 불가로 반환한다.
        } // 평가 없음 확인을 종료한다.
        return pnlValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(pnlValues.size()), 4, RoundingMode.HALF_UP); // 평균 손익률을 반환한다.
    } // 챔피언 라이브 평균 손익률 계산을 종료한다.

    private String currentWeightsJson() { // 현재 가중치 JSON을 조회한다.
        return appSettingRepository.findById(SCORING_WEIGHTS_SETTING_KEY)
                .map(AppSettingEntity::getValueJson)
                .orElse(DEFAULT_WEIGHTS_JSON); // 설정이 없으면 기본 JSON을 반환한다.
    } // 현재 가중치 JSON 조회를 종료한다.

    private void saveWeights(String weightsJson) { // 가중치 설정을 저장한다.
        AppSettingEntity setting = appSettingRepository.findById(SCORING_WEIGHTS_SETTING_KEY)
                .orElseGet(() -> new AppSettingEntity(SCORING_WEIGHTS_SETTING_KEY, DEFAULT_WEIGHTS_JSON, "추천 feature 점수 가중치", "autoresearch")); // 없으면 새 설정을 생성한다.
        String beforeJson = setting.getValueJson(); // 변경 전 값을 보관한다.
        setting.updateValue(weightsJson, "autoresearch"); // 가중치 값을 갱신한다.
        appSettingRepository.save(setting); // 설정을 저장한다.
        if (!beforeJson.equals(weightsJson)) { // 실제 변경이 있을 때만 감사 로그를 남긴다.
            auditLogRepository.save(new AuditLogEntity("autoresearch", "UPDATE_SETTING:" + SCORING_WEIGHTS_SETTING_KEY, beforeJson, weightsJson)); // 설정 변경 감사 로그를 저장한다.
        } // 변경 여부 확인을 종료한다.
    } // 가중치 설정 저장을 종료한다.

    private void saveStrategyWeights(String semver, String weightsJson) { // 전략별 가중치 스냅샷을 저장한다.
        String key = strategyWeightsKey(semver); // 스냅샷 설정 키를 만든다.
        AppSettingEntity setting = appSettingRepository.findById(key)
                .orElseGet(() -> new AppSettingEntity(key, "{}", "AutoResearch 전략 가중치 스냅샷", "autoresearch")); // 없으면 새 설정을 생성한다.
        String beforeJson = setting.getValueJson(); // 변경 전 값을 보관한다.
        setting.updateValue(weightsJson, "autoresearch"); // 스냅샷 값을 갱신한다.
        appSettingRepository.save(setting); // 스냅샷을 저장한다.
        if (!beforeJson.equals(weightsJson)) { // 실제 변경이 있을 때만 감사 로그를 남긴다.
            auditLogRepository.save(new AuditLogEntity("autoresearch", "UPDATE_SETTING:" + key, beforeJson, weightsJson)); // 스냅샷 감사 로그를 저장한다.
        } // 변경 여부 확인을 종료한다.
    } // 전략별 가중치 스냅샷 저장을 종료한다.

    private Optional<String> loadStrategyWeights(String semver) { // 전략별 가중치 스냅샷을 조회한다.
        return appSettingRepository.findById(strategyWeightsKey(semver)).map(AppSettingEntity::getValueJson); // 스냅샷 JSON을 반환한다.
    } // 전략별 가중치 스냅샷 조회를 종료한다.

    private String strategyWeightsKey(String semver) { // 전략별 가중치 스냅샷 설정 키를 만든다.
        return "autoresearch.weights." + semver; // app_setting 키 길이 내에서 semver 기반 키를 반환한다.
    } // 전략별 가중치 스냅샷 설정 키 생성을 종료한다.

    private void saveStrategyYaml(String keySuffix, String yaml) { // AutoResearch 전략 YAML 스냅샷을 저장한다.
        String key = strategyYamlKey(keySuffix); // 스냅샷 설정 키를 만든다.
        AppSettingEntity setting = appSettingRepository.findById(key)
                .orElseGet(() -> new AppSettingEntity(key, "{}", "AutoResearch strategy.yaml 스냅샷", "autoresearch")); // 없으면 새 설정을 생성한다.
        String beforeJson = setting.getValueJson(); // 변경 전 값을 보관한다.
        String valueJson = buildYamlSettingJson(yaml); // YAML 문자열을 JSON 값으로 감싼다.
        setting.updateValue(valueJson, "autoresearch"); // 스냅샷 값을 갱신한다.
        appSettingRepository.save(setting); // 스냅샷을 저장한다.
        if (!beforeJson.equals(valueJson)) { // 실제 변경이 있으면 감사 로그를 남긴다.
            auditLogRepository.save(new AuditLogEntity("autoresearch", "UPDATE_SETTING:" + key, beforeJson, valueJson)); // 스냅샷 변경 감사 로그를 저장한다.
        } // 변경 여부 확인을 종료한다.
    } // AutoResearch 전략 YAML 스냅샷 저장을 종료한다.

    private String strategyYamlKey(String keySuffix) { // 전략 YAML 스냅샷 설정 키를 만든다.
        String safeSuffix = keySuffix == null || keySuffix.isBlank() ? "unknown" : keySuffix.replaceAll("[^A-Za-z0-9_.-]", "-"); // 안전한 키 suffix를 만든다.
        if (safeSuffix.length() > 20) { // app_setting 키 길이 제한을 넘지 않도록 줄인다.
            safeSuffix = safeSuffix.substring(0, 20); // suffix를 자른다.
        } // suffix 길이 확인을 종료한다.
        return "autoresearch.strategyYaml." + safeSuffix; // 전체 키를 반환한다.
    } // 전략 YAML 스냅샷 설정 키 생성을 종료한다.

    private String buildStrategyYaml(String name, BacktestSimulationRequest request, String weightsJson, String metricsJson, BigDecimal metricValue, String decision) { // strategy.yaml 내용을 만든다.
        StringBuilder builder = new StringBuilder(); // YAML 문자열을 구성한다.
        builder.append("name: ").append(name).append('\n'); // 이름을 기록한다.
        builder.append("strategy: recommendation-engine-v1\n"); // 전략 타입을 기록한다.
        builder.append("decision: ").append(decision).append('\n'); // 의사결정을 기록한다.
        builder.append("metricName: avgPnlPct\n"); // 평가 지표명을 기록한다.
        builder.append("metricValue: ").append(metricValue == null ? "null" : metricValue).append('\n'); // 평가 지표값을 기록한다.
        builder.append("backtest:\n"); // 백테스트 블록을 시작한다.
        builder.append("  market: ").append(request.market()).append('\n'); // 시장을 기록한다.
        builder.append("  periodFrom: ").append(request.periodFrom()).append('\n'); // 시작일을 기록한다.
        builder.append("  periodTo: ").append(request.periodTo()).append('\n'); // 종료일을 기록한다.
        builder.append("  maxTickers: ").append(request.maxTickers()).append('\n'); // 최대 종목 수를 기록한다.
        builder.append("  holdingDays: ").append(request.holdingDays()).append('\n'); // 보유 기간을 기록한다.
        builder.append("  targetPct: ").append(request.targetPct()).append('\n'); // 목표 수익률을 기록한다.
        builder.append("  stopPct: ").append(request.stopPct()).append('\n'); // 손절률을 기록한다.
        if (metricsJson != null && !metricsJson.isBlank()) { // 백테스트 메트릭이 있으면 기록한다.
            builder.append("metricsJson: |\n"); // 멀티라인 JSON 블록을 시작한다.
            builder.append(indentYamlBlock(metricsJson)); // 메트릭 JSON을 들여쓴다.
        } // 메트릭 기록을 종료한다.
        builder.append("weights: |\n"); // 가중치 JSON 블록을 시작한다.
        builder.append(indentYamlBlock(weightsJson)); // 가중치 JSON을 들여쓴다.
        return builder.toString(); // YAML 문자열을 반환한다.
    } // strategy.yaml 내용 생성을 종료한다.

    private String indentYamlBlock(String value) { // YAML block scalar 값을 들여쓴다.
        return value.lines()
                .map(line -> "  " + line)
                .reduce("", (left, right) -> left + right + "\n"); // 각 줄에 들여쓰기를 추가한다.
    } // YAML block scalar 들여쓰기를 종료한다.

    private String buildYamlSettingJson(String yaml) { // YAML 문자열을 app_setting JSON으로 감싼다.
        try { // JSON 직렬화 예외를 처리한다.
            ObjectNode root = objectMapper.createObjectNode(); // JSON 객체를 만든다.
            root.put("yaml", yaml); // YAML 문자열을 저장한다.
            return objectMapper.writeValueAsString(root); // JSON 문자열로 반환한다.
        } catch (Exception exception) { // 직렬화 예외를 잡는다.
            throw new CustomException("Failed to serialize strategy yaml: " + exception.getMessage(), 500); // 직렬화 실패 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // YAML app_setting JSON 생성을 종료한다.

    private String mutateWeights(String sourceJson, FeatureICService.MutationGuide mutationGuide) { // 가중치 JSON을 변형한다.
        try { // JSON 파싱 실패를 처리한다.
            ObjectNode root = (ObjectNode) objectMapper.readTree(sourceJson).deepCopy(); // 원본 JSON을 복사한다.
            String path = mutationGuide.weightPath(); // 이번 반복에서 바꿀 경로를 결정한다.
            String[] parts = path.split("\\."); // 경로를 분리한다.
            ObjectNode parent = (ObjectNode) root.path(parts[0]); // 부모 노드를 조회한다.
            BigDecimal current = parent.path(parts[1]).isNumber() ? parent.path(parts[1]).decimalValue() : BigDecimal.valueOf(0.1); // 현재 가중치를 읽는다.
            BigDecimal factor = mutationGuide.factor(); // IC 방향에 따른 증감 배율을 가져온다.
            parent.put(parts[1], current.multiply(factor)); // 변형 가중치를 저장한다.
            normalizeWeightGroup((ObjectNode) root.path("value")); // 종합 가중치 합을 1로 정규화한다.
            normalizeWeightGroup((ObjectNode) root.path("technical")); // 기술 가중치 합을 1로 정규화한다.
            normalizeWeightGroup((ObjectNode) root.path("context")); // 컨텍스트 가중치 합을 1로 정규화한다.
            return objectMapper.writeValueAsString(root); // 변형 JSON을 반환한다.
        } catch (Exception exception) { // JSON 파싱 실패를 처리한다.
            throw new CustomException("Failed to mutate scoring weights: " + exception.getMessage(), 500); // 변형 실패 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // 가중치 변형을 종료한다.

    private void normalizeWeightGroup(ObjectNode node) { // 같은 그룹 내 가중치 합을 1로 맞춘다.
        List<String> fieldNames = new ArrayList<>(); // 필드명 목록을 준비한다.
        node.fieldNames().forEachRemaining(fieldNames::add); // 필드명을 수집한다.
        BigDecimal sum = fieldNames.stream()
                .map(name -> node.path(name).isNumber() ? node.path(name).decimalValue().max(BigDecimal.ZERO) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 양수 가중치 합을 계산한다.
        if (sum.compareTo(BigDecimal.ZERO) <= 0) { // 합이 0이면 확인한다.
            return; // 정규화하지 않는다.
        } // 합 확인을 종료한다.
        for (String fieldName : fieldNames) { // 각 필드를 순회한다.
            BigDecimal value = node.path(fieldName).isNumber() ? node.path(fieldName).decimalValue().max(BigDecimal.ZERO) : BigDecimal.ZERO; // 음수는 0으로 본다.
            node.put(fieldName, value.divide(sum, 6, RoundingMode.HALF_UP)); // 합 1 기준 값으로 저장한다.
        } // 필드 순회를 종료한다.
    } // 가중치 그룹 정규화를 종료한다.

    private Integer durationMs(LocalDateTime startedAt) { // 소요 시간을 밀리초로 계산한다.
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, Duration.between(startedAt, LocalDateTime.now()).toMillis())); // int 범위로 제한해 반환한다.
    } // 소요 시간 계산을 종료한다.

    private String hash(String value) { // 문자열 SHA-256 해시를 계산한다.
        try { // 해시 실패를 처리한다.
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // SHA-256 인스턴스를 생성한다.
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8)); // 해시 바이트를 계산한다.
            StringBuilder builder = new StringBuilder(); // hex 문자열 빌더를 생성한다.
            for (byte b : bytes) { // 각 바이트를 순회한다.
                builder.append(String.format("%02x", b)); // hex로 추가한다.
            } // 바이트 순회를 종료한다.
            return builder.toString(); // hex 해시를 반환한다.
        } catch (Exception exception) { // 해시 실패를 처리한다.
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""); // fallback 해시를 반환한다.
        } // 예외 처리를 종료한다.
    } // 문자열 해시 계산을 종료한다.

    private AutoresearchRunResponse toRunResponse(AutoresearchRunEntity entity) { // AutoResearch 실행 엔티티를 응답 DTO로 변환한다.
        return new AutoresearchRunResponse(entity.getId(), entity.getJobRunId(), entity.getIterNo(), entity.getParentSha(), entity.getProposalSha(), entity.getDiffSummary(), entity.getMetricName(), entity.getMetricValue(), entity.getChampionMetric(), entity.getDecision(), entity.getDurationMs(), entity.getStartedAt(), entity.getEndedAt()); // AutoResearch 실행 응답 DTO를 생성한다.
    } // AutoResearch 실행 DTO 변환을 종료한다.

    private StrategyVersionResponse toStrategyResponse(StrategyVersionEntity entity) { // 전략 버전 엔티티를 응답 DTO로 변환한다.
        return new StrategyVersionResponse(entity.getId(), entity.getSemver(), entity.getGitSha(), entity.getMetricValue(), entity.getPromotedAt(), entity.getChampion()); // 전략 버전 응답 DTO를 생성한다.
    } // 전략 버전 DTO 변환을 종료한다.
} // AutoResearch 서비스를 종료한다.
