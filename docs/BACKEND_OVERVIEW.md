# Stock Advisor — 백엔드 전체 구조

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
{
  "success": true,
  "data": { ... },
  "error": null
}
```

모든 API는 `ResultDto<T>` 로 래핑. 실패 시 `success: false`, `error: { code, message }`.

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

## 3. API 목록

### 3-1. 추천 `/api/recommendations`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/recommendations` | 추천 목록 조회 (status, ticker, market 필터) |
| GET | `/api/recommendations/{id}` | 추천 단건 조회 |
| POST | `/api/recommendations` | 추천 수동 생성 |
| PUT | `/api/recommendations/{id}/status` | 상태 변경 (OPEN→CLOSED/EXPIRED 등) |
| POST | `/api/recommendations/{id}/exit-confirm` | Exit 확인 기록 저장 |

**추천 상태**: `OPEN` → `CLOSED` (목표 달성) / `EXPIRED` (기간 만료)

---

### 3-2. 평가 `/api/evaluations`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/evaluations` | 평가 목록 (recommendationId 필터 가능) |
| GET | `/api/evaluations/{id}` | 평가 단건 |
| POST | `/api/evaluations` | 평가 생성 (pnlPct, exitReason, hitFlag 등 입력) |

추천이 종료될 때 실제 수익률 및 성공 여부를 기록.

---

### 3-3. 성과 통계 `/api/stats`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/stats/summary` | 전체 요약 (총건수, hitRate, avgPnl, maxDrawdown, 기간별/종료사유별 분류) |
| GET | `/api/stats/daily` | 날짜별 추천 수 / hit 수 / 평균 손익 |
| GET | `/api/stats/by-strategy` | 모델버전(전략)별 성과 비교 |

DB 조회 후 Java 인메모리 집계.

---

### 3-4. 시장 유니버스 `/api/universe`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/universe` | 후보군 목록 (market, isActive 필터) |
| POST | `/api/universe/sync/us-symbols` | NASDAQ Trader CSV → market_universe 동기화 |
| POST | `/api/universe/sync/kr-symbols` | KRX/KIND → market_universe 동기화 |
| POST | `/api/universe/sync/us-prices` | Stooq 시세 → price_daily 동기화 |

---

### 3-5. 시장 데이터 `/api/market-data`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/market-data/daily-prices` | 일봉 가격 조회 (ticker, from, to 필터) |
| POST | `/api/market-data/daily-prices/sync` | 일봉 시세 동기화 (Stooq) |
| GET | `/api/market-data/intraday-prices` | 장중 가격 조회 |
| GET | `/api/market-data/news` | RSS 수집 뉴스 조회 |
| POST | `/api/market-data/news/sync` | Google News / Yahoo Finance RSS 수집 |
| GET | `/api/market-data/disclosures` | 공시 조회 |
| POST | `/api/market-data/disclosures/sync` | DART(KR) / SEC EDGAR(US) 공시 수집 |
| GET | `/api/market-data/macro-observations` | 매크로 지표 조회 |
| POST | `/api/market-data/macro-observations/sync` | FRED 매크로 지표 수집 |
| GET | `/api/market-data/fundamentals` | 펀더멘털 조회 |
| POST | `/api/market-data/fundamentals/sync` | SEC Company Facts 펀더멘털 수집 |

---

### 3-6. 종목 관리 `/api/instruments`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/instruments` | 종목 목록 |
| GET | `/api/instruments/{ticker}` | 종목 단건 |
| POST | `/api/instruments` | 종목 등록 |
| PUT | `/api/instruments/{ticker}` | 종목 수정 |

---

### 3-7. 데일리 브리프 `/api/briefs`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/briefs` | 브리프 목록 (marketTrack 필터) |
| GET | `/api/briefs/{id}` | 브리프 단건 |
| POST | `/api/briefs` | 브리프 수동 저장 |

---

### 3-8. Codex 호출 로그 `/api/codex/calls`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/codex/calls` | Codex CLI 호출 이력 목록 |
| GET | `/api/codex/calls/{id}` | 단건 |
| POST | `/api/codex/calls` | 수동 로그 저장 |

---

### 3-9. 알림 로그 `/api/notifications/logs`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications/logs` | 알림 발송 이력 목록 |
| GET | `/api/notifications/logs/{id}` | 단건 |
| POST | `/api/notifications/logs` | 수동 로그 저장 |

---

### 3-10. 가격 예측 `/api/predictions`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/predictions` | 예측 목록 (ticker 필터) |
| GET | `/api/predictions/{id}` | 단건 |
| POST | `/api/predictions` | 예측 저장 |

---

### 3-11. 백테스트 `/api/backtests`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/backtests` | 백테스트 실행 목록 |
| GET | `/api/backtests/{id}` | 단건 |
| POST | `/api/backtests` | 백테스트 결과 저장 |

