# Multi-Source News Collection Design

## Goal

종목별 뉴스 출처를 두 개로 확장한다.

- KOSPI/KOSDAQ: Google News RSS + Naver 뉴스 검색 API
- NASDAQ/NYSE: Yahoo Finance RSS + Google News RSS
- 출처별 최대 `collection.news.limitPerTicker`건, 기본 5건
- URL 중복 제거 후 종목당 최대 10건
- 모든 출처에서 KST 기준 수집 당일 발행 뉴스만 사용

## Architecture

`RssNewsClient`는 Google News RSS와 Yahoo Finance RSS를 각각 호출할 수 있는 메서드를 제공한다. 신규 `NaverNewsClient`는 네이버 검색 API JSON 응답을 `RssNewsClient.NewsRow`와 동일한 뉴스 행 형태로 변환한다.

`MarketDataCollectionService.syncNewsArticles`가 시장별 출처를 선택하고 결과를 합친다. 한 출처가 실패하거나 키가 없으면 빈 목록으로 처리하며 다른 출처 결과는 계속 저장한다. 병합 결과는 URL 기준으로 중복 제거하고 최신 발행순으로 정렬한다.

## Naver Configuration

- Endpoint: `GET https://openapi.naver.com/v1/search/news.json`
- Headers: `X-Naver-Client-Id`, `X-Naver-Client-Secret`
- Query: `회사명 종목코드 주식`
- Parameters: `display={limit}`, `start=1`, `sort=date`
- Environment: `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- 미설정 또는 `dev-placeholder`이면 네트워크 호출 없이 빈 결과

## Data Rules

- Naver `originalLink`가 있으면 URL로 우선 사용하고, 없으면 `link` 사용
- Naver 제목/설명의 `<b>` 태그와 HTML entity 제거
- RSS/Naver 발행 시각을 `Asia/Seoul`로 정규화
- 당일이 아닌 뉴스와 발행 시각이 없는 뉴스 제외
- 저장 키와 중복 제거 기준은 `source + url` 저장 키를 유지하되, 병합 단계에서는 URL로 중복 제거

## Error Handling

- 출처별 HTTP 오류, JSON/XML 파싱 오류는 경고 로그 후 빈 목록 반환
- Naver 인증 키 미설정은 정상적인 비활성 상태로 취급
- 한 출처 실패가 전체 종목 뉴스 동기화를 실패시키지 않음

## Verification

- 미국 시장에서 Yahoo와 Google 결과가 모두 병합되는지 테스트
- 한국 시장에서 Google과 Naver 결과가 모두 병합되는지 테스트
- URL 중복 제거와 최대 10건 제한 테스트
- Naver 응답의 당일 필터, KST 변환, HTML 정리 테스트
- 전체 백엔드 테스트 및 실제 RSS/Naver 키 상태 검증
