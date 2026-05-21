# Stock Advisor 작업명세서

> 작성 기준: 2026-05-21 현재 구현 상태  
> 목표: 현재 API/프론트 골격 이후 실제 서비스까지 가기 위한 작업 목록 정리  
> 백엔드 포트: `8083`  
> 프론트 포트: `5173`  
> DB: MSSQL

## 1. 현재 구현 상태

### 1.1 백엔드 구현 완료

| 영역 | 상태 | 설명 |
|---|---|---|
| MSSQL 연결 설정 | 완료 | `application-local.yml` 기준 로컬 MSSQL 연결 |
| 공통 응답 포맷 | 완료 | `ResultDto<?>` 사용 |
| 전역 예외 처리 | 완료 | `CustomException`, `GlobalExceptionHandler` |
| 관리자 설정 API | 완료 | 설정 조회, 단건 조회, 수정, 기본값 초기화 |
| 감사 로그 API | 완료 | 설정 변경 이력 조회 |
| 종목 API | 완료 | 개발용/수동 보정용 종목 등록, 수정, 조회 |
| 가격 예측 API | 완료 | 예측 저장, 조회 |
| 추천 API | 완료 | 추천 저장, 조회, 상태 변경 |
| 평가 API | 완료 | 평가 저장, 조회 |
| 백테스트 API | 완료 | 백테스트 실행 결과 저장, 조회 |
| 알림 로그 API | 완료 | 알림 발송 결과 저장, 조회 |
| Codex 호출 로그 API | 완료 | Codex 호출 감사 로그 저장, 조회 |
| 데일리 브리프 API | 완료 | 브리프 저장, 조회 |
| AutoResearch API | 완료 | 실험 실행, 전략 버전 저장/조회 |
| 개발용 추천 자동 생성 API | 완료 | `dev-rule-v0` 기반 예측/추천 자동 생성 |
| 시장 유니버스 API | 완료 | 개발용 seed, 후보군 조회/필터, 미국 심볼/가격 동기화 |
| 한국 심볼 동기화 | 완료 | KIND 상장법인 목록 기반 KOSPI/KOSDAQ 후보군 동기화 |
| Feature API | 부분 완료 | 유동성/가격/데이터 품질과 price_daily 기반 RSI/이동평균/거래량 점수 생성 |
| 통계 API | 완료 | summary, daily, by-strategy 기본 집계 |
| 일봉 가격 API | 완료 | price_daily 저장/조회, 후보군 기반 KIS/Stooq 일봉 동기화 |
| 장중 가격 API | 완료 | price_intraday 저장/조회, ExitMonitorJob 현재가 스냅샷 저장 |
| Telegram 테스트 발송 | 부분 완료 | dev-placeholder 또는 실제 Telegram 전송, notification_log 저장 |
| Codex CLI 호출 | 부분 완료 | ProcessBuilder 호출, 수집 데이터 기반 브리프 fallback, codex_call 저장 |
| 스케줄러 골격 | 부분 완료 | KRX/US 프리오픈 Job이 후보군/일봉/추천/알림 플로우를 실행하고 ExitMonitor가 국내 현재가 스냅샷을 저장 |
| BasicAuth/Actuator | 부분 완료 | `/api/admin/**`, `/api/ops/**`, `/actuator/**` BasicAuth 보호, 옵션으로 `/api/dev/**` 보호 가능 |
| 운영 헬스체크 | 부분 완료 | `/api/ops/external-health`에서 KIS/Telegram/Codex/Stooq/KIND 설정 상태 조회 |
| Flyway 마이그레이션 | 완료 | `V1__init_schema.sql`, `ddl-auto: validate` 기준 |
| Swagger 문서 | 완료 | `http://localhost:8083/swagger-ui.html` |

### 1.2 프론트 구현 완료

