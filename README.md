# Game Connect

Pickup football matchmaking platform (MVP).

- `backend/` — Spring Boot modular monolith (Java 21), PostgreSQL + Flyway,
  JWT session in an HttpOnly cookie.
- `frontend/` — Next.js (App Router) + TypeScript + Tailwind, mobile-first.
- `docker-compose.yml` — local PostgreSQL 16.

## Features

- **Accounts** — email/password registration and login; the JWT is stored in an
  HttpOnly `auth_token` cookie; logout clears it.
- **Profiles** — display name, bio, image URL, skill level, position, and
  aggregate player stats.
- **Discover games** — browse open games with filters (date, format, skill
  level, free-text search) and pagination.
- **Create games** — host a game with turf, time window, format, skill level,
  player limits, fee, and description.
- **Join requests** — request to join, owner accepts/rejects; a rejected player
  can request again.
- **Lifecycle** — `OPEN → FULL → IN_PROGRESS → COMPLETED`; games auto-complete
  after the end time plus a grace period, and can be cancelled.
- **Attendance** — the owner marks present/absent after the game starts, within
  the attendance window.
- **Ratings** — participants rate each other after a completed game.
- **My Games** — the current user's games grouped as Upcoming / In progress /
  Completed / Cancelled / All.

## Prerequisites

- Java 21
- Node.js 20.9+
- Docker (for the local PostgreSQL and for backend tests via Testcontainers)
- Windows: use PowerShell; the Gradle wrapper is `gradlew.bat`

## Local development

### 1. Start the database

```powershell
docker compose up -d
```

This starts PostgreSQL 16 on host port `5430` with database/user/password all
set to `gameconnect`, matching the backend defaults. Data is kept in the
`gameconnect-pgdata` volume.

### 2. Start the backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Flyway applies migrations on startup. Health check:

```powershell
curl http://localhost:8080/api/health
```

### 3. Start the frontend

```powershell
cd frontend
npm install
npm run dev
```

App: `http://localhost:3000`. The Next.js server rewrites `/api/*` to the
backend, defaulting to `http://localhost:8080/api`.

## Tests, lint, and build

Backend (uses Testcontainers — Docker must be running; no local DB needed):

```powershell
cd backend
.\gradlew.bat test
```

Run a single test class:

```powershell
cd backend
.\gradlew.bat test --tests "com.gameconnect.game.JoinRequestIntegrationTest"
```

Frontend (no test runner; verified with lint and the production build, which
includes type-checking):

```powershell
cd frontend
npm run lint
npm run build
```

## Configuration

The backend reads these environment variables (see `application.yml` for the
authoritative mapping and `.env.example` for a reference template). Set them in
the shell, service manager, or deployment platform — Spring Boot does not load a
`.env` file by itself. All have local development defaults, so nothing needs to
be set for the steps above.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5430/gameconnect` | PostgreSQL JDBC URL |
| `DB_USER` | `gameconnect` | Database user |
| `DB_PASSWORD` | `gameconnect` | Database password |
| `PORT` | `8080` | Backend HTTP port |
| `JWT_SECRET` | `myDefaultDevSecretKeyMustBeAtLeast32BytesLongForHs256` | HS256 signing key (≥ 32 bytes) |
| `JWT_EXPIRATION_MINUTES` | `60` | JWT **and** auth-cookie lifetime, in minutes |
| `GAME_COMPLETION_GRACE` | `6h` | Grace after a game's end time before auto-completion (ISO-8601 duration, e.g. `6h`, `30m`) |
| `COOKIE_SECURE` | `false` | Marks the auth cookie `Secure` (set `true` under HTTPS) |

Frontend:

| Variable | Default | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Build/dev-time target for the `/api/*` rewrite; set in `frontend/.env.local` or the shell |

`docker-compose.yml` does not read a `.env` file; its credentials are hardcoded
for local development only.

## Production notes

- **JWT secret (required).** The built-in default is a public development value.
  The backend logs a startup warning when it is still in use. Generate a long,
  random, unique secret and set `JWT_SECRET`. Changing it invalidates existing
  sessions.
- **HTTPS and cookies.** Terminate TLS at your reverse proxy and set
  `COOKIE_SECURE=true` so the auth cookie is only sent over HTTPS.
- **Database.** Point `DB_URL` / `DB_USER` / `DB_PASSWORD` at your managed
  PostgreSQL instance and use a dedicated, least-privilege role. Never commit
  real credentials; keep them in your deployment secret store.
- **Frontend API base.** Set `NEXT_PUBLIC_API_BASE_URL` to the public backend
  URL (it is baked in at build time).
- **Port.** Set `PORT` if the platform assigns one.
- **Migrations.** Flyway runs automatically on backend startup; no separate
  migration step is required.

> **Never use the built-in development JWT secret in production.** It is public
> and would let anyone forge sessions. Always set a strong `JWT_SECRET`.
