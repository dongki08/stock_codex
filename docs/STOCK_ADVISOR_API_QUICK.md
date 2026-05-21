# Stock Advisor API 한눈에 보기

> 목적: 프론트 개발과 Swagger 테스트 시 빠르게 보는 요약본  
> 상세 문서: `docs/STOCK_ADVISOR_API.md`  
> 공통 응답: 성공 `{"code":200,"data":...}` / 실패 `{"code":에러코드,"error_message":"메시지"}`

## 1. 전체 기능 요약

| 기능 | API | 이 기능이 하는 일 |
|---|---|---|
| 관리자 설정 | `/api/admin/settings` | 추천 개수, 알림 시간, 손절 설정, Codex 한도 같은 런타임 설정을 조회/수정한다. |
| 감사 로그 | `/api/admin/audit-logs` | 관리자가 설정을 바꾼 이력을 확인한다. |
| 종목 관리 | `/api/instruments` | 실제 수집 전 개발용 seed 또는 자동 후보군 수동 보정용 종목을 관리한다. |
| 추천 관리 | `/api/recommendations` | 추천 엔진이 만든 매수 후보, 목표가, 손절가, 신뢰도를 저장하고 조회한다. |
| 평가 관리 | `/api/evaluations` | 추천이 실제로 성공했는지, 손익률이 얼마인지 사후 결과를 저장한다. |
| 가격 예측 | `/api/predictions` | 모델이 예측한 미래 가격 산출물을 저장하고 추천 근거로 활용한다. |
| 백테스트 | `/api/backtests` | 과거 기간에 전략을 돌린 결과와 성과 지표를 저장한다. |
| 알림 로그 | `/api/notifications/logs` | Telegram/Kakao 알림 발송 성공/실패 이력을 저장한다. |
| Codex 로그 | `/api/codex/calls` | Codex CLI 호출 횟수, 성공 여부, 지연 시간을 기록한다. |
| 데일리 브리프 | `/api/briefs` | Codex가 만든 KRX/US 장전 브리프와 품질 점수를 저장한다. |
| AutoResearch | `/api/autoresearch` | 야간 자동 실험 결과와 챔피언 전략 버전을 기록한다. |
| 개발용 자동 생성 | `/api/dev/recommendations/generate` | 실제 추천 엔진 전 단계에서 개발용 후보군 기반 예측/추천 테스트 데이터를 자동 생성한다. |

## 2. 관리자 설정

### 기능 설명

관리자 페이지에서 바꿀 수 있는 모든 설정을 DB에 저장한다. 프론트는 이 API로 설정 목록을 받아 화면을 그리고, 저장 버튼을 누르면 특정 key의 `valueJson`만 수정하면 된다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/admin/settings` | 전체 설정 조회 |
| GET | `/api/admin/settings/{key}` | 설정 단건 조회 |
| PUT | `/api/admin/settings/{key}` | 설정 수정 |
| POST | `/api/admin/settings/reset` | 기본 설정 생성/초기화 |
| GET | `/api/admin/audit-logs` | 설정 변경 이력 조회 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| key | String | 설정을 구분하는 고유 키. 예: `recommendation.short.count` |
| valueJson | String | 실제 설정값 JSON 문자열. MSSQL에서는 문자열로 저장한다. |
| description | String | 설정 설명. 관리자 UI 라벨로 사용 가능하다. |
| updatedBy | String | 마지막 수정자. 기본값은 `system` 또는 `admin`이다. |
| actor | String | 설정을 수정한 사용자명. 감사 로그에 남는다. |

### 예시

```json
{
  "valueJson": "{\"value\":5}",
  "actor": "admin"
}
```

## 3. 종목 관리

### 기능 설명

최종 추천 플로우는 사용자가 종목을 직접 등록하는 방식이 아니라, 시장 데이터를 수집해 조건에 부합하는 후보군을 자동으로 만든다. 이 API는 실제 수집기가 붙기 전 개발용 seed, 또는 자동 후보군에 대한 수동 보정 용도로 사용한다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/instruments` | 종목 목록 조회 |
| GET | `/api/instruments?market=KOSPI` | 시장별 종목 조회 |
| GET | `/api/instruments/{ticker}` | 종목 단건 조회 |
| POST | `/api/instruments` | 종목 등록 |
| PUT | `/api/instruments/{ticker}` | 종목 수정 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| ticker | String | 종목 코드. 한국은 `005930`, 미국은 `AAPL` 같은 값이다. |
| market | String | 시장 구분. `KOSPI`, `KOSDAQ`, `NYSE`, `NASDAQ` 등이다. |
| name | String | 종목명. |
| sector | String | 섹터명. 예: 반도체, 금융, AI, 헬스케어. |
| enabled | Boolean | 개발용/수동 보정 추천 유니버스 포함 여부. `false`면 추천 대상에서 제외하는 용도다. |