| 화면 | 상태 | 설명 |
|---|---|---|
| 화면 탭 전환 | 완료 | 오늘의 추천 / 시장 후보군 / 종목 관리 / 성과 통계 / 관리자 설정 |
| 관리자 설정 화면 | 완료 | 설정 조회, 검색, 그룹 탭, 저장, 기본값 초기화 |
| 오늘의 추천 화면 | 완료 | 상태/시장/종목 필터, 개발용 추천 생성, 추천 카드 표시 |
| 추천 평가 화면 | 완료 | 추천 카드에서 평가 입력, 평가 이력 조회, CLOSED/EXPIRED 상태 변경 |
| 종목 관리 화면 | 완료 | 개발용/수동 보정용 종목 등록, 조회, 검색, 수정 |
| 시장 후보군 화면 | 완료 | 개발용 seed, 미국 심볼/가격 동기화, 후보군 필터, feature 점수 표시 |
| 성과 통계 화면 | 완료 | 요약 카드, 기간별/일별/전략별 성과 표시 |
| 일봉 동기화 버튼 | 완료 | 시장 후보군 화면에서 후보 수/기간 지정 후 일봉 동기화 실행 |
| API 프록시 | 완료 | Vite `/api` → `http://localhost:8083` |

### 1.3 문서 작성 완료

| 문서 | 설명 |
|---|---|
| `docs/STOCK_ADVISOR_API.md` | 상세 API 명세 |
| `docs/STOCK_ADVISOR_API_QUICK.md` | 한눈에 보는 API 요약 |
| `docs/STOCK_ADVISOR_FLOW.md` | 전체 흐름도 |
| `docs/STOCK_ADVISOR_TODO_SPEC.md` | 현재 문서 |

## 2. 사용자가 직접 준비할 것

### 2.1 MSSQL 설정

| 작업 | 설명 | 완료 기준 |
|---|---|---|
| MSSQL 서버 실행 | 로컬 MSSQL 인스턴스 실행 | 1433 포트 접속 가능 |
| DB 생성 | `stock_advisor` 데이터베이스 생성 | DB가 존재함 |
| 비밀번호 설정 | `application-local.yml`의 `password` 수정 | 백엔드 실행 시 DB 연결 성공 |

설정 파일:

```text
apps/backend/src/main/resources/application-local.yml
```

수정 대상:

```yaml
spring:
  datasource:
    username: sa
    password: change-me
```

### 2.2 API 키 및 외부 계정

| 항목 | 용도 | 필요 시점 |
|---|---|---|
| KIS Open API Key | 한국 시세/차트 수집 | 실제 데이터 수집 구현 전 |
| DART API Key | 한국 공시 수집 | 공시 수집 구현 전 |
| FRED API Key | 미국 매크로 데이터 | 매크로 수집 구현 전 |
| Telegram Bot Token | 알림 발송 | 실제 Telegram 전송 전. 없으면 dev-placeholder 로그 모드 |
| Telegram Chat ID | 본인 채팅방 발송 | 실제 Telegram 전송 전. 없으면 dev-placeholder 로그 모드 |
| Kakao Developer Token | 카카오 나에게 보내기 | 2차 알림 구현 전 |
| Codex CLI 로그인 | 브리프/AutoResearch LLM 실행 | Codex 실제 연동 전 |

## 3. 로컬 실행 절차

### 3.1 백엔드 실행

```bash
cd C:\Users\dongki\project\stock_codex\apps\backend
gradlew.bat bootRun
```

확인:

```text
http://localhost:8083/swagger-ui.html
```

### 3.2 프론트 실행

```bash
cd C:\Users\dongki\project\stock_codex\apps\web
npm run dev
```

확인:

```text
http://127.0.0.1:5173
```

### 3.3 최소 동작 테스트

| 순서 | 작업 | 방법 |
|---:|---|---|
| 1 | 관리자 설정 초기화 | 프론트 관리자 설정 화면에서 기본값 초기화 |
| 2 | 시장 후보군 seed | 프론트 시장 후보군 화면에서 개발용 후보군 생성 |
| 3 | 개발용 추천 자동 생성 | 프론트 오늘의 추천 화면에서 개발용 추천 생성 |
| 4 | 추천 확인 | 프론트 추천 카드 확인 |
| 5 | 추천 평가 | 추천 카드의 평가 버튼으로 손익률 저장 및 상태 종료 |
| 6 | 통계 확인 | 성과 통계 화면에서 평가 집계 확인 |

## 4. 남은 개발 작업 우선순위

## P0. 프론트 기본 운영 화면 완성

