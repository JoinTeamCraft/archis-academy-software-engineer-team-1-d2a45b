# Parking Lot System · Hackathon Template

Build the backend for a parking lot management system with Spring Boot and PostgreSQL. Users register vehicles, find a free spot, reserve it for a time window, pay, and get notified. Operators manage lots, floors and spots. Admins see reports.

This repo is the starting point for every team. It builds, boots, connects to Postgres, and has CI, but it has no business logic yet. That part is yours.

The full ticket list (PLS-001 to PLS-062) lives on the Lokum project board. Every PR you open should name one of those tickets.

## Stack

- Java 21, Spring Boot 4.1, Gradle (wrapper included, no install needed)
- PostgreSQL 17 through Docker Compose
- Spring Data JPA, Bean Validation, Actuator, springdoc OpenAPI
- H2 in PostgreSQL mode for tests, so CI needs no database

## Quick start

You need JDK 21+ and Docker.

```bash
# 1. Create your team repo from this template (the "Use this template" button on GitHub), then clone it
git clone https://github.com/<your-org>/<your-team-repo>.git
cd <your-team-repo>

# 2. Start Postgres
docker compose up -d postgres

# 3. Run the app
./gradlew bootRun          # Windows: gradlew.bat bootRun
```

Check it is alive:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

Run the tests:

```bash
./gradlew test
```

Run everything in containers (useful for the demo):

```bash
docker compose --profile app up --build
```

## Configuration

All settings come from environment variables with local defaults. See [.env.example](.env.example).

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/parking_lot` |
| `DB_USERNAME` / `DB_PASSWORD` | `parking` / `parking` |
| `DB_PORT` | `5432` (host port for the Docker Postgres) |
| `DB_POOL_SIZE` | `10` |
| `DB_CONNECTION_TIMEOUT_MS` | `10000` |
| `JPA_DDL_AUTO` | `update` (switch to `validate` once you add Flyway) |
| `PORT` | `8080` |
| `LOG_LEVEL` / `LOG_LEVEL_APP` | `INFO` / `INFO` (root and `tech.lokum.parkinglot`) |
| `LOG_LEVEL_SQL` | `INFO` (`DEBUG` prints every SQL statement) |
| `LOG_FILE` | `logs/parking-lot.log` |
| `LOG_MAX_FILE_SIZE` / `LOG_MAX_HISTORY` / `LOG_TOTAL_SIZE_CAP` | `10MB` / `14` days / `1GB` |
| `DB_SLOW_QUERY_MS` | `500` (`0` turns the slow query log off) |
| `JWT_SECRET` | none, add it when you build authentication |

## Database

- On startup the app logs `Connected to PostgreSQL <version> at <url>` and fails fast if the database is unreachable.
- `/actuator/health` shows the database status under `components.db`.
- Scripts in [db/init](db/init) run once, when the Postgres container starts with an empty volume. To rerun them: `docker compose down -v && docker compose up -d postgres`.
- Tests use the in-memory H2 database from `src/test/resources/application.yml`, so they need no Postgres.
- If JDK 21 is not installed, Gradle downloads it (foojay toolchain resolver in `settings.gradle`). CI installs 21 itself, so nothing is downloaded there.
- Tables come from the JPA entities (`JPA_DDL_AUTO=update`) until Flyway is added (PLS-045).

**Port 5432 already in use?** If a Postgres is already installed on your machine, the app connects to that one instead and fails with `password authentication failed for user "parking"`. Either stop the local service, or move the container to another port:

```powershell
$env:DB_PORT=5433; docker compose up -d postgres
$env:DB_URL="jdbc:postgresql://localhost:5433/parking_lot"; .\gradlew.bat bootRun
```

## Logging

SLF4J with Logback, set up in [logback-spring.xml](src/main/resources/logback-spring.xml). Get a logger with `LoggerFactory.getLogger(MyClass.class)` and use `{}` placeholders.

- **Where:** the console, and `logs/parking-lot.log`. The file rolls daily or at `LOG_MAX_FILE_SIZE`, old files are gzipped, and anything older than `LOG_MAX_HISTORY` days or past `LOG_TOTAL_SIZE_CAP` is deleted.
- **API calls:** every request is logged once: `GET /api/lots -> 200 (12 ms)`. Status 4xx is `WARN`, 5xx is `ERROR`, `/actuator/**` is `DEBUG`. Only the method and path are logged, never headers, query strings or bodies.
- **Request id:** each request gets an id (from the `X-Request-Id` header, or a new UUID). It is sent back in the `X-Request-Id` response header and printed on every line logged during that request, so you can grep one request end to end.
- **Database:** the connection is logged at startup, queries slower than `DB_SLOW_QUERY_MS` are logged by `org.hibernate.SQL_SLOW`, and `LOG_LEVEL_SQL=DEBUG` prints every statement. Do not turn on `org.hibernate.orm.jdbc.bind`: it prints bound values, including password hashes.
- **Secrets:** values of keys like `password`, `token`, `secret`, `apiKey` and `Authorization`, plus `Bearer` tokens and JWTs, are replaced with `****` in every message. This is a safety net: never log a password, token or card number on purpose.

Which level to use:

| Level | For | Example |
| --- | --- | --- |
| `ERROR` | Something failed and needs a person to look at it | Payment provider unreachable, unexpected exception |
| `WARN` | Unexpected but handled, or a client error | Failed login, 4xx response, retrying a call |
| `INFO` | Key business events, one line each | User registered, user logged in, reservation created or cancelled |
| `DEBUG` | Detail for debugging, off by default | Computed price breakdown, SQL statements |

Log authentication by user id or email and outcome (`Login failed for user a@b.io`), never with the password or token.

To see more locally: `LOG_LEVEL_APP=DEBUG LOG_LEVEL_SQL=DEBUG ./gradlew bootRun`.

## Project layout

```
src/main/java/tech/lokum/parkinglot/
├── ParkingLotApplication.java
├── config/        Security, CORS, OpenAPI
├── controller/    Thin REST controllers
├── dto/           Request and response records
├── entity/        JPA entities
├── exception/     Custom exceptions + global handler
├── repository/    Spring Data repositories
└── service/       Business logic and transactions
```

Each package has a `package-info.java` describing what belongs in it. Keep to the layering; the reviewers score it.

## Domain at a glance

```
User ──< Vehicle
User ──< Reservation >── ParkingSpot >── Floor >── ParkingLot
Reservation ── Payment ── Invoice
User ──< Feedback / Review >── ParkingLot
```

Roles: `ADMIN`, `OPERATOR`, `CUSTOMER`. Vehicle types: `CAR`, `MOTORBIKE`, `TRUCK`, `EV` (extend if you like).

## Workflow

1. Pick a ticket on the Lokum board and move it to In Progress.
2. Branch from the latest `main`: `<username>/PLS-017-book-parking-spot`.
3. Commit as `[PLS-017] - <what changed>`.
4. Open a PR titled `[PLS-017] - Book a Parking Spot` using the template, attach the PR link to the ticket and move it to Code Review. CI must be green.
5. Once approved, merge it yourself and move the ticket to Done. The merge triggers the AI review score on Lokum.

Protect `main` so nothing lands without a PR.
