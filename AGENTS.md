# stock_codex — 에이전트 부트 컨텍스트

> 이 파일은 cwd가 이 repo 안일 때 자동 로드된다.
> **내용은 AGENTS.md와 CLAUDE.md를 동일하게 유지한다.** 한쪽 고치면 양쪽 맞출 것.
> **세션 시작 시 먼저 [docs/00-INDEX.md](docs/00-INDEX.md)를 읽어라.** 모든 문서의 진입점이다.

## 프로젝트 한 줄

개인용 일일 주식 추천 시스템. 매일 장 전 단기/장기 추천 + 목표가/손절가 산출, 사후 평가·자기개선(AutoResearch) 루프.

## 스택 / 위치

- 백엔드: `apps/backend` — Spring Boot 3.3 · Java 21 · **MSSQL** · Flyway · Spring Security BasicAuth · 포트 8083
- 프론트: `apps/web` — React 19 · Vite · 포트 5173
- 실행/셋업: 루트 [README.md](README.md), [docs/12-SETUP-CHECKLIST.md](docs/12-SETUP-CHECKLIST.md)

## 어디를 먼저 볼까 (목적별)

| 목적 | 문서 |
|---|---|
| 구조 빠르게 | [docs/30-BACKEND-OVERVIEW.md](docs/30-BACKEND-OVERVIEW.md) |
| 코드 깊이 | [docs/31-BACKEND-DEEP-DIVE.md](docs/31-BACKEND-DEEP-DIVE.md) |
| API | [docs/21-API-QUICK.md](docs/21-API-QUICK.md) → [docs/20-API.md](docs/20-API.md) |
| **진행상황/남은 작업** | [docs/11-ROADMAP.md](docs/11-ROADMAP.md) (§0 = 최신 델타) |
| **수익률 개선 작업(착수)** | [docs/40-RETURN-STRATEGY.md](docs/40-RETURN-STRATEGY.md) |
| 결함 이력 | [docs/41-DEFECTS-AND-FIXES.md](docs/41-DEFECTS-AND-FIXES.md) |

## 반드시 아는 사실 (자주 틀리는 것)

- 공통 응답 = `ResultDto<T>` = `{ code, data, error_message }` (`global/dto/ResultDto.java`). `{success,...}` 아님.
- **ExitConfirm 기능 제거됨** (V8). Exit 판정은 `ExitMonitorJob` 룰 기반(목표가/손절가/만료) 자동청산.
- Flyway 최신 = V13. 마이그레이션은 추가만(기존 수정 금지), `ddl-auto: validate`.
- 외부 키 미설정 시 `dev-placeholder` 폴백 패턴 (Codex/KIS/DART/SEC/sentiment).
- **AutoResearch 루프**: 2026-06-02 유효화됨. 백테스트가 라이브 점수기(`buildFeatureAsOf`)로 진입 판단 → 가중치 변형이 metric에 반영. 다음 약점: raw as-of 점수 기반 replay 성능과 cross-sectional snapshot 활용 고도화.

## 문서 관리 규칙 (문서 손댈 때)

1. 한 주제 = 한 정본. 복붙 금지, 링크로 참조. (규칙 전문: [docs/00-INDEX.md](docs/00-INDEX.md) §4)
2. living 문서 H1 밑 상태배너 유지(`🧭 인덱스·카테고리·상태·코드기준 날짜`).
3. 코드와 충돌 시 코드가 정답 → 문서 고치고 배너 날짜 갱신.
4. 파일 추가/이름변경 → 00-INDEX §2 표 + 참조 링크 같이 수정.
5. 안 쓰는 문서는 `docs/90-archive/`로 이동(동결).

## 작업 기록 (시키지 않아도)

작업(코드/문서 변경)을 마칠 때마다 [docs/WORKLOG.md](docs/WORKLOG.md) **맨 위**에 한 블록 추가(형식은 그 파일 참조). 큰 구조/기능 변화면 [docs/11-ROADMAP.md](docs/11-ROADMAP.md) §0 델타에도 승격.

## 코드 스타일

가독성·구조화·모듈화 우선. 중복은 추상화. 기존 코드 관습(주석 밀도/네이밍/패턴) 따라가기.
