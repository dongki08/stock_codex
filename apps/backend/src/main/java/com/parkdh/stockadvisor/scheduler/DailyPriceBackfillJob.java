package com.parkdh.stockadvisor.scheduler;

import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse;
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService;
import com.parkdh.stockadvisor.application.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DailyPriceBackfillJob {
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final MarketDataSyncService marketDataSyncService;
    private final SchedulerSettingReader schedulerSettingReader;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 10 18 * * MON-FRI", zone = "Asia/Seoul")
    public void runKrxBackfill() {
        runKrxOpenApiBackfill();
    }

    @Scheduled(cron = "0 20 7 * * TUE-SAT", zone = "Asia/Seoul")
    public void runUsBackfill() {
        runBackfill("US", List.of("NASDAQ", "NYSE"), "collection.dailyPriceBackfill.us.limitPerMarket", 50, "collection.dailyPriceBackfill.us.days", 180);
    }

    public void triggerKrxBackfill() { runKrxBackfill(); }

    public void triggerUsBackfill() { runUsBackfill(); }

    private void runBackfill(String track, List<String> markets, String limitKey, int defaultLimit, String daysKey, int defaultDays) {
        if (!schedulerSettingReader.getBoolean("collection.dailyPriceBackfill.enabled", true)) {
            log.info("DailyPriceBackfillJob {} skipped. collection.dailyPriceBackfill.enabled=false", track);
            return;
        }

        int limit = schedulerSettingReader.getInt(limitKey, defaultLimit);
        int days = schedulerSettingReader.getInt(daysKey, defaultDays);
        BackfillTotals totals = new BackfillTotals();

        for (String market : markets) {
            try {
                PriceDailySyncResponse response = marketDataSyncService.syncDailyPrices(market, limit, days);
                totals.add(response);
                log.info(
                        "DailyPriceBackfillJob {} {} completed. candidates={}, requested={}, skippedUpToDate={}, skippedNoHistory={}, fetched={}, upserted={}",
                        track,
                        market,
                        response.candidateCount(),
                        response.requestedTickerCount(),
                        response.skippedUpToDateCount(),
                        response.skippedNoHistoryCount(),
                        response.fetchedCount(),
                        response.upsertedCount()
                );
            } catch (Exception exception) {
                totals.failedMarkets++;
                log.warn("DailyPriceBackfillJob {} {} failed. error={}", track, market, exception.getMessage(), exception);
            }
        }

        notificationService.sendTelegramOnce(
                "daily-price-backfill:" + track + ":" + LocalDate.now().format(DATE_KEY_FORMATTER),
                "%s daily price backfill\ncandidates=%d\nrequested=%d\nskippedUpToDate=%d\nskippedNoHistory=%d\nfetched=%d\nupserted=%d\nfailedMarkets=%d"
                        .formatted(
                                track,
                                totals.candidateCount,
                                totals.requestedTickerCount,
                                totals.skippedUpToDateCount,
                                totals.skippedNoHistoryCount,
                                totals.fetchedCount,
                                totals.upsertedCount,
                                totals.failedMarkets
                        )
        );
    }

    private void runKrxOpenApiBackfill() {
        if (!schedulerSettingReader.getBoolean("collection.dailyPriceBackfill.enabled", true)) {
            log.info("DailyPriceBackfillJob KRX skipped. collection.dailyPriceBackfill.enabled=false");
            return;
        }

        int days = schedulerSettingReader.getInt("collection.dailyPriceBackfill.kr.days", 120);
        LocalDate to = previousBusinessDay(LocalDate.now());
        LocalDate from = to.minusDays(days);
        BackfillTotals totals = new BackfillTotals();

        for (String market : List.of("KOSPI", "KOSDAQ")) {
            try {
                PriceDailySyncResponse response = marketDataSyncService.syncKrxDailyPrices(market, from, to);
                totals.add(response);
                log.info(
                        "DailyPriceBackfillJob KRX {} completed by KRX OpenAPI. requested={}, fetched={}, upserted={}",
                        market,
                        response.requestedTickerCount(),
                        response.fetchedCount(),
                        response.upsertedCount()
                );
            } catch (Exception exception) {
                totals.failedMarkets++;
                log.warn("DailyPriceBackfillJob KRX {} KRX OpenAPI failed. Falling back to legacy ticker sync. error={}", market, exception.getMessage(), exception);
                try {
                    PriceDailySyncResponse fallback = marketDataSyncService.syncDailyPrices(
                            market,
                            schedulerSettingReader.getInt("collection.dailyPriceBackfill.kr.limitPerMarket", 50),
                            days
                    );
                    totals.add(fallback);
                } catch (Exception fallbackException) {
                    totals.failedMarkets++;
                    log.warn("DailyPriceBackfillJob KRX {} fallback failed. error={}", market, fallbackException.getMessage(), fallbackException);
                }
            }
        }

        notificationService.sendTelegramOnce(
                "daily-price-backfill:KRX:" + LocalDate.now().format(DATE_KEY_FORMATTER),
                "%s daily price backfill\ncandidates=%d\nrequested=%d\nskippedUpToDate=%d\nskippedNoHistory=%d\nfetched=%d\nupserted=%d\nfailedMarkets=%d"
                        .formatted(
                                "KRX",
                                totals.candidateCount,
                                totals.requestedTickerCount,
                                totals.skippedUpToDateCount,
                                totals.skippedNoHistoryCount,
                                totals.fetchedCount,
                                totals.upsertedCount,
                                totals.failedMarkets
                        )
        );
    }

    private LocalDate previousBusinessDay(LocalDate date) {
        LocalDate cursor = date.minusDays(1);
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }

    private static final class BackfillTotals {
        private int candidateCount;
        private int requestedTickerCount;
        private int skippedUpToDateCount;
        private int skippedNoHistoryCount;
        private int fetchedCount;
        private int upsertedCount;
        private int failedMarkets;

        private void add(PriceDailySyncResponse response) {
            candidateCount += response.candidateCount();
            requestedTickerCount += response.requestedTickerCount();
            skippedUpToDateCount += response.skippedUpToDateCount();
            skippedNoHistoryCount += response.skippedNoHistoryCount();
            fetchedCount += response.fetchedCount();
            upsertedCount += response.upsertedCount();
        }
    }
}
