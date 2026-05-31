package com.parkdh.stockadvisor.application.notification;

import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse;
import com.parkdh.stockadvisor.api.stats.dto.StatsPaperTradingResponse.PaperPosition;
import com.parkdh.stockadvisor.application.notification.NotificationLogService.NotificationDispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    public String formatPaperTradingSummary(StatsPaperTradingResponse paperTrading) {
        StringBuilder builder = new StringBuilder()
                .append("페이퍼트레이딩\n")
                .append("OPEN 추천: ")
                .append(paperTrading.openCount())
                .append("건 · 가격 확인: ")
                .append(paperTrading.pricedCount())
                .append("건\n")
                .append("평균 손익: ")
                .append(formatPercent(paperTrading.avgUnrealizedPnlPct()))
                .append(" · 비중 손익: ")
                .append(formatPercent(paperTrading.weightedUnrealizedPnlPct()))
                .append("\n")
                .append("총 비중: ")
                .append(formatUnsignedPercent(paperTrading.totalWeightPct()))
                .append(" · 목표/손절: ")
                .append(paperTrading.targetTouchCount())
                .append("/")
                .append(paperTrading.stopTouchCount())
                .append("건");

        List<PaperPosition> riskPositions = paperTrading.positions().stream()
                .filter(position -> position.unrealizedPnlPct() != null)
                .sorted(Comparator
                        .comparing(NotificationService::statusRank)
                        .thenComparing(position -> nullLast(position.distanceToStopPct()))
                        .thenComparing(PaperPosition::unrealizedPnlPct))
                .limit(5)
                .toList();
        if (!riskPositions.isEmpty()) {
            builder.append("\n리스크 체크");
            for (PaperPosition position : riskPositions) {
                builder.append("\n")
                        .append(position.ticker())
                        .append(" ")
                        .append(formatPercent(position.unrealizedPnlPct()))
                        .append(" · 비중 ")
                        .append(formatUnsignedPercent(position.positionWeightPct()))
                        .append(" · stop ")
                        .append(formatPercent(position.distanceToStopPct()));
            }
        }
        return builder.toString();
    }

    private String buildSchedulerEventKey(String jobName, String eventType, LocalDate eventDate) {
        return "scheduler:%s:%s:%s".formatted(jobName, eventType, eventDate.format(DATE_KEY_FORMATTER));
    }

    private static int statusRank(PaperPosition position) {
        return switch (position.priceStatus()) {
            case "STOP_TOUCHED" -> 0;
            case "OPEN" -> 1;
            case "TARGET_TOUCHED" -> 2;
            default -> 3;
        };
    }

    private static BigDecimal nullLast(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(Integer.MAX_VALUE) : value;
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return (value.signum() > 0 ? "+" : "") + value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private String formatUnsignedPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    public record NotificationMetric(String label, String value) {
    }
}
