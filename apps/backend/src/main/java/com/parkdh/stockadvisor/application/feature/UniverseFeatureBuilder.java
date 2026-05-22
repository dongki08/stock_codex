package com.parkdh.stockadvisor.application.feature; // 후보군 feature 애플리케이션 패키지를 선언한다.

import com.fasterxml.jackson.core.JsonProcessingException; // JSON 직렬화 예외를 가져온다.
import com.fasterxml.jackson.databind.JsonNode; // JSON 노드를 가져온다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 직렬화 도구를 가져온다.
import com.parkdh.stockadvisor.api.feature.dto.UniverseFeatureResponse; // 후보군 feature 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.DisclosureEventEntity; // 공시 이벤트 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.FundamentalMetricEntity; // 펀더멘털 지표 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.MacroObservationEntity; // 매크로 관측값 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.marketdata.NewsArticleEntity; // 뉴스 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity; // 일봉 가격 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.setting.AppSettingEntity; // 앱 설정 엔티티를 가져온다.
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity; // 시장 유니버스 엔티티를 가져온다.
import com.parkdh.stockadvisor.global.exception.CustomException; // 커스텀 예외를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.DisclosureEventRepository; // 공시 이벤트 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.FundamentalMetricRepository; // 펀더멘털 지표 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.MacroObservationRepository; // 매크로 관측값 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.marketdata.NewsArticleRepository; // 뉴스 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository; // 앱 설정 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository; // 시장 유니버스 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이징 요청 도구를 가져온다.
import org.springframework.stereotype.Service; // 서비스 어노테이션을 가져온다.

import java.math.BigDecimal; // 정밀 숫자 타입을 가져온다.
import java.math.RoundingMode; // 반올림 모드를 가져온다.
import java.util.Comparator; // 정렬 비교 도구를 가져온다.
import java.util.LinkedHashMap; // 순서 보존 Map 타입을 가져온다.
import java.util.List; // 목록 타입을 가져온다.
import java.util.Map; // 맵 타입을 가져온다.

@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Service // 스프링 서비스 빈으로 등록한다.
public class UniverseFeatureBuilder { // 후보군 feature 빌더를 정의한다.
    private static final int HISTORY_LOOKBACK = 60; // feature 계산에 사용할 최대 일봉 수를 정의한다.
    private static final String SCORING_WEIGHTS_SETTING_KEY = "recommendation.scoring.weights"; // feature 점수 가중치 설정 키를 정의한다.
    private final MarketUniverseRepository marketUniverseRepository; // 시장 유니버스 저장소 의존성을 보관한다.
    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소 의존성을 보관한다.
    private final NewsArticleRepository newsArticleRepository; // 뉴스 저장소 의존성을 보관한다.
    private final DisclosureEventRepository disclosureEventRepository; // 공시 이벤트 저장소 의존성을 보관한다.
    private final MacroObservationRepository macroObservationRepository; // 매크로 관측값 저장소 의존성을 보관한다.
    private final FundamentalMetricRepository fundamentalMetricRepository; // 펀더멘털 지표 저장소 의존성을 보관한다.
    private final AppSettingRepository appSettingRepository; // 앱 설정 저장소 의존성을 보관한다.
    private final ObjectMapper objectMapper; // JSON 직렬화 도구 의존성을 보관한다.

    public List<UniverseFeatureResponse> getFeatures(String market, Integer limit) { // 후보군 feature 목록을 조회한다.
        int safeLimit = normalizeLimit(limit); // 조회 개수를 정규화한다.
        return buildFeatures(market).stream()
                .limit(safeLimit) // 요청 개수만 남긴다.
                .map(this::toResponse) // 응답 DTO로 변환한다.
                .toList(); // 목록으로 수집한다.
    } // 후보군 feature 목록 조회를 종료한다.

