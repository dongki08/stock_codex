package com.parkdh.stockadvisor.application.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.application.feature.UniverseFeature;
import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder;
import com.parkdh.stockadvisor.domain.instrument.InstrumentEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.instrument.InstrumentRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RecommendationEngine {
    private static final String MIN_SCORE_SETTING_KEY = "recommendation.feature.minTotalScore";
    private static final String MIN_QUALITY_SETTING_KEY = "recommendation.feature.minDataQualityScore";
    private static final String EXCLUDED_SECTORS_SETTING_KEY = "recommendation.excluded.sectors";
    private static final String WATCHLIST_SETTING_KEY = "recommendation.watchlist";
    private static final String KR_MIN_MARKET_CAP_SETTING_KEY = "recommendation.marketcap.kr.min";
    private static final String US_MIN_MARKET_CAP_SETTING_KEY = "recommendation.marketcap.us.min";
    private static final String KR_MIN_TURNOVER_SETTING_KEY = "recommendation.turnover.kr.min";
    private static final String US_MIN_TURNOVER_SETTING_KEY = "recommendation.turnover.us.min";

    private final UniverseFeatureBuilder universeFeatureBuilder;
    private final InstrumentRepository instrumentRepository;
    private final AppSettingRepository appSettingRepository;
    private final PriceDailyRepository priceDailyRepository;
    private final ObjectMapper objectMapper;

    public List<RecommendationCandidate> buildCandidates(String market) {
        if (!isMarketRegimeOk(market)) {
            return List.of();
        }
        RecommendationFilter filter = resolveFilter();
        List<RecommendationCandidate> universeCandidates = universeFeatureBuilder.buildFeatures(market).stream()
                .map(this::fromFeature)
                .filter(candidate -> matchesFilter(candidate, filter))
                .sorted(candidateComparator())
                .toList();
        if (!universeCandidates.isEmpty()) {
            return universeCandidates;
        }
        return findInstrumentFallback(market).stream()
                .map(this::fromInstrument)
                .filter(candidate -> matchesFilter(candidate, filter))
                .sorted(candidateComparator())
                .toList();
    }

    public List<RecommendationCandidate> selectTopCandidates(String market, int count) {
        int maxPerSector = resolveIntSetting("recommendation.sector.max", 2);
        if (maxPerSector <= 0) {
            return buildCandidates(market).stream().limit(count).toList();
        }
        Map<String, Integer> sectorCounts = new LinkedHashMap<>();
        return buildCandidates(market).stream()
                .filter(candidate -> {
                    String sector = normalizeSector(candidate.sector());
                    int used = sectorCounts.getOrDefault(sector, 0);
                    if (used >= maxPerSector) {
                        return false;
                    }
                    sectorCounts.put(sector, used + 1);
                    return true;
                })
                .limit(count)
                .toList();
    }

    private boolean isMarketRegimeOk(String market) {
        if (!resolveBooleanSetting("recommendation.regime.filter.enabled", false)) {
            return true;
        }
        String indexTicker = indexTickerForMarket(market);
        if (indexTicker == null) {
            return true;
        }
        List<PriceDailyEntity> history = priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(market, indexTicker, PageRequest.of(0, 200));
        if (history.size() < 200) {
            return true;
        }
        BigDecimal latestClose = history.get(0).getClosePrice();
        BigDecimal ma200 = history.stream()
                .map(PriceDailyEntity::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 8, RoundingMode.HALF_UP);
        return latestClose.compareTo(ma200) >= 0;
    }

    private String indexTickerForMarket(String market) {
        if ("KOSPI".equals(market) || "KOSDAQ".equals(market)) {
            return "KOSPI";
        }
        if ("NASDAQ".equals(market) || "NYSE".equals(market) || "US".equals(market)) {
            return "SPY";
        }
        return null;
    }

    private Comparator<RecommendationCandidate> candidateComparator() {
        return Comparator.comparing(RecommendationCandidate::score).reversed().thenComparing(RecommendationCandidate::ticker);
    }

    private String normalizeSector(String sector) {
        return sector == null || sector.isBlank() ? "UNKNOWN" : sector;
    }

    private List<InstrumentEntity> findInstrumentFallback(String market) {
        if (market == null || market.isBlank() || "ALL".equals(market)) {
            return instrumentRepository.findByEnabled(true);
        }
        return instrumentRepository.findByMarketAndEnabled(market, true);
    }

    private RecommendationCandidate fromFeature(UniverseFeature feature) {
        return new RecommendationCandidate(
                feature.ticker(),
                feature.entity().getName(),
                feature.market(),
                feature.lastPrice(),
                feature.entity().getMarketCap(),
                feature.entity().getAvgTurnover(),
                feature.entity().getSector(),
                "market_universe",
                feature.totalScore(),
                feature.dataQualityScore(),
                feature.featureJson()
        );
    }

    private RecommendationCandidate fromInstrument(InstrumentEntity entity) {
        String featureJson = "{\"source\":\"instrument_fallback\",\"totalScore\":50}";
        return new RecommendationCandidate(entity.getTicker(), entity.getName(), entity.getMarket(), null, null, null, entity.getSector(), "instrument_fallback", 50, 40, featureJson);
    }

    private boolean matchesFilter(RecommendationCandidate candidate, RecommendationFilter filter) {
        if (candidate.score() < filter.minTotalScore()) {
            return false;
        }
        if (candidate.dataQualityScore() < filter.minDataQualityScore()) {
            return false;
        }
        if (filter.excludedTickers().contains(candidate.ticker())) {
            return false;
        }
        if (candidate.sector() != null && filter.excludedSectors().contains(candidate.sector())) {
            return false;
        }
        if (!matchesMarketCap(candidate, filter)) {
            return false;
        }
        return matchesTurnover(candidate, filter);
    }

    private RecommendationFilter resolveFilter() {
        int minTotalScore = resolveIntSetting(MIN_SCORE_SETTING_KEY, 0);
        int minDataQualityScore = resolveIntSetting(MIN_QUALITY_SETTING_KEY, 0);
        Set<String> excludedSectors = resolveStringArraySetting(EXCLUDED_SECTORS_SETTING_KEY, "value");
        Set<String> excludedTickers = resolveStringArraySetting(WATCHLIST_SETTING_KEY, "exclude");
        BigDecimal krMinMarketCap = resolveDecimalSetting(KR_MIN_MARKET_CAP_SETTING_KEY, BigDecimal.ZERO);
        BigDecimal usMinMarketCap = resolveDecimalSetting(US_MIN_MARKET_CAP_SETTING_KEY, BigDecimal.ZERO);
        BigDecimal krMinTurnover = resolveDecimalSetting(KR_MIN_TURNOVER_SETTING_KEY, BigDecimal.ZERO);
        BigDecimal usMinTurnover = resolveDecimalSetting(US_MIN_TURNOVER_SETTING_KEY, BigDecimal.ZERO);
        return new RecommendationFilter(minTotalScore, minDataQualityScore, excludedSectors, excludedTickers, krMinMarketCap, usMinMarketCap, krMinTurnover, usMinTurnover);
    }

    private boolean matchesMarketCap(RecommendationCandidate candidate, RecommendationFilter filter) {
        BigDecimal minMarketCap = isKoreanMarket(candidate.market()) ? filter.krMinMarketCap() : isUsMarket(candidate.market()) ? filter.usMinMarketCap() : BigDecimal.ZERO;
        return matchesMinIfKnown(candidate.marketCap(), minMarketCap);
    }

    private boolean matchesTurnover(RecommendationCandidate candidate, RecommendationFilter filter) {
        BigDecimal minTurnover = isKoreanMarket(candidate.market()) ? filter.krMinTurnover() : isUsMarket(candidate.market()) ? filter.usMinTurnover() : BigDecimal.ZERO;
        return matchesMinIfKnown(candidate.avgTurnover(), minTurnover);
    }

    private boolean matchesMinIfKnown(BigDecimal actual, BigDecimal minimum) {
        return actual == null || minimum == null || minimum.compareTo(BigDecimal.ZERO) <= 0 || actual.compareTo(minimum) >= 0;
    }

    private boolean isKoreanMarket(String market) {
        return "KOSPI".equals(market) || "KOSDAQ".equals(market);
    }

    private boolean isUsMarket(String market) {
        return "NASDAQ".equals(market) || "NYSE".equals(market) || "US".equals(market);
    }

    private int resolveIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractIntValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private boolean resolveBooleanSetting(String key, boolean defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractBooleanValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private BigDecimal resolveDecimalSetting(String key, BigDecimal defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractDecimalValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private Set<String> resolveStringArraySetting(String key, String fieldName) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractStringArray(valueJson, fieldName))
                .orElse(Set.of());
    }

    private int extractIntValue(String valueJson, int defaultValue) {
        try {
            JsonNode root = objectMapper.readTree(valueJson);
            return root.path("value").asInt(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String valueJson, boolean defaultValue) {
        try {
            JsonNode root = objectMapper.readTree(valueJson);
            return root.path("value").asBoolean(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private BigDecimal extractDecimalValue(String valueJson, BigDecimal defaultValue) {
        try {
            JsonNode valueNode = objectMapper.readTree(valueJson).path("value");
            return valueNode.isNumber() ? valueNode.decimalValue() : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private Set<String> extractStringArray(String valueJson, String fieldName) {
        try {
            JsonNode arrayNode = objectMapper.readTree(valueJson).path(fieldName);
            if (!arrayNode.isArray()) {
                return Set.of();
            }
            java.util.ArrayList<String> values = new java.util.ArrayList<>();
            arrayNode.forEach(node -> {
                String value = node.asText("").trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            });
            return values.stream().collect(Collectors.toSet());
        } catch (Exception exception) {
            return Set.of();
        }
    }

    private record RecommendationFilter(
            int minTotalScore,
            int minDataQualityScore,
            Set<String> excludedSectors,
            Set<String> excludedTickers,
            BigDecimal krMinMarketCap,
            BigDecimal usMinMarketCap,
            BigDecimal krMinTurnover,
            BigDecimal usMinTurnover
    ) {
    }
}
