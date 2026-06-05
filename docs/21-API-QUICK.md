# Stock Advisor API 한눈에 보기

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 21(API요약) · 상태 🟢 현행 · 코드기준 2026-06-04
>
> 목적: 관리자 화면·Swagger 테스트 빠른 참조  
> 상세: [20-API.md](20-API.md)  
> 공통 응답: 성공 `{"code":200,"data":...}` / 실패 `{"code":에러코드,"error_message":"메시지"}`

---

## Swagger 가시성 범례

| 표시 | 의미 |
|---|---|
| ✅ | Swagger 노출 — 관리자가 직접 사용 |
| 🔒 | Swagger 숨김(`hidden=true`) — HTTP 접근은 되지만 문서 미노출. 내부 플로우 전용 |
| 🚫 | Swagger 숨김(`@Hidden`) — 개발용. HTTP 접근은 되지만 운영에서는 호출 금지 |

---

## 1. 관리자 설정 `/api/admin`

✅ 전체 노출

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/admin/settings` | 전체 설정 조회 |
| GET | `/api/admin/settings/{key}` | 단건 조회 |
| PUT | `/api/admin/settings/{key}` | 설정 수정 |
| POST | `/api/admin/settings/reset` | 기본값 초기화 |
| GET | `/api/admin/audit-logs` | 변경 이력 |

### 주요 설정 키 목록

| 키 | 기본값 | 설명 |
|---|---|---|
| `recommendation.short.count` | `{"value":3,"min":1,"max":10}` | 단기 추천 개수 |
| `recommendation.long.count` | `{"value":3,"min":1,"max":10}` | 장기 추천 개수 |
| `recommendation.market.enabled` | `{"kr":true,"us":true}` | 시장별 활성화 |
| `recommendation.marketcap.kr.min` | `{"value":300000000000,"currency":"KRW"}` | 한국 시총 하한 |
| `recommendation.marketcap.us.min` | `{"value":1000000000,"currency":"USD"}` | 미국 시총 하한 |
| `recommendation.turnover.kr.min` | `{"value":10000000000,"currency":"KRW"}` | 한국 거래대금 하한 |
| `recommendation.turnover.us.min` | `{"value":10000000,"currency":"USD"}` | 미국 거래대금 하한 |
| `recommendation.feature.minTotalScore` | `{"value":0,"min":0,"max":100}` | 추천 최소 feature 점수 |
| `recommendation.scoring.weights` | `{...}` | feature 가중치 (liquidity/price/technical/context/fundamental/dataQuality) |
| `recommendation.excluded.sectors` | `{"value":[]}` | 제외 섹터 목록 |
| `recommendation.watchlist` | `{"include":[],"exclude":[]}` | 강제 포함/제외 종목 |
| `notification.krx.preopen.offsetMinutes` | `{"value":-30,"displayTime":"08:30"}` | KRX 프리오픈 알림 시각 |
| `notification.us.preopen.offsetMinutes` | `{"value":-30,"dstTime":"22:00","standardTime":"23:00"}` | US 프리오픈 알림 시각 |
| `notification.us.close.offsetMinutes` | `{"value":30,"dstTime":"05:30","standardTime":"06:30"}` | US 마감 알림 시각 |
| `notification.holiday.enabled` | `{"value":true}` | 휴장일 알림 발송 |
| `notification.holiday.kr.closedDates` | `{"value":[]}` | 한국 휴장일 목록 |
| `notification.holiday.us.closedDates` | `{"value":[]}` | 미국 휴장일 목록 |
| `notification.channel.priority` | `{"value":["TELEGRAM","KAKAO"]}` | 알림 채널 우선순위 |
| `collection.enabled` | `{"value":true}` | 뉴스/공시/매크로 자동 수집 활성 |
| `collection.news.tickersPerMarket` | `{"value":20}` | 시장별 뉴스 수집 후보 수 |
| `collection.news.limitPerTicker` | `{"value":5}` | 종목별 뉴스 수집 개수 |
| `collection.disclosure.limit` | `{"value":20}` | 공시 수집 개수 |
| `collection.macro.limitPerSeries` | `{"value":5}` | 매크로 지표별 수집 개수 |
| `collection.fundamental.tickersPerMarket` | `{"value":3}` | 펀더멘털 수집 후보 수 |
| `collection.kis.dailyPrice.delayMs` | `{"value":600,"min":100,"max":3000}` | KIS 일봉 호출 간 딜레이(ms) |
| `exit.polling.intervalMinutes` | `{"value":5,"options":[1,3,5,10,30]}` | 손절 모니터링 폴링 주기 |
| `exit.intraday.enabled` | `{"value":true}` | 장중 즉시 손절 알림 |
| `exit.extendedHours.enabled` | `{"value":false}` | 시간외 모니터링 |
| `backtest.period.years` | `{"value":5,"options":[1,3,5,10]}` | 백테스트 기간 |
| `backtest.walkForward.days` | `{"value":180}` | Walk-forward 윈도우 |
| `backtest.slippage.percent` | `{"value":0.05}` | 슬리피지 가정 |
| `backtest.cost.kr` | `{"taxPercent":0.18,"feePercent":0.015}` | 한국 거래비용 |
| `backtest.cost.us` | `{"secFeeEnabled":true,"fxSpreadPercent":0.5}` | 미국 거래비용 |
| `autoresearch.enabled` | `{"value":true}` | AutoResearch 활성 |
| `autoresearch.targetIterations` | `{"value":80}` | 야간 실험 목표 횟수 |
| `autoresearch.maxTickers` | `{"value":30,"min":1,"max":300}` | 백테스트 최대 종목 수 |
| `autoresearch.holdingDays` | `{"value":20,"min":1,"max":120}` | 백테스트 보유 기간 |
| `autoresearch.targetPct` | `{"value":3.0,"min":0.1,"max":50}` | 백테스트 목표 수익률 |
| `autoresearch.stopPct` | `{"value":2.0,"min":0.1,"max":50}` | 백테스트 손절률 |
| `autoresearch.rollbackValidationDays` | `{"value":7}` | Champion 롤백 검증 기간 |
| `codex.daily.callLimit` | `{"value":200}` | Codex 일 호출 한도 |
| `codex.daily.budgetUsd` | `{"value":0}` | Codex 일 예산 |
| `codex.profile` | `{"value":"stock-advisor"}` | Codex profile |
| `dailybrief.prompt.maxChars` | `{"value":6000,"min":800,"max":20000}` | Daily Brief 프롬프트 최대 길이 |
| `ops.health.priceDaily.maxAgeDays` | `{"value":3}` | 일봉 헬스체크 허용 지연일 |
| `ops.health.news.maxAgeHours` | `{"value":48}` | 뉴스 헬스체크 허용 지연시간 |
| `operation.dbBackup.enabled` | `{"value":true}` | DB 백업 스케줄 활성 |
| `operation.dbBackup.cron` | `{"value":"0 0 3 * * *"}` | DB 백업 스케줄 cron |

---

## 2. 운영 API `/api/ops`

✅ 전체 노출

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/ops/external-health` | KIS·Telegram·Codex·Stooq·KIND 연동 상태 |
| POST | `/api/ops/jobs/{jobName}/trigger` | 스케줄러 Job 수동 즉시 실행 (비동기) |

