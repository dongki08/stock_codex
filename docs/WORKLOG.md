# 작업 일지 (WORKLOG)

> 📌 **에이전트 규칙**: 작업(코드/문서 변경)을 마칠 때마다 이 파일 **맨 위**(최신이 위)에 한 블록 추가. 사용자가 매번 시키지 않아도 자동으로.
> - append-only 누적 일지. 단일출처 규칙 예외 — 막 쌓아도 됨.
> - 큰 구조/기능 변화면 [11-ROADMAP.md](11-ROADMAP.md) §0 델타에도 승격.
> - 한 달 넘게 쌓이면 월별로 잘라 [90-archive/](90-archive/)로 이동.

## 블록 형식

```
### YYYY-MM-DD — <한 줄 제목>
- 무엇: <한 일>
- 왜: <이유/배경> (자명하면 생략)
- 변경: `파일/영역` (핵심만)
- 후속: <남은 일/주의> (있으면)
```

---

### 2026-05-29 — 코드 진행상황 전수 감사 + 로드맵 실측 갱신
- 무엇: 실제 코드 인벤토리(컨트롤러19·서비스18·클라이언트13·스케줄러6·테스트24·프론트7·Flyway V8) 측정 → 11-ROADMAP 단계맵/§0/감사표 갱신.
- 발견:
  - 🔴 **빌드 리스크**: `ExitConfirmServiceTest`가 삭제된 `ExitConfirmService` 참조 → `gradlew test` 컴파일 실패. **즉시 삭제/이관 필요.**
  - ✅ 배선 확인: `SentimentAnalysisClient`·`DartFundamentalClient` → `MarketDataCollectionService` 연결. `NotificationService` → 4 스케줄러+ExitMonitor 배선(알림 실동작).
  - M5(알림) 골격→배선 완료 수준으로 상향. M1 KR 펀더멘털/감성 수집 배선 확인.
- 변경: `docs/11-ROADMAP.md`(단계맵 빌드리스크 줄·M5·§0·감사표)
- 후속: ExitConfirmServiceTest 처리 → 빌드 그린 확인. 그다음 M7 TASK-1·2.

### 2026-05-29 — docs 재구성 + 에이전트 부트 컨텍스트
- 무엇: docs 번호체계(00~90) 리네임, 00-INDEX 신설, 상태배너 통일, 스테일/모순 정정(ResultDto 포맷·ExitConfirm 제거·스택 MSSQL). 루트 CLAUDE.md/AGENTS.md + 이 WORKLOG 추가.
- 왜: 문서 난립·중복·일관성 부족 해소, 새 세션이 구조·진행상황 자동 인지하도록.
- 변경: `docs/*`, `CLAUDE.md`, `AGENTS.md`, `README.md`
- 후속: 미커밋 상태. 수익률 작업은 [40-RETURN-STRATEGY.md](40-RETURN-STRATEGY.md) TASK-1·2부터.
