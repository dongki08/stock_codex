import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { BacktestRun, fetchBacktests, simulateBacktest } from "../api/backtests";

const markets = ["ALL", "KOSPI", "KOSDAQ", "NASDAQ", "NYSE"];

type BacktestMetrics = {
  tradeCount?: number;
  hitRate?: number;
  avgPnlPct?: number;
  totalPnlPct?: number;
  maxDrawdownPct?: number;
  targetHits?: number;
  stopHits?: number;
  timeExits?: number;
  priceRows?: number;
  sampleTrades?: Array<{
    ticker: string;
    market: string;
    entryDate: string;
    exitDate: string;
    pnlPct: number;
    exitReason: string;
  }>;
};

function parseMetrics(run: BacktestRun): BacktestMetrics {
  try {
    return JSON.parse(run.metricsJson) as BacktestMetrics;
  } catch {
    return {};
  }
}

function formatPct(value: number | undefined) {
  if (typeof value !== "number") {
    return "-";
  }
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}%`;
}

function pnlColor(value: number | undefined) {
  if (typeof value !== "number") return {};
  if (value > 0) return { color: "#16a34a" };
  if (value < 0) return { color: "#dc2626" };
  return {};
}

export function BacktestsPage() {
  const queryClient = useQueryClient();
  const [strategy, setStrategy] = useState("ma20-breakout-v0");
  const [market, setMarket] = useState("ALL");
  const [periodFrom, setPeriodFrom] = useState("2025-01-01");
  const [periodTo, setPeriodTo] = useState("2025-12-31");
  const [maxTickers, setMaxTickers] = useState("30");
  const [holdingDays, setHoldingDays] = useState("20");
  const [targetPct, setTargetPct] = useState("3");
  const [stopPct, setStopPct] = useState("2");
  const [message, setMessage] = useState<string | null>(null);

  const runsQuery = useQuery({
    queryKey: ["backtests"],
    queryFn: fetchBacktests
  });

  const latestRun = useMemo(() => runsQuery.data?.[0], [runsQuery.data]);
  const latestMetrics = latestRun ? parseMetrics(latestRun) : null;

  const simulateMutation = useMutation({
    mutationFn: () =>
      simulateBacktest({
        holdingDays,
        market,
        maxTickers,
        periodFrom,
        periodTo,
        stopPct,
        strategy,
        targetPct
      }),
    onSuccess: (run) => {
      setMessage(`백테스트 #${run.id} 실행 결과를 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["backtests"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "백테스트 실행에 실패했습니다.");
    }
  });

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Backtest</p>
          <h1>백테스트</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="form-panel">
        <div className="form-grid compact-grid">
          <label>
            전략
            <input value={strategy} onChange={(event) => setStrategy(event.target.value)} />
          </label>
          <label>
            시장
            <select value={market} onChange={(event) => setMarket(event.target.value)}>
              {markets.map((option) => (
                <option key={option} value={option}>
                  {option === "ALL" ? "전체" : option}
                </option>
              ))}
            </select>
          </label>
          <label>
            시작일
            <input type="date" value={periodFrom} onChange={(event) => setPeriodFrom(event.target.value)} />
          </label>
          <label>
            종료일
            <input type="date" value={periodTo} onChange={(event) => setPeriodTo(event.target.value)} />
          </label>
          <label>
            종목 수
            <input min="1" max="300" type="number" value={maxTickers} onChange={(event) => setMaxTickers(event.target.value)} />
          </label>
          <label>
            보유일
            <input min="1" max="120" type="number" value={holdingDays} onChange={(event) => setHoldingDays(event.target.value)} />
          </label>
          <label>
            목표 %
            <input min="0.1" max="50" step="0.1" type="number" value={targetPct} onChange={(event) => setTargetPct(event.target.value)} />
          </label>
          <label>
            손절 %
            <input min="0.1" max="50" step="0.1" type="number" value={stopPct} onChange={(event) => setStopPct(event.target.value)} />
          </label>
        </div>
        <div className="form-actions">
          <button disabled={simulateMutation.isPending} type="button" onClick={() => simulateMutation.mutate()}>
            백테스트 실행
          </button>
        </div>
      </section>

      {message && <p className="notice">{message}</p>}
      {runsQuery.isError && <p className="notice error">백테스트 API에 연결할 수 없습니다.</p>}

      {latestRun && latestMetrics && (
        <section className="stats-cards">
          <div className="stat-card">
            <div className="stat-label">최근 실행</div>
            <div className="stat-value">#{latestRun.id}</div>
            <div className="stat-sub">{latestRun.strategy}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">거래 수</div>
            <div className="stat-value">{latestMetrics.tradeCount ?? "-"}</div>
            <div className="stat-sub">일봉 {latestMetrics.priceRows ?? "-"}건</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">적중률</div>
            <div className="stat-value">{formatPct(latestMetrics.hitRate)}</div>
            <div className="stat-sub">목표 {latestMetrics.targetHits ?? 0} / 손절 {latestMetrics.stopHits ?? 0} / 만료 {latestMetrics.timeExits ?? 0}</div>
          </div>
          <div className="stat-card">
            <div className="stat-label">평균 손익</div>
            <div className="stat-value" style={pnlColor(latestMetrics.avgPnlPct)}>
              {formatPct(latestMetrics.avgPnlPct)}
            </div>
            <div className="stat-sub">누적 {formatPct(latestMetrics.totalPnlPct)} / MDD {formatPct(latestMetrics.maxDrawdownPct)}</div>
          </div>
        </section>
      )}

      <section className="table-panel">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>전략</th>
              <th>기간</th>
              <th>거래</th>
              <th>적중률</th>
              <th>평균 손익</th>
              <th>MDD</th>
            </tr>
          </thead>
          <tbody>
            {runsQuery.data?.map((run) => {
              const metrics = parseMetrics(run);
              return (
                <tr key={run.id}>
                  <td>#{run.id}</td>
                  <td>{run.strategy}</td>
                  <td>
                    {run.periodFrom} ~ {run.periodTo}
                  </td>
                  <td>{metrics.tradeCount ?? "-"}</td>
                  <td>{formatPct(metrics.hitRate)}</td>
                  <td style={pnlColor(metrics.avgPnlPct)}>{formatPct(metrics.avgPnlPct)}</td>
                  <td style={pnlColor(metrics.maxDrawdownPct)}>{formatPct(metrics.maxDrawdownPct)}</td>
                </tr>
              );
            })}
            {!runsQuery.isLoading && (!runsQuery.data || runsQuery.data.length === 0) && (
              <tr>
                <td colSpan={7} style={{ color: "#66788a", textAlign: "center" }}>
                  백테스트 이력이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </main>
  );
}
