# 작업 일지 (WORKLOG)

> 📌 **에이전트 규칙**: 작업(코드/문서 변경)을 마칠 때마다 이 파일 **맨 위**(최신이 위)에 한 블록 추가. 사용자가 매번 시키지 않아도 자동으로.
> - append-only 누적 일지. 단일출처 규칙 예외 — 막 쌓아도 됨.
> - 큰 구조/기능 변화면 [11-ROADMAP.md](11-ROADMAP.md) §0 델타에도 승격.
> - 한 달 넘게 쌓이면 월별로 잘라 [90-archive/](90-archive/)로 이동.

## 블록 형식

```
### YYYY-MM-DD — <한 줄 제목>
- 무엇: <한 일>
- 왜: <이유/배경> (자명하면 생략)
- 변경: `파일/영역` (핵심만)
- 후속: <남은 일/주의> (있으면)
```

---

### 2026-06-02 — 추천 알림 아이콘 포맷 적용
- 무엇: 장전 추천 Telegram 메시지에 시장/진입/목표/손절 아이콘을 적용. 운영 스케줄러 KRX/US 알림 상세도 `🇰🇷/🇺🇸`, `🟢 진입`, `🎯 목표`, `🛑 손절` 줄바꿈 포맷으로 정렬. 같은 날 중복 추천 재사용 시에도 회사명이 빠지지 않도록 `DevRecommendationGenerateService`의 기존 추천 응답에 `market_universe` 회사명을 매핑.
- 왜: 티커·가격이 한 줄에 몰리면 모바일 Telegram에서 읽기 어려워, 장전 30분 전 실전 알림을 빠르게 스캔할 수 있게 하기 위함.
- 변경: `KrxPreOpenJob`, `UsPreOpenJob`, `DevRecommendationGenerateService`, `DevRecommendationGenerateServiceTest`.
- 검증: 아이콘 포함 테스트 메시지 `messageId=48` 전송 성공. `apps/backend` `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.

### 2026-06-02 — 프리오픈 추천 알림 회사명 포함
- 무엇: US 프리오픈 Telegram 알림 상세를 `추천 IDs` 대신 `[미장] 티커 회사명 단기/장기 | 진입 → 목표 | 손절` 라인으로 보내도록 변경. KRX는 이미 회사명 포함 포맷이어서 동일한 UX로 정렬.
- 왜: 티커(`000100`, `AAOI`)만으로는 사용자가 실제 회사명을 바로 알기 어렵기 때문.
- 변경: `UsPreOpenJob`.
- 검증: 회사명 포함 강제 테스트 메시지 `messageId=45` 전송 성공. `apps/backend` `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.

