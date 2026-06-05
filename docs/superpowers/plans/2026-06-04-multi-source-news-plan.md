# Multi-Source News Collection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 한국과 미국 종목 뉴스를 시장별 두 출처에서 수집하고, 당일 뉴스만 중복 제거하여 종목당 최대 10건 저장한다.

**Architecture:** RSS 출처 선택은 `RssNewsClient`, Naver JSON API는 신규 `NaverNewsClient`, 병합 정책은 `MarketDataCollectionService`에 둔다. 기존 `NewsRow`와 저장 파이프라인을 재사용한다.

**Tech Stack:** Java 21, Spring Boot 3.3, Java HttpClient, Jackson, JUnit 5, Mockito

---

### Task 1: RSS 출처별 호출 분리

**Files:**
- Modify: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/marketdata/news/RssNewsClient.java`
- Test: `apps/backend/src/test/java/com/parkdh/stockadvisor/infrastructure/marketdata/news/RssNewsClientTest.java`

- [x] 미국 Google News URL과 Yahoo URL을 각각 생성하는 실패 테스트 작성
- [x] 테스트 실패 확인
- [x] `fetchGoogleNews`, `fetchYahooNews`와 출처별 URL 생성 구현
- [x] RSS 테스트 통과 확인

### Task 2: Naver 뉴스 검색 API 클라이언트

**Files:**
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/config/NaverNewsProperties.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/marketdata/news/NaverNewsClient.java`
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/infrastructure/marketdata/news/NaverNewsClientTest.java`
- Modify: `apps/backend/src/main/resources/application.yml`
- Modify: `apps/backend/src/main/resources/application-local.yml.example`

- [x] 당일 필터, KST 변환, HTML 정리, originalLink 우선 테스트 작성
- [x] 테스트 실패 확인
- [x] Naver JSON 파싱과 인증 헤더 요청 구현
- [x] 키 미설정 시 빈 목록 반환 구현
- [x] Naver 클라이언트 테스트 통과 확인

### Task 3: 시장별 병합과 중복 제거

**Files:**
- Modify: `apps/backend/src/main/java/com/parkdh/stockadvisor/application/marketdata/MarketDataCollectionService.java`
- Modify: `apps/backend/src/test/java/com/parkdh/stockadvisor/application/marketdata/MarketDataCollectionServiceTest.java`

- [x] KRX Google+Naver, US Yahoo+Google 병합과 URL 중복 제거 실패 테스트 작성
- [x] 테스트 실패 확인
- [x] 시장별 출처 선택, 최신순 정렬, 종목당 최대 `2 * limit` 제한 구현
- [x] 서비스 테스트 통과 확인

### Task 4: 검증과 문서

**Files:**
- Modify: `docs/12-SETUP-CHECKLIST.md`
- Modify: `docs/20-API.md`
- Modify: `docs/30-BACKEND-OVERVIEW.md`
- Modify: `docs/31-BACKEND-DEEP-DIVE.md`
- Modify: `docs/11-ROADMAP.md`
- Modify: `docs/WORKLOG.md`

- [x] 전체 백엔드 테스트 실행
- [x] 실제 RSS 병합 결과 확인
- [x] Naver 키 설정 방법과 비활성 fallback 문서화
- [x] 작업 기록과 로드맵 갱신
