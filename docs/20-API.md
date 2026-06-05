# Stock Advisor Backend API 명세

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 20(API) · 상태 🟢 현행 · 코드기준 2026-06-04
>
> 기준: Spring Boot 백엔드 현재 구현  
> DB: MSSQL · Flyway V13
> Swagger: `http://localhost:8083/swagger-ui.html` (BasicAuth 필요)  
> 빠른 참조: [21-API-QUICK.md](21-API-QUICK.md)

---

## 1. 공통 규칙

### 1.1 응답 포맷

```json
{ "code": 200, "data": {} }
{ "code": 400, "error_message": "입력값이 올바르지 않습니다." }
```

### 1.2 날짜/시간

`LocalDate` → `2026-05-20`  
`LocalDateTime` → `2026-05-20T08:30:00`

### 1.3 JSON 문자열 필드

MSSQL 기준 JSON 성격의 값은 `nvarchar(max)` 문자열로 저장한다.

```json
{ "signalsJson": "{\"rsi\":45,\"macd\":\"golden_cross\"}" }
```

### 1.4 Swagger 가시성 정책

| 표시 | 의미 |
|---|---|
| ✅ | Swagger 노출 — 관리자 직접 사용 |
| 🔒 | `@Operation(hidden=true)` — HTTP 접근 가능, Swagger 미노출. 내부 플로우 전용 |
| 🚫 | `@Hidden` — HTTP 접근 가능, Swagger 미노출. 개발 전용, 운영 호출 금지 |

---

## 2. 관리자 설정 API ✅

```
/api/admin
```

### 2.1 설정 목록 조회

```http
GET /api/admin/settings
```

설정 키 기준 오름차순 반환.

### 2.2 설정 단건 조회

```http
GET /api/admin/settings/{key}
```

없으면 404.

### 2.3 설정 수정

```http
PUT /api/admin/settings/{key}
```

```json
{ "valueJson": "{\"value\":5}", "actor": "admin" }
```

`valueJson`은 유효한 JSON 문자열이어야 한다. 인증된 사용자가 있으면 `actor`보다 우선 적용된다.

### 2.4 기본 설정 초기화

```http
POST /api/admin/settings/reset
```

존재하지 않는 키만 새로 생성한다. 이미 있는 키는 덮어쓰지 않는다.

