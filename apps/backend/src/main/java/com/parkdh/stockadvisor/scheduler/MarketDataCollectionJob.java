package com.parkdh.stockadvisor.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkdh.stockadvisor.api.marketdata.dto.MarketDataCollectionSyncResponse;
import com.parkdh.stockadvisor.application.marketdata.MarketDataCollectionService;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.infrastructure.persistence.setting.AppSettingRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MarketDataCollectionJob {
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final MarketDataCollectionService marketDataCollectionService;
    private final MarketDataSyncService marketDataSyncService;
    private final MarketUniverseRepository marketUniverseRepository;
    private final AppSettingRepository appSettingRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 15 8 * * MON-FRI", zone = "Asia/Seoul")
    public void runKrxPreOpenCollection() {
        runMarketCollection("KRX", List.of("KOSPI", "KOSDAQ"));
    }

    @Scheduled(cron = "0 40 21 * * MON-FRI", zone = "Asia/Seoul")
    public void runUsPreOpenCollection() {
        // TASK-8: regime 필터용 지수 일봉 먼저 동기화 (^KS11 KOSPI, SPY)
        try {
            int indexSaved = marketDataSyncService.syncIndexDailyPrices(260); // 약 1년치 적재
            log.info("MarketDataCollectionJob 지수 일봉 동기화 완료. saved={}", indexSaved);
        } catch (Exception exception) {
            log.warn("MarketDataCollectionJob 지수 일봉 동기화 실패. error={}", exception.getMessage());
        }
        runMarketCollection("US", List.of("NASDAQ", "NYSE"));
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "Asia/Seoul")
    public void runMacroCollection() {
        if (!getBooleanSetting("collection.enabled", true)) {
            log.info("MarketDataCollectionJob macro skipped. collection.enabled=false");
            return;
        }
        try {
            MarketDataCollectionSyncResponse macro = marketDataCollectionService.syncMacroObservations(null, getIntSetting("collection.macro.limitPerSeries", 5));
            notificationService.sendTelegramOnce(
                    "collection:macro:" + LocalDate.now().format(DATE_KEY_FORMATTER),
                    "Market data macro collection\nsaved=" + macro.savedCount() + "/" + macro.fetchedCount()
            );
            log.info("MarketDataCollectionJob macro completed. saved={}, fetched={}", macro.savedCount(), macro.fetchedCount());
        } catch (Exception exception) {
            log.error("MarketDataCollectionJob macro failed. error={}", exception.getMessage(), exception);
            notificationService.sendTelegramOnce("collection:macro:error:" + LocalDate.now().format(DATE_KEY_FORMATTER), "❌ Macro collection error: " + exception.getMessage());
        }
    }

    private void runMarketCollection(String track, List<String> markets) {
        if (!getBooleanSetting("collection.enabled", true)) {
            log.info("MarketDataCollectionJob {} skipped. collection.enabled=false", track);
            return;
        }
        log.info("MarketDataCollectionJob {} 시작", track);
        try {
            int tickersPerMarket = getIntSetting("collection.news.tickersPerMarket", 5);
            int newsLimitPerTicker = getIntSetting("collection.news.limitPerTicker", 5);
            int disclosureLimit = getIntSetting("collection.disclosure.limit", 20);
            int fundamentalTickersPerMarket = getIntSetting("collection.fundamental.tickersPerMarket", 3);
            int newsSaved = 0;
            int newsFetched = 0;
            int fundamentalSaved = 0;
            int fundamentalFetched = 0;

            for (String market : markets) {
                for (MarketUniverseEntity candidate : topCandidates(market, tickersPerMarket)) {
                    MarketDataCollectionSyncResponse news = marketDataCollectionService.syncNewsArticles(market, candidate.getTicker(), newsLimitPerTicker);
                    newsSaved += news.savedCount();
                    newsFetched += news.fetchedCount();
                }
            }

            if ("US".equals(track)) {
                for (String market : markets) {
                    for (MarketUniverseEntity candidate : topCandidates(market, fundamentalTickersPerMarket)) {
                        MarketDataCollectionSyncResponse fundamentals = marketDataCollectionService.syncFundamentalMetrics(market, candidate.getTicker());
                        fundamentalSaved += fundamentals.savedCount();
                        fundamentalFetched += fundamentals.fetchedCount();
                    }
                }
            }

            int disclosureSaved = 0;
            int disclosureFetched = 0;
            for (String market : markets) {
                MarketDataCollectionSyncResponse disclosures = marketDataCollectionService.syncDisclosureEvents(market, null, disclosureLimit);
                disclosureSaved += disclosures.savedCount();
                disclosureFetched += disclosures.fetchedCount();
            }

            String message = "%s data collection\nnews=%d/%d\ndisclosures=%d/%d\nfundamentals=%d/%d".formatted(track, newsSaved, newsFetched, disclosureSaved, disclosureFetched, fundamentalSaved, fundamentalFetched);
            notificationService.sendTelegramOnce("collection:" + track + ":" + LocalDate.now().format(DATE_KEY_FORMATTER), message);
            log.info("MarketDataCollectionJob {} 완료. news={}/{}, disclosures={}/{}, fundamentals={}/{}", track, newsSaved, newsFetched, disclosureSaved, disclosureFetched, fundamentalSaved, fundamentalFetched);
        } catch (Exception exception) {
            log.error("MarketDataCollectionJob {} 실행 중 오류가 발생했습니다. error={}", track, exception.getMessage(), exception);
            notificationService.sendTelegramOnce("collection:" + track + ":error:" + LocalDate.now().format(DATE_KEY_FORMATTER), "❌ " + track + " collection error: " + exception.getMessage());
        }
    }

    private List<MarketUniverseEntity> topCandidates(String market, int limit) {
        return marketUniverseRepository.findByMarketAndTradableAndDelistedAtIsNull(market, true).stream()
                .sorted(Comparator.comparing(MarketUniverseEntity::getAvgTurnover, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private int getIntSetting(String key, int defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> extractIntValue(setting.getValueJson(), defaultValue))
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

    private boolean extractBooleanValue(String valueJson, boolean defaultValue) {
        try {
            JsonNode node = objectMapper.readTree(valueJson);
            return node.path("value").asBoolean(defaultValue);
        } catch (Exception exception) {
            return defaultValue;
        }
    }
}