### 예시

```json
{
  "ticker": "005930",
  "market": "KOSPI",
  "name": "삼성전자",
  "sector": "반도체",
  "enabled": true
}
```

## 4. 추천 관리

### 기능 설명

매일 생성되는 추천 결과를 저장한다. 단기/장기 추천, 진입가, 목표가, 손절가, 예상 매도일, 신뢰도, 추천 근거를 담는다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/recommendations` | 추천 전체 조회 |
| GET | `/api/recommendations?status=OPEN` | 상태별 추천 조회 |
| GET | `/api/recommendations?ticker=005930` | 종목별 추천 조회 |
| GET | `/api/recommendations/{id}` | 추천 단건 조회 |
| POST | `/api/recommendations` | 추천 생성 |
| PUT | `/api/recommendations/{id}/status` | 추천 상태 변경 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 추천 ID. |
| ticker | String | 추천 종목 코드. 최종 플로우에서는 자동 후보군에서 선별된다. |
| market | String | 시장 구분. |
| term | String | 추천 기간. `SHORT`는 단기, `LONG`은 장기다. |
| entryPrice | Decimal | 추천 기준 진입 가격. |
| targetPrice | Decimal | 목표 가격. |
| stopPrice | Decimal | 손절 가격. |
| expectedExitAt | Date | 예상 매도일. 예: `2026-05-25` |
| confidence | Integer | 추천 신뢰도. 0~100 범위다. |
| signalsJson | String | 추천 근거 JSON 문자열. RSI, MACD, 뉴스 점수 등을 담는 용도다. |
| modelVersion | String | 추천을 만든 모델/전략 버전. |
| generatedAt | DateTime | 추천 생성 시각. 없으면 서버 시간이 들어간다. |
| status | String | 추천 상태. `OPEN`, `CLOSED`, `EXPIRED` 중 하나다. |

### 예시

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
  "modelVersion": "rule-v0"
}
```

### 시장 데이터 수집

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/market-data/news` | 뉴스 제목/링크 조회 |
| POST | `/api/market-data/news/sync` | RSS 뉴스 동기화 |
| GET | `/api/market-data/disclosures` | 공시 메타데이터 조회 |
| POST | `/api/market-data/disclosures/sync` | DART/SEC 공시 동기화 |
| GET | `/api/market-data/macro-observations` | FRED 매크로 관측값 조회 |
| POST | `/api/market-data/macro-observations/sync` | FRED 매크로 관측값 동기화 |
| GET | `/api/market-data/fundamentals` | 펀더멘털 지표 조회 |
| POST | `/api/market-data/fundamentals/sync` | SEC Company Facts 펀더멘털 동기화 |

## 5. 평가 관리

### 기능 설명

추천이 실제로 맞았는지 기록한다. 목표가 도달, 손절, 시간 만료 같은 결과와 손익률을 저장해서 통계와 AutoResearch 개선에 쓴다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/evaluations` | 평가 전체 조회 |
| GET | `/api/evaluations?recommendationId=1` | 추천별 평가 조회 |
| GET | `/api/evaluations/{id}` | 평가 단건 조회 |
| POST | `/api/evaluations` | 평가 생성 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 평가 ID. |
| recommendationId | Long | 평가 대상 추천 ID. |
| actualExitPrice | Decimal | 실제 매도 가격. |
| exitReason | String | 청산 사유. `TARGET_HIT`, `STOP_HIT`, `TIME_OUT`, `MANUAL_CLOSE` 중 하나다. |
| pnlPct | Decimal | 손익률. 예: `5.2300` |
| drawdownPct | Decimal | 보유 중 최대 낙폭. |
| hitTarget | Boolean | 목표가 도달 여부. |
| evaluatedAt | DateTime | 평가 시각. 없으면 서버 시간이 들어간다. |