**설정 키 전체 목록:**

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
| `recommendation.feature.minDataQualityScore` | `{"value":0,"min":0,"max":100}` | 추천 최소 데이터 품질 점수 |
| `recommendation.scoring.weights` | `{...}` | feature 가중치 (liquidity·price·technical·context·fundamental·dataQuality) |
| `recommendation.excluded.sectors` | `{"value":[]}` | 제외 섹터 목록 |
| `recommendation.watchlist` | `{"include":[],"exclude":[]}` | 강제 포함/제외 종목 |
| `notification.krx.preopen.offsetMinutes` | `{"value":-30,"displayTime":"08:30"}` | KRX 프리오픈 알림 시각 |
| `notification.us.preopen.offsetMinutes` | `{"value":-30,"dstTime":"22:00","standardTime":"23:00"}` | US 프리오픈 알림 시각 (DST/표준시 분리) |
| `notification.us.close.offsetMinutes` | `{"value":30,"dstTime":"05:30","standardTime":"06:30"}` | US 마감 알림 시각 |
| `notification.holiday.enabled` | `{"value":true}` | 휴장일 알림 발송 여부 |
| `notification.holiday.kr.closedDates` | `{"value":[]}` | 한국 휴장일 날짜 목록 (예: `["2026-01-01"]`) |
| `notification.holiday.us.closedDates` | `{"value":[]}` | 미국 휴장일 날짜 목록 |
| `notification.channel.priority` | `{"value":["TELEGRAM","KAKAO"]}` | 알림 채널 우선순위 |
| `collection.enabled` | `{"value":true}` | 뉴스/공시/매크로 자동 수집 활성 |
| `collection.news.tickersPerMarket` | `{"value":20}` | 시장별 뉴스 수집 후보 수 |
| `collection.news.limitPerTicker` | `{"value":5}` | 종목별 뉴스 수집 개수 |
| `collection.disclosure.limit` | `{"value":20}` | 시장별 공시 수집 개수 |
| `collection.macro.limitPerSeries` | `{"value":5}` | 매크로 지표별 수집 개수 |
| `collection.fundamental.tickersPerMarket` | `{"value":3}` | 펀더멘털 수집 후보 수 |
| `collection.kis.dailyPrice.delayMs` | `{"value":600,"min":100,"max":3000}` | KIS 일봉 호출 간 딜레이(ms). KIS 초당 한도 준수용 |
| `exit.polling.intervalMinutes` | `{"value":5,"options":[1,3,5,10,30]}` | ExitMonitorJob 폴링 주기 |
| `exit.intraday.enabled` | `{"value":true}` | 장중 즉시 손절 알림 |
| `exit.extendedHours.enabled` | `{"value":false}` | 시간외 모니터링 |
| `backtest.period.years` | `{"value":5,"options":[1,3,5,10]}` | 백테스트 기본 기간 |
| `backtest.walkForward.days` | `{"value":180}` | Walk-forward 윈도우 |
| `backtest.slippage.percent` | `{"value":0.05}` | 슬리피지 가정 |
| `backtest.cost.kr` | `{"taxPercent":0.18,"feePercent":0.015}` | 한국 거래비용 가정 |
| `backtest.cost.us` | `{"secFeeEnabled":true,"fxSpreadPercent":0.5}` | 미국 거래비용 가정 |
| `autoresearch.enabled` | `{"value":true}` | AutoResearch 활성 |
| `autoresearch.targetIterations` | `{"value":80}` | 야간 실험 목표 횟수 |
| `autoresearch.maxTickers` | `{"value":30,"min":1,"max":300}` | 백테스트 최대 종목 수 |
| `autoresearch.holdingDays` | `{"value":20,"min":1,"max":120}` | 백테스트 보유 기간 |
| `autoresearch.targetPct` | `{"value":3.0,"min":0.1,"max":50}` | 백테스트 목표 수익률 |
| `autoresearch.stopPct` | `{"value":2.0,"min":0.1,"max":50}` | 백테스트 손절률 |
| `autoresearch.rollbackValidationDays` | `{"value":7}` | Champion 롤백 검증 기간 |
| `codex.daily.callLimit` | `{"value":200}` | Codex 일 호출 한도 |
| `codex.daily.budgetUsd` | `{"value":0}` | Codex 일 예산 |
| `codex.estimatedUsdPer1kChars` | `{"value":0.002}` | Codex 문자 1천자당 예상 비용 |
| `codex.estimatedResponseChars` | `{"value":4000}` | Codex 응답 예상 길이 |
| `codex.profile` | `{"value":"stock-advisor"}` | Codex CLI profile |
| `dailybrief.prompt.maxChars` | `{"value":6000,"min":800,"max":20000}` | Daily Brief 프롬프트 최대 길이 |
| `ops.health.priceDaily.maxAgeDays` | `{"value":3}` | 일봉 헬스체크 허용 지연일 |
| `ops.health.priceIntraday.maxAgeMinutes` | `{"value":120}` | 장중 헬스체크 허용 지연분 |
| `ops.health.news.maxAgeHours` | `{"value":48}` | 뉴스 헬스체크 허용 지연시간 |
| `ops.health.disclosure.maxAgeDays` | `{"value":14}` | 공시 헬스체크 허용 지연일 |
| `ops.health.macro.maxAgeDays` | `{"value":14}` | 매크로 헬스체크 허용 지연일 |
| `ops.health.fundamental.maxAgeDays` | `{"value":120}` | 펀더멘털 헬스체크 허용 지연일 |
| `operation.dbBackup.enabled` | `{"value":true}` | DB 백업 스케줄 활성 |
| `operation.dbBackup.cron` | `{"value":"0 0 3 * * *"}` | DB 백업 스케줄 cron |

### 2.5 감사 로그 조회

```http
GET /api/admin/audit-logs
```

최근 50건 역순 반환.

---

## 3. 운영 API ✅

```
/api/ops
```

### 3.1 외부 연동 상태

```http
GET /api/ops/external-health
```

KIS·Telegram·Codex·Stooq·KIND 키 설정 상태와 접근 가능 여부를 반환한다.

### 3.2 스케줄러 Job 수동 트리거

```http
POST /api/ops/jobs/{jobName}/trigger
```

Job을 즉시 한 번 실행한다. 응답은 즉시 반환되고 Job은 백그라운드에서 실행된다. 완료 결과는 Telegram 알림으로 확인한다.

**jobName:**

