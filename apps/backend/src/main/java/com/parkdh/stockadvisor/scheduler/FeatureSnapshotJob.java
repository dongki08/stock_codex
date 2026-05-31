package com.parkdh.stockadvisor.scheduler;

import com.parkdh.stockadvisor.domain.feature.FeatureSnapshotEntity;
import com.parkdh.stockadvisor.domain.price.PriceDailyEntity;
import com.parkdh.stockadvisor.domain.universe.MarketUniverseEntity;
import com.parkdh.stockadvisor.application.feature.UniverseFeature;
import com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilder;
import com.parkdh.stockadvisor.infrastructure.persistence.feature.FeatureSnapshotRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository;
import com.parkdh.stockadvisor.infrastructure.persistence.universe.MarketUniverseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * TASK-2: Point-in-time 피처 스냅샷 잡.
 * 매 장마감 후 유니버스 전 종목의 오늘자 피처를 저장하고,
 * 이전 스냅샷의 forward return(T+5, T+20)을 사후 백필한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FeatureSnapshotJob {
    // forward return 백필 대기 일수 (T+5 거래일 = 약 7일)
    private static final int FWD_RET_5D_WAIT_DAYS = 7;
    // T+20 거래일 = 약 28일
    private static final int FWD_RET_20D_WAIT_DAYS = 28;

    private final UniverseFeatureBuilder universeFeatureBuilder;
    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final MarketUniverseRepository marketUniverseRepository;
    private final PriceDailyRepository priceDailyRepository;

    // 매 평일 22시 (KST) — 한국장·미국장 모두 마감 후
    @Scheduled(cron = "0 0 22 * * MON-FRI", zone = "Asia/Seoul")
    @Transactional
    public void runDailySnapshot() {
        LocalDate today = LocalDate.now(); // 기준일
        log.info("FeatureSnapshotJob 스냅샷 시작. asOf={}", today);
        List<MarketUniverseEntity> universe = marketUniverseRepository.findByTradableAndDelistedAtIsNull(true); // 거래 가능 전 종목
        int saved = 0;
        int failed = 0;
        for (MarketUniverseEntity entity : universe) {
            try {
                UniverseFeature feature = universeFeatureBuilder.buildFeatureAsOf(entity, today); // PIT 피처 계산
                upsertSnapshot(entity.getMarket(), entity.getTicker(), today, feature.totalScore(), feature.featureJson());
                saved++;
            } catch (Exception e) {
                failed++;
                log.debug("FeatureSnapshotJob 피처 계산 실패. market={}, ticker={}, error={}", entity.getMarket(), entity.getTicker(), e.getMessage());
            }
        }
        log.info("FeatureSnapshotJob 스냅샷 완료. asOf={}, saved={}, failed={}", today, saved, failed);

        // forward return 백필
        backfillForwardReturns(today);
    }

    private void upsertSnapshot(String market, String ticker, LocalDate asOfDate, int totalScore, String featureJson) {
        String key = FeatureSnapshotEntity.buildKey(market, ticker, asOfDate);
        featureSnapshotRepository.findById(key)
                .ifPresentOrElse(
                        existing -> {
                            existing.updateFeature(totalScore, featureJson);
                            featureSnapshotRepository.save(existing);
                        },
                        () -> featureSnapshotRepository.save(new FeatureSnapshotEntity(market, ticker, asOfDate, totalScore, featureJson))
                );
    }

    // 이전 스냅샷의 T+5/T+20 forward return 백필
    private void backfillForwardReturns(LocalDate today) {
        LocalDate cutoff5d = today.minusDays(FWD_RET_5D_WAIT_DAYS); // 5거래일 경과 대기
        List<FeatureSnapshotEntity> pending = featureSnapshotRepository.findByFwdRet5dIsNullAndAsOfDateLessThanEqual(cutoff5d);
        if (pending.isEmpty()) return;

        int filled = 0;
        for (FeatureSnapshotEntity snapshot : pending) {
            try {
                BigDecimal entryClose = findCloseAt(snapshot.getMarket(), snapshot.getTicker(), snapshot.getAsOfDate());
                if (entryClose == null) continue;

                BigDecimal fwd5 = computeForwardReturn(snapshot.getMarket(), snapshot.getTicker(), snapshot.getAsOfDate(), 5, entryClose);
                BigDecimal fwd20 = computeForwardReturn(snapshot.getMarket(), snapshot.getTicker(), snapshot.getAsOfDate(), 20, entryClose);

                snapshot.updateForwardReturns(fwd5, fwd20);
                featureSnapshotRepository.save(snapshot);
                filled++;
            } catch (Exception e) {
                log.debug("FeatureSnapshotJob forward return 백필 실패. key={}, error={}", snapshot.getSnapshotKey(), e.getMessage());
            }
        }
        if (filled > 0) {
            log.info("FeatureSnapshotJob forward return 백필 완료. filled={}", filled);
        }
    }

    private BigDecimal findCloseAt(String market, String ticker, LocalDate date) {
        return priceDailyRepository.findByMarketAndTickerAndTradeDate(market, ticker, date)
                .map(PriceDailyEntity::getClosePrice)
                .orElse(null);
    }

    private BigDecimal computeForwardReturn(String market, String ticker, LocalDate asOf, int nDays, BigDecimal entryClose) {
        List<PriceDailyEntity> future = priceDailyRepository.findByMarketAndTickerAndTradeDateGreaterThanOrderByTradeDateAsc(market, ticker, asOf, PageRequest.of(0, nDays));
        if (future.size() < nDays) return null; // 아직 가격 미존재
        BigDecimal exitClose = future.get(nDays - 1).getClosePrice();
        return exitClose.subtract(entryClose)
                .divide(entryClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