### 2026-06-02 — 일봉 동기화 증분화 + 장후 백필 분리
- 무엇: `MarketDataSyncService` 일봉 동기화를 DB 최신 거래일 기준 증분 수집으로 변경. 이미 `targetDate`까지 있는 종목은 외부 API 조회 없이 스킵하고, 데이터가 없거나 오래된 종목은 수동/백필 모드에서만 bootstrap한다. KRX/US 프리오픈은 `syncDailyPricesForPreOpen`으로 전환해 추천 직전 대량 일봉 조회를 막고, `DailyPriceBackfillJob`을 추가해 KRX 18:10 / US 07:20에 비어 있거나 오래된 히스토리를 장후 보강한다.
- 왜: KIS 일봉 조회가 종목당 rate limit을 타서 30~50개 후보를 매번 전량 조회하면 장전 플로우가 과도하게 느려지고, DB에 이미 최신 데이터가 있는 종목도 중복 조회하는 비효율이 있었기 때문.
- 변경: `MarketDataSyncService`, `PriceDailySyncResponse`, `KrxPreOpenJob`, `UsPreOpenJob`, 신규 `DailyPriceBackfillJob`, 신규/갱신 테스트 2종, 프론트 `PriceDailySyncResult`/일봉 동기화 메시지, `docs/11-ROADMAP.md`, `docs/30-BACKEND-OVERVIEW.md`, `docs/31-BACKEND-DEEP-DIVE.md`.
- 검증: `apps/backend` `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL, `apps/web` `npm.cmd run build` 성공, `git diff --check` 통과.
- 후속: 실제 운영 DB에서 백필 1회 실행 후 `requestedTickerCount`/`skippedUpToDateCount` 비율을 보고 KRX/US limit 기본값을 조정.

### 2026-06-02 — 프로젝트 구조 문서·설정 정합성 정리
- 무엇: 프로젝트 구조 점검 결과의 stale 항목을 실제 코드 기준으로 정리. `AGENTS.md`/`CLAUDE.md`를 동일 내용으로 맞추고 Flyway 최신 V10, AutoResearch 루프 유효화 상태, API 경로(`/api/features/universe`, `/api/codex/calls`, `/api/notifications/logs`)를 현행 문서에 반영. `application-local.yml.example`의 `ddl-auto`를 `validate`로 정정. 프론트 API 응답 타입을 `api/result.ts`로 중앙화.
- 왜: 다음 에이전트/사용자가 오래된 빌드 리스크, V8 기준, 제거된 ExitConfirm 흐름, 중복된 `ApiResult` 타입을 기준으로 작업하지 않게 하기 위함.
- 변경: `AGENTS.md`, `CLAUDE.md`, `docs/00-INDEX.md`, `docs/11-ROADMAP.md`, `docs/12-SETUP-CHECKLIST.md`, `docs/30-BACKEND-OVERVIEW.md`, `docs/31-BACKEND-DEEP-DIVE.md`, `docs/40-RETURN-STRATEGY.md`, `docs/41-DEFECTS-AND-FIXES.md`, `application-local.yml.example`, `apps/web/src/api/*`, `packages/shared-types/src/api.d.ts`.
- 검증: `gradlew test` BUILD SUCCESSFUL, `npm run build` BUILD SUCCESSFUL.

### 2026-06-02 — 추천 중복 저장 방지 + KRX 데이터 수집 기간 확대
- 무엇: (1) `DevRecommendationGenerateService.generate` 진입부에 당일 중복 가드 추가 — 같은 모델버전으로 당일 추천이 이미 있으면 새 레코드 없이 기존 결과 반환. (2) `KrxPreOpenJob` 일봉 수집을 30일→120일로 확대, KOSDAQ도 함께 수집.
- 왜: 수동 트리거 여러 번 호출 시 동일 종목이 DB에 무한 중복 저장되는 문제 제거. 30일치만 수집하면 MA20·RSI14 등 히스토리 기반 지표가 일부 종목에서 계산 불가 → 필터 탈락 → 추천 2종목만 나오는 문제 해소.
- 변경: `DevRecommendationGenerateService`(당일 중복 가드), `KrxPreOpenJob`(120일, KOSDAQ 추가).
- 검증: `gradlew test` BUILD SUCCESSFUL.

### 2026-06-02 — 백테스트 진입 점수 조회: feature_snapshot 우선, buildFeatureAsOf 폴백
- 무엇: `BacktestRunService.simulateTicker`의 진입 점수 조회를 `resolveScore`로 추출. `feature_snapshot` 테이블에 해당 날짜 스냅샷 있으면 즉시 반환, 없으면 `buildFeatureAsOf` 재계산(폴백).
- 왜: `FeatureSnapshotJob`이 매일 totalScore를 저장하는데 백테스트가 그걸 무시하고 raw DB 다회 조회로 재계산하는 중복 제거. 스냅샷 적재 후엔 종목×일수만큼 발생하던 price_daily+news+disclosure+macro+fundamental 다회 조회 → 단건 PK 조회로 대체. IC로 검증된 실제 그날 점수를 그대로 써서 일관성 향상.
- 변경: `BacktestRunService`(FeatureSnapshotRepository 주입, `resolveScore` 헬퍼), `BacktestRunServiceTest`(mock 추가, 스냅샷 없음 스텁).
- 검증: `gradlew test` 전체 BUILD SUCCESSFUL.

### 2026-06-02 — 루프 인과 회귀 테스트 2종 추가
- 무엇: (1) `UniverseFeatureBuilderTest.buildFeatureAsOfScoreRespondsToWeightChanges` — 같은 가격 데이터에 가중치만 technical-heavy↔liquidity-heavy로 바꾸면 `buildFeatureAsOf().totalScore()`가 달라짐을 단언(키스톤 인과 가드). (2) `AutoresearchServiceTest.runAutoResearchUsesCodexProposalWhenEnabled` — `autoresearch.codex.enabled=true` + Codex 유효 JSON 응답 시 후보가 codex 경로로 생성됨을(`diffSummary` "codex-weights proposal") 단언.
- 왜: 두 변경(백테스트↔점수기 연결, Codex 전략가)이 향후 리팩토링에 조용히 끊기지 않도록 회귀 고정.
- 변경: `UniverseFeatureBuilderTest`, `AutoresearchServiceTest`.
- 검증: `gradlew test` 전체 BUILD SUCCESSFUL.

### 2026-06-02 — Codex를 AutoResearch 가중치 전략가로 결합
- 무엇: AutoResearch 반복에서 가중치 후보 생성을 Codex 제안 우선으로 전환. `autoresearch.codex.enabled=true`면 Codex에 [현재 가중치 + 피처 IC 리포트 + 챔피언 metric + variant 번호]를 주고 가중치 제안 JSON을 받음 → 기존 키만 병합 → 그룹 합=1 정규화 → **백테스트가 심판(metric 개선 시만 KEEP)**.
- 왜: Codex가 데일리브리프(서술)에만 쓰여 의사결정 루프 밖이었음. 루프가 인과적으로 작동하게 된 지금, Codex를 그 루프의 제안기로 투입. 생성(LLM)+검증(백테스트) 패턴이라 나쁜 제안은 자동 DISCARD.
- 안전장치: Codex 미설정/실패/타임아웃/예산초과/JSON파싱실패 → `Optional.empty()` → 기존 IC 변형(`mutateWeights`)으로 자동 폴백. 키 추가/삭제 금지(기존 키 위에만 숫자 병합), 음수 0 클램프, 정규화 유지. 기본값 `enabled=false`(옵트인).
- 변경: `AutoresearchService`(CodexClient 주입, 루프 Codex 분기, `codexProposeWeights`/`buildWeightsPrompt`/`extractJsonObject`/`mergeWeightGroup`/`readBooleanSetting` 추가), `AutoresearchServiceTest`(CodexClient mock).
- 검증: `gradlew test` 전체 BUILD SUCCESSFUL. 기본 비활성이라 기존 테스트 동작 불변.
- 설정: 켜기 = `PUT /api/admin/settings/autoresearch.codex.enabled` body `{"value":true}`. Codex CLI(`CODEX_COMMAND`) 미설정이면 켜도 폴백 동작.
- 후속: (1) variant 다양성은 프롬프트 힌트 의존 — 동일 IC 입력에 유사 제안 가능성, 결과 모니터링 필요. (2) 반복당 1회 호출(기본 8회/실행) — `codex.daily.callLimit`/`budgetUsd`로 통제됨. (3) Codex 제안 채택률/개선폭을 `autoresearch_run.diff_summary`("codex-weights proposal #N")로 추적 가능.

### 2026-06-02 — AutoResearch 루프 유효화 (백테스트↔라이브 점수기 연결)
- 무엇: 백테스트 진입 점수를 하드코딩 `computePriceScore`(MA/RSI/Vol 0.35/0.35/0.30)에서 라이브 추천 점수기 `UniverseFeatureBuilder.buildFeatureAsOf().totalScore()`로 교체.
- 왜: **진짜 결함 발견** — 백테스트가 `recommendation.scoring.weights`를 안 읽어, AutoResearch가 가중치를 변형해도 `avgPnlPct`가 불변 → 챔피언 승격이 노이즈 기반(루프 무효). 기존 CLAUDE.md 배너의 "미래참조 → 무효"는 오진(미래참조는 이미 차단돼 있었음). 실제 원인은 **최적화 대상(가중치)과 측정 함수의 분리**.
- 변경: `BacktestRunService`(UniverseFeatureBuilder·MarketUniverseRepository 주입, `simulateTicker` 진입 점수 교체, 죽은 `computePriceScore`/`averageClose` 제거), `BacktestRunServiceTest`(생성자·`buildFeatureAsOf` 스텁). WIP 깨짐 동반 수정: `PricePredictorTest`·`DevRecommendationGenerateServiceTest`에 `RecommendationCandidate.name` 인자 보강.
- 검증: `gradlew test` 전체 BUILD SUCCESSFUL. `buildFeatureAsOf`가 `resolveScoringWeights()`로 현재 가중치를 읽으므로 AutoResearch가 `saveWeights(proposal)` 후 평가 시 metric이 가중치에 반응 → 루프 작동.
- 후속: (1) raw asOf 점수 사용 — 라이브의 cross-sectional 표준화(TASK-5) 미반영, 완전충실 replay는 후속. (2) `buildFeatureAsOf`가 종목·일자별 DB 다회 조회 → 대규모 유니버스 시 성능 점검. (3) 가중치 2벌 → metric 차이 자동 검증 테스트 추가 권장.

### 2026-06-01 — Codex CLI 연동 시도 + KIS 키 설정
- 무엇: `codex exec --dangerously-bypass-approvals-and-sandbox --ephemeral` 방식으로 OpenAI Codex CLI 연동 시도. `ProcessBuilder` stdin 닫기, 타임아웃 120→300초. application-local.yml에 KIS/DART 키 설정.
- 결과: Codex CLI(gpt-5.5 에이전트)가 300초 초과로 타임아웃. fallback 템플릿으로 정상 작동. Codex 완전 연동은 미완.
- 변경: `CodexClient`(exec 서브커맨드, stdin close, 타임아웃 증가), `application-local.yml`(KIS/DART/Telegram 키).
- 후속: Codex 대신 Anthropic Claude API 직접 호출로 교체 검토. 또는 codex exec 완료 대기 방식 개선.

### 2026-06-01 — Yahoo Finance 일봉 클라이언트 추가 + FRED HTTP/1.1 수정
- 무엇: Stooq가 일봉 히스토리에 API 키 요구로 정책 변경 → `YahooFinanceClient` 신규 구현. `MarketDataSyncService.fetchDailyPrices` US 시장을 Stooq→Yahoo로 교체. FRED/ExternalApiPingClient HTTP/2 RST_STREAM 오류 → HTTP_1_1 강제. DART API 키 설정.
- 검증: `daily-prices/sync` NASDAQ 50종목 → `upserted=4366`. `UsPreOpenJob` 트리거 → `dailySaved=2573, recommendations=4` 성공. Telegram 추천 알림 수신.
- 변경: 신규 `YahooFinanceClient`, `MarketDataSyncService`(Yahoo 연결), `FredMacroClient`(HTTP_1_1), `ExternalApiPingClient`(HTTP_1_1), `application-local.yml`(DART 키).

### 2026-06-01 — 운영 버그 3종 수정 (KIND 파싱·name 길이·Stooq --)
- 무엇:
  - **KIND 파싱 수정**: `KrxSymbolClient.parseRow`에서 `cells.get(1)`(시장구분)을 ticker로 잘못 읽던 문제 → 6자리 숫자 셀을 stream으로 탐색하도록 변경.
  - **name 컬럼 확장**: `market_universe.name` `nvarchar(200)` → `nvarchar(500)`. V10 마이그레이션 추가. `MarketUniverseEntity`에 `truncate(500)` 방어 처리.
  - **Stooq `--` 파싱 오류**: `parseQuote`/`parseDailyPriceLine` 유효성 체크에 `"--"` 추가 (`N/D`와 동일 처리).
- 왜: US 프리오픈 스케줄러 트리거 시 연속 에러 발생. 각각 KIND 테이블 컬럼 순서 변경, 미국 종목 긴 이름, Stooq 데이터 없음 응답 때문.
- 변경: `KrxSymbolClient`, `MarketUniverseEntity`, `V10__market_universe_name_expand.sql`, `StooqQuoteClient`.
- 검증: `UsPreOpenJob` 트리거 → `universeSaved=7425, quotesSaved=49, recommendations=4` 성공. `gradlew test` BUILD SUCCESSFUL.
- 후속: KRX 추천은 KIS API 키 없으면 `No usable price data` 에러(dev-placeholder 한계). US 플로우 정상.

### 2026-06-01 — 스케줄러 수동 트리거 API 추가 + 운영 검증
- 무엇: `DevNotificationController`에 KRX프리오픈/US프리오픈/US마감 즉시 실행 엔드포인트 추가. `UsPreOpenJob`/`UsCloseSummaryJob`에 `public trigger()` 노출. KRX·US마감 트리거 실행 후 Telegram 알림 수신 확인.
- 왜: 실제 cron 시각(KRX 08:30, US 마감 05:30) 대기 없이 스케줄러 통합 검증 필요.
- 변경: `DevNotificationController`(트리거 3개 엔드포인트), `UsPreOpenJob.trigger()`, `UsCloseSummaryJob.trigger()`.
- 검증: `POST /api/dev/notifications/trigger/krx-preopen`, `/trigger/us-close` → 200, Telegram 메시지 수신.
- 후속: `POST /api/dev/notifications/trigger/us-preopen` 동일 방식 검증 가능.

### 2026-06-01 — Telegram 실발송 검증 완료 + 전체 빌드 확인
- 무엇: `application-local.yml`에 실제 Bot Token/Chat ID 설정 후 `/api/dev/notifications/test` 호출로 Telegram 메시지 수신 확인. 백엔드 `gradlew test` + 프론트 `npm run build` 전체 통과.
- 변경: `application-local.yml`(telegram.bot-token, telegram.chat-id 실값 설정).
- 검증: `sent:true, devMode:false` 응답, Telegram 메시지 도착 확인. BUILD SUCCESSFUL.
- 후속: 스케줄러 실 cron(KRX/US 프리오픈·마감) 알림 운영 검증.

### 2026-05-31 — Telegram 실발송 검증 정보 보강
- 무엇: Telegram 전송 결과를 `TelegramSendResult`로 구조화해 dev-placeholder 여부, HTTP 상태 코드, 실패 원인을 구분하고, 테스트 발송 API와 `notification_log.error_message`에 실패 원인을 남기도록 변경.
- 왜: M8 실제 Telegram 검증 시 토큰 누락, Chat ID 누락, Telegram API 거절, 네트워크 실패를 `sent=false` 하나로만 볼 수 있어 원인 파악이 어려웠기 때문.
- 변경: `TelegramClient`, `NotificationLogService`, `DevNotificationController`, `DevNotificationTestResponse`, `NotificationLogServiceTest`, `docs/20-API.md`, `docs/21-API-QUICK.md`, `docs/12-SETUP-CHECKLIST.md`.
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.notification.NotificationLogServiceTest --tests com.parkdh.stockadvisor.application.notification.NotificationServiceTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: 실제 `TELEGRAM_BOT_TOKEN`/`TELEGRAM_CHAT_ID` 설정 후 `/api/dev/notifications/test` 호출로 메시지 도착과 `notification_log`의 `SENT` 기록 확인.

### 2026-05-31 — M8 페이퍼트레이딩 운영 알림 연동
- 무엇: US 마감 요약 알림에 `StatsService.getPaperTrading()` 기반 페이퍼트레이딩 요약을 포함. OPEN 추천 수, 가격 확인 수, 평균/비중 반영 미실현 손익, 목표/손절 터치 수, 리스크 체크 포지션을 Telegram 메시지 본문에 함께 표시.
- 왜: 성과 통계 화면을 열지 않아도 운영 알림에서 M7 추천의 페이퍼 성과와 손절 근접 종목을 매일 확인할 수 있어야 하기 때문.
- 변경: `NotificationService.formatPaperTradingSummary`, `UsCloseSummaryJob`, `NotificationServiceTest`.
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.notification.NotificationServiceTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: 실제 Telegram Bot Token/Chat ID 환경에서 발송 도착 및 `notification_log` 성공 기록 검증.

### 2026-05-31 — M8 페이퍼트레이딩 프론트 패널 추가
- 무엇: 성과 통계 화면에 `GET /api/stats/paper-trading` 결과를 표시하는 페이퍼트레이딩 패널을 추가. OPEN 추천 수, 가격 확인 수, 평균/비중 반영 미실현 손익, 목표/손절 터치 수, 포지션별 현재가·비중·손익·이격률을 표로 표시.
- 왜: M8 운영 검증을 위해 백엔드 API만이 아니라 실제 화면에서 OPEN 추천의 페이퍼 성과를 매일 확인할 수 있어야 하기 때문.
- 변경: `apps/web/src/api/stats.ts`, `apps/web/src/pages/StatsPage.tsx`, `apps/web/src/styles.css`; 프론트 의존성 설치(`apps/web/node_modules`, git 추적 제외).
- 검증: `npm.cmd install`, `npm.cmd run build` 성공.
- 후속: 운영 알림(US/KRX 마감 요약)에 paper-trading 요약 포함, 실제 Telegram 발송 검증.

### 2026-05-31 — M8 페이퍼트레이딩 모니터링 API 추가
- 무엇: OPEN 추천의 최신 `price_daily` 종가 기준 미실현 손익, `signalsJson.positionWeightPct` 반영 손익, 목표/손절 터치 상태를 반환하는 `GET /api/stats/paper-trading` 추가.
- 왜: M7 추천 두뇌 개선 후 실제 매매 전 페이퍼트레이딩으로 운영 성과를 매일 추적할 수 있어야 하기 때문.
- 변경: `StatsPaperTradingResponse`, `StatsService.getPaperTrading`, `StatsController`, `StatsServiceTest`, API 문서(`20-API`, `21-API-QUICK`).
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.stats.StatsServiceTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: 프론트 통계 화면에 paper-trading 패널 추가, 운영 알림에 paper-trading 요약 연동, 실제 Telegram 발송 검증.

### 2026-05-31 — M7 TASK-7 구현 (확신도·역변동성 포지션 사이징)
- 무엇: `PricePredictor`가 최근 변동성(`volatilityPct`)과 score×역변동성 기반 `positionSizingScore`를 산출하도록 확장하고, `DevRecommendationGenerateService`가 선택된 SHORT/LONG 추천 묶음별로 confidence×positionSizingScore를 총 100% 기준으로 정규화하되 종목당 20% 상한을 적용해 `signalsJson.positionWeightPct`에 기록하도록 변경.
- 왜: 기존 `positionWeightPct`는 개별 추천의 손절폭과 confidence만 보고 산출해 후보군 간 상대 확신도·변동성 차이를 포트폴리오 비중에 반영하지 못했기 때문.
- 변경: `PredictedRecommendation`(`volatilityPct`, `positionSizingScore` 필드 추가), `PricePredictor`, `DevRecommendationGenerateService`, `PricePredictorTest`, `DevRecommendationGenerateServiceTest`.
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.recommendation.PricePredictorTest --tests com.parkdh.stockadvisor.application.dev.DevRecommendationGenerateServiceTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: M7 TASK-1~8 중 핵심 구현은 TASK-1·2·3·4·5·6·7·8 완료. 다음은 M5·M8 실키 검증/페이퍼트레이딩.

### 2026-05-31 — M7 TASK-6 구현 (IC 측정 + mutation 가이드)
- 무엇: `feature_snapshot.feature_json`의 피처 점수와 `fwd_ret_5d`/`fwd_ret_20d`를 Spearman IC로 측정하는 `FeatureICService`를 추가하고, `AutoresearchService.mutateWeights`가 IC 상위 피처는 증액, IC 약하거나 음수인 피처는 감액하도록 가이드 연결.
- 왜: 기존 AutoResearch가 10% 순환 랜덤 변형만 수행해 어떤 피처가 forward return과 상관 있는지 반영하지 못했기 때문.
- 변경: 신규 `FeatureICService`, `FeatureSnapshotRepository.findByFwdRet5dIsNotNullOrFwdRet20dIsNotNull`, `AutoresearchService`(IC-guided `MutationGuide`, `diffSummary`에 IC 요약 기록), `FeatureICServiceTest`, `AutoresearchServiceTest`.
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.research.FeatureICServiceTest --tests com.parkdh.stockadvisor.application.autoresearch.AutoresearchServiceTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: TASK-7 확신도·역변동성 포지션 사이징 정교화.

### 2026-05-31 — M7 TASK-5 구현 (cross-sectional 표준화)
- 무엇: `UniverseFeatureBuilder.buildFeatures()`에서 시장별 유니버스 그룹 안의 raw feature 점수를 백분위 기반 상대 점수(35~95)로 변환한 뒤 기술/컨텍스트/종합 점수를 재합산하도록 변경. 단일 종목 그룹은 기존 원점수 유지.
- 왜: 절대 계단 점수로 종목들이 같은 버킷에 몰려 Top-N 차별력이 약한 문제를 줄이고, TASK-6 IC 측정 전 피처 분포를 넓히기 위해.
- 변경: `UniverseFeatureBuilder`(시장별 cross-sectional normalization, `featureJson`에 `raw*Score`, `crossSectionalNormalized`, `feature-rule-v4` 기록), `UniverseFeatureBuilderTest`(시장 내 상대 점수 정렬/JSON 검증 추가).
- 검증: `.\gradlew.bat test --tests com.parkdh.stockadvisor.application.feature.UniverseFeatureBuilderTest --console=plain` BUILD SUCCESSFUL, `.\gradlew.bat test --console=plain` BUILD SUCCESSFUL.
- 후속: TASK-6 IC 측정 파이프라인 + `AutoresearchService.mutateWeights` 가이드, TASK-7 포지션 사이징 정교화.

### 2026-05-31 — M7 TASK-1·2·3·4·8 구현 (수익 두뇌 토대)
- 무엇:
  - **TASK-1** `BacktestRunService` — `selectTopCandidates`(현재 시점 필터) 제거. 과거 윈도우만으로 계산하는 `computePriceScore`(MA 35%+RSI 35%+Volume 30%) 도입. `backtest.entry.minScore`(기본 60) 달성 시 진입.
  - **TASK-2** `feature_snapshot` 테이블(V9 마이그레이션) + `FeatureSnapshotEntity`/`FeatureSnapshotRepository` 신설. `UniverseFeatureBuilder.buildFeatureAsOf(entity, asOf)` 추가(PIT, 미래참조 차단). `FeatureSnapshotJob`(22:00 MON-FRI KST) T+5/T+20 forward return 백필 포함.
  - **TASK-3** `scoreFundamentals`→ PER/PBR/ROE/revenueGrowthYoY/epsGrowthYoY 방향성 점수화(이전: 데이터 개수 카운트). `scoreMacro`→ T10Y2Y/UNRATE/BAMLH0A0HYM2 regime 점수화. 데이터 없음 시 50(중립) 반환.
  - **TASK-4** `scoreNews`→ 감성 평균(-1~+1)을 30-90 선형 매핑. 1건 시 중립 수축. 데이터 없음 시 50.
  - **TASK-8** `StooqQuoteClient.fetchDailyPricesByStooqSymbol` 추가. `MarketDataSyncService.syncIndexDailyPrices`(^ks11=KOSPI, spy.us=SPY). `MarketDataCollectionJob.runUsPreOpenCollection`에서 지수 일봉 선행 동기화. V9 마이그레이션에 `recommendation.regime.filter.enabled=true` 기본값 설정.
- 변경: `BacktestRunService`, `UniverseFeatureBuilder`, `StooqQuoteClient`, `MarketDataSyncService`, `MarketDataCollectionJob`, `BacktestRunServiceTest`(테스트 갱신), 신규: `V9__feature_snapshot.sql`, `FeatureSnapshotEntity`, `FeatureSnapshotRepository`, `FeatureSnapshotJob`, PriceDailyRepository/News/Disclosure/Fundamental/MacroObservation 레포지토리 date-bounded 쿼리 추가.
- 검증: `gradlew test` BUILD SUCCESSFUL (BacktestRunServiceTest 2건, UniverseFeatureBuilderTest 1건 + 전체 통과).
- 후속: TASK-5 cross-sectional 표준화, TASK-6 IC 측정, TASK-7 포지션 사이징.

### 2026-05-29 — 코드 진행상황 전수 감사 + 로드맵 실측 갱신
- 무엇: 실제 코드 인벤토리(컨트롤러19·서비스18·클라이언트13·스케줄러6·테스트24·프론트7·Flyway V8) 측정 → 11-ROADMAP 단계맵/§0/감사표 갱신.
- 발견:
  - 🔴 **빌드 리스크**: `ExitConfirmServiceTest`가 삭제된 `ExitConfirmService` 참조 → `gradlew test` 컴파일 실패. **즉시 삭제/이관 필요.**
  - ✅ 배선 확인: `SentimentAnalysisClient`·`DartFundamentalClient` → `MarketDataCollectionService` 연결. `NotificationService` → 4 스케줄러+ExitMonitor 배선(알림 실동작).
  - M5(알림) 골격→배선 완료 수준으로 상향. M1 KR 펀더멘털/감성 수집 배선 확인.
- 변경: `docs/11-ROADMAP.md`(단계맵 빌드리스크 줄·M5·§0·감사표)
- 후속: ExitConfirmServiceTest 처리 → 빌드 그린 확인. 그다음 M7 TASK-1·2.

### 2026-05-29 — docs 재구성 + 에이전트 부트 컨텍스트
- 무엇: docs 번호체계(00~90) 리네임, 00-INDEX 신설, 상태배너 통일, 스테일/모순 정정(ResultDto 포맷·ExitConfirm 제거·스택 MSSQL). 루트 CLAUDE.md/AGENTS.md + 이 WORKLOG 추가.
- 왜: 문서 난립·중복·일관성 부족 해소, 새 세션이 구조·진행상황 자동 인지하도록.
- 변경: `docs/*`, `CLAUDE.md`, `AGENTS.md`, `README.md`
- 후속: 미커밋 상태. 수익률 작업은 [40-RETURN-STRATEGY.md](40-RETURN-STRATEGY.md) TASK-1·2부터.
