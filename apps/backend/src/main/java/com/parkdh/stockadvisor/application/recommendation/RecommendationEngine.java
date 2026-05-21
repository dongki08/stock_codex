package com.parkdh.stockadvisor.application.recommendation; // 추천 애플리케이션 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드 타입을 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.application.feature.UniverseFeature; // 시장 후보군 feature 값을 가져온다.
import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder; // 시장 후보군 feature 빌더를 가져온다.
import com.parkdh.stockadvisor.domain.instrument.InstrumentEntity; // 수동 종목 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 앱 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository; // 종목 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 앱 설정 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Set; // 집합 타입을 가져온다.
import java.util.stream.Collectors; // 스트림 수집 도구를 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class RecommendationEngine { // 추천 후보 선별 엔진을 정의한다.
    private static final String MIN_SCORE_SETTING_KEY = "recommendation.feature.minTotalScore"; // 최소 feature 점수 설정 키를 정의한다.
    private static final String MIN_QUALITY_SETTING_KEY = "recommendation.feature.minDataQualityScore"; // 최소 데이터 품질 점수 설정 키를 정의한다.
    private static final String EXCLUDED_SECTORS_SETTING_KEY = "recommendation.excluded.sectors"; // 제외 섹터 설정 키를 정의한다.
    private static final String WATCHLIST_SETTING_KEY = "recommendation.watchlist"; // 관심/제외 종목 설정 키를 정의한다.

    private final UniverseFeatureBuilder universeFeatureBuilder; // 시장 후보군 feature 빌더 의존성을 보관한다.
    private final InstrumentRepository instrumentRepository; // 수동 종목 저장소 의존성을 보관한다.
    private final AppSettingRepository appSettingRepository; // 앱 설정 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서 의존성을 보관한다.

    public List<RecommendationCandidate> buildCandidates(String market) { // 시장 조건에 맞는 추천 후보 목록을 만든다.
        RecommendationFilter filter = resolveFilter(); // 추천 필터 설정을 조회한다.
        List<RecommendationCandidate> universeCandidates = universeFeatureBuilder.buildFeatures(market).stream()
                .map(this::fromFeature) // feature를 추천 후보로 변환한다.
                .filter(candidate -> matchesFilter(candidate, filter)) // 운영 필터 기준을 적용한다.
                .sorted(Comparator.comparing(RecommendationCandidate::score).reversed().thenComparing(RecommendationCandidate::ticker)) // 점수와 티커 기준으로 정렬한다.
                .toList(); // 후보 목록으로 수집한다.
        if (!universeCandidates.isEmpty()) { // 시장 후보군 기반 후보가 있는지 확인한다.
            return universeCandidates; // 시장 후보군 기반 후보를 반환한다.
        } // 시장 후보군 후보 확인을 종료한다.
        return findInstrumentFallback(market).stream()
                .map(this::fromInstrument) // 수동 종목을 추천 후보로 변환한다.
                .filter(candidate -> matchesFilter(candidate, filter)) // 운영 필터 기준을 적용한다.
                .sorted(Comparator.comparing(RecommendationCandidate::score).reversed().thenComparing(RecommendationCandidate::ticker)) // 점수와 티커 기준으로 정렬한다.
                .toList(); // fallback 후보 목록으로 수집한다.
    } // 추천 후보 목록 생성을 종료한다.

    public List<RecommendationCandidate> selectTopCandidates(String market, int count) { // 상위 추천 후보를 선택한다.
        return buildCandidates(market).stream().limit(count).toList(); // 요청 개수만큼 후보를 반환한다.
    } // 상위 추천 후보 선택을 종료한다.

    private List<InstrumentEntity> findInstrumentFallback(String market) { // 수동 종목 fallback 후보를 조회한다.
        if (market == null || market.isBlank() || "ALL".equals(market)) { // 전체 시장 요청인지 확인한다.
            return instrumentRepository.findByEnabled(true); // 전체 활성 수동 종목을 반환한다.
        } // 전체 시장 확인을 종료한다.
        return instrumentRepository.findByMarketAndEnabled(market, true); // 시장별 활성 수동 종목을 반환한다.
    } // 수동 종목 fallback 후보 조회를 종료한다.

    private RecommendationCandidate fromFeature(UniverseFeature feature) { // feature 값을 추천 후보로 변환한다.
        return new RecommendationCandidate(feature.ticker(), feature.market(), feature.lastPrice(), feature.entity().getSector(), "market_universe", feature.totalScore(), feature.dataQualityScore(), feature.featureJson()); // 시장 후보군 추천 후보를 반환한다.
    } // feature 추천 후보 변환을 종료한다.

    private RecommendationCandidate fromInstrument(InstrumentEntity entity) { // 수동 종목을 추천 후보로 변환한다.
        String featureJson = "{\"source\":\"instrument_fallback\",\"totalScore\":50}"; // fallback feature JSON을 만든다.
        return new RecommendationCandidate(entity.getTicker(), entity.getMarket(), null, entity.getSector(), "instrument_fallback", 50, 40, featureJson); // 수동 종목 추천 후보를 반환한다.
    } // 수동 종목 추천 후보 변환을 종료한다.

    private boolean matchesFilter(RecommendationCandidate candidate, RecommendationFilter filter) { // 후보가 운영 필터를 통과하는지 확인한다.
        if (candidate.score() < filter.minTotalScore()) { // 최소 종합 점수 미만인지 확인한다.
            return false; // 추천 후보에서 제외한다.
        } // 최소 종합 점수 확인을 종료한다.
        if (candidate.dataQualityScore() < filter.minDataQualityScore()) { // 최소 데이터 품질 점수 미만인지 확인한다.
            return false; // 추천 후보에서 제외한다.
        } // 최소 데이터 품질 확인을 종료한다.
        if (filter.excludedTickers().contains(candidate.ticker())) { // 제외 종목인지 확인한다.
            return false; // 추천 후보에서 제외한다.
        } // 제외 종목 확인을 종료한다.
        return candidate.sector() == null || !filter.excludedSectors().contains(candidate.sector()); // 제외 섹터가 아니면 true를 반환한다.
    } // 운영 필터 통과 여부 확인을 종료한다.

    private RecommendationFilter resolveFilter() { // 추천 필터 설정을 조회한다.
        int minTotalScore = resolveIntSetting(MIN_SCORE_SETTING_KEY, 0); // 최소 종합 점수를 조회한다.
        int minDataQualityScore = resolveIntSetting(MIN_QUALITY_SETTING_KEY, 0); // 최소 데이터 품질 점수를 조회한다.
        Set<String> excludedSectors = resolveStringArraySetting(EXCLUDED_SECTORS_SETTING_KEY, "value"); // 제외 섹터 목록을 조회한다.
        Set<String> excludedTickers = resolveStringArraySetting(WATCHLIST_SETTING_KEY, "exclude"); // 제외 종목 목록을 조회한다.
        return new RecommendationFilter(minTotalScore, minDataQualityScore, excludedSectors, excludedTickers); // 추천 필터를 반환한다.
    } // 추천 필터 설정 조회를 종료한다.

    private int resolveIntSetting(String key, int defaultValue) { // 정수 설정을 조회한다.
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson) // 설정 JSON을 가져온다.
                .map(this::extractIntValue) // 정수 값을 추출한다.
                .orElse(defaultValue); // 설정이 없으면 기본값을 반환한다.
    } // 정수 설정 조회를 종료한다.

    private Set<String> resolveStringArraySetting(String key, String fieldName) { // 문자열 배열 설정을 조회한다.
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson) // 설정 JSON을 가져온다.
                .map(valueJson -> extractStringArray(valueJson, fieldName)) // 문자열 배열을 추출한다.
                .orElse(Set.of()); // 설정이 없으면 빈 집합을 반환한다.
    } // 문자열 배열 설정 조회를 종료한다.

    private int extractIntValue(String valueJson) { // 설정 JSON에서 value 정수를 추출한다.
        try { // 간단한 JSON 추출 예외를 처리한다.
            JsonNode root = objectMapper.readTree(valueJson); // 설정 JSON을 파싱한다.
            return root.path("value").asInt(0); // value 정수를 반환한다.
        } catch (Exception exception) { // 파싱 실패를 처리한다.
            return 0; // 기본값을 반환한다.
        } // 예외 처리를 종료한다.
    } // value 정수 추출을 종료한다.

    private Set<String> extractStringArray(String valueJson, String fieldName) { // 설정 JSON에서 문자열 배열을 추출한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode arrayNode = objectMapper.readTree(valueJson).path(fieldName); // 배열 노드를 가져온다.
            if (!arrayNode.isArray()) { // 배열인지 확인한다.
                return Set.of(); // 배열이 아니면 빈 집합을 반환한다.
            } // 배열 확인을 종료한다.
            java.util.ArrayList<String> values = new java.util.ArrayList<>(); // 문자열 목록을 생성한다.
            arrayNode.forEach(node -> { // 배열 노드를 순회한다.
                String value = node.asText("").trim(); // 문자열 값을 가져온다.
                if (!value.isBlank()) { // 빈 문자열이 아닌지 확인한다.
                    values.add(value); // 값을 추가한다.
                } // 빈 문자열 확인을 종료한다.
            }); // 배열 순회를 종료한다.
            return values.stream().collect(Collectors.toSet()); // 집합으로 반환한다.
        } catch (Exception exception) { // 파싱 실패를 처리한다.
            return Set.of(); // 빈 집합을 반환한다.
        } // 예외 처리를 종료한다.
    } // 문자열 배열 추출을 종료한다.

    private record RecommendationFilter(int minTotalScore, int minDataQualityScore, Set<String> excludedSectors, Set<String> excludedTickers) { // 추천 필터 설정을 정의한다.
    } // 추천 필터 설정을 종료한다.
} // 추천 후보 선별 엔진을 종료한다.
