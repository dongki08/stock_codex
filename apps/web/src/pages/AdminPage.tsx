const settings = [
  { key: "recommendation.short.count", label: "단기 추천 개수", value: "{\"value\":3}" },
  { key: "recommendation.long.count", label: "장기 추천 개수", value: "{\"value\":3}" },
  { key: "codex.daily.callLimit", label: "Codex 일 호출 한도", value: "{\"value\":200}" }
];

export function AdminPage() {
  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Stock Advisor</p>
          <h1>관리자 설정</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8080/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar">
        <span>API 계약 기준 UI 골격</span>
        <span>백엔드 연결 후 OpenAPI 타입으로 교체</span>
      </section>

      <section className="settings-grid">
        {settings.map((setting) => (
          <article className="setting-row" key={setting.key}>
            <div>
              <h2>{setting.label}</h2>
              <p>{setting.key}</p>
            </div>
            <textarea defaultValue={setting.value} spellCheck={false} />
            <button type="button">저장</button>
          </article>
        ))}
      </section>
    </main>
  );
}
