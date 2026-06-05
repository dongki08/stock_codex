# Stock Advisor — 백엔드 전체 구조

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 30(아키텍처) · 상태 🟢 현행 · 코드기준 2026-06-04
>
> **Stack**: Spring Boot 3.3 · Java 21 · MSSQL · Flyway · Spring Security BasicAuth  
> **포트**: 8083  
> **Swagger**: http://localhost:8083/swagger-ui.html

---

## 1. 레이어 구조

```
api/           → REST Controller + DTO (입력/출력 형태 정의)
application/   → 비즈니스 로직 Service
domain/        → JPA Entity (DB 테이블 1:1 매핑)
infrastructure/→ 외부 API 클라이언트 + Repository 인터페이스
scheduler/     → @Scheduled 크론잡
config/        → Properties 설정 레코드 + SecurityConfig
global/        → ResultDto, CustomException, GlobalExceptionHandler
```

### 응답 공통 포맷

```json
// 성공
{ "code": 200, "data": { ... } }
// 실패
{ "code": 404, "error_message": "추천을 찾을 수 없습니다." }
```

모든 API는 `ResultDto<T>` = `{ code, data, error_message }` 로 래핑 (`global/dto/ResultDto.java`). `@JsonInclude(NON_NULL)`이라 null 필드는 응답에서 빠짐.

---

## 2. 인증

| 구분 | 방식 |
|------|------|
| `/api/admin/**` | HTTP BasicAuth 필수 |
| `/api/ops/**` | HTTP BasicAuth 필수 |
| `/actuator/**` | HTTP BasicAuth 필수 |
| `/api/dev/**` | `protect-dev-api: true` 일 때만 인증 |
| 나머지 | 인증 없음 (공개) |

계정: `application-local.yml` 의 `stock-advisor.admin.username / password`

---

## 3. API 목록 — Swagger 가시성 포함

> Swagger 가시성: ✅ 노출 / 🔒 hidden=true(내부 전용) / 🚫 @Hidden(개발 전용)
> 상세 계약: [20-API.md](20-API.md) · 빠른 참조: [21-API-QUICK.md](21-API-QUICK.md)

### 관리자·운영 (BasicAuth 필수)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/admin/settings` | 앱 설정 전체 조회 |
| ✅ | GET/PUT | `/api/admin/settings/{key}` | 단건 조회·수정 |
| ✅ | POST | `/api/admin/settings/reset` | 기본값 초기화 |
| ✅ | GET | `/api/admin/audit-logs` | 설정 변경 감사 로그 |
| ✅ | GET | `/api/ops/external-health` | KIS·Telegram·Codex·Stooq·KIND 연결 상태 |
| ✅ | POST | `/api/ops/jobs/{jobName}/trigger` | 스케줄러 Job 수동 즉시 실행 (비동기) |

jobName: `krx-preopen` · `us-preopen` · `backfill-kr` · `backfill-us` · `feature-snapshot`

---

### 성과 조회 (공개)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/stats/summary` | 전체 요약 (hitRate, avgPnl, MDD) |
| ✅ | GET | `/api/stats/daily` | 일별 손익 |
| ✅ | GET | `/api/stats/by-strategy` | 전략별 성과 |
| ✅ | GET | `/api/stats/paper-trading` | OPEN 추천 미실현 손익·비중·목표/손절 터치 |

---

### 추천·평가 (공개)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/recommendations` | 목록 (status·ticker 필터) |
| ✅ | GET | `/api/recommendations/{id}` | 단건 |
| 🔒 | POST | `/api/recommendations` | 생성 — KrxPreOpenJob·UsPreOpenJob 내부 호출 |
| 🔒 | PUT | `/api/recommendations/{id}/status` | 상태 변경 — ExitMonitorJob 자동 처리 |
| ✅ | GET | `/api/evaluations` | 목록 (recommendationId 필터) |
| ✅ | GET | `/api/evaluations/{id}` | 단건 |
| 🔒 | POST | `/api/evaluations` | 생성 — ExitMonitorJob 자동 처리 |

> Exit 판정은 `ExitMonitorJob` 룰 기반(목표가·손절가·만료) 자동 청산. 구 `exit-confirm` API 제거됨.

---

