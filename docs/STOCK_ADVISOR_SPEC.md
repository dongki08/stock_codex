# 📈 Daily Stock Advisor — 개발명세서 (v1.2)

> **프로젝트 코드명**: `stock-advisor`
> **작성일**: 2026-05-18 (v1.2 갱신)
> **작성자**: park
> **타깃 시장**: KOSPI / KOSDAQ + NYSE / NASDAQ
> **기술 스택**: Spring Boot 3.x (Java 21) + React 19 + PostgreSQL(TimescaleDB) + Redis
> **LLM 백엔드**: **OpenAI Codex CLI (GPT Pro 구독)** — 별도 API 키/과금 없음
> **운영 형태**: 개인용(Self-hosted, Single Tenant)
> **알림 채널**: Telegram Bot (Primary) + KakaoTalk(나에게 보내기, Secondary)

### 🆕 v1.2 변경 이력 (Changelog)
- **변경**: 프리오픈 알림 시각을 **개장 30분 전**으로 통일 (KRX 08:30 / US 22:00 DST·23:00 STD)
- **변경**: 모든 핵심 파라미터(종목 개수·백테스트 기간·예산·알림 시각·손절 모드)를 **관리자 페이지(/admin)** 에서 런타임 조정 가능
- **신규**: **OpenAI Codex CLI** 를 AutoResearch / Daily Brief 실행 백엔드로 채택 → GPT Pro 구독 내에서 처리, 별도 API 키·과금 제거
- **변경**: 유료 데이터(Polygon) 제외, **무료 소스만 사용** (KIS Open API · pykrx · yfinance · DART · SEC EDGAR · FRED · RSS)
- **추가**: 손절 모니터링을 **하이브리드(Polling + AI Trigger)** 로 명세화 — §8 확장
- **추가**: 백테스트 기본 기간 **5년** (사용자 변경 가능) 권고

### 🆕 v1.1 (이전)
- Karpathy **AutoResearch Loop** 패턴(`karpathy/autoresearch`)을 동향 분석/시그널 최적화 모듈로 통합 → §18
- 07:00 KST 알림을 국장 프리오픈 브리핑으로 명확화
- 미국장 프리오픈/마감 결산 알림 추가 (DST 자동 분기)

---

## 1. 프로젝트 개요

### 1.1 목적
하루 두 차례 — **국장 프리오픈(KRX 개장 30분 전, 기본 08:30 KST)** 과 **미장 프리오픈(NYSE/NASDAQ 개장 30분 전, DST 자동 분기)** — 에 그날(혹은 그 주/그 분기)의 매수 후보 종목을 **단기 / 장기**로 구분하여 추천하고, **목표가·손절가·매도 시점**까지 함께 제공한다. 추천 결과는 매일 DB에 적재되어 **사후 검증**을 거치며, 누적된 성공/실패 통계를 바탕으로 **Karpathy AutoResearch Loop** 패턴 + **OpenAI Codex CLI** 가 모델과 시그널 가중치를 야간에 자동으로 **자기 개선(Self-Improvement)** 한다. 모든 LLM 추론은 사용자의 **GPT Pro 구독 내 Codex CLI** 로 위임하여 별도 API 과금이 발생하지 않는다.

### 1.2 핵심 가치
| # | 가치 | 설명 |
|---|------|------|
| 1 | **자동화** | 사람이 개입하지 않아도 매일 7시에 신뢰도 있는 시그널이 도착 |
| 2 | **검증 가능성** | 모든 추천은 DB에 기록되고 사후 결과로 정량 평가 |
| 3 | **자기 개선** | 실적 데이터로 모델 가중치/필터를 주기적으로 재학습 |
| 4 | **투명성** | 통계 페이지에서 hit-rate, ROI, Drawdown 등 모두 가시화 |

### 1.3 비목표(Out of Scope)
- 실제 주문 자동 집행(증권사 API 연동 매매)은 v1에서 제외 → **추천 시스템에 집중**
- 다중 사용자 / 결제 / 권한 시스템 (개인용)
- 옵션·선물·파생상품 (v1은 현물 주식/ETF만)

---

## 2. 도메인 사전 지식 (반드시 반영해야 할 룰)

### 2.1 시장별 거래 시간 (KST 기준)
| 시장 | 정규장 | 시간외 | 비고 |
|------|--------|--------|------|
| KOSPI/KOSDAQ | 09:00 ~ 15:30 | 장전 08:30~09:00 / 장후 15:40~18:00 | 점심시간 없음(2000년 폐지) |
| NYSE/NASDAQ (서머타임 적용 시) | 22:30 ~ 05:00 | Pre-market 17:00~22:30 / After 05:00~09:00 | 3월~11월 |
| NYSE/NASDAQ (표준시) | 23:30 ~ 06:00 | Pre 18:00~23:30 / After 06:00~10:00 | 11월~3월 |

→ 두 시장 모두 **개장 30분 전** 알림이 기본값. 사용자가 관리자 페이지에서 ±분 단위로 미세 조정 가능.

#### ⏰ 발송 스케줄 (DST 자동 분기, 모두 관리자 페이지에서 조정 가능)
| 트리거 시각(KST) | 트리거 | 내용 |
|------------------|--------|------|
| **08:30** (KRX 09:00 - 30분) | 매일 | 🇰🇷 국장 프리오픈 브리핑 + 글로벌 전일 마감 요약 |
| **22:00 (DST) / 23:00 (표준시)** (US 22:30/23:30 - 30분) | 미장 영업일 | 🇺🇸 미장 프리오픈 브리핑 (Pre-market 동향 포함) |
| **05:30 (DST) / 06:30 (표준시)** | 미장 영업일 다음날 | 🇺🇸 미장 마감 요약 + 보유종목 결산 |
| **장중 (조건부)** | 실시간 | 손절/목표가 도달 즉시 알림 (§8 하이브리드 모드) |

> 💡 **기본 알림 시각의 근거**
> - **30분 전**은 ① 시장가 호가 흐름이 어느 정도 형성되어 판단에 충분하고 ② 사용자가 메시지를 읽고 매수 주문을 미리 걸어둘 여유가 확보되는 황금 구간이다.
> - 너무 일찍(2시간 전) 보내면 정보가 낡고, 너무 늦게(10분 전) 보내면 결정·주문 시간이 부족하다.

### 2.2 휴장일/이벤트 캘린더
- 한국: 한국거래소(KRX) 휴장일 — 신정, 설/추석 연휴, 노동절, 어린이날, 광복절, 개천절, 12/31 등
- 미국: NYSE/NASDAQ 휴장일 — New Year, MLK Day, Presidents' Day, Good Friday, Memorial Day, Juneteenth, Independence Day, Labor Day, Thanksgiving, Christmas
- 조기 폐장일(Half-day): 추수감사절 다음날, 크리스마스 이브 등 (13:00 ET 폐장)
- 휴장일에는 추천 대신 **시장 휴장 안내** 메시지 발송 + 통계만 갱신

### 2.3 가격 단위 / 호가 규칙
- 한국: 가격대별 호가단위 차등(예: 2,000원 미만 1원, 5,000원 미만 5원, …, 50만원 이상 1,000원)
- 미국: $1 이상 0.01 / $1 미만 0.0001 (Sub-penny Rule)
- 상하한가: 한국 ±30% (KOSPI/KOSDAQ), 미국은 상하한가 없음(개별 종목 Circuit Breaker)

