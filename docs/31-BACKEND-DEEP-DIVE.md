# Stock Advisor 백엔드 완벽 가이드

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 31(상세 아키텍처) · 상태 🟢 현행(2026-06-02 갱신) · 요약은 [30-BACKEND-OVERVIEW.md](30-BACKEND-OVERVIEW.md)
>
> 이 문서는 `apps/backend` (Spring Boot 3.3 / Java 21 / MSSQL) 전체 코드를 처음 보는 사람이 **주식 도메인 지식까지** 함께 이해할 수 있도록 정리한 종합 문서다.
>
> "내가 짠 게 아니라 AI가 만든 코드라 뭘 만든지 모르겠다" 라는 출발점을 가정하고, 코드를 한 줄도 안 봐도 흐름과 근거를 알 수 있도록 작성했다.
>
> **2026-05-22 대규모 개정**: P0/P1/P2 결함 19개 중 16개 적용 완료. 자세한 결함/수정 이력은 별도 문서 `41-DEFECTS-AND-FIXES.md` 참고.
>
> **2026-06-02 재정리 (현 시점)**: 실제 코드 기준으로 패키지 수·테스트 수·Flyway V10·Properties 클래스·API 경로 누락분 보정. 본 문서는 현재 코드 기준으로 갱신됨.
>
> 주요 추가/변경:
> - AutoResearch 본체 가동 (`AutoResearchJob` 스케줄러 + 가중치 변형/검증/롤백 사이클)
> - 추천 confidence 가짜 해시 → 실데이터 hit rate 기반 (`RecommendationConfidenceService`)
> - ExitMonitor KR+US 통합 (StooqQuoteClient), ExitConfirm 전면 제거 (순수 룰 기반)
> - 거래비용/슬리피지 가격 반영 (entry/target/stop 모두)
> - 시장 레짐(MA200) 필터 + 섹터 분산 캡
> - ExitMonitor 자동 평가/청산 (target/stop/expiry 도달 시 `evaluation` 자동 생성)
> - BCrypt 패스워드 + dev API 기본 보호
> - DB V7 마이그레이션 (`market_universe.delisted_at` 추가, 생존편향 보정 준비)
> - `infrastructure/ops/` 패키지 추가 (`ExternalHealthChecker`)
> - `SentimentAnalysisProperties` (감성 sidecar 토글)
> - 테스트 21개로 확장 (application/config/global/infrastructure/scheduler 다층)

---

## 목차