> 정정: 종목 관리는 최종 핵심 플로우가 아니라 개발용/수동 보정용 화면이다. 최종 제품은 사용자가 종목을 하나씩 등록하는 방식이 아니라, 시장 데이터를 수집해 조건에 부합하는 종목을 자동으로 골라 추천한다.

### P0-1. 종목 관리 화면

| 항목 | 내용 |
|---|---|
| 목적 | 실제 데이터 수집 전 개발용 추천 생성을 테스트하거나, 자동 수집 결과를 수동 보정할 수 있게 한다. |
| 필요 API | `GET /api/instruments`, `POST /api/instruments`, `PUT /api/instruments/{ticker}` |
| 화면 기능 | 종목 목록, 시장 필터, 종목 등록 폼, 활성/비활성 수정 |
| 상태 | 완료 |
| 완료 기준 | 실제 데이터 수집 전 임시로 `005930`, `AAPL` 같은 종목을 등록하고 추천 생성에 사용할 수 있음 |

작업 상세:

| 작업 | 설명 |
|---|---|
| API 클라이언트 추가 | `src/api/instruments.ts` 생성 |
| 화면 추가 | `src/pages/InstrumentsPage.tsx` 생성 |
| 탭 추가 | App 네비게이션에 `종목 관리` 추가 |
| 폼 검증 | ticker, market, name, enabled 필수 처리 |
| 상태 처리 | 등록 성공/실패 메시지 표시 |

### P0-2. 추천 상세 및 평가 화면

| 항목 | 내용 |
|---|---|
| 목적 | 추천을 종료하고 평가 결과를 저장할 수 있게 한다. |
| 필요 API | `GET /api/recommendations/{id}`, `POST /api/evaluations`, `PUT /api/recommendations/{id}/status` |
| 화면 기능 | 추천 상세, 평가 입력, CLOSED/EXPIRED 처리 |
| 상태 | 완료 |
| 완료 기준 | 프론트에서 추천 하나를 평가하고 상태를 CLOSED로 바꿀 수 있음 |

작업 상세:

| 작업 | 설명 |
|---|---|
| 평가 API 클라이언트 | `src/api/evaluations.ts` 생성 |
| 추천 카드 액션 | 평가 버튼 추가 |
| 평가 폼 | actualExitPrice, exitReason, pnlPct, hitTarget 입력 |
| 상태 변경 | 평가 저장 후 추천 상태 CLOSED 처리 |

### P0-3. 추천 목록 필터 강화

| 항목 | 내용 |
|---|---|
| 목적 | 추천 상태/시장/종목별 조회를 쉽게 한다. |
| 필요 API | `GET /api/recommendations?status=OPEN`, `GET /api/recommendations?ticker=005930` |
| 화면 기능 | 상태 필터, 시장 표시, 종목 검색 |
| 상태 | 완료 |
| 완료 기준 | OPEN/CLOSED/EXPIRED 추천을 화면에서 전환 조회 가능 |

## P1. 실제 알림과 스케줄러

### P1-1. Telegram 발송 클라이언트

| 항목 | 내용 |
|---|---|
| 목적 | 추천/브리프/손절 알림을 Telegram으로 실제 발송한다. |
| 필요 설정 | Telegram Bot Token, Chat ID |
| 관련 테이블 | `notification_log` |
| 상태 | 부분 완료 |
| 완료 기준 | 테스트 API 호출 시 Telegram 메시지가 도착하고 로그가 저장됨 |

작업 상세:

| 작업 | 설명 |
|---|---|
| 설정 추가 | `stock-advisor.telegram.botToken`, `stock-advisor.telegram.chatId` 설정 추가 완료 |
| 클라이언트 구현 | `TelegramClient` 구현 완료 |
| 발송 서비스 | 공용 NotificationService 분리는 미구현 |
| 실패 처리 | 테스트 API에서 `notification_log`에 SENT/FAILED 저장 완료 |
| 테스트 API | `/api/dev/notifications/test` 추가 완료 |

### P1-2. 스케줄러 기본 골격

| 항목 | 내용 |
|---|---|
| 목적 | 지정 시각에 자동으로 추천/요약 작업을 실행한다. |
| 대상 스케줄 | KRX 프리오픈, US 프리오픈, US 마감, ExitMonitor |
| 상태 | 부분 완료 |
| 완료 기준 | 정해진 cron에 Job이 실행되고 로그가 남음 |