### 2.4 환율 / 거래비용
- 미국 종목 추천 시 **원화 환산 가격 병기** (한국투자증권/네이버 환율 기준)
- 거래수수료·세금 가정값(시뮬레이션용)
  - 한국: 매도 시 0.18% 거래세 + 매수/매도 수수료 0.015% 가정
  - 미국: SEC fee + 환전 스프레드 0.5% 가정

### 2.5 단기 / 장기 정의
- **단기(Short-term)**: 1일 ~ 4주 보유 가정. 기술적 지표(MACD, RSI, 거래량 폭발, 뉴스 모멘텀) 중심
- **장기(Long-term)**: 3개월 ~ 12개월+ 보유. 펀더멘털(PER/PBR/ROE/매출성장률) + 산업 트렌드 + 매크로 중심

---

## 3. 기능 요구사항 (Functional Requirements)

### 3.1 핵심 유스케이스 (3-Track 스케줄)
```
[Cron 08:30 KST]                       ← 🇰🇷 국장 트랙 (개장 30분 전)
    └─ KrxPreOpenJob
        ├─ MarketCalendar.isKrxOpenToday()?
        ├─ DataCollector(KR + 전일 글로벌)
        ├─ CodexAgent.dailyBrief("KRX")        ← Codex CLI 호출 (§19)
        ├─ FeatureBuilder + RecommendationEngine(KR)
        ├─ PricePredictor → recommendation INSERT
        └─ Notifier.send(Telegram: 🇰🇷 KR-PreOpen)

[Cron 22:00 KST(DST) / 23:00(STD)]     ← 🇺🇸 미장 트랙 (개장 30분 전)
    └─ UsPreOpenJob
        ├─ MarketCalendar.isUsOpenToday()?
        ├─ DataCollector(US Pre-market, 야간 뉴스)
        ├─ CodexAgent.dailyBrief("US")
        ├─ RecommendationEngine(US)
        └─ Notifier.send(Telegram: 🇺🇸 US-PreOpen)

[Cron 05:30 KST(DST) / 06:30(STD)]     ← 🌅 글로벌 결산 트랙
    └─ UsCloseSummaryJob
        ├─ EvaluationService.closeFinishedTrades()
        ├─ DailyStatsAggregator.run()
        └─ Notifier.send(Telegram: 미장 마감 요약 + 보유 현황)

[Cron 03:00 KST 일요일]                ← 🤖 AutoResearch 트랙
    └─ AutoResearchJob.runOvernight()  ← Codex 기반 Karpathy Loop (§18 + §19)
        └─ N experiments (예산 한도 내) → Champion/Challenger 모델 채택

[Polling 5분 간격, 장중]               ← 🛡 손절 모니터링 (§8)
    └─ ExitMonitorJob (싸고 빠른 룰 기반)
        └─ 임계 도달 시 → CodexAgent.confirmExit() (조건부 1회)
```

### 3.2 기능 목록
| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F-01 | KRX 프리오픈 자동 추천 (08:30 기본) | P0 | 단기 N개, 장기 N개 종목 + 예측가/매도가 |
| F-01b | 미장 프리오픈 알림 (DST 분기, 개장 30분 전) | P0 | 22:00/23:00 KST 자동 발송 |
| F-01c | 미장 마감 글로벌 결산 알림 | P1 | 미장 마감 후 30분, 보유 현황·평가 포함 |
| F-01d | **관리자 페이지 (/admin)** | P0 | 추천 개수·백테스트 기간·예산·알림 시각·손절 모드 등 모든 핵심 파라미터 런타임 조정 |
| F-02 | 휴장일 처리 | P0 | 휴장 안내 + 다음 거래일 정보 발송 |
| F-03 | 실시간 시세/뉴스 수집 파이프라인 | P0 | 한국·미국 시세, 뉴스, 공시, 지표 |
| F-04 | 가격 예측 모듈 | P0 | 룰 기반 + ML(LightGBM/Prophet) 하이브리드 |
| F-05 | 매도 시점 알림 | P0 | 목표가 도달, 손절선 이탈, 시간만료 시 즉시 푸시 |
| F-06 | 추천 적재 및 결과 추적 | P0 | 모든 추천은 N일 후 자동 평가 |
| F-07 | 성공/실패 분석 + 피드백 | P0 | 적중률, 평균 수익률, MDD, Sharpe 등 |
| F-08 | 통계 대시보드 (React) | P0 | 누적 통계·종목별·전략별 시각화 |
| F-09 | 웹 스크래핑/RSS 뉴스 수집 | P1 | 네이버금융, 한경, MarketWatch, Reuters, SeekingAlpha |
| F-10 | 모델 자동 재학습 | P1 | 주 1회 누적 데이터로 가중치 갱신 |
| F-11 | 백테스트 모듈 | P1 | 과거 N년 가상 매매 시뮬레이션 |
| F-12 | 텔레그램 봇 양방향 | P2 | `/today`, `/stats`, `/stop AAPL` 등 명령 |
| F-13 | 관심종목 필터 | P2 | 보유종목/제외종목 사용자 등록 |
| F-14 | **AutoResearch Loop (Karpathy)** | P1 | 야간 자율 실험 → 시그널 가중치/프롬프트 자동 개선 (§18) |
| F-15 | **Daily Research Brief (Codex Agent)** | P0 | 매 트랙마다 Codex CLI가 뉴스·매크로·차트 종합한 1페이지 브리프 생성 (§19) |
| F-16 | **Codex CLI 백엔드 통합** | P0 | GPT Pro 구독 내에서 LLM 호출, 외부 API 키 의존 제거 (§19) |

### 3.3 추천 메시지 포맷 (Telegram)

**🇰🇷 국장 트랙 (08:30 KST · KRX 개장 30분 전)**
```
📊 2026-05-18 (월) 🇰🇷 KR-PreOpen 브리핑
────────────────────
🌏 Overnight: S&P +0.4% · NDX +0.8% · DXY 104.2 · WTI $78
📰 핵심 헤드라인 (LLM 요약 by AutoResearch Agent):
  • 美 4월 CPI 3.2% (예상 3.3%), 6월 금리인하 확률 ↑
  • 엔비디아 실적 서프라이즈 → 반도체 섹터 호조
────────────────────
[단기] 005930 삼성전자 ₩78,400
   ▸ 목표 ₩82,500(+5.2%) · 손절 ₩76,200(-2.8%)
   ▸ 매도예상 ~ 5/25 (5영업일)
   ▸ 시그널: RSI 38→45 반등, 외인 3일 순매수
   ▸ 신뢰도 ★★★★☆ (78%)

[장기] 373220 LG에너지솔루션 ₩395,000
   ▸ 목표 ₩460,000(+16%) · 손절 ₩360,000(-8.8%)
   ▸ 매도예상 ~ 2026 Q4
   ▸ 신뢰도 ★★★★☆ (82%)

📈 어제 추천 결과: 3/4 적중 (+2.1%)
🤖 AutoResearch v23 (champion) · last retrain 5/11
🔗 https://stock.local/today
```

