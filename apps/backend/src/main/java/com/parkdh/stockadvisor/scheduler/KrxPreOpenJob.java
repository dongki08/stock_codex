package com.parkdh.stockadvisor.scheduler; // 스케줄러 패키지를 선언한다.

import com.parkdh.stockadvisor.api.dev.dto.DevRecommendationGenerateResponse; // 개발용 추천 생성 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.marketdata.dto.PriceDailySyncResponse; // 일봉 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.api.universe.dto.MarketUniverseSyncResponse; // 유니버스 동기화 응답 DTO를 가져온다.
import com.parkdh.stockadvisor.application.dev.DevRecommendationGenerateService; // 개발용 추천 생성 서비스를 가져온다.
import com.parkdh.stockadvisor.application.marketdata.MarketDataSyncService; // 시장 데이터 동기화 서비스를 가져온다.
import com.parkdh.stockadvisor.application.universe.MarketUniverseService; // 시장 유니버스 서비스를 가져온다.
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient; // Telegram 클라이언트를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.scheduling.annotation.Scheduled; // 스케줄 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

@Slf4j // SLF4J 로거를 자동 생성한다.
@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class KrxPreOpenJob { // KRX 장 전 브리핑 스케줄 작업을 정의한다.

    private final MarketUniverseService marketUniverseService; // 시장 유니버스 서비스를 보관한다.
    private final MarketDataSyncService marketDataSyncService; // 시장 데이터 동기화 서비스를 보관한다.
    private final DevRecommendationGenerateService devRecommendationGenerateService; // 개발용 추천 생성 서비스를 보관한다.
    private final TelegramClient telegramClient; // Telegram 클라이언트를 보관한다.

    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul") // 매일 08:30 KST에 실행한다.
    public void run() { // 스케줄 작업을 실행한다.
        log.info("KrxPreOpenJob 시작"); // 작업 시작 로그를 출력한다.
        try { // 예외를 처리한다.
            MarketUniverseSyncResponse universe = marketUniverseService.syncKrSymbols("ALL"); // 한국 상장 심볼을 동기화한다.
            PriceDailySyncResponse daily = marketDataSyncService.syncDailyPrices("KOSPI", 30, 180); // KOSPI 일봉을 제한 수량으로 동기화한다.
            DevRecommendationGenerateResponse recommendations = devRecommendationGenerateService.generate("KOSPI", 3, 3); // KOSPI 추천을 생성한다.
            String message = buildMessage(universe, daily, recommendations); // 브리핑 메시지를 구성한다.
            telegramClient.sendMessage(message); // 브리핑 메시지를 전송한다.
            log.info("KrxPreOpenJob 완료. universeSaved={}, dailySaved={}, recommendations={}", universe.upsertedCount(), daily.upsertedCount(), recommendations.generatedRecommendationCount()); // 작업 완료 로그를 출력한다.
        } catch (Exception exception) { // 예외를 잡는다.
            log.error("KrxPreOpenJob 실행 중 오류가 발생했습니다. error={}", exception.getMessage(), exception); // 오류 로그를 출력한다.
            telegramClient.sendMessage("❌ KrxPreOpenJob 오류: " + exception.getMessage()); // 오류 알림 메시지를 전송한다.
        } // 예외 처리를 종료한다.
    } // 스케줄 작업을 종료한다.

    private String buildMessage(MarketUniverseSyncResponse universe, PriceDailySyncResponse daily, DevRecommendationGenerateResponse recommendations) { // KRX 프리오픈 메시지를 만든다.
        return "KRX PreOpen\n" // 제목을 추가한다.
                + "후보군 저장: " + universe.upsertedCount() + "개\n" // 후보군 저장 수를 추가한다.
                + "일봉 저장: " + daily.upsertedCount() + "개\n" // 일봉 저장 수를 추가한다.
                + "추천 생성: " + recommendations.generatedRecommendationCount() + "건\n" // 추천 생성 수를 추가한다.
                + "추천 IDs: " + recommendations.recommendationIds(); // 추천 ID 목록을 추가한다.
    } // KRX 프리오픈 메시지 생성을 종료한다.
} // KRX 장 전 브리핑 작업을 종료한다.