작업 상세:

| 작업 | 설명 |
|---|---|
| Scheduler 설정 | Spring Scheduling 활성화 완료 |
| KRX Job | `KrxPreOpenJob` 골격 구현 완료 |
| US Job | `UsPreOpenJob`, `UsCloseSummaryJob` 골격 구현 완료 |
| Exit Job | `ExitMonitorJob` 골격 구현 완료 |
| 관리자 설정 연동 | `app_setting`의 알림/폴링 설정 읽기 미구현 |

### P1-3. 개발용 추천 자동 생성 스케줄 연결

| 항목 | 내용 |
|---|---|
| 목적 | 실제 엔진 전까지 `dev-rule-v0` 자동 추천을 스케줄로 실행한다. |
| 상태 | 미구현 |
| 완료 기준 | 수동 버튼 없이 KRX 시간에 추천이 생성됨 |

## P2. 실제 데이터 수집

### P2-0. 시장 유니버스 자동 구성

| 항목 | 내용 |
|---|---|
| 목적 | 사용자가 종목을 직접 등록하지 않아도 KOSPI/KOSDAQ/NYSE/NASDAQ 후보군을 자동으로 구성한다. |
| 입력 | 거래소 상장 종목 목록, 시총, 거래대금, 가격, 섹터, 상장 상태 |
| 출력 | 추천 엔진이 스캔할 후보 종목 목록 |
| 상태 | 부분 완료 |
| 완료 기준 | 수동 종목 등록 없이 시장별 후보군을 조회하고 필터링할 수 있음 |

작업 상세:

| 작업 | 설명 |
|---|---|
| universe 테이블 설계 | ticker, market, name, sector, marketCap, avgTurnover, lastPrice, tradable, lastSyncedAt 저장 완료 |
| 개발용 seed | KOSPI/KOSDAQ/NASDAQ/NYSE seed API 및 화면 완료 |
| 한국 종목 목록 수집 | KIND 상장법인 목록 기반 KOSPI/KOSDAQ 목록 동기화 완료 |
| 미국 종목 목록 수집 | NASDAQ Trader 기반 NASDAQ/NYSE 심볼 동기화 완료 |
| 미국 가격 수집 | Stooq 기반 최근 가격/거래대금 동기화 완료 |
| 기본 필터 | 시장, 거래 가능, 시총/거래대금/가격 하한 필터 완료 |
| 관리자 설정 연동 | `recommendation.marketcap.*`, `recommendation.turnover.*`, 제외 섹터/종목 설정 반영 미구현 |

### P2-1. KIS Open API 시세 수집

| 항목 | 내용 |
|---|---|
| 목적 | 자동 후보군에 포함된 한국 주식의 현재가/일봉/거래량 데이터를 가져온다. |
| 필요 설정 | KIS App Key, App Secret |
| 상태 | 부분 완료 |
| 완료 기준 | 시장 후보군 전체를 순회하며 현재가와 OHLCV를 조회/저장 가능 |

작업 상세:

| 작업 | 설명 |
|---|---|
| 설정 키 추가 | KIS API key/secret 설정 완료 |
| HTTP 클라이언트 구성 | Java HttpClient 기반 클라이언트 구현 완료 |
| 토큰 발급 | KIS 접근 토큰 발급/캐시 구현 완료 |
| 시세 조회 | 현재가 조회 메서드 구현 완료 |
| 한국 유니버스 동기화 | KIND 기반 KOSPI/KOSDAQ 전체 후보군 수집 완료 |
| 일봉/OHLCV 조회 | KIS 일봉 조회 및 price_daily 저장 완료 |
| 장중 현재가 저장 | ExitMonitorJob에서 KIS 현재가를 price_intraday에 저장 완료 |
| 저장 테이블 | price_daily, price_intraday 마이그레이션 완료 |

### P2-2. yfinance 또는 대체 US 시세 수집

| 항목 | 내용 |
|---|---|
| 목적 | 자동 후보군에 포함된 미국 주식 가격 데이터를 가져온다. |
| 후보 | yfinance Python sidecar 또는 Stooq/Alpha Vantage |
| 상태 | 부분 완료 |
| 완료 기준 | NASDAQ/NYSE 후보군을 순회하며 가격 데이터를 조회/저장 가능 |

