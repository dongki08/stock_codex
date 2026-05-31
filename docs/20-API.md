# Stock Advisor Backend API 명세

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 20(API) · 상태 🟢 현행 · 빠른 요약은 [21-API-QUICK.md](21-API-QUICK.md)
>
> 기준: Spring Boot 백엔드 현재 구현 API  
> DB 전제: MSSQL  
> Swagger: `http://localhost:8083/swagger-ui.html`  
> 공통 응답: `ResultDto<?>`

## 1. 공통 규칙

### 1.1 응답 포맷

모든 API는 성공/실패 모두 동일한 응답 래퍼를 사용한다.

성공 응답:

```json
{
  "code": 200,
  "data": {}
}
```

성공 응답 중 반환 데이터가 없는 경우:

```json
{
  "code": 200
}
```

실패 응답:

```json
{
  "code": 400,
  "error_message": "입력값이 올바르지 않습니다."
}
```

### 1.2 날짜/시간 포맷

`LocalDate`:

```text
2026-05-20
```

`LocalDateTime`:

```text
2026-05-20T08:30:00
```

### 1.3 JSON 문자열 필드

MSSQL 기준으로 JSON 성격의 값은 `nvarchar(max)` 문자열로 저장한다. 요청 시 JSON 객체가 아니라 JSON 문자열을 넣는다.

예:

```json
{
  "signalsJson": "{\"rsi\":45,\"macd\":\"golden_cross\"}"
}
```

## 2. 관리자 설정 API

관리자 페이지가 사용할 런타임 설정과 감사 로그 API다.

Base URL:

```text
/api/admin
```

### 2.1 설정 목록 조회

```http
GET /api/admin/settings
```

설정 키 기준 오름차순으로 전체 설정을 조회한다.

응답 `data`:

```json
[
  {
    "key": "recommendation.short.count",
    "valueJson": "{\"value\":3,\"min\":1,\"max\":10}",
    "description": "단기 추천 개수",
    "updatedBy": "system"
  }
]
```

### 2.2 설정 단건 조회

```http
GET /api/admin/settings/{key}
```

Path:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| key | String | Y | 설정 키 |

대표 에러:

| code | message |
|---:|---|
| 404 | 설정을 찾을 수 없습니다. |

### 2.3 설정 수정

```http
PUT /api/admin/settings/{key}
```

Path:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| key | String | Y | 설정 키 |

Request:

```json
{
  "valueJson": "{\"value\":5}",
  "actor": "admin"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| valueJson | 필수, 유효한 JSON 문자열 |
| actor | 필수 |

대표 에러:

| code | message |
|---:|---|
| 400 | 설정 JSON 형식이 올바르지 않습니다. |
| 404 | 설정을 찾을 수 없습니다. |

### 2.4 기본 설정 초기화

```http
POST /api/admin/settings/reset
```

§20 관리자 페이지 기본 설정값을 생성/덮어쓴다.

포함 설정:

| 영역 | 주요 키 |
|---|---|
| 추천 | `recommendation.short.count`, `recommendation.long.count`, `recommendation.market.enabled` |
| 유니버스 | `recommendation.marketcap.kr.min`, `recommendation.turnover.us.min`, `recommendation.excluded.sectors` |
| 알림 | `notification.krx.preopen.offsetMinutes`, `notification.us.preopen.offsetMinutes`, `notification.channel.priority` |
| 손절 | `exit.polling.intervalMinutes`, `exit.riskBand.percent`, `exit.codex.confirmLimitPerTickerDaily` |
| 백테스트 | `backtest.period.years`, `backtest.walkForward.days`, `backtest.slippage.percent` |
| Codex | `codex.daily.callLimit`, `codex.daily.budgetUsd`, `codex.profile` |
| AutoResearch | `autoresearch.enabled`, `autoresearch.targetIterations`, `autoresearch.rollbackValidationDays` |
| 운영 | `operation.dbBackup.enabled`, `operation.dbBackup.cron` |

### 2.5 감사 로그 조회

```http
GET /api/admin/audit-logs
```

응답 `data`:

```json
[
  {
    "id": 1,
    "actor": "admin",
    "action": "UPDATE_SETTING:recommendation.short.count",
    "beforeJson": "{\"value\":3}",
    "afterJson": "{\"value\":5}"
  }
]
```

## 3. 종목 마스터 API

추천, 예측, 백테스트 대상 종목을 관리한다.

Base URL:

```text
/api/instruments
```

### 3.1 종목 목록 조회

```http
GET /api/instruments?market=KOSPI
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| market | String | N | KOSPI/KOSDAQ/NYSE/NASDAQ |

