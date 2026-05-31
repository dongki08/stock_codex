package com.parkdh.stockadvisor.scheduler; // 스케줄러 패키지를 선언한다.

import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.application.notification.NotificationService.NotificationMetric;
import com.parkdh.stockadvisor.application.stats.StatsService;
import com.parkdh.stockadvisor.infrastructure.persistence.price.PriceDailyRepository; // 일봉 가격 저장소를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.data.domain.PageRequest; // 페이징 요청 타입을 가져온다.
import org.springframework.scheduling.annotation.Scheduled; // 스케줄 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j // SLF4J 로거를 자동 생성한다.
@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class UsCloseSummaryJob { // 미장 마감 요약 스케줄 작업을 정의한다.
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul"); // 스케줄 실행 기준 타임존을 정의한다.
    private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York"); // 미국장 DST 판단 기준 타임존을 정의한다.
    private static final Set<String> US_MARKETS = Set.of("NASDAQ", "NYSE", "AMEX"); // 미국 시장 코드를 정의한다.

    private final RecommendationRepository recommendationRepository; // 추천 저장소를 보관한다.
    private final PriceDailyRepository priceDailyRepository; // 일봉 가격 저장소를 보관한다.
    private final NotificationService notificationService;
    private final StatsService statsService;
    private final SchedulerSettingReader schedulerSettingReader; // 스케줄러 설정 조회 도구를 보관한다.

    @Scheduled(cron = "0 * * * * TUE-SAT", zone = "Asia/Seoul") // 화~토 매분 설정된 미국 마감 요약 시각인지 확인한다.
    public void runConfiguredCloseSummary() { // 설정 기반 미국 마감 요약 작업을 실행한다.
        boolean currentDst = isUsDstNow(); // 현재 미국 DST 여부를 계산한다.
        String fieldName = currentDst ? "dstTime" : "standardTime"; // DST 여부에 따른 설정 필드명을 결정한다.
        String configuredTime = schedulerSettingReader.getStringField("notification.us.close.offsetMinutes", fieldName, currentDst ? "05:30" : "06:30"); // 미국 마감 요약 시각을 조회한다.
        if (!isCurrentSeoulMinute(configuredTime)) { // 현재 시간이 설정 시각인지 확인한다.
            return; // 대상 시간이 아니면 종료한다.
        } // 설정 시각 확인을 종료한다.
        run(); // 실제 작업을 실행한다.
    } // 설정 기반 미국 마감 요약 작업을 종료한다.

    private void run() { // 스케줄 작업을 실행한다.
        log.info("UsCloseSummaryJob 시작"); // 작업 시작 로그를 출력한다.
        try { // 예외를 처리한다.
            if (!schedulerSettingReader.getBooleanField("recommendation.market.enabled", "us", true)) { // 미국 시장 활성화 설정을 확인한다.
                log.info("UsCloseSummaryJob 비활성화. setting=recommendation.market.enabled.us"); // 비활성화 로그를 출력한다.
                return; // 작업을 종료한다.
            } // 미국 시장 활성화 확인을 종료한다.
            LocalDate tradeDate = LocalDate.now(NEW_YORK_ZONE); // 미국 거래일 기준 날짜를 조회한다.
            if (schedulerSettingReader.containsDate("notification.holiday.us.closedDates", tradeDate)) { // 미국 휴장일 목록에 포함되는지 확인한다.
                log.info("UsCloseSummaryJob 휴장일로 건너뜀. date={}", tradeDate); // 휴장일 로그를 출력한다.
                if (schedulerSettingReader.getBoolean("notification.holiday.enabled", true)) { // 휴장일 알림 설정을 확인한다.
                    notificationService.sendSchedulerEvent(
                            "us-close-summary",
                            "holiday",
                            "US Close Summary",
                            tradeDate,
                            List.of(new NotificationMetric("상태", "휴장일")),
                            List.of("마감 요약 작업을 건너뜁니다.")
                    );
                } // 휴장 알림 확인을 종료한다.
                return; // 작업을 종료한다.
            } // 휴장일 확인을 종료한다.
            String message = buildCloseSummaryMessage(tradeDate); // 마감 요약 메시지를 구성한다.
            notificationService.sendSchedulerMessage("us-close-summary", "success", tradeDate, message);
            log.info("UsCloseSummaryJob 완료. tradeDate={}", tradeDate); // 작업 완료 로그를 출력한다.
        } catch (Exception exception) { // 예외를 잡는다.
            log.error("UsCloseSummaryJob 실행 중 오류가 발생했습니다. error={}", exception.getMessage(), exception); // 오류 로그를 출력한다.
            notificationService.sendSchedulerError("us-close-summary", LocalDate.now(NEW_YORK_ZONE), exception.getMessage());
        } // 예외 처리를 종료한다.
    } // 스케줄 작업을 종료한다.

    private boolean isUsDstNow() { // 미국 동부 DST 여부를 계산한다.
        Instant now = Instant.now(); // 현재 시각을 조회한다.
        return NEW_YORK_ZONE.getRules().isDaylightSavings(now); // DST 적용 여부를 반환한다.
    } // 미국 동부 DST 여부 계산을 종료한다.

    private boolean isCurrentSeoulMinute(String configuredTime) { // 현재 서울 시간이 설정된 HH:mm과 일치하는지 확인한다.
        try { // 설정 파싱 예외를 처리한다.
            LocalTime target = LocalTime.parse(configuredTime); // 설정 시간을 파싱한다.
            LocalTime now = LocalTime.now(SEOUL_ZONE).withSecond(0).withNano(0); // 현재 서울 시간을 분 단위로 정규화한다.
            return now.equals(target); // 일치 여부를 반환한다.
        } catch (Exception exception) { // 파싱 실패를 잡는다.
            log.warn("UsCloseSummaryJob 알림 시각 설정이 올바르지 않습니다. configuredTime={}", configuredTime); // 경고 로그를 출력한다.
            return false; // 잘못된 설정이면 실행하지 않는다.
        } // 예외 처리를 종료한다.
    } // 현재 서울 시간 확인을 종료한다.

    private String buildCloseSummaryMessage(LocalDate tradeDate) { // 미국장 마감 요약 메시지를 만든다.
        List<RecommendationSnapshot> snapshots = recommendationRepository.findByStatus("OPEN").stream()
                .filter(recommendation -> US_MARKETS.contains(recommendation.getMarket()))
                .map(recommendation -> {
                    BigDecimal closePrice = priceDailyRepository
                            .findByMarketAndTickerOrderByTradeDateDesc(recommendation.getMarket(), recommendation.getTicker(), PageRequest.of(0, 1))
                            .stream()
                            .findFirst()
                            .map(price -> price.getClosePrice())
                            .orElse(null);
                    return RecommendationSnapshot.from(recommendation, closePrice, tradeDate);
                })
                .toList();

        int openCount = snapshots.size();
        int pricedCount = (int) snapshots.stream().filter(snapshot -> snapshot.pnlPct() != null).count();
        int targetReachedCount = (int) snapshots.stream().filter(RecommendationSnapshot::targetReached).count();
        int expiredCount = (int) snapshots.stream().filter(RecommendationSnapshot::expired).count();
        BigDecimal avgPnl = snapshots.stream()
                .filter(snapshot -> snapshot.pnlPct() != null)
                .map(RecommendationSnapshot::pnlPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pricedCount > 0) {
            avgPnl = avgPnl.divide(BigDecimal.valueOf(pricedCount), 2, RoundingMode.HALF_UP);
        }

        StringBuilder message = new StringBuilder()
                .append("🌅 미장 마감 요약\n")
                .append("거래일: ").append(tradeDate).append("\n")
                .append("OPEN 추천: ").append(openCount).append("건")
                .append(" · 가격 확인: ").append(pricedCount).append("건\n")
                .append("평균 손익: ").append(formatPercent(avgPnl)).append("\n")
                .append("목표 도달: ").append(targetReachedCount).append("건")
                .append(" · 예상 종료일 초과: ").append(expiredCount).append("건");

        List<RecommendationSnapshot> highlights = snapshots.stream()
                .filter(snapshot -> snapshot.pnlPct() != null)
                .sorted(Comparator.comparing(RecommendationSnapshot::pnlPct))
                .limit(5)
                .toList();
        if (!highlights.isEmpty()) {
            message.append("\n\n하위 손익 5건");
            highlights.forEach(snapshot -> message
                    .append("\n")
                    .append(snapshot.ticker())
                    .append(" ")
                    .append(formatPercent(snapshot.pnlPct()))
                    .append(" · stop ")
                    .append(formatPercent(snapshot.stopDistancePct())));
        }
        message.append("\n\n")
                .append(notificationService.formatPaperTradingSummary(statsService.getPaperTrading()));
        return message.toString();
    }

    private String formatPercent(BigDecimal value) { // 퍼센트 표시 문자열을 만든다.
        if (value == null) {
            return "N/A";
        }
        return (value.signum() > 0 ? "+" : "") + value.setScale(2, RoundingMode.HALF_UP) + "%";
    } // 퍼센트 표시 문자열 생성을 종료한다.

    private record RecommendationSnapshot(
            String ticker,
            BigDecimal pnlPct,
            BigDecimal stopDistancePct,
            boolean targetReached,
            boolean expired
    ) {
        private static RecommendationSnapshot from(com.parkdh.stockadvisor.domain.recommendation.RecommendationEntity recommendation, BigDecimal closePrice, LocalDate tradeDate) {
            if (closePrice == null || closePrice.compareTo(BigDecimal.ZERO) <= 0 || recommendation.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return new RecommendationSnapshot(
                        recommendation.getTicker(),
                        null,
                        null,
                        false,
                        tradeDate.isAfter(recommendation.getExpectedExitAt())
                );
            }
            BigDecimal pnlPct = closePrice.subtract(recommendation.getEntryPrice())
                    .divide(recommendation.getEntryPrice(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            BigDecimal stopDistancePct = closePrice.subtract(recommendation.getStopPrice())
                    .divide(closePrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            boolean targetReached = closePrice.compareTo(recommendation.getTargetPrice()) >= 0;
            boolean expired = tradeDate.isAfter(recommendation.getExpectedExitAt());
            return new RecommendationSnapshot(recommendation.getTicker(), pnlPct, stopDistancePct, targetReached, expired);
        }
    }
} // 미장 마감 요약 작업을 종료한다.
