IF NOT EXISTS (SELECT 1 FROM app_setting WHERE setting_key = 'collection.news.tickersPerMarket')
BEGIN
    INSERT INTO app_setting (setting_key, value_json, description, updated_by, created_at, updated_at)
    VALUES ('collection.news.tickersPerMarket', '{"value":20}', N'시장별 뉴스 수집 후보 수', 'migration-v12', SYSDATETIME(), SYSDATETIME());
END
ELSE
BEGIN
    UPDATE app_setting
    SET value_json = '{"value":20}',
        description = N'시장별 뉴스 수집 후보 수',
        updated_by = 'migration-v12',
        updated_at = SYSDATETIME()
    WHERE setting_key = 'collection.news.tickersPerMarket'
      AND value_json = '{"value":5}';
END