### Job 트리거 jobName 목록

| jobName | 실행 내용 |
|---|---|
| `krx-preopen` | KRX 장전 추천 + 유니버스 동기화 + Telegram 알림 |
| `us-preopen` | 미국장 장전 추천 + 유니버스 동기화 + Telegram 알림 |
| `backfill-kr` | KOSPI·KOSDAQ 일봉 히스토리 백필 |
| `backfill-us` | NASDAQ·NYSE 일봉 히스토리 백필 |
| `feature-snapshot` | 전 종목 feature 스냅샷 저장 (T+5·T+20 forward return 백필 포함) |

응답: `{"code":200,"data":{"jobName":"krx-preopen","status":"TRIGGERED"}}`  
Job은 백그라운드 실행 → 완료 결과는 Telegram으로 확인

---

## 3. 성과 통계 `/api/stats`

✅ 전체 노출

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/stats/summary` | 전체 개수·hit rate·평균/누적 손익·MDD |
| GET | `/api/stats/daily` | 일별 평가 손익·누적 손익 |
| GET | `/api/stats/by-strategy` | 전략 버전별 성과 |
| GET | `/api/stats/paper-trading` | OPEN 추천 미실현 손익·목표/손절 터치 현황 |

---

## 4. 추천 `/api/recommendations`

✅ GET · 🔒 POST·PUT

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/recommendations` | ✅ | 목록 조회 (`?status=OPEN`, `?ticker=005930`) |
| GET | `/api/recommendations/{id}` | ✅ | 단건 조회 |
| POST | `/api/recommendations` | 🔒 | 추천 생성 (KrxPreOpenJob·UsPreOpenJob 내부 호출) |
| PUT | `/api/recommendations/{id}/status` | 🔒 | 상태 변경 (ExitMonitorJob 내부 호출) |

---

## 5. 평가 `/api/evaluations`

✅ GET · 🔒 POST

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/evaluations` | ✅ | 목록 조회 (`?recommendationId=1`) |
| GET | `/api/evaluations/{id}` | ✅ | 단건 조회 |
| POST | `/api/evaluations` | 🔒 | 평가 생성 (ExitMonitorJob 내부 호출) |

---

## 6. 시장 데이터 `/api/market-data`

✅ 전체 노출

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/market-data/daily-prices` | 일봉 조회 (`?market=KOSPI&ticker=005930&limit=100`) |
| GET | `/api/market-data/intraday-prices` | 장중 가격 조회 |
| POST | `/api/market-data/daily-prices/sync` | 일봉 수동 동기화 (`?market=ALL&limit=20&days=120`) |
| GET | `/api/market-data/news` | 뉴스 조회 |
| POST | `/api/market-data/news/sync` | 멀티소스 뉴스 동기화. 출처별 limit, KST 기준 당일 발행분만 저장 |
| GET | `/api/market-data/disclosures` | 공시 조회 |
| POST | `/api/market-data/disclosures/sync` | DART/SEC 공시 동기화 |
| GET | `/api/market-data/macro-observations` | FRED 매크로 조회 |
| POST | `/api/market-data/macro-observations/sync` | FRED 매크로 동기화 |
| GET | `/api/market-data/fundamentals` | 펀더멘털 조회 |
| POST | `/api/market-data/fundamentals/sync` | 펀더멘털 동기화 (`ticker` 필수) |

