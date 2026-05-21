import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  DisclosureEvent,
  FundamentalMetric,
  getDisclosureEvents,
  getFundamentalMetrics,
  getMacroObservations,
  getNewsArticles,
  MacroObservation,
  NewsArticle,
  syncDisclosureEvents,
  syncFundamentalMetrics,
  syncMacroObservations,
  syncNewsArticles
} from "../api/marketData";

const markets = ["KOSPI", "KOSDAQ", "NYSE", "NASDAQ"];
const macroSeries = ["", "DGS10", "FEDFUNDS", "CPIAUCSL", "DCOILWTICO", "DTWEXBGS"];

type View = "news" | "disclosures" | "macro" | "fundamentals";

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium"
  }).format(new Date(value));
}

function formatNumber(value: number | null) {
  if (value === null) {
    return "-";
  }

  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 4
  }).format(value);
}

export function MarketDataPage() {
  const queryClient = useQueryClient();
  const [view, setView] = useState<View>("news");
  const [market, setMarket] = useState("NASDAQ");
  const [ticker, setTicker] = useState("AAPL");
  const [limit, setLimit] = useState("20");
  const [seriesId, setSeriesId] = useState("DGS10");
  const [macroLimit, setMacroLimit] = useState("20");
  const [message, setMessage] = useState<string | null>(null);

  const newsQuery = useQuery({
    enabled: view === "news",
    queryKey: ["market-data-news", market, ticker, limit],
    queryFn: () => getNewsArticles(market, ticker, limit)
  });

  const disclosuresQuery = useQuery({
    enabled: view === "disclosures",
    queryKey: ["market-data-disclosures", market, ticker, limit],
    queryFn: () => getDisclosureEvents(market, ticker, limit)
  });

  const macroQuery = useQuery({
    enabled: view === "macro",
    queryKey: ["market-data-macro", seriesId, macroLimit],
    queryFn: () => getMacroObservations(seriesId, macroLimit)
  });

  const fundamentalsQuery = useQuery({
    enabled: view === "fundamentals",
    queryKey: ["market-data-fundamentals", market, ticker, limit],
    queryFn: () => getFundamentalMetrics(market, ticker, limit)
  });

  const syncNewsMutation = useMutation({
    mutationFn: () => syncNewsArticles(market, ticker, limit),
    onSuccess: (result) => {
      setMessage(`${result.source} 뉴스 ${result.savedCount}/${result.fetchedCount}건을 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["market-data-news"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "뉴스 동기화에 실패했습니다.");
    }
  });

  const syncDisclosuresMutation = useMutation({
    mutationFn: () => syncDisclosureEvents(market, ticker, limit),
    onSuccess: (result) => {
      setMessage(`${result.source} 공시 ${result.savedCount}/${result.fetchedCount}건을 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["market-data-disclosures"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "공시 동기화에 실패했습니다.");
    }
  });

  const syncMacroMutation = useMutation({
    mutationFn: () => syncMacroObservations(seriesId, macroLimit),
    onSuccess: (result) => {
      setMessage(`${result.source} 매크로 ${result.savedCount}/${result.fetchedCount}건을 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["market-data-macro"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "매크로 동기화에 실패했습니다.");
    }
  });

  const syncFundamentalsMutation = useMutation({
    mutationFn: () => syncFundamentalMetrics(market, ticker),
    onSuccess: (result) => {
      setMessage(`${result.source} 펀더멘털 ${result.savedCount}/${result.fetchedCount}건을 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["market-data-fundamentals"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "펀더멘털 동기화에 실패했습니다.");
    }
  });

  const activeRows = useMemo(() => {
    if (view === "news") {
      return newsQuery.data?.length ?? 0;
    }
    if (view === "disclosures") {
      return disclosuresQuery.data?.length ?? 0;
    }
    if (view === "fundamentals") {
      return fundamentalsQuery.data?.length ?? 0;
    }
    return macroQuery.data?.length ?? 0;
  }, [disclosuresQuery.data?.length, fundamentalsQuery.data?.length, macroQuery.data?.length, newsQuery.data?.length, view]);

  const isLoading = newsQuery.isLoading || disclosuresQuery.isLoading || macroQuery.isLoading || fundamentalsQuery.isLoading;
  const isError = newsQuery.isError || disclosuresQuery.isError || macroQuery.isError || fundamentalsQuery.isError;

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Market Data</p>
          <h1>수집 데이터</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar data-toolbar">
        <div>
          <strong>{activeRows}</strong>
          <span>개 항목</span>
        </div>
        <div className="tabs">
          <button className={view === "news" ? "active" : ""} type="button" onClick={() => setView("news")}>
            뉴스
          </button>
          <button className={view === "disclosures" ? "active" : ""} type="button" onClick={() => setView("disclosures")}>
            공시
          </button>
          <button className={view === "macro" ? "active" : ""} type="button" onClick={() => setView("macro")}>
            매크로
          </button>
          <button className={view === "fundamentals" ? "active" : ""} type="button" onClick={() => setView("fundamentals")}>
            펀더멘털
          </button>
        </div>
      </section>

      {view !== "macro" && (
        <section className="filters data-filters">
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
          <input aria-label="종목 코드" placeholder="종목 코드" value={ticker} onChange={(event) => setTicker(event.target.value)} />
          <input aria-label="조회 개수" max="100" min="1" type="number" value={limit} onChange={(event) => setLimit(event.target.value)} />
          {view === "news" ? (
            <button disabled={syncNewsMutation.isPending} type="button" onClick={() => syncNewsMutation.mutate()}>
              뉴스 동기화
            </button>
          ) : view === "disclosures" ? (
            <button disabled={syncDisclosuresMutation.isPending} type="button" onClick={() => syncDisclosuresMutation.mutate()}>
              공시 동기화
            </button>
          ) : (
            <button disabled={syncFundamentalsMutation.isPending || !ticker.trim()} type="button" onClick={() => syncFundamentalsMutation.mutate()}>
              펀더멘털 동기화
            </button>
          )}
        </section>
      )}

      {view === "macro" && (
        <section className="filters macro-filters">
          <label className="inline-field">
            <span>지표</span>
            <select value={seriesId} onChange={(event) => setSeriesId(event.target.value)}>
              {macroSeries.map((series) => (
                <option key={series || "ALL"} value={series}>
                  {series || "기본 묶음"}
                </option>
              ))}
            </select>
          </label>
          <input aria-label="매크로 조회 개수" max="100" min="1" type="number" value={macroLimit} onChange={(event) => setMacroLimit(event.target.value)} />
          <button disabled={syncMacroMutation.isPending} type="button" onClick={() => syncMacroMutation.mutate()}>
            매크로 동기화
          </button>
        </section>
      )}

      {message && <p className="notice">{message}</p>}
      {isLoading && <p className="notice">수집 데이터를 불러오는 중입니다.</p>}
      {isError && <p className="notice error">수집 데이터 API에 연결할 수 없습니다.</p>}

      {view === "news" && <NewsTable rows={newsQuery.data ?? []} />}
      {view === "disclosures" && <DisclosureTable rows={disclosuresQuery.data ?? []} />}
      {view === "macro" && <MacroTable rows={macroQuery.data ?? []} />}
      {view === "fundamentals" && <FundamentalTable rows={fundamentalsQuery.data ?? []} />}
    </main>
  );
}

function NewsTable({ rows }: { rows: NewsArticle[] }) {
  if (rows.length === 0) {
    return <EmptyState text="뉴스 데이터가 없습니다. 동기화 버튼을 눌러 수집하세요." />;
  }

  return (
    <section className="table-panel">
      <table>
        <thead>
          <tr>
            <th>발행</th>
            <th>종목</th>
            <th>제목</th>
            <th>감성</th>
            <th>출처</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.articleKey}>
              <td>{formatDateTime(row.publishedAt)}</td>
              <td>
                <strong>{row.ticker ?? "-"}</strong>
                <span className="subtle-text">{row.market ?? "-"}</span>
              </td>
              <td>
                <a href={row.url} rel="noreferrer" target="_blank">
                  {row.title}
                </a>
              </td>
              <td>{formatNumber(row.sentimentScore)}</td>
              <td>{row.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function DisclosureTable({ rows }: { rows: DisclosureEvent[] }) {
  if (rows.length === 0) {
    return <EmptyState text="공시 데이터가 없습니다. DART 키 또는 SEC 연결 상태를 확인한 뒤 동기화하세요." />;
  }

  return (
    <section className="table-panel">
      <table>
        <thead>
          <tr>
            <th>공시일</th>
            <th>종목</th>
            <th>공시명</th>
            <th>유형</th>
            <th>중요도</th>
            <th>출처</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.disclosureKey}>
              <td>{formatDateTime(row.disclosedAt)}</td>
              <td>
                <strong>{row.ticker ?? "-"}</strong>
                <span className="subtle-text">{row.market ?? "-"}</span>
              </td>
              <td>
                {row.url ? (
                  <a href={row.url} rel="noreferrer" target="_blank">
                    {row.title}
                  </a>
                ) : (
                  row.title
                )}
              </td>
              <td>{row.disclosureType ?? "-"}</td>
              <td>
                <span className={getImportanceClass(row.importanceScore)}>{row.importanceScore ?? "-"}</span>
              </td>
              <td>{row.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function getImportanceClass(score: number | null) {
  if (score === null) {
    return "score-pill muted";
  }

  if (score >= 85) {
    return "score-pill warn";
  }

  if (score >= 70) {
    return "score-pill";
  }

  return "score-pill muted";
}

function MacroTable({ rows }: { rows: MacroObservation[] }) {
  if (rows.length === 0) {
    return <EmptyState text="매크로 데이터가 없습니다. 동기화 버튼을 눌러 FRED 관측값을 수집하세요." />;
  }

  return (
    <section className="table-panel">
      <table>
        <thead>
          <tr>
            <th>관측일</th>
            <th>지표</th>
            <th>값</th>
            <th>출처</th>
            <th>수집 시각</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.observationKey}>
              <td>{formatDate(row.observedDate)}</td>
              <td>
                <strong>{row.seriesId}</strong>
                <span className="subtle-text">{row.seriesName}</span>
              </td>
              <td>{formatNumber(row.observedValue)}</td>
              <td>{row.source}</td>
              <td>{formatDateTime(row.fetchedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function FundamentalTable({ rows }: { rows: FundamentalMetric[] }) {
  if (rows.length === 0) {
    return <EmptyState text="펀더멘털 데이터가 없습니다. 미국 종목 ticker를 입력한 뒤 동기화하세요." />;
  }

  return (
    <section className="table-panel">
      <table>
        <thead>
          <tr>
            <th>기간</th>
            <th>종목</th>
            <th>지표</th>
            <th>값</th>
            <th>회계기간</th>
            <th>출처</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.metricKey}>
              <td>{row.periodEnd ? formatDate(row.periodEnd) : "-"}</td>
              <td>
                <strong>{row.ticker}</strong>
                <span className="subtle-text">{row.market}</span>
              </td>
              <td>{row.metricName}</td>
              <td>
                {formatNumber(row.metricValue)}
                <span className="subtle-text">{row.unit ?? ""}</span>
              </td>
              <td>
                {row.fiscalYear ?? "-"}
                <span className="subtle-text">{row.fiscalPeriod ?? "-"}</span>
              </td>
              <td>{row.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <section className="empty-state">
      <h2>표시할 데이터가 없습니다.</h2>
      <p>{text}</p>
    </section>
  );
}