| 값 | 실행 내용 | 스케줄 (평시) |
|---|---|---|
| `krx-preopen` | KRX 유니버스 동기화 + 일봉 증분 + 단기·장기 추천 생성 + Telegram 알림 | 평일 08:30 KST |
| `us-preopen` | 미국 심볼·시세 동기화 + 일봉 증분 + 추천 생성 + Telegram 알림 | 평일 DST 22:00 / 표준시 23:00 KST |
| `backfill-kr` | KOSPI·KOSDAQ 일봉 히스토리 백필 (최대 50종목 × 120일) | 평일 18:10 KST |
| `backfill-us` | NASDAQ·NYSE 일봉 히스토리 백필 (최대 50종목 × 180일) | 화~토 07:20 KST |
| `feature-snapshot` | 전 종목 PIT feature 스냅샷 저장 + T+5·T+20 forward return 백필 | 평일 22:00 KST |

응답:

```json
{ "code": 200, "data": { "jobName": "krx-preopen", "status": "TRIGGERED" } }
```

없는 jobName은 404 반환.

---

## 4. 성과 통계 API ✅

```
/api/stats
```

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/stats/summary` | 전체·종료·진행·만료 개수, hit rate, 평균·누적 손익률, MDD |
| GET | `/api/stats/daily` | 일별 평가 손익, 누적 손익 |
| GET | `/api/stats/by-strategy` | 전략 버전별 성과 |
| GET | `/api/stats/paper-trading` | OPEN 추천 미실현 손익·비중·목표/손절 터치 현황 |

### 4.1 페이퍼트레이딩 응답 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| openCount | Integer | OPEN 추천 수 |
| pricedCount | Integer | 최신 일봉 가격이 있는 추천 수 |
| avgUnrealizedPnlPct | Decimal | 단순 평균 미실현 손익률 |
| weightedUnrealizedPnlPct | Decimal | `signalsJson.positionWeightPct` 반영 손익률 |
| targetTouchCount | Integer | 최신 종가 ≥ 목표가 추천 수 |
| stopTouchCount | Integer | 최신 종가 ≤ 손절가 추천 수 |
| positions | Array | 종목별 현재가·손익률·목표/손절 이격률 |

`positionWeightPct` 없는 추천은 동일가중 fallback, 종목당 20% 상한 적용.

---

## 5. 추천 API ✅ GET / 🔒 POST·PUT

```
/api/recommendations
```

### 5.1 추천 목록 조회

```http
GET /api/recommendations?status=OPEN&ticker=005930
```

`ticker` 지정 시 `status`보다 우선 적용.

응답 `data` 항목:

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 추천 ID |
| ticker / market | String | 종목 코드 / 시장 |
| term | String | `SHORT` / `LONG` |
| entryPrice / targetPrice / stopPrice | Decimal | 진입·목표·손절 가격 |
| expectedExitAt | Date | 예상 매도일 |
| confidence | Integer | 신뢰도 0~100 |
| signalsJson | String | RSI·MACD·뉴스 점수 등 근거 JSON |
| modelVersion | String | 추천 생성 전략 버전 |
| status | String | `OPEN` / `CLOSED` / `EXPIRED` |

### 5.2 추천 단건 조회

```http
GET /api/recommendations/{id}
```

### 5.3 추천 생성 🔒

```http
POST /api/recommendations
```

KrxPreOpenJob·UsPreOpenJob이 직접 Service를 호출한다. HTTP로 호출할 필요 없음.

### 5.4 추천 상태 변경 🔒

```http
PUT /api/recommendations/{id}/status
```

ExitMonitorJob이 자동 처리. 허용 상태: `OPEN` / `CLOSED` / `EXPIRED`.

---

## 6. 평가 API ✅ GET / 🔒 POST

```
/api/evaluations
```

### 6.1 평가 목록 조회

```http
GET /api/evaluations?recommendationId=1
```

### 6.2 평가 단건 조회

```http
GET /api/evaluations/{id}
```

### 6.3 평가 생성 🔒

```http
POST /api/evaluations
```

ExitMonitorJob이 목표가·손절가·만료 기준으로 자동 생성. `exitReason`: `TARGET_HIT` / `STOP_HIT` / `TIME_OUT` / `MANUAL_CLOSE`.

---

## 7. 시장 데이터 API ✅

```
/api/market-data
```

### 7.1 일봉 가격

```http
GET  /api/market-data/daily-prices?market=KOSPI&ticker=005930&limit=100
POST /api/market-data/daily-prices/sync?market=ALL&limit=20&days=120
```

동기화는 KIS(한국) 또는 Yahoo Finance(미국)에서 증분 조회한다. KIS 호출 간 딜레이는 `collection.kis.dailyPrice.delayMs` 설정으로 조정한다.

### 7.2 장중 가격

```http
GET /api/market-data/intraday-prices?market=KOSPI&ticker=005930&limit=100
```

### 7.3 뉴스

```http
GET  /api/market-data/news?market=KOSPI&ticker=005930&limit=20
POST /api/market-data/news/sync?market=KOSPI&ticker=005930&limit=5
```

한국은 Google News RSS + Naver 뉴스 검색 API, 미국은 Yahoo Finance RSS + Google News RSS에서 수집한다. 출처별 `limit`건을 조회해 URL 중복 제거 후 최대 `limit × 2`건을 저장한다. 발행 시각을 KST로 정규화하고 **수집 실행일 당일 뉴스만** 저장한다. Naver 키 미설정 시 한국은 Google News RSS만 사용한다.

### 7.4 공시

```http
GET  /api/market-data/disclosures?market=KOSPI&ticker=005930
POST /api/market-data/disclosures/sync?market=KOSPI&limit=20
```

한국: DART API Key 필요. 미국: SEC EDGAR Atom feed (키 불필요).

### 7.5 매크로

```http
GET  /api/market-data/macro-observations?seriesId=DGS10&limit=10
POST /api/market-data/macro-observations/sync?seriesId=FEDFUNDS&limit=5
```

FRED 공개 CSV. `seriesId` 없으면 기본 핵심 지표 묶음 수집.

### 7.6 펀더멘털

```http
GET  /api/market-data/fundamentals?market=NASDAQ&ticker=AAPL
POST /api/market-data/fundamentals/sync?market=NASDAQ&ticker=AAPL
```

미국: SEC Company Facts. 한국: KIS 현재가 + DART 주요계정. `ticker` 필수.

---

## 8. 유니버스 API ✅

```
/api/universe
```

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/universe` | 시장 후보군 조회 (`market`, `tradable`, `minMarketCap`, `minAvgTurnover`, `minLastPrice`) |
| POST | `/api/universe/sync/kr-symbols` | KIND 상장법인 다운로드로 KOSPI·KOSDAQ 심볼 동기화 |
| POST | `/api/universe/sync/us-symbols` | NASDAQ Trader 공개 파일로 NASDAQ·NYSE 심볼 동기화 |
| POST | `/api/universe/sync/us-prices` | Stooq 공개 CSV로 미국 후보군 최근 시세 갱신 |

