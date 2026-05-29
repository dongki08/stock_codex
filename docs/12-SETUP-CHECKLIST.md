# Stock Advisor 사용자 준비/실행 체크리스트

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 12(셋업) · 상태 🟢 현행(2026-05-29 정정) · 실행 전 확인용

작성일: 2026-05-21 · 정정: 2026-05-29 (ExitConfirm 제거·sentiment 설정 반영)

## 현재까지 진행된 것

백엔드 기준으로 다음 큰 축은 1차 구현이 완료됐다.

| 영역 | 상태 | 비고 |
|---|---|---|
| 일봉/장중 가격 저장/조회 | 완료 | `price_daily`, `price_intraday` |
| ExitMonitor 자동 청산 | 완료 | 룰 기반(목표가/손절가/만료) 자동 청산 + 평가 기록. ⚠️ 구 `exit-confirm` API/테이블은 V8에서 제거됨 |
| 뉴스 RSS 수집 | 완료 | 제목/링크/발행시각 저장 |
| 공시 수집 | 완료 | DART, SEC EDGAR 메타데이터 저장 |
| 매크로 수집 | 완료 | FRED 공개 CSV 주요 지표 저장 |
| 수집 데이터 프론트 화면 | 완료 | 뉴스/공시/매크로 조회 및 동기화 버튼 |
| 뉴스/공시/매크로 자동 수집 Job | 완료 | KRX/US 장 전 및 매크로 아침 수집 |
| 펀더멘털 수집 | 부분 완료 | 미국 SEC Company Facts + 한국 KIS PER/PBR/EPS/BPS/ROE + DART 매출/영업이익/순이익 YoY 수집 |
| 추천 Feature 수집 데이터 반영 | 부분 완료 | 뉴스 감성, 공시 중요도, 매크로/펀더멘털 데이터 가용성 점수 반영 |
| DailyBrief 데이터 연동 | 완료 | 추천/후보군/가격/뉴스/공시/매크로/성과 통계 포함 |
| 운영 헬스체크 | 완료 | KIS, Telegram, Codex, DART, SEC, RSS, FRED 등 |

단, “서비스 전체 완성”은 아직 아니다. 아래 후속 개발 항목은 남아 있다.

| 남은 개발 항목 | 설명 |
|---|---|
| 뉴스 감성 점수 | 현재는 뉴스 제목/링크 저장만 한다. FinBERT 등 감성 분석은 미구현 |
| 펀더멘털 수집 | 한국 PER/PBR/ROE와 DART 매출/영업이익/순이익 YoY 1차 구현. 분기 누적/상세 계정 정규화는 후속 |
| 뉴스 감성 분석 고도화 | 현재는 제목/요약 키워드 기반 룰 점수 |
| 공시 중요도 분류 고도화 | 현재는 제목/유형 키워드 기반 룰 점수 |
| 실운영 API 키 검증 | 로컬 환경에서 실제 키로 호출 성공 확인 필요 |
| 한국 펀더멘털 수집 | KIS 현재가 기반 PER/PBR/EPS/BPS/ROE + DART 사업보고서 주요계정 YoY 1차 구현 |

## 사용자가 준비해야 할 것

### 1. MSSQL 실행 및 DB 준비

로컬 MSSQL이 실행 중이어야 한다.

| 항목 | 값 |
|---|---|
| DB 이름 | `stock_advisor` |
| 기본 포트 | `1433` |
| 설정 파일 | `apps/backend/src/main/resources/application-local.yml` |

현재 설정 파일에서 확인할 항목:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=stock_advisor;encrypt=true;trustServerCertificate=true
    username: park
    password: 12345
