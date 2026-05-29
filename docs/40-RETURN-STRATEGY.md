# 백엔드 수익률 개선 작업지시서

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 40(수익전략) · 상태 🟢 신규 작업지시 · 기존 결함이력은 [41-DEFECTS-AND-FIXES.md](41-DEFECTS-AND-FIXES.md)
>
> **대상**: 이 문서를 읽는 AI/개발자가 현재 백엔드에 **바로 적용** 가능하도록 작성.
> **코드 기준**: `apps/backend` · Spring Boot 3.3 · Java 21 · MSSQL · Flyway(최신 V8)
> **읽는 법**: 각 TASK는 `대상 → 현황 → 문제 → 변경 → 검증` 순. 위에서부터 우선순위(P0→P3).
> 모든 가중치/임계값은 `app_setting` 키-값으로 런타임 조정. 신규 기본값은 Flyway 마이그레이션 또는 `PUT /api/admin/settings/{key}`로 주입.

---

## 적용 순서 (의존성)

```
P0  TASK-1  백테스트 진입을 score 기반으로 (루프 유효화)
P0  TASK-2  Point-in-time 피처 스냅샷 + forward return (lookahead 제거)  ← 1과 함께 토대
P1  TASK-3  펀더멘털/매크로를 "존재 여부"→"방향성 신호"로
P1  TASK-4  뉴스 점수: 감성 주축 / 빈도 보조
P2  TASK-5  cross-sectional 표준화(z-score/백분위)
P2  TASK-6  IC(정보계수) 측정 파이프라인 + 가중치 가이드
P2  TASK-7  확신도·역변동성 포지션 사이징
P3  TASK-8  regime 필터 기본 on + 지수 일봉 적재
```

> TASK-8/3/4는 1·2와 **독립**이라 병렬 가능. TASK-5·6·7은 TASK-2(스냅샷) 완료 후 효과 검증 가능.

---

## ⛔ 회귀 방지 (절대 하지 말 것)

1. **백테스트 종목 선별에 현재 시점 피처를 쓰지 말 것.** `RecommendationEngine.selectTopCandidates`는 오늘자 뉴스/공시/펀더멘털/가격으로 점수 매김(`UniverseFeatureBuilder.buildFeature`). 과거 구간 시뮬에 그대로 넣으면 미래참조·생존편향. TASK-1/2의 핵심.
2. **점수 함수가 "데이터 개수"를 점수로 환산하지 말 것.** 방향성(싸다/비싸다, 좋다/나쁘다)만 점수화.
3. 가중치 정규화(`AutoresearchService.normalizeWeightGroup`) 깨지 말 것 — 그룹 합=1 유지.

---

## TASK-1 (P0) — 백테스트 진입을 score 기반으로

**대상**: `application/backtest/BacktestRunService.java` (`simulateTicker:140`, `evaluateStrategy:99`)

**현황**: `simulateTicker`는 MA20 돌파(`row.getClosePrice() >= ma20`)로만 진입. scoring weights를 전혀 안 씀. AutoResearch가 가중치를 바꿔도 진입 규칙은 불변 → `avgPnlPct` 지표가 가중치 변화에 반응 안 함.

**문제**: AutoResearch 루프(`AutoresearchService.runAutoResearch`)가 최적화하는 대상(가중치)과 백테스트 진입 신호가 **분리**됨. 챔피언 승격이 노이즈 기반.

**변경**:
1. `simulateTicker` 진입 조건을 **그 시점 종합 score ≥ 임계값**으로 교체. score는 TASK-2의 PIT 스냅샷에서 해당 `tradeDate` 값을 조회(스냅샷 없으면 그 시점 가격 윈도우로 재계산하되 미래봉 사용 금지).
2. 진입 임계값은 설정 키 `backtest.entry.minScore`(기본 60)로.
3. `evaluateStrategy`에서 `selectTopCandidates(market)`(현재 시점 선별) 호출 제거 → 과거 각 거래일에 점수 임계 통과한 종목을 그 날 진입.

**의존**: TASK-2 스냅샷 필요. 스냅샷 전까지는 임시로 "진입일 이전 60봉만으로 재계산한 score"를 써서 미래참조만 차단.

**검증**: 동일 기간·다른 가중치 2벌로 `evaluateRecommendationEngine` 실행 → `avgPnlPct`가 **달라져야** 함(현재는 거의 동일). 달라지면 루프 유효.

---

## TASK-2 (P0) — Point-in-time 피처 스냅샷 + forward return

**대상**: 신규 테이블 + `UniverseFeatureBuilder`, 신규 스케줄러

**현황**: 피처는 매 호출 시 최신 데이터로 즉석 계산. 과거 시점 피처가 저장 안 됨. `market_signal_score`(V5) 있으나 시점별 종합 score 이력 아님.

**문제**: 과거 시점의 "그날 알 수 있던 정보"만으로 백테스트/학습 불가 → 모든 최적화가 미래참조.