### 예시

```json
{
  "recommendationId": 1,
  "actualExitPrice": 82500,
  "exitReason": "TARGET_HIT",
  "pnlPct": 5.23,
  "drawdownPct": 1.12,
  "hitTarget": true
}
```

## 6. 가격 예측

### 기능 설명

모델이 예측한 가격을 저장한다. 추천 엔진은 이 데이터를 참고해서 목표가나 신뢰도를 만들 수 있다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/predictions` | 예측 전체 조회 |
| GET | `/api/predictions?ticker=005930` | 종목별 예측 조회 |
| GET | `/api/predictions/{id}` | 예측 단건 조회 |
| POST | `/api/predictions` | 예측 생성 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 예측 ID. |
| ticker | String | 예측 대상 종목 코드. |
| horizonDays | Integer | 며칠 뒤 가격을 예측했는지 나타낸다. |
| predictedPrice | Decimal | 예측 가격. |
| modelVersion | String | 예측 모델 버전. |
| generatedAt | DateTime | 예측 생성 시각. |

### 예시

```json
{
  "ticker": "005930",
  "horizonDays": 5,
  "predictedPrice": 82500,
  "modelVersion": "lightgbm-v0"
}
```

## 7. 백테스트

### 기능 설명

과거 기간에 전략을 돌린 결과를 저장한다. ROI, hitRate, MDD, Sharpe 같은 성과 지표는 `metricsJson`에 담는다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/backtests` | 백테스트 실행 목록 조회 |
| GET | `/api/backtests/{id}` | 백테스트 실행 단건 조회 |
| POST | `/api/backtests` | 백테스트 실행 결과 저장 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 백테스트 실행 ID. |
| strategy | String | 전략명 또는 모델 버전명. |
| periodFrom | Date | 백테스트 시작일. |
| periodTo | Date | 백테스트 종료일. |
| metricsJson | String | 성과 지표 JSON 문자열. |

### 예시

```json
{
  "strategy": "rule-v0",
  "periodFrom": "2021-05-20",
  "periodTo": "2026-05-20",
  "metricsJson": "{\"roi\":12.3,\"hitRate\":57.8,\"mdd\":-8.4,\"sharpe\":1.12}"
}
```

## 8. 알림 로그

### 기능 설명

Telegram/Kakao 알림 발송 결과를 저장한다. 발송 실패 추적과 운영 감사에 사용한다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/notifications/logs` | 알림 로그 전체 조회 |
| GET | `/api/notifications/logs?status=SENT` | 상태별 알림 로그 조회 |
| GET | `/api/notifications/logs/{id}` | 알림 로그 단건 조회 |
| POST | `/api/notifications/logs` | 알림 로그 저장 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 알림 로그 ID. |
| channel | String | 알림 채널. 예: `TELEGRAM`, `KAKAO` |
| payloadHash | String | 발송 내용의 해시. 본문 원문 대신 해시로 추적한다. |
| sentAt | DateTime | 발송 시각. |
| status | String | 발송 상태. 예: `SENT`, `FAILED`, `SKIPPED` |
| errorMessage | String | 실패 사유. 성공 시 null 가능. |

### 예시

```json
{
  "channel": "TELEGRAM",
  "payloadHash": "e3b0c44298fc1c149afbf4c8996fb924",
  "status": "SENT"
}
```

## 9. Codex 호출 로그

### 기능 설명

Codex CLI 호출 기록을 저장한다. 프롬프트 원문은 저장하지 않고 해시, 길이, 성공 여부, 지연 시간을 남긴다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/codex/calls` | Codex 호출 로그 전체 조회 |
| GET | `/api/codex/calls?caller=BRIEF_KR` | 호출자별 로그 조회 |
| GET | `/api/codex/calls/{id}` | 호출 로그 단건 조회 |
| POST | `/api/codex/calls` | 호출 로그 저장 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | Codex 호출 로그 ID. |
| caller | String | 호출 위치. 예: `BRIEF_KR`, `BRIEF_US`, `EXIT_CONFIRM`, `AR_PROPOSE` |
| promptHash | String | 프롬프트 해시. |
| promptLen | Integer | 프롬프트 길이. |
| responseLen | Integer | 응답 길이. |
| toolsUsedJson | String | 사용 도구 JSON 문자열. |
| durationMs | Integer | 호출 소요 시간, 밀리초 단위. |
| succeeded | Boolean | 호출 성공 여부. |
| errorMessage | String | 실패 메시지. |
| calledAt | DateTime | 호출 시각. |