### 시장 데이터 (공개)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/market-data/daily-prices` | 일봉 조회 |
| ✅ | POST | `/api/market-data/daily-prices/sync` | KIS(KR)/Yahoo(US) 증분 동기화. 딜레이: `collection.kis.dailyPrice.delayMs` 설정 |
| ✅ | GET/POST | `/api/market-data/intraday-prices` | 장중 가격 |
| ✅ | GET/POST | `/api/market-data/news/sync` | KST 기준 당일 멀티소스 뉴스 |
| ✅ | GET/POST | `/api/market-data/disclosures/sync` | DART·SEC 공시 |
| ✅ | GET/POST | `/api/market-data/macro-observations/sync` | FRED 매크로 |
| ✅ | GET/POST | `/api/market-data/fundamentals/sync` | 펀더멘털 |
| ✅ | GET | `/api/universe` | 후보군 목록 |
| ✅ | POST | `/api/universe/sync/kr-symbols` | KIND 한국 심볼 동기화 |
| ✅ | POST | `/api/universe/sync/us-symbols` | NASDAQ Trader 미국 심볼 동기화 |
| ✅ | POST | `/api/universe/sync/us-prices` | Stooq 미국 시세 갱신 |

---

### 백테스트·AutoResearch (공개)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/backtests` | 목록 |
| ✅ | GET | `/api/backtests/{id}` | 단건 |
| ✅ | POST | `/api/backtests/simulate` | 일봉 기반 룰 백테스트 실행 |
| 🔒 | POST | `/api/backtests` | 결과 저장 — AutoResearchJob 내부 호출 |
| ✅ | GET | `/api/autoresearch/runs` | 실험 이력 |
| ✅ | POST | `/api/autoresearch/runs/auto` | 가중치 자동 실험 + champion 갱신 |
| 🔒 | POST | `/api/autoresearch/runs` | 실험 결과 저장 — 내부 호출 |
| ✅ | GET | `/api/autoresearch/strategies` | 전략 버전 목록 |
| 🔒 | POST | `/api/autoresearch/strategies` | 전략 저장 — 내부 호출 |

---

### 로그 조회 (공개)

| Swagger | Method | 경로 | 설명 |
|---------|--------|------|------|
| ✅ | GET | `/api/briefs` | 데일리 브리프 목록 |
| 🔒 | POST | `/api/briefs` | 저장 — Job 내부 호출 |
| ✅ | GET | `/api/notifications/logs` | 알림 발송 이력 |
| 🔒 | POST | `/api/notifications/logs` | 저장 — NotificationService 내부 호출 |
| ✅ | GET | `/api/codex/calls` | Codex 호출 이력 |
| 🔒 | POST | `/api/codex/calls` | 저장 — CodexClient 내부 호출 |

---

### 개발 전용 — Swagger 숨김, 운영 호출 금지 🚫

| 경로 | 설명 |
|------|------|
| `/api/dev/recommendations/generate` | 더미 추천 생성 |
| `/api/dev/universe/seed` | 유니버스 시드 |
| `/api/dev/brief/generate` | 브리프 생성 테스트 |
| `/api/dev/notifications/test` | Telegram 테스트 발송 |
| `/api/instruments` | 개발용 종목 seed 관리 |
| `/api/features` | feature 점수 직접 조회 |
| `/api/predictions` | 가격 예측 조회·생성 |

---

## 4. DB 테이블 (Flyway V1~V13)

| 테이블 | 설명 |
|--------|------|
| `app_setting` | 운영 설정 키-값 |
| `audit_log` | 설정 변경 이력 |
| `instrument` | 종목 마스터 |
| `market_universe` | 추천 후보군 |
| `recommendation` | 추천 이력 |
| `evaluation` | 추천 결과 평가 |
| `prediction` | 가격 예측 |
| `backtest_run` | 백테스트 실행 |
| `notification_log` | 알림 발송 이력 |
| `codex_call` | Codex CLI 호출 로그 |
| `daily_brief` | AI 생성 데일리 브리프 |
| `autoresearch_run` | AutoResearch 실행 이력 |
| `strategy_version` | 전략 버전 |
| `price_daily` | 일봉 시세 (V2) |
| `price_intraday` | 장중 시세 (V2) |
| `news_article` | 멀티소스 뉴스 (V4) |
| `macro_observation` | FRED 매크로 (V4) |
| `disclosure_event` | DART/SEC 공시 (V4) |
| `market_signal_score` | 시그널 스코어 (V5) |
| `fundamental_metric` | SEC/KIS/DART 펀더멘털 (V6) |
| `feature_snapshot` | PIT feature 점수와 forward return (V9) |
| `context_relation_analysis` | Codex 뉴스·공시 관계 분석 결과 (V13) |

V10은 `market_universe.name` 컬럼 길이를 `nvarchar(500)`으로 확장한다. V11은 기존 뉴스 수집 후보 기본값을 5개에서 20개로 확장하고, V12는 누락된 설정 키도 20으로 생성한다. V13은 Codex 뉴스·공시 관계 분석 결과를 저장하는 `context_relation_analysis`를 추가한다.

