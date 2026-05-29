# stock_codex — Codex/에이전트 부트 컨텍스트

> Codex CLI는 cwd부터 상위로 `AGENTS.md`를 자동 로드한다. 이 repo 안에서 실행하면 이 파일이 잡힌다.
> **내용은 [CLAUDE.md](CLAUDE.md)와 동일하게 유지한다.** 한쪽 고치면 양쪽 맞출 것(단일출처: docs).
> **세션 시작 시 먼저 [docs/00-INDEX.md](docs/00-INDEX.md)를 읽어라.**

## 프로젝트 한 줄

개인용 일일 주식 추천 시스템. 매일 장 전 단기/장기 추천 + 목표가/손절가, 사후 평가·자기개선(AutoResearch) 루프.

## 스택 / 위치

- 백엔드: `apps/backend` — Spring Boot 3.3 · Java 21 · **MSSQL** · Flyway · BasicAuth · 8083
- 프론트: `apps/web` — React 19 · Vite · 5173
- 실행/셋업: [README.md](README.md), [docs/12-SETUP-CHECKLIST.md](docs/12-SETUP-CHECKLIST.md)

## 목적별 진입 문서

| 목적 | 문서 |
|---|---|
| 구조 빠르게 | [docs/30-BACKEND-OVERVIEW.md](docs/30-BACKEND-OVERVIEW.md) |
| 코드 깊이 | [docs/31-BACKEND-DEEP-DIVE.md](docs/31-BACKEND-DEEP-DIVE.md) |
| API | [docs/21-API-QUICK.md](docs/21-API-QUICK.md) → [docs/20-API.md](docs/20-API.md) |
| 진행상황/남은 작업 | [docs/11-ROADMAP.md](docs/11-ROADMAP.md) (§0 = 최신 델타) |
| 수익률 개선 착수 | [docs/40-RETURN-STRATEGY.md](docs/40-RETURN-STRATEGY.md) |
| 결함 이력 | [docs/41-DEFECTS-AND-FIXES.md](docs/41-DEFECTS-AND-FIXES.md) |

## 반드시 아는 사실

- 공통 응답 `ResultDto<T>` = `{ code, data, error_message }`. `{success,...}` 아님.
- **ExitConfirm 제거됨**(V8). Exit = `ExitMonitorJob` 룰(목표가/손절가/만료) 자동청산.
- Flyway 최신 V8. 마이그레이션 추가만, `ddl-auto: validate`.
- 외부 키 없으면 `dev-placeholder` 폴백(Codex/KIS/DART/SEC/sentiment).
- 최대 약점: 백테스트 미래참조 → AutoResearch 루프 무효. 수익 작업은 40-RETURN-STRATEGY TASK-1·2부터.

## 문서 관리 규칙

1. 한 주제 = 한 정본, 복붙 금지·링크 참조 (전문: [docs/00-INDEX.md](docs/00-INDEX.md) §4).
2. living 문서 상태배너 유지.
3. 코드와 충돌 시 코드 우선 → 문서 고치고 날짜 갱신.
4. 파일 추가/리네임 → 00-INDEX §2 + 참조 링크 동기 수정.
5. 안 쓰는 문서 → `docs/90-archive/` 동결.

## 작업 기록 (시키지 않아도)

작업 마칠 때마다 [docs/WORKLOG.md](docs/WORKLOG.md) 맨 위에 한 블록 추가(형식 그 파일 참조). 큰 변화면 [docs/11-ROADMAP.md](docs/11-ROADMAP.md) §0에도 승격.

## 코드 스타일

가독성·구조화·모듈화 우선. 중복은 추상화. 기존 관습 따라가기.
