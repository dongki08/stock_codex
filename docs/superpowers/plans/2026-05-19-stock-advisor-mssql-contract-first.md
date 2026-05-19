# Stock Advisor MSSQL Contract-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Stock Advisor project skeleton with a Spring Boot MSSQL backend, Swagger/OpenAPI contract, admin settings API, React frontend shell, and OpenAPI-based shared types.

**Architecture:** The backend owns domain behavior and exposes documented REST APIs using a common `ResultDto<?>` response wrapper. The frontend consumes generated OpenAPI types and keeps business logic out of UI code. Local MSSQL is used through configurable Spring properties instead of Docker-managed database containers.

**Tech Stack:** Java 21, Spring Boot 3.x, Gradle, Spring Data JPA, MSSQL JDBC, springdoc-openapi, React 19, TypeScript, Vite, TanStack Query, OpenAPI TypeScript generator.

---

## File Structure

- Create: `apps/backend/`
  - Spring Boot application and Gradle project.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/StockAdvisorApplication.java`
  - Backend application entry point.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/dto/ResultDto.java`
  - Standard success and error response body.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/exception/CustomException.java`
  - Business exception with HTTP-style code.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/exception/GlobalExceptionHandler.java`
  - Converts exceptions into `ResultDto` error responses.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/common/BaseEntity.java`
  - Common created/updated timestamp fields.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/common/CreatedEntity.java`
  - Common created timestamp field.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/setting/AppSettingEntity.java`
  - MSSQL-backed setting entity.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/audit/AuditLogEntity.java`
  - Audit log entity.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/persistence/setting/AppSettingRepository.java`
  - JPA repository for settings.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/persistence/audit/AuditLogRepository.java`
  - JPA repository for audit logs.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/dto/*.java`
  - Request and response DTOs for admin APIs.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/application/admin/AdminSettingService.java`
  - Service-layer setting read/write logic.
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/AdminSettingController.java`
  - Swagger-documented admin API controller.
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/global/dto/ResultDtoTest.java`
  - Common response serialization test.
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/global/exception/GlobalExceptionHandlerTest.java`
  - Exception response test.
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/application/admin/AdminSettingServiceTest.java`
  - Service behavior test with mocks.
- Create: `apps/web/`
  - React application.
- Create: `packages/shared-types/`
  - Generated OpenAPI TypeScript types.
- Modify: `STOCK_ADVISOR_SPEC.md`
  - Keep source spec as-is unless a later documentation task copies it into `docs/`.

---

### Task 1: Repository Root and Documentation Baseline

**Files:**
- Create: `.gitignore`
- Create: `README.md`
- Create: `docs/STOCK_ADVISOR_SPEC.md`

- [ ] **Step 1: Add root ignore rules**

Create `.gitignore` with:

```gitignore
.idea/
.vscode/
.superpowers/
node_modules/
dist/
build/
.gradle/
out/
target/
.env
*.log
```

- [ ] **Step 2: Add project README**

Create `README.md` with:

```markdown
# Stock Advisor

Daily Stock Advisor is a personal stock recommendation system.

## First milestone

- Spring Boot backend with MSSQL connection.
- Swagger/OpenAPI contract.
- Admin settings API.
- React UI shell generated from API types.

## Local database

The project connects to an existing local SQL Server installation.
Database credentials must be provided through environment variables or a local Spring profile file.
```

- [ ] **Step 3: Copy source spec into docs**

Run:

```powershell
Copy-Item -LiteralPath 'STOCK_ADVISOR_SPEC.md' -Destination 'docs\STOCK_ADVISOR_SPEC.md' -Force
```

Expected: `docs/STOCK_ADVISOR_SPEC.md` exists and matches the root spec.

- [ ] **Step 4: Commit documentation baseline**

Run:

```powershell
git status --short
git add .gitignore README.md docs/STOCK_ADVISOR_SPEC.md docs/superpowers/specs/2026-05-19-stock-advisor-mssql-contract-first-design.md docs/superpowers/plans/2026-05-19-stock-advisor-mssql-contract-first.md
git commit -m "docs: add stock advisor contract-first plan"
```

Expected: commit succeeds if the folder has been initialized as a git repository.

---

### Task 2: Backend Project Skeleton

**Files:**
- Create: `apps/backend/settings.gradle`
- Create: `apps/backend/build.gradle`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/StockAdvisorApplication.java`
- Create: `apps/backend/src/main/resources/application.yml`
- Create: `apps/backend/src/main/resources/application-local.yml.example`

- [ ] **Step 1: Create Gradle settings**

Create `apps/backend/settings.gradle` with:

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = 'stock-advisor-backend'
```

- [ ] **Step 2: Create Gradle build file**

Create `apps/backend/build.gradle` with:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.parkdh'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.microsoft.sqlserver:mssql-jdbc'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Add Spring Boot entry point**

Create `apps/backend/src/main/java/com/parkdh/stockadvisor/StockAdvisorApplication.java` with:

```java
package com.parkdh.stockadvisor; // 애플리케이션 기본 패키지를 선언한다.

import org.springframework.boot.SpringApplication; // 스프링 부트 실행 도구를 가져온다.
import org.springframework.boot.autoconfigure.SpringBootApplication; // 자동 설정 애플리케이션 어노테이션을 가져온다.

@SpringBootApplication // 스프링 부트 자동 설정과 컴포넌트 스캔을 활성화한다.
public class StockAdvisorApplication { // 애플리케이션 시작 클래스를 정의한다.
    public static void main(String[] args) { // JVM 실행 진입점을 정의한다.
        SpringApplication.run(StockAdvisorApplication.class, args); // 스프링 부트 애플리케이션을 실행한다.
    } // main 메서드를 종료한다.
} // 시작 클래스를 종료한다.
```

- [ ] **Step 4: Add base application configuration**

Create `apps/backend/src/main/resources/application.yml` with:

```yaml
spring:
  application:
    name: stock-advisor-backend
  profiles:
    active: local
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 5: Add local MSSQL example configuration**

Create `apps/backend/src/main/resources/application-local.yml.example` with:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=stock_advisor;encrypt=true;trustServerCertificate=true
    username: sa
    password: change-me
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.SQLServerDialect
```

- [ ] **Step 6: Run backend build**

Run:

```powershell
.\gradlew.bat build
```

from `apps/backend`.

Expected: build fails only if dependencies cannot be downloaded or Java 21 is unavailable.

---

### Task 3: Common Response and Exception Layer

**Files:**
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/dto/ResultDto.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/exception/CustomException.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/global/exception/GlobalExceptionHandler.java`
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/global/dto/ResultDtoTest.java`
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/global/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write response serialization test**

Create `ResultDtoTest.java` with:

```java
package com.parkdh.stockadvisor.global.dto; // 테스트 대상 패키지를 선언한다.

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 직렬화 도구를 가져온다.
import org.junit.jupiter.api.Test; // 테스트 어노테이션을 가져온다.

import java.util.Map; // 테스트 데이터 맵을 가져온다.

import static org.assertj.core.api.Assertions.assertThat; // 검증 메서드를 가져온다.

class ResultDtoTest { // 공통 응답 DTO 테스트 클래스를 정의한다.
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 직렬화를 수행할 ObjectMapper를 준비한다.

    @Test // 성공 응답 직렬화를 검증한다.
    void successWithDataContainsCodeAndData() throws Exception { // 데이터 포함 성공 응답 테스트를 정의한다.
        ResultDto<?> result = ResultDto.success(Map.of("name", "stock")); // 성공 응답 객체를 생성한다.
        String json = objectMapper.writeValueAsString(result); // 응답 객체를 JSON 문자열로 변환한다.
        assertThat(json).contains("\"code\":200"); // 성공 코드가 포함되는지 검증한다.
        assertThat(json).contains("\"data\":{\"name\":\"stock\"}"); // 데이터 필드가 포함되는지 검증한다.
    } // 테스트 메서드를 종료한다.

    @Test // 에러 응답 직렬화를 검증한다.
    void errorContainsCodeAndErrorMessage() throws Exception { // 에러 응답 테스트를 정의한다.
        ResultDto<?> result = ResultDto.error(404, "설정을 찾을 수 없습니다."); // 에러 응답 객체를 생성한다.
        String json = objectMapper.writeValueAsString(result); // 응답 객체를 JSON 문자열로 변환한다.
        assertThat(json).contains("\"code\":404"); // 에러 코드가 포함되는지 검증한다.
        assertThat(json).contains("\"error_message\":\"설정을 찾을 수 없습니다.\""); // 에러 메시지가 포함되는지 검증한다.
    } // 테스트 메서드를 종료한다.
} // 테스트 클래스를 종료한다.
```

- [ ] **Step 2: Run failing response test**

Run:

```powershell
.\gradlew.bat test --tests "com.parkdh.stockadvisor.global.dto.ResultDtoTest"
```

Expected: FAIL because `ResultDto` does not exist.

- [ ] **Step 3: Add `ResultDto`**

Create `ResultDto.java` with:

```java
package com.parkdh.stockadvisor.global.dto; // 공통 DTO 패키지를 선언한다.

import com.fasterxml.jackson.annotation.JsonInclude; // null 필드 제외 어노테이션을 가져온다.
import com.fasterxml.jackson.annotation.JsonProperty; // JSON 필드명 지정 어노테이션을 가져온다.

@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 필드는 응답 JSON에서 제외한다.
public record ResultDto<T>( // 공통 API 응답 레코드를 정의한다.
        Integer code, // 응답 코드를 보관한다.
        T data, // 성공 응답 데이터를 보관한다.
        @JsonProperty("error_message") String errorMessage // 에러 메시지 JSON 필드를 보관한다.
) { // 레코드 본문을 시작한다.
    public static <T> ResultDto<T> success(T data) { // 데이터가 있는 성공 응답을 생성한다.
        return new ResultDto<>(200, data, null); // 성공 코드와 데이터를 담아 반환한다.
    } // success 메서드를 종료한다.

    public static ResultDto<?> success() { // 데이터가 없는 성공 응답을 생성한다.
        return new ResultDto<>(200, null, null); // 성공 코드만 담아 반환한다.
    } // success 메서드를 종료한다.

    public static ResultDto<?> error(Integer code, String errorMessage) { // 에러 응답을 생성한다.
        return new ResultDto<>(code, null, errorMessage); // 에러 코드와 메시지를 담아 반환한다.
    } // error 메서드를 종료한다.
} // 응답 레코드를 종료한다.
```

- [ ] **Step 4: Add exception classes**

Create `CustomException.java` with:

```java
package com.parkdh.stockadvisor.global.exception; // 예외 패키지를 선언한다.

public class CustomException extends RuntimeException { // 비즈니스 예외 클래스를 정의한다.
    private final int code; // 클라이언트에 전달할 에러 코드를 보관한다.

    public CustomException(String message, int code) { // 메시지와 코드로 예외를 생성한다.
        super(message); // 상위 RuntimeException에 메시지를 전달한다.
        this.code = code; // 에러 코드를 필드에 저장한다.
    } // 생성자를 종료한다.

    public int getCode() { // 에러 코드를 반환한다.
        return code; // 저장된 에러 코드를 반환한다.
    } // getter를 종료한다.
} // 예외 클래스를 종료한다.
```

Create `GlobalExceptionHandler.java` with:

```java
package com.parkdh.stockadvisor.global.exception; // 예외 처리 패키지를 선언한다.

import com.parkdh.stockadvisor.global.dto.ResultDto; // 공통 응답 DTO를 가져온다.
import org.springframework.http.ResponseEntity; // HTTP 응답 엔티티를 가져온다.
import org.springframework.web.bind.MethodArgumentNotValidException; // 검증 실패 예외를 가져온다.
import org.springframework.web.bind.annotation.ExceptionHandler; // 예외 처리 어노테이션을 가져온다.
import org.springframework.web.bind.annotation.RestControllerAdvice; // 전역 REST 예외 처리 어노테이션을 가져온다.

@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리한다.
public class GlobalExceptionHandler { // 전역 예외 처리 클래스를 정의한다.
    @ExceptionHandler(CustomException.class) // 커스텀 예외를 처리한다.
    public ResponseEntity<ResultDto<?>> handleCustomException(CustomException exception) { // 커스텀 예외 응답을 만든다.
        return ResponseEntity.status(exception.getCode()).body(ResultDto.error(exception.getCode(), exception.getMessage())); // 예외 코드와 메시지를 응답한다.
    } // 커스텀 예외 처리 메서드를 종료한다.

    @ExceptionHandler(MethodArgumentNotValidException.class) // Bean Validation 예외를 처리한다.
    public ResponseEntity<ResultDto<?>> handleValidationException(MethodArgumentNotValidException exception) { // 검증 실패 응답을 만든다.
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst().map(error -> error.getDefaultMessage()).orElse("입력값이 올바르지 않습니다."); // 첫 번째 검증 메시지를 선택한다.
        return ResponseEntity.badRequest().body(ResultDto.error(400, message)); // 400 응답과 검증 메시지를 반환한다.
    } // 검증 예외 처리 메서드를 종료한다.
} // 전역 예외 처리 클래스를 종료한다.
```

- [ ] **Step 5: Run common layer tests**

Run:

```powershell
.\gradlew.bat test --tests "com.parkdh.stockadvisor.global.*"
```

Expected: PASS.

---

### Task 4: Base Entities and Admin Domain

**Files:**
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/common/BaseEntity.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/common/CreatedEntity.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/setting/AppSettingEntity.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/domain/audit/AuditLogEntity.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/persistence/setting/AppSettingRepository.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/infrastructure/persistence/audit/AuditLogRepository.java`

- [ ] **Step 1: Add timestamp base classes**

Create `BaseEntity.java` with:

```java
package com.parkdh.stockadvisor.domain.common; // 공통 엔티티 패키지를 선언한다.

import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.MappedSuperclass; // 매핑 상위 클래스 어노테이션을 가져온다.
import jakarta.persistence.PrePersist; // 저장 전 콜백 어노테이션을 가져온다.
import jakarta.persistence.PreUpdate; // 수정 전 콜백 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@MappedSuperclass // 하위 엔티티에 공통 매핑 필드를 제공한다.
public abstract class BaseEntity { // 생성일과 수정일을 가진 기본 엔티티를 정의한다.
    @Comment("생성 일시") // 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "created_at", nullable = false, updatable = false) // 생성 일시 컬럼을 매핑한다.
    private LocalDateTime createdAt; // 생성 일시 값을 보관한다.

    @Comment("수정 일시") // 수정 일시 컬럼 설명을 지정한다.
    @Column(name = "updated_at", nullable = false) // 수정 일시 컬럼을 매핑한다.
    private LocalDateTime updatedAt; // 수정 일시 값을 보관한다.

    @PrePersist // 최초 저장 전에 실행한다.
    protected void onCreate() { // 생성 콜백 메서드를 정의한다.
        LocalDateTime now = LocalDateTime.now(); // 현재 시간을 구한다.
        this.createdAt = now; // 생성 일시를 현재 시간으로 설정한다.
        this.updatedAt = now; // 수정 일시를 현재 시간으로 설정한다.
    } // 생성 콜백을 종료한다.

    @PreUpdate // 수정 전에 실행한다.
    protected void onUpdate() { // 수정 콜백 메서드를 정의한다.
        this.updatedAt = LocalDateTime.now(); // 수정 일시를 현재 시간으로 갱신한다.
    } // 수정 콜백을 종료한다.

    public LocalDateTime getCreatedAt() { return createdAt; } // 생성 일시를 반환한다.
    public LocalDateTime getUpdatedAt() { return updatedAt; } // 수정 일시를 반환한다.
} // 기본 엔티티를 종료한다.
```

Create `CreatedEntity.java` with:

```java
package com.parkdh.stockadvisor.domain.common; // 공통 엔티티 패키지를 선언한다.

import jakarta.persistence.Column; // 컬럼 매핑 어노테이션을 가져온다.
import jakarta.persistence.MappedSuperclass; // 매핑 상위 클래스 어노테이션을 가져온다.
import jakarta.persistence.PrePersist; // 저장 전 콜백 어노테이션을 가져온다.
import org.hibernate.annotations.Comment; // 컬럼 설명 어노테이션을 가져온다.

import java.time.LocalDateTime; // 날짜 시간 타입을 가져온다.

@MappedSuperclass // 하위 엔티티에 공통 매핑 필드를 제공한다.
public abstract class CreatedEntity { // 생성일만 가진 기본 엔티티를 정의한다.
    @Comment("생성 일시") // 생성 일시 컬럼 설명을 지정한다.
    @Column(name = "created_at", nullable = false, updatable = false) // 생성 일시 컬럼을 매핑한다.
    private LocalDateTime createdAt; // 생성 일시 값을 보관한다.

    @PrePersist // 최초 저장 전에 실행한다.
    protected void onCreate() { // 생성 콜백 메서드를 정의한다.
        this.createdAt = LocalDateTime.now(); // 생성 일시를 현재 시간으로 설정한다.
    } // 생성 콜백을 종료한다.

    public LocalDateTime getCreatedAt() { return createdAt; } // 생성 일시를 반환한다.
} // 생성 엔티티를 종료한다.
```

- [ ] **Step 2: Add admin entities and repositories**

Create `AppSettingEntity.java` with fields `settingKey`, `valueJson`, `description`, `updatedBy`, extending `BaseEntity`.

Create `AuditLogEntity.java` with fields `id`, `actor`, `action`, `beforeJson`, `afterJson`, extending `CreatedEntity`.

Create repositories extending `JpaRepository<AppSettingEntity, String>` and `JpaRepository<AuditLogEntity, Long>`.

- [ ] **Step 3: Run compile**

Run:

```powershell
.\gradlew.bat compileJava
```

Expected: PASS.

---

### Task 5: Admin Settings Service and API

**Files:**
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/dto/AdminSettingResponse.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/dto/AdminSettingUpdateRequest.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/dto/AuditLogResponse.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/application/admin/AdminSettingService.java`
- Create: `apps/backend/src/main/java/com/parkdh/stockadvisor/api/admin/AdminSettingController.java`
- Create: `apps/backend/src/test/java/com/parkdh/stockadvisor/application/admin/AdminSettingServiceTest.java`

- [ ] **Step 1: Write service tests**

Create tests for these cases:

```java
// getSettings_returnsAllSettingsSortedByKey
// getSetting_throws404WhenMissing
// updateSetting_rejectsInvalidJson
// updateSetting_savesAuditLog
```

Expected failure before implementation: `AdminSettingService` does not exist.

- [ ] **Step 2: Add DTOs**

Create DTO records:

```java
public record AdminSettingResponse(String key, String valueJson, String description, String updatedBy) {}
public record AdminSettingUpdateRequest(@NotBlank String valueJson, @NotBlank String actor) {}
public record AuditLogResponse(Long id, String actor, String action, String beforeJson, String afterJson) {}
```

Each DTO line must include Korean line comments in the final implementation.

- [ ] **Step 3: Add service implementation**

Implement:

```java
public ResultDto<?> getSettings()
public ResultDto<?> getSetting(String key)
@Transactional
public ResultDto<?> updateSetting(String key, AdminSettingUpdateRequest request)
public ResultDto<?> getAuditLogs()
```

Rules:

- Validate JSON by reading `valueJson` with Jackson `ObjectMapper.readTree`.
- Throw `new CustomException("설정을 찾을 수 없습니다.", 404)` when key is missing.
- Throw `new CustomException("설정 JSON 형식이 올바르지 않습니다.", 400)` when JSON is invalid.
- Save one audit log per update.
- Return only DTOs, never entities.

- [ ] **Step 4: Add controller with Swagger docs**

Expose:

```text
GET /api/admin/settings
GET /api/admin/settings/{key}
PUT /api/admin/settings/{key}
GET /api/admin/audit-logs
```

Each controller method must return `ResultDto<?>` and call only one service method.

- [ ] **Step 5: Run service and controller tests**

Run:

```powershell
.\gradlew.bat test --tests "com.parkdh.stockadvisor.application.admin.AdminSettingServiceTest"
```

Expected: PASS.

---

### Task 6: Frontend and Shared Types Skeleton

**Files:**
- Create: `apps/web/package.json`
- Create: `apps/web/index.html`
- Create: `apps/web/src/main.tsx`
- Create: `apps/web/src/App.tsx`
- Create: `apps/web/src/pages/AdminPage.tsx`
- Create: `packages/shared-types/package.json`
- Create: `packages/shared-types/openapi-typescript.config.json`

- [ ] **Step 1: Create frontend package**

Create `apps/web/package.json` with scripts:

```json
{
  "name": "stock-advisor-web",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 127.0.0.1",
    "build": "tsc && vite build",
    "generate:api": "openapi-typescript http://localhost:8080/v3/api-docs -o ../../packages/shared-types/src/api.d.ts"
  },
  "dependencies": {
    "@vitejs/plugin-react": "latest",
    "vite": "latest",
    "typescript": "latest",
    "react": "latest",
    "react-dom": "latest",
    "@tanstack/react-query": "latest",
    "openapi-typescript": "latest"
  },
  "devDependencies": {}
}
```

- [ ] **Step 2: Create admin UI shell**

Create `AdminPage.tsx` with:

```tsx
export function AdminPage() {
  return (
    <main>
      <h1>관리자 설정</h1>
      <section>
        <h2>설정 API 연결 대기</h2>
      </section>
    </main>
  );
}
```

- [ ] **Step 3: Add shared types package**

Create `packages/shared-types/package.json` with:

```json
{
  "name": "@stock-advisor/shared-types",
  "private": true,
  "type": "module",
  "exports": {
    "./api": "./src/api.d.ts"
  }
}
```

- [ ] **Step 4: Run frontend build**

Run:

```powershell
npm install
npm run build
```

from `apps/web`.

Expected: PASS if Node and npm are available.

---

### Task 7: Final Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run backend tests**

Run:

```powershell
.\gradlew.bat test
```

from `apps/backend`.

Expected: PASS.

- [ ] **Step 2: Run backend build**

Run:

```powershell
.\gradlew.bat build
```

from `apps/backend`.

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm run build
```

from `apps/web`.

Expected: PASS.

- [ ] **Step 4: Document local run commands**

Add to `README.md`:

```markdown
## Run backend

Copy `apps/backend/src/main/resources/application-local.yml.example` to `application-local.yml` and set local SQL Server credentials.

```powershell
cd apps/backend
.\gradlew.bat bootRun
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Run frontend

```powershell
cd apps/web
npm install
npm run dev
```
```

- [ ] **Step 5: Commit verified skeleton**

Run:

```powershell
git status --short
git add .
git commit -m "feat: add MSSQL contract-first stock advisor skeleton"
```

Expected: commit succeeds if git is initialized.

---

## Self-Review

- Spec coverage: The plan covers MSSQL configuration, Swagger/OpenAPI, backend-owned behavior, admin settings API, audit logs, React shell, and shared OpenAPI types.
- Deferred scope: Recommendation engine, external market data, Telegram/Kakao, Codex CLI integration, ML sidecar behavior, and AutoResearch remain outside this first implementation plan by design.
- Placeholder scan: The plan avoids unspecified future work in the active tasks and gives exact paths, commands, and required behavior.
- Type consistency: `ResultDto<?>`, `CustomException`, `AppSettingEntity`, `AuditLogEntity`, and `AdminSettingService` names are consistent across tasks.
