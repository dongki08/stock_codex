# Stock Advisor MSSQL Contract-First Design

작성일: 2026-05-19

## 결정 사항

- 기존 `STOCK_ADVISOR_SPEC.md`의 제품 방향은 유지한다.
- 데이터베이스는 PostgreSQL/TimescaleDB가 아니라 로컬 PC에 이미 설치된 MSSQL을 사용한다.
- 기능 구현은 가능한 한 백엔드에 둔다.
- 프론트엔드는 Swagger/OpenAPI 계약과 생성 타입을 기준으로 UI만 구현한다.
- 하네스엔지니어링은 이번 범위에서 제외한다.
- 백엔드는 사용자가 제공한 `springboot-pro-architect` 규칙을 따른다.

## 목표

첫 단계는 전체 추천 시스템을 한 번에 완성하는 것이 아니라, 프론트와 백엔드가 갈라져 병렬로 진행될 수 있는 API 기반 골격을 만든다.

초기 산출물은 모노레포 구조, Spring Boot 백엔드, MSSQL 연결 설정, Swagger/OpenAPI 문서, 공통 응답/예외 처리, 관리자 설정 API, React 프론트 골격, OpenAPI 타입 생성 구조다.

## 아키텍처

프로젝트는 다음 모노레포 구조를 사용한다.

```text
stock-advisor/
  apps/
    backend/
    web/
    ml-sidecar/
  packages/
    shared-types/
  infra/
    docker/
  docs/
    STOCK_ADVISOR_SPEC.md
    superpowers/
      specs/
```

백엔드는 Spring Boot 3.x와 Java 21로 구현한다. API 문서는 `springdoc-openapi` 기반 Swagger UI로 제공한다. 프론트는 React 19, TypeScript, Vite 기반으로 시작하고, 백엔드 OpenAPI 문서에서 TypeScript 타입과 클라이언트 코드를 생성한다.

MSSQL은 Docker 컨테이너로 새로 띄우지 않는다. 이미 PC에 설치된 로컬 SQL Server 인스턴스에 접속한다. 접속 정보는 코드에 고정하지 않고 `application-local.yml` 또는 `.env`에서 받는다.

Redis는 캐시와 향후 작업 큐 용도로 남긴다. 첫 단계에서는 Redis 의존 기능을 필수로 만들지 않고, 설정 API와 감사 로그는 MSSQL만으로 동작하게 한다.

## 백엔드 설계

패키지는 `com.parkdh.stockadvisor` 아래에 둔다.

```text
api/
application/
common/
config/
domain/
global/
infrastructure/
scheduler/
```

컨트롤러는 요청을 받고 서비스 메서드를 그대로 호출한다. 비즈니스 로직은 서비스 레이어에 둔다. 엔티티를 API에 직접 노출하지 않고 모든 요청/응답은 독립 DTO 파일로 분리한다.

모든 API 응답은 `global/dto/ResultDto<T>`를 사용한다. 컨트롤러 반환 타입은 `ResultDto<?>`로 통일한다.

예외는 `CustomException(message, code)`로 던지고 `GlobalExceptionHandler`에서 아래 형식으로 변환한다.

```json
{
  "code": 404,
  "error_message": "메시지"
}
```

쓰기 메서드에만 `jakarta.transaction.Transactional`을 개별 적용한다. 조회 메서드에는 트랜잭션을 붙이지 않는다.

백엔드 코드에는 사용자가 지정한 규칙대로 한국어 매 줄 주석을 작성한다.

## MSSQL 데이터 설계

초기 구현 대상 테이블은 다음 두 개다.

`app_setting`

- `setting_key`: 설정 키, PK
- `value_json`: 설정 값 JSON 문자열
- `description`: 설정 설명
- `updated_at`: 수정 일시
- `updated_by`: 수정자

`audit_log`

- `id`: 감사 로그 ID
- `actor`: 수행자
- `action`: 작업명
- `before_json`: 변경 전 JSON
- `after_json`: 변경 후 JSON
- `occurred_at`: 발생 일시

MSSQL에서는 PostgreSQL의 `JSONB`를 쓰지 않는다. JSON 값은 `nvarchar(max)`에 저장하고, 필요하면 `ISJSON` 체크 제약 조건을 추가한다.

