# 📚 Stock Advisor 문서 인덱스

> 모든 문서의 시작점. **어떤 문서를 봐야 하는지 여기서 결정.**
> 최종 정리: 2026-05-29 · 코드 기준: `apps/backend` (Spring Boot 3.3 / Java 21 / MSSQL)

---

## 1. 목적별 시작점

| 너의 목적 | 먼저 볼 문서 |
|---|---|
| 제품이 뭘 하는지 (무엇/왜) | [10-PRODUCT-SPEC](10-PRODUCT-SPEC.md) |
| 백엔드 구조 빠르게 파악 | [30-BACKEND-OVERVIEW](30-BACKEND-OVERVIEW.md) |
| 코드 깊이 이해 (도메인 지식 포함) | [31-BACKEND-DEEP-DIVE](31-BACKEND-DEEP-DIVE.md) |
| API 붙이기 (프론트/연동) | [21-API-QUICK](21-API-QUICK.md) → [20-API](20-API.md) |
| 시스템 동작 순서 | [32-SYSTEM-FLOW](32-SYSTEM-FLOW.md) |
| 로컬 실행/셋업 | [README](../README.md) → [12-SETUP-CHECKLIST](12-SETUP-CHECKLIST.md) |
| **수익률 올리는 작업 (AI 착수)** | [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md) |
| 알려진 결함/수정 이력 | [41-DEFECTS-AND-FIXES](41-DEFECTS-AND-FIXES.md) |
| 남은 작업/로드맵 | [11-ROADMAP](11-ROADMAP.md) |

---

## 2. 문서 카탈로그

| # | 문서 | 카테고리 | 역할 | 상태 |
|---|---|---|---|---|
| 00 | [00-INDEX](00-INDEX.md) | 인덱스 | 이 문서. 진입점 + 관리규칙 | 🟢 |
| 10 | [10-PRODUCT-SPEC](10-PRODUCT-SPEC.md) | 제품 | 제품 명세 v1.2 (방향/요구사항) | 🟠 스택 정정 표기됨 |
| 11 | [11-ROADMAP](11-ROADMAP.md) | 제품 | 구현 현황 + 남은 작업 | 🟢 현행(§0=05-29 델타) |
| 12 | [12-SETUP-CHECKLIST](12-SETUP-CHECKLIST.md) | 제품 | 실행 전 사용자 준비 항목 | 🟢 현행(05-29 정정) |
| 20 | [20-API](20-API.md) | API | 전체 API 상세 명세 | 🟢 |
| 21 | [21-API-QUICK](21-API-QUICK.md) | API | API 요약(빠른 참조) | 🟢 |
| 30 | [30-BACKEND-OVERVIEW](30-BACKEND-OVERVIEW.md) | 아키텍처 | 레이어/테이블/스케줄러 요약 | 🟢 현행 |
| 31 | [31-BACKEND-DEEP-DIVE](31-BACKEND-DEEP-DIVE.md) | 아키텍처 | 코드 완벽 가이드 | 🟢 2026-05-26 |
| 32 | [32-SYSTEM-FLOW](32-SYSTEM-FLOW.md) | 아키텍처 | 동작 흐름도 | 🟢 |
| 40 | [40-RETURN-STRATEGY](40-RETURN-STRATEGY.md) | 수익/품질 | 수익률 개선 작업지시서 | 🟢 신규 |
| 41 | [41-DEFECTS-AND-FIXES](41-DEFECTS-AND-FIXES.md) | 수익/품질 | 결함 목록 + 수정 이력 | 🟢 이력 |
| — | [WORKLOG](WORKLOG.md) | 메타 | 날짜별 작업 일지(에이전트 자동 기록, append-only) | 🟢 |
| 90 | [90-archive/](90-archive/) | 아카이브 | 초기 plan/design (2026-05-19, 동결) | ⚪ 동결 |

상태 범례: 🟢 현행 · 🟡 부분 스테일(날짜 확인) · 🟠 일부 정정 표기 · ⚪ 동결(갱신 안 함)

---

## 3. 번호 체계

```
00      인덱스/메타
10~19   제품 (무엇/왜) — 명세·로드맵·셋업
20~29   API 계약 (외부 인터페이스)
30~39   아키텍처 (어떻게 — 구조·코드·흐름)
40~49   수익/품질 (전략·결함·튜닝)
90~     아카이브 (동결된 과거 문서)
```

신규 문서는 해당 대역의 다음 번호로. 대역 안에서 작은 번호 = 더 요약/상위.

---

## 4. 문서 관리 규칙

1. **단일 출처(Single Source of Truth).** 한 주제는 한 문서가 정본. 같은 내용을 두 곳에 복붙 금지(루트/`docs` SPEC 중복이 이 사고였음 → 제거됨). 다른 문서는 **링크**로 참조.
2. **상태 배너 필수.** 모든 living 문서 H1 바로 아래 한 줄:
   ```
   > 🧭 인덱스: [00-INDEX.md](00-INDEX.md) · 카테고리 NN(이름) · 상태 🟢/🟡/🟠 · 코드기준 YYYY-MM-DD
   ```
3. **상대 링크 + 번호 파일명.** 문서 간 참조는 `[20-API](20-API.md)`처럼 번호 파일명 상대링크. 파일명 바꾸면 이 인덱스 + 참조 링크 같이 수정.
4. **코드와 충돌 시 코드가 정답.** 문서가 코드와 다르면 문서를 고치고, 고쳤다는 날짜를 배너에 반영. (예: 응답포맷은 `ResultDto = {code, data, error_message}` 가 정본.)
5. **스테일 표기.** 못 고친 옛 내용은 지우지 말고 `🟡`/`⚠️ 정정` 으로 표시 후 정본 문서로 링크. 독자가 함정에 안 빠지게.
6. **아카이브는 동결.** `90-archive/` 는 역사 기록. 내부 링크/내용 수정 안 함. 더 안 쓰는 문서는 여기로 이동.
7. **새 문서 만들기 전 인덱스 확인.** 이미 정본이 있으면 거기에 합치기. 정말 새 주제일 때만 신규 + 이 표에 한 줄 추가.

---

## 5. 갱신 절차 (문서 손댈 때)

1. 정본 문서 본문 수정.
2. 그 문서 배너의 `코드기준` 날짜 갱신.
3. 파일 추가/이름변경/삭제면 → 이 인덱스의 §2 표 + 관련 참조 링크 수정.
4. 상태 변동(🟢↔🟡)이면 §2 표 상태 갱신.
