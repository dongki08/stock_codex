package com.parkdh.stockadvisor.application.notification;

import com.parkdh.stockadvisor.domain.notification.NotificationLogEntity;
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient;
import com.parkdh.stockadvisor.infrastructure.notification.TelegramClient.TelegramSendResult;
import com.parkdh.stockadvisor.infrastructure.persistence.notification.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {
    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private TelegramClient telegramClient;

    private NotificationLogService notificationLogService;

    @BeforeEach
    void setUp() {
        notificationLogService = new NotificationLogService(notificationLogRepository, telegramClient);
    }

    @Test
    void sendTelegramOnceSkipsWhenSentPayloadAlreadyExists() {
        when(notificationLogRepository.existsByChannelAndPayloadHashAndStatus(eq("TELEGRAM"), any(String.class), eq("SENT"))).thenReturn(true);

        NotificationLogService.NotificationDispatchResult result = notificationLogService.sendTelegramOnce("same-key", "message");

        assertThat(result.duplicate()).isTrue();
        assertThat(result.sent()).isFalse();
        verify(telegramClient, never()).sendMessageWithResult(any(String.class));
        verify(notificationLogRepository, never()).save(any(NotificationLogEntity.class));
    }

    @Test
    void sendTelegramOnceSendsAndStoresSentLogWhenPayloadIsNew() {
        when(notificationLogRepository.existsByChannelAndPayloadHashAndStatus(eq("TELEGRAM"), any(String.class), eq("SENT"))).thenReturn(false);
        when(telegramClient.sendMessageWithResult("message")).thenReturn(new TelegramSendResult(true, false, 200, null));
        when(notificationLogRepository.save(any(NotificationLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationLogService.NotificationDispatchResult result = notificationLogService.sendTelegramOnce("new-key", "message");

        assertThat(result.sent()).isTrue();
        assertThat(result.duplicate()).isFalse();
        ArgumentCaptor<NotificationLogEntity> captor = ArgumentCaptor.forClass(NotificationLogEntity.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("TELEGRAM");
        assertThat(captor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(captor.getValue().getPayloadHash()).hasSize(64);
    }

    @Test
    void sendTelegramOnceStoresDetailedFailureReason() {
        when(notificationLogRepository.existsByChannelAndPayloadHashAndStatus(eq("TELEGRAM"), any(String.class), eq("SENT"))).thenReturn(false);
        when(telegramClient.sendMessageWithResult("message")).thenReturn(new TelegramSendResult(false, false, 401, "Telegram API returned HTTP 401: unauthorized"));
        when(notificationLogRepository.save(any(NotificationLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationLogService.NotificationDispatchResult result = notificationLogService.sendTelegramOnce("new-key", "message");

        assertThat(result.sent()).isFalse();
        ArgumentCaptor<NotificationLogEntity> captor = ArgumentCaptor.forClass(NotificationLogEntity.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getSentAt()).isNull();
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("Telegram API returned HTTP 401: unauthorized");
    }
}
