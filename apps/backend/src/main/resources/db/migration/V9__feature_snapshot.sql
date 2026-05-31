-- V9__feature_snapshot.sql
-- TASK-2: feature_snapshot 테이블 (Point-in-time 피처 스냅샷, 미래참조 차단)
-- TASK-1: backtest.entry.minScore 기본값
-- TASK-8: recommendation.regime.filter.enabled 기본값 true

-- feature_snapshot 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='feature_snapshot' AND xtype='U')
CREATE TABLE feature_snapshot (
    snapshot_key   VARCHAR(80)    NOT NULL,
    market         NVARCHAR(20)   NOT NULL,
    ticker         NVARCHAR(20)   NOT NULL,
    as_of_date     DATE           NOT NULL,
    total_score    INT            NOT NULL,
    feature_json   NVARCHAR(MAX)  NOT NULL,
    fwd_ret_5d     DECIMAL(12,6)  NULL,
    fwd_ret_20d    DECIMAL(12,6)  NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_feature_snapshot PRIMARY KEY (snapshot_key)
);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='ix_feature_snapshot_date')
    CREATE INDEX ix_feature_snapshot_date ON feature_snapshot (as_of_date, market);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='ix_feature_snapshot_ticker')
    CREATE INDEX ix_feature_snapshot_ticker ON feature_snapshot (market, ticker, as_of_date);

-- TASK-8: regime 필터 기본값 활성화 (없으면 insert, 있으면 true로 update)
IF NOT EXISTS (SELECT 1 FROM app_setting WHERE setting_key = 'recommendation.regime.filter.enabled')
    INSERT INTO app_setting (setting_key, value_json, description, updated_by, created_at, updated_at)
    VALUES ('recommendation.regime.filter.enabled', '{"value":true}',
            '시장 레짐 필터 활성화 여부 (지수 200일선 기준)', 'migration-v9',
            SYSDATETIME(), SYSDATETIME());
ELSE
    UPDATE app_setting
    SET value_json = '{"value":true}', updated_at = SYSDATETIME(), updated_by = 'migration-v9'
    WHERE setting_key = 'recommendation.regime.filter.enabled';

-- TASK-1: 백테스트 score 진입 임계값
IF NOT EXISTS (SELECT 1 FROM app_setting WHERE setting_key = 'backtest.entry.minScore')
    INSERT INTO app_setting (setting_key, value_json, description, updated_by, created_at, updated_at)
    VALUES ('backtest.entry.minScore', '{"value":60}',
            '백테스트 score 기반 진입 최소 점수 (0-100)', 'migration-v9',
            SYSDATETIME(), SYSDATETIME());