**🇺🇸 미장 트랙 (개장 30분 전, DST 자동)**
```
📊 2026-05-18 (월) 🇺🇸 US-PreOpen 브리핑 — 22:00 KST (DST)
────────────────────
🌒 Pre-market: SPY +0.3% · QQQ +0.5% · VIX 13.1
📰 야간 헤드라인:
  • Walmart 실적 호조 → 소비주 강세 전환
  • 10Y Yield 4.32% → 성장주 우호적
────────────────────
[단기] NVDA $920.40 (₩1,250,000)
   ▸ 목표 $965(+4.8%) · 손절 $895(-2.7%)
   ▸ 매도예상 ~ 5/22 (4영업일)
   ▸ 신뢰도 ★★★★★ (88%)

[장기] MSFT $440 (₩596,000)
   ▸ 목표 $520(+18%) · 손절 $400(-9%)
   ▸ 매도예상 2026 Q4

⏰ 미장 개장까지 30분 · 보유 5종목 점검 권장
🔗 https://stock.local/today/us
```

**🌅 미장 마감 결산 (05:30/06:30 KST)**
```
🌅 미장 마감 요약
────────────────────
SPY +0.42% · QQQ +0.65% · DJI +0.12%
✅ NVDA: 목표 도달, +5.1% 익절 (자동 CLOSE)
🛑 TSLA: 손절선 이탈, -3.2% (자동 CLOSE)
⏳ MSFT: HOLD 유지, +0.8%
누적 7일 ROI: +3.4% (Hit 4/5)
```

---

## 4. 비기능 요구사항 (Non-Functional)

| 항목 | 요구 수준 |
|------|-----------|
| 추천 발송 정시성 | 07:00 KST ± 60초 |
| 데이터 수집 SLA | 매일 06:55 까지 전일 데이터 100% 적재 |
| 추천 신뢰도 KPI | 단기 적중률 ≥ 55%, 장기 ≥ 60% (3개월 이상 데이터 기준) |
| 시스템 가용성 | 99% (개인용 기준, 단일 노드 허용) |
| 응답 속도(대시보드) | P95 < 500ms |
| 보안 | API Key는 환경변수/Vault, 외부 노출 없음, HTTPS 강제 |
| 백업 | DB 일 1회 스냅샷(7일 보관), 주 1회 외부 스토리지 |

---

## 5. 시스템 아키텍처

### 5.1 컴포넌트 구성
```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React 19)                       │
│   Dashboard · Stats · Recommendation History · Backtest UI      │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS / REST + SSE
┌───────────────────────────────▼─────────────────────────────────┐
│                    Spring Boot 3.x (Java 21)                    │
│  ┌──────────────┬─────────────────┬─────────────────────────┐   │
│  │ API Layer    │ Scheduler       │ Notifier                │   │
│  │ (Controller) │ (Quartz/Cron)   │ (Telegram / Kakao)      │   │
│  ├──────────────┴─────────────────┴─────────────────────────┤   │
│  │ Domain: Recommendation · Prediction · Evaluation         │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ DataCollector · FeatureBuilder · RecommendationEngine    │   │
│  │ PricePredictor · BacktestEngine · FeedbackLoop           │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────┬───────────────┬───────────────┬────────────────────────┘
        │               │               │
   ┌────▼────┐    ┌─────▼─────┐    ┌────▼─────┐
   │ Postgres│    │  Redis    │    │ Python   │
   │ + Time- │    │  (cache,  │    │ ML Sidecar│
   │ scaleDB │    │  queue)   │    │ (FastAPI) │
   └─────────┘    └───────────┘    └──────────┘
        ▲                                ▲
        │                                │
┌───────┴────────────────────────────────┴────────────┐
│           External Data Sources                      │
│  • 한국투자증권 KIS Open API (실시간 시세, 차트)        │
│  • Alpha Vantage / Polygon / Yahoo Finance (US)     │
│  • DART, KRX, SEC EDGAR (공시)                       │
│  • Naver Finance, MarketWatch, Reuters, RSS         │
│  • FRED (매크로 지표), Investing.com 경제캘린더        │
└──────────────────────────────────────────────────────┘
```

### 5.2 모듈러 패키지 구조 (Spring Boot)
```
com.parkdh.stockadvisor
├── api                  # REST Controller, DTO
├── config               # SecurityConfig, SchedulerConfig, WebClientConfig
├── domain
│   ├── recommendation   # Aggregate Root: Recommendation
│   ├── prediction       # Price Prediction Entity
│   ├── evaluation       # Performance / Outcome
│   └── instrument       # Stock, Market, Calendar
├── application          # UseCase / Service (Hexagonal)
│   ├── recommend
│   ├── evaluate
│   ├── notify
│   └── autoresearch     # 🆕 Karpathy Loop 오케스트레이션 (§18)
├── infrastructure
│   ├── persistence      # JPA Repositories, QueryDSL
│   ├── datasource       # KisApiClient, YahooClient, NewsScraper
│   ├── ml               # ML Sidecar gRPC/REST Client
│   └── messaging        # TelegramBotClient, KakaoClient
├── scheduler            # Quartz Jobs (DailyJob, EvalJob, RetrainJob)
└── common               # Result, Error, Money(VO), KoreanCurrency
```

### 5.3 ML 사이드카 (Python FastAPI)
- Spring Boot는 데이터 수집·도메인·알림에 집중
- 모델 학습/추론은 **Python FastAPI 사이드카**로 분리(`/predict`, `/train`)
- 모델 후보: LightGBM(회귀), Prophet(시계열), FinBERT(뉴스 감성)
- 모델 버전은 MLflow로 관리

---

## 6. 데이터 모델 (PostgreSQL + TimescaleDB)

### 6.1 ERD 요약
```
instrument (PK ticker, market, name, sector)
   │
   ├─< price_daily (FK ticker, date)            [TimescaleDB hypertable]
   ├─< price_intraday (FK ticker, ts)            [TimescaleDB hypertable]
   ├─< fundamental (FK ticker, fiscal_quarter)
   └─< news (FK ticker, published_at, sentiment)

recommendation (PK id, ticker, term, target_price, stop_price, expected_exit_at,
                confidence, signals_json, generated_at)
   │
   └─< evaluation (PK id, recommendation_id, actual_exit_price, exit_reason,
                   pnl_pct, hit_target, drawdown_pct, evaluated_at)

prediction (PK id, ticker, horizon_days, predicted_price, model_version,
            generated_at)

backtest_run (PK id, strategy, period_from, period_to, metrics_json)

notification_log (PK id, channel, payload_hash, sent_at, status)

system_metric (PK ts, name, value)   [TimescaleDB hypertable]
```

