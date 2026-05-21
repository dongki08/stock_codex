package com.parkdh.stockadvisor.domain.recommendation;

import com.parkdh.stockadvisor.domain.common.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "exit_confirm_log")
public class ExitConfirmLogEntity extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Exit Confirm 로그 ID")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("추천 ID")
    @Column(name = "recommendation_id", nullable = false)
    private Long recommendationId;

    @Comment("종목 코드")
    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Comment("시장 구분")
    @Column(name = "market", length = 20, nullable = false)
    private String market;

    @Comment("판단 기준 현재가")
    @Column(name = "current_price", precision = 18, scale = 4)
    private BigDecimal currentPrice;

    @Comment("손절가")
    @Column(name = "stop_price", precision = 18, scale = 4)
    private BigDecimal stopPrice;

    @Comment("손절가 대비 이격률")
    @Column(name = "distance_pct", precision = 10, scale = 4)
    private BigDecimal distancePct;

    @Comment("판단 액션")
    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Comment("fallback 사용 여부")
    @Column(name = "used_fallback", nullable = false)
    private Boolean usedFallback;

    @Comment("Codex 오류 메시지")
    @Column(name = "codex_error", columnDefinition = "nvarchar(max)")
    private String codexError;

    @Comment("알림 발송 여부")
    @Column(name = "notified", nullable = false)
    private Boolean notified;

    @Comment("알림 중복 방지 키")
    @Column(name = "notify_key", length = 120)
    private String notifyKey;

    @Comment("판단 일시")
    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    public ExitConfirmLogEntity(Long recommendationId, String ticker, String market, BigDecimal currentPrice, BigDecimal stopPrice,
                                BigDecimal distancePct, String action, Boolean usedFallback, String codexError,
                                Boolean notified, String notifyKey, LocalDateTime confirmedAt) {
        this.recommendationId = recommendationId;
        this.ticker = ticker;
        this.market = market;
        this.currentPrice = currentPrice;
        this.stopPrice = stopPrice;
        this.distancePct = distancePct;
        this.action = action;
        this.usedFallback = usedFallback;
        this.codexError = codexError;
        this.notified = notified;
        this.notifyKey = notifyKey;
        this.confirmedAt = confirmedAt;
    }
}
