import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  AdminSetting,
  getAdminSettings,
  getAuditLogs,
  resetAdminSettings,
  updateAdminSetting
} from "../api/adminSettings";
import { clearCredentials, hasCredentials, setCredentials } from "../api/auth";

type DraftMap = Record<string, string>;

const groups = [
  { id: "recommendation", label: "추천", prefixes: ["recommendation."] },
  { id: "notification", label: "알림", prefixes: ["notification."] },
  { id: "exit", label: "손절", prefixes: ["exit."] },
  { id: "backtest", label: "백테스트", prefixes: ["backtest."] },
  { id: "codex", label: "Codex", prefixes: ["codex.", "autoresearch."] },
  { id: "operation", label: "운영", prefixes: ["operation."] }
];

function getGroupId(settingKey: string) {
  return groups.find((group) => group.prefixes.some((prefix) => settingKey.startsWith(prefix)))?.id ?? "etc";
}

function formatJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function LoginForm({ onLogin }: { onLogin: () => void }) {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setCredentials(username, password);
    try {
      await getAdminSettings();
      onLogin();
    } catch (err) {
      clearCredentials();
      setError("인증에 실패했습니다. 아이디/비밀번호를 확인하세요.");
    }
  }

  return (
    <div className="page">
      <h2>관리자 로그인</h2>
      <form className="login-form" onSubmit={handleSubmit}>
        <label>
          아이디
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </label>
        <label>
          비밀번호
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        </label>
        {error && <p className="error-msg">{error}</p>}
        <button type="submit">로그인</button>
      </form>
    </div>
  );
}

export function AdminPage() {
  const queryClient = useQueryClient();
  const [authed, setAuthed] = useState(hasCredentials());
  const [selectedGroup, setSelectedGroup] = useState("recommendation");
  const [keyword, setKeyword] = useState("");
  const [drafts, setDrafts] = useState<DraftMap>({});
  const [message, setMessage] = useState<string | null>(null);

  if (!authed) {
    return <LoginForm onLogin={() => setAuthed(true)} />;
  }

  const settingsQuery = useQuery({
    queryKey: ["admin-settings"],
    queryFn: getAdminSettings
  });

  const auditQuery = useQuery({
    queryKey: ["audit-logs"],
    queryFn: getAuditLogs
  });

  const resetMutation = useMutation({
    mutationFn: resetAdminSettings,
    onSuccess: (settings) => {
      setDrafts(Object.fromEntries(settings.map((setting) => [setting.key, formatJson(setting.valueJson)])));
      setMessage("기본 설정을 초기화했습니다.");
      queryClient.invalidateQueries({ queryKey: ["admin-settings"] });
      queryClient.invalidateQueries({ queryKey: ["audit-logs"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "기본 설정 초기화에 실패했습니다.");
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ key, valueJson }: { key: string; valueJson: string }) => updateAdminSetting(key, valueJson),
    onSuccess: (setting) => {
      setDrafts((current) => ({ ...current, [setting.key]: formatJson(setting.valueJson) }));
      setMessage(`${setting.description} 설정을 저장했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["admin-settings"] });
      queryClient.invalidateQueries({ queryKey: ["audit-logs"] });
    },
    onError: (error) => {
      setMessage(error instanceof Error ? error.message : "설정 저장에 실패했습니다.");
    }
  });

  const settings = settingsQuery.data ?? [];
  const auditLogs = auditQuery.data ?? [];

  const filteredSettings = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    return settings.filter((setting) => {
      const matchesGroup = selectedGroup === "all" || getGroupId(setting.key) === selectedGroup;
      const matchesKeyword =
        normalizedKeyword.length === 0 ||
        setting.key.toLowerCase().includes(normalizedKeyword) ||
        setting.description.toLowerCase().includes(normalizedKeyword);

      return matchesGroup && matchesKeyword;
    });
  }, [keyword, selectedGroup, settings]);

  const updateDraft = (key: string, value: string) => {
    setDrafts((current) => ({ ...current, [key]: value }));
  };

  const getDraftValue = (setting: AdminSetting) => drafts[setting.key] ?? formatJson(setting.valueJson);

  const saveSetting = (setting: AdminSetting) => {
    const value = getDraftValue(setting);

    try {
      JSON.parse(value);
      updateMutation.mutate({ key: setting.key, valueJson: JSON.stringify(JSON.parse(value)) });
    } catch {
      setMessage("JSON 형식이 올바르지 않습니다.");
    }
  };

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Stock Advisor</p>
          <h1>관리자 설정</h1>
        </div>
        <a className="swagger-link" href="http://localhost:8083/swagger-ui.html">
          Swagger
        </a>
      </header>

      <section className="toolbar">
        <div>
          <strong>{settings.length}</strong>
          <span>개 설정</span>
        </div>
        <div>
          <strong>{auditLogs.length}</strong>
          <span>개 변경 이력</span>
        </div>
        <button disabled={resetMutation.isPending} type="button" onClick={() => resetMutation.mutate()}>
          기본값 초기화
        </button>
      </section>

      <section className="filters">
        <div className="tabs" role="tablist" aria-label="설정 그룹">
          <button className={selectedGroup === "all" ? "active" : ""} type="button" onClick={() => setSelectedGroup("all")}>
            전체
          </button>
          {groups.map((group) => (
            <button
              className={selectedGroup === group.id ? "active" : ""}
              key={group.id}
              type="button"
              onClick={() => setSelectedGroup(group.id)}
            >
              {group.label}
            </button>
          ))}
        </div>
        <input
          aria-label="설정 검색"
          placeholder="설정명 또는 key 검색"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
      </section>

      {message && <p className="notice">{message}</p>}
      {settingsQuery.isLoading && <p className="notice">설정을 불러오는 중입니다.</p>}
      {settingsQuery.isError && <p className="notice error">백엔드 API에 연결할 수 없습니다.</p>}

      <section className="settings-grid">
        {filteredSettings.map((setting) => (
          <article className="setting-row" key={setting.key}>
            <div>
              <h2>{setting.description}</h2>
              <p>{setting.key}</p>
              <span>수정자: {setting.updatedBy}</span>
            </div>
            <textarea
              value={getDraftValue(setting)}
              spellCheck={false}
              onChange={(event) => updateDraft(setting.key, event.target.value)}
            />
            <button disabled={updateMutation.isPending} type="button" onClick={() => saveSetting(setting)}>
              저장
            </button>
          </article>
        ))}
      </section>

      <section className="audit-panel">
        <header>
          <h2>최근 변경 이력</h2>
        </header>
        <div className="audit-list">
          {auditLogs.slice(0, 8).map((log) => (
            <article key={log.id}>
              <strong>{log.action}</strong>
              <span>{log.actor}</span>
            </article>
          ))}
          {auditLogs.length === 0 && <p>변경 이력이 없습니다.</p>}
        </div>
      </section>
    </main>
  );
}