### 6.2 핵심 테이블 DDL (요약)
```sql
CREATE TABLE recommendation (
  id              BIGSERIAL PRIMARY KEY,
  ticker          VARCHAR(20)  NOT NULL,
  market          VARCHAR(10)  NOT NULL,           -- KOSPI/KOSDAQ/NYSE/NASDAQ
  term            VARCHAR(10)  NOT NULL,           -- SHORT / LONG
  entry_price     NUMERIC(18,4) NOT NULL,
  target_price    NUMERIC(18,4) NOT NULL,
  stop_price      NUMERIC(18,4) NOT NULL,
  expected_exit_at DATE         NOT NULL,
  confidence      SMALLINT     NOT NULL,           -- 0~100
  signals         JSONB        NOT NULL,           -- 시그널 raw
  model_version   VARCHAR(40)  NOT NULL,
  generated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' -- OPEN/CLOSED/EXPIRED
);
CREATE INDEX idx_reco_generated_at ON recommendation(generated_at DESC);
CREATE INDEX idx_reco_ticker_term ON recommendation(ticker, term, status);

CREATE TABLE evaluation (
  id                 BIGSERIAL PRIMARY KEY,
  recommendation_id  BIGINT REFERENCES recommendation(id) ON DELETE CASCADE,
  actual_exit_price  NUMERIC(18,4),
  exit_reason        VARCHAR(20) NOT NULL,         -- TARGET_HIT / STOP_HIT / TIME_OUT
  pnl_pct            NUMERIC(8,4) NOT NULL,
  drawdown_pct       NUMERIC(8,4),
  hit_target         BOOLEAN NOT NULL,
  evaluated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 6.3 TimescaleDB 활용
- `price_daily`, `price_intraday`, `system_metric` 은 hypertable
- `time_bucket('1 day', ts)` 로 통계 쿼리 가속
- 90일 이후 데이터는 자동 압축(compression policy)

---

## 7. 추천 엔진 상세

### 7.1 데이터 입력 파이프라인 (매일 06:00 KST)
1. **시세 수집** — 전일 종가, OHLCV, 거래대금, 외국인/기관 순매수
2. **펀더멘털 수집** — DART/SEC EDGAR 분기 실적, PER/PBR/ROE
3. **뉴스 크롤링** — Naver Finance, 한경, MarketWatch, Reuters, SeekingAlpha (RSS 우선, robots.txt 준수)
4. **공시/이벤트** — 어닝 콜 일정, 배당락일, 자사주 매입 발표
5. **매크로 지표** — FRED(금리, CPI), DXY, WTI, US10Y, KOSPI 외인 보유 비중
6. **감성 분석** — FinBERT로 뉴스 감성 점수(-1 ~ +1) 산출

### 7.2 시그널 생성 (Feature Engineering)
| 카테고리 | 시그널 | 단기 가중 | 장기 가중 |
|----------|--------|-----------|-----------|
| 기술적 | RSI(14), MACD, Bollinger, 거래량 z-score | ★★★ | ★ |
| 추세 | 20/60/120일 이평선 정배열 | ★★ | ★★ |
| 수급 | 외인/기관 순매수 누적 | ★★★ | ★★ |
| 펀더멘털 | PER, PBR, ROE, 매출 YoY | ★ | ★★★ |
| 뉴스 감성 | FinBERT score, 뉴스 빈도 | ★★★ | ★★ |
| 매크로 | 금리 추세, 섹터 로테이션 | ★ | ★★★ |
| 이벤트 | 어닝 서프라이즈, 배당, M&A | ★★ | ★★ |

### 7.3 후보 선정 → 랭킹 → Top-N
1. 유니버스: KOSPI200 + KOSDAQ150 + S&P500 + NASDAQ100 (총 ~750종목)
2. 필터: 시총 하한(한국 3,000억/미국 1B$), 거래대금 하한, 관리종목 제외
3. 스코어링: `score = Σ(weight_i × normalized_feature_i)`
4. 단기 Top 3 / 장기 Top 3 선정 (사용자 설정 가능)

### 7.4 가격 예측 모델
- **단기 목표가**: 최근 20일 변동성(ATR) 기반 + LightGBM 회귀
- **장기 목표가**: DCF 간이 모델 + 애널리스트 컨센서스 가중평균 + Prophet 추세
- **손절가**: ATR × k (단기 k=1.5, 장기 k=2.5)
- **예상 매도일**: 단기 = 영업일 5~20일 후, 장기 = 분기 단위

### 7.5 신뢰도(Confidence) 산출
- 각 시그널의 강도와 과거 유사 패턴의 적중률을 베이지안 결합
- `confidence = sigmoid(score × historical_hit_rate × news_consistency)`

---

## 8. 매도 시점 알림 (Exit Logic) — 하이브리드 모드

### 8.1 두 가지 접근의 비교
| 방식 | 장점 | 단점 |
|------|------|------|
| **A. 가격 폴링 루프 (Cron)** | 비용 0, 결정론적, 단순 | 룰 외 상황(뉴스 쇼크 등) 인지 불가 |
| **B. AI 상시 모니터링** | 뉴스·체결강도 등 컨텍스트 반영 | 추론 비용·API 호출 ↑, 과민반응 위험 |

→ **본 시스템은 C. 하이브리드 채택**: 평시는 싸고 빠른 폴링, 임계 도달 시에만 Codex 1회 호출로 컨펌.

### 8.2 하이브리드 동작 흐름
```
[Polling 5분 간격, 장중]
  ExitMonitorJob
    ├─ 현재가/거래량 fetch
    ├─ 보유 종목 each:
    │    ├─ if price ≥ target_price            → TARGET_HIT (즉시 알림 + 평가 마감)
    │    ├─ if price ≤ stop_price              → STOP_HIT   (즉시 알림 + 평가 마감)
    │    ├─ if price within ±2% of stop_price  → ⚠ 위험구간 진입
    │    │      └─ CodexAgent.confirmExit(ticker, ctx)   ← AI 1회 호출
    │    │            • 뉴스 1시간 윈도우 감성 + 거래량 z-score 평가
    │    │            • verdict: HOLD / CUT_NOW / TIGHTEN_STOP
    │    │      └─ verdict에 따라 액션 + 텔레그램 알림
    │    └─ if today == expected_exit_at       → TIME_OUT
    └─ 결과를 evaluation 테이블에 기록
```

### 8.3 트리거 조건
| 조건 | 1차 액션(룰) | 2차 액션(Codex, 조건부) |
|------|--------------|--------------------------|
| 종가/장중가 목표가 도달 | `TARGET_HIT` | — (즉시 마감) |
| 종가/장중가 손절가 이탈 | `STOP_HIT` | — (즉시 마감) |
| 손절가 ±2% 위험구간 | ⚠ 경고 | Codex가 HOLD/CUT/TIGHTEN 컨펌 |
| 예상 매도일 경과 | `TIME_OUT` | Codex가 연장 vs 청산 권고 |
| 뉴스 감성 급변(±0.7) | 알림 | Codex가 영향도 판단 |

### 8.4 모니터링 주기 (관리자 페이지에서 조정 가능)
- **국장**: 09:00 ~ 15:30, **5분 간격 폴링** (총 80회)
- **미장**: 22:30 ~ 05:00 KST (DST), **5분 간격 폴링**
- 시간외/Pre/After 시장은 옵션(기본 OFF)
- Codex 호출은 **종목당 일 최대 N회**(기본 3회) 한도 — 관리자에서 조정

### 8.5 왜 폴링이 5분 간격인가
- KIS Open API/yfinance 모두 분 단위 호출 한도가 넉넉(분당 수십~수백)
- 5분 = 시장의 노이즈(틱) 제거 + 갑작스런 갭에도 충분히 빠른 대응
- 사용자가 1분 / 3분 / 10분 / 30분 으로 변경 가능

---

## 9. 피드백 루프 (Self-Improvement)

### 9.1 일일 사이클
```
07:00 추천 발송 → … → 매도조건 도달 시 evaluation INSERT
                                    ↓
                              22:00 일일 통계 집계
                                    ↓
                              주 1회(일요일) 모델 재학습
                                    ↓
                              가중치/하이퍼파라미터 업데이트