현재 구현:

| 작업 | 설명 |
|---|---|
| 심볼 목록 | NASDAQ Trader 공개 파일 기반 동기화 완료 |
| 최근 가격 | Stooq CSV 기반 `lastPrice`, `avgTurnover` 갱신 완료 |
| 시세 히스토리 | Stooq 일봉 조회 및 price_daily 저장 완료 |

### P2-3. 뉴스/RSS 수집

| 항목 | 내용 |
|---|---|
| 목적 | 추천 근거와 브리프 생성을 위한 뉴스 데이터 수집 |
| 소스 | Naver Finance, MarketWatch, Reuters RSS 등 |
| 완료 기준 | 종목별 최근 뉴스 제목/링크 저장 가능 |

### P2-4. 공시/매크로 수집

| 항목 | 내용 |
|---|---|
| 목적 | 장기 추천과 브리프에 필요한 공시/매크로 데이터 확보 |
| 소스 | DART, SEC EDGAR, FRED |
| 완료 기준 | 종목 공시와 주요 지표를 조회/저장 가능 |

## P3. 실제 추천 엔진

### P3-1. FeatureBuilder

| 항목 | 내용 |
|---|---|
| 목적 | 자동 후보군의 시세/거래량/뉴스/펀더멘털 데이터를 추천 점수로 변환한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 후보군 전체에 대해 feature JSON 생성 가능 |

현재 구현은 유동성, 가격 구간, 데이터 품질에 price_daily 기반 RSI/이동평균/거래량 점수를 결합하는 `feature-rule-v1` 수준이다. 뉴스, 펀더멘털, 매크로 feature는 아직 없다.

필요 Feature:

| 카테고리 | 예시 |
|---|---|
| 기술적 | RSI, MACD, 이동평균, Bollinger |
| 거래량 | 거래량 z-score, 거래대금 |
| 수급 | 외국인/기관 순매수 |
| 뉴스 | 뉴스 빈도, 감성 점수 |
| 펀더멘털 | PER, PBR, ROE, 매출 성장률 |
| 매크로 | 금리, 환율, 섹터 로테이션 |

### P3-2. RecommendationEngine

| 항목 | 내용 |
|---|---|
| 목적 | 자동 후보군 feature를 기반으로 조건에 부합하는 종목을 점수화하고 Top-N을 선정한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 설정된 단기/장기 개수만큼 추천 생성 |

현재 구현은 독립 `RecommendationEngine`에서 feature 점수순 Top-N을 생성하고 제외 섹터/종목, 최소 점수, 최소 데이터 품질 설정을 반영한다.

### P3-3. PricePredictor

| 항목 | 내용 |
|---|---|
| 목적 | 목표가, 손절가, 예상 매도일을 계산한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 추천 생성 시 entry/target/stop/expectedExitAt 자동 산출 |

현재 구현은 독립 `PricePredictor`에서 최근 일봉 변동성을 기반으로 entry/target/stop/expectedExitAt을 산출하고, 히스토리가 부족하면 fallback 배수를 사용한다.

## P4. Codex CLI 실제 연동

### P4-1. CodexClient 구현

| 항목 | 내용 |
|---|---|
| 목적 | Java에서 Codex CLI를 실행하고 응답을 받는다. |
| 상태 | 부분 완료 |
| 완료 기준 | 테스트 프롬프트 호출 결과가 반환되고 `codex_call`에 저장됨 |

작업 상세:

| 작업 | 설명 |
|---|---|
| 설정 키 | `codex.profile`, `stock-advisor.codex.command` 사용 완료. 일 호출 한도 적용 미구현 |
| 프로세스 실행 | `ProcessBuilder` 기반 CLI 호출 완료 |
| timeout | 호출 제한 시간 처리 완료 |
| 로그 저장 | promptHash, promptLen, responseLen, succeeded 저장 완료 |
| fallback | 실패 시 로컬 템플릿 사용 완료 |

### P4-2. DailyBriefService 실제 생성

| 항목 | 내용 |
|---|---|
| 목적 | 수집된 뉴스/매크로/시세 데이터를 Codex로 요약한다. |
| 상태 | 부분 완료 |
| 완료 기준 | KRX/US 브리프 Markdown 생성 및 저장 |