심볼 동기화는 ticker 목록만 저장한다. 가격·시총은 이후 `daily-prices/sync` 단계에서 채워진다.

---

## 9. 백테스트 API ✅ GET·simulate / 🔒 POST(저장)

```
/api/backtests
```

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/backtests` | ✅ | 실행 목록 조회 |
| GET | `/api/backtests/{id}` | ✅ | 단건 조회 |
| POST | `/api/backtests/simulate` | ✅ | 일봉 기반 룰 백테스트 실행 후 결과 저장 |
| POST | `/api/backtests` | 🔒 | 외부 결과 저장 (AutoResearch 내부 호출) |

### 9.1 백테스트 시뮬레이션

```http
POST /api/backtests/simulate
```

```json
{
  "strategy": "ma20-breakout-v0",
  "market": "KOSPI",
  "periodFrom": "2024-01-01",
  "periodTo": "2026-06-01",
  "maxTickers": 30,
  "holdingDays": 20,
  "targetPct": 3.0,
  "stopPct": 2.0
}
```

20일 이동평균 돌파 룰 기반. 저장된 `price_daily` 사용.

---

## 10. AutoResearch API ✅ GET·auto / 🔒 POST(저장)

```
/api/autoresearch
```

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/autoresearch/runs` | ✅ | 실험 목록 (`?jobRunId=uuid`) |
| GET | `/api/autoresearch/runs/{id}` | ✅ | 단건 조회 |
| POST | `/api/autoresearch/runs/auto` | ✅ | 가중치 후보 자동 생성 → 백테스트 → champion 갱신 |
| POST | `/api/autoresearch/runs` | 🔒 | 실험 결과 저장 (내부 호출) |
| GET | `/api/autoresearch/strategies` | ✅ | 전략 버전 목록 (`?champion=true`) |
| GET | `/api/autoresearch/strategies/{id}` | ✅ | 단건 조회 |
| POST | `/api/autoresearch/strategies` | 🔒 | 전략 버전 저장 (내부 호출) |

### 10.1 자동 실험 (`/runs/auto`)

scoring weights 변형 후보를 생성해 백테스트하고, 기존 champion보다 성과가 좋으면 champion 전략 버전을 저장한다. `AutoresearchJob`이 야간 자동 실행하지만 수동 호출도 가능하다.

---

## 11. 알림 로그 API ✅ GET / 🔒 POST