```

### 9.2 평가 지표
| 지표 | 정의 |
|------|------|
| Hit Rate | 목표가 도달 비율 |
| Avg Return | 평균 수익률(%) |
| Win/Loss Ratio | 평균 수익 / 평균 손실 |
| Max Drawdown | 최대 낙폭 |
| Sharpe Ratio | 위험조정 수익률 |
| Time-to-Target | 평균 목표 도달 영업일 수 |

### 9.3 자동 학습 트리거
- 신규 평가 데이터 100건 누적 시 또는 매주 일요일 03:00 KST
- 학습 결과가 직전 모델 대비 **Hit Rate +1%p 이상** 일 때만 채택(Champion/Challenger)

---

## 10. 외부 데이터 소스 (Source Matrix) — **무료 소스 전용**

> ⚠️ v1.2 기준 **유료 구독 금지**. 모든 데이터는 무료 티어로 충당하고 호출량은 캐시·배치로 절약한다.

| 소스 | 용도 | 한도/요금 | 비고 |
|------|------|-----------|------|
| **KIS Open API** | 한국 시세 (주력) | 무료(증권계좌 필요) | 초당 호출 제한, REST + WebSocket |
| **pykrx** | 한국 시세/지표 (보조) | 무료(공식 스크래핑) | 일봉/외인기관 수급 보강 |
| **yfinance** | 미국 시세 (주력) | 무료(비공식) | 분/일봉 OHLCV, 옵션 데이터 |
| **Stooq / Alpha Vantage(무료 티어)** | 미국 시세 (보조) | 무료 | 폴백용 |
| **DART Open API** | 한국 공시 | 무료 | api key 필요 |
| **SEC EDGAR** | 미국 공시 | 무료 | atom feed |
| **Naver Finance** | 뉴스 (KR) | 스크래핑 | robots.txt 준수, 본문은 요약만 |
| **MarketWatch / Reuters / SeekingAlpha RSS** | 뉴스 (US) | 무료 RSS | 본문은 요약/링크 |
| **FRED** | 매크로 | 무료 | api key |
| **한국은행 ECOS / Investing.com 캘린더** | 매크로/경제캘린더 | 무료 | RSS·스크래핑 |
| **ExchangeRate-API / Frankfurter** | 환율 | 무료 | Frankfurter는 키 불필요 |
| **OpenAI Codex CLI** | LLM 추론(브리프·AutoResearch) | **GPT Pro 구독 내** | 별도 API 키 없음 (§19) |

#### 무료 한도 관리 전략
- 모든 외부 호출은 Redis로 **TTL 캐시** (시세 5분, 뉴스 15분, 공시 1시간, 매크로 1일)
- Rate Limit 초과 시 Resilience4j Circuit Breaker → 보조 소스로 자동 폴백
- 일/월 호출 카운터를 Grafana로 모니터링, 80% 도달 시 텔레그램 경고

---

## 11. 알림 시스템

### 11.1 Telegram Bot
- **Primary 채널**. `BotFather`로 토큰 발급 → 본인 chat_id에 전송
- 명령어: `/today`, `/stats [week|month|all]`, `/holding`, `/stop <ticker>`, `/help`
- 메시지는 MarkdownV2 + 차트 이미지(matplotlib → PNG) 첨부 옵션

### 11.2 KakaoTalk
- 카카오 디벨로퍼스 "나에게 메시지 보내기" API (OAuth refresh token 갱신 필요)
- 텔레그램 실패 시 Fallback 또는 동시 발송

### 11.3 발송 신뢰성
- 발송 실패 시 5회 지수백오프 재시도, 모두 실패하면 이메일 발송(SendGrid)
- 모든 발송 기록은 `notification_log` 테이블에 저장

---

## 12. 프론트엔드 (React 19)

### 12.1 페이지 구성
| 경로 | 페이지 | 핵심 컴포넌트 |
|------|--------|---------------|
| `/` | 오늘의 추천 | RecommendationCard, ConfidenceGauge |
| `/history` | 추천 이력 | DataGrid, FilterBar |
| `/stats` | 통계 대시보드 | KpiSummary, EquityCurveChart, HitRateHeatmap |
| `/instrument/:ticker` | 종목 상세 | PriceChart, SignalTimeline, NewsFeed |
| `/backtest` | 백테스트 | StrategyForm, ResultChart |
| `/settings` | 환경설정 | 알림 채널, 관심/제외 종목 |
| `/admin` | **관리자 페이지 (§20)** | 추천 개수·백테스트 기간·예산·알림 시각·손절 모드·Codex 한도 |

### 12.2 기술 선택
- **React 19** + **TypeScript** + **Vite**
- **TanStack Query** (서버 상태) + **Zustand** (UI 상태)
- **shadcn/ui** + **TailwindCSS** (디자인 시스템)
- **Recharts** + **lightweight-charts** (TradingView 스타일 차트)
- **SSE(Server-Sent Events)** 로 매도 알림 실시간 푸시

### 12.3 통계 대시보드에 표시할 위젯
1. 누적 ROI 곡선 (Equity Curve)
2. 단기 vs 장기 Hit Rate 비교
3. 섹터별 수익률 히트맵
4. 신뢰도 구간별 적중률 (Calibration Plot)
5. 최근 30일 추천 결과 캘린더 뷰
6. 모델 버전별 성능 비교

---

## 13. 보안 / 운영

### 13.1 보안
- 모든 API Key는 `.env` + Spring `@ConfigurationProperties` + 운영 시 HashiCorp Vault 권장
- 외부 노출은 Cloudflare Tunnel + Basic Auth (개인용 기준)
- DB 백업 파일은 GPG 암호화
- Spring Security: 단일 사용자 BasicAuth + Rate Limit

### 13.2 운영(Observability)
- 로그: Logback + Loki (Grafana)
- 메트릭: Micrometer + Prometheus + Grafana
- 트레이싱: OpenTelemetry → Tempo
- 알람: 추천 발송 실패 / 데이터 수집 지연 / 모델 정확도 급락 시 Telegram 즉시 알림

### 13.3 배포
- Docker Compose 1차 (개인 NAS / 홈서버)
  - 컨테이너: `app`, `ml-sidecar`, `postgres`, `redis`, `grafana`, `prometheus`, `loki`
- 추후 Kubernetes (k3s) 마이그레이션 가능 구조

---

## 14. 개발 로드맵 (12주)

| 주차 | 마일스톤 | 산출물 |
|------|-----------|--------|
| W1 | 환경 셋업 / 도메인 설계 | 패키지 구조, ERD, Docker Compose |
| W2-3 | 데이터 수집 파이프라인 | KIS, yfinance, 뉴스 RSS, DART |
| W4 | 시그널/피처 빌더 + 백테스트 골격 | Backtest CLI |
| W5 | 추천 엔진 v0 (룰 기반) | DailyJob 동작 |
| W6 | 가격 예측 ML 사이드카 | FastAPI + LightGBM |
| W7 | 평가/피드백 루프 | evaluation 적재, 통계 집계 |
| W8 | Telegram 봇 + 알림 | 양방향 명령 |
| W9 | React 대시보드 v1 | 오늘의 추천 + 통계 |
| W10 | **AutoResearch Loop v1 (Karpathy 패턴)** | `karpathy/autoresearch` 기반 야간 자율 실험 사이드카 |
| W11 | Champion/Challenger + Daily Brief Agent | MLflow + LLM 브리핑 |
| W12 | 관측성/보안/배포 | Grafana 대시보드, 백업 자동화 |
| W13 | 베타 운영 + AutoResearch 튜닝 | KPI 측정, 회고 |

---

## 15. 리스크 / 의사결정 필요 항목

| # | 리스크 | 대응 |
|---|--------|------|
| R1 | 무료 API 호출 한도 | Redis TTL 캐시 + 다중 소스 폴백(KIS↔pykrx, yfinance↔Stooq) — 유료 전환 없음 |
| R1b | Codex CLI 응답 지연/실패 | 5회 백오프 → 로컬 룰 기반 브리프 폴백 (§19.5) |
| R2 | 뉴스 사이트 robots.txt / 저작권 | RSS 우선, 본문은 요약만 저장 |
| R3 | 과적합(Overfitting) | Walk-forward validation, Out-of-sample 검증 필수 |
| R4 | 모델 신뢰 과대평가 | Confidence Calibration Plot으로 상시 점검 |
| R5 | 한국/미국 시차 데이터 정합성 | 모든 시간 UTC 저장 + 표시 시 KST 변환 |
| R6 | 단일 노드 장애 | DB 일일 백업 + Docker healthcheck + watchdog |
| R7 | 추천만 제공 / 실매매 미연동 | v2에서 모의투자(증권사 API)부터 단계적 도입 |

### v1.2 의사결정 (모두 확정됨 ✅)
1. ✅ **추천 종목 개수** — 관리자 페이지에서 1~10 자유 설정 (기본 단기 3 / 장기 3)
2. ✅ **유료 데이터** — 사용 안 함, 무료 소스 + 캐시·폴백 전략
3. ✅ **백테스트 기간** — 관리자 페이지에서 자유 설정 (기본 **5년**)
4. ✅ **손절 알림** — **하이브리드** (5분 폴링 + 위험구간 Codex 1회 컨펌)
5. ✅ **LLM 백엔드** — OpenAI Codex CLI (GPT Pro 구독)
6. ✅ **호출 한도** — 관리자 페이지에서 일 호출 수·예산 자유 설정 (기본 200/일, 외부 과금 $0)

---

## 16. 부록

### 16.1 환경 변수 예시
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://postgres:5432/stock
DB_USER=stock
DB_PASSWORD=*****
REDIS_URL=redis://redis:6379
# 무료 데이터 소스
KIS_APP_KEY=*****
KIS_APP_SECRET=*****
ALPHA_VANTAGE_KEY=*****          # 무료 티어
FRED_API_KEY=*****
DART_API_KEY=*****
# 알림
TELEGRAM_BOT_TOKEN=*****
TELEGRAM_CHAT_ID=*****
KAKAO_REST_API_KEY=*****
KAKAO_REFRESH_TOKEN=*****
# LLM 백엔드 (별도 API 키 없음, Codex CLI 실행 환경만 마련)
CODEX_BIN=/usr/local/bin/codex
CODEX_PROFILE=stock-advisor
CODEX_DAILY_BUDGET_USD=0          # GPT Pro 구독 사용, 외부 과금 0
CODEX_DAILY_CALL_LIMIT=200
TIMEZONE=Asia/Seoul
```

