-- V1__init_schema.sql : stock-advisor 초기 스키마 (MSSQL)
-- IF NOT EXISTS 가드 사용 → 기존 DB에서도 안전하게 실행 가능

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='app_setting' AND xtype='U')
CREATE TABLE app_setting (
    setting_key  nvarchar(60)   NOT NULL,
    value_json   nvarchar(max)  NOT NULL,
    description  nvarchar(500)  NOT NULL,
    updated_by   nvarchar(40)   NOT NULL,
    created_at   datetime2      NOT NULL,
    updated_at   datetime2      NOT NULL,
    CONSTRAINT PK_app_setting PRIMARY KEY (setting_key)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='audit_log' AND xtype='U')
CREATE TABLE audit_log (
    id          bigint IDENTITY(1,1) NOT NULL,
    actor       nvarchar(40)   NOT NULL,
    action      nvarchar(60)   NOT NULL,
    before_json nvarchar(max)  NOT NULL,
    after_json  nvarchar(max)  NOT NULL,
    created_at  datetime2      NOT NULL,
    CONSTRAINT PK_audit_log PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='instrument' AND xtype='U')
CREATE TABLE instrument (
    ticker     nvarchar(20)   NOT NULL,
    market     nvarchar(20)   NOT NULL,
    name       nvarchar(200)  NOT NULL,
    sector     nvarchar(100)  NULL,
    enabled    bit            NOT NULL,
    created_at datetime2      NOT NULL,
    updated_at datetime2      NOT NULL,
    CONSTRAINT PK_instrument PRIMARY KEY (ticker)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='recommendation' AND xtype='U')
CREATE TABLE recommendation (
    id               bigint IDENTITY(1,1) NOT NULL,
    ticker           nvarchar(20)   NOT NULL,
    market           nvarchar(20)   NOT NULL,
    term             nvarchar(10)   NOT NULL,
    entry_price      numeric(18,4)  NOT NULL,
    target_price     numeric(18,4)  NOT NULL,
    stop_price       numeric(18,4)  NOT NULL,
    expected_exit_at date           NOT NULL,
    confidence       int            NOT NULL,
    signals_json     nvarchar(max)  NOT NULL,
    model_version    nvarchar(40)   NOT NULL,
    generated_at     datetime2      NOT NULL,
    status           nvarchar(20)   NOT NULL,
    created_at       datetime2      NOT NULL,
    updated_at       datetime2      NOT NULL,
    CONSTRAINT PK_recommendation PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='evaluation' AND xtype='U')
CREATE TABLE evaluation (
    id                bigint IDENTITY(1,1) NOT NULL,
    recommendation_id bigint         NOT NULL,
    actual_exit_price numeric(18,4)  NULL,
    exit_reason       nvarchar(20)   NOT NULL,
    pnl_pct           numeric(8,4)   NOT NULL,
    drawdown_pct      numeric(8,4)   NULL,
    hit_target        bit            NOT NULL,
    evaluated_at      datetime2      NOT NULL,
    created_at        datetime2      NOT NULL,
    CONSTRAINT PK_evaluation PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='prediction' AND xtype='U')
CREATE TABLE prediction (
    id              bigint IDENTITY(1,1) NOT NULL,
    ticker          nvarchar(20)   NOT NULL,
    horizon_days    int            NOT NULL,
    predicted_price numeric(18,4)  NOT NULL,
    model_version   nvarchar(40)   NOT NULL,
    generated_at    datetime2      NOT NULL,
    created_at      datetime2      NOT NULL,
    CONSTRAINT PK_prediction PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='backtest_run' AND xtype='U')
CREATE TABLE backtest_run (
    id           bigint IDENTITY(1,1) NOT NULL,
    strategy     nvarchar(100)  NOT NULL,
    period_from  date           NOT NULL,
    period_to    date           NOT NULL,
    metrics_json nvarchar(max)  NOT NULL,
    created_at   datetime2      NOT NULL,
    CONSTRAINT PK_backtest_run PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='notification_log' AND xtype='U')
