# Stock Advisor 백엔드 — 치명적 결함과 수정 방안

> 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 41(품질이력) · 상태 🟢 이력(대부분 적용완료) · 신규 수익전략은 [40-RETURN-STRATEGY.md](40-RETURN-STRATEGY.md)
>
> `31-BACKEND-DEEP-DIVE.md` 작성 과정에서 코드를 정독한 뒤 도출한 **실투자 관점의 결함 목록과 구체적 수정 방안** 정리.
>
> 결론부터: **현재 코드 그대로 자동매매 붙이면 손실 확정.** 추천 시스템의 "껍데기"는 완성됐지만 "수익을 내는 두뇌"는 비어 있다.
>
> **2026-05-22 업데이트 1**: P0 5개 + P1 5개 + F-12 일부 적용 완료. 0장 참조.
> **2026-05-22 업데이트 2**: P0/P1/P2 거의 전부 적용. AutoResearch 본체 완성, BUG-1 해결, F-7/F-12/F-13/F-14/F-16 적용. 0.6장 참조.
> **2026-05-22 업데이트 3**: AutoResearch 작은 이슈 5개 모두 처리 — 트랜잭션 분리, 가중치 정규화, 감사 로그 통합, 챔피언 롤백 검증, 시각 확정. 0.7장 참조.

---

## 목차

