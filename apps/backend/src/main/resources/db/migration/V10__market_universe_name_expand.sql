-- V10__market_universe_name_expand.sql : market_universe.name 컬럼을 nvarchar(500)으로 확장
-- 미국 주식 종목명이 200자를 초과하는 경우가 있어 확장한다.
ALTER TABLE market_universe
    ALTER COLUMN name nvarchar(500) NOT NULL;
