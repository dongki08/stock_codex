# Stock Advisor 작업명세서

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 11(로드맵) · 상태 🟢 현행(§0=2026-05-31 델타, §1↓=05-22 스냅샷)
>
> 작성 기준: 2026-05-22 (아래 §0 = 2026-05-31 재검토 델타)

## 📍 단계별 진행 맵 (어디까지 왔나 — 한눈에)

> 마일스톤 순서. 주차는 네 속도로 배치(예: M7을 1~3주차로). 상태: ✅완료 · 🔶부분 · ⚠️됐지만 결함 · ⛔미착수
> **현재 위치: 토대(M0~M6) 대부분 완성. M4가 결함, M7(수익 두뇌)이 다음 핵심.**
>
> 🔴 **즉시 수정 빌드 리스크**: `ExitConfirmServiceTest`가 삭제된 `ExitConfirmService`를 참조 → `gradlew test` 컴파일 실패. 해당 테스트 삭제/이관 먼저.

| 단계 | 내용 | 상태 | 근거/핵심 | 남은 일 |
|---|---|:--:|---|---|
| **M0** | 스켈레톤·API 계약 | ✅ | MSSQL·Swagger·`ResultDto`·Security | — |
| **M1** | 시장데이터 인프라 | ✅ | price/news/disclosure/macro/fundamental 수집 + 클라이언트 7종(KIS/Stooq/DART/SEC/FRED/RSS) | 펀더멘털 분기누적·정규화 후속 |
| **M2** | 추천 엔진(룰) | ⚠️ | `RecommendationEngine`·`UniverseFeatureBuilder`·`PricePredictor`·섹터캡 | 점수가 알파 약함 → M7에서 강화 |
| **M3** | 평가·Exit 자동화 | ✅ | `ExitMonitorJob` 룰 자동청산 + `evaluation`·confidence | — |
| **M4** | 자기개선 루프 | ⚠️ | `AutoresearchService`·`BacktestRunService` 본체 완성 | **백테스트 미래참조로 사실상 무효** → M7 TASK-1·2로 재작업 |
| **M5** | 알림·브리프·스케줄러 | 🔶 | `NotificationService` 4개 스케줄러+ExitMonitor 배선, Telegram·Codex 브리프, `ExternalApiPingClient` 헬스 | 실키 발송 검증, dev-placeholder→실연동 |
| **M6** | 프론트 운영화면 | ✅ | 추천/후보군/종목/통계/설정/수집/백테스트 화면 | 잔여 폴리시 |
| **M7** | 💰 수익 두뇌 강화 | ✅ | TASK-1·2·3·4·5·6·7·8 완료 | 운영 데이터로 검증. [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md) |
| **M8** | 실운영·검증 | 🔶 | `GET /api/stats/paper-trading` + 성과 통계 화면 페이퍼트레이딩 패널 + US 마감 운영 알림 요약으로 OPEN 추천 추적 시작 | 실제 Telegram 발송 검증 |

### 지금 당장 할 일 (순서)

1. ~~**M7 TASK-1·2**~~ ✅ — 백테스트 score 진입화 + Point-in-time 스냅샷.
2. ~~**M7 TASK-8·3·4**~~ ✅ — regime 필터 on / 펀더멘털·매크로 방향성화 / 뉴스 감성 주축.
3. ~~**M7 TASK-5**~~ ✅ — cross-sectional 표준화.
4. ~~**M7 TASK-6**~~ ✅ — IC 측정 + 가중치 가이드.
5. ~~**M7 TASK-7**~~ ✅ — 포지션 사이징.
6. **M5·M8** — 실제 Telegram 발송 검증 → 페이퍼 트레이딩으로 실제 수익률 추적.

> 상세 작업카드(대상 파일·변경·검증)는 [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md). 진행하며 완료 단계 상태(⛔→🔶→✅) 갱신.

---

## 0. 2026-05-29 갱신 델타 (이후 변경분)

> §1 이하 본문은 2026-05-22 스냅샷. 그 뒤 코드 변경 핵심만 여기 정리. 충돌 시 이 절·코드가 우선.