---

### 3-12. AutoResearch `/api/autoresearch`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/autoresearch/runs` | AutoResearch 실행 이력 |
| GET | `/api/autoresearch/runs/{id}` | 단건 |
| POST | `/api/autoresearch/runs` | 실행 기록 저장 |
| GET | `/api/autoresearch/strategies` | 전략 버전 목록 |
| GET | `/api/autoresearch/strategies/{id}` | 단건 |
| POST | `/api/autoresearch/strategies` | 전략 버전 저장 |

---

### 3-13. 유니버스 Feature `/api/features`

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/features/universe` | 후보군 전체의 시그널 스코어 + 기술적 feature 조회 |

---

### 3-14. 관리자 설정 `/api/admin` *(BasicAuth 필수)*

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/admin/settings` | 앱 설정 전체 조회 |
| GET | `/api/admin/settings/{key}` | 단건 조회 |
| PUT | `/api/admin/settings/{key}` | 설정 값 수정 |
| POST | `/api/admin/settings/reset` | 기본값으로 초기화 |
| GET | `/api/admin/audit-logs` | 감사 로그 조회 |

---

### 3-15. 운영 상태 `/api/ops` *(BasicAuth 필수)*

| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/ops/external-health` | KIS / Telegram / DART / SEC / FRED 연결 상태 체크 |

---

### 3-16. 개발 전용 `/api/dev` *(인증 선택)*

| Method | 경로 | 설명 |
|--------|------|------|
| POST | `/api/dev/brief/generate` | Codex CLI 호출해 데일리 브리프 생성 (dev-placeholder 모드 지원) |
| POST | `/api/dev/notifications/test` | Telegram 테스트 알림 발송 |
| POST | `/api/dev/recommendations/generate` | 추천 자동 생성 (RecommendationEngine 직접 실행) |
| POST | `/api/dev/universe/seed` | 시장 유니버스 초기 데이터 seed |

---

## 4. DB 테이블 (Flyway V1~V6)

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
| `exit_confirm_log` | Exit 확인 로그 (V3) |
| `news_article` | RSS 뉴스 (V4) |
| `macro_observation` | FRED 매크로 (V4) |
| `disclosure_event` | DART/SEC 공시 (V4) |
| `market_signal_score` | 시그널 스코어 (V5) |
| `fundamental_metric` | SEC 펀더멘털 (V6) |

---

## 5. 외부 연동 클라이언트

| 클라이언트 | 대상 | 용도 |
|-----------|------|------|
| `KisApiClient` | 한국투자증권 OpenAPI | KR 장중 시세 |
| `KisTokenStore` | KIS OAuth | 토큰 인메모리 캐시 |
| `KrxSymbolClient` | KRX/KIND | KR 상장 종목 목록 |
| `StooqQuoteClient` | Stooq CSV | US/KR 일봉 시세 (무료) |
| `NasdaqTraderSymbolClient` | NASDAQ Trader | US 상장 종목 목록 |
| `RssNewsClient` | Google News / Yahoo Finance RSS | 뉴스 수집 |
| `DisclosureClient` | DART(KR) / SEC EDGAR(US) | 공시 수집 |
| `FredMacroClient` | FRED 공개 CSV | 매크로 지표 |
| `SecFundamentalClient` | SEC Company Facts | US 펀더멘털 |
| `CodexClient` | Codex CLI (ProcessBuilder) | AI 브리프 생성 |
| `TelegramClient` | Telegram Bot API | 알림 발송 |

> **dev-placeholder 모드**: `CODEX_COMMAND=dev-placeholder` 환경변수 시 CLI 없이 템플릿 응답 반환. KIS/DART/SEC도 동일 패턴.

---

## 6. 스케줄러 (크론잡)

| 클래스 | 실행 시각 (KST) | 동작 |
|--------|----------------|------|
| `KrxPreOpenJob` | 평일 08:30 | KR 장 전 데이터 수집 + 브리프 생성 |
| `MarketDataCollectionJob` | 평일 08:15 | KR 데이터 수집 시작 |
| `MarketDataCollectionJob` | 평일 21:40 | US 데이터 수집 시작 |
| `MarketDataCollectionJob` | 평일 07:00 | 전일 데이터 정리 |
| `UsPreOpenJob` | 평일 22:00 | US 장 전 브리프 생성 |
| `UsCloseSummaryJob` | 화~토 05:30 | US 마감 요약 |
| `ExitMonitorJob` | 평일 09:00~15:55 (5분마다) | KR 보유 종목 Exit 조건 모니터링 |

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
  sec:
    user-agent: StockAdvisor/1.0 contact@example.com
```