### 예시

```json
{
  "caller": "BRIEF_KR",
  "promptHash": "d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2d2",
  "promptLen": 2400,
  "responseLen": 1800,
  "toolsUsedJson": "{\"shell\":false}",
  "durationMs": 11200,
  "succeeded": true
}
```

## 10. 데일리 브리프

### 기능 설명

Codex가 만든 시장 브리프를 저장한다. KRX 장전, US 장전, US 마감 요약 같은 본문과 품질 점수를 기록한다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/briefs` | 브리프 전체 조회 |
| GET | `/api/briefs?marketTrack=KRX` | 시장 트랙별 브리프 조회 |
| GET | `/api/briefs/{id}` | 브리프 단건 조회 |
| POST | `/api/briefs` | 브리프 저장 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 브리프 ID. |
| marketTrack | String | 시장 트랙. `KRX`, `US`, `US_CLOSE` 등이다. |
| briefMd | String | 브리프 본문. Markdown 형식이다. |
| draftNo | Integer | 초안 번호. |
| coverage | Decimal | 필수 정보 커버리지 점수. 0~1 사이 값으로 쓰는 것을 권장한다. |
| hallucinationFlags | Integer | 수치 검증 실패 같은 환각 플래그 수. |
| llmModel | String | 사용한 LLM 모델명. |
| generatedAt | DateTime | 생성 시각. |

### 예시

```json
{
  "marketTrack": "KRX",
  "briefMd": "## KR-PreOpen\n- 반도체 섹터 강세",
  "draftNo": 1,
  "coverage": 0.92,
  "hallucinationFlags": 0,
  "llmModel": "gpt-5"
}
```

## 11. AutoResearch

### 기능 설명

야간 자동 실험 결과와 챔피언 전략 버전을 저장한다. 어떤 실험을 채택했고 폐기했는지 기록해서 전략 개선 이력을 남긴다.

### API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/autoresearch/runs` | 실험 실행 목록 조회 |
| GET | `/api/autoresearch/runs?jobRunId={uuid}` | 특정 야간 작업의 실험 조회 |
| GET | `/api/autoresearch/runs/{id}` | 실험 실행 단건 조회 |
| POST | `/api/autoresearch/runs` | 실험 실행 결과 저장 |
| GET | `/api/autoresearch/strategies` | 전략 버전 목록 조회 |
| GET | `/api/autoresearch/strategies?champion=true` | 챔피언 전략 조회 |
| GET | `/api/autoresearch/strategies/{id}` | 전략 버전 단건 조회 |
| POST | `/api/autoresearch/strategies` | 전략 버전 저장 |

### 실험 실행 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 실험 실행 ID. |
| jobRunId | UUID | 야간 AutoResearch 작업 단위 ID. |
| iterNo | Integer | 반복 번호. |
| parentSha | String | 실험 기준 커밋 SHA. |
| proposalSha | String | 실험 제안 커밋 SHA. |
| diffSummary | String | 실험 변경 요약. |
| metricName | String | 평가 지표명. 예: `walk_forward_sharpe` |
| metricValue | Decimal | 이번 실험의 지표 값. |
| championMetric | Decimal | 기존 챔피언의 지표 값. |
| decision | String | 실험 결정. `KEEP`, `DISCARD`, `ERROR` 권장. |
| durationMs | Integer | 실험 소요 시간. |
| startedAt | DateTime | 실험 시작 시각. |
| endedAt | DateTime | 실험 종료 시각. |

