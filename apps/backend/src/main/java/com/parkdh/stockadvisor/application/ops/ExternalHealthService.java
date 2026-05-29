package com.parkdh.stockadvisor.application.ops; // 운영 애플리케이션 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.ops.dto.ComponentHealthResponse; // 개별 외부 연동 상태 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.ops.dto.ExternalHealthResponse; // 외부 연동 상태 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.config.CodexCliProperties; // Codex CLI 설정을 가져온다.
import com.parkdh.stockadvisor.config.DartProperties; // DART 설정을 가져온다.
import com.parkdh.stockadvisor.config.KisProperties; // KIS 설정을 가져온다.
import com.parkdh.stockadvisor.config.SecProperties; // SEC 설정을 가져온다.
import com.parkdh.stockadvisor.config.SentimentAnalysisProperties;
import com.parkdh.stockadvisor.config.TelegramProperties; // Telegram 설정을 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity;
import com.parkdh.stockadvisor.global.util.MarketUtil; // 시장 공통 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.ops.ExternalApiPingClient;
import com.parkdh.stockadvisor.infrastructure.persistence.codex.CodexCallRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List; // 목록 타입을 가져온다.
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class ExternalHealthService { // 외부 연동 상태 서비스를 정의한다.
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

    private final KisProperties kisProperties; // KIS 설정 의존성을 보관한다.
    private final TelegramProperties telegramProperties; // Telegram 설정 의존성을 보관한다.
    private final CodexCliProperties codexCliProperties; // Codex CLI 설정 의존성을 보관한다.
    private final DartProperties dartProperties; // DART 설정 의존성을 보관한다.
    private final SecProperties secProperties; // SEC 설정 의존성을 보관한다.
    private final SentimentAnalysisProperties sentimentAnalysisProperties;
    private final CodexCallRepository codexCallRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    private final PriceDailyRepository priceDailyRepository;
    private final PriceIntradayRepository priceIntradayRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final DisclosureEventRepository disclosureEventRepository;
    private final MacroObservationRepository macroObservationRepository;
    private final FundamentalMetricRepository fundamentalMetricRepository;
    private final ExternalApiPingClient externalApiPingClient;

    public ExternalHealthResponse getExternalHealth() { // 외부 연동 상태를 조회한다.
        List<ComponentHealthResponse> components = new ArrayList<>(List.of(
                configured("KIS", kisProperties.appKey(), "KIS API 키가 설정되어 있습니다.", "KIS API 키가 dev-placeholder입니다."),
                configured("Telegram", telegramProperties.botToken(), "Telegram Bot Token이 설정되어 있습니다.", "Telegram Bot Token이 dev-placeholder입니다."),
                configured("Codex CLI", codexCliProperties.command(), "Codex CLI 명령이 설정되어 있습니다.", "Codex CLI 명령이 dev-placeholder입니다."),
                codexDailyUsage(),
                codexDailyBudget(),
                configured("DART", dartProperties.apiKey(), "DART API 키가 설정되어 있습니다.", "DART API 키가 dev-placeholder입니다."),
                configured("SEC EDGAR", secProperties.userAgent(), "SEC User-Agent가 설정되어 있습니다.", "SEC User-Agent가 dev-placeholder입니다."),
                publicSource("RSS", "Google News/Yahoo Finance RSS 공개 피드를 사용합니다."),
                publicSource("FRED", "FRED 공개 CSV 소스를 사용하며 기본 지표는 별도 키가 필요 없습니다."),
                publicSource("Stooq", "공개 CSV 소스를 사용하며 별도 키가 필요 없습니다."),
                publicSource("KIND", "KRX KIND 상장법인 공개 목록을 사용하며 별도 키가 필요 없습니다."),
                priceDailyData(),
                priceIntradayData(),
                newsData(),
                disclosureData(),
                macroData(),
                fundamentalData()
        )); // 구성 요소 상태 목록을 만든다.
        components.addAll(externalApiPingComponents());
        return new ExternalHealthResponse(LocalDateTime.now(), components); // 점검 시각과 상태 목록을 반환한다.
    } // 외부 연동 상태 조회를 종료한다.

    private ComponentHealthResponse configured(String name, String value, String readyMessage, String missingMessage) { // 설정 기반 외부 연동 상태를 만든다.
        if (value == null || value.isBlank() || MarketUtil.isDevPlaceholder(value)) { // 설정값이 없거나 개발용 placeholder인지 확인한다.
            return new ComponentHealthResponse(name, "MISSING_CONFIG", missingMessage); // 설정 누락 상태를 반환한다.
        } // 설정값 확인을 종료한다.
        return new ComponentHealthResponse(name, "READY", readyMessage); // 준비 완료 상태를 반환한다.
    } // 설정 기반 외부 연동 상태 생성을 종료한다.

    private ComponentHealthResponse publicSource(String name, String message) { // 공개 소스 상태를 만든다.
        return new ComponentHealthResponse(name, "PUBLIC_SOURCE", message); // 공개 소스 상태를 반환한다.
    } // 공개 소스 상태 생성을 종료한다.

    private List<ComponentHealthResponse> externalApiPingComponents() {
        return List.of(
                configuredPing("KIS", kisProperties.appKey(), kisProperties.baseUrl(), Map.of()),
                configuredPing("DART", dartProperties.apiKey(), "https://opendart.fss.or.kr/api/list.json?crtfc_key=" + encode(dartProperties.apiKey()) + "&page_no=1&page_count=1", Map.of()),
                configuredPing("SEC EDGAR", secProperties.userAgent(), "https://data.sec.gov/submissions/CIK0000320193.json", Map.of("User-Agent", secProperties.userAgent())),
                publicPing("RSS", "https://news.google.com/rss/search?q=AAPL%20stock&hl=en-US&gl=US&ceid=US:en"),
                publicPing("FRED", "https://fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10"),
                publicPing("Stooq", "https://stooq.com/q/l/?s=aapl.us&f=sd2t2ohlcv&h&e=csv"),
                publicPing("KIND", "https://kind.krx.co.kr/corpgeneral/corpList.do?method=download&marketType=stockMkt"),
                sentimentSidecarPing()
        );
    }

    private ComponentHealthResponse sentimentSidecarPing() {
        if (!sentimentAnalysisProperties.enabled()) {
            return new ComponentHealthResponse("Ping Sentiment Sidecar", "MISSING_CONFIG", "감성 분석 사이드카가 비활성화되어 있습니다.");
        }
        if (sentimentAnalysisProperties.baseUrl() == null || sentimentAnalysisProperties.baseUrl().isBlank() || MarketUtil.isDevPlaceholder(sentimentAnalysisProperties.baseUrl())) {
            return new ComponentHealthResponse("Ping Sentiment Sidecar", "MISSING_CONFIG", "Sentiment Sidecar 설정이 없어 외부 API ping을 건너뜁니다.");
        }
        return ping("Sentiment Sidecar", normalizeBaseUrl(sentimentAnalysisProperties.baseUrl()) + "/health", Map.of(), true);
    }

    private ComponentHealthResponse configuredPing(String name, String configValue, String url, Map<String, String> headers) {
        if (configValue == null || configValue.isBlank() || MarketUtil.isDevPlaceholder(configValue) || url == null || url.isBlank()) {
            return new ComponentHealthResponse("Ping " + name, "MISSING_CONFIG", name + " 설정이 없어 외부 API ping을 건너뜁니다.");
        }
        return ping(name, url, headers);
    }

    private ComponentHealthResponse publicPing(String name, String url) {
        return ping(name, url, Map.of());
    }

    private ComponentHealthResponse ping(String name, String url, Map<String, String> headers) {
        return ping(name, url, headers, false);
    }

    private ComponentHealthResponse ping(String name, String url, Map<String, String> headers, boolean require2xx) {
        ExternalApiPingClient.PingResult result = externalApiPingClient.ping(name, url, headers);
        boolean ready = result.reachable() && (!require2xx || is2xx(result.statusCode()));
        if (ready) {
            return new ComponentHealthResponse("Ping " + name, "READY", "외부 API ping 성공. status=" + result.statusCode() + ", elapsedMs=" + result.elapsedMillis());
        }
        return new ComponentHealthResponse("Ping " + name, "UNREACHABLE", "외부 API ping 실패. status=" + Objects.toString(result.statusCode(), "N/A") + ", elapsedMs=" + result.elapsedMillis() + ", error=" + Objects.toString(result.errorMessage(), "N/A"));
    }

    private boolean is2xx(Integer statusCode) {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private ComponentHealthResponse priceDailyData() {
        return priceDailyRepository.findAllByOrderByTradeDateDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateData(
                        "Data Price Daily",
                        row.getTradeDate(),
                        resolveIntSetting("ops.health.priceDaily.maxAgeDays", 3),
                        "%s %s latestTradeDate=%s source=%s".formatted(row.getMarket(), row.getTicker(), row.getTradeDate(), row.getSource())
                ))
                .orElseGet(() -> noData("Data Price Daily"));
    }

    private ComponentHealthResponse priceIntradayData() {
        return priceIntradayRepository.findAllByOrderByTickAtDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateTimeData(
                        "Data Price Intraday",
                        row.getTickAt(),
                        resolveIntSetting("ops.health.priceIntraday.maxAgeMinutes", 120),
                        ChronoUnit.MINUTES,
                        "%s %s latestTickAt=%s source=%s".formatted(row.getMarket(), row.getTicker(), row.getTickAt(), row.getSource())
                ))
                .orElseGet(() -> noData("Data Price Intraday"));
    }

    private ComponentHealthResponse newsData() {
        return newsArticleRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateTimeData(
                        "Data News",
                        row.getPublishedAt(),
                        resolveIntSetting("ops.health.news.maxAgeHours", 48),
                        ChronoUnit.HOURS,
                        "%s %s latestPublishedAt=%s source=%s".formatted(
                                Objects.toString(row.getMarket(), "MARKET"),
                                Objects.toString(row.getTicker(), "MARKET"),
                                row.getPublishedAt(),
                                row.getSource()
                        )
                ))
                .orElseGet(() -> noData("Data News"));
    }

    private ComponentHealthResponse disclosureData() {
        return disclosureEventRepository.findAllByOrderByDisclosedAtDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateTimeData(
                        "Data Disclosure",
                        row.getDisclosedAt(),
                        resolveIntSetting("ops.health.disclosure.maxAgeDays", 14),
                        ChronoUnit.DAYS,
                        "%s %s latestDisclosedAt=%s source=%s type=%s".formatted(
                                Objects.toString(row.getMarket(), "MARKET"),
                                Objects.toString(row.getTicker(), "MARKET"),
                                row.getDisclosedAt(),
                                row.getSource(),
                                Objects.toString(row.getDisclosureType(), "N/A")
                        )
                ))
                .orElseGet(() -> noData("Data Disclosure"));
    }

    private ComponentHealthResponse macroData() {
        return macroObservationRepository.findAllByOrderByObservedDateDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateData(
                        "Data Macro",
                        row.getObservedDate(),
                        resolveIntSetting("ops.health.macro.maxAgeDays", 14),
                        "%s latestObservedDate=%s fetchedAt=%s source=%s".formatted(row.getSeriesId(), row.getObservedDate(), row.getFetchedAt(), row.getSource())
                ))
                .orElseGet(() -> noData("Data Macro"));
    }

    private ComponentHealthResponse fundamentalData() {
        return fundamentalMetricRepository.findAllByOrderByPeriodEndDesc(PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(row -> dateData(
                        "Data Fundamental",
                        row.getPeriodEnd(),
                        resolveIntSetting("ops.health.fundamental.maxAgeDays", 120),
                        "%s %s metric=%s periodEnd=%s fetchedAt=%s source=%s".formatted(
                                row.getMarket(),
                                row.getTicker(),
                                row.getMetricName(),
                                row.getPeriodEnd(),
                                row.getFetchedAt(),
                                row.getSource()
                        )
                ))
                .orElseGet(() -> noData("Data Fundamental"));
    }

    private ComponentHealthResponse dateData(String name, LocalDate latestDate, int maxAgeDays, String message) {
        if (latestDate == null) {
            return staleData(name, message + " age=unknown");
        }
        long ageDays = ChronoUnit.DAYS.between(latestDate, LocalDate.now(APP_ZONE));
        if (ageDays > maxAgeDays) {
            return staleData(name, message + " ageDays=" + ageDays + ", maxAgeDays=" + maxAgeDays);
        }
        return readyData(name, message + " ageDays=" + Math.max(ageDays, 0));
    }

    private ComponentHealthResponse dateTimeData(String name, LocalDateTime latestAt, int maxAge, ChronoUnit unit, String message) {
        if (latestAt == null) {
            return staleData(name, message + " age=unknown");
        }
        long age = unit.between(latestAt, LocalDateTime.now(APP_ZONE));
        if (age > maxAge) {
            return staleData(name, message + " age" + unitLabel(unit) + "=" + age + ", maxAge" + unitLabel(unit) + "=" + maxAge);
        }
        return readyData(name, message + " age" + unitLabel(unit) + "=" + Math.max(age, 0));
    }

    private ComponentHealthResponse readyData(String name, String message) {
        return new ComponentHealthResponse(name, "READY", message);
    }

    private ComponentHealthResponse staleData(String name, String message) {
        return new ComponentHealthResponse(name, "STALE", message);
    }

    private ComponentHealthResponse noData(String name) {
        return new ComponentHealthResponse(name, "NO_DATA", "저장된 수집 데이터가 없습니다.");
    }

    private String unitLabel(ChronoUnit unit) {
        return switch (unit) {
            case MINUTES -> "Minutes";
            case HOURS -> "Hours";
            case DAYS -> "Days";
            default -> "Units";
        };
    }

    private ComponentHealthResponse codexDailyUsage() {
        int limit = resolveIntSetting("codex.daily.callLimit", 200);
        LocalDate today = LocalDate.now(APP_ZONE);
        long calls = codexCallRepository.countByCalledAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        if (limit < 0) {
            return new ComponentHealthResponse("Codex Daily Limit", "DISABLED", "Codex 일 호출 한도 제한이 비활성화되어 있습니다. calls=" + calls);
        }
        if (limit >= 0 && calls >= limit) {
            return new ComponentHealthResponse("Codex Daily Limit", "LIMIT_REACHED", "오늘 Codex 호출 수가 한도에 도달했습니다. calls=" + calls + ", limit=" + limit);
        }
        return new ComponentHealthResponse("Codex Daily Limit", "READY", "오늘 Codex 호출 수 " + calls + "/" + limit);
    }

    private ComponentHealthResponse codexDailyBudget() {
        double budgetUsd = resolveDoubleSetting("codex.daily.budgetUsd", 0.0);
        if (budgetUsd <= 0) {
            return new ComponentHealthResponse("Codex Daily Budget", "DISABLED", "Codex 일 예산 제한이 비활성화되어 있습니다.");
        }

        double estimatedUsdPer1kChars = resolveDoubleSetting("codex.estimatedUsdPer1kChars", 0.002);
        LocalDate today = LocalDate.now(APP_ZONE);
        long textLength = codexCallRepository.sumSucceededTextLengthByCalledAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        double usedUsd = estimateBudgetUsd(textLength, estimatedUsdPer1kChars);
        if (usedUsd >= budgetUsd) {
            return new ComponentHealthResponse("Codex Daily Budget", "LIMIT_REACHED", "오늘 Codex 예상 비용이 예산에 도달했습니다. usedUsd=" + String.format("%.4f", usedUsd) + ", budgetUsd=" + budgetUsd);
        }
        return new ComponentHealthResponse("Codex Daily Budget", "READY", "오늘 Codex 예상 비용 $" + String.format("%.4f", usedUsd) + " / $" + budgetUsd);
    }

    private double estimateBudgetUsd(long textLength, double usdPer1kChars) {
        if (usdPer1kChars <= 0) {
            return 0.0;
        }
        return (textLength / 1000.0) * usdPer1kChars;
    }

    private int resolveIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractIntValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private int extractIntValue(String valueJson, int defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asInt(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private double resolveDoubleSetting(String key, double defaultValue) {
        return appSettingRepository.findById(key)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> extractDoubleValue(valueJson, defaultValue))
                .orElse(defaultValue);
    }

    private double extractDoubleValue(String valueJson, double defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asDouble(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }
} // 외부 연동 상태 서비스를 종료한다.