### 16.2 주요 라이브러리
- Backend: Spring Boot 3.3, Spring Data JPA, QueryDSL, Quartz, WebClient, Resilience4j, MapStruct, jsoup(크롤링), pykrx-api wrapper
- ML: Python 3.12, FastAPI, LightGBM, Prophet, transformers(FinBERT), MLflow, pandas, numpy
- Frontend: React 19, TypeScript 5, Vite 5, TanStack Query 5, Zustand, shadcn/ui, Tailwind 4, Recharts, lightweight-charts
- Infra: Docker Compose, PostgreSQL 16 + TimescaleDB 2.x, Redis 7, Grafana, Prometheus, Loki, Cloudflare Tunnel

### 16.3 폴더 구조 (Mono-repo 권장)
```
stock-advisor/
├── apps/
│   ├── backend/          # Spring Boot
│   ├── ml-sidecar/       # Python FastAPI
│   └── web/              # React
├── packages/
│   └── shared-types/     # OpenAPI -> TS 타입 자동 생성
├── infra/
│   ├── docker/
│   └── grafana/
└── docs/
    └── STOCK_ADVISOR_SPEC.md
```

---

## 17. 다음 단계 (Action Items)

1. ✅ v1.2 명세서 확정 (모든 의사결정 반영 완료)
2. ⬜ KIS Open API · DART · Telegram Bot 토큰 발급
3. ⬜ **OpenAI Codex CLI 설치 + GPT Pro 로그인 + `stock-advisor` profile 생성**
4. ⬜ Docker Compose 베이스라인 작성 → DB/Redis/앱/Codex runner 컨테이너 부팅
5. ⬜ W1 마일스톤 시작 (도메인 모델 + ERD 코드화 + `app_setting`/`audit_log` 우선 구현)
6. ⬜ **관리자 페이지(/admin)** 부터 만들어서 모든 파라미터를 초기에 UI로 조정 가능하게
7. ⬜ 첫 번째 룰 기반 추천(F-01) MVP를 2주 안에 동작시키기
8. ⬜ `karpathy/autoresearch` 레포 clone → strategy.yaml 골격 작성 → W10에 Codex 기반 야간 루프 PoC

---

## 18. 🤖 AutoResearch Loop — Karpathy 패턴 적용

