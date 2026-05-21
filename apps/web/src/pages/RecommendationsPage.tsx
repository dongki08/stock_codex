import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { createEvaluation, getEvaluations } from "../api/evaluations";
import { generateDevRecommendations, getRecommendations, Recommendation, RecommendationStatus, updateRecommendationStatus } from "../api/recommendations";

type EvaluationForm = {
  actualExitPrice: string;
  exitReason: "TARGET_HIT" | "STOP_HIT" | "TIME_OUT" | "MANUAL_CLOSE";
  pnlPct: string;
  drawdownPct: string;
  hitTarget: boolean;
  closeStatus: "CLOSED" | "EXPIRED";
};

const DEFAULT_EVALUATION_FORM: EvaluationForm = {
  actualExitPrice: "",
  exitReason: "TARGET_HIT",
  pnlPct: "",
  drawdownPct: "",
  hitTarget: true,
  closeStatus: "CLOSED"
};

function formatPrice(value: number, market: string) {
  const currency = market === "KOSPI" || market === "KOSDAQ" ? "KRW" : "USD";

  return new Intl.NumberFormat("ko-KR", {
    currency,
    maximumFractionDigits: market === "KOSPI" || market === "KOSDAQ" ? 0 : 2,
    style: "currency"
  }).format(value);
}

function parseSignals(recommendation: Recommendation) {
  try {
    return JSON.parse(recommendation.signalsJson) as Record<string, unknown>;
  } catch {
    return {};
  }
}

