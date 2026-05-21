-- V3__exit_confirm_log.sql : Exit Confirm 자동 호출 이력과 알림 중복 방지 인덱스

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='exit_confirm_log' AND xtype='U')
CREATE TABLE exit_confirm_log (
    id                bigint IDENTITY(1,1) NOT NULL,
    recommendation_id bigint        NOT NULL,
    ticker            nvarchar(20)  NOT NULL,
    market            nvarchar(20)  NOT NULL,
    current_price     numeric(18,4) NULL,
    stop_price        numeric(18,4) NULL,
    distance_pct      numeric(10,4) NULL,
    action            nvarchar(20)  NOT NULL,
    used_fallback     bit           NOT NULL,
    codex_error       nvarchar(max) NULL,
    notified          bit           NOT NULL,
    notify_key        nvarchar(120) NULL,
    confirmed_at      datetime2     NOT NULL,
    created_at        datetime2     NOT NULL,
    CONSTRAINT PK_exit_confirm_log PRIMARY KEY (id)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_exit_confirm_log_market_ticker_confirmed'
      AND object_id = OBJECT_ID('exit_confirm_log')
)
CREATE INDEX IX_exit_confirm_log_market_ticker_confirmed
ON exit_confirm_log (market, ticker, confirmed_at DESC);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_exit_confirm_log_recommendation_confirmed'
      AND object_id = OBJECT_ID('exit_confirm_log')
)
CREATE INDEX IX_exit_confirm_log_recommendation_confirmed
ON exit_confirm_log (recommendation_id, confirmed_at DESC);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='UX_exit_confirm_log_notify_key'
      AND object_id = OBJECT_ID('exit_confirm_log')
)
CREATE UNIQUE INDEX UX_exit_confirm_log_notify_key
ON exit_confirm_log (notify_key)
WHERE notify_key IS NOT NULL;

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_notification_log_channel_payload_status'
      AND object_id = OBJECT_ID('notification_log')
)
CREATE INDEX IX_notification_log_channel_payload_status
ON notification_log (channel, payload_hash, status);
