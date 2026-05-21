-- V6__fundamental_metric.sql : company fundamental metrics

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='fundamental_metric' AND xtype='U')
CREATE TABLE fundamental_metric (
    metric_key    nvarchar(120) NOT NULL,
    ticker        nvarchar(20)  NOT NULL,
    market        nvarchar(20)  NOT NULL,
    metric_name   nvarchar(80)  NOT NULL,
    metric_value  numeric(24,6) NULL,
    unit          nvarchar(30)  NULL,
    fiscal_year   int           NULL,
    fiscal_period nvarchar(10)  NULL,
    period_end    date          NULL,
    source        nvarchar(50)  NOT NULL,
    fetched_at    datetime2     NOT NULL,
    created_at    datetime2     NOT NULL,
    CONSTRAINT PK_fundamental_metric PRIMARY KEY (metric_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_fundamental_metric_market_ticker_period'
      AND object_id = OBJECT_ID('fundamental_metric')
)
CREATE INDEX IX_fundamental_metric_market_ticker_period
ON fundamental_metric (market, ticker, period_end DESC);
