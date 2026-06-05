IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='context_relation_analysis' AND xtype='U')
CREATE TABLE context_relation_analysis (
    analysis_key         nvarchar(80)   NOT NULL,
    ticker               nvarchar(20)   NOT NULL,
    market               nvarchar(20)   NOT NULL,
    analysis_date        date           NOT NULL,
    direction            nvarchar(12)   NOT NULL,
    confidence           int            NOT NULL,
    risk_level           nvarchar(12)   NOT NULL,
    relation_score       int            NOT NULL,
    summary              nvarchar(1000) NOT NULL,
    key_factors_json     nvarchar(max)  NOT NULL,
    contradictions_json  nvarchar(max)  NOT NULL,
    model                nvarchar(50)   NOT NULL,
    analyzed_at          datetime2      NOT NULL,
    created_at           datetime2      NOT NULL,
    updated_at           datetime2      NOT NULL,
    CONSTRAINT PK_context_relation_analysis PRIMARY KEY (analysis_key)
);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_context_relation_analysis_market_ticker_date'
      AND object_id = OBJECT_ID('context_relation_analysis')
)
CREATE INDEX IX_context_relation_analysis_market_ticker_date
ON context_relation_analysis (market, ticker, analysis_date DESC);

IF NOT EXISTS (SELECT 1 FROM app_setting WHERE setting_key = 'context.relation.codex.enabled')
INSERT INTO app_setting (setting_key, value_json, description, updated_by, created_at, updated_at)
VALUES ('context.relation.codex.enabled', '{"value":true}', N'뉴스·공시 관계 Codex 분석 활성화 여부', 'migration-v13', SYSDATETIME(), SYSDATETIME());