- **마이그레이션 V7·V8 추가** — V7 `market_universe.delisted_at`, V8 `exit_confirm_log` 테이블 제거.
- **ExitConfirm 기능 제거** — `ExitConfirmService`/`ExitConfirmLogEntity`/`ExitConfirmLogRepository`/DTO 삭제. Exit 판정은 `ExitMonitorJob`의 룰 기반(목표가/손절가/만료)으로 일원화.
- **AutoResearch 루프 본체 완성** — 가중치 mutation→백테스트→챔피언 승격/롤백(`AutoresearchService`). ⚠️ 백테스트 미래참조 한계 있음 → [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md) TASK-1·2.
- **비용 반영 PnL** — 거래세/수수료/슬리피지/환전 스프레드 반영(`PricePredictor`, `BacktestRunService`).
- **섹터 분산** — `recommendation.sector.max`(기본 2) 종목당 캡.
- **BCrypt** — admin 비밀번호 해시 저장.
- **신규 클라이언트(미커밋, 배선 완료)** — `DartFundamentalClient`(KR 재무 YoY)·`SentimentAnalysisClient`(외부 감성)는 `MarketDataCollectionService`에 연결. `NotificationService`는 4개 스케줄러+ExitMonitor에 배선. `ExternalApiPingClient`(ops) 추가. 각각 테스트 보유.
- **컨트롤러 19종** 운영 중(§ [30-BACKEND-OVERVIEW](30-BACKEND-OVERVIEW.md) 참조).
- **테스트 24종** (`RecommendationEngineTest`·`UniverseFeatureBuilderTest`·`BacktestRunServiceTest`·`PricePredictorTest` 등).
- 🔴 **빌드 리스크(즉시)**: `ExitConfirmServiceTest`가 삭제된 `ExitConfirmService`를 참조 → 컴파일 실패. 테스트 삭제/이관 필요.
- **남은 핵심 과제**: 수익률 두뇌 강화 → [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md). 결함 이력 → [41-DEFECTS-AND-FIXES](41-DEFECTS-AND-FIXES.md).
- **2026-05-31 M7 TASK-1·2·3·4·8 완료** — 백테스트 score 진입(미래참조 제거), PIT 피처 스냅샷(`feature_snapshot` V9), 펀더멘털/매크로 방향성 점수, 뉴스 감성 주축, regime 필터 기본 ON + 지수 일봉 적재. `gradlew test` BUILD SUCCESSFUL.
- **2026-05-31 M7 TASK-5 완료** — `UniverseFeatureBuilder.buildFeatures()`에서 시장별 raw feature 점수를 백분위 기반 cross-sectional 점수로 변환 후 재합산. `featureJson`에 `raw*Score`, `crossSectionalNormalized`, `feature-rule-v4` 기록. `gradlew test` BUILD SUCCESSFUL. 다음: TASK-6·7.
- **2026-05-31 M7 TASK-6 완료** — `FeatureICService`가 `feature_snapshot`의 피처값과 forward return 간 Spearman IC를 계산하고, `AutoresearchService` mutation을 IC 기반 증감으로 가이드. `autoresearch_run.diff_summary`에 IC 요약 기록. `gradlew test` BUILD SUCCESSFUL. 다음: TASK-7.
- **2026-05-31 M7 TASK-7 완료** — `PricePredictor`가 변동성/position sizing 원점수를 산출하고, 추천 생성 시 confidence×역변동성 점수를 묶음별 총 100%, 종목당 20% 상한으로 정규화해 `signalsJson.positionWeightPct`에 기록. `gradlew test` BUILD SUCCESSFUL. 다음: M5·M8 실키 검증/페이퍼트레이딩.
- **2026-05-31 M8 페이퍼트레이딩 모니터링 착수** — `GET /api/stats/paper-trading` 추가. OPEN 추천의 최신 일봉 종가 기준 미실현 손익, 비중 반영 손익, 목표/손절 터치 상태를 반환. `gradlew test` BUILD SUCCESSFUL. 다음: 프론트 패널/운영 알림 연동, 실제 Telegram 발송 검증.
- **2026-05-31 M8 페이퍼트레이딩 프론트 패널 완료** — 성과 통계 화면에 OPEN 추천 페이퍼트레이딩 요약/포지션 표 추가. `npm run build` BUILD SUCCESSFUL. 다음: 운영 알림 연동, 실제 Telegram 발송 검증.
- **2026-05-31 M8 페이퍼트레이딩 운영 알림 연동** — US 마감 요약 알림에 `StatsService.getPaperTrading()` 기반 OPEN 추천 수, 가격 확인 수, 평균/비중 미실현 손익, 목표/손절 터치 수, 리스크 체크 포지션을 포함. `gradlew test` BUILD SUCCESSFUL. 다음: 실제 Telegram Bot Token/Chat ID 환경에서 도착 및 `notification_log` 성공 기록 검증.
- **2026-05-31 Telegram 실발송 검증 정보 보강** — `TelegramClient`가 dev-placeholder 여부, HTTP 상태 코드, 실패 원인을 담은 상세 결과를 반환하고 `/api/dev/notifications/test` 및 `notification_log.error_message`에 반영. 다음: 실제 환경 변수 설정 후 테스트 API 호출.