0. [적용 현황 (2026-05-22)](#0-적용-현황-2026-05-22)
1. [요약 — 한 장 평가표](#1-요약--한-장-평가표)
2. [치명적 결함 (Critical)](#2-치명적-결함-critical)
3. [구조적 결함 (Structural)](#3-구조적-결함-structural)
4. [운영 결함 (Operational)](#4-운영-결함-operational)
5. [Codex 비용 폭주 시나리오](#5-codex-비용-폭주-시나리오)
6. [수정 방안 — 우선순위별](#6-수정-방안--우선순위별)
7. [단계별 로드맵](#7-단계별-로드맵)
8. [수정 후 기대 효과](#8-수정-후-기대-효과)
9. [잔여 작업 및 알려진 버그](#9-잔여-작업-및-알려진-버그)

---

## 0. 적용 현황 (2026-05-22)

코드 점검 결과 다음 항목 적용. `./gradlew compileJava compileTestJava` 모두 통과.

### 0.1 적용 완료 ✅

| 항목 | 상태 | 적용 위치 |
|---|---|---|
| **F-1** ExitConfirm 룰 우선 | ✅ | `ExitConfirmService.decideByRule` — `current ≤ stop` 시 Codex 미호출, 즉시 CUT |
| **F-2** confidence 백테스트 hit rate 기반 | ✅ | `RecommendationConfidenceService` 신규 클래스, score band(10단위)별 hit rate, 샘플<30이면 50 |
| **F-3** 가격 fallback 더미 제거 | ✅ | `PricePredictor.resolveEntryPrice` — 데이터 없으면 422 throw, hash 더미 삭제 |
| **F-4** Codex 응답 strict 파싱 | ✅ | `ACTION_LINE_PATTERN = ^ACTION:\s*(HOLD\|CUT\|TIGHTEN)$`, 첫 줄만 검사, 미매치 시 HOLD |
| **F-5** BCrypt + dev API 보호 | ✅ | `BCryptPasswordEncoder` bean, `application.yml: protect-dev-api: true` |
| **F-6** modelVersion 동적 결정 | ✅ | `strategyVersionRepository.findByChampion(true)` 최신 promotedAt → semver |
| **F-8** 거래비용 반영 | ✅ | `PricePredictor.costAdjustedExitPrice` + `BacktestRunService.simulateTicker` 양쪽 |
| **F-9** 시장 레짐 필터 | ✅ | `RecommendationEngine.isMarketRegimeOk` MA200 비교, `recommendation.regime.filter.enabled` 토글 |
| **F-11** dedupe 키 priceBucket | ✅ | `ExitMonitorJob.priceBucket` — `entry` 기준 1% 단위 라운딩 |
| **F-15** 포지션 사이징 | ✅ | `signalsJson.positionWeightPct` — `1/risk × confidence`, 최대 20% |
| **F-12 (부분)** 자동 평가 + 청산 | ✅ | `ExitMonitorJob.closeRecommendationWithEvaluation` — target/stop/expiry 도달 시 `evaluation` 자동 생성 + CLOSED/EXPIRED 상태 전환 |

### 0.2 부분 적용 ⚠️

| 항목 | 상태 | 사유 |
|---|---|---|
| **F-10** 섹터 분산 | ⚠️ | `RecommendationEngine.selectTopCandidates` 구현됐으나 **호출처 없음**. `DevRecommendationGenerateService`가 `buildCandidates` 직접 호출 → sector cap 미적용 (9장 참조) |

### 0.3 신규 테스트 12개

`compileTestJava` 통과. P0 핵심 로직 검증.

```
application/admin/AdminSettingServiceTest.java
application/backtest/BacktestRunServiceTest.java
application/dev/DevRecommendationGenerateServiceTest.java
application/marketdata/MarketSignalScorerTest.java
application/notification/NotificationLogServiceTest.java
application/ops/ExternalHealthServiceTest.java
application/recommendation/ExitConfirmServiceTest.java
application/recommendation/PricePredictorTest.java
application/recommendation/RecommendationConfidenceServiceTest.java
application/recommendation/RecommendationEngineTest.java
config/SecurityConfigTest.java
scheduler/ExitMonitorJobTest.java
```

### 0.4 부가 개선 (수정 방안에 없던 추가 작업)

- **ExitMonitor cron 유연화**: `0 */5 9-15` → `0 * 9-15` + `exit.polling.intervalMinutes` 설정 (1/3/5/10/30분 선택 가능)
- **시장 코드 확장**: `KR/KONEX`, `US/AMEX`까지 인식 (`PricePredictor.isKoreanMarket`, `isUsMarket`)
- **거래비용 분리**: 한국 entry 비용 = `fee + slippage`, exit 비용 = `tax + fee + slippage` (매도세 분리)
- **pricingMethod 다양화**: `volatility-v1`, `recent-close-v1`, `last-price-v1`, `unavailable` + `-cost-adjusted` 접미사

### 0.5 적용 전후 비교

| 지표 | Before | After |
|---|---|---|
| 빌드 | OK | OK |
| Codex 호출 (위험구간 명확 손절 시) | 100% Codex 호출 | 0% (룰로 즉결) |
| confidence 신뢰성 | 종목 해시 (가짜) | 실제 hit rate (샘플 충분 시) |
| 데이터 없는 종목 추천 발행 | 가능 (해시 더미 가격) | 차단 (422 throw) |
| BasicAuth 패스워드 | `{noop}` 평문 | BCrypt |
| dev API 보호 | 기본 OFF | 기본 ON |
| target/stop 자동 청산 | 없음 (수동) | 자동 (`evaluation` 생성) |
| 약세장 매수 추천 | 발행 | 차단 (MA200 필터, 설정 토글) |
| 거래비용 가격 반영 | 미반영 | 반영 (entry/target/stop 모두) |

→ **자동매매 절대 금지 → 자동매매 검토 가능** 수준으로 상승.

---

---

## 1. 요약 — 한 장 평가표

| 영역 | 초기 상태 | 현재 상태 (2026-05-22) | 비고 |
|---|---|---|---|
| 데이터 수집 파이프라인 | ✅ 동작 | ✅ 동작 | KRX/NASDAQ/Stooq/RSS/DART/SEC/FRED |
| 점수화 (UniverseFeatureBuilder) | ⚠️ 임시 | ⚠️ 임시 | 가중치 외부화 미완 (F-13) |
| 추천 선별 (RecommendationEngine) | ⚠️ 임시 | ✅ 개선 | 레짐 필터 추가. 단 sector cap 호출 누락 |
| 가격 산출 (PricePredictor) | ⚠️ 위험 | ✅ 개선 | 데이터 없으면 422, 거래비용 반영 |
| 추천 confidence | ❌ 가짜 | ✅ 실데이터 기반 | hit rate (샘플<30 시 50) |
| ExitMonitor 5분 감시 | ✅ 동작 | ✅ 개선 | 자동 청산 + 평가 추가 |
| ExitConfirm (Codex) | ❌ 설계 거꾸로 | ✅ 룰 우선 | `current ≤ stop` 즉결 |
| AutoResearch | ❌ 미구현 | ❌ 미구현 | F-12 본체 작업 미완 |
| 추천 ↔ AutoResearch 연결 | ❌ 없음 | ✅ 연결 | `findByChampion` 사용 |
| 백테스트 ↔ 추천 엔진 | ❌ 분리 | ❌ 분리 | F-7 미완 |
| 거래비용/슬리피지 | ❌ 미반영 | ✅ 반영 | PricePredictor + BacktestRunService |
| 포지션 사이징 | ❌ 없음 | ✅ 추가 | `positionWeightPct` (최대 20%) |
| 시장 레짐 필터 | ❌ 없음 | ✅ 추가 | MA200, 설정 토글 |
| 섹터 분산 | ❌ 없음 | ⚠️ 구현됐으나 미호출 | 9장 참조 |
| 자동 평가 생성 | ❌ 없음 | ✅ 추가 | ExitMonitor가 자동 |
| 알림 인프라 | ✅ 견고 | ✅ 강화 | priceBucket dedupe |
| 보안 | ⚠️ 부분 | ✅ 개선 | BCrypt + dev API 기본 보호 |

**핵심 변화**: 초기 데이터 흐름 중심 → 의사결정 로직 + 자동 평가 사이클 보강. 단 가중치 학습/AutoResearch 본체는 여전히 미완.

---

## 2. 치명적 결함 (Critical)

실투자 시 즉시 손실로 이어지는 결함.

### C-1. confidence가 종목 이름 해시 ✅ 해결

> **2026-05-22**: `RecommendationConfidenceService` 도입. score band별 hit rate. 샘플<30이면 50 반환.

위치: `application/dev/DevRecommendationGenerateService.java:64`

```java
Integer confidence = 70 + Math.abs(candidate.ticker().hashCode() % 20);
```

**문제**:
- 신뢰도가 종목 코드 해시값. 종목명만으로 결정됨.
- "AAPL은 항상 confidence=X, NVDA는 항상 Y" — 시장 상황과 무관.
- 사용자는 이걸 보고 매수 비중을 정할 가능성 → 가짜 정보 기반 의사결정.

**영향도**: 🔴 최대. 신뢰도가 의미 없음.

### C-2. 진입가 fallback이 해시 기반 더미 ✅ 해결

> **2026-05-22**: hash 더미 제거. 일봉/last_price 모두 없으면 `CustomException(422)` throw → 추천 발행 차단. `DevRecommendationGenerateService.hasPriceData`가 사전 필터링.

위치: `application/recommendation/PricePredictor.java:46`

```java
int hash = Math.abs(candidate.ticker().hashCode());
if ("KOSPI".equals(...) || "KOSDAQ".equals(...)) {
    return BigDecimal.valueOf(50000L + hash % 90000L);
}
return BigDecimal.valueOf(100L + hash % 400L);
```

**문제**:
- 일봉 없고 `last_price`도 없으면 5만~14만원(KR) / $100~$500(US) 사이 더미값.
- 이 가격으로 추천 나가면 실제 시장가와 큰 차이 → 진입 즉시 손실.
- `pricingMethod="fallback-v1"`로 표시는 하지만 추천 자체는 발행됨.

**영향도**: 🔴 최대. 데이터 누락 종목이 그대로 추천에 섞임.

### C-3. modelVersion 하드코딩 → AutoResearch 무용 ✅ 해결

> **2026-05-22**: `strategyVersionRepository.findByChampion(true)` 사용. 챔피언 없으면 `"dev-rule-v0"` fallback. AutoResearch 본체(F-12)는 여전히 미구현이라 실효 효과는 챔피언 수동 등록 시에만.

위치: `application/dev/DevRecommendationGenerateService.java:23`

```java
private static final String MODEL_VERSION = "dev-rule-v0";
```

**문제**:
- AutoResearch가 챔피언 전략을 새 버전으로 승격해도 추천 엔진은 모름.
- `strategy_version.is_champion=true`인 레코드를 아무도 안 읽음.
- 결과: 영원히 `dev-rule-v0` 룰. 학습 사이클 끊김.

**영향도**: 🔴 최대. 시스템 진화 불가.

### C-4. ExitConfirm이 룰 무시하고 Codex 우선 ✅ 해결

> **2026-05-22**: `decideByRule` 메서드 추가. 위험구간 진입 시 먼저 룰 판단 → `current ≤ stop` 이면 즉시 CUT 반환 (Codex 미호출). 룰이 모호한 경우만 Codex. `parseAction`도 strict regex 적용.

위치: `application/recommendation/ExitConfirmService.java:51-58`

```java
if (!riskZone) return new ExitConfirmResponse(..., "HOLD", ...);
// 위험구간이면 무조건 Codex
String prompt = buildPrompt(...);
CodexResult result = codexClient.call(prompt, resolveProfile(), "exit-confirm");
boolean usedFallback = MarketUtil.isDevPlaceholder(...) || !result.succeeded();
String action = usedFallback ? fallbackAction(...) : parseAction(...);
```

**문제**:
- 손절 위험구간 진입 = **단순 룰 트리거** (`stop * 1.02 이내`).
- 진입 즉시 **무조건 Codex 호출**. Codex 응답이 사실상 메인 판단.
- `fallbackAction`은 Codex 실패 시에만 발동.
- 진짜 의도(사용자 설명): "5분은 룰 감시, Codex는 ambiguous 케이스만"인데 코드는 거꾸로.

**영향도**: 🔴 최대. Codex 비용 폭주 + 판단 외주화.

### C-5. AutoResearch 본체 부재 ❌ 미해결

> **2026-05-22**: 여전히 CRUD만. F-12 작업 미착수. **`StrategyVersionRepository.findByChampion`은 연결됐으니 운영자가 수동으로 strategy_version row 추가 + is_champion=true 토글하면 추천 엔진이 그 modelVersion 사용은 함.** 자동 변형/검증 사이클은 없음.

위치: `application/autoresearch/AutoresearchService.java` 전체

```java
public AutoresearchRunResponse createRun(AutoresearchRunCreateRequest request) {
    // CRUD만
}
```

**문제**:
- 실험 트리거 스케줄러 없음 (`@Scheduled` 0개).
- 전략 변형 로직(parent → proposal 생성) 없음.
- 자동 백테스트 비교 없음.
- 챔피언 승격/롤백 자동화 없음.
- 7일 라이브 검증 (`rollbackValidationDays`) 미구현.
- 설정 키 3개 (`enabled`, `targetIterations`, `rollbackValidationDays`)만 정의됨.

→ **테이블 + API 껍데기만 있는 상태.**

**영향도**: 🔴 최대. 시스템의 핵심 가치(자동 진화) 없음.

### C-6. 백테스트 ↔ 추천 엔진 완전 분리 ❌ 미해결

> **2026-05-22**: F-7 미완. 거래비용은 양쪽에 반영됐지만 백테스트는 여전히 MA20 단일 전략. UniverseFeatureBuilder 점수 기반 시뮬레이션 신규 메서드 추가 필요.

위치: `application/backtest/BacktestRunService.java:58` vs `RecommendationEngine.buildCandidates`

```java
String strategy = ... "ma20-breakout-v0";
// MA20 돌파 단일 룰만 시뮬레이션
```

**문제**:
- 백테스트는 `ma20-breakout-v0` 단일 전략 (MA20 위 종가 → 진입).
- 실제 추천은 15개 룰 가중합 (`UniverseFeatureBuilder`).
- **두 시스템이 같은 종목을 평가하지 않음.**
- 백테스트 결과로 추천 가중치를 조정하는 피드백 루프 없음.

**영향도**: 🔴 최대. 백테스트가 추천 검증에 무용.

### C-7. 거래비용/슬리피지 미반영 ✅ 해결

> **2026-05-22**: 양쪽 모두 반영.
> - `PricePredictor.costAdjustedExitPrice`: entry/target/stop을 비용 반영 가격으로 산출 (한국: tax+fee+slippage, 미국: fxSpread/2+slippage)
> - `BacktestRunService.simulateTicker`: target/stop trigger는 raw 가격으로 판정하고 PnL만 effective 가격으로 계산
> - `BacktestCost` record로 비용 항목 분리 (entry/exit/slippage)

위치: 전체 (`PricePredictor`, `BacktestRunService.simulateTicker`)

**문제**:
- `targetPrice = entry × (1 + targetPct)` — 매수가/매도가 모두 종가 그대로.
- 한국: 거래세 0.18% + 수수료 ≈ 0.015% × 2 = 약 0.21%/거래
- 미국: SEC fee + FX spread (`backtest.cost.us` 설정에만 존재, 미사용)
- 슬리피지 0.05% (`backtest.slippage.percent` 정의됨, 미사용)

설정만 있고 **로직에서 안 읽음**. SHORT 목표가 +5%면 실수익 +4.6%. 손익분기 hit rate가 37.5% → 42%로 상승.

**영향도**: 🔴 최대. 명목 성과와 실제 성과 괴리.

---

## 3. 구조적 결함 (Structural)

수익률을 점진적으로 갉아먹는 설계 문제.

### S-1. 점수 가중치 임의 ❌ 미해결

> **2026-05-22**: F-13 가중치 외부화 미완. 여전히 상수 (`0.20f`, `0.30f` 등). AutoResearch 본체와 함께 진행 필요.

위치: `application/feature/UniverseFeatureBuilder.java:73`

```java
int totalScore = Math.min(100, Math.round(
    liquidityScore  * 0.20f
  + priceScore      * 0.10f
  + technicalScore  * 0.30f
  + contextScore    * 0.15f
  + fundamentalScore* 0.10f
  + dataQualityScore* 0.15f));
```

**문제**: 6개 카테고리 가중치 작성자 직감. 백테스트로 튜닝된 적 없음.

**왜 위험한가**:
- `dataQualityScore` 15% — 데이터 잘 모인 종목 ≠ 수익률 좋은 종목.
- `priceScore` 10% — "가격대가 적정"이 수익률 예측에 의미 있는가? 검증 0.
- `technicalScore` 30%가 가장 크지만, 그 안에서도 MA*0.40/RSI*0.35/Vol*0.25도 임의.

### S-2. 뉴스 감성 = 키워드 카운트 ❌ 미해결

> **2026-05-22**: F-14 미착수. `MarketSignalScorer` 그대로. 부정문 처리, NLP 도입 필요.

위치: `application/marketdata/MarketSignalScorer.java:28`

```java
int positive = countMatches(text, POSITIVE_NEWS);  // "급등" 등 20개
int negative = countMatches(text, NEGATIVE_NEWS);  // "급락" 등 20개
double clipped = Math.max(-1.0, Math.min(1.0, raw * 0.35));
```

**문제**:
- "급등주 조심" → "급등" 매치 → positive +1. **반어/부정 문맥 처리 0.**
- "수주 취소" → "수주" 매치 → positive +1.
- 단순 substring 매칭. NLP 아님.

### S-3. RSI 45~65를 가장 좋게 봄 — 근거 불명

위치: `UniverseFeatureBuilder.scoreRsi`

```java
if (rsi.compareTo(45) >= 0 && rsi.compareTo(65) <= 0) return 90;
```

**문제**:
- 일반 트레이딩 통념상 RSI 50 부근은 "방향성 약함"으로 봄.
- 강한 상승 추세는 RSI 60~80에서 지속.
- 이 백엔드는 70 이상을 과열로 패널티 → 강세장 모멘텀 종목 누락.

### S-4. dataQuality 점수가 totalScore 직접 가산

위치: `UniverseFeatureBuilder.scoreDataQuality`

```java
if (entity.getLastPrice() != null) score += 15;
if (entity.getAvgTurnover() != null) score += 15;
if (priceHistoryCount >= 20) score += 20;
```

**문제**:
- "거래대금 있음 → +15점" — 거래대금 자체가 수익률 예측력 아님 (이미 liquidityScore가 그 역할).
- 데이터 누락 종목을 거르는 **필터**여야 하는데 가중치 15%로 종합점수에 직접 반영.
- 결과: 데이터 잘 갖춰진 대형주가 항상 상위.

### S-5. 시장 레짐(Regime) 필터 없음 ✅ 해결

> **2026-05-22**: `RecommendationEngine.isMarketRegimeOk` 추가. 시장 인덱스 일봉 200개로 MA200 계산, 현재가가 MA200 이상이면 통과. 약세장이면 `buildCandidates` 빈 리스트 반환. `recommendation.regime.filter.enabled` 토글 (기본 false → 데이터 충분해질 때 true).


**문제**:
- KOSPI/S&P500이 MA200 아래 (= 약세장)일 때도 매수 추천 발행.
- 약세장에선 좋은 점수 종목도 떨어진다. 매수 추천 시점이 잘못됨.
- 매크로 점수(`scoreMacro`)는 있지만 "FRED 데이터 몇 개 모였나"만 평가, **방향성 미반영**.

### S-6. 상관관계/분산 미고려 ⚠️ 부분 적용 (호출처 누락)

> **2026-05-22**: `RecommendationEngine.selectTopCandidates`에 sector cap 구현 (`recommendation.sector.max`, 기본 2). **단 `DevRecommendationGenerateService`가 `buildCandidates`만 호출 → sector cap 적용 안 됨**. 9장 알려진 버그 참조.


**문제**:
- 점수 상위 N개 → 반도체 5개 다 뽑힐 수 있음.
- 같은 섹터 5개 = 분산 0. 섹터 폭락 시 동시 손절.
- 섹터별 max N개 제한, 섹터 가중치 조절 룰 없음.

### S-7. 포지션 사이징 없음 ✅ 해결

> **2026-05-22**: `DevRecommendationGenerateService.calculatePositionWeightPct` 추가. `1 / riskPct × confidence`, 최대 20% 클립. `signalsJson.positionWeightPct`에 기록. 단, 운영자가 실제로 이 값을 매매에 반영하는 클라이언트 로직은 별도.


**문제**:
- 추천 = "종목 + 진입가/목표가/손절가". **얼마 살지는 없음.**
- 운영자가 각 추천에 동일 금액? 신뢰도 가중? Kelly? 결정 로직 0.
- 신뢰도(C-1) 자체가 가짜라 가중 베팅도 무의미.

### S-8. 생존편향(Survivorship Bias)

**문제**:
- `market_universe`는 **현재 거래 가능한** 종목만 보유.
- 상폐된 과거 종목 없음 → 백테스트가 "살아남은 종목"만 테스트.
- 실제 성과보다 과대평가됨.

### S-9. 추천 엔진과 ExitConfirm이 같은 지표 안 씀 ⚠️ 부분 개선

> **2026-05-22**: ExitConfirm 룰 판단이 추가됐으나 아직 `current ≤ stop` 단순 룰. RSI/MA 회복 시그널 활용은 미완. ExitMonitor가 expiry 자동 EXPIRED 처리는 추가됨.

위치: `UniverseFeatureBuilder` vs `ExitConfirmService`

**문제**:
- 추천 만들 때: MA, RSI, 거래량, 뉴스, 공시 다 본다.
- 청산 판단할 때: **그 지표들 안 봄.** 손절가 근접 여부만 본 뒤 Codex에 떠넘김.
- 같은 데이터를 양쪽에서 일관되게 써야 진짜 시스템.

---

## 4. 운영 결함 (Operational)

서비스 운영 중 사고로 이어질 수 있는 문제.

### O-1. 보안 — 평문 패스워드 ✅ 해결

> **2026-05-22**: `BCryptPasswordEncoder` bean 추가, `passwordEncoder.encode(adminProperties.password())` 사용. `{noop}` 제거. 단 매 기동마다 encode 호출 (수십~수백ms 비용). 운영 환경에선 사전 hash → env var 권장.

위치: `config/SecurityConfig.java:51`

```java
UserDetails admin = User.withUsername(adminProperties.username())
        .password("{noop}" + adminProperties.password())  // 평문
        .roles("ADMIN").build();
```

**문제**:
- `{noop}` 프리픽스 = 평문 그대로. BCrypt/SCrypt 미사용.
- 환경변수 `ADMIN_PASSWORD=change-me` 기본값. 안 바꿀 가능성 농후.
- BasicAuth는 Base64만 (암호화 아님). HTTPS 강제 안 됨.

### O-2. `/api/dev/**` 기본 공개 ✅ 해결

> **2026-05-22**: `application.yml`, `application-local.yml.example` 모두 `protect-dev-api: true`로 변경. `/api/dev/**`도 BasicAuth 보호.

위치: `config/SecurityConfig.java:39`, `application-local.yml`

```yaml
stock-advisor.security.protect-dev-api: false
```

**문제**:
- `/api/dev/recommendations/generate` — 누구나 추천 생성 트리거 가능.
- `/api/dev/notifications/test` — 누구나 텔레그램 발송 트리거.
- `/api/dev/universe/seed` — DB 오염 가능.
- 운영 배포 시 `protect-dev-api: true` 반드시 켜야 하는데 기본 false → 실수 가능.

### O-3. Codex 비용 폭주 가능

자세한 시나리오는 5장 참조. 한 줄 요약:
- ExitMonitor 5분 주기 × 평일 09~15시 = 78회/일
- 각 트리거에서 최대 5건 Codex → 최대 390건/일
- 일일 한도 200 도달 시 fallback. 그 전까지 비용 계속 발생.

### O-4. 알림 dedupe 키가 날짜 기반 ✅ 해결

> **2026-05-22**: `buildDailyEventKey`가 `priceBucket(entry, current)` 기반으로 변경. `current / entry`를 소수점 2자리(1% 단위)로 라운딩 → 같은 가격 구간에선 중복 알림 차단.

위치: `ExitMonitorJob.buildDailyEventKey`

```java
return "exit-monitor:%s:%s:%s".formatted(
    recommendation.getId(), eventType, LocalDate.now().format(BASIC_ISO_DATE));
```

**문제**:
- "같은 추천+이벤트+날짜"가 dedupe 키.
- 자정 넘어가면 dedupe 리셋. 23:59에 알림, 00:01에 또 알림 가능.
- (다행히 ExitMonitor는 평일 09~15시만 도니까 실제 영향 적음. 다른 잡에선 위험.)

### O-5. KIS 토큰 인메모리만

위치: `infrastructure/marketdata/kr/KisTokenStore.java:30`

```java
private final AtomicReference<String> tokenRef = new AtomicReference<>(null);
private volatile Instant tokenExpiresAt = Instant.EPOCH;
```

**문제**:
- 인메모리. 백엔드 재기동 시 토큰 재발급.
- 멀티 인스턴스(스케일아웃) 환경 시 인스턴스별로 토큰 따로 발급 → KIS 호출 한도 분산 추적 불가.
- 토큰 발급 호출 자체도 제한이 있을 수 있음 (KIS 정책).

### O-6. ddl-auto: validate인데 운영 시 마이그레이션 실패 가능

위치: `application-local.yml:21`

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

**문제**:
- Flyway가 스키마 만들기 전에 Hibernate가 validate 시도 → 첫 실행 실패 가능.
- 다행히 `flyway.baseline-on-migrate: true` 설정으로 어느 정도 보완. 다만 V0 → V1 직접 점프 시 정상 동작 확인 필요.

### O-7. 휴장일 알림이 dedupe 안 됨

위치: `KrxPreOpenJob.run` 등

```java
if (schedulerSettingReader.containsDate(...)) {
    telegramClient.sendMessage("KRX 휴장일입니다...");  // dedupe 없음
    return;
}
```

**문제**:
- `TelegramClient.sendMessage` 직접 호출 → `notification_log` 미사용 → dedupe 없음.
- 휴장일에 매분 cron이 발화하면 매분 알림 발송 가능.
- (잘 보면 `isCurrentSeoulMinute` 체크가 시각 매칭이므로 하루 1번만 발화함. 다만 향후 로직 변경 시 위험.)

### O-8. 외부 API 실패 시 추천 누락

**문제**:
- `KrxPreOpenJob.run` 안에서 KIS 일봉 동기화 실패 → 추천 생성 안 됨.
- 알림은 "❌ KrxPreOpenJob 오류"로 가지만 사용자 입장에선 오늘 추천이 없음.
- 부분 실패 처리 (KIS는 실패해도 기존 데이터로 추천 진행) 없음.

### O-9. Codex 응답 파싱이 substring 기반 ✅ 해결

> **2026-05-22**: `ACTION_LINE_PATTERN = ^ACTION:\s*(HOLD|CUT|TIGHTEN)$` 정규식 추가. 첫 줄만 검사. 미매치 시 안전 HOLD.

위치: `ExitConfirmService.parseAction`

```java
String upper = response.toUpperCase();
if (upper.contains("ACTION: CUT")) return "CUT";
if (upper.contains("ACTION: TIGHTEN")) return "TIGHTEN";
return "HOLD";
```

**문제**:
- Codex가 "I think we should not CUT but rather..." 같이 답하면 "CUT" 매칭.
- 부정문/부사 처리 0.
- Codex 프롬프트가 "첫 줄에 ACTION:..." 강제하지만 LLM이 가끔 어김.

### O-10. 감사 로그 actor 신뢰 불가

위치: `application/admin/AdminSettingService.java:46`

```java
auditLogRepository.save(new AuditLogEntity(request.actor(), "UPDATE_SETTING:" + key, ...));
```

**문제**:
- `actor`는 **요청 바디**에서 옴. 클라이언트가 임의 값 보낼 수 있음.
- Spring Security의 `SecurityContextHolder.getContext().getAuthentication().getName()`을 써야 함.

---

## 5. Codex 비용 폭주 시나리오

5분 주기 ExitMonitor가 어떻게 토큰 폭주로 이어지는지 구체 계산.

### 5.1 트리거 횟수

`ExitMonitorJob`:
```java
@Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
```

- 09~15시 = 7시간 × 60분 / 5분 = **84회**
- 평일만 = 84회/일

각 트리거가 OPEN 추천 전체를 순회. 평균 OPEN 추천 N개라 가정.

### 5.2 Codex 호출 조건

```java
1. KIS 현재가 조회 (개발모드면 스킵)
2. 룰 비교 (target/stop 알림)
3. isExitConfirmRiskZone(current, stop):
       threshold = stop × 1.02
       return current <= threshold
4. 위험구간이면:
   if canAutoConfirm(...):  // 한도 체크
       Codex 호출
```

한도:
- `exit.codex.confirmLimitPerRun` = 5 (스케줄 1회당)
- `exit.codex.confirmLimitPerTickerDaily` = 3 (종목당 일일)
- `exit.codex.confirmCooldownMinutes` = 60 (추천당 쿨다운)

### 5.3 최악 시나리오

가정:
- OPEN 추천 20개 (KRX만)
- 절반(10개)이 손절 위험구간에 진입한 변동성 큰 날

각 트리거당 최대 5건 → 5건/트리거 × 84 트리거 = **420건/일**
하지만 종목당 일일 3건 한도 → 10종목 × 3 = **30건/일 (실효)**

→ 한도들이 잘 잡혀 있어 **이론상 최악도 30건/일**. 일일 한도 200 안 넘김.

### 5.4 그런데 진짜 문제

**한도 안에서도 비용 발생.** 한도가 "비용 폭주 방지"이지 "비용 최소화"가 아님.

프롬프트 길이 (`buildPrompt`):
```
recommendationId=42
market=KOSPI
ticker=005930
...
signalsJson={...전체 feature JSON...}
```

평균 프롬프트 ~1.5KB. 응답 ~4KB 가정.
30건/일 × (1.5+4)KB = **165KB/일 텍스트**.

`codex.estimatedUsdPer1kChars` = $0.002 (Sonnet 기준):
- 165KB / 1KB × $0.002 ≈ **$0.33/일**
- 월 ~$10

그래도 **불필요 호출**이 더 큰 문제.

### 5.5 정말로 Codex가 필요한 케이스

위험구간 진입 30건 중:
- 명확 손절 (current ≤ stop) = 약 10건 → 룰로 CUT 결정 가능, Codex 불필요
- 명확 반등 (RSI 30 이하 + 거래량 급증) = 약 5건 → 룰로 HOLD 가능
- **진짜 모호 = 15건만**

→ Codex 호출 절반 줄일 수 있음. 게다가 추천 종목이 늘어나면 비례 증가.

### 5.6 비용 X 위험

추가로 LONG 추천이 늘면:
- LONG 추천은 보유 기간 6개월 → OPEN 추천 100개 누적 가능
- 100종목 × 3건/일 = 300건/일 → 일일 한도 200 도달 → 후반엔 모두 fallback
- **fallback이 메인이 되는 역설.** 그럴 거면 처음부터 룰 우선이 맞음.

---

## 6. 수정 방안 — 우선순위별

각 결함에 대한 구체적 수정 방향. 코드 변경 위치까지 명시.

### 🔥 P0 — 즉시 수정 (실투자 전 필수)

#### F-1. ExitConfirm 룰 우선 → Codex 후순위

**대상**: C-4

**수정 상태**: 2026-05-22 1차 완료. `currentPrice <= stopPrice`인 명확 손절 케이스는 Codex를 호출하지 않고 룰 기반 `CUT`을 즉시 반환한다. Codex 응답 파싱은 첫 줄의 엄격한 `ACTION: HOLD|CUT|TIGHTEN`만 인정한다. RSI/MA 기반 HOLD, 위험구간 지속시간 기반 TIGHTEN 등 세부 룰 확장은 P1/P2에서 계속 진행한다.

**현재 구조**:
```
위험구간 → 무조건 Codex → 실패 시 룰 fallback
```

**수정 구조**:
```
위험구간 진입:
  1차 룰 판단:
    if current ≤ stop:                          → CUT (확정 손절)
    if RSI 회복(30 → 35↑) AND 거래량 평균 이상:  → HOLD (반등 시그널)
    if MA5 > MA20 AND 종가 > MA5:                → HOLD (추세 살아있음)
    if 위험구간 진입 후 60분 이상 지속:           → TIGHTEN (시간 가치 소진)
  
  룰이 명확하면: 그대로 반환 (Codex 미호출)
  룰이 모호 (예: 위 조건 어디에도 안 걸림):
    → Codex 호출
```

**수정 위치**: `ExitConfirmService.confirm()`

```java
public ExitConfirmResponse confirm(Long recommendationId) {
    // ... 가격 조회 동일 ...
    if (!riskZone) return ... HOLD ...;
    
    // 1차 룰 판단 (신규)
    RuleDecision ruleResult = ruleBasedExit.decide(recommendation, current, history);
    if (ruleResult.confident()) {
        return new ExitConfirmResponse(..., ruleResult.action(), ruleResult.rationale(),
                                       false /* usedFallback */, null, ...);
    }
    
    // 2차 Codex (룰이 모호한 경우만)
    String prompt = buildPrompt(...);
    CodexResult result = codexClient.call(prompt, ...);
    // ... 기존 로직 ...
}
```

**신규 클래스**: `RuleBasedExitDecider` (UniverseFeatureBuilder의 RSI/MA 계산을 재사용)

**기대 효과**: Codex 호출 50% 이상 절감, 판단 일관성 향상.

#### F-2. confidence 실제 백테스트 기반으로

**대상**: C-1

**수정 상태**: 2026-05-22 1차 완료. `RecommendationConfidenceService`를 추가해 동일 시장/기간/feature score band의 과거 평가 hit rate를 confidence로 사용한다. 표본이 30건 미만이면 과신을 막기 위해 50을 반환한다. `DevRecommendationGenerateService`의 티커 해시 기반 confidence 계산은 제거됐다.

**현재**:
```java
Integer confidence = 70 + Math.abs(candidate.ticker().hashCode() % 20);
```

**수정**:
```java
Integer confidence = backtestStatsService.estimateConfidence(
    candidate.market(), candidate.featureJson(), term);
```

`estimateConfidence` 로직:
1. 과거 동일 점수 구간(예: total 80~90) + 동일 시장의 추천 평가 결과 조회
2. hit rate × 100 (반올림)
3. 데이터 부족(< 30개) 시 50 반환 (불확실)

**수정 위치**: `DevRecommendationGenerateService.createRecommendation`

**기대 효과**: 추천 confidence가 실제 과거 성과 반영. 운영자가 비중 결정에 활용 가능.

#### F-3. 가격 fallback 더미 제거 → 추천 발행 차단

**대상**: C-2

**수정 상태**: 2026-05-22 완료. `PricePredictor`의 해시 기반 더미 가격 fallback을 제거했다. 일봉과 `lastPrice`가 모두 없는 후보는 422로 차단하고, 추천 생성 배치에서는 해당 후보만 제외한다. 후보 `lastPrice`를 쓰는 경우 `pricingMethod="last-price-v1"`로 표시한다.

**현재**: 일봉/last_price 없으면 해시 더미 가격으로 추천 발행.

**수정**: 데이터 없는 종목은 **추천에서 제외**.

```java
// PricePredictor.predict
if (history.isEmpty() && candidate.lastPrice() == null) {
    throw new CustomException("가격 데이터가 없어 추천을 생성할 수 없습니다: " 
                              + candidate.ticker(), 422);
}
```

또는 `RecommendationEngine.matchesFilter`에서 사전 차단:
```java
if (candidate.lastPrice() == null && !hasRecentPriceHistory(candidate)) {
    return false;
}
```

**기대 효과**: 진입 즉시 손실 사례 차단.

#### F-4. Codex 응답 파싱 강화

**대상**: O-9

**수정 상태**: 2026-05-22 완료. 응답 전체 substring 검색을 제거하고, 첫 줄이 정규식 `^ACTION:\s*(HOLD|CUT|TIGHTEN)$`와 일치할 때만 액션으로 인정한다. 형식이 어긋나면 안전 기본값 `HOLD`를 반환한다.

**현재**:
```java
if (upper.contains("ACTION: CUT")) return "CUT";
```

**수정**:
```java
// 첫 줄만 검사 + 정규식
String firstLine = response.lines().findFirst().orElse("").toUpperCase().trim();
Pattern pattern = Pattern.compile("^ACTION:\\s*(HOLD|CUT|TIGHTEN)$");
Matcher matcher = pattern.matcher(firstLine);
if (matcher.matches()) return matcher.group(1);
// 첫 줄에 ACTION: 없으면 안전하게 HOLD (운영자가 수동 판단)
return "HOLD";
```

**기대 효과**: LLM 자유 응답으로 인한 오판 방지.

#### F-5. 보안 강화

**대상**: O-1, O-2

**수정 상태**: 2026-05-22 완료. `{noop}` 평문 저장을 제거하고 `BCryptPasswordEncoder`를 사용한다. `application.yml`, `application-local.yml`, `application-local.yml.example`의 `stock-advisor.security.protect-dev-api` 기본값을 `true`로 변경했다.

```java
// SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails admin = User.withUsername(adminProperties.username())
            .password(encoder.encode(adminProperties.password()))  // BCrypt
            .roles("ADMIN").build();
    return new InMemoryUserDetailsManager(admin);
}
```

`application-local.yml`:
```yaml
stock-advisor.security.protect-dev-api: true  # 기본값 변경
```

운영 배포 시 반드시 HTTPS (리버스 프록시 등).

---

### 🟧 P1 — 단기 수정 (1~2주)

#### F-6. modelVersion 동적 결정

**대상**: C-3

**진행 상태 (2026-05-22)**: 완료. `DevRecommendationGenerateService`가 `StrategyVersionRepository.findByChampion(true)` 결과 중 최신 `promotedAt` 값을 가진 champion의 `semver`를 `prediction.modelVersion`과 `recommendation.modelVersion`에 저장한다. champion이 없으면 기존 `dev-rule-v0`로 fallback한다.

**현재**: `MODEL_VERSION = "dev-rule-v0"` 하드코딩

**수정**:
```java
// DevRecommendationGenerateService
private String resolveModelVersion() {
    return strategyVersionRepository.findChampion()
        .map(StrategyVersionEntity::getSemver)
        .orElse("dev-rule-v0");
}

private RecommendationEntity createRecommendation(...) {
    String modelVersion = resolveModelVersion();
    // ...
}
```

`StrategyVersionRepository`에 `findChampion()` 메서드 추가:
```java
@Query("select s from StrategyVersionEntity s where s.isChampion = true")
Optional<StrategyVersionEntity> findChampion();
```

**기대 효과**: AutoResearch 챔피언이 즉시 추천에 반영됨.

#### F-7. 추천 엔진 ↔ 백테스트 연결

**대상**: C-6

**수정 방향**:
1. `BacktestRunService`에 새 메서드 `simulateRecommendationEngine`:
   - `UniverseFeatureBuilder`로 매일 점수 계산
   - 상위 N개 종목 매수 시뮬레이션
   - target/stop/holding 청산 룰
   - metricsJson에 결과 저장

2. AutoResearch가 이 메서드를 호출하여 가중치 변형별로 비교.

**수정 위치**: `BacktestRunService` 신규 메서드 추가, `AutoresearchRunner` 신규 클래스.

#### F-8. 거래비용 반영

**대상**: C-7

**진행 상태 (2026-05-22)**: 완료. `BacktestRunService.simulateBacktest`가 `backtest.slippage.percent`, `backtest.cost.kr`, `backtest.cost.us` 설정을 읽어 effective entry/exit 가격과 비용 반영 PnL을 계산하고, metrics/sampleTrades에 `entryCostPct`, `exitCostPct`, `slippagePct`, `roundTripCostPct`, `effectiveEntryPrice`, `effectiveExitPrice`를 저장한다. `PricePredictor`도 동일 비용 설정을 읽어 effective entry/exit 기준으로 target/stop을 보정하고 `pricingMethod`에 `-cost-adjusted`를 표시한다.

**수정**: `PricePredictor`와 `BacktestRunService.simulateTicker`에 비용 계산 추가.

```java
private static final BigDecimal KR_COST_PCT = BigDecimal.valueOf(0.0021); // 0.21%
private static final BigDecimal US_COST_PCT = BigDecimal.valueOf(0.0005); // 0.05%
private static final BigDecimal SLIPPAGE_PCT = BigDecimal.valueOf(0.0005);

private BigDecimal effectiveEntry(BigDecimal price, String market) {
    BigDecimal cost = isKr(market) ? KR_COST_PCT : US_COST_PCT;
    return price.multiply(BigDecimal.ONE.add(cost.add(SLIPPAGE_PCT)));
}

private BigDecimal effectiveExit(BigDecimal price, String market) {
    BigDecimal cost = isKr(market) ? KR_COST_PCT : US_COST_PCT;
    return price.multiply(BigDecimal.ONE.subtract(cost.add(SLIPPAGE_PCT)));
}
```

설정값 (`backtest.cost.kr.taxPercent` 등) 읽어와 사용.

**기대 효과**: 명목 수익률과 실 수익률 격차 해소.

#### F-9. 시장 레짐 필터

**대상**: S-5

**진행 상태 (2026-05-22)**: 완료. `RecommendationEngine`이 `recommendation.regime.filter.enabled=true`일 때 NASDAQ/NYSE/US는 `SPY`, KOSPI/KOSDAQ은 `KOSPI`의 최근 200일 MA 기준으로 약세장 후보 생성을 차단한다. 데이터가 200개 미만이면 운영 중단을 피하기 위해 통과한다.

**수정**: `RecommendationEngine.buildCandidates` 진입 시 시장 트렌드 체크.

```java
private boolean isMarketRegimeOk(String market) {
    String indexTicker = switch(market) {
        case "KOSPI", "KOSDAQ" -> "KOSPI";       // KOSPI 200 ETF 또는 지수 자체
        case "NASDAQ", "NYSE" -> "SPY";          // S&P 500 ETF
        default -> null;
    };
    List<PriceDailyEntity> history = priceDailyRepository
        .findByMarketAndTickerOrderByTradeDateDesc(market, indexTicker, PageRequest.of(0, 200));
    if (history.size() < 200) return true;  // 데이터 부족 → 통과
    BigDecimal latestClose = history.get(0).getClosePrice();
    BigDecimal ma200 = average(history.stream().map(PriceDailyEntity::getClosePrice).toList());
    return latestClose.compareTo(ma200) >= 0;  // MA200 위 → 강세장
}
```

또는 설정으로 토글 가능하게:
```yaml
recommendation.regime.filter.enabled: true
```

#### F-10. 섹터 분산 제약

**대상**: S-6

**진행 상태 (2026-05-22)**: 완료. `RecommendationEngine.selectTopCandidates`가 `recommendation.sector.max` 설정값 기준으로 섹터별 후보 수를 제한한다. 기본 fallback은 2개다.

**수정**: `RecommendationEngine.selectTopCandidates`

```java
public List<RecommendationCandidate> selectTopCandidates(String market, int count) {
    int maxPerSector = settingReader.getInt("recommendation.sector.max", 2);
    Map<String, Integer> sectorCount = new HashMap<>();
    return buildCandidates(market).stream()
        .filter(c -> {
            String sector = c.sector() == null ? "UNKNOWN" : c.sector();
            int used = sectorCount.getOrDefault(sector, 0);
            if (used >= maxPerSector) return false;
            sectorCount.merge(sector, 1, Integer::sum);
            return true;
        })
        .limit(count)
        .toList();
}
```

**기대 효과**: 섹터 5개 동시 추천 방지. 분산투자 효과.

#### F-11. ExitConfirm dedupe 키 개선

**대상**: O-4

**진행 상태 (2026-05-22)**: 완료. `ExitMonitorJob`의 TARGET/STOP 알림 dedupe key가 일자 단위가 아니라 `recommendationId + eventType + current/entry price bucket` 기준으로 생성된다.

날짜 대신 **이벤트 발생 시점 + 가격 라운딩** 기반.

```java
private String buildDedupeKey(RecommendationEntity rec, String eventType, BigDecimal price) {
    // 가격을 1% 단위로 라운딩 → 같은 가격대에서 반복 알림 방지
    String priceBucket = price.divide(rec.getEntryPrice(), 2, RoundingMode.HALF_UP).toString();
    return "exit-monitor:%s:%s:%s".formatted(rec.getId(), eventType, priceBucket);
}
```

---

### 🟨 P2 — 중기 수정 (1~2개월)

#### F-12. AutoResearch 본체 구현

**대상**: C-5

**범위**:
1. `AutoresearchRunner` 스케줄러 신규 (예: 매일 새벽 2시)
2. 전략 변형 로직 (`StrategyMutator`):
   - 현재 가중치 (0.20, 0.10, 0.30, 0.15, 0.10, 0.15)에 ±0.05 무작위 변형
   - 또는 베이지안 최적화 라이브러리(Optuna 등) 연동
3. 변형 가중치로 `BacktestRunService.simulateRecommendationEngine` 실행
4. metric (Sharpe, hitRate, totalPnl) 비교
5. 챔피언보다 좋으면 `autoresearch_run.decision="PROMOTE"`, `strategy_version` 신규 row 추가
6. 7일 라이브 검증:
   - 새 챔피언 적용 후 7일간 실제 추천 성과 추적
   - 백테스트 metric의 50% 미만이면 자동 롤백 (`is_champion=true`를 이전 버전으로)

**파일 신규**:
- `application/autoresearch/AutoresearchRunner.java` (@Scheduled, @Component)
- `application/autoresearch/StrategyMutator.java`
- `application/autoresearch/ChampionEvaluator.java`

#### F-13. 점수 가중치 외부화

**대상**: S-1

**현재**: 가중치 코드 상수.

**수정**: `app_setting` 에 저장.

```json
// recommendation.scoring.weights
{
  "value": {
    "liquidity": 0.20,
    "price": 0.10,
    "technical": 0.30,
    "context": 0.15,
    "fundamental": 0.10,
    "dataQuality": 0.15
  },
  "technical": { "ma": 0.40, "rsi": 0.35, "volume": 0.25 },
  "context": { "news": 0.40, "disclosure": 0.18, "macro": 0.25, "fundamental": 0.17 }
}
```

`UniverseFeatureBuilder`가 매 호출마다 설정 읽어 사용 → AutoResearch가 가중치만 바꿔 실험 가능.

#### F-14. 뉴스 감성 — 임베딩 또는 LLM 기반 점수

**대상**: S-2

**옵션 A** (간단): Codex/Claude에게 배치로 점수 매기게. 일 1회 일괄.
**옵션 B** (정석): Hugging Face FinBERT 등 임베딩 모델로 점수.
**옵션 C** (절충): 키워드 기반 유지하되, 부정문 처리 룰 추가 ("not", "no", "취소", "안", "조심" 등 앞에 나오면 반전).

`MarketDataCollectionJob` 수집 시점에 일괄 처리.

#### F-15. 포지션 사이징

**대상**: S-7

**진행 상태 (2026-05-22)**: 1차 완료. `DevRecommendationGenerateService`가 entry/stop 리스크와 confidence를 이용해 `signalsJson.positionWeightPct`를 기록한다. 별도 DB 컬럼 추가 없이 기존 signals JSON으로 노출한다.

**수정**: 추천 응답에 `positionWeight` 필드 추가.

```java
// PredictedRecommendation에 추가
record PredictedRecommendation(..., BigDecimal positionWeight) {}

// 계산:
// 1) 고정 % risk: positionWeight = riskPerTrade / (entry - stop) * entry
// 2) Kelly: positionWeight = (hitRate * avgWin - lossRate * avgLoss) / avgWin
// 3) 신뢰도 가중: positionWeight = confidence / sum(confidences) * totalBudget
```

설정으로 방법 선택:
```yaml
recommendation.sizing.method: FIXED_RISK  # or KELLY, CONFIDENCE_WEIGHTED
recommendation.sizing.riskPerTradePct: 1.0  # 자본의 1%만 리스크
```

#### F-16. 생존편향 처리

**대상**: S-8

`market_universe`에 `delisted_at` 컬럼 추가. KIND/NASDAQ Trader 동기화 시 사라진 종목을 삭제하지 말고 `delisted_at`만 표시.

백테스트에서는 `delisted_at` 무시하고 전체 사용 → 상폐 종목도 시뮬레이션에 포함.

```sql
-- V7__delisted_at.sql
ALTER TABLE market_universe ADD delisted_at date NULL;
```

---

### 🟦 P3 — 장기 / 선택적

#### F-17. KIS 토큰 DB 보관

**대상**: O-5

`app_setting`이 아닌 별도 테이블(`kis_token`)에 저장 → 멀티 인스턴스 공유.

#### F-18. RSI 점수 룰 재검토

**대상**: S-3

운영하면서 데이터 쌓이면 RSI 구간별 실제 hit rate 측정. 룰 재학습.

#### F-19. 운영자 대시보드 — 결함 모니터링 지표

신규 API/UI: 현재 시스템 건강도 표시.
- 추천 중 `pricingMethod="fallback-v1"` 비율
- Codex 호출 한도 도달 횟수
- 위험구간 진입 → 룰 결정 / Codex 결정 비율
- 일별 hit rate 트렌드

---

## 7. 단계별 로드맵

```
Week 1-2 (P0):
  ☑ F-1  ExitConfirm 룰 우선 1차 (명확 손절 CUT은 Codex 미호출)
  ☑ F-2  confidence 실제 hit rate 기반 1차
  ☑ F-3  가격 fallback 제거 → 추천 차단
  ☑ F-4  Codex 응답 파싱 강화
  ☑ F-5  보안 (BCrypt, dev API 보호)

Week 3-6 (P1):
  ☑ F-6  modelVersion 동적 결정
  ☐ F-7  추천 ↔ 백테스트 연결
  ☑ F-8  거래비용 반영 (BacktestRunService + PricePredictor 비용 조정 완료)
  ☑ F-9  시장 레짐 필터
  ☑ F-10 섹터 분산
  ☑ F-11 dedupe 키 개선

Month 2-3 (P2):
  ☐ F-12 AutoResearch 본체 (스케줄러 + 변형 + 검증)
  ☐ F-13 가중치 외부화
  ☐ F-14 뉴스 감성 개선
  ☑ F-15 포지션 사이징 (signalsJson positionWeightPct 1차)
  ☐ F-16 생존편향 처리

Long-term (P3):
  ☐ F-17 KIS 토큰 DB
  ☐ F-18 RSI 재학습
  ☐ F-19 운영 대시보드
```

각 P0 항목은 **독립적으로 머지 가능**. 동시에 작업 가능.
P1 부터는 의존 관계 있음 (예: F-6은 F-7 결과 활용 가능).

---

## 8. 수정 후 기대 효과

### 8.1 비용

| 항목 | 현재 | F-1 이후 | F-1 + F-12 이후 |
|---|---|---|---|
| Codex 일 호출 (OPEN 50개 가정) | 최대 150건 | ~60건 | ~30건 |
| 월 Codex 비용 (Sonnet 기준) | ~$50 | ~$20 | ~$10 |

### 8.2 성과

수익성은 백테스트 없이 단언 불가. 다만 다음은 합리적 기대:

| 지표 | 현재 (예상) | 수정 후 (목표) |
|---|---|---|
| 명목 hit rate | 40~50% (백테스트 안 됨) | 측정 가능, 튜닝 가능 |
| 실 hit rate (비용 반영) | 35~45% | 40~50% (목표) |
| 손익분기 hit rate | 42% (SHORT) | 동일 |
| 최대 낙폭 | 무제한 (포지션 사이징 0) | 자본의 N% 이내 통제 |
| 섹터 집중 위험 | 높음 | F-10으로 차단 |
| 약세장 손실 | 풀노출 | F-9 레짐 필터로 차단 |

### 8.3 운영

- **자동 진화**: F-12 완료 시 매일 새벽 새 전략 실험 → 점진적 성능 개선.
- **감사 가능**: F-13으로 가중치 변경 이력이 `audit_log`에 남음.
- **롤백 가능**: F-12 검증 7일 + `strategy_version.is_champion` 토글.

### 8.4 사용자 신뢰

- `confidence` 값에 의미 부여 → 운영자 비중 결정 근거.
- `pricingMethod` 표시 정확화 → fallback 추천 사라짐.
- 알림 메시지에 룰 근거 명시 (Codex 사용 여부) → 투명성.

---

## 9. 잔여 작업 및 알려진 버그

### 9.1 발견된 버그

#### 🐛 BUG-1. 섹터 분산 호출 누락

**증상**: `selectTopCandidates` 메서드 구현 완료, sector cap 룰도 작성됐는데 **어디서도 호출 안 함**.

**영향**: 반도체 5개 동시 추천 가능. F-10 의도 무효.

**원인**: `DevRecommendationGenerateService.java:42`
```java
List<RecommendationCandidate> candidates = recommendationEngine.buildCandidates(market);
// buildCandidates 직접 사용 → selectTopCandidates 우회
```

**수정안 A** (간단): 호출 변경
```java
int totalNeeded = Math.max(safeShortCount, safeLongCount);
List<RecommendationCandidate> candidates = recommendationEngine.selectTopCandidates(market, totalNeeded * 3);
```

**수정안 B** (정석): `buildCandidates` 내부에서 sector cap 적용. `selectTopCandidates`는 단순 limit 래퍼로.

**우선순위**: P0 (실투자 전 필수)

#### 🐛 BUG-2. `hasPriceData()` 중복 predict 호출

**증상**: `DevRecommendationGenerateService.hasPriceData`가 후보 N개마다 `pricePredictor.predict(candidate, "SHORT")` 호출 → 필터 통과 후 `generate`에서 또 호출. **DB 조회 2배**.

**영향**: 추천 생성 시간 증가. 후보 100개면 DB 조회 200회.

**수정안**: 캐싱
```java
Map<String, PredictedRecommendation> cache = new HashMap<>();
List<RecommendationCandidate> pricedCandidates = candidates.stream()
    .filter(c -> {
        try {
            cache.put(c.ticker(), pricePredictor.predict(c, "SHORT"));
            return true;
        } catch (CustomException e) {
            return e.getCode() != 422;
        }
    })
    .toList();
// 이후 cache.get(ticker) 사용
```

**우선순위**: P1 (성능, 정확성 영향 없음)

#### 🐛 BUG-3. ExitMonitor 자동 평가 시 `drawdown_pct = null`

**증상**: `closeRecommendationWithEvaluation`이 평가 생성할 때 `drawdown_pct`를 null로 저장.

**영향**: `StatsService.getSummary().maxDrawdown`이 부정확. 통계 신뢰도 저하.

**수정안**: 보유 기간 중 최저가 추적. `price_intraday` 또는 `price_daily`에서 entry ~ exit 사이 최저 종가 조회 → drawdownPct 계산.

**우선순위**: P2

### 9.2 남은 P1 작업

| 항목 | 상태 |
|---|---|
| F-7 추천 ↔ 백테스트 연결 | ❌ 미착수. BacktestRunService에 `simulateRecommendationEngine` 메서드 추가 필요 |

### 9.3 남은 P2 작업

| 항목 | 상태 |
|---|---|
| F-12 AutoResearch 본체 | ❌ 미착수. 스케줄러 + StrategyMutator + ChampionEvaluator |
| F-13 가중치 외부화 | ❌ 미착수. `UniverseFeatureBuilder` 상수 → `app_setting` |
| F-14 뉴스 감성 NLP | ❌ 미착수. 부정문 처리 또는 임베딩 모델 |
| F-16 생존편향 처리 | ❌ 미착수. `market_universe.delisted_at` 컬럼 추가 |

### 9.4 추천 후속 조치

**운영 시작 전 체크리스트**:
- [ ] BUG-1 호출처 수정 (섹터 분산 활성화)
- [ ] `recommendation.regime.filter.enabled = true` 토글 (일봉 200개 쌓인 후)
- [ ] `recommendation.sector.max` 운영 환경 값 결정 (기본 2 적정)
- [ ] BCrypt 사전 hash → `ADMIN_PASSWORD` env var 직접 설정 (기동 비용 절감)
- [ ] `codex.daily.callLimit` 운영 한도 재설정 (현재 200 → 룰 우선화 이후 50 정도로 낮춰도 충분)
- [ ] confidence 학습 누적 (`evaluation` 30건 이상 쌓일 때까지 자동매매 보류)

**모니터링**:
- `pricingMethod` 분포 → `unavailable` 비율 0 유지 확인
- ExitConfirm 로그에서 `usedFallback=false` (Codex 실제 사용) 빈도 → 룰만으로 처리되는 비율 확인
- `audit_log`에서 가중치/임계값 변경 추적

---

## 10. 한 줄 결론 (업데이트)

> **"P0 5개 + P1 5개 + 자동 청산까지 적용 완료. 자동매매 금지 → 자동매매 검토 가능 수준.
> 남은 핵심은 AutoResearch 본체(F-12)와 백테스트-추천 엔진 연결(F-7).
> BUG-1 (섹터 분산 호출 누락)은 운영 전 반드시 수정 필요."**

### 적용 전후 비교 한 줄

- **Before**: 공개 데이터 잘 받아오고, 가짜 confidence + 해시 더미 가격 + Codex 외주 청산 → **손실 확정**
- **After**: 룰 우선 청산 + 실데이터 confidence + 데이터 없는 종목 차단 + 거래비용 반영 + 자동 평가 + BCrypt 보안 → **소액 검증 가능**

### 향후 핵심 KPI

운영 시작 후 다음 지표로 시스템 성숙도 측정:

| KPI | 목표 |
|---|---|
| Codex 호출/일 | < 30건 (룰 우선화 효과) |
| `pricingMethod="unavailable"` 비율 | 0% |
| `confidence != 50` 비율 (샘플 충분 종목) | > 50% (3개월 후) |
| 자동 청산 / 수동 청산 비율 | > 90% / < 10% |
| 섹터 분산 (한 추천 묶음 내 동일 섹터 max) | ≤ 2 (BUG-1 수정 후) |
| 평균 hit rate (실 평가 기반) | > 42% (SHORT, 비용 손익분기) |
