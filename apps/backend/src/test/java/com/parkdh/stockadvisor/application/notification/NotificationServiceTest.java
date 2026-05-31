package com.parkdh.stockadvisor.application.notification;

import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse.PaperPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationLogService notificationLogService;

    @Test
    void sendTelegramOnceEscapesHtmlBeforeDelegating() {
        NotificationService service = new NotificationService(notificationLogService);
        when(notificationLogService.sendTelegramOnce("job:key", "AAPL &lt;STOP&gt; &amp; risk"))
                .thenReturn(new NotificationLogService.NotificationDispatchResult(true, false, "hash"));

        NotificationLogService.NotificationDispatchResult result = service.sendTelegramOnce("job:key", "AAPL <STOP> & risk");

        assertThat(result.sent()).isTrue();
        verify(notificationLogService).sendTelegramOnce("job:key", "AAPL &lt;STOP&gt; &amp; risk");
    }

    @Test
    void sendSchedulerEventUsesStableDailyDedupeKeyAndFormattedBody() {
        NotificationService service = new NotificationService(notificationLogService);
        when(notificationLogService.sendTelegramOnce("scheduler:krx-preopen:success:20260526", """
                KRX PreOpen
                date=2026-05-26
                - 후보군 저장: 10개
                - 추천 생성: 3건

                추천 IDs: [1, 2, 3]"""))
                .thenReturn(new NotificationLogService.NotificationDispatchResult(true, false, "hash"));

        NotificationLogService.NotificationDispatchResult result = service.sendSchedulerEvent(
                "krx-preopen",
                "success",
                "KRX PreOpen",
                LocalDate.of(2026, 5, 26),
                List.of(
                        new NotificationService.NotificationMetric("후보군 저장", "10개"),
                        new NotificationService.NotificationMetric("추천 생성", "3건")
                ),
                List.of("추천 IDs: [1, 2, 3]")
        );

        assertThat(result.sent()).isTrue();
        verify(notificationLogService).sendTelegramOnce("scheduler:krx-preopen:success:20260526", """
                KRX PreOpen
                date=2026-05-26
                - 후보군 저장: 10개
                - 추천 생성: 3건

                추천 IDs: [1, 2, 3]""");
    }

    @Test
    void sendSchedulerErrorKeepsOneKeyPerJobDateAndMessage() {
        NotificationService service = new NotificationService(notificationLogService);
        when(notificationLogService.sendTelegramOnce(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ERROR\njob=us-preopen\ndate=2026-05-26\nmessage=timeout")))
                .thenReturn(new NotificationLogService.NotificationDispatchResult(false, false, "hash"));

        service.sendSchedulerError("us-preopen", LocalDate.of(2026, 5, 26), "timeout");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationLogService).sendTelegramOnce(keyCaptor.capture(), org.mockito.ArgumentMatchers.eq("ERROR\njob=us-preopen\ndate=2026-05-26\nmessage=timeout"));
        assertThat(keyCaptor.getValue()).startsWith("scheduler:us-preopen:error:20260526:");
    }

    @Test
    void formatPaperTradingSummaryShowsPortfolioSummaryAndRiskPositions() {
        NotificationService service = new NotificationService(notificationLogService);
        StatsPaperTradingResponse response = new StatsPaperTradingResponse(
                3,
                2,
                BigDecimal.valueOf(-1.234),
                BigDecimal.valueOf(-0.456),
                BigDecimal.valueOf(30),
                1,
                1,
                List.of(
                        position("BBB", "STOP_TOUCHED", "-8.25", "10.00", "-2.00"),
                        position("AAA", "OPEN", "3.50", "20.00", "5.00"),
                        new PaperPosition(3L, "CCC", "NASDAQ", "SHORT", BigDecimal.TEN, null, null,
                                BigDecimal.valueOf(12), BigDecimal.valueOf(9), 70, BigDecimal.valueOf(20),
                                null, null, null, null, "NO_PRICE")
                )
        );

        String summary = service.formatPaperTradingSummary(response);

        assertThat(summary).isEqualTo("""
                페이퍼트레이딩
                OPEN 추천: 3건 · 가격 확인: 2건
                평균 손익: -1.23% · 비중 손익: -0.46%
                총 비중: 30.00% · 목표/손절: 1/1건
                리스크 체크
                BBB -8.25% · 비중 10.00% · stop -2.00%
                AAA +3.50% · 비중 20.00% · stop +5.00%""");
    }

    private PaperPosition position(String ticker, String priceStatus, String pnlPct, String weightPct, String stopDistancePct) {
        return new PaperPosition(
                1L,
                ticker,
                "NASDAQ",
                "SHORT",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                LocalDate.of(2026, 5, 29),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(95),
                70,
                new BigDecimal(weightPct),
                new BigDecimal(pnlPct),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                new BigDecimal(stopDistancePct),
                priceStatus
        );
    }
}
