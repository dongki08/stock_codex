import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useMemo, useState } from "react";
import { createInstrument, getInstruments, Instrument, InstrumentPayload, updateInstrument } from "../api/instruments";

const markets = ["KOSPI", "KOSDAQ", "NYSE", "NASDAQ"];

const emptyForm: InstrumentPayload = {
  enabled: true,
  market: "KOSPI",
  name: "",
  sector: "",
  ticker: ""
};

export function InstrumentsPage() {
  const queryClient = useQueryClient();
  const [market, setMarket] = useState("ALL");
  const [keyword, setKeyword] = useState("");
  const [form, setForm] = useState<InstrumentPayload>(emptyForm);
  const [editing, setEditing] = useState<Record<string, InstrumentPayload>>({});
  const [message, setMessage] = useState<string | null>(null);

  const instrumentsQuery = useQuery({
    queryKey: ["instruments", market],
    queryFn: () => getInstruments(market)
  });

  const createMutation = useMutation({
    mutationFn: createInstrument,
    onSuccess: (instrument) => {
      setMessage(`${instrument.ticker} 종목을 등록했습니다.`);
      setForm(emptyForm);
      queryClient.invalidateQueries({ queryKey: ["instruments"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "종목 등록에 실패했습니다.");
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ ticker, payload }: { ticker: string; payload: InstrumentPayload }) =>
      updateInstrument(ticker, {
        enabled: payload.enabled,
        market: payload.market,
        name: payload.name,
        sector: payload.sector
      }),
    onSuccess: (instrument) => {
      setMessage(`${instrument.ticker} 종목을 수정했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["instruments"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "종목 수정에 실패했습니다.");
    }
  });

  const instruments = instrumentsQuery.data ?? [];
  const filteredInstruments = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    return instruments.filter((instrument) => {
      if (!normalizedKeyword) {
        return true;
      }

      return (
        instrument.ticker.toLowerCase().includes(normalizedKeyword) ||
        instrument.name.toLowerCase().includes(normalizedKeyword) ||
        (instrument.sector ?? "").toLowerCase().includes(normalizedKeyword)
      );
    });
  }, [instruments, keyword]);

  const enabledCount = instruments.filter((instrument) => instrument.enabled).length;

  const submitCreate = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    createMutation.mutate({
      ...form,
      sector: form.sector.trim(),
      ticker: form.ticker.trim().toUpperCase()
    });
  };

  const startEdit = (instrument: Instrument) => {
    setEditing((current) => ({
      ...current,
      [instrument.ticker]: {
        enabled: instrument.enabled,
        market: instrument.market,
        name: instrument.name,
        sector: instrument.sector ?? "",
        ticker: instrument.ticker
      }
    }));
  };

  const updateEdit = (ticker: string, patch: Partial<InstrumentPayload>) => {
    setEditing((current) => ({
      ...current,
      [ticker]: {
        ...current[ticker],
        ...patch
      }
    }));
  };

  const saveEdit = (ticker: string) => {
    const payload = editing[ticker];

    if (!payload) {
      return;
    }

    updateMutation.mutate({ payload, ticker });
  };

  const cancelEdit = (ticker: string) => {
    setEditing((current) => {
      const next = { ...current };

      delete next[ticker];

      return next;
    });
  };

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Universe</p>
          <h1>종목 관리</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar">
        <div>
          <strong>{instruments.length}</strong>
          <span>개 종목</span>
        </div>
        <div>
          <strong>{enabledCount}</strong>
          <span>개 활성</span>
        </div>
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
      </section>

      {message && <p className="notice">{message}</p>}
      {instrumentsQuery.isLoading && <p className="notice">종목을 불러오는 중입니다.</p>}
      {instrumentsQuery.isError && <p className="notice error">종목 API에 연결할 수 없습니다.</p>}

      <section className="form-panel">
        <header>
          <h2>종목 등록</h2>
        </header>
        <form className="instrument-form" onSubmit={submitCreate}>
          <label>
            <span>종목 코드</span>
            <input
              required
              placeholder="005930"
              value={form.ticker}
              onChange={(event) => setForm((current) => ({ ...current, ticker: event.target.value }))}
            />
          </label>
          <label>
            <span>시장</span>
            <select value={form.market} onChange={(event) => setForm((current) => ({ ...current, market: event.target.value }))}>
              {markets.map((marketOption) => (
                <option key={marketOption} value={marketOption}>
                  {marketOption}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>종목명</span>
            <input
              required
              placeholder="삼성전자"
              value={form.name}
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
            />
          </label>
          <label>
            <span>섹터</span>
            <input
              placeholder="반도체"
              value={form.sector}
              onChange={(event) => setForm((current) => ({ ...current, sector: event.target.value }))}
            />
          </label>
          <label className="check-field">
            <input
              checked={form.enabled}
              type="checkbox"
              onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.checked }))}
            />
            <span>활성</span>
          </label>
          <button disabled={createMutation.isPending} type="submit">
            등록
          </button>
        </form>
      </section>

      <section className="filters">
        <input
          aria-label="종목 검색"
          placeholder="종목 코드, 이름, 섹터 검색"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
      </section>

      <section className="table-panel">
        <table>
          <thead>
            <tr>
              <th>종목</th>
              <th>시장</th>
              <th>이름</th>
              <th>섹터</th>
              <th>활성</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {filteredInstruments.map((instrument) => {
              const draft = editing[instrument.ticker];

              return (
                <tr key={instrument.ticker}>
                  <td>
                    <strong>{instrument.ticker}</strong>
                  </td>
                  <td>
                    {draft ? (
                      <select value={draft.market} onChange={(event) => updateEdit(instrument.ticker, { market: event.target.value })}>
                        {markets.map((marketOption) => (
                          <option key={marketOption} value={marketOption}>
                            {marketOption}
                          </option>
                        ))}
                      </select>
                    ) : (
                      instrument.market
                    )}
                  </td>
                  <td>
                    {draft ? (
                      <input value={draft.name} onChange={(event) => updateEdit(instrument.ticker, { name: event.target.value })} />
                    ) : (
                      instrument.name
                    )}
                  </td>
                  <td>
                    {draft ? (
                      <input value={draft.sector} onChange={(event) => updateEdit(instrument.ticker, { sector: event.target.value })} />
                    ) : (
                      instrument.sector || "-"
                    )}
                  </td>
                  <td>
                    {draft ? (
                      <input
                        checked={draft.enabled}
                        type="checkbox"
                        onChange={(event) => updateEdit(instrument.ticker, { enabled: event.target.checked })}
                      />
                    ) : (
                      <span className={instrument.enabled ? "status-pill on" : "status-pill"}>{instrument.enabled ? "활성" : "비활성"}</span>
                    )}
                  </td>
                  <td>
                    {draft ? (
                      <div className="row-actions">
                        <button disabled={updateMutation.isPending} type="button" onClick={() => saveEdit(instrument.ticker)}>
                          저장
                        </button>
                        <button className="secondary" type="button" onClick={() => cancelEdit(instrument.ticker)}>
                          취소
                        </button>
                      </div>
                    ) : (
                      <button className="secondary" type="button" onClick={() => startEdit(instrument)}>
                        수정
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {filteredInstruments.length === 0 && <p className="table-empty">표시할 종목이 없습니다.</p>}
      </section>
    </main>
  );
}