```
/api/notifications/logs
```

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/notifications/logs?status=SENT` | ✅ | 목록 조회 (`SENT` / `FAILED` / `SKIPPED`) |
| GET | `/api/notifications/logs/{id}` | ✅ | 단건 조회 |
| POST | `/api/notifications/logs` | 🔒 | 발송 이력 저장 (NotificationService 내부 호출) |

---

## 12. Codex 호출 로그 API ✅ GET / 🔒 POST

```
/api/codex/calls
```

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/codex/calls?caller=BRIEF_KR` | ✅ | 목록 조회 |
| GET | `/api/codex/calls/{id}` | ✅ | 단건 조회 |
| POST | `/api/codex/calls` | 🔒 | 호출 이력 저장 (CodexClient 내부 호출) |

프롬프트 원문은 저장하지 않고 해시·길이·성공 여부·소요 시간만 저장한다.

---

## 13. 데일리 브리프 API ✅ GET / 🔒 POST

```
/api/briefs
```

| Method | URL | Swagger | 설명 |
|---|---|---|---|
| GET | `/api/briefs?marketTrack=KRX` | ✅ | 목록 조회 (`KRX` / `US` / `US_CLOSE`) |
| GET | `/api/briefs/{id}` | ✅ | 단건 조회 |
| POST | `/api/briefs` | 🔒 | 브리프 저장 (KrxPreOpenJob·UsPreOpenJob 내부 호출) |

---

## 14. Swagger 숨김 전용 엔드포인트

### 14.1 내부 플로우 전용 🔒

HTTP 접근은 가능하지만 Swagger에 미노출. 모두 Spring Service 직접 호출로 동작하므로 HTTP로 별도 호출 불필요.

| 엔드포인트 | 호출자 |
|---|---|
| `POST /api/recommendations` | KrxPreOpenJob · UsPreOpenJob |
| `PUT /api/recommendations/{id}/status` | ExitMonitorJob |
| `POST /api/evaluations` | ExitMonitorJob |
| `POST /api/backtests` | AutoResearchJob |
| `POST /api/autoresearch/runs` | AutoResearchJob |
| `POST /api/autoresearch/strategies` | AutoResearchJob |
| `POST /api/briefs` | KrxPreOpenJob · UsPreOpenJob |
| `POST /api/codex/calls` | CodexClient |
| `POST /api/notifications/logs` | NotificationService |

### 14.2 개발 전용 🚫

HTTP 접근은 가능하지만 운영 환경에서 호출 금지. 추후 `@Profile("dev")`로 prod 미등록 예정.

| Base URL | 설명 |
|---|---|
| `POST /api/dev/recommendations/generate` | 개발용 더미 추천 생성 |
| `POST /api/dev/universe/seed` | 개발용 유니버스 시드 저장 |
| `POST /api/dev/brief/generate` | 개발용 브리프 생성 |
| `POST /api/dev/notifications/test` | Telegram 테스트 발송 |
| `GET/POST/PUT /api/instruments` | 개발용 종목 seed 관리 |
| `GET /api/features` | feature 점수 직접 조회 |
| `GET/POST /api/predictions` | 가격 예측 조회·생성 |

---

## 15. 스케줄러 Job 전체 목록

| Job | 스케줄 | 설명 |
|---|---|---|
| `KrxPreOpenJob` | 평일 08:30 KST | 한국 유니버스 동기화 + 일봉 증분 + 추천 생성 + 알림 |
| `UsPreOpenJob` | 평일 22:00/23:00 KST | 미국 유니버스·시세 동기화 + 일봉 증분 + 추천 생성 + 알림 |
| `DailyPriceBackfillJob` (KRX) | 평일 18:10 KST | KOSPI·KOSDAQ 일봉 히스토리 백필 |
| `DailyPriceBackfillJob` (US) | 화~토 07:20 KST | NASDAQ·NYSE 일봉 히스토리 백필 |
| `ExitMonitorJob` | 평일 장중 5분 주기 | 목표가·손절가·만료 기준 자동 평가 + 추천 상태 변경 |
| `FeatureSnapshotJob` | 평일 22:00 KST | 전 종목 PIT feature 스냅샷 + T+5·T+20 forward return 백필 |
| `MarketDataCollectionJob` | 별도 스케줄 | 뉴스·공시·매크로·펀더멘털 자동 수집 |
| `UsCloseSummaryJob` | 미국 마감 후 | US 마감 요약 알림 |
| `AutoResearchJob` | 야간 | scoring weights 자동 실험 + champion 전략 갱신 |

수동 실행: `POST /api/ops/jobs/{jobName}/trigger` (krx-preopen · us-preopen · backfill-kr · backfill-us · feature-snapshot)
