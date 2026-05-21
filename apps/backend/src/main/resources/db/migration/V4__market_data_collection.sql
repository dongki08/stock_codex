-- V4__market_data_collection.sql : news, disclosure, macro collection tables

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='news_article' AND xtype='U')
CREATE TABLE news_article (
    article_key     nvarchar(80)   NOT NULL,
    ticker          nvarchar(20)   NULL,
    market          nvarchar(20)   NULL,
    title           nvarchar(500)  NOT NULL,
    url             nvarchar(1000) NOT NULL,
    source          nvarchar(50)   NOT NULL,
    published_at    datetime2      NULL,
    summary         nvarchar(max)  NULL,
    sentiment_score numeric(6,3)   NULL,
    created_at      datetime2      NOT NULL,
    CONSTRAINT PK_news_article PRIMARY KEY (article_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_news_article_market_ticker_published'
      AND object_id = OBJECT_ID('news_article')
)
CREATE INDEX IX_news_article_market_ticker_published
ON news_article (market, ticker, published_at DESC);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='disclosure_event' AND xtype='U')
CREATE TABLE disclosure_event (
    disclosure_key  nvarchar(100)  NOT NULL,
    ticker          nvarchar(20)   NULL,
    market          nvarchar(20)   NULL,
    title           nvarchar(500)  NOT NULL,
    url             nvarchar(1000) NULL,
    source          nvarchar(50)   NOT NULL,
    disclosure_type nvarchar(60)   NULL,
    disclosed_at    datetime2      NULL,
    raw_json        nvarchar(max)  NULL,
    created_at      datetime2      NOT NULL,
    CONSTRAINT PK_disclosure_event PRIMARY KEY (disclosure_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_disclosure_event_market_ticker_disclosed'
      AND object_id = OBJECT_ID('disclosure_event')
)
CREATE INDEX IX_disclosure_event_market_ticker_disclosed
ON disclosure_event (market, ticker, disclosed_at DESC);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='macro_observation' AND xtype='U')
CREATE TABLE macro_observation (
    observation_key nvarchar(100) NOT NULL,
    series_id       nvarchar(40)  NOT NULL,
    series_name     nvarchar(200) NOT NULL,
    observed_date   date          NOT NULL,
    observed_value  numeric(24,6) NULL,
    source          nvarchar(50)  NOT NULL,
    fetched_at      datetime2     NOT NULL,
    created_at      datetime2     NOT NULL,
    CONSTRAINT PK_macro_observation PRIMARY KEY (observation_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_macro_observation_series_date'
      AND object_id = OBJECT_ID('macro_observation')
)
CREATE INDEX IX_macro_observation_series_date
ON macro_observation (series_id, observed_date DESC);