응답 `data`:

```json
[
  {
    "ticker": "005930",
    "market": "KOSPI",
    "name": "삼성전자",
    "sector": "반도체",
    "enabled": true
  }
]
```

### 3.2 종목 단건 조회

```http
GET /api/instruments/{ticker}
```

대표 에러:

| code | message |
|---:|---|
| 404 | 종목을 찾을 수 없습니다. |

### 3.3 종목 등록

```http
POST /api/instruments
```

Request:

```json
{
  "ticker": "005930",
  "market": "KOSPI",
  "name": "삼성전자",
  "sector": "반도체",
  "enabled": true
}
```

Validation:

| 필드 | 조건 |
|---|---|
| ticker | 필수 |
| market | 필수 |
| name | 필수 |
| enabled | 필수 |

대표 에러:

| code | message |
|---:|---|
| 409 | 이미 등록된 종목입니다. |

### 3.4 종목 수정

```http
PUT /api/instruments/{ticker}
```

Request:

```json
{
  "market": "KOSPI",
  "name": "삼성전자",
  "sector": "반도체",
  "enabled": true
}
```

## 4. 추천 API

추천 엔진 결과를 저장하고 조회한다.

Base URL:

```text
/api/recommendations
```

### 4.1 추천 목록 조회

```http
GET /api/recommendations?status=OPEN&ticker=005930
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| status | String | N | OPEN/CLOSED/EXPIRED |
| ticker | String | N | 종목 코드. ticker가 있으면 status보다 우선 적용 |

응답 `data`:

```json
[
  {
    "id": 1,
    "ticker": "005930",
    "market": "KOSPI",
    "term": "SHORT",
    "entryPrice": 78400.0000,
    "targetPrice": 82500.0000,
    "stopPrice": 76200.0000,
    "expectedExitAt": "2026-05-25",
    "confidence": 78,
    "signalsJson": "{\"rsi\":45,\"foreignBuyDays\":3}",
    "modelVersion": "rule-v0",
    "generatedAt": "2026-05-20T08:30:00",
    "status": "OPEN"
  }
]
```

### 4.2 추천 단건 조회

```http
GET /api/recommendations/{id}
```

대표 에러:

| code | message |
|---:|---|
| 404 | 추천을 찾을 수 없습니다. |

### 4.3 추천 생성

```http
POST /api/recommendations
```

Request:

```json
{
  "ticker": "005930",
  "market": "KOSPI",
  "term": "SHORT",
  "entryPrice": 78400,
  "targetPrice": 82500,
  "stopPrice": 76200,
  "expectedExitAt": "2026-05-25",
  "confidence": 78,
  "signalsJson": "{\"rsi\":45,\"foreignBuyDays\":3}",
  "modelVersion": "rule-v0",
  "generatedAt": "2026-05-20T08:30:00"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| ticker | 필수, 현재 개발용 수동 입력에서는 등록된 후보 종목이어야 함 |
| market | 필수 |
| term | 필수, SHORT 또는 LONG |
| entryPrice | 필수, 0 초과 |
| targetPrice | 필수, 0 초과 |
| stopPrice | 필수, 0 초과 |
| expectedExitAt | 필수 |
| confidence | 필수, 0~100 |
| signalsJson | 필수, 유효한 JSON 문자열 |
| modelVersion | 필수 |

생성 시 `status`는 항상 `OPEN`으로 저장된다.

대표 에러:

| code | message |
|---:|---|
| 400 | 보유 기간 구분은 SHORT 또는 LONG이어야 합니다. |
| 400 | 시그널 JSON 형식이 올바르지 않습니다. |
| 404 | 등록되지 않은 종목은 추천할 수 없습니다. |

### 4.4 추천 상태 수정

```http
PUT /api/recommendations/{id}/status
```

Request:

```json
{
  "status": "CLOSED"
}
```

허용 상태:

| 값 | 설명 |
|---|---|
| OPEN | 진행 중 |
| CLOSED | 평가 완료 또는 수동 종료 |
| EXPIRED | 기간 만료 |

## 5. 평가 API

추천의 실제 성과를 저장하고 조회한다.

Base URL:

```text
/api/evaluations
```

### 5.1 평가 목록 조회

```http
GET /api/evaluations?recommendationId=1
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| recommendationId | Long | N | 추천 ID |

### 5.2 평가 단건 조회

```http
GET /api/evaluations/{id}
```

### 5.3 평가 생성

```http
POST /api/evaluations
```

Request:

```json
{
  "recommendationId": 1,
  "actualExitPrice": 82500,
  "exitReason": "TARGET_HIT",
  "pnlPct": 5.2300,
  "drawdownPct": 1.1200,
  "hitTarget": true,
  "evaluatedAt": "2026-05-25T15:30:00"
}
```

허용 `exitReason`:

| 값 | 설명 |
|---|---|
| TARGET_HIT | 목표가 도달 |
| STOP_HIT | 손절가 이탈 |
| TIME_OUT | 예상 매도일 만료 |
| MANUAL_CLOSE | 수동 종료 |

대표 에러:

| code | message |
|---:|---|
| 400 | 청산 사유는 TARGET_HIT, STOP_HIT, TIME_OUT, MANUAL_CLOSE 중 하나여야 합니다. |
| 404 | 평가할 추천을 찾을 수 없습니다. |

## 6. 가격 예측 API

가격 예측 모델 산출물을 저장하고 조회한다.

Base URL:

```text
/api/predictions
```

### 6.1 가격 예측 목록 조회

```http
GET /api/predictions?ticker=005930
```

### 6.2 가격 예측 단건 조회

```http
GET /api/predictions/{id}
```

### 6.3 가격 예측 생성

```http
POST /api/predictions
```

Request:

```json
{
  "ticker": "005930",
  "horizonDays": 5,
  "predictedPrice": 82500,
  "modelVersion": "lightgbm-v0",
  "generatedAt": "2026-05-20T08:25:00"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| ticker | 필수, 현재 개발용 수동 입력에서는 등록된 후보 종목이어야 함 |
| horizonDays | 필수, 1 이상 |
| predictedPrice | 필수, 0 초과 |
| modelVersion | 필수 |

대표 에러:

| code | message |
|---:|---|
| 404 | 등록되지 않은 종목은 예측할 수 없습니다. |

## 7. 백테스트 API

백테스트 실행 이력과 지표를 저장한다.

Base URL:

```text
/api/backtests
```

### 7.1 백테스트 실행 목록 조회

```http
GET /api/backtests
```

### 7.2 백테스트 실행 단건 조회

```http
GET /api/backtests/{id}
```

### 7.3 백테스트 실행 저장

```http
POST /api/backtests
```

Request:

```json
{
  "strategy": "rule-v0",
  "periodFrom": "2021-05-20",
  "periodTo": "2026-05-20",
  "metricsJson": "{\"roi\":12.3,\"hitRate\":57.8,\"mdd\":-8.4,\"sharpe\":1.12}"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| strategy | 필수 |
| periodFrom | 필수 |
| periodTo | 필수, periodFrom보다 빠를 수 없음 |
| metricsJson | 필수, 유효한 JSON 문자열 |

대표 에러:

| code | message |
|---:|---|
| 400 | 백테스트 시작일은 종료일보다 늦을 수 없습니다. |
| 400 | 지표 JSON 형식이 올바르지 않습니다. |

## 8. 알림 로그 API

Telegram/Kakao 발송 결과를 기록한다.

Base URL:

```text
/api/notifications/logs
```

### 8.1 알림 로그 목록 조회

```http
GET /api/notifications/logs?status=SENT
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| status | String | N | SENT/FAILED/SKIPPED |

### 8.2 알림 로그 단건 조회

```http
GET /api/notifications/logs/{id}
```

### 8.3 알림 로그 저장

```http
POST /api/notifications/logs
```

Request:

```json
{
  "channel": "TELEGRAM",
  "payloadHash": "e3b0c44298fc1c149afbf4c8996fb924...",
  "sentAt": "2026-05-20T08:30:03",
  "status": "SENT",
  "errorMessage": null
}
```

Validation:

| 필드 | 조건 |
|---|---|
| channel | 필수 |
| payloadHash | 필수 |
| status | 필수 |

### 8.4 Telegram 테스트 발송

```http
POST /api/dev/notifications/test?message=Stock%20Advisor%20Telegram%20test
```

Response:

```json
{
  "code": 200,
  "data": {
    "sent": true,
    "devMode": false,
    "statusCode": 200,
    "errorMessage": null,
    "message": "Stock Advisor Telegram test",
    "logId": 10
  },
  "error_message": null
}
```

`devMode=true`이면 `TELEGRAM_BOT_TOKEN=dev-placeholder` 로그 출력 모드다. 실제 발송 검증은 `devMode=false`, `sent=true`, `errorMessage=null`을 기준으로 본다. 실패 시 `errorMessage`에는 토큰/Chat ID 누락, Telegram API HTTP 오류, 네트워크 오류가 저장되고 같은 값이 `notification_log.error_message`에 남는다.

## 9. Codex 호출 로그 API

Codex CLI 호출 이력을 저장하고 조회한다. 프롬프트 원문은 저장하지 않고 해시와 길이만 저장하는 계약이다.

Base URL:

```text
/api/codex/calls
```

### 9.1 Codex 호출 로그 목록 조회

```http
GET /api/codex/calls?caller=BRIEF_KR
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| caller | String | N | BRIEF_KR/BRIEF_US/EXIT_CONFIRM/AR_PROPOSE 등 |

### 9.2 Codex 호출 로그 단건 조회

```http
GET /api/codex/calls/{id}
```

### 9.3 Codex 호출 로그 저장

```http
POST /api/codex/calls
```

Request:

```json
{
  "caller": "BRIEF_KR",
  "promptHash": "d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2",
  "promptLen": 2400,
  "responseLen": 1800,
  "toolsUsedJson": "{\"shell\":false,\"web\":false}",
  "durationMs": 11200,
  "succeeded": true,
  "errorMessage": null,
  "calledAt": "2026-05-20T08:25:00"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| caller | 필수 |
| promptHash | 필수 |
| promptLen | 필수, 0 이상 |
| toolsUsedJson | 선택, 입력 시 유효한 JSON 문자열 |
| succeeded | 필수 |

대표 에러:

| code | message |
|---:|---|
| 400 | 사용 도구 JSON 형식이 올바르지 않습니다. |

## 10. 데일리 브리프 API

KRX/US/US_CLOSE 브리프 초안과 품질 점수를 저장한다.

Base URL:

```text
/api/briefs
```

### 10.1 데일리 브리프 목록 조회

```http
GET /api/briefs?marketTrack=KRX
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| marketTrack | String | N | KRX/US/US_CLOSE |

### 10.2 데일리 브리프 단건 조회

```http
GET /api/briefs/{id}
```

### 10.3 데일리 브리프 저장

```http
POST /api/briefs
```

Request:

```json
{
  "marketTrack": "KRX",
  "briefMd": "## KR-PreOpen 브리핑\n- S&P +0.4%\n- 반도체 섹터 강세",
  "draftNo": 1,
  "coverage": 0.920,
  "hallucinationFlags": 0,
  "llmModel": "gpt-5",
  "generatedAt": "2026-05-20T08:26:00"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| marketTrack | 필수 |
| briefMd | 필수 |
| draftNo | 필수 |

## 11. AutoResearch API

야간 실험 반복 이력과 전략 버전을 저장한다.

Base URL:

```text
/api/autoresearch
```

### 11.1 AutoResearch 실행 목록 조회

```http
GET /api/autoresearch/runs?jobRunId=9ef7b45d-6a8a-4ec8-89e9-20708d0b7d01
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| jobRunId | UUID | N | 야간 작업 실행 UUID |

### 11.2 AutoResearch 실행 단건 조회

```http
GET /api/autoresearch/runs/{id}
```

### 11.3 AutoResearch 실행 저장

```http
POST /api/autoresearch/runs
```

Request:

```json
{
  "jobRunId": "9ef7b45d-6a8a-4ec8-89e9-20708d0b7d01",
  "iterNo": 1,
  "parentSha": "abc123",
  "proposalSha": "def456",
  "diffSummary": "RSI weight increased from 0.20 to 0.25",
  "metricName": "walk_forward_sharpe",
  "metricValue": 1.2300,
  "championMetric": 1.1100,
  "decision": "KEEP",
  "durationMs": 280000,
  "startedAt": "2026-05-24T03:00:00",
  "endedAt": "2026-05-24T03:04:40"
}
```

Validation:

| 필드 | 조건 |
|---|---|
| jobRunId | 필수 |
| iterNo | 필수 |
| decision | 필수 |

권장 `decision` 값:

| 값 | 설명 |
|---|---|
| KEEP | 챔피언보다 개선되어 채택 |
| DISCARD | 개선 실패로 폐기 |
| ERROR | 실험 실패 |

### 11.4 전략 버전 목록 조회

```http
GET /api/autoresearch/strategies?champion=true
```

Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| champion | Boolean | N | 챔피언 전략 여부 |

### 11.5 전략 버전 단건 조회

```http
GET /api/autoresearch/strategies/{id}
```

### 11.6 전략 버전 저장

```http
POST /api/autoresearch/strategies
```

Request:

```json
{
  "semver": "v1.0.0",
  "gitSha": "def456",
  "metricValue": 1.2300,
  "promotedAt": "2026-05-24T03:05:00",
  "champion": true
}
```

Validation:

| 필드 | 조건 |
|---|---|
| semver | 필수 |
| gitSha | 필수 |
| metricValue | 필수 |
| champion | 필수 |

## 12. 시장 데이터 수집

뉴스/공시/매크로 데이터를 저장하고 조회한다. 본문 전문은 저장하지 않고 제목, 링크, 메타데이터 중심으로 저장한다.

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/market-data/news` | 저장된 뉴스 조회 |
| POST | `/api/market-data/news/sync` | RSS 뉴스 동기화 |
| GET | `/api/market-data/disclosures` | 저장된 공시 조회 |
| POST | `/api/market-data/disclosures/sync` | DART/SEC 공시 동기화 |
| GET | `/api/market-data/macro-observations` | 저장된 FRED 매크로 관측값 조회 |
| POST | `/api/market-data/macro-observations/sync` | FRED 매크로 관측값 동기화 |
| GET | `/api/market-data/fundamentals` | 저장된 펀더멘털 지표 조회 |
| POST | `/api/market-data/fundamentals/sync` | SEC Company Facts(US) / KIS 현재가 + DART 주요계정(KR) 펀더멘털 동기화 |

공통 Query:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| market | String | N | `KOSPI`, `KOSDAQ`, `NASDAQ`, `NYSE`, `ALL` |
| ticker | String | N | 종목 코드. 뉴스/공시 조회 및 동기화에 사용 |
| limit | Integer | N | 조회/동기화 개수 제한 |
| seriesId | String | N | 매크로 지표 코드. 예: `DGS10`, `FEDFUNDS`, `CPIAUCSL` |

펀더멘털 동기화는 `ticker`가 필요하다. 미국 시장은 SEC Company Facts, 한국 시장은 KIS 현재가와 DART 단일회사 주요계정을 사용한다.

설정:

| 설정 | 설명 |
|---|---|
| `DART_API_KEY` | 한국 DART 공시 수집용. 없으면 DART 동기화는 빈 결과를 반환 |
| `SEC_USER_AGENT` | SEC EDGAR 요청 User-Agent |

## 13. 통계 API

종료 추천의 사후 성과와 OPEN 추천의 페이퍼트레이딩 미실현 손익을 조회한다.

Base URL:

```text
/api/stats
```

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/stats/summary` | 전체/종료/진행/만료 개수, hit rate, 평균/누적 손익률, MDD 조회 |
| GET | `/api/stats/daily` | 일별 평가 손익과 누적 손익 조회 |
| GET | `/api/stats/by-strategy` | 전략 버전별 평가 성과 조회 |
| GET | `/api/stats/paper-trading` | OPEN 추천의 최신 일봉 기준 미실현 손익과 비중 반영 손익 조회 |

### 13.1 페이퍼트레이딩 모니터링

```http
GET /api/stats/paper-trading
```

응답 `data` 주요 필드:

| 필드 | 타입 | 설명 |
|---|---|---|
| openCount | Integer | OPEN 추천 수 |
| pricedCount | Integer | 최신 `price_daily` 종가가 있어 계산 가능한 추천 수 |
| avgUnrealizedPnlPct | Decimal | 단순 평균 미실현 손익률 |
| weightedUnrealizedPnlPct | Decimal | `signalsJson.positionWeightPct`를 반영한 미실현 포트폴리오 손익률 |
| totalWeightPct | Decimal | 계산 가능한 추천의 총 비중 |
| targetTouchCount | Integer | 최신 종가가 목표가 이상인 추천 수 |
| stopTouchCount | Integer | 최신 종가가 손절가 이하인 추천 수 |
| positions | Array | 추천별 현재가, 거래일, 비중, 손익률, 목표/손절 이격률, 가격 상태 |

`positionWeightPct`가 없는 과거 추천은 동일가중 fallback을 사용하되 종목당 20% 상한을 적용한다. 최신 가격이 없으면 해당 포지션은 `priceStatus=NO_PRICE`로 내려가고 요약 손익 계산에서는 제외된다.

## 14. 프론트 연동 권장 순서

관리자 UI는 API 계약을 기준으로 다음 순서로 붙이는 것을 권장한다.

1. `POST /api/admin/settings/reset`으로 기본 설정 생성
2. `GET /api/admin/settings`로 관리자 설정 화면 렌더링
3. `PUT /api/admin/settings/{key}`로 개별 설정 저장
4. `GET /api/instruments`로 개발용/수동 보정 종목 관리
5. `GET /api/recommendations`로 오늘의 추천/이력 화면 구성
6. `GET /api/evaluations`로 추천 성과 화면 구성
7. `GET /api/stats/paper-trading`으로 OPEN 추천 페이퍼트레이딩 상태 구성
8. `GET /api/backtests`, `GET /api/autoresearch/strategies`로 백테스트/전략 탭 구성

## 15. 현재 구현 범위와 다음 구현 대상

현재 구현된 범위:

| 영역 | 상태 |
|---|---|
| API 계약 | 구현 |
| Swagger 문서 | 구현 |
| MSSQL JPA 엔티티 | 구현 |
| DTO 요청/응답 | 구현 |
| 기본 Validation | 구현 |
| CRUD성 저장/조회 서비스 | 구현 |

다음 구현 대상:

| 영역 | 내용 |
|---|---|
| 실제 데이터 수집 | 뉴스 RSS, DART/SEC 공시, FRED 매크로, SEC/KIS/DART 펀더멘털 수집과 룰 기반 감성 점수 1차 구현 |
| 스케줄러 | KRX/US 프리오픈, US 마감, ExitMonitor, AutoResearch Job |
| 추천 엔진 | FeatureBuilder, RecommendationEngine, PricePredictor |
| 알림 발송 | Telegram/Kakao 실제 발송 클라이언트 |
| Codex 실행 | Codex CLI 프로세스 호출, budget guard, fallback |
| 통계 API | ROI, Hit Rate, MDD, Sharpe 집계 API |
| 보안 | BasicAuth, 관리자 비밀번호, rate limit |