JPA 엔티티명은 `AppSettingEntity`, `AuditLogEntity`처럼 `XxxEntity`로 명명한다. 일반 엔티티는 `BaseEntity`, 생성일만 필요한 엔티티는 `CreatedEntity`를 상속한다. 모든 컬럼에는 `@Comment("한글 설명")`을 작성한다.

## 초기 API 계약

관리자 설정 API를 첫 계약으로 만든다.

```text
GET    /api/admin/settings
GET    /api/admin/settings/{key}
PUT    /api/admin/settings/{key}
POST   /api/admin/settings/reset
GET    /api/admin/audit-logs
```

모든 컨트롤러 메서드는 `@Operation`으로 목적, 요청 파라미터, 성공 반환 필드, 에러 반환 필드, 주요 제약 사항을 상세히 문서화한다.

초기 설정 키는 다음 범위를 포함한다.

- 단기 추천 개수
- 장기 추천 개수
- KRX 알림 시각
- US 알림 시각
- 백테스트 기본 기간
- 손절 모니터링 주기
- Codex 일 호출 한도
- AutoResearch 활성 여부

## 프론트엔드 설계

프론트는 API 서버의 Swagger/OpenAPI를 기준으로 타입을 생성한다. 프론트 내부에는 도메인 계산 로직을 넣지 않는다.

초기 화면은 `/admin`을 우선 구현한다. `/`, `/history`, `/stats`, `/backtest`, `/settings`는 라우팅 골격과 빈 화면 또는 API 계약 대기 상태로 둔다.

관리자 화면은 백엔드 설정 API에서 받은 키/값/설명을 렌더링하고, 변경 시 `PUT /api/admin/settings/{key}`를 호출한다. 저장 성공 후 감사 로그 목록을 갱신한다.

## 에러 처리

백엔드 입력값은 Bean Validation과 서비스 레이어 비즈니스 검증을 함께 사용한다.

대표 에러는 다음처럼 구분한다.

- `400`: 잘못된 설정 값
- `404`: 존재하지 않는 설정 키
- `409`: 동시 수정 충돌 또는 상태 충돌
- `500`: 처리 중 서버 오류

프론트는 `error_message`를 사용자에게 표시하고, `code`를 기준으로 재시도 가능 여부를 판단한다.

## 테스트 전략

백엔드는 우선 다음 테스트를 작성한다.

- `ResultDto` 직렬화 테스트
- `CustomException` 변환 테스트
- 설정 조회/수정 서비스 테스트
- 관리자 설정 컨트롤러 통합 테스트

MSSQL 연동 테스트는 로컬 설치 상태에 따라 처음에는 profile 기반으로 분리한다. 실제 DB 연결 정보가 확정되면 통합 테스트를 활성화한다.

프론트는 OpenAPI 타입 생성이 되는지와 `/admin` 화면 렌더링 및 저장 액션을 검증한다.

## 구현 순서

1. 모노레포 디렉터리와 문서 위치 정리
2. Spring Boot 백엔드 생성
3. MSSQL 드라이버와 JPA 설정
4. Swagger/OpenAPI 설정
5. 공통 응답, 예외, 베이스 엔티티 작성
6. `app_setting`, `audit_log` 엔티티/DTO/서비스/컨트롤러 작성
7. React 프론트 골격 생성
8. OpenAPI 타입 생성 흐름 추가
9. `/admin` UI 연결
10. 빌드와 기본 테스트 확인

## 범위 제외

- 실제 종목 추천 엔진
- KIS, DART, yfinance, FRED 연동
- Telegram/Kakao 알림 발송
- Codex CLI 실제 호출
- ML 사이드카 학습/추론
- AutoResearch 루프
- 실거래 주문 연동

이 범위 제외 항목은 첫 API 계약과 관리자 설정 기반이 안정화된 뒤 다음 구현 계획에서 순차적으로 다룬다.

## 자체 검토

- 스펙의 PostgreSQL/TimescaleDB 전제는 이 설계에서 MSSQL로 명시적으로 변경했다.
- 프론트와 백엔드의 책임 경계는 Swagger/OpenAPI 계약을 기준으로 분리했다.
- 백엔드 구현 규칙은 사용자가 제공한 Spring Boot 프로 아키텍트 기준을 반영했다.
- 첫 구현 범위는 설정 API와 감사 로그로 제한해 과도한 추천 엔진 구현을 피했다.
- 외부 API 키와 실제 로컬 MSSQL 접속 정보는 코드에 고정하지 않는 것으로 명시했다.