---

## 5. 외부 연동 클라이언트

| 클라이언트 | 대상 | 용도 |
|-----------|------|------|
| `KisApiClient` | 한국투자증권 OpenAPI | KR 장중 시세 |
| `KisTokenStore` | KIS OAuth | 토큰 인메모리 캐시 |
| `KrxOpenApiClient` | KRX OpenAPI | KOSPI/KOSDAQ 일별매매정보를 날짜·시장 단위로 수집해 국장 일봉 백필/프리오픈 속도 개선 |
| `KrxSymbolClient` | KRX/KIND | KR 상장 종목 목록 |
| `StooqQuoteClient` | Stooq CSV | US 최근 quote, 지수 일봉(KOSPI/SPY), ExitMonitor US 현재가 |
| `YahooFinanceClient` | Yahoo Finance chart API | US 후보군 일봉 히스토리 |
| `NasdaqTraderSymbolClient` | NASDAQ Trader | US 상장 종목 목록 |
| `RssNewsClient` | Google News / Yahoo Finance RSS | KR Google, US Yahoo+Google 당일 RSS 수집 |
| `NaverNewsClient` | Naver 뉴스 검색 API | KR 당일 뉴스 수집, 키 미설정 시 비활성 |
| `DisclosureClient` | DART(KR) / SEC EDGAR(US) | 공시 수집 |
| `FredMacroClient` | FRED 공개 CSV | 매크로 지표 |
| `SecFundamentalClient` / `KisApiClient` / `DartFundamentalClient` | SEC Company Facts / KIS 현재가 / DART 단일회사 주요계정 | US 펀더멘털 / KR PER·PBR·ROE / KR 재무 YoY |
| `CodexClient` | Codex CLI (ProcessBuilder) | AI 브리프 생성 |
| `TelegramClient` | Telegram Bot API | 알림 발송 |

> **dev-placeholder 모드**: `CODEX_COMMAND=dev-placeholder` 환경변수 시 CLI 없이 템플릿 응답 반환. KIS/DART/SEC도 동일 패턴.

---

## 6. 스케줄러 (크론잡)

수동 트리거: `POST /api/ops/jobs/{jobName}/trigger`

| 클래스 | 실행 시각 (KST) | jobName | 동작 |
|--------|----------------|---------|------|
| `KrxPreOpenJob` | 평일 08:30 | `krx-preopen` | KR 유니버스 동기화 + KRX OpenAPI 최근 실거래일 일봉 우선 + 추천 생성 + Telegram |
| `UsPreOpenJob` | 평일 22:00(DST)/23:00(표준) | `us-preopen` | US 심볼·시세 동기화 + 일봉 증분 + 추천 생성 + Telegram |
| `DailyPriceBackfillJob` | 평일 18:10 (KRX) | `backfill-kr` | KRX OpenAPI 날짜·시장 단위 KOSPI·KOSDAQ 일봉 백필, 실패 시 KIS fallback |
| `DailyPriceBackfillJob` | 화~토 07:20 (US) | `backfill-us` | NASDAQ·NYSE 일봉 히스토리 백필 (최대 50종목 × 180일) |
| `FeatureSnapshotJob` | 평일 22:00 | `feature-snapshot` | 전 종목 PIT feature 스냅샷 저장 + T+5·T+20 forward return 백필 |
| `ExitMonitorJob` | 평일 장중 5분 주기 | — | 목표가·손절가·만료 자동 청산. 추천 상태 CLOSED/EXPIRED 변경 + 평가 생성 |
| `MarketDataCollectionJob` | 평일 08:15 / 21:40 | — | 거래대금 상위 시장별 20종목의 당일 뉴스·공시·매크로 자동 수집 |
| `UsCloseSummaryJob` | 화~토 05:30(DST)/06:30(표준) | — | US 마감 요약 알림 |
| `AutoResearchJob` | 야간 | — | scoring weights 자동 실험 + champion 전략 갱신 |

---

## 7. 설정 프로퍼티 (`application-local.yml`)

```yaml
stock-advisor:
  admin:
    username: admin          # BasicAuth 계정
    password: change-me
  security:
    allowed-origins:
      - http://localhost:5173
    protect-dev-api: false   # true → /api/dev/** 도 인증 필요
  codex:
    command: dev-placeholder # 실제 CLI 경로로 교체
  kis:
    app-key: ...
    app-secret: ...
  telegram:
    bot-token: ...
    chat-id: ...
  dart:
    api-key: ...
  krx-openapi:
    auth-key: ...
  sec:
    user-agent: StockAdvisor/1.0 contact@example.com
```