> 참고 레포: [`karpathy/autoresearch`](https://github.com/karpathy/autoresearch) (2026-03 공개, 21k★)
> "Single file · Single metric · Time-boxed experiment · Keep-or-Discard"

### 18.1 왜 도입하는가
기존 피드백 루프(섹션 9)는 사람이 정한 가중치 공간 안에서만 학습된다. Karpathy 패턴은 **에이전트가 직접 코드/설정을 변형**하고, 객관 지표가 좋아지면 채택, 나빠지면 폐기하는 식이라서 우리가 미리 정한 탐색공간을 벗어난 개선까지 자율적으로 찾아낸다.

본 시스템에서는 두 가지 레이어에 동시에 적용한다.
1. **Strategy AutoResearch** — 야간 시그널 가중치/룰 자동 실험 (Champion/Challenger 채택)
2. **Daily Brief AutoResearch** — 매 트랙(국장/미장) 발송 직전, LLM 에이전트가 수집된 데이터를 다단계 추론으로 종합해 1페이지 브리프 생성

### 18.2 Karpathy Loop의 3대 원칙 (본 프로젝트 매핑)
| 원칙 | Karpathy 원본 | Stock Advisor 매핑 |
|------|--------------|---------------------|
| ① 단일 수정 대상 (Single File) | `train.py` | `strategy.yaml` (시그널 가중치, 임계값, 룰) |
| ② 단일 객관 지표 (Single Metric) | `val_bpb` | `walk_forward_sharpe` 또는 `hit_rate_60d` |
| ③ 시간 박스 (Time-boxed) | 5분 트레이닝 | 5분 백테스트(최근 6개월 walk-forward) |

### 18.3 Strategy AutoResearch — 야간 자율 실험
```
[Sun 03:00 KST] AutoResearchJob.runOvernight()
    program.md (목표/제약 명세)
        ↓
    ┌──── LOOP (target_iterations=80, wall_clock_max=6h) ────┐
    │ 1. Agent.proposeChange(strategy.yaml, history)         │
    │ 2. Git commit (branch: ar/<run_id>/<iter>)             │
    │ 3. BacktestEngine.run(timebox=5min, period=180d)       │
    │ 4. metric = sharpe + 0.3*hit_rate − 0.5*max_dd         │
    │ 5. if metric > champion: promote ; else: revert        │
    │ 6. autoresearch_run 테이블에 모든 시도 기록            │
    └────────────────────────────────────────────────────────┘
    아침 보고: 텔레그램으로 "지난밤 84실험, 신규 챔피언 v24 채택" 알림
```

#### 에이전트가 만질 수 있는 것 (Action Space)
- `strategy.yaml` 시그널 가중치/임계값
- 신규 시그널 함수 등록(`signals/*.py` 추가)
- 필터 룰(시총·거래대금·섹터 제외)
- 포지션 사이징(Kelly fraction, ATR 멀티플라이어)

#### 에이전트가 만질 수 없는 것 (Guardrails)
- 휴장일/장 시간 로직, 알림 라우팅, DB 스키마, 보안 코드
- 단일 종목 비중 > 25% / 단일 섹터 > 40% 제한
- 신규 실험은 항상 **paper trade** 결과만 반영, 실거래에는 영향 없음

### 18.4 Daily Brief AutoResearch — 매일 LLM 브리핑
국장/미장 발송 직전에 동작하는 짧은 루프. Karpathy 패턴의 "탐험→측정→채택" 사상을 LLM 추론에 적용.

```
brief_prompt.md  (시장별 시스템 프롬프트)
sources/         (RSS, 공시, 매크로, 차트 캡션)
        ↓
LLM Agent (Claude / GPT) ──→ draft_v1
        ↓
Self-Critic ──→ "사실 오류·시각화 누락 점검" ──→ draft_v2
        ↓
Coverage Score = Σ(hit_keyword) / Σ(should_cover)
        ↓
score ≥ 0.85 면 채택, 미만이면 추가 fetch + 재생성 (최대 3회)
```

- **단일 파일**: `brief.md` (전체 브리프)
- **단일 지표**: Coverage Score (필수 키워드 누락률) + Hallucination Penalty(주가/실적 수치 ±1% 검증)
- **시간 박스**: 90초/회 × 최대 3회 = 4.5분 안에 결정

### 18.5 새 도메인 / 테이블
```sql
-- 야간 실험 1건 = 1 row
CREATE TABLE autoresearch_run (
  id            BIGSERIAL PRIMARY KEY,
  job_run_id    UUID NOT NULL,
  iter_no       INT  NOT NULL,
  parent_sha    VARCHAR(64),      -- 베이스 챔피언 git sha
  proposal_sha  VARCHAR(64),      -- 실험 brunch git sha
  diff_summary  TEXT,             -- agent가 적은 한 줄 설명
  metric_name   VARCHAR(40),
  metric_value  NUMERIC(10,4),
  champion_metric NUMERIC(10,4),
  decision      VARCHAR(10),      -- KEEP / DISCARD / ERROR
  duration_ms   INT,
  started_at    TIMESTAMPTZ,
  ended_at      TIMESTAMPTZ
);

-- 채택된 챔피언 모델/전략 버전
CREATE TABLE strategy_version (
  id            BIGSERIAL PRIMARY KEY,
  semver        VARCHAR(20) NOT NULL,    -- v24.0.0
  git_sha       VARCHAR(64) NOT NULL,
  metric_value  NUMERIC(10,4) NOT NULL,
  promoted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_champion   BOOLEAN NOT NULL DEFAULT false
);

-- 브리프 1건 = 1 row (모든 draft 보관)
CREATE TABLE daily_brief (
  id            BIGSERIAL PRIMARY KEY,
  market_track  VARCHAR(10) NOT NULL,    -- KRX / US / US_CLOSE
  brief_md      TEXT NOT NULL,
  draft_no      SMALLINT NOT NULL,
  coverage      NUMERIC(4,3),
  halluc_flags  INT,
  llm_model     VARCHAR(40),
  generated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 18.6 안전장치 (Karpathy도 강조한 부분)
1. **Reproducibility**: 모든 실험 git commit + 시드 고정
2. **Cost cap**: LLM 토큰 일일 한도(예: $5/day), 초과 시 자동 중단
3. **Out-of-sample 보호**: 채택 기준에 직전 30일 데이터가 절대 들어가지 않음(데이터 누수 방지)
4. **Champion 롤백**: 신규 챔피언이 다음 7일 실거래 성과로 또 검증, 실패 시 직전 챔피언 자동 복귀
5. **Kill switch**: `autoresearch.enabled = false` 한 줄로 전체 비활성화

### 18.7 신규 패키지/디렉터리
```
apps/
├── autoresearch/                  # 🆕 Python 사이드카 (LangGraph)
│   ├── agent.py                   # 제안→평가 루프 메인
│   ├── tools/
│   │   ├── backtest.py
│   │   ├── git_io.py
│   │   └── strategy_yaml.py
│   ├── prompts/
│   │   ├── program.md             # 야간 실험 목표
│   │   ├── brief_kr.md
│   │   └── brief_us.md
│   └── strategies/
│       ├── strategy.yaml          # 현재 챔피언
│       └── archive/               # 과거 챔피언
```

### 18.8 운영 지표 (Grafana 대시보드 추가)
- 야간 실험 횟수 / 채택률
- 챔피언 교체 빈도
- Codex 호출 수 · 일 한도 대비 사용률
- 브리프 Coverage Score 추이

---

## 19. ⚙️ OpenAI Codex CLI 통합 (LLM 백엔드)

> **결정 배경**: 사용자가 GPT Pro 구독자이며 외부 API 과금을 원치 않음 → Codex CLI를 시스템의 단일 LLM 게이트웨이로 채택.

### 19.1 통합 구조
```
Spring Boot ──(stdio/pty)──> codex CLI ──> ChatGPT(GPT-5)
       │                          │
       │                          ├─ Reasoning · Tool use (file edit, shell)
       │                          └─ Cached context per profile
       │
       └─ CodexClient (Java) — 단일 진입점
           ├─ codex.chat(prompt, ctx)            → 텍스트 응답
           ├─ codex.runTask(taskMd, repoPath)    → 파일 수정 + 결과 반환
           └─ codex.exec(prompt, allowedTools)   → 도구 호출 가능 모드
```

### 19.2 사용처
| 호출 위치 | 빈도 | 목적 |
|-----------|------|------|
| **DailyBriefService** | 3회/일 (KR/US/Close) | 뉴스·매크로 데이터를 1페이지 브리프로 종합 |
| **ExitMonitor** (조건부) | 평균 0~5회/일 | 위험구간 진입 시 HOLD/CUT/TIGHTEN 컨펌 |
| **AutoResearchJob** (야간) | 일요일 1회 × 수십 iteration | strategy.yaml 변형 제안·평가 (§18) |
| **RecommendationEngine** (옵션) | 0~2회/일 | 룰 기반 후보에 대한 LLM 2차 검증(켜고 끄기 가능) |

### 19.3 호출 규약 (Java 측 인터페이스)
```java
public interface CodexClient {
    /** 단순 텍스트 추론. 캐시된 시스템 프롬프트 사용. */
    String chat(String userPrompt, Map<String, Object> ctx);

    /** 디렉터리 컨텍스트에서 파일 수정 + diff 반환. */
    CodexTaskResult runTask(Path repoRoot, String taskInstruction);

    /** Tool use 모드: shell·http·fs 허용 범위 화이트리스트. */
    String exec(String prompt, Set<CodexTool> allowedTools, Duration timeout);
}
```

### 19.4 호출 한도 / 비용 가드
- 모든 호출은 `CodexBudgetGuard` Interceptor 통과
- 일 한도 초과 시: ① 비핵심 호출(2차 검증) 스킵 → ② 브리프 캐시 폴백 → ③ 텔레그램 경고
- 일/주/월 호출 카운터 + 응답 시간 메트릭 → Grafana
- **외부 과금 0원**: Codex CLI는 GPT Pro 세션을 재사용

### 19.5 폴백 / 장애 대응
- Codex CLI 응답 실패: 5회 지수백오프 → 로컬 룰 기반 브리프(템플릿)로 폴백 → 텔레그램에 "AI 브리프 생략" 표시
- 응답 hallucination(주가/실적 수치 ±1% 검증 실패): 1회 재시도 → 그래도 실패 시 해당 라인 제거

### 19.6 보안
- `codex` 실행은 별도 Linux user (`codex-runner`)로 sandbox
- 허용된 파일 경로 화이트리스트(`apps/autoresearch/`, `prompts/`)만 RW
- 시크릿 환경변수는 마운트 제외
- 모든 prompt/response를 `codex_call` 테이블에 해시·길이·tool 사용 여부와 함께 감사 로그로 적재

### 19.7 신규 테이블
```sql
CREATE TABLE codex_call (
  id            BIGSERIAL PRIMARY KEY,
  caller        VARCHAR(40) NOT NULL,    -- BRIEF_KR / BRIEF_US / EXIT_CONFIRM / AR_PROPOSE / RECO_VERIFY
  prompt_hash   CHAR(64)   NOT NULL,
  prompt_len    INT        NOT NULL,
  response_len  INT,
  tools_used    JSONB,
  duration_ms   INT,
  succeeded     BOOLEAN    NOT NULL,
  error_msg     TEXT,
  called_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_codex_called_at ON codex_call(called_at DESC);
CREATE INDEX idx_codex_caller    ON codex_call(caller, called_at DESC);
```

---

## 20. 🛠 관리자 페이지 (/admin) 명세

> 단일 사용자 환경이지만 BasicAuth + 별도 비밀번호로 보호. 모든 변경은 즉시 반영(다음 Cron부터 적용) + `audit_log`에 기록.

### 20.1 화면 구성 (탭형)

#### Tab 1 — 추천 설정
| 항목 | 기본값 | 범위 | 설명 |
|------|--------|------|------|
| 단기 추천 개수 | 3 | 1~10 | 매 트랙 단기 후보 N개 |
| 장기 추천 개수 | 3 | 1~10 | 매 트랙 장기 후보 N개 |
| 시장 활성화 | KR✓ US✓ | — | 트랙별 ON/OFF |
| 시총 하한 (KR) | 3,000억 ₩ | 자유 | 유니버스 필터 |
| 시총 하한 (US) | $1B | 자유 | |
| 거래대금 하한 (KR/US) | 100억 ₩ / $10M | 자유 | |
| 제외 섹터 | 없음 | 멀티 | 예: 바이오·중국주 |
| 보유/제외 종목 | — | 자유 | 화이트/블랙리스트 |

#### Tab 2 — 알림 시각
| 항목 | 기본값 | 설명 |
|------|--------|------|
| KRX 프리오픈 알림 | 개장 30분 전 (08:30) | 분 단위 조정 |
| US 프리오픈 알림 | 개장 30분 전 (22:00 DST / 23:00 STD) | 분 단위 조정 |
| US 마감 결산 | 마감 30분 후 | 분 단위 조정 |
| 휴장일 알림 발송 여부 | ON | |
| 채널 우선순위 | Telegram → Kakao | Drag 정렬 |

#### Tab 3 — 손절/매도 모니터링
| 항목 | 기본값 | 설명 |
|------|--------|------|
| 폴링 주기 | 5분 | 1·3·5·10·30분 선택 |
| 위험 구간 폭 | ±2% | 손절선 근방 AI 컨펌 임계 |
| 종목당 일 Codex 컨펌 한도 | 3회 | |
| 장중 즉시 손절 알림 | ON | OFF 시 종가 기준만 |
| 시간외 모니터링 | OFF | Pre/After-market |

#### Tab 4 — 백테스트
| 항목 | 기본값 | 설명 |
|------|--------|------|
| 기본 기간 | **5년** | 1·3·5·10년 또는 사용자 지정 |
| Walk-forward 윈도우 | 180일 | |
| 슬리피지 가정 | 0.05% | |
| 거래비용(KR/US) | 0.18%·세금 / 0.5%·환전 | |

> **5년 기본 근거**: 2020 코로나 충격 + 2022 베어마켓 + 2023-25 회복기를 모두 포함해 강세장·약세장 모두 통과한 전략만 통과시킬 수 있어 가장 균형 잡힌 기본값.

#### Tab 5 — AutoResearch / Codex
| 항목 | 기본값 | 설명 |
|------|--------|------|
| AutoResearch 활성화 | ON | Kill switch |
| 야간 실험 목표 횟수 | 80 | wall clock 6h 한도 |
| Codex 일 호출 한도 | 200 | 초과 시 비핵심 호출 스킵 |
| Codex 일 예산($) | $0 (GPT Pro) | 외부 과금 시에만 의미 |
| Champion 롤백 검증 기간 | 7일 | 신규 챔피언 paper-trade 기간 |
| 사용 모델 | gpt-5 (Codex 기본) | 사용 가능한 Codex profile |

#### Tab 6 — 운영
- DB 백업 즉시 실행 / 스케줄
- 호출 카운터(데이터 소스별, Codex)
- 최근 audit_log 50건
- 모든 설정 JSON export / import

### 20.2 설정 저장 방식
```sql
CREATE TABLE app_setting (
  key         VARCHAR(60) PRIMARY KEY,
  value_json  JSONB       NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by  VARCHAR(40) NOT NULL DEFAULT 'admin'
);

CREATE TABLE audit_log (
  id          BIGSERIAL PRIMARY KEY,
  actor       VARCHAR(40),
  action      VARCHAR(60) NOT NULL,
  before_json JSONB,
  after_json  JSONB,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- Spring `@RefreshScope` 패턴 + Redis Pub/Sub → 변경 시 모든 노드 즉시 반영
- Quartz 스케줄도 cron 변경 시 핫리로드

---

> **v1.2 상태**: 모든 핵심 결정 확정 ✅ — W1 마일스톤(도메인 모델 + 관리자 페이지 골격 + Codex 통합 PoC) 착수 가능.
> 다음 v1.3에서는 (a) Codex CLI 실제 호출 인터페이스 fixture, (b) AutoResearch program.md 초안, (c) 관리자 페이지 와이어프레임을 포함합니다.
