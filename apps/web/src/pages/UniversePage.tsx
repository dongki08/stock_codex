import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { syncDailyPrices } from "../api/marketData";
import { getUniverse, getUniverseFeatures, MarketUniverseItem, seedDevUniverse, syncKrSymbols, syncUsPrices, syncUsSymbols, UniverseFeature } from "../api/universe";

const markets = ["KOSPI", "KOSDAQ", "NYSE", "NASDAQ"];

function formatCompact(value: number | null, market: string) {
  if (value === null) {
    return "-";
  }

  const currency = market === "KOSPI" || market === "KOSDAQ" ? "KRW" : "USD";

  return new Intl.NumberFormat("ko-KR", {
    compactDisplay: "short",
    currency,
    maximumFractionDigits: 1,
    notation: "compact",
    style: "currency"
  }).format(value);
}

function formatPrice(value: number | null, market: string) {
  if (value === null) {
    return "-";
  }

  const currency = market === "KOSPI" || market === "KOSDAQ" ? "KRW" : "USD";

  return new Intl.NumberFormat("ko-KR", {
    currency,
    maximumFractionDigits: market === "KOSPI" || market === "KOSDAQ" ? 0 : 2,
    style: "currency"
  }).format(value);
}

export function UniversePage() {
  const queryClient = useQueryClient();
  const [market, setMarket] = useState("ALL");
  const [seedMarket, setSeedMarket] = useState("ALL");
  const [krSyncMarket, setKrSyncMarket] = useState("ALL");
  const [syncMarket, setSyncMarket] = useState("ALL");
  const [priceSyncLimit, setPriceSyncLimit] = useState("50");
  const [dailySyncMarket, setDailySyncMarket] = useState("ALL");
  const [dailySyncLimit, setDailySyncLimit] = useState("20");
  const [dailySyncDays, setDailySyncDays] = useState("120");
  const [keyword, setKeyword] = useState("");
  const [tradableOnly, setTradableOnly] = useState(true);
  const [minMarketCap, setMinMarketCap] = useState("");
  const [minAvgTurnover, setMinAvgTurnover] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const universeQuery = useQuery({
    queryKey: ["universe", market, tradableOnly, minMarketCap, minAvgTurnover],
    queryFn: () =>
      getUniverse({
        market,
        minAvgTurnover,
        minMarketCap,
        tradable: tradableOnly ? true : undefined
      })
  });

  const featuresQuery = useQuery({
    queryKey: ["universe-features", market],
    queryFn: () => getUniverseFeatures(market, "500")
  });

  const seedMutation = useMutation({
    mutationFn: () => seedDevUniverse(seedMarket),
    onSuccess: (result) => {
      setMessage(`${result.market} 후보군 ${result.upsertedCount}개를 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["universe"] });
      queryClient.invalidateQueries({ queryKey: ["universe-features"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "개발용 후보군 생성에 실패했습니다.");
    }
  });

  const syncMutation = useMutation({
    mutationFn: () => syncUsSymbols(syncMarket),
    onSuccess: (result) => {
      setMessage(`${result.source} ${result.market} 심볼 ${result.upsertedCount}개를 동기화했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["universe"] });
      queryClient.invalidateQueries({ queryKey: ["universe-features"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "미국 심볼 동기화에 실패했습니다.");
    }
  });

  const krSyncMutation = useMutation({
    mutationFn: () => syncKrSymbols(krSyncMarket),
    onSuccess: (result) => {
      setMessage(`${result.source} ${result.market} 한국 심볼 ${result.upsertedCount}개를 동기화했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["universe"] });
      queryClient.invalidateQueries({ queryKey: ["universe-features"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "한국 심볼 동기화에 실패했습니다.");
    }
  });

  const priceSyncMutation = useMutation({
    mutationFn: () => syncUsPrices(syncMarket, priceSyncLimit),
    onSuccess: (result) => {
      setMessage(`${result.source} ${result.market} 가격 ${result.upsertedCount}/${result.fetchedCount}개를 갱신했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["universe"] });
      queryClient.invalidateQueries({ queryKey: ["universe-features"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "미국 가격 동기화에 실패했습니다.");
    }
  });

  const dailySyncMutation = useMutation({
    mutationFn: () => syncDailyPrices(dailySyncMarket, dailySyncLimit, dailySyncDays),
    onSuccess: (result) => {
      setMessage(`${result.market} 일봉 ${result.upsertedCount}개를 저장했습니다. 대상 후보 ${result.candidateCount}개`);
      queryClient.invalidateQueries({ queryKey: ["universe"] });
      queryClient.invalidateQueries({ queryKey: ["universe-features"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "일봉 가격 동기화에 실패했습니다.");
    }
  });

  const universe = universeQuery.data ?? [];
  const featureByKey = useMemo(() => {
    return new Map((featuresQuery.data ?? []).map((feature) => [feature.universeKey, feature]));
  }, [featuresQuery.data]);

  const filteredUniverse = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    if (!normalizedKeyword) {
      return universe;
    }

    return universe.filter((item) => item.ticker.toLowerCase().includes(normalizedKeyword) || item.name.toLowerCase().includes(normalizedKeyword) || (item.sector ?? "").toLowerCase().includes(normalizedKeyword));
  }, [keyword, universe]);

  const tradableCount = universe.filter((item) => item.tradable).length;

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Market Universe</p>
          <h1>시장 후보군</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar">
        <div>
          <strong>{filteredUniverse.length}</strong>
          <span>개 후보</span>
        </div>
        <div>
          <strong>{tradableCount}</strong>
          <span>개 거래 가능</span>
        </div>
        <label className="inline-field">
          <span>Seed 시장</span>
          <select value={seedMarket} onChange={(event) => setSeedMarket(event.target.value)}>
            <option value="ALL">전체</option>
            {markets.map((marketOption) => (
              <option key={marketOption} value={marketOption}>
                {marketOption}
              </option>
            ))}
          </select>
        </label>
        <button disabled={seedMutation.isPending} type="button" onClick={() => seedMutation.mutate()}>
          개발용 후보군 생성
        </button>
        <label className="inline-field">
          <span>한국 시장</span>
          <select value={krSyncMarket} onChange={(event) => setKrSyncMarket(event.target.value)}>
            <option value="ALL">전체</option>
            <option value="KOSPI">KOSPI</option>
            <option value="KOSDAQ">KOSDAQ</option>
          </select>
        </label>
        <button className="secondary" disabled={krSyncMutation.isPending} type="button" onClick={() => krSyncMutation.mutate()}>
          한국 심볼 동기화
        </button>
        <label className="inline-field">
          <span>미국 시장</span>
          <select value={syncMarket} onChange={(event) => setSyncMarket(event.target.value)}>
            <option value="ALL">전체</option>
            <option value="NASDAQ">NASDAQ</option>
            <option value="NYSE">NYSE</option>
          </select>
        </label>
        <button className="secondary" disabled={syncMutation.isPending} type="button" onClick={() => syncMutation.mutate()}>
          미국 심볼 동기화
        </button>
        <label className="inline-field">
          <span>가격 개수</span>
          <input
            min="1"
            max="500"
            type="number"
            value={priceSyncLimit}
            onChange={(event) => setPriceSyncLimit(event.target.value)}
          />
        </label>
        <button className="secondary" disabled={priceSyncMutation.isPending} type="button" onClick={() => priceSyncMutation.mutate()}>
          미국 가격 동기화
        </button>
        <label className="inline-field">
          <span>일봉 시장</span>
          <select value={dailySyncMarket} onChange={(event) => setDailySyncMarket(event.target.value)}>
            <option value="ALL">전체</option>
            {markets.map((marketOption) => (
              <option key={marketOption} value={marketOption}>
                {marketOption}
              </option>
            ))}
          </select>
        </label>
        <label className="inline-field">
          <span>후보 수</span>
          <input
            min="1"
            max="500"
            type="number"
            value={dailySyncLimit}
            onChange={(event) => setDailySyncLimit(event.target.value)}
          />
        </label>
        <label className="inline-field">
          <span>일수</span>
          <input
            min="1"
            max="2000"
            type="number"
            value={dailySyncDays}
            onChange={(event) => setDailySyncDays(event.target.value)}
          />
        </label>
        <button className="secondary" disabled={dailySyncMutation.isPending} type="button" onClick={() => dailySyncMutation.mutate()}>
          일봉 동기화
        </button>
      </section>

      <section className="filters universe-filters">
        <label className="inline-field">
          <span>시장</span>
          <select value={market} onChange={(event) => setMarket(event.target.value)}>
            <option value="ALL">전체</option>
            {markets.map((marketOption) => (
              <option key={marketOption} value={marketOption}>
                {marketOption}
              </option>
            ))}
          </select>
        </label>
        <input
          aria-label="후보군 검색"
          placeholder="종목 코드, 이름, 섹터 검색"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <input
          aria-label="시가총액 하한"
          placeholder="시총 하한"
          type="number"
          value={minMarketCap}
          onChange={(event) => setMinMarketCap(event.target.value)}
        />
        <input
          aria-label="평균 거래대금 하한"
          placeholder="거래대금 하한"
          type="number"
          value={minAvgTurnover}
          onChange={(event) => setMinAvgTurnover(event.target.value)}
        />
        <label className="check-field compact">
          <input checked={tradableOnly} type="checkbox" onChange={(event) => setTradableOnly(event.target.checked)} />
          <span>거래 가능만</span>
        </label>
      </section>

      {message && <p className="notice">{message}</p>}
      {universeQuery.isLoading && <p className="notice">후보군을 불러오는 중입니다.</p>}
      {universeQuery.isError && <p className="notice error">후보군 API에 연결할 수 없습니다.</p>}
      {featuresQuery.isError && <p className="notice error">Feature 점수 API에 연결할 수 없습니다.</p>}

      {filteredUniverse.length === 0 && !universeQuery.isLoading && (
        <section className="empty-state">
          <h2>표시할 후보군이 없습니다.</h2>
          <p>개발용 후보군 생성 버튼을 누르거나 필터 조건을 낮추세요.</p>
        </section>
      )}

      {filteredUniverse.length > 0 && (
        <section className="table-panel">
          <table>
            <thead>
              <tr>
                <th>종목</th>
                <th>시장</th>
                <th>섹터</th>
                <th>최근 가격</th>
                <th>시총</th>
                <th>거래대금</th>
                <th>종합 점수</th>
                <th>세부 점수</th>
                <th>상태</th>
                <th>출처</th>
              </tr>
            </thead>
            <tbody>
              {filteredUniverse.map((item) => (
                <UniverseRow feature={featureByKey.get(item.universeKey)} item={item} key={item.universeKey} />
              ))}
            </tbody>
          </table>
        </section>
      )}
    </main>
  );
}

function getScoreClass(score: number | undefined) {
  if (typeof score !== "number") {
    return "score-pill muted";
  }

  if (score >= 80) {
    return "score-pill strong";
  }

  if (score >= 60) {
    return "score-pill";
  }

  return "score-pill warn";
}

type FeatureJsonDetails = {
  contextScore?: number;
  newsScore?: number;
  disclosureScore?: number;
  macroScore?: number;
  fundamentalScore?: number;
  newsCount?: number;
  disclosureCount?: number;
  macroObservationCount?: number;
  fundamentalMetricCount?: number;
  source?: string;
};

function parseFeatureDetails(feature?: UniverseFeature): FeatureJsonDetails | null {
  if (!feature?.featureJson) {
    return null;
  }

  try {
    return JSON.parse(feature.featureJson) as FeatureJsonDetails;
  } catch {
    return null;
  }
}

function UniverseRow({ feature, item }: { feature?: UniverseFeature; item: MarketUniverseItem }) {
  const featureDetails = parseFeatureDetails(feature);

  return (
    <tr>
      <td>
        <strong>{item.ticker}</strong>
        <span className="subtle-text">{item.name}</span>
      </td>
      <td>{item.market}</td>
      <td>{item.sector || "-"}</td>
      <td>{formatPrice(item.lastPrice, item.market)}</td>
      <td>{formatCompact(item.marketCap, item.market)}</td>
      <td>{formatCompact(item.avgTurnover, item.market)}</td>
      <td>
        <span className={getScoreClass(feature?.totalScore)}>{feature?.totalScore ?? "-"}</span>
      </td>
      <td>
        {feature ? (
          <span className="subtle-text inline-score">
            기술 {feature.technicalScore} / RSI {feature.rsiScore} / 이평 {feature.movingAverageScore} / 거래량 {feature.volumeScore} / 품질 {feature.dataQualityScore} / {feature.priceHistoryCount}일
            {featureDetails && typeof featureDetails.contextScore === "number" && (
              <>
                <br />
                맥락 {featureDetails.contextScore} / 뉴스 {featureDetails.newsScore ?? "-"}({featureDetails.newsCount ?? 0}) / 공시 {featureDetails.disclosureScore ?? "-"}({featureDetails.disclosureCount ?? 0}) / 매크로 {featureDetails.macroScore ?? "-"} / 펀더멘털 {featureDetails.fundamentalScore ?? "-"}({featureDetails.fundamentalMetricCount ?? 0})
              </>
            )}
          </span>
        ) : (
          <span className="subtle-text">-</span>
        )}
      </td>
      <td>
        <span className={item.tradable ? "status-pill on" : "status-pill"}>{item.tradable ? "거래 가능" : "제외"}</span>
      </td>
      <td>{item.source}</td>
    </tr>
  );
}
