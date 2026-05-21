package com.parkdh.stockadvisor.scheduler; // 스케줄러 패키지를 선언한다.

import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient; // Telegram 클라이언트를 가져온다.
import com.parkdh.stockadvisor.infrastructure.persistence.recommendation.RecommendationRepository; // 추천 저장소를 가져온다.
import lombok.RequiredArgsConstructor; // 생성자 주입 어노테이션을 가져온다.
import lombok.extern.slf4j.Slf4j; // SLF4J 로거 어노테이션을 가져온다.
import org.springframework.scheduling.annotation.Scheduled; // 스케줄 어노테이션을 가져온다.
import org.springframework.stereotype.Component; // 컴포넌트 어노테이션을 가져온다.

@Slf4j // SLF4J 로거를 자동 생성한다.
@RequiredArgsConstructor // private final 필드의 생성자를 자동 생성한다.
@Component // 스프링 컴포넌트로 등록한다.
public class UsCloseSummaryJob { // 미장 마감 요약 스케줄 작업을 정의한다.

    private final RecommendationRepository recommendationRepository; // 추천 저장소를 보관한다.
    private final TelegramClient telegramClient; // Telegram 클라이언트를 보관한다.

    @Scheduled(cron = "0 30 5 * * TUE-SAT", zone = "Asia/Seoul") // 화~토 05:30 KST에 실행한다.
    public void run() { // 스케줄 작업을 실행한다.
        log.info("UsCloseSummaryJob 시작"); // 작업 시작 로그를 출력한다.
        try { // 예외를 처리한다.
            int openCount = recommendationRepository.findByStatus("OPEN").size(); // OPEN 상태 추천 수를 조회한다.
            String message = "🌅 미장 마감 요약\n보유 OPEN 추천: " + openCount + "건"; // 요약 메시지를 구성한다.
            telegramClient.sendMessage(message); // 요약 메시지를 전송한다.
            log.info("UsCloseSummaryJob 완료. OPEN 추천={}건", openCount); // 작업 완료 로그를 출력한다.
        } catch (Exception exception) { // 예외를 잡는다.
            log.error("UsCloseSummaryJob 실행 중 오류가 발생했습니다. error={}", exception.getMessage(), exception); // 오류 로그를 출력한다.
            telegramClient.sendMessage("❌ UsCloseSummaryJob 오류: " + exception.getMessage()); // 오류 알림 메시지를 전송한다.
        } // 예외 처리를 종료한다.
    } // 스케줄 작업을 종료한다.
} // 미장 마감 요약 작업을 종료한다.
