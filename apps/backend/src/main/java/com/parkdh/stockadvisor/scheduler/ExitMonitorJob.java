package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient.KisCurrentPrice;
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.us.StooqQuoteClient.StooqQuote;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExitMonitorJob {
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final RecommendationRepository recommendationRepository;
    private final EvaluationRepository evaluationRepository;
    private final KisApiClient kisApiClient;
    private final StooqQuoteClient stooqQuoteClient;
    private final MarketDataSyncService marketDataSyncService;
    private final NotificationService notificationService;
    private final AppSettingRepository appSettingRepository;
    private final PriceIntradayRepository priceIntradayRepository;
    private final ObjectMapper objectMapper;

    // 한국장: 평일 09~15시 (KST)
    @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void runKr() {
        runForRegion("KR");
    }

    // 미국장 저녁: 평일 22~23시 (KST) — DST 22:30 / 표준시 23:30 개장 포함
    @Scheduled(cron = "0 * 22-23 * * MON-FRI", zone = "Asia/Seoul")
    public void runUsEvening() {
        runForRegion("US");
    }

    // 미국장 심야~새벽: 화~토 00~05시 (KST) — DST 05:00 / 표준시 06:00 폐장 커버
    @Scheduled(cron = "0 * 0-5 * * TUE-SAT", zone = "Asia/Seoul")
    public void runUsNight() {
        runForRegion("US");
    }

    private void runForRegion(String region) {
        int pollingIntervalMinutes = getIntSetting("exit.polling.intervalMinutes", 5);
        if (!shouldRunAtConfiguredInterval(pollingIntervalMinutes)) {
            return;
        }
        if (!getBooleanSetting("exit.intraday.enabled", true)) {
            log.info("ExitMonitorJob 비활성화. region={}", region);
            return;
        }

        log.info("ExitMonitorJob 시작. region={}", region);
        try {
            List<RecommendationEntity> openRecommendations = recommendationRepository.findByStatus("OPEN");
            int processed = 0;
            for (RecommendationEntity recommendation : openRecommendations) {
                boolean isKr = isKoreanMarket(recommendation.getMarket());
                if ("KR".equals(region) && !isKr) continue;
                if ("US".equals(region) && isKr) continue;
                checkExitCondition(recommendation);
                processed++;
            }
            log.info("ExitMonitorJob 완료. region={}, 처리={}건", region, processed);
        } catch (Exception exception) {
            log.error("ExitMonitorJob 오류. region={}, error={}", region, exception.getMessage(), exception);
            notificationService.sendTelegramOnce(
                    "exit-monitor:error:" + region + ":" + LocalDate.now(SEOUL_ZONE).format(DATE_KEY_FORMATTER),
                    "❌ ExitMonitorJob 오류 [" + region + "]: " + exception.getMessage()
            );
        }
    }

    private void checkExitCondition(RecommendationEntity recommendation) {
        String ticker = recommendation.getTicker();
        String market = recommendation.getMarket();

        BigDecimal currentPrice;
        BigDecimal volume;
        String priceSource;

        if (isKoreanMarket(market)) {
            Optional<KisCurrentPrice> priceOpt = kisApiClient.fetchCurrentPrice(ticker);
            if (priceOpt.isEmpty()) {
                log.debug("KIS 현재가 없음. ticker={}", ticker);
                return;
            }
            currentPrice = priceOpt.get().currentPrice();
            volume = priceOpt.get().volume();
            priceSource = "KIS";
        } else {
            Optional<StooqQuote> quoteOpt = stooqQuoteClient.fetchQuote(ticker);
            if (quoteOpt.isEmpty()) {
                log.debug("Stooq 현재가 없음. ticker={}", ticker);
                return;
            }
            currentPrice = quoteOpt.get().close();
            volume = quoteOpt.get().volume();
            priceSource = "STOOQ";
        }

        marketDataSyncService.saveIntradayPrice(ticker, market, LocalDateTime.now(SEOUL_ZONE), currentPrice, volume, priceSource);

        BigDecimal targetPrice = recommendation.getTargetPrice();
        BigDecimal stopPrice = recommendation.getStopPrice();
        boolean targetReached = targetPrice.compareTo(currentPrice) <= 0;
        boolean stopBreached = stopPrice.compareTo(currentPrice) >= 0;

        if (targetReached) {
            String message = "🎯 목표가 도달: " + ticker + " [" + market + "]\n현재가=" + currentPrice + ", 목표가=" + targetPrice;
            notificationService.sendTelegramOnce(buildEventKey(recommendation, "TARGET", currentPrice), message);
            log.info("목표가 도달. ticker={}, market={}, 현재가={}, 목표가={}", ticker, market, currentPrice, targetPrice);
        }
        if (stopBreached) {
            String message = "🛑 손절가 이탈: " + ticker + " [" + market + "]\n현재가=" + currentPrice + ", 손절가=" + stopPrice;
            notificationService.sendTelegramOnce(buildEventKey(recommendation, "STOP", currentPrice), message);
            log.info("손절가 이탈. ticker={}, market={}, 현재가={}, 손절가={}", ticker, market, currentPrice, stopPrice);
        }
        if (targetReached || stopBreached) {
            closeRecommendationWithEvaluation(recommendation, currentPrice, targetReached ? "TARGET_HIT" : "STOP_HIT", targetReached, "CLOSED");
            return;
        }
        if (LocalDate.now(SEOUL_ZONE).isAfter(recommendation.getExpectedExitAt())) {
            closeRecommendationWithEvaluation(recommendation, currentPrice, "TIME_OUT", false, "EXPIRED");
        }
    }

    private void closeRecommendationWithEvaluation(RecommendationEntity recommendation, BigDecimal exitPrice, String exitReason, boolean hitTarget, String status) {
        if (evaluationRepository.existsByRecommendationId(recommendation.getId())) {
            recommendation.updateStatus(status);
            recommendationRepository.save(recommendation);
            return;
        }
        BigDecimal pnlPct = exitPrice.subtract(recommendation.getEntryPrice())
                .divide(recommendation.getEntryPrice(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal drawdownPct = calculateDrawdownPct(recommendation, exitPrice);
        evaluationRepository.save(new EvaluationEntity(
                recommendation.getId(),
                exitPrice,
                exitReason,
                pnlPct,
                drawdownPct,
                hitTarget,
                LocalDateTime.now(SEOUL_ZONE)
        ));
        recommendation.updateStatus(status);
        recommendationRepository.save(recommendation);
        log.info("추천 자동 청산. ticker={}, status={}, exitReason={}, pnlPct={}",
                recommendation.getTicker(), status, exitReason, pnlPct);
    }

    private BigDecimal calculateDrawdownPct(RecommendationEntity recommendation, BigDecimal exitPrice) {
        BigDecimal entryPrice = recommendation.getEntryPrice();
        if (entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        LocalDateTime from = recommendation.getGeneratedAt() == null ? LocalDateTime.now(SEOUL_ZONE).minusDays(1) : recommendation.getGeneratedAt();
        BigDecimal minPrice = priceIntradayRepository.findByMarketAndTickerAndTickAtBetweenOrderByTickAtAsc(
                        recommendation.getMarket(),
                        recommendation.getTicker(),
                        from,
                        LocalDateTime.now(SEOUL_ZONE)
                ).stream()
                .map(row -> row.getPrice())
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(exitPrice);
        if (exitPrice != null && exitPrice.compareTo(minPrice) < 0) minPrice = exitPrice;
        if (minPrice.compareTo(entryPrice) > 0) minPrice = entryPrice;
        return minPrice.subtract(entryPrice)
                .divide(entryPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean shouldRunAtConfiguredInterval(int pollingIntervalMinutes) {
        if (pollingIntervalMinutes <= 0) return false;
        LocalTime now = LocalTime.now(SEOUL_ZONE);
        int minuteOfDay = now.getHour() * 60 + now.getMinute();
        return minuteOfDay % pollingIntervalMinutes == 0;
    }

    private String buildEventKey(RecommendationEntity recommendation, String eventType, BigDecimal currentPrice) {
        return "exit-monitor:%s:%s:%s".formatted(recommendation.getId(), eventType, priceBucket(recommendation.getEntryPrice(), currentPrice));
    }

    private String priceBucket(BigDecimal basePrice, BigDecimal currentPrice) {
        if (basePrice == null || currentPrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) return "unknown";
        return currentPrice.divide(basePrice, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean isKoreanMarket(String market) {
        return "KOSPI".equals(market) || "KOSDAQ".equals(market);
    }

    private int getIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(s -> extractIntValue(s.getValueJson(), defaultValue))
                .orElse(defaultValue);
    }

    private boolean getBooleanSetting(String key, boolean defaultValue) {
        return appSettingRepository.findById(key)
                .map(s -> extractBooleanValue(s.getValueJson(), defaultValue))
                .orElse(defaultValue);
    }

    private int extractIntValue(String valueJson, int defaultValue) {
        try {
            return objectMapper.readTree(valueJson).path("value").asInt(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String valueJson, boolean defaultValue) {
        try {
            return objectMapper.readTree(valueJson).path("value").asBoolean(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
