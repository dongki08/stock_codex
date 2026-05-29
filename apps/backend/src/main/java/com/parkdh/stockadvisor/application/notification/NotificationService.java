package com.parkdh.stockadvisor.application.notification;

import com.parkdh.stockadvisor.application.notification.NotificationLogService.NotificationDispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Service
public class NotificationService {
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final NotificationLogService notificationLogService;

    public NotificationDispatchResult sendTelegramOnce(String dedupeKey, String message) {
        return notificationLogService.sendTelegramOnce(dedupeKey, HtmlUtils.htmlEscape(message));
    }

    public NotificationDispatchResult sendSchedulerMessage(String jobName, String eventType, LocalDate eventDate, String message) {
        return sendTelegramOnce(buildSchedulerEventKey(jobName, eventType, eventDate), message);
    }

    public NotificationDispatchResult sendSchedulerEvent(
            String jobName,
            String eventType,
            String title,
            LocalDate eventDate,
            List<NotificationMetric> metrics,
            List<String> details
    ) {
        return sendSchedulerMessage(jobName, eventType, eventDate, formatSchedulerEvent(title, eventDate, metrics, details));
    }

    public NotificationDispatchResult sendSchedulerError(String jobName, LocalDate eventDate, String errorMessage) {
        String safeMessage = errorMessage == null || errorMessage.isBlank() ? "unknown" : errorMessage;
        String dedupeKey = buildSchedulerEventKey(jobName, "error", eventDate) + ":" + Integer.toHexString(safeMessage.hashCode());
        return sendTelegramOnce(dedupeKey, "ERROR\njob=" + jobName + "\ndate=" + eventDate + "\nmessage=" + safeMessage);
    }

    public String formatSchedulerEvent(String title, LocalDate eventDate, List<NotificationMetric> metrics, List<String> details) {
        StringBuilder builder = new StringBuilder()
                .append(title)
                .append("\n")
                .append("date=")
                .append(eventDate);
        for (NotificationMetric metric : metrics) {
            builder.append("\n- ").append(metric.label()).append(": ").append(metric.value());
        }
        if (!details.isEmpty()) {
            builder.append("\n\n").append(String.join("\n", details));
        }
        return builder.toString();
    }

    private String buildSchedulerEventKey(String jobName, String eventType, LocalDate eventDate) {
        return "scheduler:%s:%s:%s".formatted(jobName, eventType, eventDate.format(DATE_KEY_FORMATTER));
    }

    public record NotificationMetric(String label, String value) {
    }
}
