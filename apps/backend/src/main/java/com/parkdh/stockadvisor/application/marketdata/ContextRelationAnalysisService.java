package com.parkdh.stockadvisor.application.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.domain.marketdata.ContextRelationAnalysisEntity;
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity;
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.ContextRelationAnalysisRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContextRelationAnalysisService {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int INPUT_LIMIT_PER_TYPE = 3;
    private static final Set<String> DIRECTIONS = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final NewsArticleRepository newsArticleRepository;
    private final DisclosureEventRepository disclosureEventRepository;
    private final ContextRelationAnalysisRepository contextRelationAnalysisRepository;
    private final AppSettingRepository appSettingRepository;
    private final CodexClient codexClient;
    private final ObjectMapper objectMapper;

    public int analyzeMarket(String market, List<MarketUniverseEntity> candidates) {
        if (!isEnabled() || candidates == null || candidates.isEmpty()) return 0;

        LocalDate today = LocalDate.now(SEOUL_ZONE);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);
        List<CandidateContext> contexts = candidates.stream()
                .map(candidate -> loadContext(market, candidate, from, to))
                .filter(CandidateContext::hasInputs)
                .toList();
        if (contexts.isEmpty()) return 0;

        CodexClient.CodexResult result = codexClient.call(
                buildPrompt(market, today, contexts),
                "context-relation",
                "context-relation-" + market
        );
        if (!result.succeeded()) {
            log.warn("Context relation analysis failed. market={}, error={}", market, result.errorMessage());
            return 0;
        }

        try {
            JsonNode root = objectMapper.readTree(extractJsonArray(result.response()));
            if (!root.isArray()) return 0;
            List<ContextRelationAnalysisEntity> entities = new ArrayList<>();
            for (JsonNode item : root) {
                toEntity(market, today, item).ifPresent(entities::add);
            }
            if (entities.isEmpty()) return 0;
            contextRelationAnalysisRepository.saveAll(entities);
            return entities.size();
        } catch (Exception exception) {
            log.warn("Context relation response parsing failed. market={}, error={}", market, exception.getMessage());
            return 0;
        }
    }

    public Optional<ContextRelationAnalysisEntity> find(String market, String ticker, LocalDate date) {
        return contextRelationAnalysisRepository.findByMarketAndTickerAndAnalysisDate(market, ticker, date);
    }

    private CandidateContext loadContext(String market, MarketUniverseEntity candidate, LocalDateTime from, LocalDateTime to) {
        List<NewsArticleEntity> news = newsArticleRepository.findByMarketAndTickerAndPublishedAtBetweenOrderByPublishedAtDesc(
                market, candidate.getTicker(), from, to, PageRequest.of(0, INPUT_LIMIT_PER_TYPE));
        List<DisclosureEventEntity> disclosures = disclosureEventRepository.findByMarketAndTickerAndDisclosedAtBetweenOrderByDisclosedAtDesc(
                market, candidate.getTicker(), from, to, PageRequest.of(0, INPUT_LIMIT_PER_TYPE));
        return new CandidateContext(candidate, news, disclosures);
    }

    private String buildPrompt(String market, LocalDate date, List<CandidateContext> contexts) {
        StringBuilder prompt = new StringBuilder("""
                Analyze relationships between today's news and disclosures for the listed stocks.
                Return JSON array only. One object per ticker with:
                ticker, direction(POSITIVE|NEUTRAL|NEGATIVE), confidence(0-100),
                riskLevel(LOW|MEDIUM|HIGH), score(0-100), summary,
                keyFactors(string array), contradictions(string array).
                Do not recommend prices or trades. Use only the supplied facts.
                market=%s date=%s
                """.formatted(market, date));
        for (CandidateContext context : contexts) {
            prompt.append("\nTICKER ").append(context.candidate().getTicker())
                    .append(" NAME ").append(sanitize(context.candidate().getName())).append('\n');
            context.news().forEach(news -> prompt.append("NEWS: ").append(sanitize(news.getTitle()))
                    .append(" | ").append(sanitize(news.getSummary())).append('\n'));
            context.disclosures().forEach(disclosure -> prompt.append("DISCLOSURE: ")
                    .append(sanitize(disclosure.getTitle())).append(" | type=")
                    .append(sanitize(disclosure.getDisclosureType())).append(" | importance=")
                    .append(disclosure.getImportanceScore()).append('\n'));
        }
        return prompt.toString();
    }

    private Optional<ContextRelationAnalysisEntity> toEntity(String market, LocalDate date, JsonNode item) throws Exception {
        String ticker = item.path("ticker").asText("").trim();
        String direction = item.path("direction").asText("").toUpperCase(Locale.ROOT);
        String riskLevel = item.path("riskLevel").asText("").toUpperCase(Locale.ROOT);
        if (ticker.isBlank() || !DIRECTIONS.contains(direction) || !RISK_LEVELS.contains(riskLevel)) {
            return Optional.empty();
        }
        int confidence = clamp(item.path("confidence").asInt(50));
        int score = clamp(item.path("score").asInt(50));
        String summary = item.path("summary").asText("");
        String keyFactors = objectMapper.writeValueAsString(item.path("keyFactors").isArray() ? item.path("keyFactors") : objectMapper.createArrayNode());
        String contradictions = objectMapper.writeValueAsString(item.path("contradictions").isArray() ? item.path("contradictions") : objectMapper.createArrayNode());
        ContextRelationAnalysisEntity entity = contextRelationAnalysisRepository.findByMarketAndTickerAndAnalysisDate(market, ticker, date)
                .orElseGet(() -> new ContextRelationAnalysisEntity(
                        ticker, market, date, direction, confidence, riskLevel, score, summary,
                        keyFactors, contradictions, "codex-context-relation-v1", LocalDateTime.now(SEOUL_ZONE)
                ));
        entity.update(direction, confidence, riskLevel, score, summary, keyFactors, contradictions,
                "codex-context-relation-v1", LocalDateTime.now(SEOUL_ZONE));
        return Optional.of(entity);
    }

    private boolean isEnabled() {
        return appSettingRepository.findById("context.relation.codex.enabled")
                .map(setting -> {
                    try {
                        return objectMapper.readTree(setting.getValueJson()).path("value").asBoolean(true);
                    } catch (Exception ignored) {
                        return true;
                    }
                })
                .orElse(true);
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start < 0 || end < start) throw new IllegalArgumentException("JSON array not found");
        return response.substring(start, end + 1);
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record CandidateContext(MarketUniverseEntity candidate, List<NewsArticleEntity> news,
                                    List<DisclosureEventEntity> disclosures) {
        private boolean hasInputs() {
            return !news.isEmpty() || !disclosures.isEmpty();
        }
    }
}
