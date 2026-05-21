-- V2__price_history.sql : 가격 히스토리 저장소 추가 (MSSQL)
-- 추천 feature와 백테스트가 함께 참조할 일봉/장중 가격 테이블을 만든다.

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='price_daily' AND xtype='U')
CREATE TABLE price_daily (
    price_key    nvarchar(80)   NOT NULL,
    ticker       nvarchar(20)   NOT NULL,
    market       nvarchar(20)   NOT NULL,
    trade_date   date           NOT NULL,
    open_price   numeric(24,4)  NOT NULL,
    high_price   numeric(24,4)  NOT NULL,
    low_price    numeric(24,4)  NOT NULL,
    close_price  numeric(24,4)  NOT NULL,
    volume       numeric(24,4)  NOT NULL,
    turnover     numeric(24,4)  NULL,
    source       nvarchar(50)   NOT NULL,
    created_at   datetime2      NOT NULL,
    updated_at   datetime2      NOT NULL,
    CONSTRAINT PK_price_daily PRIMARY KEY (price_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_price_daily_market_ticker_date'
      AND object_id = OBJECT_ID('price_daily')
)
CREATE INDEX IX_price_daily_market_ticker_date
ON price_daily (market, ticker, trade_date DESC);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='price_intraday' AND xtype='U')
CREATE TABLE price_intraday (
    price_key    nvarchar(100)  NOT NULL,
    ticker       nvarchar(20)   NOT NULL,
    market       nvarchar(20)   NOT NULL,
    tick_at      datetime2      NOT NULL,
    price        numeric(24,4)  NOT NULL,
    volume       numeric(24,4)  NULL,
    source       nvarchar(50)   NOT NULL,
    created_at   datetime2      NOT NULL,
    updated_at   datetime2      NOT NULL,
    CONSTRAINT PK_price_intraday PRIMARY KEY (price_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_price_intraday_market_ticker_tick'
      AND object_id = OBJECT_ID('price_intraday')
)
CREATE INDEX IX_price_intraday_market_ticker_tick
ON price_intraday (market, ticker, tick_at DESC);