export function RecommendationsPage() {
  const queryClient = useQueryClient();
  const [generateMarket, setGenerateMarket] = useState("KOSPI");
  const [statusFilter, setStatusFilter] = useState<RecommendationStatus>("OPEN");
  const [marketFilter, setMarketFilter] = useState("ALL");
  const [tickerFilter, setTickerFilter] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [selectedRecommendationId, setSelectedRecommendationId] = useState<number | null>(null);
  const [evaluationForm, setEvaluationForm] = useState<EvaluationForm>(DEFAULT_EVALUATION_FORM);

  const recommendationsQuery = useQuery({
    queryKey: ["recommendations", statusFilter, tickerFilter],
    queryFn: () => getRecommendations({ status: statusFilter, ticker: tickerFilter })
  });

  const generateMutation = useMutation({
    mutationFn: () => generateDevRecommendations(generateMarket),
    onSuccess: (result) => {
      setMessage(`${result.market} 기준 추천 ${result.generatedRecommendationCount}건과 예측 ${result.generatedPredictionCount}건을 생성했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["recommendations"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "개발용 추천 생성에 실패했습니다.");
    }
  });

  const evaluationsQuery = useQuery({
    queryKey: ["evaluations", selectedRecommendationId],
    queryFn: () => getEvaluations(selectedRecommendationId ?? undefined),
    enabled: selectedRecommendationId !== null
  });

  const evaluateMutation = useMutation({
    mutationFn: async (recommendation: Recommendation) => {
      const actualExitPrice = evaluationForm.actualExitPrice === "" ? null : Number(evaluationForm.actualExitPrice);
      const drawdownPct = evaluationForm.drawdownPct === "" ? null : Number(evaluationForm.drawdownPct);

      await createEvaluation({
        recommendationId: recommendation.id,
        actualExitPrice,
        exitReason: evaluationForm.exitReason,
        pnlPct: Number(evaluationForm.pnlPct),
        drawdownPct,
        hitTarget: evaluationForm.hitTarget
      });

      return updateRecommendationStatus(recommendation.id, evaluationForm.closeStatus);
    },
    onSuccess: (_result, recommendation) => {
      setMessage(`${recommendation.ticker} 추천 평가를 저장하고 상태를 ${evaluationForm.closeStatus}로 변경했습니다.`);
      setSelectedRecommendationId(null);
      setEvaluationForm(DEFAULT_EVALUATION_FORM);
      queryClient.invalidateQueries({ queryKey: ["recommendations"] });
      queryClient.invalidateQueries({ queryKey: ["evaluations", recommendation.id] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "추천 평가 저장에 실패했습니다.");
    }
  });

  const recommendations = useMemo(() => {
    const data = recommendationsQuery.data ?? [];

    if (marketFilter === "ALL") {
      return data;
    }

    return data.filter((recommendation) => recommendation.market === marketFilter);
  }, [marketFilter, recommendationsQuery.data]);

  const grouped = useMemo(
    () => ({
      long: recommendations.filter((recommendation) => recommendation.term === "LONG"),
      short: recommendations.filter((recommendation) => recommendation.term === "SHORT")
    }),
    [recommendations]
  );

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Recommendations</p>
          <h1>오늘의 추천</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar">
        <div>
          <strong>{recommendations.length}</strong>
          <span>개 추천</span>
        </div>
        <label className="inline-field">
          <span>생성 시장</span>
          <select value={generateMarket} onChange={(event) => setGenerateMarket(event.target.value)}>
            <option value="KOSPI">KOSPI</option>
            <option value="KOSDAQ">KOSDAQ</option>
            <option value="NYSE">NYSE</option>
            <option value="NASDAQ">NASDAQ</option>
          </select>
        </label>
        <button disabled={generateMutation.isPending} type="button" onClick={() => generateMutation.mutate()}>
          개발용 추천 생성
        </button>
      </section>

      <section className="filters recommendation-filters">
        <div className="tabs" role="tablist" aria-label="추천 상태">
          {["ALL", "OPEN", "CLOSED", "EXPIRED"].map((status) => (
            <button className={statusFilter === status ? "active" : ""} key={status} type="button" onClick={() => setStatusFilter(status)}>
              {status === "ALL" ? "전체" : status}
            </button>
          ))}
        </div>
        <label className="inline-field">
          <span>시장</span>
          <select value={marketFilter} onChange={(event) => setMarketFilter(event.target.value)}>
            <option value="ALL">전체</option>
            <option value="KOSPI">KOSPI</option>
            <option value="KOSDAQ">KOSDAQ</option>
            <option value="NYSE">NYSE</option>
            <option value="NASDAQ">NASDAQ</option>
          </select>
        </label>
        <input
          aria-label="추천 종목 검색"
          placeholder="종목 코드 검색"
          type="search"
          value={tickerFilter}
          onChange={(event) => setTickerFilter(event.target.value)}
        />
      </section>

      {message && <p className="notice">{message}</p>}
      {recommendationsQuery.isLoading && <p className="notice">추천을 불러오는 중입니다.</p>}
      {recommendationsQuery.isError && <p className="notice error">추천 API에 연결할 수 없습니다.</p>}

      {recommendations.length === 0 && !recommendationsQuery.isLoading && (
        <section className="empty-state">
          <h2>표시할 추천이 없습니다.</h2>
          <p>시장 후보군 화면에서 개발용 후보군을 생성한 뒤 개발용 추천 생성 버튼을 누르세요.</p>
        </section>
      )}

      {grouped.short.length > 0 && (
        <section className="recommendation-section">
          <header>
            <h2>단기 추천</h2>
            <span>1일에서 4주 보유 가정</span>
          </header>
          <div className="recommendation-grid">
            {grouped.short.map((recommendation) => (
              <RecommendationCard
                key={recommendation.id}
                evaluationForm={evaluationForm}
                evaluations={selectedRecommendationId === recommendation.id ? evaluationsQuery.data ?? [] : []}
                isEvaluating={evaluateMutation.isPending}
                isLoadingEvaluations={selectedRecommendationId === recommendation.id && evaluationsQuery.isLoading}
                isSelected={selectedRecommendationId === recommendation.id}
                recommendation={recommendation}
                onChangeEvaluationForm={setEvaluationForm}
                onSelect={() => {
                  setSelectedRecommendationId(selectedRecommendationId === recommendation.id ? null : recommendation.id);
                  setEvaluationForm(DEFAULT_EVALUATION_FORM);
                }}
                onSubmitEvaluation={() => evaluateMutation.mutate(recommendation)}
              />
            ))}
          </div>
        </section>
      )}

      {grouped.long.length > 0 && (
        <section className="recommendation-section">
          <header>
            <h2>장기 추천</h2>
            <span>3개월 이상 보유 가정</span>
          </header>
          <div className="recommendation-grid">
            {grouped.long.map((recommendation) => (
              <RecommendationCard
                key={recommendation.id}
                evaluationForm={evaluationForm}
                evaluations={selectedRecommendationId === recommendation.id ? evaluationsQuery.data ?? [] : []}
                isEvaluating={evaluateMutation.isPending}
                isLoadingEvaluations={selectedRecommendationId === recommendation.id && evaluationsQuery.isLoading}
                isSelected={selectedRecommendationId === recommendation.id}
                recommendation={recommendation}
                onChangeEvaluationForm={setEvaluationForm}
                onSelect={() => {
                  setSelectedRecommendationId(selectedRecommendationId === recommendation.id ? null : recommendation.id);
                  setEvaluationForm(DEFAULT_EVALUATION_FORM);
                }}
                onSubmitEvaluation={() => evaluateMutation.mutate(recommendation)}
              />
            ))}
          </div>
        </section>
      )}
    </main>
  );
}

function RecommendationCard({
  evaluationForm,
  evaluations,
  isEvaluating,
  isLoadingEvaluations,
  isSelected,
  recommendation,
  onChangeEvaluationForm,
  onSelect,
  onSubmitEvaluation
}: {
  evaluationForm: EvaluationForm;
  evaluations: {
    id: number;
    actualExitPrice: number | null;
    exitReason: string;
    pnlPct: number;
    drawdownPct: number | null;
    hitTarget: boolean;
    evaluatedAt: string;
  }[];
  isEvaluating: boolean;
  isLoadingEvaluations: boolean;
  isSelected: boolean;
  recommendation: Recommendation;
  onChangeEvaluationForm: (form: EvaluationForm) => void;
  onSelect: () => void;
  onSubmitEvaluation: () => void;
}) {
  const signals = parseSignals(recommendation);
  const canSubmit = evaluationForm.pnlPct.trim() !== "" && !Number.isNaN(Number(evaluationForm.pnlPct));

  return (
    <article className="recommendation-card">
      <header>
        <div>
          <span className="badge">{recommendation.market}</span>
          <h3>{recommendation.ticker}</h3>
        </div>
        <strong>{recommendation.confidence}%</strong>
      </header>
      <dl>
        <div>
          <dt>진입가</dt>
          <dd>{formatPrice(recommendation.entryPrice, recommendation.market)}</dd>
        </div>
        <div>
          <dt>목표가</dt>
          <dd>{formatPrice(recommendation.targetPrice, recommendation.market)}</dd>
        </div>
        <div>
          <dt>손절가</dt>
          <dd>{formatPrice(recommendation.stopPrice, recommendation.market)}</dd>
        </div>
        <div>
          <dt>예상 매도일</dt>
          <dd>{recommendation.expectedExitAt}</dd>
        </div>
      </dl>
      <footer>
        <span>{recommendation.modelVersion}</span>
        <span>{String(signals.reason ?? recommendation.status)}</span>
      </footer>
      <div className="card-actions">
        <button className="secondary" type="button" onClick={onSelect}>
          {isSelected ? "평가 닫기" : "평가"}
        </button>
      </div>
      {isSelected && (
        <section className="evaluation-panel">
          <header>
            <h4>추천 평가</h4>
            <span>추천 ID {recommendation.id}</span>
          </header>
          <div className="evaluation-history">
            {isLoadingEvaluations && <p>평가 이력을 불러오는 중입니다.</p>}
            {!isLoadingEvaluations && evaluations.length === 0 && <p>아직 저장된 평가가 없습니다.</p>}
            {evaluations.map((evaluation) => (
              <div key={evaluation.id} className="evaluation-item">
                <strong>{evaluation.pnlPct}%</strong>
                <span>{evaluation.exitReason}</span>
                <span>{evaluation.hitTarget ? "목표 도달" : "목표 미도달"}</span>
              </div>
            ))}
          </div>
          <form
            className="evaluation-form"
            onSubmit={(event) => {
              event.preventDefault();
              onSubmitEvaluation();
            }}
          >
            <label>
              <span>실제 매도가</span>
              <input
                min="0"
                step="0.01"
                type="number"
                value={evaluationForm.actualExitPrice}
                onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, actualExitPrice: event.target.value })}
              />
            </label>
            <label>
              <span>청산 사유</span>
              <select value={evaluationForm.exitReason} onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, exitReason: event.target.value as EvaluationForm["exitReason"] })}>
                <option value="TARGET_HIT">목표가 도달</option>
                <option value="STOP_HIT">손절가 도달</option>
                <option value="TIME_OUT">기간 만료</option>
                <option value="MANUAL_CLOSE">수동 종료</option>
              </select>
            </label>
            <label>
              <span>손익률(%)</span>
              <input
                required
                step="0.01"
                type="number"
                value={evaluationForm.pnlPct}
                onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, pnlPct: event.target.value })}
              />
            </label>
            <label>
              <span>최대 낙폭(%)</span>
              <input
                step="0.01"
                type="number"
                value={evaluationForm.drawdownPct}
                onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, drawdownPct: event.target.value })}
              />
            </label>
            <label>
              <span>변경 상태</span>
              <select value={evaluationForm.closeStatus} onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, closeStatus: event.target.value as EvaluationForm["closeStatus"] })}>
                <option value="CLOSED">종료</option>
                <option value="EXPIRED">만료</option>
              </select>
            </label>
            <label className="check-field compact">
              <input checked={evaluationForm.hitTarget} type="checkbox" onChange={(event) => onChangeEvaluationForm({ ...evaluationForm, hitTarget: event.target.checked })} />
              <span>목표 도달</span>
            </label>
            <button disabled={!canSubmit || isEvaluating} type="submit">
              평가 저장
            </button>
          </form>
        </section>
      )}
    </article>
  );
}
