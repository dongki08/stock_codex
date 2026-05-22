package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.recommendation.dto.ExitConfirmResponse;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationLogService;
import com.parkdh.stockadvisor.application.notification.NotificationLogService.NotificationDispatchResult;
import com.parkdh.stockadvisor.application.recommendation.ExitConfirmService;
import com.parkdh.stockadvisor.domain.evaluation.EvaluationEntity;
import com.parkdh.stockadvisor.domain.recommendation.ExitConfirmLogEntity;
import com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient;
import com.parkdh.stockadvisor.infrastructure.marketdata.kr.KisApiClient.KisCurrentPrice;
import com.parkdh.stockadvisor.infrastructure.persistence.evaluation.EvaluationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceIntradayRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.ExitConfirmLogRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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
    private final ExitConfirmLogRepository exitConfirmLogRepository;
    private final KisApiClient kisApiClient;
    private final MarketDataSyncService marketDataSyncService;
    private final ExitConfirmService exitConfirmService;
    private final NotificationLogService notificationLogService;
    private final AppSettingRepository appSettingRepository;
    private final PriceIntradayRepository priceIntradayRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 * 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void run() {
        int pollingIntervalMinutes = getIntSetting("exit.polling.intervalMinutes", 5);
        if (!shouldRunAtConfiguredInterval(pollingIntervalMinutes)) {
            return;
        }
        log.info("ExitMonitorJob 시작");
        try {
            if (!getBooleanSetting("exit.intraday.enabled", true)) {
                log.info("ExitMonitorJob 비활성화. setting=exit.intraday.enabled");
                return;
            }

            List<RecommendationEntity> openRecommendations = recommendationRepository.findByStatus("OPEN");
            int confirmLimitPerRun = getIntSetting("exit.codex.confirmLimitPerRun", 5);
            int confirmCount = 0;
            for (RecommendationEntity recommendation : openRecommendations) {
                if (checkExitCondition(recommendation, confirmCount, confirmLimitPerRun).confirmed()) {
                    confirmCount++;
                }
            }
            log.info("ExitMonitorJob 완료. 확인 대상={}건, exitConfirm={}건", openRecommendations.size(), confirmCount);
        } catch (Exception exception) {
            log.error("ExitMonitorJob 실행 중 오류가 발생했습니다. error={}", exception.getMessage(), exception);
            notificationLogService.sendTelegramOnce(
                    "exit-monitor:error:" + LocalDate.now().format(DATE_KEY_FORMATTER) + ":" + exception.getMessage(),
                    "❌ ExitMonitorJob 오류: " + exception.getMessage()
            );
        }
    }

    private boolean shouldRunAtConfiguredInterval(int pollingIntervalMinutes) {
        if (pollingIntervalMinutes <= 0) {
            log.debug("ExitMonitorJob 폴링 주기가 0 이하입니다. interval={}", pollingIntervalMinutes);
            return false;
        }
        LocalTime now = LocalTime.now(SEOUL_ZONE);
        int minuteOfDay = now.getHour() * 60 + now.getMinute();
        return minuteOfDay % pollingIntervalMinutes == 0;
    }

    private MonitorResult checkExitCondition(RecommendationEntity recommendation, int confirmCount, int confirmLimitPerRun) {
        String ticker = recommendation.getTicker();
        String market = recommendation.getMarket();
        if (!isKoreanMarket(market)) {
            log.debug("국내 시장이 아니어서 ExitMonitorJob 현재가 조회를 건너뜁니다. market={}, ticker={}", market, ticker);
            return MonitorResult.skipped();
        }

        Optional<KisCurrentPrice> priceOptional = kisApiClient.fetchCurrentPrice(ticker);
        if (priceOptional.isEmpty()) {
            log.debug("현재가 조회 결과가 없습니다. ticker={} (개발 모드 또는 조회 실패)", ticker);
            return MonitorResult.skipped();
        }

        KisCurrentPrice currentPriceRow = priceOptional.get();
        BigDecimal currentPrice = currentPriceRow.currentPrice();
        marketDataSyncService.saveIntradayPrice(ticker, market, LocalDateTime.now(), currentPrice, currentPriceRow.volume(), "KIS");

        BigDecimal targetPrice = recommendation.getTargetPrice();
        BigDecimal stopPrice = recommendation.getStopPrice();
        boolean targetReached = targetPrice.compareTo(currentPrice) <= 0;
        boolean stopBreached = stopPrice.compareTo(currentPrice) >= 0;
        if (targetReached) {
            String message = "🎯 목표가 도달: " + ticker + "\n현재가=" + currentPrice + ", 목표가=" + targetPrice;
            notificationLogService.sendTelegramOnce(buildDailyEventKey(recommendation, "TARGET", currentPrice), message);
            log.info("목표가 도달 알림 처리. ticker={}, 현재가={}, 목표가={}", ticker, currentPrice, targetPrice);
        }
        if (stopBreached) {
            String message = "🛑 손절가 이탈: " + ticker + "\n현재가=" + currentPrice + ", 손절가=" + stopPrice;
            notificationLogService.sendTelegramOnce(buildDailyEventKey(recommendation, "STOP", currentPrice), message);
            log.info("손절가 이탈 알림 처리. ticker={}, 현재가={}, 손절가={}", ticker, currentPrice, stopPrice);
        }
        if (targetReached || stopBreached) {
            closeRecommendationWithEvaluation(recommendation, currentPrice, targetReached ? "TARGET_HIT" : "STOP_HIT", targetReached, "CLOSED");
            return MonitorResult.checked();
        }
        if (LocalDate.now(SEOUL_ZONE).isAfter(recommendation.getExpectedExitAt())) {
            closeRecommendationWithEvaluation(recommendation, currentPrice, "TIME_OUT", false, "EXPIRED");
            return MonitorResult.checked();
        }

        if (!isExitConfirmRiskZone(currentPrice, stopPrice)) {
            return MonitorResult.checked();
        }
        if (!canAutoConfirm(recommendation, confirmCount, confirmLimitPerRun)) {
            return MonitorResult.skipped();
        }

        ExitConfirmResponse response = exitConfirmService.confirm(recommendation.getId());
        NotificationDispatchResult notificationResult = notifyExitConfirmIfNeeded(response);
        exitConfirmLogRepository.save(new ExitConfirmLogEntity(
                response.recommendationId(),
                response.ticker(),
                response.market(),
                response.currentPrice(),
                response.stopPrice(),
                response.distancePct(),
                response.action(),
                response.usedFallback(),
                response.codexError(),
                notificationResult.sent(),
                notificationResult.sent() ? buildExitConfirmEventKey(response) : null,
                response.confirmedAt()
        ));
        log.info("Exit Confirm 자동 수행. recommendationId={}, ticker={}, action={}, notified={}",
                response.recommendationId(), response.ticker(), response.action(), notificationResult.sent());
        return MonitorResult.confirmedResult();
    }

    private void closeRecommendationWithEvaluation(RecommendationEntity recommendation, BigDecimal exitPrice, String exitReason, boolean hitTarget, String status) {
        if (evaluationRepository.existsByRecommendationId(recommendation.getId())) {
            recommendation.updateStatus(status);
            recommendationRepository.save(recommendation);
            log.debug("기존 평가가 있어 추천 상태만 갱신했습니다. recommendationId={}, status={}", recommendation.getId(), status);
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
        log.info("추천 자동 전환 및 평가 생성. recommendationId={}, ticker={}, status={}, exitReason={}, pnlPct={}",
                recommendation.getId(), recommendation.getTicker(), status, exitReason, pnlPct);
    }

    private BigDecimal calculateDrawdownPct(RecommendationEntity recommendation, BigDecimal exitPrice) {
        BigDecimal entryPrice = recommendation.getEntryPrice();
        if (entryPrice == null || entryPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        LocalDateTime from = recommendation.getGeneratedAt() == null ? LocalDateTime.now(SEOUL_ZONE).minusDays(1) : recommendation.getGeneratedAt();
        LocalDateTime to = LocalDateTime.now(SEOUL_ZONE);
        BigDecimal minPrice = priceIntradayRepository.findByMarketAndTickerAndTickAtBetweenOrderByTickAtAsc(
                        recommendation.getMarket(),
                        recommendation.getTicker(),
                        from,
                        to
                ).stream()
                .map(row -> row.getPrice())
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(exitPrice);
        if (exitPrice != null && exitPrice.compareTo(minPrice) < 0) {
            minPrice = exitPrice;
        }
        if (minPrice.compareTo(entryPrice) > 0) {
            minPrice = entryPrice;
        }
        return minPrice.subtract(entryPrice)
                .divide(entryPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean canAutoConfirm(RecommendationEntity recommendation, int confirmCount, int confirmLimitPerRun) {
        if (confirmLimitPerRun <= 0 || confirmCount >= confirmLimitPerRun) {
            log.debug("Exit Confirm 스케줄 한도 초과. recommendationId={}, limitPerRun={}", recommendation.getId(), confirmLimitPerRun);
            return false;
        }

        int dailyLimit = getIntSetting("exit.codex.confirmLimitPerTickerDaily", 3);
        if (dailyLimit <= 0) {
            log.debug("Exit Confirm 일일 한도가 0 이하입니다. recommendationId={}", recommendation.getId());
            return false;
        }

        LocalDate today = LocalDate.now();
        long dailyCount = exitConfirmLogRepository.countByMarketAndTickerAndConfirmedAtBetween(
                recommendation.getMarket(),
                recommendation.getTicker(),
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX)
        );
        if (dailyCount >= dailyLimit) {
            log.debug("Exit Confirm 종목별 일일 한도 초과. market={}, ticker={}, count={}, limit={}",
                    recommendation.getMarket(), recommendation.getTicker(), dailyCount, dailyLimit);
            return false;
        }

        int cooldownMinutes = getIntSetting("exit.codex.confirmCooldownMinutes", 60);
        if (cooldownMinutes > 0
                && exitConfirmLogRepository.existsByRecommendationIdAndConfirmedAtAfter(recommendation.getId(), LocalDateTime.now().minusMinutes(cooldownMinutes))) {
            log.debug("Exit Confirm 쿨다운 적용. recommendationId={}, cooldownMinutes={}", recommendation.getId(), cooldownMinutes);
            return false;
        }
        return true;
    }

    private NotificationDispatchResult notifyExitConfirmIfNeeded(ExitConfirmResponse response) {
        if (!"CUT".equals(response.action()) && !"TIGHTEN".equals(response.action())) {
            return new NotificationDispatchResult(false, false, null);
        }

        String message = """
                ⚠️ Exit Confirm: %s %s
                action=%s
                현재가=%s, 손절가=%s, 이격률=%s%%
                fallback=%s
                %s
                """.formatted(
                response.market(),
                response.ticker(),
                response.action(),
                response.currentPrice(),
                response.stopPrice(),
                response.distancePct(),
                response.usedFallback(),
                HtmlUtils.htmlEscape(response.rationale())
        );
        return notificationLogService.sendTelegramOnce(buildExitConfirmEventKey(response), message);
    }

    private boolean isExitConfirmRiskZone(BigDecimal currentPrice, BigDecimal stopPrice) {
        BigDecimal riskBandPercent = getDecimalSetting("exit.riskBand.percent", BigDecimal.valueOf(2.0));
        BigDecimal threshold = stopPrice.multiply(BigDecimal.ONE.add(riskBandPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        return currentPrice.compareTo(threshold) <= 0;
    }

    private String buildDailyEventKey(RecommendationEntity recommendation, String eventType, BigDecimal currentPrice) {
        return "exit-monitor:%s:%s:%s".formatted(recommendation.getId(), eventType, priceBucket(recommendation.getEntryPrice(), currentPrice));
    }

    private String priceBucket(BigDecimal basePrice, BigDecimal currentPrice) {
        if (basePrice == null || currentPrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return "unknown";
        }
        return currentPrice.divide(basePrice, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildExitConfirmEventKey(ExitConfirmResponse response) {
        return "exit-confirm:%s:%s:%s".formatted(response.recommendationId(), response.action(), LocalDate.now().format(DATE_KEY_FORMATTER));
    }

    private int getIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> extractIntValue(setting.getValueJson(), defaultValue))
                .orElse(defaultValue);
    }

    private BigDecimal getDecimalSetting(String key, BigDecimal defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> extractDecimalValue(setting.getValueJson(), defaultValue))
                .orElse(defaultValue);
    }

    private boolean getBooleanSetting(String key, boolean defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> extractBooleanValue(setting.getValueJson(), defaultValue))
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

    private BigDecimal extractDecimalValue(String valueJson, BigDecimal defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").decimalValue();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String valueJson, boolean defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asBoolean(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private boolean isKoreanMarket(String market) {
        return "KOSPI".equals(market) || "KOSDAQ".equals(market);
    }

    private record MonitorResult(boolean confirmed) {
        private static MonitorResult checked() {
            return new MonitorResult(false);
        }

        private static MonitorResult skipped() {
            return new MonitorResult(false);
        }

        private static MonitorResult confirmedResult() {
            return new MonitorResult(true);
        }
    }
}
