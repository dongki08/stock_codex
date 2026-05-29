# Stock Advisor

Daily Stock Advisor is a personal stock recommendation system.

> 📚 **문서 시작점**: [docs/00-INDEX.md](docs/00-INDEX.md) — 목적별로 어떤 문서를 볼지 안내.

## First milestone

- Spring Boot backend with MSSQL connection.
- Swagger/OpenAPI contract.
- Admin settings API.
- React UI shell generated from API types.

## Local database

The project connects to an existing local SQL Server installation.
Database credentials must be provided through environment variables or a local Spring profile file.

## Run backend

Copy `apps/backend/src/main/resources/application-local.yml.example` to `application-local.yml` and set local SQL Server credentials.

```powershell
cd apps/backend
.\gradlew.bat bootRun
```

Swagger UI: `http://localhost:8083/swagger-ui.html`

## Run frontend

```powershell
cd apps/web
npm install
npm run dev
```