```

DB 계정/비밀번호가 다르면 위 값을 본인 환경에 맞게 수정해야 한다.

### 2. 필수/선택 API 키 준비

실제 외부 연동을 하려면 아래 환경변수를 설정한다.

| 환경변수 | 필수 여부 | 용도 | 없을 때 동작 |
|---|---:|---|---|
| `KIS_APP_KEY` | 권장 | 한국 시세/일봉/현재가 | KIS 호출 스킵 또는 개발 모드 |
| `KIS_APP_SECRET` | 권장 | 한국 시세/일봉/현재가 | KIS 호출 스킵 또는 개발 모드 |
| `TELEGRAM_BOT_TOKEN` | 선택 | 실제 Telegram 알림 | 로그 출력 개발 모드 |
| `TELEGRAM_CHAT_ID` | 선택 | 실제 Telegram 알림 대상 | 로그 출력 개발 모드 |
| `CODEX_COMMAND` | 선택 | Codex CLI 실제 호출 | 로컬 fallback 브리프/판단 |
| `DART_API_KEY` | 선택 | 한국 공시 수집 | DART 수집은 빈 결과 |
| `SEC_USER_AGENT` | 권장 | SEC EDGAR 요청 User-Agent | 기본값 사용 |
| `stock-advisor.sentiment.enabled` / `.base-url` (yml) | 선택 | 외부 뉴스 감성 분석 사이드카 | `false`/`dev-placeholder`(기본) 시 키워드 룰 폴백 |
| `ADMIN_USERNAME` | 권장 | BasicAuth 계정 | 기본 `admin` |
| `ADMIN_PASSWORD` | 권장 | BasicAuth 비밀번호 | 기본 `change-me` |

Windows PowerShell 예시:

```powershell
$env:KIS_APP_KEY="..."
$env:KIS_APP_SECRET="..."
$env:DART_API_KEY="..."
$env:TELEGRAM_BOT_TOKEN="..."
$env:TELEGRAM_CHAT_ID="..."
$env:SEC_USER_AGENT="StockAdvisor/1.0 your-email@example.com"
```

SEC EDGAR는 명확한 User-Agent를 요구하므로 `SEC_USER_AGENT`에는 본인이 확인 가능한 이메일을 넣는 것이 좋다.

### 3. 백엔드 실행

```bat
cd C:\Users\dongki\project\stock_codex\apps\backend
gradlew.bat bootRun
```

확인 URL:

```text
http://localhost:8083/swagger-ui.html
```

처음 실행 시 Flyway가 아래 테이블들을 생성한다.

| 테이블 | 용도 |
|---|---|
| `price_daily` | 일봉 가격 |
| `price_intraday` | 장중 가격 |
| `news_article` | 뉴스 제목/링크 |
| `disclosure_event` | 공시 메타데이터 |
| `macro_observation` | 매크로 관측값 |

### 4. 프론트 실행

```bat
cd C:\Users\dongki\project\stock_codex\apps\web
npm run dev
```

확인 URL:

```text
http://127.0.0.1:5173
```

프론트 상단 탭의 `수집 데이터` 화면에서 뉴스/공시/매크로 조회와 수동 동기화를 실행할 수 있다.

## 자동 수집 스케줄

백엔드 실행 중에는 아래 Job이 자동으로 돈다.

| Job | Cron | 설명 |
|---|---|---|
| KRX 수집 | 평일 08:15 KST | KOSPI/KOSDAQ 상위 후보 뉴스와 공시 수집 |
| 매크로 수집 | 평일 07:00 KST | FRED 기본 지표 수집 |
| US 수집 | 평일 21:40 KST | NASDAQ/NYSE 상위 후보 뉴스와 공시 수집 |

자동 수집 관련 관리자 설정:

| 설정 키 | 기본값 | 설명 |
|---|---:|---|
| `collection.enabled` | `true` | 뉴스/공시/매크로 자동 수집 활성 여부 |
| `collection.news.tickersPerMarket` | `5` | 시장별 뉴스 수집 후보 수 |
| `collection.news.limitPerTicker` | `5` | 종목별 뉴스 수집 개수 |
| `collection.disclosure.limit` | `20` | 시장별 공시 수집 개수 |
| `collection.macro.limitPerSeries` | `5` | 매크로 지표별 수집 개수 |
| `collection.fundamental.tickersPerMarket` | `3` | 시장별 펀더멘털 수집 후보 수 |

## 최초 데이터 준비 순서

아래 순서로 실행하면 수집/추천/브리프 흐름을 확인하기 쉽다.

### 1. 관리자 설정 초기화

```http
POST /api/admin/settings/reset
```

### 2. 시장 후보군 생성 또는 동기화

개발용 seed:

```http
POST /api/dev/universe/seed
```

한국 후보군 동기화:

```http
POST /api/universe/sync/kr-symbols?market=KOSPI
POST /api/universe/sync/kr-symbols?market=KOSDAQ
```

미국 후보군 동기화:

```http
POST /api/universe/sync/us-symbols?market=NASDAQ
POST /api/universe/sync/us-symbols?market=NYSE
```

### 3. 일봉 가격 동기화

```http
POST /api/market-data/daily-prices/sync?market=ALL&limit=20&days=120
```

### 4. 뉴스 동기화

한국 예시:

```http
POST /api/market-data/news/sync?market=KOSPI&ticker=005930&limit=20
```

미국 예시:

```http
POST /api/market-data/news/sync?market=NASDAQ&ticker=AAPL&limit=20
```

조회:

```http
GET /api/market-data/news?market=NASDAQ&ticker=AAPL&limit=20
```

### 5. 공시 동기화

한국 DART 예시:

```http
POST /api/market-data/disclosures/sync?market=KOSPI&ticker=005930&limit=20
```

미국 SEC 예시:

```http
POST /api/market-data/disclosures/sync?market=NASDAQ&ticker=AAPL&limit=20
```

조회:

```http
GET /api/market-data/disclosures?market=NASDAQ&limit=20
```

주의: DART는 `DART_API_KEY`가 없으면 수집 결과가 비어 있을 수 있다.

### 6. 매크로 동기화

기본 지표 묶음:

```http
POST /api/market-data/macro-observations/sync?limit=5
```

특정 지표:

```http
POST /api/market-data/macro-observations/sync?seriesId=DGS10&limit=20
```

조회:

```http
GET /api/market-data/macro-observations?seriesId=DGS10&limit=20
```

기본 수집 지표:

| seriesId | 설명 |
|---|---|
| `DGS10` | 미국 10년물 국채 금리 |
| `FEDFUNDS` | 연방기금금리 |
| `CPIAUCSL` | 미국 CPI |
| `DCOILWTICO` | WTI 유가 |
| `DTWEXBGS` | 달러 인덱스 계열 |

### 7. 펀더멘털 동기화

미국 종목 예시:

```http
POST /api/market-data/fundamentals/sync?market=NASDAQ&ticker=AAPL
POST /api/market-data/fundamentals/sync?market=KOSPI&ticker=005930
```

조회:

```http
GET /api/market-data/fundamentals?market=NASDAQ&ticker=AAPL&limit=20
```

현재 수집 지표:

| metricName | 설명 |
|---|---|
| `REVENUE` | 매출 |
| `NET_INCOME` | 순이익 |
| `OPERATING_INCOME` | 영업이익 |
| `ASSETS` | 자산 |
| `LIABILITIES` | 부채 |
| `EQUITY` | 자본 |
| `EPS_DILUTED` | 희석 EPS |

주의: 미국은 SEC Company Facts 기반 주요 재무 지표를 저장한다. 한국은 KIS 현재가 응답 기반 PER/PBR/EPS/BPS와 EPS/BPS 계산 ROE, DART 단일회사 주요계정 기반 매출/영업이익/순이익 및 YoY 성장률을 저장한다.

### 8. 추천 생성

개발용 추천 생성:

```http
POST /api/dev/recommendations/generate?market=ALL
```

추천 조회:

```http
GET /api/recommendations?status=OPEN
```

### 9. 브리프 생성

```http
POST /api/dev/brief/generate?marketTrack=KRX
POST /api/dev/brief/generate?marketTrack=US
```

브리프 조회:

```http
GET /api/briefs
```

Codex CLI가 설정되어 있지 않아도 수집된 데이터 기반 로컬 fallback 브리프가 저장된다.

## 운영 확인 API

외부 연동 설정 상태:

```http
GET /api/ops/external-health
```

`/api/ops/**`는 BasicAuth 보호 대상이다.

## 지금 바로 확인할 최소 체크

1. 백엔드 실행 후 Swagger 접속
2. `POST /api/admin/settings/reset`
3. `POST /api/dev/universe/seed`
4. `POST /api/market-data/news/sync?market=NASDAQ&ticker=AAPL&limit=5`
5. `GET /api/market-data/news?market=NASDAQ&ticker=AAPL&limit=5`
6. `POST /api/market-data/macro-observations/sync?seriesId=DGS10&limit=5`
7. `GET /api/market-data/macro-observations?seriesId=DGS10&limit=5`
8. `POST /api/market-data/fundamentals/sync?market=NASDAQ&ticker=AAPL` 또는 `POST /api/market-data/fundamentals/sync?market=KOSPI&ticker=005930`
9. `GET /api/market-data/fundamentals?market=NASDAQ&ticker=AAPL&limit=20`
10. `POST /api/dev/brief/generate?marketTrack=US`
11. `GET /api/briefs`

여기까지 성공하면 뉴스/매크로 수집 데이터가 브리프까지 연결된 것이다.

## 주의사항

- 뉴스 본문 전문은 저장하지 않는다. 제목, 링크, 요약 메타데이터 중심으로 저장한다.
- DART API 키가 없으면 한국 공시는 빈 결과가 정상일 수 있다.
- SEC EDGAR 호출은 `SEC_USER_AGENT`를 실제 연락 가능한 값으로 설정하는 편이 안전하다.
- 외부 RSS/FRED/SEC 호출은 네트워크 상태와 제공자 정책에 따라 일시 실패할 수 있다.
- SEC 펀더멘털은 `SEC_USER_AGENT`가 부적절하면 차단될 수 있다.
- 실제 투자 판단 자동화 전에는 추천/Exit Confirm 결과를 반드시 수동 검토해야 한다.
