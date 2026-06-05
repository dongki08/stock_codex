UPDATE app_setting
SET value_json = '{"value":20}',
    description = N'시장별 뉴스 수집 후보 수',
    updated_by = 'system',
    updated_at = SYSDATETIME()
WHERE setting_key = 'collection.news.tickersPerMarket'
  AND value_json = '{"value":5}';
