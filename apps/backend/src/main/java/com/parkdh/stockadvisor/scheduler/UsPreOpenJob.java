package com.parkdh.stockadvisor.scheduler; // 스케줄러 패키지를 선언한다.

import com.parkdh.stockadvisor.api.dev.dto.DevRecommendationGenerateResponse; // 개발용 추천 생성 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse; // 일봉 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.universe.dto.MarketUniverseSyncResponse; // 유니버스 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.application.dev.DevRecommendationGenerateService; // 개발용 추천 생성 서비스를 가져온다.
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService; // 시장 데이터 동기화 서비스를 가져온다.
import com.parkdh.stockadvisor.application.notification.NotificationService;
import com.parkdh.stockadvisor.application.notification.NotificationService.NotificationMetric;
import com.parkdh.stockadvisor.application.universe.MarketUniverseService; // 시장 유니버스 서비스를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.scheduling.annotation.Scheduled; // 스케줄 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j // SLF4J 로거를 자동 생성한다.
@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class UsPreOpenJob { // 미국 장 전 브리핑 스케줄 작업을 정의한다.
    private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York"); // 미국장 DST 판단 기준 타임존을 정의한다.

    private final MarketUniverseService marketUniverseService; // 시장 유니버스 서비스를 보관한다.
    private final MarketDataSyncService marketDataSyncService; // 시장 데이터 동기화 서비스를 보관한다.
    private final DevRecommendationGenerateService devRecommendationGenerateService; // 개발용 추천 생성 서비스를 보관한다.
    private final NotificationService notificationService;
    private final SchedulerSettingReader schedulerSettingReader; // 스케줄러 설정 조회 도구를 보관한다.

    @Scheduled(cron = "0 * * * * MON-FRI", zone = "Asia/Seoul") // 평일 매분 설정된 미국 프리오픈 시각인지 확인한다.
    public void runConfiguredPreOpen() { // 설정 기반 미국 장 전 작업을 실행한다.
        boolean currentDst = isUsDstNow(); // 현재 미국 DST 여부를 계산한다.
        String fieldName = currentDst ? "dstTime" : "standardTime"; // DST 여부에 따른 설정 필드명을 결정한다.
        String configuredTime = schedulerSettingReader.getStringField("notification.us.preopen.offsetMinutes", fieldName, currentDst ? "22:00" : "23:00"); // 미국 프리오픈 시각을 조회한다.
        if (!isCurrentSeoulMinute(configuredTime)) { // 현재 시간이 설정 시각인지 확인한다.
            return; // 대상 시간이 아니면 종료한다.
        } // 설정 시각 확인을 종료한다.
        run(); // 실제 작업을 실행한다.
    } // 설정 기반 미국 장 전 작업을 종료한다.

    private void run() { // 스케줄 작업을 실행한다.
        log.info("UsPreOpenJob 시작"); // 작업 시작 로그를 출력한다.
        try { // 예외를 처리한다.
            if (!schedulerSettingReader.getBooleanField("recommendation.market.enabled", "us", true)) { // 미국 시장 활성화 설정을 확인한다.
                log.info("UsPreOpenJob 비활성화. setting=recommendation.market.enabled.us"); // 비활성화 로그를 출력한다.
                return; // 작업을 종료한다.
            } // 미국 시장 활성화 확인을 종료한다.
            LocalDate tradeDate = LocalDate.now(NEW_YORK_ZONE); // 미국 거래일 기준 날짜를 조회한다.
            if (schedulerSettingReader.containsDate("notification.holiday.us.closedDates", tradeDate)) { // 미국 휴장일 목록에 포함되는지 확인한다.
                log.info("UsPreOpenJob 휴장일로 건너뜀. date={}", tradeDate); // 휴장일 로그를 출력한다.
                if (schedulerSettingReader.getBoolean("notification.holiday.enabled", true)) { // 휴장일 알림 설정을 확인한다.
                    notificationService.sendSchedulerEvent(
                            "us-preopen",
                            "holiday",
                            "US PreOpen",
                            tradeDate,
                            List.of(new NotificationMetric("상태", "휴장일")),
                            List.of("정규 프리오픈 작업을 건너뜁니다.")
                    );
                } // 휴장 알림 확인을 종료한다.
                return; // 작업을 종료한다.
            } // 휴장일 확인을 종료한다.
            int shortCount = schedulerSettingReader.getInt("recommendation.short.count", 3); // 단기 추천 개수를 조회한다.
            int longCount = schedulerSettingReader.getInt("recommendation.long.count", 3); // 장기 추천 개수를 조회한다.
            MarketUniverseSyncResponse universe = marketUniverseService.syncUsSymbols("ALL"); // 미국 상장 심볼을 동기화한다.
            MarketUniverseSyncResponse quotes = marketUniverseService.syncUsPrices("NASDAQ", 50); // NASDAQ 최근 가격을 동기화한다.
            PriceDailySyncResponse daily = marketDataSyncService.syncDailyPrices("NASDAQ", 30, 180); // NASDAQ 일봉을 제한 수량으로 동기화한다.
            DevRecommendationGenerateResponse recommendations = devRecommendationGenerateService.generate("NASDAQ", shortCount, longCount); // NASDAQ 추천을 생성한다.
            notificationService.sendSchedulerEvent(
                    "us-preopen",
                    "success",
                    "US PreOpen",
                    tradeDate,
                    List.of(
                            new NotificationMetric("후보군 저장", universe.upsertedCount() + "개"),
                            new NotificationMetric("최근 가격 갱신", quotes.upsertedCount() + "개"),
                            new NotificationMetric("일봉 저장", daily.upsertedCount() + "개"),
                            new NotificationMetric("추천 생성", recommendations.generatedRecommendationCount() + "건")
                    ),
                    List.of("추천 IDs: " + recommendations.recommendationIds())
            );
            log.info("UsPreOpenJob 완료. universeSaved={}, quotesSaved={}, dailySaved={}, recommendations={}", universe.upsertedCount(), quotes.upsertedCount(), daily.upsertedCount(), recommendations.generatedRecommendationCount()); // 작업 완료 로그를 출력한다.
        } catch (Exception exception) { // 예외를 잡는다.
            log.error("UsPreOpenJob 실행 중 오류가 발생했습니다. error={}", exception.getMessage(), exception); // 오류 로그를 출력한다.
            notificationService.sendSchedulerError("us-preopen", LocalDate.now(NEW_YORK_ZONE), exception.getMessage());
        } // 예외 처리를 종료한다.
    } // 스케줄 작업을 종료한다.

    private boolean isUsDstNow() { // 미국 동부 DST 여부를 계산한다.
        Instant now = Instant.now(); // 현재 시각을 조회한다.
        return NEW_YORK_ZONE.getRules().isDaylightSavings(now); // DST 적용 여부를 반환한다.
    } // 미국 동부 DST 여부 계산을 종료한다.

    private boolean isCurrentSeoulMinute(String configuredTime) { // 현재 서울 시간이 설정된 HH:mm과 일치하는지 확인한다.
        try { // 설정 파싱 예외를 처리한다.
            LocalTime target = LocalTime.parse(configuredTime); // 설정 시간을 파싱한다.
            LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0); // 현재 서울 시간을 분 단위로 정규화한다.
            return now.equals(target); // 일치 여부를 반환한다.
        } catch (Exception exception) { // 파싱 실패를 잡는다.
            log.warn("UsPreOpenJob 알림 시각 설정이 올바르지 않습니다. configuredTime={}", configuredTime); // 경고 로그를 출력한다.
            return false; // 잘못된 설정이면 실행하지 않는다.
        } // 예외 처리를 종료한다.
    } // 현재 서울 시간 확인을 종료한다.
} // 미국 장 전 브리핑 작업을 종료한다.