    public List<UniverseFeature> buildFeatures(String market) { // 후보군 feature 목록을 생성한다.
        List<MarketUniverseEntity> entities = findUniverse(market); // 시장 조건에 맞는 후보군을 조회한다.
        return entities.stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getTradable())) // 거래 가능한 후보만 남긴다.
                .map(this::buildFeature) // feature로 변환한다.
                .sorted(Comparator.comparing(UniverseFeature::totalScore).reversed().thenComparing(UniverseFeature::ticker)) // 종합 점수 내림차순으로 정렬한다.
                .toList(); // 목록으로 수집한다.
    } // 후보군 feature 목록 생성을 종료한다.

    private List<MarketUniverseEntity> findUniverse(String market) { // 시장 조건으로 후보군을 조회한다.
        if (market == null || market.isBlank() || "ALL".equals(market)) { // 전체 시장 요청인지 확인한다.
            return marketUniverseRepository.findByTradableAndDelistedAtIsNull(true); // 전체 거래 가능 후보군을 반환한다.
        } // 전체 시장 확인을 종료한다.
        return marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(market, true); // 특정 시장 거래 가능 후보군을 반환한다.
    } // 후보군 조회를 종료한다.

    private UniverseFeature buildFeature(MarketUniverseEntity entity) { // 단일 후보군 feature를 생성한다.
        ScoringWeights weights = resolveScoringWeights(); // 설정 기반 점수 가중치를 조회한다.
        List<PriceDailyEntity> priceHistory = getPriceHistory(entity); // 최근 일봉 히스토리를 조회한다.
        TechnicalSnapshot technical = buildTechnicalSnapshot(priceHistory, weights.technical()); // 일봉 기반 기술적 feature를 계산한다.
        int liquidityScore = scoreLiquidity(entity.getAvgTurnover()); // 유동성 점수를 계산한다.
        int priceScore = scorePrice(entity.getLastPrice(), entity.getMarket()); // 가격 점수를 계산한다.
        int dataQualityScore = scoreDataQuality(entity, technical.priceHistoryCount()); // 데이터 품질 점수를 계산한다.
        ContextSnapshot context = buildContextSnapshot(entity, weights.context()); // 뉴스/공시/매크로 컨텍스트 feature를 계산한다.
        int totalScore = weightedScore(
                List.of(liquidityScore, priceScore, technical.technicalScore(), context.contextScore(), context.fundamentalScore(), dataQualityScore),
                List.of(weights.liquidity(), weights.price(), weights.technicalWeight(), weights.contextWeight(), weights.fundamental(), weights.dataQuality())
        ); // 가중 종합 점수를 계산한다.
        String featureJson = buildFeatureJson(entity, liquidityScore, priceScore, technical, context, dataQualityScore, totalScore); // feature JSON 문자열을 만든다.
        return new UniverseFeature(entity, liquidityScore, priceScore, technical.movingAverageScore(), technical.rsiScore(), technical.volumeScore(), technical.technicalScore(), dataQualityScore, technical.priceHistoryCount(), totalScore, featureJson); // feature 값을 반환한다.
    } // 단일 후보군 feature 생성을 종료한다.

    private ContextSnapshot buildContextSnapshot(MarketUniverseEntity entity, ContextWeights weights) { // 뉴스/공시/매크로 컨텍스트 feature를 계산한다.
        List<NewsArticleEntity> news = newsArticleRepository.findByMarketAndTickerOrderByPublishedAtDesc(entity.getMarket(), entity.getTicker(), PageRequest.of(0, 10)); // 최근 뉴스 목록을 조회한다.
        List<DisclosureEventEntity> disclosures = disclosureEventRepository.findByMarketAndTickerOrderByDisclosedAtDesc(entity.getMarket(), entity.getTicker(), PageRequest.of(0, 10)); // 최근 공시 목록을 조회한다.
        List<MacroObservationEntity> macroObservations = macroObservationRepository.findAllByOrderByObservedDateDesc(PageRequest.of(0, 20)); // 최근 매크로 관측값을 조회한다.
        List<FundamentalMetricEntity> fundamentals = fundamentalMetricRepository.findByMarketAndTickerOrderByPeriodEndDesc(entity.getMarket(), entity.getTicker(), PageRequest.of(0, 20)); // 최근 펀더멘털 지표를 조회한다.
        int newsScore = scoreNews(news); // 뉴스 점수를 계산한다.
        int disclosureScore = scoreDisclosures(disclosures); // 공시 점수를 계산한다.
        int macroScore = scoreMacro(macroObservations); // 매크로 점수를 계산한다.
        int fundamentalScore = scoreFundamentals(fundamentals); // 펀더멘털 데이터 점수를 계산한다.
        int contextScore = weightedScore(
                List.of(newsScore, disclosureScore, macroScore, fundamentalScore),
                List.of(weights.news(), weights.disclosure(), weights.macro(), weights.fundamental())
        ); // 컨텍스트 종합 점수를 계산한다.
        return new ContextSnapshot(newsScore, disclosureScore, macroScore, fundamentalScore, contextScore, news.size(), disclosures.size(), macroObservations.size(), fundamentals.size()); // 컨텍스트 스냅샷을 반환한다.
    } // 뉴스/공시/매크로 컨텍스트 feature 계산을 종료한다.

    private int scoreNews(List<NewsArticleEntity> news) { // 최근 뉴스 빈도와 감성 점수를 계산한다.
        int newsCount = news.size(); // 뉴스 개수를 계산한다.
        if (newsCount >= 5) { // 뉴스가 충분한지 확인한다.
            return applySentimentAdjustment(88, averageSentiment(news)); // 충분한 뉴스 점수를 반환한다.
        } // 충분한 뉴스 확인을 종료한다.
        if (newsCount >= 2) { // 최소 뉴스가 있는지 확인한다.
            return applySentimentAdjustment(72, averageSentiment(news)); // 보통 뉴스 점수를 반환한다.
        } // 최소 뉴스 확인을 종료한다.
        if (newsCount == 1) { // 뉴스가 1건인지 확인한다.
            return applySentimentAdjustment(58, averageSentiment(news)); // 낮은 뉴스 점수를 반환한다.
        } // 1건 뉴스 확인을 종료한다.
        return 45; // 뉴스 없음 점수를 반환한다.
    } // 최근 뉴스 빈도와 감성 점수 계산을 종료한다.

    private int scoreDisclosures(List<DisclosureEventEntity> disclosures) { // 최근 공시 중요도 점수를 계산한다.
        int disclosureCount = disclosures.size(); // 공시 개수를 계산한다.
        int maxImportance = disclosures.stream()
                .map(DisclosureEventEntity::getImportanceScore)
                .filter(score -> score != null)
                .max(Integer::compareTo)
                .orElse(0); // 공시 최대 중요도를 계산한다.
        if (maxImportance >= 85) { // 고중요 공시가 있는지 확인한다.
            return 82; // 고중요 이벤트 점수를 반환한다.
        } // 고중요 공시 확인을 종료한다.
        if (disclosureCount >= 3) { // 공시가 많은지 확인한다.
            return 72; // 활성 이벤트 점수를 반환한다.
        } // 공시 다수 확인을 종료한다.
        if (disclosureCount >= 1) { // 공시가 있는지 확인한다.
            return 64; // 보통 이벤트 점수를 반환한다.
        } // 공시 확인을 종료한다.
        return 55; // 공시 없음 중립 점수를 반환한다.
    } // 최근 공시 중요도 점수 계산을 종료한다.

    private BigDecimal averageSentiment(List<NewsArticleEntity> news) { // 뉴스 평균 감성 점수를 계산한다.
        List<BigDecimal> scores = news.stream()
                .map(NewsArticleEntity::getSentimentScore)
                .filter(score -> score != null)
                .toList(); // 감성 점수 목록을 만든다.
        if (scores.isEmpty()) { // 감성 점수가 없는지 확인한다.
            return BigDecimal.ZERO; // 중립 감성을 반환한다.
        } // 감성 점수 없음 확인을 종료한다.
        return average(scores); // 평균 감성을 반환한다.
    } // 뉴스 평균 감성 점수 계산을 종료한다.

    private int applySentimentAdjustment(int baseScore, BigDecimal averageSentiment) { // 평균 감성을 점수에 반영한다.
        int adjustment = averageSentiment.multiply(BigDecimal.valueOf(15)).setScale(0, RoundingMode.HALF_UP).intValue(); // -15~+15 범위 보정값을 계산한다.
        return Math.max(25, Math.min(100, baseScore + adjustment)); // 점수 범위를 제한해 반환한다.
    } // 평균 감성 점수 반영을 종료한다.

    private int scoreMacro(List<MacroObservationEntity> macroObservations) { // 매크로 데이터 점수를 계산한다.
        long seriesCount = macroObservations.stream().map(MacroObservationEntity::getSeriesId).distinct().count(); // 확보된 지표 수를 계산한다.
        if (seriesCount >= 5) { // 주요 지표가 모두 있는지 확인한다.
            return 82; // 높은 매크로 데이터 점수를 반환한다.
        } // 주요 지표 확인을 종료한다.
        if (seriesCount >= 3) { // 일부 주요 지표가 있는지 확인한다.
            return 70; // 보통 매크로 데이터 점수를 반환한다.
        } // 일부 지표 확인을 종료한다.
        if (seriesCount >= 1) { // 최소 지표가 있는지 확인한다.
            return 58; // 낮은 매크로 데이터 점수를 반환한다.
        } // 최소 지표 확인을 종료한다.
        return 45; // 매크로 데이터 없음 점수를 반환한다.
    } // 매크로 데이터 점수 계산을 종료한다.

    private int scoreFundamentals(List<FundamentalMetricEntity> fundamentals) { // 펀더멘털 데이터 점수를 계산한다.
        long metricCount = fundamentals.stream().map(FundamentalMetricEntity::getMetricName).distinct().count(); // 확보된 펀더멘털 지표 수를 계산한다.
        if (metricCount >= 6) { // 주요 펀더멘털이 충분한지 확인한다.
            return 84; // 높은 펀더멘털 데이터 점수를 반환한다.
        } // 충분한 펀더멘털 확인을 종료한다.
        if (metricCount >= 3) { // 일부 펀더멘털이 있는지 확인한다.
            return 70; // 보통 펀더멘털 데이터 점수를 반환한다.
        } // 일부 펀더멘털 확인을 종료한다.
        if (metricCount >= 1) { // 최소 펀더멘털이 있는지 확인한다.
            return 58; // 낮은 펀더멘털 데이터 점수를 반환한다.
        } // 최소 펀더멘털 확인을 종료한다.
        return 45; // 펀더멘털 데이터 없음 점수를 반환한다.
    } // 펀더멘털 데이터 점수 계산을 종료한다.

    private int scoreLiquidity(BigDecimal avgTurnover) { // 평균 거래대금으로 유동성 점수를 계산한다.
        if (avgTurnover == null) { // 거래대금이 없는지 확인한다.
            return 35; // 데이터 부족 기본 점수를 반환한다.
        } // 거래대금 확인을 종료한다.
        if (avgTurnover.compareTo(BigDecimal.valueOf(5_000_000_000L)) >= 0) { // 거래대금이 매우 큰지 확인한다.
            return 95; // 높은 유동성 점수를 반환한다.
        } // 매우 큰 거래대금 확인을 종료한다.
        if (avgTurnover.compareTo(BigDecimal.valueOf(1_000_000_000L)) >= 0) { // 거래대금이 충분한지 확인한다.
            return 85; // 충분한 유동성 점수를 반환한다.
        } // 충분한 거래대금 확인을 종료한다.
        if (avgTurnover.compareTo(BigDecimal.valueOf(100_000_000L)) >= 0) { // 거래대금이 보통인지 확인한다.
            return 70; // 보통 유동성 점수를 반환한다.
        } // 보통 거래대금 확인을 종료한다.
        return 50; // 낮은 유동성 점수를 반환한다.
    } // 유동성 점수 계산을 종료한다.

    private int scorePrice(BigDecimal lastPrice, String market) { // 최근 가격으로 가격 점수를 계산한다.
        if (lastPrice == null) { // 가격이 없는지 확인한다.
            return 40; // 데이터 부족 기본 점수를 반환한다.
        } // 가격 확인을 종료한다.
        BigDecimal low = "KOSPI".equals(market) || "KOSDAQ".equals(market) ? BigDecimal.valueOf(5000) : BigDecimal.valueOf(5); // 시장별 저가 기준을 정한다.
        BigDecimal high = "KOSPI".equals(market) || "KOSDAQ".equals(market) ? BigDecimal.valueOf(500000) : BigDecimal.valueOf(1000); // 시장별 고가 기준을 정한다.
        if (lastPrice.compareTo(low) < 0) { // 가격이 너무 낮은지 확인한다.
            return 45; // 낮은 가격 점수를 반환한다.
        } // 저가 확인을 종료한다.
        if (lastPrice.compareTo(high) > 0) { // 가격이 너무 높은지 확인한다.
            return 70; // 높은 가격 점수를 반환한다.
        } // 고가 확인을 종료한다.
        return 85; // 일반 가격대 점수를 반환한다.
    } // 가격 점수 계산을 종료한다.

    private int scoreDataQuality(MarketUniverseEntity entity, int priceHistoryCount) { // 데이터 품질 점수를 계산한다.
        int score = 40; // 기본 점수를 설정한다.
        if (entity.getLastPrice() != null) { // 최근 가격이 있는지 확인한다.
            score += 15; // 가격 데이터 점수를 더한다.
        } // 최근 가격 확인을 종료한다.
        if (entity.getAvgTurnover() != null) { // 거래대금이 있는지 확인한다.
            score += 15; // 거래대금 데이터 점수를 더한다.
        } // 거래대금 확인을 종료한다.
        if (priceHistoryCount >= 20) { // 충분한 일봉 히스토리가 있는지 확인한다.
            score += 20; // 충분한 히스토리 점수를 더한다.
        } else if (priceHistoryCount >= 5) { // 최소 일봉 히스토리가 있는지 확인한다.
            score += 10; // 최소 히스토리 점수를 더한다.
        } // 가격 히스토리 확인을 종료한다.
        if (entity.getLastSyncedAt() != null) { // 동기화일이 있는지 확인한다.
            score += 10; // 동기화 데이터 점수를 더한다.
        } // 동기화일 확인을 종료한다.
        return Math.min(100, score); // 최대 100점으로 제한해 반환한다.
    } // 데이터 품질 점수 계산을 종료한다.

    private List<PriceDailyEntity> getPriceHistory(MarketUniverseEntity entity) { // 후보군의 최근 일봉 히스토리를 조회한다.
        return priceDailyRepository.findByMarketAndTickerOrderByTradeDateDesc(entity.getMarket(), entity.getTicker(), PageRequest.of(0, HISTORY_LOOKBACK)).stream()
                .sorted(Comparator.comparing(PriceDailyEntity::getTradeDate)) // 기술적 지표 계산을 위해 오래된 거래일부터 정렬한다.
                .toList(); // 목록으로 수집한다.
    } // 최근 일봉 히스토리 조회를 종료한다.

    private TechnicalSnapshot buildTechnicalSnapshot(List<PriceDailyEntity> priceHistory, TechnicalWeights weights) { // 일봉 히스토리로 기술적 feature를 계산한다.
        int priceHistoryCount = priceHistory.size(); // 가격 히스토리 개수를 구한다.
        int movingAverageScore = scoreMovingAverage(priceHistory); // 이동평균 점수를 계산한다.
        int rsiScore = scoreRsi(priceHistory); // RSI 점수를 계산한다.
        int volumeScore = scoreVolume(priceHistory); // 거래량 점수를 계산한다.
        int technicalScore = weightedScore(
                List.of(movingAverageScore, rsiScore, volumeScore),
                List.of(weights.ma(), weights.rsi(), weights.volume())
        ); // 기술적 종합 점수를 계산한다.
        return new TechnicalSnapshot(movingAverageScore, rsiScore, volumeScore, technicalScore, priceHistoryCount); // 기술적 feature 스냅샷을 반환한다.
    } // 기술적 feature 계산을 종료한다.

    private int scoreMovingAverage(List<PriceDailyEntity> priceHistory) { // 이동평균 추세 점수를 계산한다.
        if (priceHistory.size() < 20) { // 20일 이동평균 계산이 가능한지 확인한다.
            return priceHistory.size() >= 5 ? 55 : 40; // 데이터 부족 점수를 반환한다.
        } // 일봉 개수 확인을 종료한다.
        BigDecimal close = latestClose(priceHistory); // 최근 종가를 가져온다.
        BigDecimal ma5 = averageClose(priceHistory, 5); // 5일 이동평균을 계산한다.
        BigDecimal ma20 = averageClose(priceHistory, 20); // 20일 이동평균을 계산한다.
        if (close.compareTo(ma5) >= 0 && ma5.compareTo(ma20) >= 0) { // 단기 상승 정렬인지 확인한다.
            return 92; // 강한 상승 추세 점수를 반환한다.
        } // 상승 정렬 확인을 종료한다.
        if (close.compareTo(ma20) >= 0) { // 종가가 20일선 위인지 확인한다.
            return 78; // 양호한 추세 점수를 반환한다.
        } // 20일선 확인을 종료한다.
        if (close.compareTo(ma20.multiply(BigDecimal.valueOf(0.95))) < 0) { // 20일선에서 크게 이탈했는지 확인한다.
            return 45; // 약한 추세 점수를 반환한다.
        } // 하락 이탈 확인을 종료한다.
        return 62; // 중립 추세 점수를 반환한다.
    } // 이동평균 추세 점수 계산을 종료한다.

    private int scoreRsi(List<PriceDailyEntity> priceHistory) { // RSI 점수를 계산한다.
        if (priceHistory.size() < 15) { // 14일 RSI 계산이 가능한지 확인한다.
            return 45; // 데이터 부족 점수를 반환한다.
        } // 일봉 개수 확인을 종료한다.
        List<BigDecimal> closes = priceHistory.stream().map(PriceDailyEntity::getClosePrice).toList(); // 종가 목록을 만든다.
        int start = closes.size() - 15; // 최근 14개 변화량 시작 인덱스를 계산한다.
        BigDecimal gains = BigDecimal.ZERO; // 상승폭 합계를 초기화한다.
        BigDecimal losses = BigDecimal.ZERO; // 하락폭 합계를 초기화한다.
        for (int i = start + 1; i < closes.size(); i++) { // 최근 14개 변화량을 순회한다.
            BigDecimal diff = closes.get(i).subtract(closes.get(i - 1)); // 전일 대비 차이를 계산한다.
            if (diff.compareTo(BigDecimal.ZERO) > 0) { // 상승일인지 확인한다.
                gains = gains.add(diff); // 상승폭을 더한다.
            } else { // 하락일이면 처리한다.
                losses = losses.add(diff.abs()); // 하락폭 절댓값을 더한다.
            } // 상승/하락 확인을 종료한다.
        } // 변화량 순회를 종료한다.
        BigDecimal rsi = calculateRsi(gains, losses); // RSI 값을 계산한다.
        if (rsi.compareTo(BigDecimal.valueOf(45)) >= 0 && rsi.compareTo(BigDecimal.valueOf(65)) <= 0) { // 안정적 상승 구간인지 확인한다.
            return 90; // 우수한 RSI 점수를 반환한다.
        } // 우수 구간 확인을 종료한다.
        if (rsi.compareTo(BigDecimal.valueOf(35)) >= 0 && rsi.compareTo(BigDecimal.valueOf(75)) <= 0) { // 일반 허용 구간인지 확인한다.
            return 75; // 양호한 RSI 점수를 반환한다.
        } // 일반 구간 확인을 종료한다.
        if (rsi.compareTo(BigDecimal.valueOf(25)) >= 0 && rsi.compareTo(BigDecimal.valueOf(85)) <= 0) { // 과열/침체 경계 구간인지 확인한다.
            return 55; // 낮은 RSI 점수를 반환한다.
        } // 경계 구간 확인을 종료한다.
        return 35; // 극단 구간 점수를 반환한다.
    } // RSI 점수 계산을 종료한다.

    private int scoreVolume(List<PriceDailyEntity> priceHistory) { // 거래량 점수를 계산한다.
        if (priceHistory.size() < 20) { // 20일 거래량 계산이 가능한지 확인한다.
            return priceHistory.size() >= 5 ? 55 : 40; // 데이터 부족 점수를 반환한다.
        } // 일봉 개수 확인을 종료한다.
        List<BigDecimal> recent = priceHistory.subList(priceHistory.size() - 20, priceHistory.size()).stream().map(PriceDailyEntity::getVolume).toList(); // 최근 20일 거래량을 가져온다.
        BigDecimal latest = recent.get(recent.size() - 1); // 최신 거래량을 가져온다.
        BigDecimal average = average(recent); // 평균 거래량을 계산한다.
        BigDecimal stdDev = standardDeviation(recent, average); // 거래량 표준편차를 계산한다.
        if (stdDev.compareTo(BigDecimal.ZERO) == 0) { // 표준편차가 없는지 확인한다.
            return 60; // 중립 점수를 반환한다.
        } // 표준편차 확인을 종료한다.
        BigDecimal zScore = latest.subtract(average).divide(stdDev, 4, RoundingMode.HALF_UP); // 거래량 z-score를 계산한다.
        if (zScore.compareTo(BigDecimal.valueOf(1.5)) >= 0) { // 거래량이 강하게 증가했는지 확인한다.
            return 92; // 강한 거래량 점수를 반환한다.
        } // 강한 증가 확인을 종료한다.
        if (zScore.compareTo(BigDecimal.ZERO) >= 0) { // 평균 이상 거래량인지 확인한다.
            return 76; // 양호한 거래량 점수를 반환한다.
        } // 평균 이상 확인을 종료한다.
        if (zScore.compareTo(BigDecimal.valueOf(-1.0)) < 0) { // 거래량이 크게 위축됐는지 확인한다.
            return 45; // 낮은 거래량 점수를 반환한다.
        } // 위축 확인을 종료한다.
        return 60; // 중립 거래량 점수를 반환한다.
    } // 거래량 점수 계산을 종료한다.

    private BigDecimal latestClose(List<PriceDailyEntity> priceHistory) { // 최근 종가를 반환한다.
        return priceHistory.get(priceHistory.size() - 1).getClosePrice(); // 마지막 일봉의 종가를 반환한다.
    } // 최근 종가 반환을 종료한다.

    private BigDecimal averageClose(List<PriceDailyEntity> priceHistory, int days) { // 최근 N일 종가 평균을 계산한다.
        List<BigDecimal> closes = priceHistory.subList(priceHistory.size() - days, priceHistory.size()).stream().map(PriceDailyEntity::getClosePrice).toList(); // 최근 N일 종가를 가져온다.
        return average(closes); // 평균을 반환한다.
    } // 최근 N일 종가 평균 계산을 종료한다.

    private BigDecimal average(List<BigDecimal> values) { // 숫자 목록의 평균을 계산한다.
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP); // 평균을 반환한다.
    } // 평균 계산을 종료한다.

    private BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal average) { // 표준편차를 계산한다.
        double variance = values.stream().map(value -> value.subtract(average).doubleValue()).mapToDouble(diff -> diff * diff).average().orElse(0.0); // 분산을 계산한다.
        return BigDecimal.valueOf(Math.sqrt(variance)); // 표준편차를 반환한다.
    } // 표준편차 계산을 종료한다.

    private BigDecimal calculateRsi(BigDecimal gains, BigDecimal losses) { // 상승폭과 하락폭으로 RSI 값을 계산한다.
        if (losses.compareTo(BigDecimal.ZERO) == 0) { // 하락폭이 없는지 확인한다.
            return BigDecimal.valueOf(100); // RSI 100을 반환한다.
        } // 하락폭 확인을 종료한다.
        if (gains.compareTo(BigDecimal.ZERO) == 0) { // 상승폭이 없는지 확인한다.
            return BigDecimal.ZERO; // RSI 0을 반환한다.
        } // 상승폭 확인을 종료한다.
        BigDecimal rs = gains.divide(losses, 8, RoundingMode.HALF_UP); // 상대강도를 계산한다.
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP)); // RSI를 반환한다.
    } // RSI 값 계산을 종료한다.

    private int weightedScore(List<Integer> scores, List<Float> weights) { // 가중 평균 점수를 계산한다.
        float totalWeight = 0f; // 총 가중치를 초기화한다.
        float weightedSum = 0f; // 가중합을 초기화한다.
        for (int i = 0; i < scores.size(); i++) { // 각 점수와 가중치를 순회한다.
            float weight = Math.max(0f, weights.get(i)); // 음수 가중치는 무시한다.
            totalWeight += weight; // 총 가중치를 누적한다.
            weightedSum += scores.get(i) * weight; // 가중합을 누적한다.
        } // 점수 순회를 종료한다.
        if (totalWeight <= 0f) { // 유효 가중치가 없는지 확인한다.
            return Math.min(100, (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0))); // 단순 평균으로 fallback한다.
        } // 유효 가중치 확인을 종료한다.
        return Math.min(100, Math.round(weightedSum / totalWeight)); // 100점 상한을 적용해 반환한다.
    } // 가중 평균 점수 계산을 종료한다.

    private ScoringWeights resolveScoringWeights() { // 설정에서 feature 점수 가중치를 조회한다.
        ScoringWeights defaults = ScoringWeights.defaults(); // 기본 가중치를 준비한다.
        return appSettingRepository.findById(SCORING_WEIGHTS_SETTING_KEY)
                .map(AppSettingEntity::getValueJson)
                .map(valueJson -> parseScoringWeights(valueJson, defaults))
                .orElse(defaults); // 설정이 없으면 기존 상수와 같은 기본값을 사용한다.
    } // 점수 가중치 조회를 종료한다.

    private ScoringWeights parseScoringWeights(String valueJson, ScoringWeights defaults) { // JSON 설정을 점수 가중치로 변환한다.
        try { // JSON 파싱 실패를 처리한다.
            JsonNode root = objectMapper.readTree(valueJson); // 설정 JSON을 읽는다.
            JsonNode value = root.path("value"); // 종합 점수 가중치 노드를 가져온다.
            JsonNode technical = root.path("technical"); // 기술 점수 가중치 노드를 가져온다.
            JsonNode context = root.path("context"); // 컨텍스트 점수 가중치 노드를 가져온다.
            return new ScoringWeights(
                    readWeight(value, "liquidity", defaults.liquidity()),
                    readWeight(value, "price", defaults.price()),
                    readWeight(value, "technical", defaults.technicalWeight()),
                    readWeight(value, "context", defaults.contextWeight()),
                    readWeight(value, "fundamental", defaults.fundamental()),
                    readWeight(value, "dataQuality", defaults.dataQuality()),
                    new TechnicalWeights(
                            readWeight(technical, "ma", defaults.technical().ma()),
                            readWeight(technical, "rsi", defaults.technical().rsi()),
                            readWeight(technical, "volume", defaults.technical().volume())
                    ),
                    new ContextWeights(
                            readWeight(context, "news", defaults.context().news()),
                            readWeight(context, "disclosure", defaults.context().disclosure()),
                            readWeight(context, "macro", defaults.context().macro()),
                            readWeight(context, "fundamental", defaults.context().fundamental())
                    )
            ); // 설정 기반 가중치를 반환한다.
        } catch (Exception exception) { // 설정 JSON이 깨졌으면 처리한다.
            return defaults; // 기존 동작으로 fallback한다.
        } // JSON 파싱 예외 처리를 종료한다.
    } // 점수 가중치 파싱을 종료한다.

    private float readWeight(JsonNode node, String fieldName, float defaultValue) { // 단일 가중치를 읽는다.
        JsonNode value = node.path(fieldName); // 필드 노드를 조회한다.
        if (!value.isNumber()) { // 숫자 값인지 확인한다.
            return defaultValue; // 숫자가 아니면 기본값을 사용한다.
        } // 숫자 확인을 종료한다.
        float weight = (float) value.asDouble(defaultValue); // float 가중치로 변환한다.
        return Float.isFinite(weight) ? weight : defaultValue; // 유효 숫자면 반환하고 아니면 기본값을 사용한다.
    } // 단일 가중치 읽기를 종료한다.

    private String buildFeatureJson(MarketUniverseEntity entity, int liquidityScore, int priceScore, TechnicalSnapshot technical, ContextSnapshot context, int dataQualityScore, int totalScore) { // feature JSON 문자열을 만든다.
        try { // JSON 직렬화 예외를 처리한다.
            Map<String, Object> featureMap = new LinkedHashMap<>(); // feature 값을 순서 있는 Map으로 만든다.
            featureMap.put("ticker", entity.getTicker()); // 종목 코드를 추가한다.
            featureMap.put("market", entity.getMarket()); // 시장 구분을 추가한다.
            featureMap.put("liquidityScore", liquidityScore); // 유동성 점수를 추가한다.
            featureMap.put("priceScore", priceScore); // 가격 점수를 추가한다.
            featureMap.put("movingAverageScore", technical.movingAverageScore()); // 이동평균 점수를 추가한다.
            featureMap.put("rsiScore", technical.rsiScore()); // RSI 점수를 추가한다.
            featureMap.put("volumeScore", technical.volumeScore()); // 거래량 점수를 추가한다.
            featureMap.put("technicalScore", technical.technicalScore()); // 기술적 종합 점수를 추가한다.
            featureMap.put("newsScore", context.newsScore()); // 뉴스 점수를 추가한다.
            featureMap.put("disclosureScore", context.disclosureScore()); // 공시 점수를 추가한다.
            featureMap.put("macroScore", context.macroScore()); // 매크로 점수를 추가한다.
            featureMap.put("fundamentalScore", context.fundamentalScore()); // 펀더멘털 점수를 추가한다.
            featureMap.put("contextScore", context.contextScore()); // 컨텍스트 종합 점수를 추가한다.
            featureMap.put("newsCount", context.newsCount()); // 뉴스 개수를 추가한다.
            featureMap.put("disclosureCount", context.disclosureCount()); // 공시 개수를 추가한다.
            featureMap.put("macroObservationCount", context.macroObservationCount()); // 매크로 관측값 개수를 추가한다.
            featureMap.put("fundamentalMetricCount", context.fundamentalMetricCount()); // 펀더멘털 지표 개수를 추가한다.
            featureMap.put("dataQualityScore", dataQualityScore); // 데이터 품질 점수를 추가한다.
            featureMap.put("priceHistoryCount", technical.priceHistoryCount()); // 가격 히스토리 개수를 추가한다.
            featureMap.put("totalScore", totalScore); // 종합 점수를 추가한다.
            featureMap.put("source", "feature-rule-v2"); // feature 버전을 추가한다.
            return objectMapper.writeValueAsString(featureMap); // feature JSON 직렬화를 종료한다.
        } catch (JsonProcessingException e) { // JSON 직렬화 실패를 처리한다.
            throw new CustomException("feature JSON 직렬화에 실패했습니다: " + e.getMessage(), 500); // 직렬화 실패 예외를 던진다.
        } // 예외 처리를 종료한다.
    } // feature JSON 문자열 생성을 종료한다.

    private int normalizeLimit(Integer limit) { // 조회 개수를 정규화한다.
        int value = limit == null ? 50 : limit; // 값이 없으면 기본값을 사용한다.
        if (value < 1 || value > 500) { // 허용 범위를 벗어났는지 확인한다.
            throw new CustomException("feature 조회 limit은 1~500 사이여야 합니다.", 400); // 범위 예외를 던진다.
        } // 허용 범위 확인을 종료한다.
        return value; // 정규화된 값을 반환한다.
    } // 조회 개수 정규화를 종료한다.

    private UniverseFeatureResponse toResponse(UniverseFeature feature) { // feature를 응답 DTO로 변환한다.
        return new UniverseFeatureResponse(feature.entity().getUniverseKey(), feature.ticker(), feature.market(), feature.entity().getName(), feature.lastPrice(), feature.entity().getAvgTurnover(), feature.liquidityScore(), feature.priceScore(), feature.movingAverageScore(), feature.rsiScore(), feature.volumeScore(), feature.technicalScore(), feature.dataQualityScore(), feature.priceHistoryCount(), feature.totalScore(), feature.featureJson()); // feature 응답 DTO를 생성한다.
    } // feature 응답 DTO 변환을 종료한다.

    private record TechnicalSnapshot(Integer movingAverageScore, Integer rsiScore, Integer volumeScore, Integer technicalScore, Integer priceHistoryCount) { // 기술적 feature 계산 결과를 정의한다.
    } // 기술적 feature 계산 결과를 종료한다.

    private record ContextSnapshot(Integer newsScore, Integer disclosureScore, Integer macroScore, Integer fundamentalScore, Integer contextScore, Integer newsCount, Integer disclosureCount, Integer macroObservationCount, Integer fundamentalMetricCount) { // 뉴스/공시/매크로/펀더멘털 feature 계산 결과를 정의한다.
    } // 뉴스/공시/매크로/펀더멘털 feature 계산 결과를 종료한다.

    private record ScoringWeights(float liquidity, float price, float technicalWeight, float contextWeight, float fundamental, float dataQuality, TechnicalWeights technical, ContextWeights context) { // feature 종합 점수 가중치를 정의한다.
        private static ScoringWeights defaults() { // 기존 상수와 동일한 기본 가중치를 반환한다.
            return new ScoringWeights(0.20f, 0.10f, 0.30f, 0.15f, 0.10f, 0.15f, new TechnicalWeights(0.40f, 0.35f, 0.25f), new ContextWeights(0.40f, 0.18f, 0.25f, 0.17f)); // 기본 가중치를 생성한다.
        } // 기본 가중치 생성을 종료한다.
    } // feature 종합 점수 가중치 정의를 종료한다.

    private record TechnicalWeights(float ma, float rsi, float volume) { // 기술 점수 내부 가중치를 정의한다.
    } // 기술 점수 내부 가중치 정의를 종료한다.

    private record ContextWeights(float news, float disclosure, float macro, float fundamental) { // 컨텍스트 점수 내부 가중치를 정의한다.
    } // 컨텍스트 점수 내부 가중치 정의를 종료한다.
} // 후보군 feature 빌더를 종료한다.
