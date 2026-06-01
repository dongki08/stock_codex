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
