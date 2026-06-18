# WorkPulse Backend

WorkPulse is a Spring Boot backend for the MVP attendance flow: Auth/Security, Company/Branch, Employee, Device/EmployeeCredential, Attendance, DeviceEvent processing, and Reports with Excel export.

## Requirements

- Java 21
- Maven
- Docker
- PostgreSQL 16 via Docker Compose

## Run Local

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

Local database defaults are aligned with `docker-compose.yml`:

- Host port: `5433`
- Database: `work_pulse`
- Username: `work_pulse`
- Password: `work_pulse`

Override with environment variables when needed:

```bash
DB_URL=jdbc:postgresql://localhost:5433/work_pulse \
DB_USERNAME=work_pulse \
DB_PASSWORD=work_pulse \
mvn spring-boot:run
```

## Login

The dev/local seed user is:

- Email: `admin@workpulse.uz`
- Password: `admin123`

This seed is for local/dev profiles only.

## Health Check

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "work-pulse",
  "timestamp": "2026-06-13T00:00:00Z"
}
```

## Postman Demo

Import these files into Postman:

- Collection: `docs/postman/workpulse-mvp.postman_collection.json`
- Environment: `docs/postman/workpulse-local.postman_environment.json`

Select the `WorkPulse Local` environment, then run folders in order:

1. `Health`
2. `A. Auth`
3. `B. Company`
4. `C. Branch`
5. `D. Employee`
6. `E. Device`
7. `F. EmployeeCredential`
8. `G. Attendance Manual`
9. `H. DeviceEvent`
10. `I. Reports`

Postman scripts save `accessToken`, `refreshToken`, and created resource IDs into the environment automatically.

Skip `Change Password (optional - skip for demo)` during the normal demo flow.

## Demo Data SQL

Optional manual demo seed data is available at:

```text
docs/demo/demo-data.sql
```

It is not connected to Flyway or production startup. Run it manually only against a local/demo database.

## E2E Checklist

Manual smoke-test steps are documented in:

```text
docs/testing/e2e-checklist.md
```

## Common Errors

### Port 5432 Already in Use

This project maps Docker PostgreSQL to host port `5433`, so it should avoid most local PostgreSQL conflicts. If you change the compose port, update `DB_URL` accordingly.

### Old PostgreSQL Volume Password Issue

If Docker reuses an old `postgres_data` volume with different credentials, PostgreSQL may reject `work_pulse/work_pulse`.

For a disposable local DB, remove the volume and start again:

```bash
docker compose down -v
docker compose up -d postgres
```

### Docker Daemon Not Running

If `docker compose up -d postgres` fails with a daemon/socket error, start Docker Desktop first and retry.

### Login Seed Missing

The admin seed runs only for `dev` or `local` profiles. The default profile is `dev`; if you override profiles, include one of those profiles for local demo login.