### 2026-05-29 코드 감사 결과 (실측)

| 구분 | 실측 | 비고 |
|---|---|---|
| 컨트롤러 | 19 | — |
| application 서비스 | 18 | NotificationService·NotificationLogService 포함 |
| infra 클라이언트 | 13 | Dart·Sentiment·ExternalApiPing 신규 |
| 스케줄러 | 6 + SettingReader | KRX/US 프리오픈·US마감·수집·AutoResearch·ExitMonitor |
| Flyway | V1~V8 | exit_confirm_log 제거됨(V8) |
| 테스트 | 24 | ⚠️ ExitConfirmServiceTest 오펀 |
| 프론트 화면 | 7 | Admin/Backtests/Instruments/MarketData/Recommendations/Stats/Universe |

---

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
| 백테스트 API | 부분 완료 | 백테스트 실행 결과 저장/조회, price_daily 기반 룰 시뮬레이션 실행 |
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
| 뉴스/공시/매크로/펀더멘털 API | 부분 완료 | RSS, DART/SEC, FRED, SEC Company Facts 수집/조회 API와 스케줄러 연결 |
| Telegram 테스트 발송 | 부분 완료 | dev-placeholder 또는 실제 Telegram 전송, notification_log 저장 |
| Codex CLI 호출 | 부분 완료 | ProcessBuilder 호출, 일 호출/예상 예산 한도 차단, 수집 데이터 기반 브리프/Exit Confirm fallback, 엄격한 ACTION 파싱, codex_call 저장 |
| 스케줄러 골격 | 부분 완료 | KRX/US 프리오픈 Job이 후보군/일봉/추천/알림 플로우를 실행하고 ExitMonitor가 국내 현재가 스냅샷/Exit Confirm을 처리 |
| BasicAuth/Actuator | 부분 완료 | `/api/admin/**`, `/api/ops/**`, `/actuator/**` BasicAuth 보호, `/api/dev/**` 기본 보호, BCrypt 패스워드 저장 |
| 운영 헬스체크 | 부분 완료 | `/api/ops/external-health`에서 KIS/Telegram/Codex/Stooq/KIND 설정 상태, Codex 일 호출량/예상 예산, 수집 데이터 최신성 READY/STALE/NO_DATA 조회 |
| Flyway 마이그레이션 | 완료 | `V1`~`V8` 적용 (V8: `exit_confirm_log` 제거), `ddl-auto: validate` 기준 |
| Swagger 문서 | 완료 | `http://localhost:8083/swagger-ui.html` |

### 1.2 프론트 구현 완료

