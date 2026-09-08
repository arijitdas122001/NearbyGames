# Game Connect

Pickup football matchmaking platform (MVP).

- `backend/` — Spring Boot modular monolith (Java 21), PostgreSQL + Flyway, JWT (Phase 2+).
- `frontend/` — Next.js (App Router) + TypeScript + Tailwind, mobile-first.
- `docker-compose.yml` — local PostgreSQL 16.

## Prerequisites

- Java 21
- Node.js 20.9+
- Docker (for the local database)

## Getting started

1. Start the database:

   ```sh
   docker compose up -d
   ```

2. Run the backend:

   ```sh
   cd backend
   ./gradlew bootRun
   ```

   Flyway applies migrations on startup. Health check: `http://localhost:8080/api/health`.

   Backend tests (uses Testcontainers, no local DB needed):

   ```sh
   cd backend
   ./gradlew test
   ```

3. Run the frontend:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

   App: `http://localhost:3000`. API base defaults to `http://localhost:8080/api`
   (override with `NEXT_PUBLIC_API_BASE_URL`).

## Phase 1 scope

Project skeleton + full PostgreSQL schema (see `backend/src/main/resources/db/migration/V1__init.sql`).
Authentication, profiles, games, and all feature endpoints land in later phases.
