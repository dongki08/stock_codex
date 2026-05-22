-- V7__market_universe_delisted_at.sql : 상장폐지일 보관으로 생존편향 보정 준비

IF COL_LENGTH('market_universe', 'delisted_at') IS NULL
ALTER TABLE market_universe
ADD delisted_at date NULL;

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_market_universe_tradable_delisted'
      AND object_id = OBJECT_ID('market_universe')
)
CREATE INDEX IX_market_universe_tradable_delisted
ON market_universe (market, tradable, delisted_at);
