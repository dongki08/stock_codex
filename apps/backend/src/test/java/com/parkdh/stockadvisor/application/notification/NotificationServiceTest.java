package com.parkdh.stockadvisor.application.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