| 화면 | 상태 | 설명 |
|---|---|---|
| 화면 탭 전환 | 완료 | 오늘의 추천 / 시장 후보군 / 종목 관리 / 성과 통계 / 관리자 설정 |
| 관리자 설정 화면 | 완료 | 설정 조회, 검색, 그룹 탭, 저장, 기본값 초기화 |
| 오늘의 추천 화면 | 완료 | 상태/시장/종목 필터, 개발용 추천 생성, 추천 카드 표시 |
| 추천 평가 화면 | 완료 | 추천 카드에서 평가 입력, 평가 이력 조회, CLOSED/EXPIRED 상태 변경 |
| 종목 관리 화면 | 완료 | 개발용/수동 보정용 종목 등록, 조회, 검색, 수정 |
| 시장 후보군 화면 | 완료 | 개발용 seed, 미국/한국 동기화, 후보군 필터, 기술/맥락/품질 feature 점수 표시 |
| 수집 데이터 화면 | 완료 | 일봉, 뉴스, 공시, 매크로, 펀더멘털 조회/수동 동기화 |
| 성과 통계 화면 | 완료 | 요약 카드, 누적 ROI 차트, 기간별/일별/전략별 성과 표시 |
| 백테스트 화면 | 부분 완료 | 일봉 기반 시뮬레이션 실행, 최근 결과 요약, 실행 이력 표시 |
| 일봉 동기화 버튼 | 완료 | 시장 후보군 화면에서 후보 수/기간 지정 후 일봉 동기화 실행 |
| API 프록시 | 완료 | Vite `/api` → `http://localhost:8083` |

### 1.3 문서 작성 완료

| 문서 | 설명 |
|---|---|
| `docs/20-API.md` | 상세 API 명세 |
| `docs/21-API-QUICK.md` | 한눈에 보는 API 요약 |
| `docs/32-SYSTEM-FLOW.md` | 전체 흐름도 |
| `docs/11-ROADMAP.md` | 현재 문서 |

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
| US Job | `UsPreOpenJob`, `UsCloseSummaryJob` 골격 구현, DST/표준시 분기, 마감 요약 고도화 완료 |
| Exit Job | `ExitMonitorJob` 골격 구현 완료 |
| 관리자 설정 연동 | 추천 개수, 시장 활성화, DST 분기, 설정 기반 휴장일 차단, 프리오픈/마감 알림 시각 동적 변경, ExitMonitor 폴링 주기 반영 |

### P1-3. 개발용 추천 자동 생성 스케줄 연결

| 항목 | 내용 |
|---|---|
| 목적 | 실제 엔진 전까지 `dev-rule-v0` 자동 추천을 스케줄로 실행한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 수동 버튼 없이 KRX/US 프리오픈 시간에 추천이 생성됨 |

현재 KRX/US 프리오픈 Job에서 `DevRecommendationGenerateService`를 호출하고, `recommendation.short.count`, `recommendation.long.count`, `recommendation.market.enabled`를 반영한다. KRX 프리오픈은 `notification.krx.preopen.offsetMinutes.displayTime`, US 프리오픈은 `notification.us.preopen.offsetMinutes.dstTime/standardTime`, US 마감은 `notification.us.close.offsetMinutes.dstTime/standardTime`을 매분 확인해 설정 시각에만 실행한다. ExitMonitor도 매분 확인하되 `exit.polling.intervalMinutes` 주기와 일치하는 분에만 실제 실행한다. `notification.holiday.kr.closedDates`, `notification.holiday.us.closedDates`에 등록된 날짜는 정규 작업을 건너뛰며, `notification.holiday.enabled`가 true면 휴장 알림만 보낸다. US 마감 요약은 미국 OPEN 추천의 최신 일봉 종가 기준 평균 손익, 목표 도달, 손절 근접, 예상 종료일 초과, 하위 손익 5건을 전송한다.

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
| 관리자 설정 연동 | `recommendation.marketcap.*`, `recommendation.turnover.*`, 제외 섹터/종목 설정 일부 반영 완료. 시총/거래대금 값이 수집된 후보에 하한 적용 |

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
| 상태 | 부분 완료 |
| 완료 기준 | 종목별 최근 뉴스 제목/링크 저장 가능 |

