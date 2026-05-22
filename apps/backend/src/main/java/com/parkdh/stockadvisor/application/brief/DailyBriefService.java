package com.parkdh.stockadvisor.application.brief; // 데일리 브리프 서비스 패키지를 선언한다.

import com.fasterxml.jackson.databind.JsonNode; // JSON 노드 타입을 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파서를 가져온다.
import com.parkdh.stockadvisor.api.brief.dto.DailyBriefCreateRequest; // 데일리 브리프 생성 요청 DTO를 가져온다.
import com.parkdh.stockadvisor.api.brief.dto.DailyBriefResponse; // 데일리 브리프 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.stats.dto.StatsSummaryResponse; // 통계 요약 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.application.stats.StatsService; // 통계 서비스를 가져온다.
import com.parkdh.stockadvisor.config.CodexCliProperties; // Codex CLI 설정을 가져온다.
import com.parkdh.stockadvisor.domain.brief.DailyBriefEntity; // 데일리 브리프 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity; // 공시 이벤트 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity; // 펀더멘털 지표 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity; // 매크로 관측값 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity; // 뉴스 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity; // 추천 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 앱 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.global.util.MarketUtil; // 시장 공통 유틸을 가져온다.
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient; // Codex CLI 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.codex.CodexClient.CodexResult; // Codex 호출 결과를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.brief.DailyBriefRepository; // 데일리 브리프 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository; // 공시 이벤트 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository; // 펀더멘털 지표 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository; // 매크로 관측값 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository; // 뉴스 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 앱 설정 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository; // 시장 유니버스 저장소를 가져온다.
import jakarta.transaction.Transactional; // 쓰기 트랜잭션 어노테이션을 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이지 요청 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Objects; // 객체 유틸을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class DailyBriefService { // 데일리 브리프 서비스를 정의한다.
    private static final String DEFAULT_PROFILE = "stock-advisor"; // 기본 Codex profile을 정의한다.

    private final DailyBriefRepository dailyBriefRepository; // 데일리 브리프 저장소 의존성을 보관한다.
    private final RecommendationRepository recommendationRepository; // 추천 저장소 의존성을 보관한다.
    private final MarketUniverseRepository marketUniverseRepository; // 시장 유니버스 저장소 의존성을 보관한다.
    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소 의존성을 보관한다.
    private final NewsArticleRepository newsArticleRepository; // 뉴스 저장소 의존성을 보관한다.
    private final DisclosureEventRepository disclosureEventRepository; // 공시 이벤트 저장소 의존성을 보관한다.
    private final MacroObservationRepository macroObservationRepository; // 매크로 관측값 저장소 의존성을 보관한다.
    private final FundamentalMetricRepository fundamentalMetricRepository; // 펀더멘털 지표 저장소 의존성을 보관한다.
    private final AppSettingRepository appSettingRepository; // 앱 설정 저장소 의존성을 보관한다.
    private final StatsService statsService; // 통계 서비스 의존성을 보관한다.
    private final CodexClient codexClient; // Codex CLI 클라이언트 의존성을 보관한다.
    private final CodexCliProperties codexCliProperties; // Codex CLI 설정 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 파서 의존성을 보관한다.

    public List<DailyBriefResponse> getDailyBriefs(String marketTrack) { // 데일리 브리프 목록을 조회한다.
        List<DailyBriefEntity> entities = marketTrack == null || marketTrack.isBlank() ? dailyBriefRepository.findAll() : dailyBriefRepository.findByMarketTrack(marketTrack); // 시장 트랙 조건 여부에 따라 브리프를 조회한다.
        return entities.stream().sorted(Comparator.comparing(DailyBriefEntity::getGeneratedAt).reversed()).map(this::toResponse).toList(); // 생성 일시 역순으로 정렬해 DTO로 변환한다.
    } // 데일리 브리프 목록 조회를 종료한다.

    public DailyBriefResponse getDailyBrief(Long id) { // 데일리 브리프 단건을 조회한다.
        DailyBriefEntity entity = dailyBriefRepository.findById(id).orElseThrow(() -> new CustomException("데일리 브리프를 찾을 수 없습니다.", 404)); // 브리프가 없으면 404 예외를 던진다.
        return toResponse(entity); // 데일리 브리프 DTO를 반환한다.
    } // 데일리 브리프 단건 조회를 종료한다.

    @Transactional // 데일리 브리프 생성을 쓰기 트랜잭션으로 처리한다.
    public DailyBriefResponse createDailyBrief(DailyBriefCreateRequest request) { // 데일리 브리프를 생성한다.
        LocalDateTime generatedAt = request.generatedAt() == null ? LocalDateTime.now() : request.generatedAt(); // 생성 일시가 없으면 현재 시각을 사용한다.
        DailyBriefEntity entity = new DailyBriefEntity(request.marketTrack(), request.briefMd(), request.draftNo(), request.coverage(), request.hallucinationFlags(), request.llmModel(), generatedAt); // 새 데일리 브리프 엔티티를 생성한다.
        DailyBriefEntity saved = dailyBriefRepository.save(entity); // 새 데일리 브리프를 저장한다.
        return toResponse(saved); // 저장된 데일리 브리프를 반환한다.
    } // 데일리 브리프 생성을 종료한다.

    @Transactional // Codex 브리프 생성을 쓰기 트랜잭션으로 처리한다.
    public DailyBriefResponse generateDailyBrief(String marketTrack, String additionalPrompt) { // 수집 데이터 기반 데일리 브리프를 생성한다.
        String safeMarketTrack = marketTrack == null || marketTrack.isBlank() ? "KRX" : marketTrack; // 시장 트랙 기본값을 정한다.
        BriefContext context = buildContext(safeMarketTrack); // 브리프 컨텍스트를 구성한다.
        String profile = resolveProfile(); // Codex profile을 결정한다.
        String prompt = buildPrompt(context, additionalPrompt); // 실제 수집 데이터를 포함한 프롬프트를 만든다.
        CodexResult result = codexClient.call(prompt, profile, "daily-brief"); // Codex CLI를 호출하고 감사 로그를 남긴다.
        boolean useLocalBrief = MarketUtil.isDevPlaceholder(codexCliProperties.command()) || !result.succeeded(); // 로컬 브리프 사용 여부를 정한다.
        String briefMd = useLocalBrief ? buildLocalBrief(context, result.errorMessage()) : result.response(); // 저장할 브리프 본문을 결정한다.
        DailyBriefEntity entity = dailyBriefRepository.save(new DailyBriefEntity(
                safeMarketTrack,
                briefMd,
                1,
                calculateCoverage(context),
                useLocalBrief ? 1 : 0,
                profile,
                LocalDateTime.now())); // 생성된 브리프를 저장한다.
        return toResponse(entity); // 저장된 브리프를 반환한다.
    } // 수집 데이터 기반 데일리 브리프 생성을 종료한다.

    private BriefContext buildContext(String marketTrack) { // 브리프 입력 컨텍스트를 구성한다.
        List<String> markets = resolveMarkets(marketTrack); // 시장 트랙에 해당하는 시장 목록을 구한다.
        List<RecommendationEntity> recommendations = recommendationRepository.findByStatus("OPEN").stream()
                .filter(recommendation -> markets.contains(recommendation.getMarket())) // 해당 시장 추천만 남긴다.
                .sorted(Comparator.comparing(RecommendationEntity::getGeneratedAt).reversed()) // 생성 일시 역순으로 정렬한다.
                .limit(5) // 최근 추천 5건만 사용한다.
                .toList(); // 추천 목록으로 수집한다.
        List<MarketUniverseEntity> universe = markets.stream()
                .flatMap(market -> marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(market, true).stream()) // 시장별 거래 가능 후보군을 조회한다.
                .sorted(Comparator.comparing(MarketUniverseEntity::getAvgTurnover, Comparator.nullsLast(Comparator.reverseOrder()))) // 평균 거래대금 역순으로 정렬한다.
                .limit(8) // 상위 후보 8건만 사용한다.
                .toList(); // 후보군 목록으로 수집한다.
        List<PriceDailyEntity> prices = markets.stream()
                .flatMap(market -> priceDailyRepository.findByMarketOrderByTradeDateDesc(market, PageRequest.of(0, 20)).stream()) // 시장별 최근 일봉을 조회한다.
                .sorted(Comparator.comparing(PriceDailyEntity::getTradeDate).reversed()) // 거래일 역순으로 정렬한다.
                .limit(20) // 최근 일봉 20건만 사용한다.
                .toList(); // 일봉 목록으로 수집한다.
        List<NewsArticleEntity> news = markets.stream()
                .flatMap(market -> newsArticleRepository.findByMarketOrderByPublishedAtDesc(market, PageRequest.of(0, 10)).stream()) // 시장별 최근 뉴스를 조회한다.
                .sorted(Comparator.comparing(NewsArticleEntity::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))) // 발행 일시 역순으로 정렬한다.
                .limit(10) // 최근 뉴스 10건만 사용한다.
                .toList(); // 뉴스 목록으로 수집한다.
        List<DisclosureEventEntity> disclosures = markets.stream()
                .flatMap(market -> disclosureEventRepository.findByMarketOrderByDisclosedAtDesc(market, PageRequest.of(0, 10)).stream()) // 시장별 최근 공시를 조회한다.
                .sorted(Comparator.comparing(DisclosureEventEntity::getDisclosedAt, Comparator.nullsLast(Comparator.reverseOrder()))) // 공시 일시 역순으로 정렬한다.
                .limit(10) // 최근 공시 10건만 사용한다.
                .toList(); // 공시 목록으로 수집한다.
        List<MacroObservationEntity> macroObservations = macroObservationRepository.findAllByOrderByObservedDateDesc(PageRequest.of(0, 20)); // 최근 매크로 관측값을 조회한다.
        List<FundamentalMetricEntity> fundamentals = markets.stream()
                .flatMap(market -> fundamentalMetricRepository.findByMarketOrderByPeriodEndDesc(market, PageRequest.of(0, 20)).stream()) // 시장별 최근 펀더멘털을 조회한다.
                .sorted(Comparator.comparing(FundamentalMetricEntity::getPeriodEnd, Comparator.nullsLast(Comparator.reverseOrder()))) // 기간 종료일 역순으로 정렬한다.
                .limit(20) // 최근 펀더멘털 20건만 사용한다.
                .toList(); // 펀더멘털 목록으로 수집한다.
        StatsSummaryResponse stats = statsService.getSummary(); // 성과 통계 요약을 조회한다.
        return new BriefContext(marketTrack, markets, recommendations, universe, prices, news, disclosures, macroObservations, fundamentals, stats); // 컨텍스트를 반환한다.
    } // 브리프 입력 컨텍스트 구성을 종료한다.

    private String buildPrompt(BriefContext context, String additionalPrompt) { // Codex 입력 프롬프트를 구성한다.
        StringBuilder builder = new StringBuilder(); // 프롬프트 문자열 빌더를 만든다.
        builder.append("아래 실제 수집 데이터를 기반으로 ").append(context.marketTrack()).append(" 데일리 브리프를 작성해줘.\n");
        builder.append("형식: 마크다운, 700자 이내, 섹션은 시장 요약/추천 점검/리스크/오늘 할 일.\n");
        builder.append("주의: 제공된 데이터에 없는 사실은 추정하지 말고 '데이터 없음'이라고 적어.\n\n");
        builder.append("성과 요약: ").append(formatStats(context.stats())).append("\n\n");
        builder.append("OPEN 추천:\n").append(formatRecommendations(context.recommendations())).append("\n");
        builder.append("시장 후보군 상위:\n").append(formatUniverse(context.universe())).append("\n");
        builder.append("최근 일봉 샘플:\n").append(formatPrices(context.prices())).append("\n");
        builder.append("최근 뉴스:\n").append(formatNews(context.news())).append("\n");
        builder.append("최근 공시:\n").append(formatDisclosures(context.disclosures())).append("\n");
        builder.append("매크로 관측값:\n").append(formatMacro(context.macroObservations())).append("\n");
        builder.append("펀더멘털 지표:\n").append(formatFundamentals(context.fundamentals())).append("\n");
        if (additionalPrompt != null && !additionalPrompt.isBlank()) { // 추가 요청이 있는지 확인한다.
            builder.append("\n사용자 추가 요청:\n").append(additionalPrompt).append("\n"); // 추가 요청을 프롬프트에 포함한다.
        } // 추가 요청 확인을 종료한다.
        return builder.toString(); // 프롬프트를 반환한다.
    } // Codex 입력 프롬프트 구성을 종료한다.

    private String buildLocalBrief(BriefContext context, String errorMessage) { // 로컬 fallback 브리프를 만든다.
        return """
                # %s 데일리 브리프

                ## 시장 요약
                - 추적 시장: %s
                - 거래 가능 후보 샘플: %d건
                - 최근 일봉 샘플: %d건
                - 최근 뉴스: %d건, 공시: %d건, 매크로 관측값: %d건, 펀더멘털 지표: %d건

                ## 추천 점검
                %s

                ## 성과 요약
                - 전체 추천 %d건, OPEN %d건, CLOSED %d건, EXPIRED %d건
                - Hit Rate %.1f%%, 평균 PnL %s

                ## 리스크
                - Codex CLI가 미설정이거나 실패해 로컬 템플릿으로 생성했습니다.%s
                - 제공된 수집 데이터 범위 밖의 뉴스/공시/매크로 이벤트는 포함하지 않았습니다.
                """.formatted(
                context.marketTrack(),
                String.join(", ", context.markets()),
                context.universe().size(),
                context.prices().size(),
                context.news().size(),
                context.disclosures().size(),
                context.macroObservations().size(),
                context.fundamentals().size(),
                formatRecommendationBullets(context.recommendations()),
                context.stats().total(),
                context.stats().open(),
                context.stats().closed(),
                context.stats().expired(),
                context.stats().hitRate(),
                context.stats().avgPnlPct(),
                errorMessage == null ? "" : " 오류: " + errorMessage
        ); // 로컬 브리프 본문을 반환한다.
    } // 로컬 fallback 브리프 생성을 종료한다.

    private List<String> resolveMarkets(String marketTrack) { // 시장 트랙을 실제 시장 목록으로 변환한다.
        return switch (marketTrack) {
            case "US", "US_CLOSE" -> List.of("NASDAQ", "NYSE"); // 미국 시장 트랙을 반환한다.
            case "KOSPI" -> List.of("KOSPI"); // KOSPI 단일 시장을 반환한다.
            case "KOSDAQ" -> List.of("KOSDAQ"); // KOSDAQ 단일 시장을 반환한다.
            case "NASDAQ" -> List.of("NASDAQ"); // NASDAQ 단일 시장을 반환한다.
            case "NYSE" -> List.of("NYSE"); // NYSE 단일 시장을 반환한다.
            default -> List.of("KOSPI", "KOSDAQ"); // 기본 KRX 시장을 반환한다.
        }; // 시장 목록 변환을 종료한다.
    } // 시장 트랙 변환을 종료한다.

    private String resolveProfile() { // Codex profile 설정값을 조회한다.
        return appSettingRepository.findById("codex.profile")
                .map(AppSettingEntity::getValueJson)
                .map(this::extractStringValue)
                .orElse(DEFAULT_PROFILE); // 설정이 없으면 기본 profile을 반환한다.
    } // Codex profile 설정값 조회를 종료한다.

    private String extractStringValue(String valueJson) { // 설정 JSON에서 value 문자열을 추출한다.
        try { // JSON 파싱 예외를 처리한다.
            JsonNode node = objectMapper.readTree(valueJson); // 설정 JSON을 파싱한다.
            String value = node.path("value").asText(DEFAULT_PROFILE); // value 필드를 읽는다.
            return value.isBlank() ? DEFAULT_PROFILE : value; // 빈 값이면 기본 profile을 반환한다.
        } catch (Exception exception) { // 파싱 예외를 잡는다.
            return DEFAULT_PROFILE; // 파싱 실패 시 기본 profile을 반환한다.
        } // 예외 처리를 종료한다.
    } // 설정 JSON value 추출을 종료한다.

    private BigDecimal calculateCoverage(BriefContext context) { // 브리프 필수 데이터 커버리지 점수를 계산한다.
        int covered = 0; // 확보된 데이터 영역 수를 초기화한다.
        covered += context.recommendations().isEmpty() ? 0 : 1; // 추천 데이터 확보 여부를 반영한다.
        covered += context.universe().isEmpty() ? 0 : 1; // 후보군 데이터 확보 여부를 반영한다.
        covered += context.prices().isEmpty() ? 0 : 1; // 일봉 데이터 확보 여부를 반영한다.
        covered += context.news().isEmpty() ? 0 : 1; // 뉴스 데이터 확보 여부를 반영한다.
        covered += context.disclosures().isEmpty() ? 0 : 1; // 공시 데이터 확보 여부를 반영한다.
        covered += context.macroObservations().isEmpty() ? 0 : 1; // 매크로 데이터 확보 여부를 반영한다.
        covered += context.fundamentals().isEmpty() ? 0 : 1; // 펀더멘털 데이터 확보 여부를 반영한다.
        covered += context.stats() == null ? 0 : 1; // 통계 데이터 확보 여부를 반영한다.
        return BigDecimal.valueOf(covered).divide(BigDecimal.valueOf(8), 3, java.math.RoundingMode.HALF_UP); // 0~1 사이 커버리지 점수를 반환한다.
    } // 브리프 필수 데이터 커버리지 점수 계산을 종료한다.

    private String formatStats(StatsSummaryResponse stats) { // 통계 요약을 문자열로 변환한다.
        return "total=%d, open=%d, closed=%d, expired=%d, hitRate=%.1f, avgPnl=%s".formatted(stats.total(), stats.open(), stats.closed(), stats.expired(), stats.hitRate(), stats.avgPnlPct()); // 통계 요약 문자열을 반환한다.
    } // 통계 요약 문자열 변환을 종료한다.

    private String formatRecommendations(List<RecommendationEntity> recommendations) { // 추천 목록을 프롬프트 문자열로 변환한다.
        if (recommendations.isEmpty()) { // 추천이 없는지 확인한다.
            return "- OPEN 추천 없음\n"; // 추천 없음 문자열을 반환한다.
        } // 추천 없음 확인을 종료한다.
        return recommendations.stream()
                .map(recommendation -> "- %s %s %s entry=%s target=%s stop=%s confidence=%d".formatted(recommendation.getMarket(), recommendation.getTicker(), recommendation.getTerm(), recommendation.getEntryPrice(), recommendation.getTargetPrice(), recommendation.getStopPrice(), recommendation.getConfidence())) // 추천 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 추천 목록 프롬프트 문자열 변환을 종료한다.

    private String formatRecommendationBullets(List<RecommendationEntity> recommendations) { // 로컬 브리프용 추천 목록을 만든다.
        if (recommendations.isEmpty()) { // 추천이 없는지 확인한다.
            return "- OPEN 추천이 없습니다."; // 추천 없음 문구를 반환한다.
        } // 추천 없음 확인을 종료한다.
        return recommendations.stream()
                .map(recommendation -> "- %s %s: 진입 %s, 목표 %s, 손절 %s".formatted(recommendation.getMarket(), recommendation.getTicker(), recommendation.getEntryPrice(), recommendation.getTargetPrice(), recommendation.getStopPrice())) // 추천 bullet을 만든다.
                .reduce("", (left, right) -> left + right + "\n")
                .trim(); // 추천 bullet 문자열을 반환한다.
    } // 로컬 브리프용 추천 목록 생성을 종료한다.

    private String formatUniverse(List<MarketUniverseEntity> universe) { // 후보군 목록을 프롬프트 문자열로 변환한다.
        if (universe.isEmpty()) { // 후보군이 없는지 확인한다.
            return "- 후보군 없음\n"; // 후보군 없음 문자열을 반환한다.
        } // 후보군 없음 확인을 종료한다.
        return universe.stream()
                .map(entity -> "- %s %s %s lastPrice=%s avgTurnover=%s sector=%s".formatted(entity.getMarket(), entity.getTicker(), entity.getName(), formatDecimal(entity.getLastPrice()), formatDecimal(entity.getAvgTurnover()), Objects.toString(entity.getSector(), "UNKNOWN"))) // 후보군 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 후보군 목록 프롬프트 문자열 변환을 종료한다.

    private String formatPrices(List<PriceDailyEntity> prices) { // 일봉 목록을 프롬프트 문자열로 변환한다.
        if (prices.isEmpty()) { // 일봉이 없는지 확인한다.
            return "- 일봉 데이터 없음\n"; // 일봉 없음 문자열을 반환한다.
        } // 일봉 없음 확인을 종료한다.
        return prices.stream()
                .map(price -> "- %s %s %s close=%s volume=%s source=%s".formatted(price.getMarket(), price.getTicker(), price.getTradeDate(), price.getClosePrice(), price.getVolume(), price.getSource())) // 일봉 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 일봉 목록 프롬프트 문자열 변환을 종료한다.

    private String formatNews(List<NewsArticleEntity> news) { // 뉴스 목록을 프롬프트 문자열로 변환한다.
        if (news.isEmpty()) { // 뉴스가 없는지 확인한다.
            return "- 뉴스 데이터 없음\n"; // 뉴스 없음 문자열을 반환한다.
        } // 뉴스 없음 확인을 종료한다.
        return news.stream()
                .map(article -> "- %s %s %s title=%s source=%s url=%s".formatted(Objects.toString(article.getPublishedAt(), "N/A"), Objects.toString(article.getMarket(), "N/A"), Objects.toString(article.getTicker(), "MARKET"), article.getTitle(), article.getSource(), article.getUrl())) // 뉴스 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 뉴스 목록 프롬프트 문자열 변환을 종료한다.

    private String formatDisclosures(List<DisclosureEventEntity> disclosures) { // 공시 목록을 프롬프트 문자열로 변환한다.
        if (disclosures.isEmpty()) { // 공시가 없는지 확인한다.
            return "- 공시 데이터 없음\n"; // 공시 없음 문자열을 반환한다.
        } // 공시 없음 확인을 종료한다.
        return disclosures.stream()
                .map(disclosure -> "- %s %s %s title=%s type=%s source=%s url=%s".formatted(Objects.toString(disclosure.getDisclosedAt(), "N/A"), Objects.toString(disclosure.getMarket(), "N/A"), Objects.toString(disclosure.getTicker(), "MARKET"), disclosure.getTitle(), Objects.toString(disclosure.getDisclosureType(), "N/A"), disclosure.getSource(), Objects.toString(disclosure.getUrl(), "N/A"))) // 공시 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 공시 목록 프롬프트 문자열 변환을 종료한다.

    private String formatMacro(List<MacroObservationEntity> macroObservations) { // 매크로 관측값 목록을 프롬프트 문자열로 변환한다.
        if (macroObservations.isEmpty()) { // 매크로 데이터가 없는지 확인한다.
            return "- 매크로 데이터 없음\n"; // 매크로 없음 문자열을 반환한다.
        } // 매크로 데이터 없음 확인을 종료한다.
        return macroObservations.stream()
                .map(observation -> "- %s %s %s=%s source=%s".formatted(observation.getObservedDate(), observation.getSeriesId(), observation.getSeriesName(), Objects.toString(observation.getObservedValue(), "N/A"), observation.getSource())) // 매크로 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 매크로 관측값 목록 프롬프트 문자열 변환을 종료한다.

    private String formatFundamentals(List<FundamentalMetricEntity> fundamentals) { // 펀더멘털 지표 목록을 프롬프트 문자열로 변환한다.
        if (fundamentals.isEmpty()) { // 펀더멘털 데이터가 없는지 확인한다.
            return "- 펀더멘털 데이터 없음\n"; // 펀더멘털 없음 문자열을 반환한다.
        } // 펀더멘털 데이터 없음 확인을 종료한다.
        return fundamentals.stream()
                .map(metric -> "- %s %s %s %s=%s %s period=%s/%s".formatted(metric.getPeriodEnd(), metric.getMarket(), metric.getTicker(), metric.getMetricName(), Objects.toString(metric.getMetricValue(), "N/A"), Objects.toString(metric.getUnit(), ""), Objects.toString(metric.getFiscalYear(), "N/A"), Objects.toString(metric.getFiscalPeriod(), "N/A"))) // 펀더멘털 행을 만든다.
                .reduce("", (left, right) -> left + right + "\n"); // 줄 단위 문자열로 결합한다.
    } // 펀더멘털 지표 목록 프롬프트 문자열 변환을 종료한다.

    private String formatDecimal(BigDecimal value) { // 숫자를 표시 문자열로 변환한다.
        return value == null ? "N/A" : value.toPlainString(); // null이면 N/A를 반환한다.
    } // 숫자 표시 문자열 변환을 종료한다.

    private DailyBriefResponse toResponse(DailyBriefEntity entity) { // 데일리 브리프 엔티티를 응답 DTO로 변환한다.
        return new DailyBriefResponse(entity.getId(), entity.getMarketTrack(), entity.getBriefMd(), entity.getDraftNo(), entity.getCoverage(), entity.getHallucinationFlags(), entity.getLlmModel(), entity.getGeneratedAt()); // 데일리 브리프 응답 DTO를 생성한다.
    } // 데일리 브리프 DTO 변환을 종료한다.

    private record BriefContext(String marketTrack, List<String> markets, List<RecommendationEntity> recommendations, List<MarketUniverseEntity> universe, List<PriceDailyEntity> prices, List<NewsArticleEntity> news, List<DisclosureEventEntity> disclosures, List<MacroObservationEntity> macroObservations, List<FundamentalMetricEntity> fundamentals, StatsSummaryResponse stats) { // 브리프 생성을 위한 내부 컨텍스트를 정의한다.
    } // 브리프 내부 컨텍스트를 종료한다.
} // 데일리 브리프 서비스를 종료한다.