현재는 개발용 프롬프트 기반 `/api/dev/brief/generate`로 브리프를 저장한다. 실제 뉴스/매크로/시세 컨텍스트 결합은 미구현이다.

### P4-3. Exit Confirm

| 항목 | 내용 |
|---|---|
| 목적 | 손절 위험 구간에서 Codex가 HOLD/CUT/TIGHTEN 판단을 보조한다. |
| 상태 | 미구현 |
| 완료 기준 | 위험 구간 진입 시 Codex 판단 결과 저장 |

## P5. 통계 대시보드

### P5-1. 통계 API

| 항목 | 내용 |
|---|---|
| 목적 | 평가 데이터를 집계해 성과 지표를 제공한다. |
| 필요 API | `/api/stats/summary`, `/api/stats/daily`, `/api/stats/by-strategy` |
| 상태 | 완료 |
| 완료 기준 | Hit Rate, ROI, MDD, Sharpe 기본 응답 제공 |

### P5-2. 통계 프론트

| 항목 | 내용 |
|---|---|
| 목적 | 누적 성과와 전략별 성과를 시각화한다. |
| 후보 라이브러리 | Recharts |
| 상태 | 부분 완료 |
| 완료 기준 | ROI 곡선, Hit Rate 카드, 최근 결과 목록 표시 |

현재는 Recharts 없이 표와 요약 카드로 기본 통계를 표시한다. ROI 곡선은 미구현이다.

## P6. 보안/운영

### P6-1. BasicAuth

| 항목 | 내용 |
|---|---|
| 목적 | 개인용 관리자 페이지와 API 보호 |
| 상태 | 부분 완료 |
| 완료 기준 | 인증 없이는 `/api/admin/**` 접근 불가 |

현재 `/api/admin/**`, `/actuator/**`는 BasicAuth로 보호된다. 나머지 API는 개발 편의를 위해 공개 상태다.

### P6-2. CORS/프록시 정리

| 항목 | 내용 |
|---|---|
| 목적 | 개발/운영 환경별 프론트-백엔드 연결 정리 |
| 상태 | 부분 완료 |
| 완료 기준 | 개발은 Vite proxy, 운영은 고정 origin 정책 적용 |

현재 개발 환경 Vite proxy는 `/api` → `http://localhost:8083`으로 설정되어 있다. 운영 origin 정책은 미정이다.

### P6-3. DB 마이그레이션

| 항목 | 내용 |
|---|---|
| 목적 | `ddl-auto:update` 의존 제거 |
| 후보 | Flyway 또는 Liquibase |
| 상태 | 완료 |
| 완료 기준 | 스키마 변경이 migration 파일로 관리됨 |

현재 `V1__init_schema.sql`과 `ddl-auto: validate` 기준으로 동작한다.

### P6-4. 운영 로그/헬스체크

| 항목 | 내용 |
|---|---|
| 목적 | 서버 상태와 외부 API 상태 확인 |
| 필요 API | `/actuator/health`, `/actuator/metrics` |
| 상태 | 부분 완료 |
| 완료 기준 | 헬스체크와 기본 메트릭 조회 가능 |

Actuator 의존성과 보호 설정은 있다. 외부 API별 상세 헬스체크는 미구현이다.

## 5. 추천 개발 순서

가장 현실적인 순서는 아래와 같다.

| 순서 | 작업 | 이유 |
|---:|---|---|
| 1 | 백엔드 테스트 복구 | 현재 테스트가 서비스 반환 타입 변경을 따라오지 못해 CI 기준이 깨져 있음 |
| 2 | 한국 시장 유니버스 동기화 | KOSPI/KOSDAQ 후보군을 개발용 seed가 아니라 실제 목록으로 구성하기 위함 |
| 3 | RecommendationEngine 설정 고도화 | 제외 섹터/종목과 시장별 최소 조건을 엔진에 반영하기 위함 |
| 4 | price_intraday 활용 | 완료. ExitMonitorJob 현재가 스냅샷 저장과 조회 API 구현 |
| 5 | 운영 보강 | 부분 완료. 보호 API 범위, 개발 API 보호 옵션, CORS origin 설정, 외부 연동 상태 API 구현 |
| 6 | Codex 컨텍스트 고도화 | 부분 완료. DailyBrief는 실제 수집 데이터 컨텍스트를 사용하고, Exit Confirm 수동 API 구현 |