현재 Google News / Yahoo Finance RSS 기반 수집, 저장, 조회, 스케줄러 연동이 구현되어 있다. 뉴스 감성 점수는 간단한 룰 기반이며 추천 feature 반영은 아직 제한적이다.

### P2-4. 공시/매크로 수집

| 항목 | 내용 |
|---|---|
| 목적 | 장기 추천과 브리프에 필요한 공시/매크로 데이터 확보 |
| 소스 | DART, SEC EDGAR, FRED |
| 상태 | 부분 완료 |
| 완료 기준 | 종목 공시와 주요 지표를 조회/저장 가능 |

현재 DART/SEC 공시, FRED 매크로, SEC Company Facts 펀더멘털 수집/조회 API가 있다. 한국 펀더멘털, 환율, 섹터 로테이션, 추천 엔진 feature 결합은 남아 있다.

## P3. 실제 추천 엔진

### P3-1. FeatureBuilder

| 항목 | 내용 |
|---|---|
| 목적 | 자동 후보군의 시세/거래량/뉴스/펀더멘털 데이터를 추천 점수로 변환한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 후보군 전체에 대해 feature JSON 생성 가능 |

현재 구현은 유동성, 가격 구간, 데이터 품질, price_daily 기반 RSI/이동평균/거래량에 뉴스/공시/매크로/펀더멘털 컨텍스트 점수를 결합하는 `feature-rule-v2` 수준이다. 시장 후보군 화면에서도 맥락 점수와 수집 데이터 건수를 표시한다.

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

현재 구현은 독립 `RecommendationEngine`에서 feature 점수순 Top-N을 생성하고 제외 섹터/종목, 최소 점수, 최소 데이터 품질, 시장별 시총/거래대금 하한 설정을 반영한다. 시총/거래대금 값이 아직 없는 후보는 데이터 품질 점수에서 불리하게 두고, 하한 필터는 값이 수집된 경우에만 적용한다.

### P3-3. PricePredictor

| 항목 | 내용 |
|---|---|
| 목적 | 목표가, 손절가, 예상 매도일을 계산한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 추천 생성 시 entry/target/stop/expectedExitAt 자동 산출 |

현재 구현은 독립 `PricePredictor`에서 최근 일봉 변동성을 기반으로 entry/target/stop/expectedExitAt을 산출한다. 일봉이 없으면 후보군 `lastPrice`를 사용하고, 일봉과 `lastPrice`가 모두 없으면 해시 기반 더미 가격을 만들지 않고 해당 후보를 추천 생성에서 제외한다.

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
| 설정 키 | `codex.profile`, `stock-advisor.codex.command`, `codex.daily.callLimit`, `codex.daily.budgetUsd`, `codex.estimatedUsdPer1kChars`, `codex.estimatedResponseChars` 사용 완료 |
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

현재 `/api/dev/brief/generate`는 OPEN 추천, 시장 후보군, 일봉, 뉴스, 공시, 매크로, 펀더멘털, 성과 통계 컨텍스트를 묶어 Codex에 전달한다. Codex CLI 미설정 또는 실패 시 로컬 템플릿 브리프로 fallback한다. 남은 작업은 컨텍스트 품질 고도화, 토큰/호출량 제한, 실제 운영 스케줄 메시지 포맷 정리다.

### P4-3. Exit Confirm

| 항목 | 내용 |
|---|---|
| 목적 | 손절 위험 구간에서 Codex가 HOLD/CUT/TIGHTEN 판단을 보조한다. |
| 상태 | 부분 완료 |
| 완료 기준 | 위험 구간 진입 시 Codex 판단 결과 저장 |