---

## 7. 유니버스 `/api/universe`

✅ 전체 노출

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/universe` | 시장 후보군 조회 (필터: `market`, `tradable`, `minMarketCap`, `minAvgTurnover`) |
| POST | `/api/universe/sync/kr-symbols` | KIND 한국 상장 심볼 동기화 |
| POST | `/api/universe/sync/us-symbols` | NASDAQ Trader 미국 심볼 동기화 |
| POST | `/api/universe/sync/us-prices` | Stooq 미국 최근 시세 동기화 |

---

## 8. 백테스트 `/api/backtests`

✅ GET·simulate · 🔒 POST(저장)

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/backtests` | ✅ | 실행 목록 조회 |
| GET | `/api/backtests/{id}` | ✅ | 단건 조회 |
| POST | `/api/backtests/simulate` | ✅ | 일봉 기반 룰 백테스트 실행 |
| POST | `/api/backtests` | 🔒 | 결과 저장 (AutoResearch 내부 호출) |

---

## 9. AutoResearch `/api/autoresearch`

✅ GET·auto · 🔒 POST(저장)

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/autoresearch/runs` | ✅ | 실험 실행 목록 (`?jobRunId=uuid`) |
| GET | `/api/autoresearch/runs/{id}` | ✅ | 단건 조회 |
| POST | `/api/autoresearch/runs/auto` | ✅ | 가중치 자동 실험 + champion 갱신 |
| POST | `/api/autoresearch/runs` | 🔒 | 실험 결과 저장 (내부 호출) |
| GET | `/api/autoresearch/strategies` | ✅ | 전략 버전 목록 (`?champion=true`) |
| GET | `/api/autoresearch/strategies/{id}` | ✅ | 단건 조회 |
| POST | `/api/autoresearch/strategies` | 🔒 | 전략 버전 저장 (내부 호출) |

---

## 10. 알림 로그 `/api/notifications/logs`

✅ GET · 🔒 POST

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/notifications/logs` | ✅ | 목록 조회 (`?status=SENT`) |
| GET | `/api/notifications/logs/{id}` | ✅ | 단건 조회 |
| POST | `/api/notifications/logs` | 🔒 | 로그 저장 (NotificationService 내부 호출) |

---

## 11. Codex 호출 로그 `/api/codex/calls`

✅ GET · 🔒 POST

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/codex/calls` | ✅ | 목록 조회 (`?caller=BRIEF_KR`) |
| GET | `/api/codex/calls/{id}` | ✅ | 단건 조회 |
| POST | `/api/codex/calls` | 🔒 | 로그 저장 (CodexClient 내부 호출) |

---

## 12. 데일리 브리프 `/api/briefs`

✅ GET · 🔒 POST

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/briefs` | ✅ | 목록 조회 (`?marketTrack=KRX`) |
| GET | `/api/briefs/{id}` | ✅ | 단건 조회 |
| POST | `/api/briefs` | 🔒 | 브리프 저장 (KrxPreOpenJob·UsPreOpenJob 내부 호출) |

---

## 13. Swagger 숨김 전용 엔드포인트

### 🔒 내부 전용 (플로우 자동 호출)

| URL | 호출자 |
|---|---|
| `POST /api/recommendations` | KrxPreOpenJob / UsPreOpenJob |
| `PUT /api/recommendations/{id}/status` | ExitMonitorJob |
| `POST /api/evaluations` | ExitMonitorJob |
| `POST /api/backtests` | AutoResearchJob |
| `POST /api/autoresearch/runs` | AutoResearchJob |
| `POST /api/autoresearch/strategies` | AutoResearchJob |
| `POST /api/briefs` | KrxPreOpenJob / UsPreOpenJob |
| `POST /api/codex/calls` | CodexClient |
| `POST /api/notifications/logs` | NotificationService |

### 🚫 개발 전용 (운영 호출 금지)

| Base URL | 설명 |
|---|---|
| `/api/dev/recommendations/*` | 개발용 더미 추천 생성 |
| `/api/dev/universe/*` | 개발용 유니버스 시드 |
| `/api/dev/brief/*` | 개발용 브리프 생성 |
| `/api/dev/notifications/*` | Telegram 테스트 발송 |
| `/api/instruments` | 개발용 종목 seed 관리 |
| `/api/features` | feature 점수 직접 조회 |
| `/api/predictions` | 가격 예측 조회/생성 |