## 6. 다음 작업 상세 명세

다음 작업은 `Exit Confirm`으로 한다.

### 6.1 목표

손절 위험 구간에 진입한 OPEN 추천에 대해 최근 가격과 추천 근거를 묶어 Codex가 HOLD/CUT/TIGHTEN 판단을 보조하게 만든다.

### 6.2 구현 파일

| 파일 | 작업 |
|---|---|
| `apps/backend/src/main/java/com/parkdh/stockadvisor/application/recommendation/ExitConfirmService.java` | 위험 구간 추천의 판단 컨텍스트와 Codex 호출 구현 완료 |
| `apps/backend/src/main/java/com/parkdh/stockadvisor/api/recommendation/RecommendationController.java` | `POST /api/recommendations/{id}/exit-confirm` 수동 API 추가 완료 |
| `apps/backend/src/main/java/com/parkdh/stockadvisor/scheduler/ExitMonitorJob.java` | 손절 근접 시 Exit Confirm 자동 호출 연결 미구현 |
| `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/persistence/price/PriceIntradayRepository.java` | 최근 장중 가격 조회 메서드 활용 완료 |

### 6.3 구성

| 영역 | 내용 |
|---|---|
| 입력 | OPEN 추천, 최근 price_intraday, 최근 price_daily, 추천 signalsJson |
| 위험 판단 | 현재가가 손절가에 가까운 경우 |
| 호출 | CodexClient 호출 및 codex_call 감사 로그 저장 |
| 출력 | HOLD/CUT/TIGHTEN 판단과 요약 메시지 |

### 6.4 완료 기준

| 기준 | 확인 방법 |
|---|---|
| 위험 구간 감지 | 손절가 대비 지정 비율 이내 추천만 Codex 판단 |
| fallback 유지 | Codex CLI 미설정 시 규칙 기반 HOLD/CUT/TIGHTEN 생성 |
| 감사 로그 유지 | codex_call에 성공/실패 로그가 저장됨 |
| 기존 기능 유지 | `gradlew.bat test`, `npm run build` 성공 |

## 7. 작업 완료 체크리스트

### 로컬 실행 체크

| 항목 | 상태 |
|---|---|
| MSSQL 실행 | 미확인 |
| `stock_advisor` DB 생성 | 미확인 |
| 백엔드 8083 실행 | 수동 확인 필요 |
| 프론트 5173 실행 | 수동 확인 필요 |
| Swagger 접속 | 수동 확인 필요 |

### 기능 체크

| 항목 | 상태 |
|---|---|
| 관리자 기본값 초기화 | 구현됨 |
| 종목 등록 | API 구현됨, 프론트 구현됨, 최종 플로우에서는 개발용/수동 보정용 |
| 개발용 추천 자동 생성 | 구현됨 |
| 추천 목록 표시 | 구현됨 |
| 추천 평가 | API 구현됨, 프론트 구현됨 |
| 시장 유니버스 자동 구성 | 부분 구현됨. 개발용 seed, 한국/미국 심볼 동기화, 미국 가격 동기화, 후보군 화면 구현됨 |
| 실제 알림 | 부분 구현됨. Telegram 테스트 발송과 스케줄러 메시지 전송 구현됨 |
| 실제 데이터 수집 | 부분 구현됨. KIS 현재가/일봉, Stooq 최근 quote/일봉, price_daily 저장, ExitMonitor price_intraday 저장 구현됨 |
| 실제 추천 엔진 | 부분 구현됨. RecommendationEngine/PricePredictor 분리, 제외 섹터/종목/최소 점수/품질 설정 반영 |
| 스케줄러 | 부분 구현됨. KRX/US 프리오픈은 수집/추천/알림 연결됨. 설정 기반 동적 스케줄과 US_CLOSE 고도화는 미구현 |
| 통계 화면 | 구현됨. ROI 차트는 미구현 |
| 보안 | 부분 구현됨. 관리자/운영/Actuator BasicAuth 적용, 개발 API 보호 옵션과 CORS origin 설정 추가, 전체 운영 공개 정책은 미정 |