CREATE TABLE notification_log (
    id            bigint IDENTITY(1,1) NOT NULL,
    channel       nvarchar(30)   NOT NULL,
    payload_hash  nvarchar(64)   NOT NULL,
    sent_at       datetime2      NULL,
    status        nvarchar(20)   NOT NULL,
    error_message nvarchar(max)  NULL,
    created_at    datetime2      NOT NULL,
    CONSTRAINT PK_notification_log PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='codex_call' AND xtype='U')
CREATE TABLE codex_call (
    id              bigint IDENTITY(1,1) NOT NULL,
    caller          nvarchar(40)   NOT NULL,
    prompt_hash     nvarchar(64)   NOT NULL,
    prompt_len      int            NOT NULL,
    response_len    int            NULL,
    tools_used_json nvarchar(max)  NULL,
    duration_ms     int            NULL,
    succeeded       bit            NOT NULL,
    error_message   nvarchar(max)  NULL,
    called_at       datetime2      NOT NULL,
    created_at      datetime2      NOT NULL,
    CONSTRAINT PK_codex_call PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='daily_brief' AND xtype='U')
CREATE TABLE daily_brief (
    id                  bigint IDENTITY(1,1) NOT NULL,
    market_track        nvarchar(20)   NOT NULL,
    brief_md            nvarchar(max)  NOT NULL,
    draft_no            int            NOT NULL,
    coverage            numeric(4,3)   NULL,
    hallucination_flags int            NULL,
    llm_model           nvarchar(40)   NULL,
    generated_at        datetime2      NOT NULL,
    created_at          datetime2      NOT NULL,
    CONSTRAINT PK_daily_brief PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='autoresearch_run' AND xtype='U')
CREATE TABLE autoresearch_run (
    id              bigint IDENTITY(1,1) NOT NULL,
    job_run_id      uniqueidentifier NOT NULL,
    iter_no         int              NOT NULL,
    parent_sha      nvarchar(64)     NULL,
    proposal_sha    nvarchar(64)     NULL,
    diff_summary    nvarchar(max)    NULL,
    metric_name     nvarchar(40)     NULL,
    metric_value    numeric(10,4)    NULL,
    champion_metric numeric(10,4)    NULL,
    decision        nvarchar(10)     NOT NULL,
    duration_ms     int              NULL,
    started_at      datetime2        NULL,
    ended_at        datetime2        NULL,
    created_at      datetime2        NOT NULL,
    CONSTRAINT PK_autoresearch_run PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='strategy_version' AND xtype='U')
CREATE TABLE strategy_version (
    id           bigint IDENTITY(1,1) NOT NULL,
    semver       nvarchar(20)   NOT NULL,
    git_sha      nvarchar(64)   NOT NULL,
    metric_value numeric(10,4)  NOT NULL,
    promoted_at  datetime2      NOT NULL,
    is_champion  bit            NOT NULL,
    created_at   datetime2      NOT NULL,
    CONSTRAINT PK_strategy_version PRIMARY KEY (id)
);

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='market_universe' AND xtype='U')
CREATE TABLE market_universe (
    universe_key   nvarchar(60)   NOT NULL,
    ticker         nvarchar(20)   NOT NULL,
    market         nvarchar(20)   NOT NULL,
    name           nvarchar(200)  NOT NULL,
    sector         nvarchar(100)  NULL,
    market_cap     numeric(24,4)  NULL,
    avg_turnover   numeric(24,4)  NULL,
    last_price     numeric(24,4)  NULL,
    tradable       bit            NOT NULL,
    source         nvarchar(50)   NOT NULL,
    last_synced_at date           NULL,
    created_at     datetime2      NOT NULL,
    updated_at     datetime2      NOT NULL,
    CONSTRAINT PK_market_universe PRIMARY KEY (universe_key)
);