### 전략 버전 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | 전략 버전 ID. |
| semver | String | 전략 버전명. 예: `v1.0.0` |
| gitSha | String | 전략 파일이 채택된 커밋 SHA. |
| metricValue | Decimal | 전략 채택 기준 지표 값. |
| promotedAt | DateTime | 전략 승격 시각. |
| champion | Boolean | 현재 챔피언 전략 여부. |

## 12. 프론트에서 먼저 붙이면 좋은 순서

| 순서 | 화면/기능 | 사용할 API |
|---:|---|---|
| 1 | 관리자 설정 초기화 | `POST /api/admin/settings/reset` |
| 2 | 관리자 설정 화면 | `GET /api/admin/settings`, `PUT /api/admin/settings/{key}` |
| 3 | 개발용/수동 보정 종목 관리 화면 | `GET/POST/PUT /api/instruments` |
| 4 | 오늘의 추천 화면 | `GET /api/recommendations?status=OPEN` |
| 5 | 추천 상세/평가 화면 | `GET /api/recommendations/{id}`, `GET /api/evaluations?recommendationId={id}` |
| 6 | 백테스트 화면 | `GET /api/backtests` |
| 7 | AutoResearch 화면 | `GET /api/autoresearch/runs`, `GET /api/autoresearch/strategies` |
| 8 | 운영 로그 화면 | `GET /api/notifications/logs`, `GET /api/codex/calls` |

## 13. 개발용 자동 추천 생성

### 기능 설명

실제 시세 수집과 추천 엔진이 붙기 전, 개발용 후보군을 기준으로 예측과 추천을 자동 생성한다. 프론트 추천 목록 화면 개발이나 API 플로우 확인에 쓰는 개발용 기능이다.

### API

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/dev/recommendations/generate` | 전체 개발용 후보군 기준 자동 생성 |
| POST | `/api/dev/recommendations/generate?market=KOSPI` | 특정 시장 후보군 기준 자동 생성 |
| POST | `/api/dev/recommendations/generate?market=KOSPI&shortCount=3&longCount=3` | 단기/장기 개수 지정 |

### 주요 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| market | String | 생성 대상 시장. 없으면 전체 후보군을 사용한다. |
| shortCount | Integer | 단기 추천 생성 개수. 기본 3, 범위 1~10. |
| longCount | Integer | 장기 추천 생성 개수. 기본 3, 범위 1~10. |
| sourceInstrumentCount | Integer | 추천 생성에 사용한 개발용 후보 종목 수. |
| generatedPredictionCount | Integer | 생성된 예측 데이터 수. |
| generatedRecommendationCount | Integer | 생성된 추천 데이터 수. |
| predictionIds | Array | 생성된 예측 ID 목록. |
| recommendationIds | Array | 생성된 추천 ID 목록. |

### 예시 응답

```json
{
  "code": 200,
  "data": {
    "market": "KOSPI",
    "sourceInstrumentCount": 1,
    "generatedPredictionCount": 1,
    "generatedRecommendationCount": 2,
    "predictionIds": [1],
    "recommendationIds": [1, 2]
  }
}
```

### 주의

`dev-rule-v0` 개발용 더미 룰이다. 실제 투자 판단용 추천 로직이 아니다.

## 14. 필수 생성 순서

데이터를 직접 넣어 테스트할 때는 아래 순서를 지키면 된다.

1. 개발용 후보 종목 등록: `POST /api/instruments`
2. 가격 예측 저장: `POST /api/predictions`
3. 추천 저장: `POST /api/recommendations`
4. 평가 저장: `POST /api/evaluations`
5. 추천 상태 변경: `PUT /api/recommendations/{id}/status`

현재 개발용 수동 입력 API는 등록된 후보 종목만 허용한다. 최종 플로우에서는 자동 수집한 시장 유니버스가 이 역할을 대체한다.

개발용 자동 생성 API를 사용할 때는 아래처럼 줄일 수 있다.

1. 개발용 후보 종목 등록: `POST /api/instruments`
2. 자동 생성: `POST /api/dev/recommendations/generate?market=KOSPI`
3. 추천 조회: `GET /api/recommendations?status=OPEN`
