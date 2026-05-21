-- V5__market_signal_scores.sql : disclosure importance score for rule-based market signals

IF COL_LENGTH('disclosure_event', 'importance_score') IS NULL
ALTER TABLE disclosure_event
ADD importance_score int NULL;

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name='IX_disclosure_event_importance'
      AND object_id = OBJECT_ID('disclosure_event')
)
CREATE INDEX IX_disclosure_event_importance
ON disclosure_event (importance_score DESC);