**변경**:
1. **신규 마이그레이션 `V9__feature_snapshot.sql`**:
```sql
CREATE TABLE feature_snapshot (
    snapshot_key   VARCHAR(80)  NOT NULL PRIMARY KEY, -- {market}:{ticker}:{yyyymmdd}
    market         VARCHAR(20)  NOT NULL,
    ticker         VARCHAR(20)  NOT NULL,
    as_of_date     DATE         NOT NULL,
    total_score    INT          NOT NULL,
    feature_json   NVARCHAR(MAX) NOT NULL,  -- UniverseFeatureBuilder.buildFeatureJson 결과
    fwd_ret_5d     DECIMAL(12,6) NULL,      -- 진입+5거래일 수익률(%) (사후 채움)
    fwd_ret_20d    DECIMAL(12,6) NULL,
    created_at     DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);
CREATE INDEX ix_feature_snapshot_date ON feature_snapshot (as_of_date, market);
CREATE INDEX ix_feature_snapshot_ticker ON feature_snapshot (market, ticker, as_of_date);
```
2. `UniverseFeatureBuilder`에 `buildFeatureAsOf(entity, LocalDate asOf)` 추가 — 가격/뉴스/공시/펀더멘털 조회를 **`asOf` 이하**로만 제한(현재 `OrderBy...Desc` 쿼리에 `...AndTradeDateLessThanEqual` / `...AndPublishedAtLessThanEqual` 변형 추가).
3. 신규 스케줄러 `FeatureSnapshotJob`(매 장마감 후): 유니버스 전 종목 `buildFeatureAsOf(today)` 저장.
4. forward return 백필: T+5/T+20 거래일 종가로 `fwd_ret_*` 채우는 잡(`price_daily` 사용).

**검증**: 임의 과거 날짜 스냅샷의 `feature_json`을 그날 기준 수동 계산과 대조 → 미래 데이터 미포함 확인. `fwd_ret_5d`가 채워지는지 확인.

---

## TASK-3 (P1) — 펀더멘털/매크로를 방향성 신호로

**대상**: `UniverseFeatureBuilder.scoreFundamentals:166`, `scoreMacro:152`

**현황**:
```java
// scoreFundamentals: 지표 "개수"만 셈
long metricCount = fundamentals.stream().map(FundamentalMetricEntity::getMetricName).distinct().count();
if (metricCount >= 6) return 84;  // PER가 100이든 5든 동일 84점
```
`scoreMacro`도 `seriesId` distinct 개수만.

**문제**: 방향성 알파 0. 커버리지 좋은 대형주에 점수 쏠림. 펀더멘털 가중치 올려도 수익률 기여 없음.

**변경** (`FundamentalMetricEntity.metricName` / `metricValue` 사용. 리포: `findByMarketAndTickerOrderByPeriodEndDesc`):
1. metricName별 최신값 추출 후 **방향성 점수화**:
   - `PER` 낮을수록 ↑ (예: <10 → 90, 10~20 → 70, 20~40 → 55, >40 → 40, 음수/적자 → 35)
   - `PBR` 낮을수록 ↑
   - `ROE` 높을수록 ↑
   - `revenueGrowthYoY`/`epsGrowthYoY` 양수·클수록 ↑ (DART YoY는 진행 중 `DartFundamentalClient`)
2. 가용 지표만 가중 평균. **데이터 없으면 50(중립)**, 개수로 가점 금지.
3. `scoreMacro`: series 개수 대신 **regime 방향**. 예) 10Y-2Y 스프레드/실업률/하이일드 스프레드 추세로 risk-on/off 점수.

**검증**: 저PER·고ROE 종목이 고PER·적자 종목보다 `fundamentalScore` 높아야 함. 동일 커버리지면 값 방향으로만 차이.

---

## TASK-4 (P1) — 뉴스 점수: 감성 주축 / 빈도 보조

**대상**: `UniverseFeatureBuilder.scoreNews:103`, `applySentimentAdjustment:147` · `infrastructure/marketdata/news/SentimentAnalysisClient.java`(신규, 진행 중)

**현황**: 뉴스 **개수**로 45→88 결정, 감성은 ±15 보정뿐. 악재 도배 종목도 88-15=73점.

**문제**: 폴라리티 < 노출량. 부정 뉴스 많은 종목 못 거름.

**변경**:
1. 점수 = **감성 기반 베이스**(평균 감성 -1~+1 → 30~90 선형 매핑)로 전환. 빈도는 신뢰도 보정(뉴스 0~1건이면 중립 50으로 수축).
2. 감성은 `SentimentAnalysisClient.analyze()`(LLM/모델) 연속값 우선. 실패/비활성 시 기존 `MarketSignalScorer.scoreNewsSentiment` 키워드 폴백.
3. `news_article.sentiment_score`에 연속값 적재되도록 수집 파이프라인 연결.

**검증**: 평균 감성 음수 종목 score < 50. 긍정 도배 < 소수 강한 호재일 때 강호재가 더 높게.

---

## TASK-5 (P2) — cross-sectional 표준화

**대상**: `UniverseFeatureBuilder.buildFeatures:55` (유니버스 일괄 계산 지점)

**현황**: 각 종목 점수가 절대 계단 상수(92/78/62/45). 종목 간 독립.