1. [한눈에 보는 시스템](#1-한눈에-보는-시스템)
   - [1.1 쉬운 설명 — 가중치 변형 / 백테스트 / 챔피언 승격이란?](#11-쉬운-설명--가중치-변형--백테스트--챔피언-승격이란)
2. [주식 기초 지식 — 이 백엔드를 이해하기 위한 최소한](#2-주식-기초-지식--이-백엔드를-이해하기-위한-최소한)
3. [전체 데이터 흐름 (Flow)](#3-전체-데이터-흐름-flow)
4. [기술 스택 & 프로젝트 구조](#4-기술-스택--프로젝트-구조)
5. [데이터베이스 스키마](#5-데이터베이스-스키마)
6. [외부 연동 — 데이터를 어디서 어떻게 가져오나](#6-외부-연동--데이터를-어디서-어떻게-가져오나)
7. [추천 엔진 — 어떤 근거로 종목을 고르나 (핵심)](#7-추천-엔진--어떤-근거로-종목을-고르나-핵심)
8. [가격 산출(PricePredictor) — 진입가/목표가/손절가 계산](#8-가격-산출pricepredictor--진입가목표가손절가-계산)
9. [손절 모니터링 — ExitMonitorJob (순수 룰 기반)](#9-손절-모니터링--exitmonitorjob-순수-룰-기반)
10. [스케줄러 11종 — 자동 동작 시각표](#10-스케줄러-11종--자동-동작-시각표)
11. [백테스트 — 과거 데이터로 전략 검증](#11-백테스트--과거-데이터로-전략-검증)
12. [통계 / 데일리 브리프 / AutoResearch](#12-통계--데일리-브리프--autoresearch)
13. [API 카탈로그](#13-api-카탈로그)
14. [보안 / 운영 / Codex 한도](#14-보안--운영--codex-한도)
15. [개발 모드 vs 실운영 모드](#15-개발-모드-vs-실운영-모드)
16. [로컬 실행 방법](#16-로컬-실행-방법)

---

## 1. 한눈에 보는 시스템

이 백엔드는 **"AI(Codex)와 룰 기반 점수 시스템을 결합한 주식 추천/감시 서비스 + 야간 자동 전략 진화 사이클"** 의 백엔드다.
하루의 일과는 대략 이렇게 흘러간다.

```
[02:20 KST 화-토] AutoResearch 자동 실행      ← 가중치 변형 → 백테스트 → 챔피언 승격/롤백
[07:00 KST] 매크로 지표 수집 (FRED)          ← 환율, 금리, 원자재
[08:15]     KRX 뉴스/공시/펀더멘털 수집       ← Google News RSS, DART, SEC
[08:30]     KRX 프리오픈                      ← 한국 심볼 동기화 → 최신 일봉 증분만 확인 → 추천 생성 → 텔레그램 발송
[09:00~15:30] 평일 폴링 ExitMonitor (KR)     ← OPEN KR 추천 감시: target/stop/expiry 자동 평가 (KIS 가격)
[18:10]     KRX 일봉 백필                     ← 비어 있거나 오래된 후보군 히스토리 장후 보강
[22:00~05:00] 평일 폴링 ExitMonitor (US)     ← OPEN US 추천 감시: target/stop/expiry 자동 평가 (Stooq 가격)
[21:40]     US 뉴스/공시/펀더멘털 수집
[22:00/23:00] US 프리오픈 (DST에 따라)        ← 미국 심볼 동기화 → 최신 일봉 증분만 확인 → 추천 → 텔레그램
[05:30/06:30] US Close 요약 (다음날 새벽)     ← 어제 미장 마감 결과 요약
[07:20 화~토] US 일봉 백필                    ← 비어 있거나 오래된 후보군 히스토리 장후 보강
```

각 시각은 DB의 `app_setting` 테이블 값으로 변경 가능하다(스케줄러는 cron으로 매분 깨어나 설정 시각과 비교).
ExitMonitor 폴링 주기는 `exit.polling.intervalMinutes` (1/3/5/10/30) 로 조절.

핵심 컴포넌트:

| 영역 | 주요 클래스 | 역할 |
|---|---|---|
| **시장 후보군 동기화** | `MarketUniverseService`, `KrxSymbolClient`, `NasdaqTraderSymbolClient`, `StooqQuoteClient` | KRX KIND/NASDAQ Trader 공개 목록에서 상장 종목 목록을 받아 `market_universe` 테이블에 저장. `delisted_at` 컬럼으로 상폐 이력 보관 |
| **가격 데이터 수집** | `MarketDataSyncService`, `KisApiClient`, `YahooFinanceClient`, `StooqQuoteClient` | KIS(KR 일봉/현재가), Yahoo(US 일봉), Stooq(US quote/지수)에서 가격을 받아 `price_daily`, `price_intraday`에 저장. 일봉은 DB 최신 거래일을 확인해 필요한 구간만 증분 수집 |
| **컨텍스트 데이터 수집** | `MarketDataCollectionService`, `RssNewsClient`, `DisclosureClient`, `FredMacroClient`, `SecFundamentalClient`, `MarketSignalScorer` | 뉴스, 공시, 매크로, 펀더멘털을 외부에서 받아 4종 테이블에 저장. 부정문 처리 포함 감성/중요도 점수 부여 |
| **추천 후보 점수화** | `UniverseFeatureBuilder` | 위 데이터를 합쳐 종목별 **0~100점 종합 feature 점수**. 가중치는 `app_setting.recommendation.scoring.weights`에서 동적으로 로딩 |
| **추천 후보 선별** | `RecommendationEngine` | 점수/시총/거래대금/섹터/종목 필터, MA200 시장 레짐 필터, 섹터 분산 캡, 상위 N개 선택 |
| **추천 가격 계산** | `PricePredictor` | 진입가/목표가/손절가/예상 청산일 산출. 거래비용/슬리피지 반영. 데이터 없으면 422 throw |
| **추천 신뢰도** | `RecommendationConfidenceService` | 과거 평가 데이터의 score band별 hit rate → confidence (샘플<30이면 50) |
| **추천 저장** | `DevRecommendationGenerateService`, `RecommendationService` | 단기/장기 추천을 DB에 OPEN 상태로 저장. modelVersion은 챔피언 전략의 semver |
| **장중 감시 + 자동 청산** | `ExitMonitorJob` | 폴링: target/stop/expiry 도달 시 `evaluation` 생성 + 상태 전환. KR=KIS, US=Stooq. 순수 룰 기반, Codex 없음 |
| **알림** | `TelegramClient`, `NotificationLogService` | 텔레그램 발송. SHA-256 dedupe + priceBucket 1% 단위 라운딩 |
| **백테스트** | `BacktestRunService` | MA20 단순 룰 또는 `recommendation-engine-v1` (추천 엔진 선별 종목)로 시뮬레이션. 거래비용 반영 |
| **AutoResearch 사이클** | `AutoresearchService`, `AutoResearchJob` | 야간에 가중치 변형 + 백테스트 → 챔피언 승격, 7일 라이브 검증 후 자동 롤백 |
| **통계** | `StatsService` | 평가 결과를 모아 hit rate, 평균 PnL, 일별 누적 PnL 등을 계산 |
| **데일리 브리프** | `DailyBriefService` | 수집한 모든 데이터를 모아 Codex에게 마크다운 브리프 작성 요청 |
| **운영 관리** | `AdminSettingService`, `ExternalHealthService` | 설정 CRUD, 감사 로그(AutoResearch 변경 포함), 외부 API 설정 상태 점검 + Codex 일 한도/예산 |

### 1.1 쉬운 설명 — 가중치 변형 / 백테스트 / 챔피언 승격이란?

#### 큰 그림

이 시스템은 **"매일 주식 종목을 골라서 텔레그램으로 알려주는 로봇"** 이다. 그냥 알려주는 게 아니라, **자기가 얼마나 잘 골랐는지 스스로 평가하고 매주 나아진다.**

#### 추천 점수 공식

종목마다 0~100점을 매긴다.

```
삼성전자 점수 =
    유동성점수 × 20%    ← 거래가 활발한가?
  + 가격점수   × 10%    ← 가격대가 적당한가?
  + 기술점수   × 30%    ← RSI/이동평균/거래량이 좋은가?
  + 뉴스점수   × 15%    ← 최근 뉴스가 긍정적인가?
  + 펀더점수   × 10%    ← 실적 데이터가 탄탄한가?
  + 품질점수   × 15%    ← 데이터가 충분히 모였는가?
```

점수 높은 순으로 상위 N개 선택 → 추천.

#### 가중치 변형이란?

각 항목의 비율(%)이 **"가중치"**. 가중치 변형 = **이 비율을 살짝 바꿔보는 것**.

```
현재:  기술 30%, 유동성 20%, 뉴스 15%, 품질 15% ...

실험1: 기술을 33%로 올리고 유동성을 17%로 낮춰봄
실험2: 뉴스를 16.5%로 올리고 펀더를 8.5%로 낮춰봄
실험3: 품질을 13.5%로 낮추고 기술을 16.5%로 올려봄
        (매주 8~80번 반복)
```

#### 백테스트란?

변형된 가중치로 **"과거로 돌아가서 실험해보는 것"**.

```
"만약 작년에 이 가중치로 종목을 골랐다면 어떤 결과가 나왔을까?"
  → 과거 가격 데이터로 가상 매매 시뮬레이션
  → 수익률 계산 (예: 평균 +2.3%)
실제 돈 쓰지 않고 과거 데이터로 결과를 예측.
```

#### 챔피언 승격이란?

백테스트 결과 비교:

```
현재 쓰는 가중치 → 평균 수익률 +1.8%
실험 가중치 A    → 평균 수익률 +2.3%  ← 더 좋음! → 챔피언 승격
실험 가중치 B    → 평균 수익률 +0.9%  ← 버림
실험 가중치 C    → 평균 수익률 +2.1%  ← A보다 낮음 → 버림
```

가중치 A가 **챔피언**이 되면 다음날부터 이 비율로 추천 생성. 1주일 뒤 실제 성과도 검증:

```
챔피언 A로 추천한 실제 결과가 백테스트의 50% 미만
  → 자동으로 이전 챔피언으로 롤백 (되돌리기)
```

#### 전체 닫힌 사이클

```
가중치로 종목 선택
  → 텔레그램 발송
  → 목표가/손절가 도달 시 자동 평가 기록
  → 매주 새벽: 기록 보고 가중치 실험 (백테스트)
  → 더 좋은 가중치 발견 → 챔피언 교체
  → 다음 추천에 반영 → 반복
사람이 개입하지 않아도 매주 스스로 조금씩 나아지는 구조.
```

---

## 2. 주식 기초 지식 — 이 백엔드를 이해하기 위한 최소한

코드를 보기 전에 알아야 할 주식 용어 정리. 모르고 보면 점수 산식이 뜬구름이다.

### 2.1 시장(Market)

| 코드 | 시장 | 국가 | 통화 |
|---|---|---|---|
| `KOSPI` | 코스피(유가증권시장) | 한국 | KRW |
| `KOSDAQ` | 코스닥 | 한국 | KRW |
| `NASDAQ` | 나스닥 | 미국 | USD |
| `NYSE` | 뉴욕증권거래소 | 미국 | USD |

거래 시간:
- 한국: **09:00 ~ 15:30 KST** (점심 휴장 없음)
- 미국: **09:30 ~ 16:00 ET** → 서울 기준 **22:30 ~ 05:00** (서머타임 DST 시) / **23:30 ~ 06:00** (표준시)

→ 그래서 KRX 프리오픈은 08:30, US 프리오픈은 22:00/23:00, US Close 요약은 05:30/06:30에 돈다.

### 2.2 종목(Ticker, Instrument)

- **티커(ticker)**: 종목 코드.
  - 한국: 6자리 숫자. 예) 삼성전자 = `005930`, SK하이닉스 = `000660`
  - 미국: 알파벳. 예) Apple = `AAPL`, NVIDIA = `NVDA`
- **시가총액(market cap)**: `현재가 × 발행주식수`. 회사 전체의 시장 평가액.
- **섹터(sector)**: 업종. 반도체, 인터넷, 헬스케어, 2차전지 등.

### 2.3 가격 데이터 종류

| 종류 | 설명 | 테이블 |
|---|---|---|
| **일봉(Daily)** | 하루 단위 OHLCV. 시가(Open) / 고가(High) / 저가(Low) / 종가(Close) / 거래량(Volume) | `price_daily` |
| **장중(Intraday)** | 장중 특정 시각의 현재가 스냅샷 | `price_intraday` |
| **거래대금(Turnover)** | `가격 × 거래량` = 그날 돈으로 얼마가 거래됐는지 | `price_daily.turnover` |

### 2.4 추천에서 자주 쓰는 지표

- **이동평균선 (Moving Average, MA)**: 최근 N일 종가의 평균.
  - MA5 (5일선)는 단기 추세, MA20 (20일선)은 중기 추세.
  - 종가 > MA5 > MA20 정렬 = **정배열**, 상승 추세로 본다.
- **RSI (Relative Strength Index)**: 0~100. 최근 14일의 상승/하락 폭 비율.
  - 70 이상 = 과열(매도 신호 가능), 30 이하 = 침체(매수 신호 가능).
  - 이 백엔드는 45~65를 "안정적 상승 구간"으로 가장 좋게 본다.
- **거래량 Z-score**: `(오늘 거래량 - 20일 평균) / 표준편차`.
  - 1.5 이상 = 평소보다 거래가 강하게 늘어남.
- **변동성(Volatility)**: 최근 종가의 평균 절대 수익률. 0.02 = 2% 변동.
  - 이 백엔드는 1~8% 범위로 클립한다.

### 2.5 추천의 3대 가격

- **진입가(entry price)**: 매수 권장 가격. 최근 종가 사용.
- **목표가(target price)**: 익절 기준. `entry × (1 + 변동성 × 배수)`.
- **손절가(stop price)**: 손절 기준. `entry × (1 - 변동성 × 배수)`.

이 백엔드는 단기(SHORT)/장기(LONG)에 따라 배수를 다르게 잡는다 (8장 참조).

### 2.6 추천 상태 머신

```
OPEN  ─ 목표 도달 ─→ CLOSED (TARGET_HIT)
  │
  ├─ 손절 이탈 ──→ CLOSED (STOP_HIT)
  │
  ├─ 시간 만료 ─→ EXPIRED  (TIME_OUT)
  │
  └─ 운영자 수동 ─→ CLOSED (MANUAL_CLOSE)
```

`ExitMonitorJob`이 5분 주기로 OPEN 추천만 감시하고, 목표/손절 이벤트 시 텔레그램 알림 후 상태는 운영자가 수동 또는 별도 API로 변경한다(자동 closing 로직은 아직 없다).

---

## 3. 전체 데이터 흐름 (Flow)

여러 컴포넌트가 어떻게 맞물려 돌아가는지 큰 그림.

```
┌────────────────────────────────────────────────────────────────────┐
│  외부 데이터 소스                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐  │
│  │ KIS API  │ │ KRX KIND │ │ NASDAQ   │ │ Stooq   │ │ Google   │  │
│  │ (KR 시세)│ │ (KR 심볼)│ │ Trader   │ │ (US시세)│ │ News RSS │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ └────┬─────┘  │
│       │            │            │            │           │         │
│  ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────────┐ ┌┴─────────────────┐  │
│  │ DART     │ │ SEC EDGAR│ │ FRED CSV     │ │ Yahoo Finance    │  │
│  │ (KR공시) │ │ (US공시) │ │ (매크로)     │ │ RSS (US 뉴스)    │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────────┘ └┬─────────────────┘  │
└───────┼────────────┼────────────┼────────────┼─────────────────────┘
        ▼            ▼            ▼            ▼
┌────────────────────────────────────────────────────────────────────┐
│  Infrastructure 클라이언트 (HTTP)                                   │
│  KisApiClient · KrxSymbolClient · NasdaqTraderSymbolClient ·       │
│  StooqQuoteClient · RssNewsClient · DisclosureClient ·             │
│  FredMacroClient · SecFundamentalClient                             │
└────────────────────────────────────────────────────────────────────┘
        ▼
┌────────────────────────────────────────────────────────────────────┐
│  Application 서비스                                                  │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐    │
│  │ MarketUniverseService│  │ MarketDataSyncService            │    │
│  │ (심볼 → universe)    │  │ (시세 → price_daily/intraday)    │    │
│  └──────┬───────────────┘  └──────┬───────────────────────────┘    │
│         │                         │                                 │
│         │  ┌──────────────────────┴────────────────┐               │
│         │  │ MarketDataCollectionService           │               │
│         │  │ + MarketSignalScorer                  │               │
│         │  │ (뉴스/공시/매크로/펀더 → 4종 테이블)   │               │
│         │  │ + 감성/중요도 점수 부여               │               │
│         │  └──────┬────────────────────────────────┘               │
│         ▼         ▼                                                 │
│  ┌────────────────────────────────────────────────┐                 │
│  │ UniverseFeatureBuilder                          │                 │
│  │ ▸ 유동성/가격/MA/RSI/거래량/뉴스/공시/매크로/   │                 │
│  │   펀더/데이터품질 점수를 0~100점으로 산출       │                 │
│  │ ▸ 가중합 → totalScore (종합 점수)               │                 │
│  └────────┬────────────────────────────────────────┘                 │
│           ▼                                                          │
│  ┌────────────────────────────────────────────────┐                 │
│  │ RecommendationEngine                            │                 │
│  │ ▸ MA200 시장 레짐 필터 (선택)                    │                 │
│  │ ▸ score / dataQuality 하한 필터                 │                 │
│  │ ▸ 제외 섹터/종목 필터                            │                 │
│  │ ▸ 시장별 시총·거래대금 하한 필터                 │                 │
│  │ ▸ 섹터 분산 캡 (selectTopCandidates)             │                 │
│  │ ▸ 점수 내림차순 정렬, 상위 N개 선택              │                 │
│  └────────┬────────────────────────────────────────┘                 │
│           ▼                                                          │
│  ┌────────────────────────────────────────────────┐                 │
│  │ PricePredictor                                   │                 │
│  │ ▸ 최근 20일 일봉으로 변동성 계산                 │                 │
│  │ ▸ 거래비용(세금/수수료/슬리피지) 가격 반영        │                 │
│  │ ▸ entry / target / stop / expectedExitAt 산출   │                 │
│  │ ▸ 데이터 없으면 422 throw (해시 더미 제거)       │                 │
│  └────────┬────────────────────────────────────────┘                 │
│           ▼                                                          │
│  ┌────────────────────────────────────────────────┐                 │
│  │ RecommendationConfidenceService                  │                 │
│  │ ▸ 과거 평가의 score band별 hit rate              │                 │
│  │ ▸ 샘플 < 30이면 50 반환                          │                 │
│  └────────┬────────────────────────────────────────┘                 │
│           ▼                                                          │
│  ┌────────────────────────────────────────────────┐                 │
│  │ DevRecommendationGenerateService                │                 │
│  │ ▸ modelVersion = 챔피언 semver (또는 dev-rule-v0)│                 │
│  │ ▸ recommendation 테이블에 OPEN 상태로 저장      │                 │
│  │ ▸ signalsJson에 positionWeightPct 포함          │                 │
│  └────────┬────────────────────────────────────────┘                 │
└───────────┼──────────────────────────────────────────────────────────┘
            ▼
┌──────────────────────────────────────────────────────────────────────┐
│  알림                                                                 │
│  KrxPreOpenJob / UsPreOpenJob → TelegramClient                       │
│  (NotificationLogService로 dedupe 후 중복 방지)                       │
└──────────────────────────────────────────────────────────────────────┘
            │
            ▼  (이후 장중에 폴링)
┌──────────────────────────────────────────────────────────────────────┐
│  ExitMonitorJob — 3개 cron, 순수 룰 기반, Codex 없음                  │
│  KR: 09~15시 MON-FRI (KIS) / US: 22~23시 MON-FRI + 00~05시 TUE-SAT  │
│  ▸ 현재가 조회(KIS/Stooq) → price_intraday 저장                       │
│  ▸ 목표가 도달 → 🎯 priceBucket dedupe 알림 + 자동 평가/CLOSED        │
│  ▸ 손절가 이탈 → 🛑 알림 + 자동 평가/CLOSED                           │
│  ▸ expectedExitAt 경과 → 자동 평가/EXPIRED                            │
└──────────────────────────────────────────────────────────────────────┘
            │
            ▼  (ExitMonitor가 자동 생성)
┌──────────────────────────────────────────────────────────────────────┐
│  evaluation 테이블                                                    │
│  StatsService → 통계 집계 (hit rate, 평균 PnL, MDD)                   │
│  DailyBriefService → Codex로 마크다운 일일 보고서 생성                │
└──────────────────────────────────────────────────────────────────────┘
            │
            ▼  (매주 화~토 02:20)
┌──────────────────────────────────────────────────────────────────────┐
│  AutoResearchJob → AutoresearchService                                │
│  1. 챔피언 라이브 검증 (라이브 PnL < 50% × backtest → 자동 롤백)       │
│  2. iterations회: mutateWeights → 백테스트 → KEEP/DISCARD            │
│  3. 최고 후보 → strategy_version 신규 + 챔피언 승격                   │
│  4. 가중치 스냅샷 저장 (롤백용)                                       │
└──────────────────────────────────────────────────────────────────────┘
            │
            ▼ (다음날 추천 생성 시 새 가중치 자동 적용)
```

핵심은 **"외부 데이터 → 점수화 → 필터 → 가격 산출 → 추천 → 자동 청산/평가 → 통계 → 가중치 진화 → 다시 추천"** 의 닫힌 사이클이 자동으로 도는 것.

---

## 4. 기술 스택 & 프로젝트 구조

### 4.1 스택

`build.gradle`:

- **Java 21** (toolchain)
- **Spring Boot 3.3.5**
- **Spring Security** (BasicAuth, In-memory user, `/api/admin/**`·`/api/ops/**`·`/actuator/**` 보호)
- **Spring Data JPA + Hibernate** (`ddl-auto: validate` — DDL은 Flyway만 담당)
- **Flyway** (스키마 마이그레이션, `db/migration/V1..V7`)
- **MSSQL** (`mssql-jdbc`, SQL Server Dialect)
- **springdoc-openapi 2.6.0** (Swagger UI: `/swagger-ui.html`)
- **Lombok**
- **JUnit 5** (현재 테스트 21개 — `application/`, `config/`, `global/`, `infrastructure/`, `scheduler/` 5개 영역 커버. 핵심 단위: AutoresearchService, RecommendationEngine, PricePredictor, RecommendationConfidenceService, BacktestRunService, MarketSignalScorer, SecurityConfig, GlobalExceptionHandler 등)

`application.yml` (공통) + `application-local.yml` (로컬) 주요 설정:

```yaml
# application.yml (공통)
spring.application.name: stock-advisor-backend
spring.profiles.active: local
spring.jpa.hibernate.ddl-auto: validate     # DDL은 Flyway 단독 담당
spring.jpa.open-in-view: false
springdoc.swagger-ui.path: /swagger-ui.html
stock-advisor.security.allowed-origins: [http://localhost:5173, http://127.0.0.1:5173]
stock-advisor.security.protect-dev-api: true
stock-advisor.dart.api-key: ${DART_API_KEY:dev-placeholder}
stock-advisor.sec.user-agent: ${SEC_USER_AGENT:StockAdvisor/1.0 contact@example.com}
stock-advisor.sentiment.enabled: ${SENTIMENT_SIDECAR_ENABLED:false}
stock-advisor.sentiment.base-url: ${SENTIMENT_SIDECAR_BASE_URL:dev-placeholder}

# application-local.yml (로컬)
server.port: 8083
spring.datasource.url: jdbc:sqlserver://localhost:1433;databaseName=stock_advisor
spring.flyway.enabled: true
stock-advisor.codex.command: ${CODEX_COMMAND:dev-placeholder}
stock-advisor.kis.app-key: ${KIS_APP_KEY:dev-placeholder}
stock-advisor.telegram.bot-token: ${TELEGRAM_BOT_TOKEN:dev-placeholder}
stock-advisor.admin.username: ${ADMIN_USERNAME:admin}
stock-advisor.admin.password: ${ADMIN_PASSWORD:change-me}
```

값이 `dev-placeholder` 이면 코드 곳곳의 `MarketUtil.isDevPlaceholder(...)` 체크에서 **개발 모드** 로 분기 → 외부 호출 스킵, 로컬 fallback 응답.

### 4.2 패키지 구조 (Hexagonal-lite)

```
com.parkdh.stockadvisor                (총 171개 .java)
├── StockAdvisorApplication.java       // 진입점
├── config/                            // Spring 설정, @ConfigurationProperties (10개)
│   ├── AppConfig.java                 //   @EnableScheduling, @ConfigurationPropertiesScan
│   ├── SecurityConfig.java            //   BasicAuth + CORS + BCryptPasswordEncoder
│   ├── AdminProperties                //   ADMIN_USERNAME/PASSWORD
│   ├── AppSecurityProperties          //   allowed-origins, protect-dev-api
│   ├── KisProperties                  //   KIS app-key/secret/base-url
│   ├── TelegramProperties             //   bot-token, chat-id
│   ├── DartProperties                 //   DART api-key
│   ├── SecProperties                  //   SEC User-Agent
│   ├── CodexCliProperties             //   codex.command/profile/dailyCallLimit/budget
│   └── SentimentAnalysisProperties    //   sentiment sidecar enabled/base-url
├── api/                               // 컨트롤러 + 요청/응답 DTO (16개 도메인)
│   ├── admin · autoresearch · backtest · brief · codex · dev · evaluation
│   ├── feature · instrument · marketdata · notification · ops · prediction
│   └── recommendation · stats · universe
├── application/                       // 비즈니스 로직 (Service, 16개 도메인)
│   ├── admin · autoresearch · backtest · brief · codex · dev
│   ├── evaluation · feature · instrument · marketdata · notification
│   └── ops · prediction · recommendation · stats · universe
├── domain/                            // JPA 엔티티 (15개 도메인)
│   ├── audit · autoresearch · backtest · brief · codex · common
│   ├── evaluation · instrument · marketdata · notification · prediction
│   └── price · recommendation · setting · universe
├── infrastructure/                    // 외부 시스템 어댑터
│   ├── codex/CodexClient              //   Codex CLI ProcessBuilder 호출
│   ├── marketdata/                    //   외부 시세/뉴스/공시/매크로/펀더멘털
│   │   ├── kr/KisApiClient · KisTokenStore · KrxSymbolClient
│   │   ├── us/NasdaqTraderSymbolClient · StooqQuoteClient
│   │   ├── news/RssNewsClient
│   │   ├── disclosure/DisclosureClient
│   │   ├── macro/FredMacroClient
│   │   └── fundamental/SecFundamentalClient
│   ├── notification/TelegramClient
│   ├── ops/ExternalHealthChecker      //   외부 API 설정/응답 상태 점검 (신설)
│   └── persistence/                   //   JpaRepository (14개 도메인)
│       ├── audit · autoresearch · backtest · brief · codex
│       ├── evaluation · instrument · marketdata · notification
│       └── prediction · price · recommendation · setting · universe
├── scheduler/                         // @Scheduled 8개 클래스 + 1개 헬퍼
│   ├── AutoResearchJob                //   TUE-SAT 02:20 KST 가중치 진화
│   ├── KrxPreOpenJob                  //   MON-FRI 08:30 한국 추천
│   ├── UsPreOpenJob                   //   MON-FRI 22:00/23:00 미국 추천
│   ├── UsCloseSummaryJob              //   TUE-SAT 05:30/06:30 미장 결산
│   ├── ExitMonitorJob                 //   09~15시 폴링, 자동 평가/청산
│   ├── MarketDataCollectionJob        //   뉴스/공시/매크로 수집 3개 메서드
│   └── SchedulerSettingReader         //   app_setting JSON 시각 파싱 헬퍼
└── global/                            // 공통 (예외, ResultDto, util)
    ├── dto/ResultDto
    ├── exception/CustomException · GlobalExceptionHandler
    └── util/MarketUtil · JsonValidationUtil
```

### 4.3 공통 응답 포맷

모든 API는 `ResultDto<T>`로 감싼다 (`global/dto/ResultDto.java`):

```json
{ "code": 200, "data": { ... } }
{ "code": 404, "error_message": "추천을 찾을 수 없습니다." }
```

예외 처리는 `GlobalExceptionHandler`가 전역으로 잡아 `CustomException`은 그대로 코드/메시지를 반환, `MethodArgumentNotValidException`은 400 + 첫 필드 에러 메시지, 그 외는 500 + "서버 처리 중 오류가 발생했습니다." 로 통일.

---

## 5. 데이터베이스 스키마

Flyway 마이그레이션은 V1~V7까지 있다. DDL은 모두 `IF NOT EXISTS` 가드 → 멱등.

### 5.1 V1 — 기본 스키마 (11개 테이블)

| 테이블 | PK | 핵심 컬럼 | 용도 |
|---|---|---|---|
| `app_setting` | `setting_key` | `value_json`, `description`, `updated_by` | 운영 설정 값. JSON 한 줄로 보관 (예: `{"value":3,"min":1,"max":10}`) |
| `audit_log` | `id (IDENTITY)` | `actor`, `action`, `before_json`, `after_json` | 설정 변경 감사 로그. 누가 무엇을 어떻게 바꿨는지 |
| `instrument` | `ticker` | `market`, `name`, `sector`, `enabled` | 수동 등록 종목 (`market_universe`가 비었을 때 fallback) |
| `recommendation` | `id` | `ticker`, `market`, `term`, `entry/target/stop`, `confidence`, `signals_json`, `status` | 추천 결과 본체 |
| `evaluation` | `id` | `recommendation_id`, `actual_exit_price`, `exit_reason`, `pnl_pct`, `drawdown_pct`, `hit_target` | 추천 결과 사후 평가 |
| `prediction` | `id` | `ticker`, `horizon_days`, `predicted_price`, `model_version` | 예측 결과 (현재는 dev-rule-v0로만 채워짐) |
| `backtest_run` | `id` | `strategy`, `period_from/to`, `metrics_json` | 백테스트 실행 결과 |
| `notification_log` | `id` | `channel`, `payload_hash`, `status` | 알림 발송 로그. payload_hash로 중복 발송 방지 |
| `codex_call` | `id` | `caller`, `prompt_hash`, `prompt_len`, `response_len`, `duration_ms`, `succeeded` | Codex CLI 호출 감사 로그. 호출 한도/예산 산정의 근거 |
| `daily_brief` | `id` | `market_track`, `brief_md`, `coverage`, `hallucination_flags`, `llm_model` | Codex가 만든 일일 마크다운 브리프 |
| `autoresearch_run` / `strategy_version` | | | 야간 자동 연구 실험 이력 (현재는 API만 있고 자동 실행 로직은 미구현) |
| `market_universe` | `universe_key (= "market:ticker")` | `name`, `sector`, `market_cap`, `avg_turnover`, `last_price`, `tradable`, `source`, `last_synced_at` | **추천의 출발점.** 자동 수집된 상장 종목 목록 |

### 5.2 V2 — 가격 히스토리

| 테이블 | PK | 핵심 컬럼 | 용도 |
|---|---|---|---|
| `price_daily` | `price_key (= "market:ticker:yyyymmdd")` | OHLCV + `turnover`, `source` | 일봉. KIS / Stooq에서 가져옴 |
| `price_intraday` | `price_key (= "market:ticker:ISO일시")` | `price`, `volume`, `source` | 장중 스냅샷 (5분 단위로 정규화) |

각 테이블에 `(market, ticker, 거래일/스냅샷시각 DESC)` 인덱스 1개씩.

### 5.3 V3 — Exit Confirm + 알림 중복 방지 인덱스

- `exit_confirm_log` : ~~2026-05-26 V8 마이그레이션으로 테이블 삭제됨.~~ ExitConfirm 기능 제거로 미사용.
- `notification_log` 위에 `(channel, payload_hash, status)` 인덱스 추가 → `NotificationLogService.sendTelegramOnce()`의 dedupe 쿼리 인덱스.

### 5.4 V4 — 시장 데이터 수집

| 테이블 | PK | 용도 |
|---|---|---|
| `news_article` | `article_key (= "NEWS:" + sha256(source+url))` | RSS 뉴스 |
| `disclosure_event` | `disclosure_key (= "DART:rcept_no" 또는 "SEC:hash")` | DART / SEC EDGAR 공시 |
| `macro_observation` | `observation_key (= "FRED:seriesId:date")` | FRED 매크로 (10년 국채금리, FFR, CPI, WTI, 달러지수 등) |

각 테이블에 `(market, ticker, 발행일 DESC)` 또는 `(series_id, observed_date DESC)` 인덱스.

### 5.5 V5 — 공시 중요도 점수

`disclosure_event.importance_score (int NULL)` 컬럼 추가 + `IX_disclosure_event_importance` 인덱스.
`MarketSignalScorer.scoreDisclosureImportance()`가 0~100 점수로 채운다 (자세한 산식은 7.3 참조).

### 5.6 V6 — 펀더멘털 지표

`fundamental_metric` (PK `metric_key = "FUND:source:market:ticker:metric:period"`).
SEC Company Facts API에서 가져온 매출/순이익/영업이익/자산/부채/자본/희석EPS를 종목별로 보관.

### 5.7 V7 — 생존편향 보정 준비

`market_universe.delisted_at (date NULL)` 컬럼 + `IX_market_universe_tradable_delisted (market, tradable, delisted_at)` 인덱스.

- 상폐 종목이 동기화에서 사라져도 row 삭제하지 않고 `delisted_at`만 채움 → 백테스트에서 과거 상폐 종목 포함 시뮬레이션 가능 (F-16).
- AutoResearch가 walk-forward 검증할 때 생존편향(현재 거래 가능 종목만 보는 편향)을 점진적으로 보정 가능.

### 5.8 BaseEntity / CreatedEntity

JPA 엔티티들은 `domain/common/BaseEntity`(또는 `CreatedEntity`)를 상속. `@PrePersist`/`@PreUpdate` 훅에서 `created_at`/`updated_at`을 자동으로 채운다. 모든 시간은 `LocalDateTime` 기준이며 `application-local.yml`에서 `jdbc.time_zone: Asia/Seoul`, `jackson.time-zone: Asia/Seoul`로 강제.

---

## 6. 외부 연동 — 데이터를 어디서 어떻게 가져오나

이 백엔드가 의존하는 외부 시스템 전체 목록과 호출 방식.

### 6.1 KIS (한국투자증권) — 한국 시세

- 클래스: `infrastructure/marketdata/kr/KisApiClient.java`, `KisTokenStore.java`
- 설정: `KIS_APP_KEY`, `KIS_APP_SECRET`, `base-url: https://openapi.koreainvestment.com:9443`
- 인증: OAuth2 client_credentials. `KisTokenStore`가 access_token을 **인메모리** 캐시. 만료 10분 전 자동 재발급. 동시성은 `synchronized refreshToken()` + double-check.
- 호출:
  - `fetchCurrentPrice(ticker)` — `/uapi/domestic-stock/v1/quotations/inquire-price`, tr_id=`FHKST01010100`
    - 응답에서 `stck_prpr`(현재가), `prdy_ctrt`(전일대비율), `acml_vol`(누적거래량) 파싱
  - `fetchDailyPrices(ticker, from, to)` — `inquire-daily-itemchartprice`, tr_id=`FHKST03010100`, 수정주가 기준(`FID_ORG_ADJ_PRC=1`)
    - 응답 `output2[]`에서 `stck_bsop_date / stck_oprc / hgpr / lwpr / clpr / acml_vol / acml_tr_pbmn` 파싱
- 개발 모드: `app_key == "dev-placeholder"` 이면 호출 스킵, 빈 결과 반환. 토큰은 더미 `"dev-token"`.

### 6.2 KRX KIND — 한국 상장 종목 목록

- 클래스: `KrxSymbolClient`
- URL: `https://kind.krx.co.kr/corpgeneral/corpList.do?method=download&searchType=13&marketType=stockMkt|kosdaqMkt`
- 응답은 HTML. 정규식(`<tr>...</tr>` → `<td>...</td>`)으로 회사명+종목코드 추출. 인코딩은 Content-Type 헤더로 utf-8/EUC-KR 자동 선택.
- 종목 정규화:
  - 6자리 0-pad (`000660`처럼)
  - 이름에 **"스팩", "기업인수목적", "우" 접미사, "우선주"** 포함이면 `tradable=false`로 표시 → 추천 후보에서 제외.

### 6.3 NASDAQ Trader — 미국 상장 종목 목록

- 클래스: `NasdaqTraderSymbolClient`
- URL:
  - `https://www.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt` (NASDAQ)
  - `https://www.nasdaqtrader.com/dynamic/SymDir/otherlisted.txt` (NYSE 등)
- 응답은 `|` 구분 텍스트. "File Creation Time" 행 제외. **테스트 종목(Test Issue=Y)과 ETF는 제외**.
- NYSE는 `otherlisted.txt`에서 `exchangeCode == "N"` 만 필터.

### 6.4 Stooq — 미국 시세 (공개 CSV)

- 클래스: `StooqQuoteClient`
- 별도 키 불필요. 공개 CSV 엔드포인트.
  - 단일 종가: `https://stooq.com/q/l/?s={ticker}.us&f=sd2t2ohlcv&h&e=csv`
  - 일봉 시계열: `https://stooq.com/q/d/l/?s={ticker}.us&d1=YYYYMMDD&d2=YYYYMMDD&i=d`
- `turnover`는 응답에 없으므로 `close × volume`으로 직접 계산.

### 6.5 RSS 뉴스

- 클래스: `RssNewsClient`
- 시장에 따라 다른 피드 사용:
  - KR: `https://news.google.com/rss/search?q={ticker}+주식&hl=ko&gl=KR`
  - US (티커 있음): `https://feeds.finance.yahoo.com/rss/2.0/headline?s={ticker}&region=US`
  - US (티커 없음): `https://news.google.com/rss/search?q={query}&hl=en-US`
- XML 파서는 보안 강화(`disallow-doctype-decl=true`).
- RSS와 Atom 둘 다 지원. `pubDate`/`updated` 파싱 fallback (RFC_1123 → OffsetDateTime).
- 저장 시 `MarketSignalScorer.scoreNewsSentiment()`로 -1.0 ~ +1.0 감성 점수 부여 (7.3 참조).

### 6.6 DART (한국 공시)

- 클래스: `DisclosureClient.fetchDart()`
- URL: `https://opendart.fss.or.kr/api/list.json?crtfc_key={apiKey}&bgn_de={최근7일}&page_count={limit}`
- API 키 (`DART_API_KEY`) 필요. 없으면 스킵.
- 응답 `list[]`에서 `stock_code`, `report_nm`, `rcept_no`, `rcept_dt`, `corp_name` 추출. URL은 `https://dart.fss.or.kr/dsaf001/main.do?rcpNo={rcept_no}`로 생성.

### 6.7 SEC EDGAR (미국 공시)

- 클래스: `DisclosureClient.fetchSec()` + `SecFundamentalClient`
- 키는 불필요하지만 **User-Agent 헤더 필수** (SEC 규정). `SEC_USER_AGENT` 환경변수로 지정 (예: `"StockAdvisor/1.0 contact@example.com"`).
- 공시: `https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&type=8-K&output=atom` (또는 특정 ticker로)
- 펀더멘털: `https://data.sec.gov/api/xbrl/companyfacts/CIK{10자리}.json`
  - 먼저 `https://www.sec.gov/files/company_tickers.json`에서 ticker → CIK 매핑 조회
  - 7개 us-gaap 태그 추출: Revenues, NetIncomeLoss, OperatingIncomeLoss, Assets, Liabilities, StockholdersEquity, EarningsPerShareDiluted
  - 각 태그에서 **가장 최근 filed/end 기준** 1개씩 가져와 저장

### 6.8 FRED — 매크로 지표

- 클래스: `FredMacroClient`
- URL: `https://fred.stlouisfed.org/graph/fredgraph.csv?id={seriesId}` (키 불필요)
- 기본 수집 시리즈 5개:
  - `DGS10` 10-Year Treasury Constant Maturity Rate
  - `FEDFUNDS` Effective Federal Funds Rate
  - `CPIAUCSL` Consumer Price Index (CPI, 인플레이션)
  - `DCOILWTICO` WTI Crude Oil Spot Price
  - `DTWEXBGS` Trade Weighted U.S. Dollar Index

### 6.9 Telegram

- 클래스: `TelegramClient`
- POST `https://api.telegram.org/bot{token}/sendMessage`, parse_mode=HTML
- 토큰이 placeholder면 stdout 로그만.
- `NotificationLogService.sendTelegramOnce(dedupeKey, message)`로 호출 시 SHA-256(dedupeKey)을 `notification_log.payload_hash`로 사용 → 같은 키로 이미 `SENT` 로그가 있으면 발송 스킵.

### 6.10 Codex CLI

- 클래스: `CodexClient` (`infrastructure/codex/CodexClient.java`)
- `${CODEX_COMMAND}` 값(예: `codex`)을 `ProcessBuilder`로 실행. `--profile {profile} --print {prompt}` 형식.
- 타임아웃 120초.
- 호출 전 **3중 한도 체크**:
  1. 일 호출 횟수 (`codex.daily.callLimit`, 기본 200)
  2. 일 예산 USD (`codex.daily.budgetUsd`, 기본 0=비활성)
     - 추정 비용 = `(누적 텍스트 길이 + 새 프롬프트 길이 + 응답 예상 길이) / 1000 × usdPer1kChars`
  3. placeholder 모드면 곧장 fallback 응답
- 결과는 `codex_call` 테이블에 prompt_hash(SHA-256)/길이/duration_ms/succeeded 등으로 항상 기록.

---

## 7. 추천 엔진 — 어떤 근거로 종목을 고르나 (핵심)

**이 백엔드의 두뇌.** 주식을 모르는 입장에서 가장 궁금한 부분 — "도대체 무슨 근거로 종목을 추천하는가?" 의 답이다.

추천은 3단 파이프라인이다.

```
[1] UniverseFeatureBuilder ─→ 종목별 0~100점 산출
[2] RecommendationEngine  ─→ 필터 + 정렬 + 상위 N개 선택
[3] PricePredictor        ─→ entry/target/stop 가격 산출 (8장)
```

### 7.1 점수 계산의 큰 그림 (UniverseFeatureBuilder)

종목 하나의 **종합 점수(totalScore)** 는 6개 카테고리 점수의 가중합이다.

**가중치 외부화 (F-13)**: 가중치는 `app_setting.recommendation.scoring.weights` JSON에서 동적으로 읽는다. AutoResearch가 이 값을 변형/저장하면 다음 추천 생성부터 즉시 반영. 기본값:

```json
{
  "value":      {"liquidity":0.20, "price":0.10, "technical":0.30, "context":0.15, "fundamental":0.10, "dataQuality":0.15},
  "technical":  {"ma":0.40, "rsi":0.35, "volume":0.25},
  "context":    {"news":0.40, "disclosure":0.18, "macro":0.25, "fundamental":0.17}
}
```

| 카테고리 | 기본 가중치 | 무엇을 보나 |
|---|---:|---|
| 유동성 (liquidityScore) | **20%** | 평균 거래대금이 얼마나 큰가 |
| 가격 (priceScore) | **10%** | 현재 가격대가 추천하기 좋은 구간인가 |
| 기술적 (technicalScore) | **30%** | 추세/RSI/거래량 (서브 가중치 있음) |
| 컨텍스트 (contextScore) | **15%** | 뉴스/공시/매크로/펀더 (서브 가중치 있음) |
| 펀더멘털 (fundamentalScore) | **10%** | SEC 펀더 데이터 확보 수준 |
| 데이터 품질 (dataQualityScore) | **15%** | 누락 없이 필요한 데이터가 모였는가 |

```
totalScore = liquidity*w_liq + price*w_price + technical*w_tech
           + context*w_ctx + fundamental*w_fund + dataQuality*w_dq
```
(최대 100점)

`technicalScore` 내부:
```
technicalScore = movingAverage*w_ma + rsi*w_rsi + volume*w_vol
```

`contextScore` 내부:
```
contextScore = news*w_news + disclosure*w_disc + macro*w_macro + fundamental*w_cfund
```

→ 결국 **종합 점수는 약 15개의 작은 점수들의 합**이며, **AutoResearch가 매주 가중치를 변형 → 백테스트 → 최적 가중치를 챔피언으로 승격**한다.

### 7.2 각 점수 산식 (해설)

#### 유동성 점수 (`scoreLiquidity`)
평균 거래대금 기반. **돈이 잘 돌고 있는 종목**이어야 추천해도 사고 팔 수 있다.

| 평균 거래대금 | 점수 |
|---|---:|
| 데이터 없음 | 35 |
| < 1억 | 50 |
| 1억 ~ 10억 | 70 |
| 10억 ~ 50억 | 85 |
| 50억 이상 | 95 |

#### 가격 점수 (`scorePrice`)
가격대가 너무 싸지도 너무 비싸지도 않아야 좋다고 본다.

| 시장 | 저가 기준 | 고가 기준 | 점수 룰 |
|---|---|---|---|
| KOSPI/KOSDAQ | 5,000원 | 500,000원 | 미만 45, 초과 70, 그 사이 85 |
| NASDAQ/NYSE | $5 | $1,000 | 미만 45, 초과 70, 그 사이 85 |
| 데이터 없음 | | | 40 |

#### 이동평균 점수 (`scoreMovingAverage`)
**MA5와 MA20을 비교해 추세를 본다.** 일봉 20개 이상 있어야 정상 계산.

| 조건 | 점수 |
|---|---:|
| 종가 ≥ MA5 ≥ MA20 (정배열, 강한 상승) | **92** |
| 종가 ≥ MA20 (20일선 위) | 78 |
| 종가 < MA20 × 0.95 (20일선 아래 5% 이탈) | 45 |
| 그 외 (중립) | 62 |
| 일봉 5~19개 (데이터 부족) | 55 |
| 일봉 4개 이하 (데이터 거의 없음) | 40 |

#### RSI 점수 (`scoreRsi`)
표준 14일 RSI 공식 사용:
```
RS  = sum(상승폭) / sum(하락폭)
RSI = 100 - 100 / (1 + RS)
```

| RSI 구간 | 의미 | 점수 |
|---|---|---:|
| 45 ~ 65 | 안정적 상승 구간 | **90** |
| 35 ~ 75 | 일반 허용 | 75 |
| 25 ~ 85 | 과열/침체 경계 | 55 |
| < 25 or > 85 | 극단 (반전 위험) | 35 |
| 일봉 14개 미만 | 데이터 부족 | 45 |

#### 거래량 점수 (`scoreVolume`)
**최신 거래량이 20일 평균보다 얼마나 튀어 올랐나** — z-score로 측정.

```
z = (오늘 거래량 - 20일 평균) / 표준편차
```

| z 구간 | 점수 |
|---|---:|
| z ≥ 1.5 (강한 증가) | **92** |
| 0 ≤ z < 1.5 (평균 이상) | 76 |
| -1.0 ≤ z < 0 (중립) | 60 |
| z < -1.0 (위축) | 45 |
| 표준편차 0 (변동 없음) | 60 |
| 데이터 부족 | 40~55 |

#### 뉴스 점수 (`scoreNews` + `applySentimentAdjustment`)
**뉴스 빈도 + 평균 감성** 두 축.

빈도 기본점:
- 5건 이상: 88
- 2~4건: 72
- 1건: 58
- 0건: 45

여기에 `averageSentiment × 15` (반올림, ±15 범위)를 더하고 25~100으로 클립.

감성 점수 계산은 `MarketSignalScorer.scoreNewsSentiment(title, summary)`:
- 긍정 키워드 (영문+한글 약 20개): `beat / surge / upgrade / 호실적 / 급등 / 수주 / 흑자 ...`
- 부정 키워드 (영문+한글 약 20개): `miss / drop / downgrade / 급락 / 적자 / 소송 / 리콜 ...`
- 점수 = `clip(-1.0, +1.0, (긍정수 - 부정수) × 0.35)`

#### 공시 점수 (`scoreDisclosures`)
공시 개수와 **최대 중요도**를 본다.

| 조건 | 점수 |
|---|---:|
| maxImportance ≥ 85 (중요 공시 있음) | **82** |
| 공시 3건 이상 | 72 |
| 공시 1~2건 | 64 |
| 공시 없음 | 55 |

공시 중요도(`scoreDisclosureImportance`)는 키워드 매칭:
- HIGH (90점): `8-k / merger / acquisition / bankruptcy / earnings / 합병 / 유상증자 / 영업정지 / 횡령 / 실적 ...`
- MEDIUM (70점): `dividend / contract / appointment / 배당 / 주주총회 / 자사주 / 임원 ...`
- 그 외: 50점

#### 매크로 점수 (`scoreMacro`)
최근 매크로 시리즈가 **몇 종류** 확보됐는가.

| seriesCount | 점수 |
|---|---:|
| 5 이상 | 82 |
| 3~4 | 70 |
| 1~2 | 58 |
| 0 | 45 |

#### 펀더멘털 점수 (`scoreFundamentals`)
SEC Company Facts에서 가져온 펀더 metric 종류 수.

| metricCount | 점수 |
|---|---:|
| 6 이상 | 84 |
| 3~5 | 70 |
| 1~2 | 58 |
| 0 | 45 |

미국 종목만 의미 있다(SEC). 한국 종목은 항상 45점 → contextScore에서 17%만 영향.

#### 데이터 품질 점수 (`scoreDataQuality`)
필수 데이터가 얼마나 모였는가.

| 항목 | 가산점 |
|---|---:|
| 기본 | 40 |
| `last_price` 존재 | +15 |
| `avg_turnover` 존재 | +15 |
| 일봉 20개 이상 | +20 |
| 일봉 5~19개 | +10 |
| `last_synced_at` 존재 | +10 |
| 최대 | 100 |

데이터가 빈약한 종목은 `dataQualityScore`가 낮아져 `totalScore`도 자동으로 떨어진다.

### 7.3 MarketSignalScorer

`MarketSignalScorer`는 점수 매기는 작은 룰 엔진 3개를 묶어둔 컴포넌트:
- `scoreNewsSentiment(title, summary)` → -1.0 ~ +1.0 (부정문 처리 포함)
- `scoreDisclosureImportance(title, type)` → 50/70/90
- `classifyDisclosureType(title, original)` → `EARNINGS / M&A / FINANCING / DIVIDEND / CONTRACT / GENERAL`

**부정문 처리 (F-14)**: `NEGATED_POSITIVE_PHRASES` 키워드 (예: "수주 취소", "급등주 조심", "흑자 전환 실패" 등) 매칭 시 긍정 카운트에서 차감하고 부정으로 재분류:

```java
int positive = countMatches(text, POSITIVE_NEWS);
int negative = countMatches(text, NEGATIVE_NEWS);
int negatedPositive = countMatches(text, NEGATED_POSITIVE_PHRASES);
positive = max(0, positive - negatedPositive);
negative += negatedPositive;
```

→ "수주 취소" 같은 케이스가 positive +1로 계산되던 문제 해결. NLP 수준은 아니지만 substring 매칭의 명백한 오류는 차단.

`MarketDataCollectionService.upsertNews/upsertDisclosure`에서 저장 시점에 호출되어 컬럼에 미리 채워진다 (조회 시 재계산하지 않음).

### 7.4 후보 선별 (RecommendationEngine)

`buildCandidates(market)`:

1. **시장 레짐 필터 (F-9)**: `recommendation.regime.filter.enabled` 가 true 이면 해당 시장의 인덱스 일봉 200개로 MA200 계산. 현재가 < MA200 이면 빈 리스트 반환 (약세장 매수 차단).
2. **`UniverseFeatureBuilder.buildFeatures(market)`** 호출 → 종목별 점수.
3. **필터 적용**:
   - 최소 종합 점수: `recommendation.feature.minTotalScore` (기본 0)
   - 최소 데이터 품질: `recommendation.feature.minDataQualityScore` (기본 0)
   - 제외 종목: `recommendation.watchlist.exclude`
   - 제외 섹터: `recommendation.excluded.sectors`
   - 시장별 시총 하한: `recommendation.marketcap.{kr|us}.min`
   - 시장별 거래대금 하한: `recommendation.turnover.{kr|us}.min`
4. **정렬**: `score DESC, ticker ASC` → 동점이면 알파벳 순으로 결정적.

`selectTopCandidates(market, count)`:

`buildCandidates(market)` 결과에 **섹터 분산 캡 (F-10)** 적용:
- 종목 순회 중 같은 섹터가 `recommendation.sector.max` (기본 2) 이상이면 스킵
- 최종 `count`개 반환

→ 추천 생성/백테스트 모두 이 메서드를 호출하므로 **반도체 5개 다 뽑히는 사고 차단**.

**Fallback 로직**: `market_universe`가 비었거나 모두 필터에 걸리면 `instrument` 테이블의 수동 등록 종목으로 대체. 이때 점수는 일률적으로 50점(`instrument_fallback`).

### 7.5 추천 생성 (DevRecommendationGenerateService)

`/api/dev/recommendations/generate?market=KOSPI&shortCount=3&longCount=3` 또는 스케줄러가 호출.

```
1. modelVersion = StrategyVersionRepository.findByChampion(true) 의 최신 promotedAt semver
   ↳ 챔피언 없으면 "dev-rule-v0" fallback (C-3 해결)
2. RecommendationEngine.selectTopCandidates(market, max(shortCount, longCount) * 3) → 섹터 분산 적용된 후보
3. 각 후보를 PricePredictor.predict(candidate, "SHORT")로 시범 호출:
   - 가격 데이터 있는 후보만 통과 (data 부족하면 422 → 추천에서 제외, C-2 해결)
4. 상위 shortCount → SHORT 추천 저장
5. 상위 longCount → LONG 추천 저장
6. confidence = RecommendationConfidenceService.estimateConfidence(market, term, featureScore):
   - 과거 평가 데이터의 (market, term, scoreBand=featureScore/10) 매칭
   - hit rate × 100 반환, 샘플<30이면 50 (C-1 해결)
7. signalsJson에 source / featureScore / positionWeightPct / featureJson 전체를 JSON으로 저장
   - positionWeightPct = (1 / riskPct × confidenceScale) clip 0~20 (F-15)
```

→ **현재는 여전히 "feature 점수 상위 N개" 휴리스틱**이지만, modelVersion이 챔피언과 연동되므로 AutoResearch가 가중치를 바꿀 때마다 다음 추천이 새 가중치로 생성됨. 진짜 학습 사이클이 닫혀 있다.

---

## 8. 가격 산출(PricePredictor) — 진입가/목표가/손절가 계산

`application/recommendation/PricePredictor.java`. 후보 + 기간(`SHORT`/`LONG`)을 입력받아 `PredictedRecommendation(entry, target, stop, expectedExitAt, pricingMethod)` 반환.

### 8.1 진입가 (entry price)

1. 최근 20개 일봉 중 가장 최신 종가 → 사용 (`pricingMethod="volatility-v1"` 또는 `"recent-close-v1"`)
2. 없으면 `candidate.lastPrice` (universe의 마지막 가격, `"last-price-v1"`)
3. **둘 다 없으면 `CustomException(422)` throw → 추천 발행 차단** (C-2 해결, 해시 더미 가격 제거)
4. 모두 소수점 4자리 HALF_UP 반올림.

### 8.2 변동성 (volatilityPct)

```
move_i = |close_i - close_(i-1)| / close_(i-1)     # 절대 수익률
volatilityPct = average(move_i)                     # 평균
volatilityPct ∈ [0.01, 0.08]                        # 1~8%로 클립
```
일봉 5개 미만이면 기본 0.02 사용.

### 8.3 목표가 배수 / 손절가 배수

| 기간 | 목표 배수 | 손절 배수 | 의미 (변동성 2% 가정) |
|---|---|---|---|
| **SHORT** (단기) | `1 + max(volatility × 2.5, 0.035)` | `1 - max(volatility × 1.5, 0.025)` | 목표 +5%, 손절 -3% 정도 |
| **LONG** (장기) | `1 + max(volatility × 8, 0.12)` | `1 - max(volatility × 4, 0.08)` | 목표 +16%, 손절 -8% 정도 |

→ 같은 종목이라도 단기/장기에 따라 목표·손절 폭이 약 3~5배 차이.

### 8.4 거래비용 반영 (F-8)

원시 목표/손절 배수는 "비용 차감 후" 실효 수익률 기준. 실제 호가는 비용을 보정해서 더 높게/타이트하게 산출.

```java
effectiveEntry = entry × (1 + entryCostRate)
desiredEffectiveExit = effectiveEntry × multiplier
rawExitPrice = desiredEffectiveExit / (1 - exitCostRate)
```

비용 계산 (`resolveTradingCost`):
- **한국**: entry = `feePercent + slippage`, exit = `taxPercent + feePercent + slippage` (한국 거래세는 매도시만)
- **미국**: 양방향 `fxSpreadPercent/2 + slippage`
- 모두 `app_setting.backtest.cost.{kr|us}` + `backtest.slippage.percent` 에서 읽음 (운영자가 변경 가능)

→ SHORT 목표 +5% 명목이 비용 차감 후 +5% 실수익 보장.

### 8.5 예상 청산일 (expectedExitAt)

- SHORT → `LocalDate.now().plusDays(5)` (5거래일 ≈ 1주일)
- LONG → `LocalDate.now().plusMonths(6)` (6개월)

### 8.6 pricingMethod 표시

`pricingMethod = base + "-cost-adjusted"` 형식.
- base는 데이터 수준에 따라:
  - 일봉 5개 이상 → `"volatility-v1"`
  - 일봉 1~4개 → `"recent-close-v1"`
  - 일봉 0개 + lastPrice 있음 → `"last-price-v1"`
  - 둘 다 없음 → `"unavailable"` (예외 throw로 추천 차단)

---

## 9. 손절 모니터링 — ExitMonitorJob (순수 룰 기반)

**2026-05-26 변경**: `ExitConfirmService`(Codex 연동 HOLD/CUT/TIGHTEN 판단) 전면 제거. `exit_confirm_log` 테이블 삭제(V8 마이그레이션). 손절가 도달 즉시 STOP_HIT으로 자동 청산.

- 별도 "위험 구간" 개념 없음 — `stopPrice >= currentPrice` 이면 즉시 STOP_HIT
- KR 가격: `KisApiClient.fetchCurrentPrice()`, US 가격: `StooqQuoteClient.fetchQuote()`
- 3개 cron으로 KR 장(09~15시) + US 장 저녁(22~23시) + US 장 심야(00~05시) 커버
- 세부 로직: §10.5 참고

---

## 10. 스케줄러 11종 — 자동 동작 시각표

모든 스케줄러는 `scheduler/` 패키지. `@EnableScheduling`은 `AppConfig`에 켜져 있다.

핵심 디자인 패턴: **"cron은 매분 깨우고, 실제 동작 시각은 `app_setting`의 HH:mm 값과 비교"**. 운영자가 DB만 바꾸면 시각 변경. `SchedulerSettingReader`가 JSON 값을 파싱한다.

| 스케줄러 | cron | 기본 동작 시각 (KST) | 동작 |
|---|---|---|---|
| `AutoResearchJob` | `0 20 2 * * TUE-SAT` | 02:20 | 챔피언 롤백 검증 → 가중치 변형 N회 → 백테스트 → 최고면 새 챔피언 승격 |
| `KrxPreOpenJob` | `0 * * * * MON-FRI` | 08:30 | KR 심볼 동기화 → KOSPI/KOSDAQ 최신 일봉 증분만 확인 → 추천 생성 → 텔레그램 |
| `UsPreOpenJob` | `0 * * * * MON-FRI` | 22:00 (DST) / 23:00 (표준) | US 심볼 → NASDAQ 가격 50개 → NASDAQ 최신 일봉 증분만 확인 → 추천 → 텔레그램 |
| `UsCloseSummaryJob` | `0 * * * * TUE-SAT` | 05:30 (DST) / 06:30 (표준) | OPEN 미국 추천 전체에 대해 종가 기준 PnL/손절거리 요약 + 하위 5건 → 텔레그램 |
| `ExitMonitorJob`.runKr | `0 * 9-15 * * MON-FRI` (KST) | 평일 09~15시, 폴링 주기 | KR(KOSPI/KOSDAQ) 현재가(KIS) → 자동 평가/청산 |
| `ExitMonitorJob`.runUsEvening | `0 * 22-23 * * MON-FRI` (KST) | 평일 22~23시, 폴링 주기 | US 현재가(Stooq) → 자동 평가/청산 |
| `ExitMonitorJob`.runUsNight | `0 * 0-5 * * TUE-SAT` (KST) | 화~토 00~05시, 폴링 주기 | US 현재가(Stooq) → 자동 평가/청산 |
| `MarketDataCollectionJob`.runKrxPreOpenCollection | `0 15 8 * * MON-FRI` | 08:15 | KRX 뉴스/공시 수집 |
| `MarketDataCollectionJob`.runUsPreOpenCollection | `0 40 21 * * MON-FRI` | 21:40 | US 뉴스/공시/펀더멘털 수집 |
| `MarketDataCollectionJob`.runMacroCollection | `0 0 7 * * MON-FRI` | 07:00 | FRED 매크로 수집 (default 5개 시리즈) |
| `DailyPriceBackfillJob`.runKrxBackfill | `0 10 18 * * MON-FRI` | 18:10 | KOSPI/KOSDAQ 후보군 일봉 백필. 비어 있거나 오래된 히스토리를 장후에 보강 |
| `DailyPriceBackfillJob`.runUsBackfill | `0 20 7 * * TUE-SAT` | 07:20 | NASDAQ/NYSE 후보군 일봉 백필. 비어 있거나 오래된 히스토리를 미장 마감 후 보강 |

**ExitMonitor 폴링 유연화**: cron이 매분 깨우고, `minuteOfDay % pollingIntervalMinutes == 0` 인 분에만 실제 동작. `app_setting.exit.polling.intervalMinutes` (1/3/5/10/30 옵션, 기본 5) 변경으로 주기 조절.

**AutoResearch 시점**: 02:20 KST → 한국 프리오픈 08:30 전에 새 가중치 적용 완료. 데이터 신선도 OK.

### 10.1 시각 매칭 패턴 (KrxPreOpenJob 예)

```java
@Scheduled(cron = "0 * * * * MON-FRI", zone = "Asia/Seoul")
public void run() {
    String configuredTime = settingReader.getStringField(
        "notification.krx.preopen.offsetMinutes", "displayTime", "08:30");
    if (!isCurrentSeoulMinute(configuredTime)) return; // 그 시각이 아니면 그냥 종료
    // ... 본 로직
}
```

→ 매분 깨어나서 비교. 시각이 일치하지 않으면 즉시 return. 비용 거의 없음.

### 10.2 US DST 처리

`UsPreOpenJob`, `UsCloseSummaryJob`은 `ZoneId.of("America/New_York").getRules().isDaylightSavings(Instant.now())` 로 미국 동부 DST 여부를 매분 체크. DST일 때는 `dstTime` 필드, 표준시일 때는 `standardTime` 필드 사용.

### 10.3 휴장일 처리

각 스케줄러는 시작 전에 `notification.holiday.kr.closedDates` / `notification.holiday.us.closedDates` JSON 배열에서 오늘 날짜를 찾는다. 포함되면:
- `notification.holiday.enabled=true` (기본) → 텔레그램으로 "휴장일입니다" 알림 1회 전송 후 종료
- false → 조용히 종료

### 10.4 시장 활성화 토글

`recommendation.market.enabled` JSON (`{"kr": true, "us": true}`)으로 KR/US 추천을 각각 끌 수 있다. KrxPreOpenJob은 `kr`, UsPreOpenJob/UsCloseSummaryJob은 `us` 필드를 확인.

### 10.5 ExitMonitorJob 세부 로직 (순수 룰 기반, Codex 없음)

3개 cron이 각각 `runForRegion("KR")` / `runForRegion("US")` 호출. 폴링 주기: `minuteOfDay % exit.polling.intervalMinutes == 0` 인 분에만 실행.

```
for each recommendation in 추천.status=OPEN  (해당 region 필터):
    # 현재가 조회
    if KR market (KOSPI/KOSDAQ):
        currentPrice, volume = KisApiClient.fetchCurrentPrice(ticker)
    else (NASDAQ/NYSE/AMEX):
        currentPrice, volume = StooqQuoteClient.fetchQuote(ticker)
    if 없음: skip

    MarketDataSyncService.saveIntradayPrice(...)  # price_intraday 기록

    # 순수 룰 기반 자동 청산
    targetReached = targetPrice <= currentPrice
    stopBreached  = stopPrice  >= currentPrice

    if targetReached: 🎯 priceBucket dedupe 텔레그램 알림
    if stopBreached:  🛑 priceBucket dedupe 텔레그램 알림

    if targetReached or stopBreached:
        closeRecommendationWithEvaluation(exitReason=TARGET_HIT|STOP_HIT)
            → evaluation 자동 생성 (pnlPct, drawdownPct)
            → recommendation.status = "CLOSED"
        continue
    if today > expectedExitAt:
        closeRecommendationWithEvaluation(exitReason=TIME_OUT)
            → status = "EXPIRED"
```

ExitConfirm / Codex 호출 없음. 손절가 도달 즉시 룰로 STOP_HIT 처리.

`priceBucket` dedupe: `currentPrice / entryPrice` 를 1% 단위(소수점 2자리)로 라운딩 → 같은 가격 구간에선 같은 이벤트 중복 알림 안 감 (O-4 해결).

### 10.6 AutoResearchJob 세부 (F-12)

```
매주 화~토 02:20 KST:
    if autoresearch.enabled != true: skip

    periodTo   = 어제
    periodFrom = 어제 - 365일
    iterations = autoresearch.targetIterations (기본 8, 최대 80)

    AutoresearchService.runAutoResearch(...):
        # 1. 챔피언 롤백 검증 (validateChampionRollback)
        if 최신 챔피언 promotedAt + rollbackValidationDays(7일) 경과:
            liveMetric = 해당 모델로 발행된 추천의 evaluation 평균 PnL
            if liveMetric < championMetric × 0.5:
                현재 챔피언 demote
                직전 챔피언 promote (저장된 가중치 스냅샷 복구)
                audit_log: ROLLBACK_CHAMPION

        # 2. 가중치 변형 + 백테스트
        originalWeights = app_setting.recommendation.scoring.weights
        for iterNo in 1..iterations:
            path = MUTATION_PATHS[(iterNo-1) % 11]  # 11개 경로 순환
            proposalJson = mutateWeights(originalWeights, path, factor)
                factor = 1.10 (홀수) 또는 0.90 (짝수)
            normalizeWeightGroup(value/technical/context)  # 합 1.0으로 정규화
            saveWeights(proposalJson)  # AppSetting 갱신 + audit_log
            backtestRunService.evaluateRecommendationEngine(...) → metricValue (avgPnlPct)
            autoresearch_run 저장 (KEEP/DISCARD/ERROR)

        # 3. 챔피언 승격
        if 개선 후보 있음:
            saveWeights(bestWeights) (audit_log)
            기존 챔피언 demote
            strategy_version 신규 row (semver="ar-yyyyMMddHHmmss", is_champion=true)
            saveStrategyWeights(semver, bestWeights)  # 롤백용 스냅샷
        else:
            saveWeights(originalWeights) 복구
```

핵심 디자인:
- **iteration별 트랜잭션 분리**: 클래스/메서드에 `@Transactional` 없음. 각 save는 자체 트랜잭션 → 80회 반복 시 MSSQL 락 시간 짧음
- **가중치 정규화**: `normalizeWeightGroup`이 각 그룹 합을 1.0으로 강제 → mutate 후 합 깨짐 방지 (수정 이슈 #2)
- **감사 로그 통합**: `saveWeights` 가 직접 `audit_log` 기록 (수정 이슈 #3)
- **롤백 검증**: `validateChampionRollback`이 다음 사이클 시작 직전 실행, 라이브 PnL 평균이 백테스트 metric의 50% 미만이면 직전 챔피언으로 자동 복구 (수정 이슈 #4)
- **시점**: 02:20 KST → 08:30 KRX 프리오픈 전에 완료. 그날 추천부터 새 가중치 적용

### 10.7 MarketDataCollectionJob 세부

```
runKrxPreOpenCollection (08:15):
    for market in [KOSPI, KOSDAQ]:
        for ticker in 거래대금 상위 N개 (기본 5):
            RSS 뉴스 수집
    for market in [KOSPI, KOSDAQ]:
        DART 공시 수집 (limit 20)

runUsPreOpenCollection (21:40):
    for market in [NASDAQ, NYSE]:
        for ticker in 거래대금 상위 5:
            RSS 뉴스 수집
        for ticker in 거래대금 상위 3:
            SEC 펀더멘털 수집
        SEC 공시 수집 (limit 20)

runMacroCollection (07:00):
    FRED 5개 기본 시리즈 각 5건씩
```

각 시도는 try/catch로 감싸 실패해도 다른 작업은 진행. 발송도 `NotificationLogService.sendTelegramOnce`로 dedupe.

---

## 11. 백테스트 — 과거 데이터로 전략 검증

`application/backtest/BacktestRunService.java`. 세 가지 사용 패턴:

### 11.1 결과 저장만 (`POST /api/backtests`)

외부에서 계산한 metrics_json을 그냥 적재. `period_from <= period_to` 검증 + JSON 형식 검증 후 저장.

### 11.2 시뮬레이션 실행 (`POST /api/backtests/simulate`)

저장된 `price_daily`로 **단순 룰 기반 매매**를 시뮬레이션한다. 전략 이름은 `ma20-breakout-v0` (기본).

요청 파라미터:
- `market`: KOSPI/KOSDAQ/NASDAQ/NYSE/ALL (기본 ALL)
- `periodFrom / periodTo`: 백테스트 기간 (필수)
- `maxTickers`: 1~300 (기본 30)
- `holdingDays`: 1~120 (기본 20)
- `targetPct`: 0.1~50% (기본 3.0)
- `stopPct`: 0.1~50% (기본 2.0)

알고리즘:
```
1. 기간 + 시장 조건으로 price_daily 조회, (market:ticker)별 그룹화
2. 상위 maxTickers개 종목만 사용
3. 각 종목별 시뮬레이션:
   for i in [20, len-1):
       MA20 = 직전 20일 종가 평균
       if 오늘 종가 >= MA20: 다음 거래일 종가에 진입 (1회만)
   entry 잡힌 후:
       targetPrice = entry × (1 + targetPct/100)
       stopPrice   = entry × (1 - stopPct/100)
       holdingDays 동안 다음 조건 검사 (먼저 만나면 청산):
           - low <= stopPrice  → exit, "STOP"
           - high >= targetPrice → exit, "TARGET"
       조건 안 맞으면 holdingDays째 종가로 청산, "TIME"
4. pnlPct = (exit - entry) / entry × 100
5. 집계:
   - tradeCount, wins, losses, targetHits, stopHits, timeExits
   - hitRate = wins / tradeCount × 100
   - avgPnlPct, totalPnlPct
   - maxDrawdownPct = 누적 PnL 곡선의 최대 낙폭
   - sampleTrades 상위 20건 포함
6. metricsJson에 모두 직렬화 → backtest_run 테이블 저장
```

→ ML 백테스트가 아니라 **가장 단순한 MA20 돌파 전략 검증** 용도. AutoResearch에서 새 전략을 평가하는 베이스라인.

**거래비용 반영 (F-8)**: target/stop trigger는 raw 가격으로 판정하되, PnL 계산은 entry/exit에 각각 `cost.entryCostRate / exitCostRate` 적용:
```java
effectiveEntryPrice = entryPrice × (1 + entryCostRate)
effectiveExitPrice  = exitClose  × (1 - exitCostRate)
pnlPct = (effectiveExit - effectiveEntry) / effectiveEntry × 100
```

### 11.3 추천 엔진 시뮬레이션 (`evaluateRecommendationEngine`, F-7)

AutoResearch가 호출하는 핵심 메서드. 추천 엔진의 종목 선별 + MA20 청산 룰 시뮬레이션을 일체화.

```
input: BacktestSimulationRequest (strategy="recommendation-engine-v1", market, period, maxTickers, ...)

1. RecommendationEngine.selectTopCandidates(market, maxTickers):
   ▸ 현재 app_setting의 가중치로 점수 계산
   ▸ 섹터 분산 캡 적용
   ▸ 상위 maxTickers개 종목 선택
2. selectedKeys = {market:ticker} 집합
3. 기간 내 price_daily 조회 → selectedKeys에 속한 row만 남김
4. 종목별로 simulateTicker() 호출 (11.2의 MA20 룰)
5. 결과 집계: tradeCount, hitRate, avgPnlPct, totalPnlPct, maxDrawdownPct, sampleTrades
6. metricsJson 작성 + backtest_run 저장
7. BacktestEvaluation(strategy, periodFrom, periodTo, metricsJson, metricValue=avgPnlPct) 반환
```

**의미**: AutoResearch가 가중치를 바꿔도 → 같은 백테스트 함수가 새 점수로 종목을 골라 → 같은 청산 룰로 시뮬레이션 → metric 비교 가능. 이전엔 분리됐던 추천 엔진 ↔ 백테스트가 닫힌 루프로 연결됨.

---

## 12. 통계 / 데일리 브리프 / AutoResearch

### 12.1 StatsService

API:
- `GET /api/stats/summary` — `StatsSummaryResponse`
- `GET /api/stats/daily` — 최근 30일 일별 통계 (최신순)
- `GET /api/stats/by-strategy` — `model_version`별 집계

`StatsSummaryResponse` 필드:

| 필드 | 의미 |
|---|---|
| `total` | 전체 추천 수 |
| `open / closed / expired` | 상태별 카운트 |
| `hitRate` | TARGET 도달 비율 (소수점 1자리 %) |
| `avgPnlPct` | 평가된 추천의 평균 PnL |
| `totalPnlPct` | 평가된 추천 PnL 총합 |
| `maxDrawdown` | 평가의 `drawdown_pct` 최솟값 (= 가장 큰 손실) |
| `byTerm` | SHORT/LONG별 (count, hitRate, avgPnlPct) |
| `byExitReason` | TARGET_HIT/STOP_HIT/TIME_OUT/MANUAL_CLOSE별 카운트 |

`getDaily`는 `evaluation.evaluated_at` 날짜별로 묶어 `cumulative` PnL까지 계산해서 반환 (Frontend 차트용).

`getByStrategy`는 추천과 평가를 join해 `model_version`별 hit rate / 평균 PnL을 낸다 → AutoResearch에서 챔피언 모델을 가릴 때 사용.

### 12.2 DailyBriefService — Codex가 쓰는 일일 보고서

`POST /api/dev/brief/generate?marketTrack=KRX|US|US_CLOSE|KOSPI|...`

```
1. marketTrack을 실제 시장 목록으로 변환
   KRX → [KOSPI, KOSDAQ]
   US, US_CLOSE → [NASDAQ, NYSE]
2. 컨텍스트 수집:
   - OPEN 추천 최근 5건
   - 거래대금 상위 후보군 8건
   - 최근 일봉 20건
   - 최근 뉴스 10건, 공시 10건
   - 최근 매크로 20건, 펀더 20건
   - StatsService.getSummary() 결과
3. 마크다운 프롬프트 작성 (700자 이내, 4개 섹션 강제):
   - 시장 요약 / 추천 점검 / 리스크 / 오늘 할 일
   - "제공된 데이터에 없는 사실은 추정하지 말고 '데이터 없음'이라고 적어"
4. CodexClient.call(prompt, profile="stock-advisor", caller="daily-brief")
5. Codex가 실패하거나 dev-placeholder면 로컬 템플릿 fallback (8개 데이터 영역 카운트 + 추천 bullet + 통계)
6. coverage 점수 = 8개 데이터 영역 중 데이터 있는 영역 / 8 (0.0~1.0)
7. daily_brief 테이블에 마크다운 본문, coverage, hallucination_flags(=Codex 실패 시 1)와 함께 저장
```

`coverage`는 운영 화면에서 "이 브리프가 신뢰할 만한가" 판단용 표시.

### 12.3 AutoresearchService — 자동 진화 사이클 (F-12)

**2026-05-22 업데이트**: 이전엔 CRUD만 있던 껍데기였으나, 본체 구현 완료. 매주 화~토 02:20 KST에 `AutoResearchJob`이 자동 실행.

#### 12.3.1 데이터 모델

| 테이블 | 역할 |
|---|---|
| `autoresearch_run` | 단일 실험 1회 기록 — (jobRunId, iterNo, parentSha, proposalSha, diffSummary, metricName, metricValue, championMetric, decision: KEEP/DISCARD/ERROR, durationMs) |
| `strategy_version` | 챔피언 이력 — (semver, gitSha, metricValue, promotedAt, is_champion) |
| `app_setting.recommendation.scoring.weights` | 현재 운영 중인 가중치 JSON (AutoResearch가 갱신) |
| `app_setting.autoresearch.weights.{semver}` | 챔피언별 가중치 스냅샷 (롤백용) |

#### 12.3.2 동작 흐름

10.6 AutoResearchJob 세부 참조. 요약:

1. **롤백 검증 (validateChampionRollback)**: 챔피언 promotedAt + 7일 경과 시, 해당 modelVersion으로 발행된 추천의 evaluation 평균 PnL을 계산. 백테스트 metric의 50% 미만이면 직전 챔피언으로 자동 복구 + audit_log 기록.

2. **가중치 변형 (mutateWeights)**: 11개 경로 (`value.{liquidity|price|technical|context|fundamental|dataQuality}`, `technical.{ma|rsi|volume}`, `context.{news|disclosure|macro|fundamental}`) 중 하나를 1.10배 또는 0.90배. `normalizeWeightGroup` 으로 그룹별 합 1.0 강제.

3. **백테스트 (evaluateRecommendationEngine)**: 변형 가중치 적용 후 추천 엔진 시뮬레이션 → `avgPnlPct` 메트릭.

4. **승격 (promotion)**: 모든 iteration 중 가장 높은 metric > 기존 챔피언이면, 새 `strategy_version` row + `is_champion=true`. 직전 챔피언 demote. 가중치 스냅샷 저장.

5. **추천 반영**: 다음 추천 생성 시 `DevRecommendationGenerateService.resolveModelVersion()` 가 새 챔피언 semver를 `modelVersion`으로 사용.

#### 12.3.3 설정 키

- `autoresearch.enabled` (기본 true) — 잡 전체 토글
- `autoresearch.targetIterations` (기본 8, 최대 80) — 반복 횟수
- `autoresearch.rollbackValidationDays` (기본 7) — 챔피언 라이브 검증 기간

#### 12.3.4 API

- `GET/POST /api/autoresearch/runs` — 실험 이력 CRUD
- `GET/POST /api/autoresearch/strategy-versions` — 전략 버전 CRUD
- `POST /api/autoresearch/auto-run` — 즉시 1회 실행 (수동 트리거)

#### 12.3.5 학습 사이클 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│  매주 화~토 02:20 AutoResearchJob                                    │
│  ┌─────────────────────────┐                                         │
│  │ 1. 챔피언 롤백 검증     │ ──→ 라이브 PnL < 50% × backtest        │
│  │    (7일 경과 시)        │     → 직전 챔피언으로 복구              │
│  └────────┬────────────────┘                                         │
│           ▼                                                          │
│  ┌─────────────────────────┐                                         │
│  │ 2. iterations회 반복:   │ ──→ mutateWeights (1.10 or 0.90)       │
│  │    a. 변형              │     normalizeWeightGroup (합=1)         │
│  │    b. saveWeights       │     audit_log 기록                      │
│  │    c. 백테스트          │     evaluateRecommendationEngine        │
│  │    d. metric 비교       │     KEEP / DISCARD / ERROR              │
│  └────────┬────────────────┘                                         │
│           ▼                                                          │
│  ┌─────────────────────────┐                                         │
│  │ 3. 최고 후보 승격       │ ──→ strategy_version 신규               │
│  │    (있을 때만)          │     은 챔피언 demote                    │
│  │                         │     가중치 스냅샷 저장                  │
│  └─────────────────────────┘                                         │
└─────────────────────────────────────────────────────────────────────┘
            │
            ▼ (08:30 KRX 프리오픈, 22:00 US 프리오픈)
┌─────────────────────────────────────────────────────────────────────┐
│  추천 생성: modelVersion = 챔피언 semver                              │
│  UniverseFeatureBuilder: 챔피언 가중치로 점수 계산                    │
└─────────────────────────────────────────────────────────────────────┘
            │
            ▼ (장중 ExitMonitor 자동 청산)
┌─────────────────────────────────────────────────────────────────────┐
│  evaluation 자동 생성 → 다음 사이클의 confidence/rollback 입력으로 사용 │
└─────────────────────────────────────────────────────────────────────┘
```

→ **"가중치 → 백테스트 → 챔피언 → 추천 → 평가 → 다음 백테스트"** 사이클이 자동으로 돈다.

---

## 13. API 카탈로그

전체 컨트롤러와 엔드포인트 목록. Swagger UI: `http://localhost:8083/swagger-ui.html`

### 13.1 추천 / 평가 / 예측

| Method | Path | 컨트롤러 / 서비스 |
|---|---|---|
| GET | `/api/recommendations?status&ticker` | RecommendationController.getRecommendations |
| GET | `/api/recommendations/{id}` | getRecommendation |
| POST | `/api/recommendations` | createRecommendation (term=SHORT/LONG, ticker 등록 검증) |
| PUT | `/api/recommendations/{id}/status` | updateStatus (OPEN/CLOSED/EXPIRED) |
| ~~POST~~ | ~~`/api/recommendations/{id}/exit-confirm`~~ | ~~제거됨 (2026-05-26)~~ |
| GET/POST | `/api/evaluations` | EvaluationController (exit_reason: TARGET_HIT/STOP_HIT/TIME_OUT/MANUAL_CLOSE) |
| GET/POST | `/api/predictions` | PredictionController |

### 13.2 종목 / 시장 후보군 / Feature

| Method | Path | 비고 |
|---|---|---|
| GET/POST/PUT | `/api/instruments` | 수동 종목 CRUD (`ticker` PK) |
| GET | `/api/universe?market&tradable&minMarketCap&minAvgTurnover&minLastPrice` | market_universe 조회+필터 |
| POST | `/api/universe/sync/kr-symbols?market=KOSPI|KOSDAQ|ALL` | KRX KIND 심볼 동기화 |
| POST | `/api/universe/sync/us-symbols?market=NASDAQ|NYSE|ALL` | NASDAQ Trader 심볼 동기화 |
| POST | `/api/universe/sync/us-prices?market&limit` | Stooq 시세 동기화 |
| GET | `/api/features/universe?market&limit` | UniverseFeatureBuilder 결과 조회 (15개 sub-score 포함 JSON) |

### 13.3 시장 데이터 (가격/뉴스/공시/매크로/펀더)

| Method | Path | 비고 |
|---|---|---|
| GET | `/api/market-data/daily-prices?market&ticker&limit` | price_daily 조회 |
| GET | `/api/market-data/intraday-prices?market&ticker&limit` | price_intraday 조회 |
| POST | `/api/market-data/daily-prices/sync?market&limit&days` | 시장 후보군 일봉 동기화 |
| GET/POST | `/api/market-data/news` `/news/sync` | RSS 뉴스 |
| GET/POST | `/api/market-data/disclosures` `/disclosures/sync` | DART/SEC 공시 |
| GET/POST | `/api/market-data/macro-observations` `/macro-observations/sync` | FRED 매크로 |
| GET/POST | `/api/market-data/fundamentals` `/fundamentals/sync` | SEC 펀더 (POST는 ticker 필수) |

### 13.4 백테스트 / 통계 / 브리프 / Codex / Autoresearch

| Method | Path | 비고 |
|---|---|---|
| GET/POST | `/api/backtests` | 결과 저장만 |
| POST | `/api/backtests/simulate` | 룰 기반 MA20 시뮬레이션 |
| GET | `/api/stats/summary` `/daily` `/by-strategy` | 통계 |
| GET/POST | `/api/briefs` `/{id}` | 데일리 브리프 CRUD |
| POST | `/api/dev/brief/generate?marketTrack&prompt` | Codex로 브리프 생성 |
| GET/POST | `/api/codex/calls` | Codex 호출 감사 로그 |
| GET/POST | `/api/autoresearch/runs` `/strategy-versions` | AutoResearch 이력 |
| POST | `/api/autoresearch/auto-run` | AutoResearch 즉시 1회 실행 (수동) |
| POST | `/api/backtests/simulate` (strategy=`recommendation-engine-v1`) | 추천 엔진 백테스트 시뮬레이션 |
| GET/POST | `/api/notifications/logs` | 알림 로그 |

### 13.5 운영 / 관리자 / 개발용

| Method | Path | 보호 | 비고 |
|---|---|---|---|
| GET | `/api/admin/settings` | BasicAuth | 전체 설정 조회 |
| GET | `/api/admin/settings/{key}` | BasicAuth | |
| PUT | `/api/admin/settings/{key}` | BasicAuth | 값 변경 + audit_log 기록 |
| POST | `/api/admin/settings/reset` | BasicAuth | 기본 설정 40여개 일괄 적재 |
| GET | `/api/admin/audit-logs` | BasicAuth | 최근 50건 |
| GET | `/api/ops/external-health` | BasicAuth | KIS/Telegram/Codex/DART/SEC + Codex 일 호출/예산 한도 상태 |
| POST | `/api/dev/universe/seed?market` | (옵션) | 개발용 11개 종목 seed |
| POST | `/api/dev/recommendations/generate?market&shortCount&longCount` | (옵션) | 추천 즉시 생성 |
| POST | `/api/dev/notifications/test?message` | (옵션) | 텔레그램 발송 테스트 |
| POST | `/api/dev/brief/generate?marketTrack&prompt` | (옵션) | 브리프 즉시 생성 |
| GET | `/actuator/health` `/info` `/metrics` | BasicAuth | Spring Boot Actuator |

→ `/api/dev/**`는 기본적으로 공개. `stock-advisor.security.protect-dev-api=true`로 바꾸면 BasicAuth 보호.

---

## 14. 보안 / 운영 / Codex 한도

### 14.1 Spring Security 설정 (`config/SecurityConfig.java`)

- **CSRF 비활성** (REST API)
- **CORS**: 기본 허용 origin `http://localhost:5173`, `http://127.0.0.1:5173` (Vite dev 서버). `AppSecurityProperties.allowedOrigins`로 변경.
- **인증 정책**:
  - `OPTIONS /**` 모두 허용 (preflight)
  - `/api/admin/**`, `/api/ops/**`, `/actuator/**` → 인증 필요
  - `/api/dev/**` → `protectDevApi=true` (**2026-05-22 기본값 true로 변경**)
  - 나머지 → 모두 허용
- **인증 방식**: HTTP Basic
- **계정**: `AdminProperties` 환경변수로 인메모리 1개 (`ADMIN_USERNAME`/`ADMIN_PASSWORD`, 기본 `admin/change-me`)
  - **BCrypt 적용 (F-5)**: `BCryptPasswordEncoder` bean으로 plain → hash 인코딩. `{noop}` 평문 모드 제거.
  - 운영 시 권장: 사전 hash 생성 후 환경변수로 직접 주입 → 기동 비용 절감.

### 14.2 외부 API 키 관리

모든 외부 키는 환경변수로 주입. 값이 `dev-placeholder`면 개발 모드로 강제 분기:

| 환경변수 | 기본값 | 사용 클래스 |
|---|---|---|
| `KIS_APP_KEY` | dev-placeholder | KisApiClient, KisTokenStore |
| `KIS_APP_SECRET` | dev-placeholder | KisTokenStore |
| `TELEGRAM_BOT_TOKEN` | dev-placeholder | TelegramClient |
| `TELEGRAM_CHAT_ID` | dev-placeholder | TelegramClient |
| `DART_API_KEY` | dev-placeholder | DisclosureClient (KR) |
| `SEC_USER_AGENT` | `StockAdvisor/1.0 contact@example.com` | DisclosureClient, SecFundamentalClient |
| `CODEX_COMMAND` | dev-placeholder | CodexClient |
| `ADMIN_USERNAME` | admin | SecurityConfig |
| `ADMIN_PASSWORD` | change-me | SecurityConfig |

`MarketUtil.isDevPlaceholder(value)` 한 함수로 통일된 체크.

### 14.3 Codex 사용량 제어

`CodexClient.call()` 직전 3중 한도:

1. **일 호출 한도** (`codex.daily.callLimit`, 기본 200)
   - 오늘 0시~23:59:59 (Asia/Seoul) 사이의 `codex_call` 카운트가 한도 이상이면 fallback 반환 + 실패 로그
2. **일 예산 USD** (`codex.daily.budgetUsd`, 기본 0=비활성)
   - 추정비용 = `(누적 텍스트 길이 + 이번 prompt + 응답 예상) / 1000 × codex.estimatedUsdPer1kChars` (기본 $0.002)
   - 한도 초과 시 fallback
3. **타임아웃 120초**

각 호출은 `codex_call` 테이블에 `prompt_hash(SHA-256)`, `prompt_len`, `response_len`, `duration_ms`, `succeeded`로 기록.
`/api/ops/external-health` 가 한도 도달 여부를 함께 표시.

### 14.4 알림 중복 방지

`NotificationLogService.sendTelegramOnce(dedupeKey, message)`:
- `SHA-256(dedupeKey)` 를 `notification_log.payload_hash`로 사용
- `(channel="TELEGRAM", payload_hash, status="SENT")` 인덱스로 조회 → 이미 있으면 발송 스킵
- ExitMonitor에서 dedupe 키 예: `"exit-monitor:42:TARGET:1.05"` — entry 대비 1% 단위 priceBucket 사용 (F-11). 같은 가격 구간에선 중복 알림 없음.

### 14.5 감사 로그

`AdminSettingService.updateSetting`/`resetSettings`는 `audit_log` 테이블에 actor, action, before_json, after_json을 기록.
**AutoResearch 변경도 통합 (2026-05-22)**: `AutoresearchService.saveWeights/saveStrategyWeights`가 `AdminSettingService` 우회해서 직접 `app_setting` 저장하지만, `audit_log`에는 `actor="autoresearch"`로 명시적 기록 → 어떤 가중치/스냅샷이 언제 바뀌었는지 추적 가능. 챔피언 롤백 시 `ROLLBACK_CHAMPION` 액션도 기록.

---

## 15. 개발 모드 vs 실운영 모드

이 백엔드의 거의 모든 외부 의존성은 **placeholder 패턴**으로 둘 다 돌릴 수 있게 설계돼 있다.

| 컴포넌트 | dev-placeholder | 실운영 |
|---|---|---|
| KIS | API 호출 스킵, 빈 결과. ExitMonitorJob에서 현재가 못 받으니 자동 Confirm 안 돔 | OAuth 토큰 발급 → 실제 현재가/일봉 가져옴 |
| Telegram | stdout 로그만 출력 (`[Telegram 개발 모드] 메시지 출력: ...`) | 실제 봇으로 발송 |
| Codex CLI | "[stock-advisor 개발 모드 브리프]" 템플릿 fallback | ProcessBuilder로 CLI 실행 |
| DART | 호출 스킵, 빈 공시 목록 | 실제 DART list.json 호출 |
| KRX KIND | placeholder 체크 없음. **항상 실제 호출** | 동일 |
| NASDAQ Trader | placeholder 체크 없음. **항상 실제 호출** | 동일 |
| Stooq | placeholder 체크 없음. **항상 실제 호출** | 동일 |
| RSS (Google/Yahoo) | placeholder 체크 없음. **항상 실제 호출** | 동일 |
| FRED | placeholder 체크 없음. **항상 실제 호출** | 동일 |
| SEC | User-Agent 기본값 fallback 후 실제 호출 | 동일 (User-Agent만 잘 채우면) |

→ **공개 데이터(KRX/NASDAQ Trader/Stooq/RSS/FRED/SEC)는 키 없이도 잘 돌고**, 유료/계정 필요한 KIS, Telegram, Codex, DART만 환경변수 채워야 풀 기능이 켜진다.

스케줄러는 placeholder 모드에서도 동작한다 (모든 동기화 단계가 빈 결과로 끝나도 텔레그램 fallback 메시지가 나간다 → stdout 로그).

---

## 16. 로컬 실행 방법

### 16.1 사전 준비

1. **MSSQL** 로컬 인스턴스
   ```
   Server: localhost:1433
   DB: stock_advisor
   User: park / Pass: 12345
   ```
   `application-local.yml` 에 맞춰 미리 DB만 만들어두면 Flyway가 스키마 자동 생성.

2. **Java 21 JDK**

### 16.2 빌드 / 실행

```powershell
cd C:\Users\dongki\project\stock_codex\apps\backend
.\gradlew bootRun
```

기본 포트 8083. Swagger: `http://localhost:8083/swagger-ui.html`.

(첫 실행 후) 기본 설정 적재:
```powershell
curl -u admin:change-me -X POST http://localhost:8083/api/admin/settings/reset
```

### 16.3 개발 모드로 빠르게 추천 한 번 굴려보기

```powershell
# 1) 개발용 후보군 seed (11종목)
curl -X POST "http://localhost:8083/api/dev/universe/seed"

# 2) 추천 자동 생성
curl -X POST "http://localhost:8083/api/dev/recommendations/generate?market=KOSPI&shortCount=3&longCount=3"

# 3) 결과 조회
curl "http://localhost:8083/api/recommendations?status=OPEN"

# 4) feature 점수 자세히 보기
curl "http://localhost:8083/api/features/universe?market=KOSPI&limit=5"
```

### 16.4 실제 데이터로 굴려보기 (공개 API만)

```powershell
# 1) 한국 상장 종목 동기화 (KRX KIND)
curl -X POST "http://localhost:8083/api/universe/sync/kr-symbols?market=KOSPI"

# 2) 미국 상장 종목 동기화 (NASDAQ Trader)
curl -X POST "http://localhost:8083/api/universe/sync/us-symbols?market=ALL"

# 3) 미국 시세 동기화 (Stooq)
curl -X POST "http://localhost:8083/api/universe/sync/us-prices?market=NASDAQ&limit=50"

# 4) 일봉 동기화 (KR은 KIS 키 필요, US는 Yahoo로 가능)
curl -X POST "http://localhost:8083/api/market-data/daily-prices/sync?market=NASDAQ&limit=30&days=180"

# 5) 뉴스 / 공시 / 매크로 / 펀더 수집
curl -X POST "http://localhost:8083/api/market-data/news/sync?market=NASDAQ&ticker=AAPL"
curl -X POST "http://localhost:8083/api/market-data/disclosures/sync?market=NASDAQ&limit=10"
curl -X POST "http://localhost:8083/api/market-data/macro-observations/sync"
curl -X POST "http://localhost:8083/api/market-data/fundamentals/sync?market=NASDAQ&ticker=AAPL"

# 6) 추천 생성
curl -X POST "http://localhost:8083/api/dev/recommendations/generate?market=NASDAQ&shortCount=3&longCount=3"
```

### 16.5 풀 운영 환경

`.env` 또는 OS 환경변수에 다음 채우고 재기동:
```
KIS_APP_KEY=...
KIS_APP_SECRET=...
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
CODEX_COMMAND=codex            # PATH에 있는 Codex CLI 실행 파일
DART_API_KEY=...
SEC_USER_AGENT=YourName/1.0 you@example.com
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<강한비밀번호>   # BCrypt로 자동 해시. 사전 hash 주입 시 기동 비용 절감
```

운영 시 활성화 권장 설정 (`/api/admin/settings/{key}` PUT):
```
recommendation.regime.filter.enabled  → true (일봉 200개 쌓인 후)
recommendation.sector.max             → 2 (섹터 분산 캡)
exit.polling.intervalMinutes          → 5 (운영 안정화 후 3 또는 1로 단축)
autoresearch.enabled                  → true (1주 가동 후 첫 promotion 확인)
codex.daily.callLimit                 → 50 (룰 우선화 후 충분)
```

스케줄러는 별도 트리거 없이 켜놓기만 하면 알아서 돈다 (AutoResearchJob 포함).

---

## 17. 한 줄 요약

> **"공개 데이터(KRX/NASDAQ Trader/Stooq/RSS/DART/SEC/FRED)와 KIS 시세를 매일 모아 종목별 0~100점 feature를 계산하고 — 매주 야간에 가중치를 자동 진화시켜 — 상위 N개를 단기/장기 추천으로 만들어 텔레그램으로 알려준 뒤, 장중에 룰 기반 자동 평가/청산 + 손절 위험 시 Codex 보조 판단을 하는 Spring Boot 백엔드."**

추천의 근거는 "ML이나 LLM의 직관"이 아니라 **15개 룰 기반 점수의 가중합** 이며, 그 가중치는 **AutoResearch가 매주 백테스트로 진화**시킨다. confidence는 실데이터 hit rate로 계산. 거래비용/슬리피지 반영. 약세장 차단 + 섹터 분산. 결과가 항상 결정적이고 추적 가능.

Codex/LLM은 **보조 판단자** 역할만 한다 — 데일리 브리프 작성, **모호한** 손절 위험 시만 의견 묻기. 외부 LLM이 죽어도 룰 기반 fallback이 항상 결과를 만든다. Codex 비용 발생 빈도는 룰 우선화로 50% 이상 절감.

### 17.1 닫힌 학습 루프

```
가중치 (app_setting)
  ↓ 매일 추천 생성에 사용
추천 (recommendation)
  ↓ 장중 ExitMonitor 자동 청산
평가 (evaluation)
  ↓ 통계 / confidence / 챔피언 라이브 검증
  ↓ 매주 02:20 AutoResearchJob
가중치 변형 → 백테스트 → 챔피언 승격/롤백
  ↓ (다음 사이클의 가중치)
```

→ **공개 데이터 잘 받아오고 가짜 confidence로 손실 확정** → **closed-loop 학습 + 비용 통제된 보조 LLM 시스템** 으로 진화 완료.

### 17.2 자동매매 활성화 전 체크리스트

1. evaluation 30건 이상 누적 (RecommendationConfidenceService 학습 시작 시점)
2. AutoResearchJob 1~2주 가동 → 첫 promotion 확인 (`/api/autoresearch/strategy-versions?champion=true`)
3. `recommendation.regime.filter.enabled=true` 토글 (인덱스 일봉 200개 쌓인 후)
4. `pricingMethod="unavailable"` 비율 0 유지 확인 (`/api/recommendations` 통계)
5. ExitMonitor 자동 청산 동작 확인 (`evaluation` 테이블 TARGET_HIT/STOP_HIT/TIME_OUT 건수)
6. Codex 일 호출 50건 미만 유지 확인 (`/api/ops/external-health`)

7. 그 후에야 소액 자동매매 검토.