현재 수동 API와 ExitMonitorJob 자동 호출이 구현되어 있다. 최근 price_intraday/price_daily, 추천 signalsJson, 손절 이격률을 포함해 Codex를 호출하고, dev-placeholder 또는 실패 시 규칙 기반 fallback을 사용한다. 손절가 이하의 명확한 CUT 케이스는 Codex를 호출하지 않고 룰 기반으로 즉시 판단하며, Codex 응답은 첫 줄의 엄격한 `ACTION: HOLD|CUT|TIGHTEN` 형식만 인정한다. ExitMonitor는 목표가 도달/손절가 이탈/예상 종료일 초과 시 중복 평가가 없을 때 평가를 생성하고 추천 상태를 CLOSED 또는 EXPIRED로 자동 전환한다. 남은 작업은 RSI/MA 기반 HOLD, 위험구간 지속시간 기반 TIGHTEN, 실제 운영 메시지 포맷과 중복 알림 정책 고도화다.

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
| 상태 | 완료 |
| 완료 기준 | ROI 곡선, Hit Rate 카드, 최근 결과 목록 표시 |

현재는 Recharts 없이 SVG 기반 누적 ROI 차트, 요약 카드, 일별/기간별/전략별 표를 표시한다. `/api/stats/daily`는 일별 평균 손익, 일별 총 손익, 누적 손익을 함께 반환한다.

### P5-3. 일봉 기반 백테스트 실행

| 항목 | 내용 |
|---|---|
| 목적 | 저장된 `price_daily` 데이터로 전략 성과를 즉시 계산하고 `backtest_run`에 저장한다. |
| API | `POST /api/backtests/simulate` |
| 상태 | 부분 완료 |
| 완료 기준 | 시장/기간/종목수/목표/손절/보유일 조건으로 백테스트 결과 JSON 저장 |

현재 `ma20-breakout-v0` 기본 룰로 20일 이동평균 이상이면 다음 거래일 종가 진입, 목표가/손절가/최대 보유일 중 먼저 발생한 조건으로 청산한다. 결과는 tradeCount, hitRate, avgPnlPct, totalPnlPct, maxDrawdownPct, 청산 사유별 건수, 샘플 트레이드로 저장된다.

프론트에는 `백테스트` 탭이 추가되어 시장/기간/종목 수/보유일/목표/손절 조건으로 시뮬레이션을 실행하고 최근 결과와 실행 이력을 확인할 수 있다.

2026-05-22 기준 백테스트 PnL은 `backtest.slippage.percent`, `backtest.cost.kr`, `backtest.cost.us` 설정을 읽어 시장별 effective entry/exit 가격으로 계산한다. metrics JSON에는 `entryCostPct`, `exitCostPct`, `slippagePct`, `roundTripCostPct`와 샘플별 `effectiveEntryPrice`, `effectiveExitPrice`가 포함된다. 실추천 `PricePredictor`의 target/stop도 같은 비용 모델로 보정한다.

## P6. 보안/운영

### P6-1. BasicAuth

| 항목 | 내용 |
|---|---|
| 목적 | 개인용 관리자 페이지와 API 보호 |
| 상태 | 부분 완료 |
| 완료 기준 | 인증 없이는 `/api/admin/**` 접근 불가 |

현재 `/api/admin/**`, `/api/ops/**`, `/actuator/**`는 BasicAuth로 보호된다. `/api/dev/**`도 기본 설정에서 보호되며, 관리자 패스워드는 BCrypt로 인코딩해 메모리 사용자 저장소에 등록한다. 나머지 조회/업무 API의 운영 공개 정책은 아직 최종 확정이 필요하다.

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

Actuator 의존성과 보호 설정은 있다. `/api/ops/external-health`는 외부 연동 설정 상태, Codex 일 호출량/예상 예산, price_daily/price_intraday/news/disclosure/macro/fundamental 최신 저장 상태를 함께 반환한다. 수집 데이터는 관리자 설정의 `ops.health.*.maxAge*` 기준으로 `READY`, `STALE`, `NO_DATA`를 표시한다. 남은 작업은 실제 외부 API별 ping/check 호출과 운영 알림 연동이다.

## 5. 추천 개발 순서

가장 현실적인 순서는 아래와 같다.