**문제**: 대부분 종목이 비슷한 버킷으로 뭉침 → top-N 선별 차별력 약함, 옵티마이저가 미세차 학습 불가.

**변경**:
1. `buildFeatures`는 이미 유니버스 전체를 한 번에 처리 → 각 raw 피처(모멘텀/거래량 z/RSI 등)를 **유니버스 내 백분위 또는 z-score**로 변환 후 가중합.
2. 계단 상수 함수는 raw 피처 산출용으로만 두고, 최종 점수는 cross-sectional 순위 기반.
3. 연속값 유지(반올림으로 정보 손실 금지).

**검증**: 같은 시장 내 점수 분포가 넓어짐(표준편차 ↑). top-N이 명확히 상위 분위로 구성.

---

## TASK-6 (P2) — IC(정보계수) 측정 + 가중치 가이드

**대상**: 신규 `application/research/FeatureICService.java` · `AutoresearchService.mutateWeights:392`

**현황**: `mutateWeights`는 경로를 순환하며 ±10% 랜덤 변형(`factor 0.90/1.10`). 어떤 피처가 실제로 forward return과 상관 있는지 모름.

**변경**:
1. `feature_snapshot`(TASK-2)에서 피처별 값 vs `fwd_ret_5d/20d` **순위상관(Spearman IC)** 계산.
2. IC≈0 피처 가중치 축소, IC 높은 피처 가중치 확대 방향으로 mutation을 가이드(랜덤 대신).
3. IC를 `autoresearch_run.diff_summary` 또는 신규 메트릭에 기록.

**검증**: IC 높은 피처의 가중치가 세대 진행에 따라 증가. 백테스트 `avgPnlPct` 개선 속도가 랜덤 변형보다 빠름.

---

## TASK-7 (P2) — 확신도·역변동성 포지션 사이징

**대상**: `application/recommendation/PricePredictor.java` · `RecommendationEntity`(비중 필드 추가 검토)

**현황**: 목표/손절 배수 고정(R:R≈1.4:1). 비중 개념 없음(동일가중 암묵). 분산은 `recommendation.sector.max=2`가 전부.

**변경**:
1. 추천에 `weightPct`(포트폴리오 비중) 산출 추가: `score(또는 IC 기대수익)` × `1/변동성`(이미 계산되는 `calculateVolatilityPct` 재사용) 정규화.
2. 총합 비중 캡(예 100%), 종목당 상한(예 20%), 섹터 상한 유지.
3. 손절을 ATR/변동성 배수로(이미 부분 적용) + R-multiple 목표를 기대 edge에 연동.

**검증**: 고확신·저변동 종목이 더 큰 비중. 백테스트에서 동일 hit-rate 대비 `avgPnlPct`/Sharpe 개선.

---

## TASK-8 (P3) — regime 필터 기본 on + 지수 일봉 적재

**대상**: `RecommendationEngine.isMarketRegimeOk:85`, `indexTickerForMarket:105` · 시세 수집 파이프라인

**현황**: 200일선 게이트 존재하나 `recommendation.regime.filter.enabled` 기본 false. 또 `price_daily`에 지수 행 필요:
- KOSPI/KOSDAQ market → `market=KOSPI, ticker=KOSPI`
- US market → `market=<해당>, ticker=SPY`
미적재면 `history.size() < 200`이라 항상 통과(무효).

**변경**:
1. 시세 동기화에 지수 일봉 적재 추가(Stooq: `^KS11`(KOSPI), `SPY`). `price_daily`에 위 키로 저장.
2. 기본값 `recommendation.regime.filter.enabled=true` 주입(V9 또는 admin).

**검증**: 지수 200일선 하회 구간에 `buildCandidates`가 빈 리스트 반환. 백테스트에서 하락장 진입 차단 → maxDrawdown 축소.

---

## 신규 설정 키 요약

| 키 | 기본값 | 용도 | TASK |
|---|---|---|---|
| `backtest.entry.minScore` | 60 | 백테스트 진입 score 임계 | 1 |
| `recommendation.regime.filter.enabled` | **true** | 200일선 regime 게이트 | 8 |
| (기존) `recommendation.scoring.weights` | — | 종합/기술/컨텍스트 가중치 JSON | 3·4·5·6 |
| (기존) `recommendation.sector.max` | 2 | 섹터당 최대 종목 | 7 |
| (기존) `autoresearch.targetIterations` | 8 | 루프 반복 | 1·6 |

> `recommendation.scoring.weights` JSON 형태:
> ```json
> {"value":{"liquidity":0.20,"price":0.10,"technical":0.30,"context":0.15,"fundamental":0.10,"dataQuality":0.15},
>  "technical":{"ma":0.30,"rsi":0.25,"volume":0.20,"macd":0.15,"bollinger":0.10},
>  "context":{"news":0.40,"disclosure":0.18,"macro":0.25,"fundamental":0.17}}
> ```

---

## 한 줄 요약

루프 토대(TASK-1·2)부터. 그전엔 어떤 가중치 튜닝도 노이즈 최적화. 토대 후 TASK-3·4·8로 알파·drawdown 즉시 개선, TASK-5·6·7로 정교화.


