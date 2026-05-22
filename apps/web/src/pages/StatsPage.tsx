import { useQuery } from "@tanstack/react-query";
import {
  fetchStatsByStrategy,
  fetchStatsDaily,
  fetchStatsSummary,
  type StatsDailyResponse,
} from "../api/stats";

function numberValue(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function pnlColor(value: number) {
  if (value > 0) return { color: "#16a34a" };
  if (value < 0) return { color: "#dc2626" };
  return {};
}

function pnlText(value: number) {
  return (value > 0 ? "+" : "") + Number(value).toFixed(2) + "%";
}

function RoiChart({ daily }: { daily?: StatsDailyResponse[] }) {
  const source = [...(daily ?? [])].sort((a, b) => a.date.localeCompare(b.date));

  if (source.length === 0) {
    return <div className="roi-chart-empty">평가 데이터 없음</div>;
  }

  const width = 640;
  const height = 220;
  const padX = 44;
  const padTop = 18;
  const padBottom = 36;
  const values = source.map((d) =>
    numberValue(d.cumulativePnlPct, numberValue(d.avgPnlPct)),
  );
  const minValue = Math.min(0, ...values);
  const maxValue = Math.max(0, ...values);
  const range = maxValue - minValue || 1;
  const plotHeight = height - padTop - padBottom;
  const plotWidth = width - padX * 2;
  const points = source.map((d, index) => {
    const value = numberValue(d.cumulativePnlPct, numberValue(d.avgPnlPct));
    const x =
      source.length === 1 ? width / 2 : padX + (plotWidth * index) / (source.length - 1);
    const y = padTop + ((maxValue - value) / range) * plotHeight;
    return { date: d.date, value, x, y };
  });
  const linePoints = points.map((p) => `${p.x},${p.y}`).join(" ");
  const zeroY = padTop + ((maxValue - 0) / range) * plotHeight;
  const first = source[0];
  const last = source[source.length - 1];

  return (
    <div className="roi-chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="누적 ROI 차트">
        <line className="roi-axis" x1={padX} y1={zeroY} x2={width - padX} y2={zeroY} />
        <line className="roi-grid" x1={padX} y1={padTop} x2={width - padX} y2={padTop} />
        <line
          className="roi-grid"
          x1={padX}
          y1={height - padBottom}
          x2={width - padX}
          y2={height - padBottom}
        />
        <text className="roi-label" x={8} y={padTop + 4}>
          {pnlText(maxValue)}
        </text>
        <text className="roi-label" x={8} y={height - padBottom + 4}>
          {pnlText(minValue)}
        </text>
        <polyline className="roi-line" points={linePoints} />
        {points.map((p) => (
          <circle key={p.date} className="roi-point" cx={p.x} cy={p.y} r="3">
            <title>
              {p.date} {pnlText(p.value)}
            </title>
          </circle>
        ))}
        <text className="roi-date" x={padX} y={height - 10}>
          {first.date}
        </text>
        <text className="roi-date" x={width - padX} y={height - 10} textAnchor="end">
          {last.date}
        </text>
      </svg>
    </div>
  );
}

export function StatsPage() {
  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ["stats-summary"],
    queryFn: fetchStatsSummary,
  });

  const { data: daily, isLoading: dailyLoading } = useQuery({
    queryKey: ["stats-daily"],
    queryFn: fetchStatsDaily,
  });

  const { data: byStrategy, isLoading: strategyLoading } = useQuery({
    queryKey: ["stats-by-strategy"],
    queryFn: fetchStatsByStrategy,
  });

  return (
    <div className="page">
      <h2>성과 통계</h2>

      {summaryLoading ? (
        <p>로딩 중...</p>
      ) : summary ? (
        <>
          <section className="stats-cards">
            <div className="stat-card">
              <div className="stat-label">전체 추천</div>
              <div className="stat-value">{summary.total}</div>
              <div className="stat-sub">
                OPEN {summary.open} · CLOSED {summary.closed} · EXPIRED{" "}
                {summary.expired}
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-label">적중률</div>
              <div className="stat-value">{summary.hitRate.toFixed(1)}%</div>
              <div className="stat-sub">평가된 추천 기준</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">평균 손익률</div>
              <div
                className="stat-value"
                style={pnlColor(Number(summary.avgPnlPct))}
              >
                {pnlText(Number(summary.avgPnlPct))}
              </div>
              <div className="stat-sub">
                누적 {pnlText(Number(summary.totalPnlPct))}
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-label">최대 낙폭</div>
              <div
                className="stat-value"
                style={pnlColor(Number(summary.maxDrawdownPct))}
              >
                {pnlText(Number(summary.maxDrawdownPct))}
              </div>
              <div className="stat-sub">평가 종목 기준</div>
            </div>
          </section>

          <div className="stats-row">
            <section className="stats-section">
              <h3>기간별 성과</h3>
              <table className="stats-table">
                <thead>
                  <tr>
                    <th>구분</th>
                    <th>건수</th>
                    <th>적중률</th>
                    <th>평균 손익</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(summary.byTerm).map(([term, s]) => (
                    <tr key={term}>
                      <td>{term}</td>
                      <td>{s.count}</td>
                      <td>{s.hitRate.toFixed(1)}%</td>
                      <td style={pnlColor(Number(s.avgPnlPct))}>
                        {pnlText(Number(s.avgPnlPct))}
                      </td>
                    </tr>
                  ))}
                  {Object.keys(summary.byTerm).length === 0 && (
                    <tr>
                      <td colSpan={4} style={{ textAlign: "center", color: "#888" }}>
                        평가 데이터 없음
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </section>

            <section className="stats-section">
              <h3>청산 사유</h3>
              <table className="stats-table">
                <thead>
                  <tr>
                    <th>사유</th>
                    <th>건수</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(summary.byExitReason).map(([reason, count]) => (
                    <tr key={reason}>
                      <td>{reason}</td>
                      <td>{count}</td>
                    </tr>
                  ))}
                  {Object.keys(summary.byExitReason).length === 0 && (
                    <tr>
                      <td colSpan={2} style={{ textAlign: "center", color: "#888" }}>
                        평가 데이터 없음
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </section>
          </div>
        </>
      ) : (
        <p>데이터를 불러올 수 없습니다.</p>
      )}

      <section className="stats-section">
        <h3>누적 ROI (최근 30일)</h3>
        {dailyLoading ? <p>로딩 중...</p> : <RoiChart daily={daily} />}
      </section>

      <section className="stats-section">
        <h3>일별 이력 (최근 30일)</h3>
        {dailyLoading ? (
          <p>로딩 중...</p>
        ) : (
          <table className="stats-table">
            <thead>
              <tr>
                <th>날짜</th>
                <th>평가 건수</th>
                <th>적중</th>
                <th>평균 손익</th>
                <th>일별 손익</th>
                <th>누적 손익</th>
              </tr>
            </thead>
            <tbody>
              {daily && daily.length > 0 ? (
                daily.map((d) => (
                  <tr key={d.date}>
                    <td>{d.date}</td>
                    <td>{d.count}</td>
                    <td>{d.hitCount}</td>
                    <td style={pnlColor(Number(d.avgPnlPct))}>
                      {pnlText(Number(d.avgPnlPct))}
                    </td>
                    <td style={pnlColor(numberValue(d.totalPnlPct))}>
                      {pnlText(numberValue(d.totalPnlPct, numberValue(d.avgPnlPct)))}
                    </td>
                    <td style={pnlColor(numberValue(d.cumulativePnlPct))}>
                      {pnlText(numberValue(d.cumulativePnlPct, numberValue(d.avgPnlPct)))}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", color: "#888" }}>
                    평가 데이터 없음
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>

      <section className="stats-section">
        <h3>전략별 성과</h3>
        {strategyLoading ? (
          <p>로딩 중...</p>
        ) : (
          <table className="stats-table">
            <thead>
              <tr>
                <th>모델 버전</th>
                <th>건수</th>
                <th>적중률</th>
                <th>평균 손익</th>
              </tr>
            </thead>
            <tbody>
              {byStrategy && byStrategy.length > 0 ? (
                byStrategy.map((s) => (
                  <tr key={s.modelVersion}>
                    <td>{s.modelVersion}</td>
                    <td>{s.count}</td>
                    <td>{s.hitRate.toFixed(1)}%</td>
                    <td style={pnlColor(Number(s.avgPnlPct))}>
                      {pnlText(Number(s.avgPnlPct))}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4} style={{ textAlign: "center", color: "#888" }}>
                    평가 데이터 없음
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
