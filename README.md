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
| `JPA_DDL_AUTO` | `update` (switch to `validate` once you add Flyway) |
| `PORT` | `8080` |
| `JWT_SECRET` | none, add it when you build authentication |

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