| 순서 | 작업 | 이유 |
|---:|---|---|
| 1 | 로컬 검증 복구 | `gradlew.bat test`, `npm run build`를 최신 코드 기준으로 다시 확인 |
| 2 | 스케줄러 운영 설정 연동 | 부분 완료. 추천 개수, 시장 활성화, 알림 시각, 휴장일, DST 분기를 운영 설정과 연결 |
| 3 | 데이터 수집 품질 보강 | RSS/공시/매크로/펀더멘털 수집 결과를 추천 feature와 브리프 품질에 반영 |
| 4 | RecommendationEngine 고도화 | 부분 완료. 뉴스/공시/펀더멘털/매크로 feature와 시장별 최소 조건을 랭킹에 반영 |
| 5 | 통계/백테스트 실체화 | 부분 완료. 저장형 백테스트를 실제 과거 가격 시뮬레이션으로 확장하고 통계 ROI 차트 구현 |
| 6 | 운영 보강 | 부분 완료. 보호 API 범위, 개발 API 보호 옵션, CORS origin, 외부 연동 상태, Codex 호출/예상 예산 한도 정리 |

## 6. 다음 작업 상세 명세

다음 작업은 `로컬 검증 복구`로 한다.

### 6.1 목표

최신 코드 기준으로 백엔드 테스트와 프론트 빌드가 성공하는지 확인하고, 실패하면 우선 복구한다.

### 6.2 구현 파일

| 파일 | 작업 |
|---|---|
| `apps/backend/src/test/java/**` | 실패 테스트가 있으면 최신 서비스 반환 타입과 정책에 맞게 수정 |
| `apps/backend/build.gradle` | 테스트 실행 설정 확인 |
| `apps/web/src/**` | TypeScript 빌드 오류가 있으면 수정 |
| `apps/web/package.json` | 빌드 스크립트와 의존성 확인 |

### 6.3 구성

| 영역 | 내용 |
|---|---|
| 백엔드 | `gradlew.bat test` 또는 WSL 실행 가능한 Gradle 테스트 |
| 프론트 | `npm run build` |
| 문서 | 검증 결과를 현재 작업명세에 반영 |

### 6.4 완료 기준

| 기준 | 확인 방법 |
|---|---|
| 백엔드 테스트 | 테스트 전체 성공 |
| 프론트 빌드 | TypeScript + Vite 빌드 성공 |
| 실패 시 조치 | 실패 원인과 수정 파일을 기록 |
| 다음 단계 | 스케줄러 운영 설정 연동으로 이동 |

### 6.5 2026-05-22 검증 결과

| 항목 | 결과 | 비고 |
|---|---|---|
| 백엔드 테스트 | 성공 | Windows PowerShell에서 OpenJDK 17 기준 `apps/backend`의 `.\gradlew.bat test --console=plain` 성공. Gradle 8.14 wrapper 배포판 다운로드 후 전체 테스트 통과 |
| 프론트 빌드 | 성공 | PowerShell 실행 정책 때문에 `npm` 대신 `npm.cmd run build`로 실행. TypeScript + Vite 빌드 성공 |

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
| 실제 추천 엔진 | 부분 구현됨. RecommendationEngine/PricePredictor 분리, 제외 섹터/종목/최소 점수/품질/시장별 시총·거래대금 하한, 시장 레짐 필터, 섹터 분산, 포지션 비중 산정 반영 |
| 백테스트 실행 | 부분 구현됨. 결과 저장/조회와 price_daily 기반 `ma20-breakout-v0` 시뮬레이션 API 구현, 설정 기반 거래비용/슬리피지 반영 |
| 스케줄러 | 부분 구현됨. KRX/US 프리오픈은 수집/추천/알림 연결됨. 추천 개수/시장 활성화/DST/알림 시각/설정 기반 휴장일 차단, US_CLOSE 요약 고도화, ExitMonitor 폴링 주기 반영 완료 |
| 통계 화면 | 구현됨. 누적 ROI 차트, 요약 카드, 일별/기간별/전략별 표 표시 |
| 보안 | 부분 구현됨. 관리자/운영/Actuator BasicAuth 적용, 개발 API 기본 보호, BCrypt 패스워드 저장, CORS origin 설정 추가, Codex 일 호출/예상 예산 한도 차단, 운영 헬스체크 수집 데이터 READY/STALE/NO_DATA 표시 구현, 전체 운영 공개 정책은 미정 |
