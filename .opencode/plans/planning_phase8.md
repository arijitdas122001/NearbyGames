# GameConnect Phase 8 rating system plan

**Session ID:** ses_f283ff8d0ffeFZCAqzly7zr5aR
**Created:** 9/25/2026, 2:18:01 PM
**Updated:** 9/25/2026, 2:42:14 PM

---

## User

We have now completed Phases 1–7 of the GameConnect project.

The current domain flow is:

Game created
→ players discover games
→ players request to join
→ owner accepts/rejects
→ game takes place
→ owner records attendance
→ game automatically becomes COMPLETED after the configured grace period
→ player statistics are derived from completed/attended games

Now I want you to **PLAN Phase 8 only**.

IMPORTANT:

* You are in PLAN mode.
* Do NOT implement anything yet.
* First inspect the actual current codebase.
* Read `AGENTS.md`.
* Inspect the implementations from Phases 1–7.
* Inspect the actual database migrations/schema.
* Inspect the current `player_rating` table and any existing rating-related code.
* Inspect `PlayerStatsRepository`.
* Inspect the current profile APIs and frontend.
* Inspect the Phase 6 join-request applicant statistics.
* Do not assume previous plans exactly match the current implementation.
* The current codebase is the source of truth.

Create:

`planning_phase8.md`

Do not modify application code while planning.

---

# Phase 8 Goal

Introduce a simple, trustworthy **player rating/reputation system** based on completed games.

The intended lifecycle becomes:

```text
Game completed
        ↓
Eligible participants can rate other eligible participants
        ↓
Rating is stored
        ↓
Player average rating/statistics are updated through derived queries
        ↓
Rating appears on player profile
        ↓
Rating can be shown in future join-request applicant information
```

The rating system should be deliberately simple for the MVP.

Do NOT build a recommendation engine, ranking algorithm, player leaderboard, or complex reputation score in this phase.

---

# 1. Inspect existing rating schema first

The V1 migration already contains a `player_rating` table.

Inspect its exact schema.

Determine:

* columns
* primary key
* foreign keys
* unique constraints
* indexes
* rating value constraints
* created/updated timestamps
* whether it already supports the desired Phase 8 behavior

Also inspect whether any `PlayerRating` entity/repository/service/controller already exists.

Do not create duplicate structures.

Determine whether a migration is required.

If the existing schema is sufficient, explicitly state:

**No migration required.**

If not, specify the exact migration needed and why.

---

# 2. Define rating rules

Design precise business rules.

A reasonable MVP model is:

* Only participants of the same completed game can rate one another.
* A player can rate another player only if both were participants in that game.
* The game must be `COMPLETED`.
* A player cannot rate themselves.
* Only players who actually attended should be eligible to give a rating.
* Only players who actually attended should be eligible to receive a rating.
* A player can submit at most one rating for another player for a given game.
* Rating should be immutable after submission for the MVP unless the existing schema strongly suggests otherwise.
* Rating value should be an integer, e.g. `1–5`.

However:

**Do not blindly adopt these assumptions.**

Inspect the existing schema and code first, then document the final rules in the plan.

Explicitly decide what happens when:

* participant attendance is `NULL`
* participant attendance is `false`
* game is not completed
* player tries to rate themselves
* player tries to rate someone from another game
* player submits a duplicate rating
* player tries to rate someone who did not attend
* player tries to rate after some time has passed

Keep the rules simple and defensible.

---

# 3. Decide the rating model

Determine whether the existing `player_rating` table represents:

```text
rater → rated player → game → rating
```

or some other structure.

The preferred conceptual model is:

```text
game_id
rater_id
rated_player_id
rating
created_at
```

with uniqueness:

```text
(game_id, rater_id, rated_player_id)
```

This means:

> In one completed game, one player can rate another player once.

Do not introduce a second rating table if the existing schema already models this.

---

# 4. Attendance eligibility

Phase 7 established:

* `attended = true` → attended
* `attended = false` → did not attend
* `attended = null` → not marked
* only attended players receive played-game credit

Use this as the basis for rating eligibility.

Determine and document:

### Can an absent player give ratings?

### Can an absent player receive ratings?

### Can an unmarked player give ratings?

### Can an unmarked player receive ratings?

Prefer a simple rule that prevents unreliable ratings.

---

# 5. Rating value

Use a simple integer rating scale.

Preferred MVP:

```text
1 = Very poor
2 = Poor
3 = Average
4 = Good
5 = Excellent
```

But keep the backend domain focused on the numeric value.

Do not build complex rating categories, weighted ratings, Elo, Glicko, skill calculations, or reputation formulas.

Validate:

```text
1 <= rating <= 5
```

at the API/domain boundary and enforce it at the database level if appropriate.

---

# 6. API design

Design the required APIs.

Potentially:

### Submit rating

```text
POST /api/games/{gameId}/ratings
```

Example:

```json
{
  "ratedPlayerId": "UUID",
  "rating": 5
}
```

### View ratings for a player

Potentially:

```text
GET /api/users/{userId}/ratings
```

However, inspect the existing profile API before deciding whether a separate endpoint is actually necessary.

### View rating eligibility for a completed game

Potentially:

```text
GET /api/games/{gameId}/ratings
```

But avoid unnecessary endpoints.

For every API define:

* HTTP method
* path
* authentication
* authorization
* request
* response
* validation
* error codes
* business rules

Follow existing GameConnect API conventions.

---

# 7. Profile integration

Inspect the existing:

```text
GET /api/users/me
GET /api/users/{id}
```

and existing `PlayerStatsResponse`.

Determine how rating information should appear.

Likely useful fields include:

```text
averageRating
ratingCount
```

But only add fields that are actually supported by the data model and useful for the product.

The profile should be able to communicate:

```text
Matches Played: 12
Attendance: 92%
Rating: 4.6 ★
Ratings: 8
```

Do not build a separate reputation score in this phase.

---

# 8. Existing Phase 6 integration

Phase 6's owner join-request panel already displays applicant information and player statistics.

Inspect the actual implementation.

Determine whether rating information can be naturally added to:

```text
ApplicantSummary
```

so that the owner can see something like:

```text
Arijit Das
Intermediate
Matches: 12
Rating: 4.6 ★
```

If the existing `PlayerStatsResponse` already contains `averageRating`, determine whether only a `ratingCount` is needed.

Do not duplicate rating queries unnecessarily.

Prefer reusing the existing statistics abstraction.

---

# 9. Rating visibility

Determine what should be public.

A reasonable MVP:

* Average rating is visible on player profiles.
* Rating count is visible.
* Individual rater identities and individual rating records do not need to be publicly exposed.
* Owner sees aggregate rating when reviewing a join request.

Document the final decision.

Do not expose unnecessary personal information.

---

# 10. Authorization and security

Use the authenticated principal.

Never trust a frontend-supplied `raterId`.

The backend should derive:

```text
raterId = authenticated user
```

The request may contain only:

```text
ratedPlayerId
rating
```

Verify server-side that:

1. game exists
2. game is COMPLETED
3. authenticated user participated
4. authenticated user attended
5. rated player participated
6. rated player attended
7. rater != rated player
8. rating does not already exist

Return appropriate existing/new business error codes.

Follow the existing:

```text
BusinessException
→ GlobalExceptionHandler
→ ApiError
```

pattern.

---

# 11. Transaction design

Rating submission should be transactional.

Consider race conditions around duplicate submissions.

The application should check for an existing rating, but the database uniqueness constraint should remain the final protection.

The desired behavior is:

```text
First submission → success
Concurrent duplicate → one succeeds, other gets a controlled conflict
```

Do not add pessimistic locking unless there is an actual need.

A database unique constraint is likely sufficient.

---

# 12. Statistics

Inspect the existing `PlayerStatsRepository`.

Determine how to calculate:

```text
averageRating
ratingCount
```

Prefer derived SQL aggregation such as:

```text
AVG(...)
COUNT(...)
```

rather than storing counters on `app_user`.

Do not create:

```text
user.average_rating
user.rating_count
```

unless there is a strong architectural reason.

The source of truth should remain `player_rating`.

Also verify that a player with no ratings receives a sensible value:

```text
averageRating = null
ratingCount = 0
```

rather than pretending they have a rating.

---

# 13. Frontend design

Inspect the current frontend structure.

The Phase 8 UI should be integrated into the existing pages rather than redesigning the application.

Likely areas:

### Completed game detail

For an eligible participant:

```text
Rate players

Arijit Das       ★ ★ ★ ★ ★
Rahul            ★ ★ ★ ★ ★
Amit             Already rated
```

Use a simple 1–5 rating control.

After submission:

```text
Rated: 5/5
```

Do not allow duplicate submission.

### Player profile

Display:

```text
Rating: 4.6 ★
8 ratings
```

If there are no ratings:

```text
No ratings yet
```

### Join request panel

If the backend already provides the aggregate rating through `PlayerStatsResponse`, display it naturally alongside the existing applicant statistics.

Do not create a second rating-fetch mechanism if it is unnecessary.

---

# 14. UX considerations

Keep the UI mobile-first.

Use:

* clear rating controls
* loading state
* disabled state during submission
* success feedback
* friendly error messages
* empty state
* no rating spam

Do not introduce a global state-management library.

Use the existing:

* `apiFetch`
* `useState`
* `useEffect`
* existing component conventions

---

# 15. Important edge cases

The plan must cover:

### Valid

* completed game
* both players attended
* first rating

### Invalid

* game not completed
* rater did not participate
* rater did not attend
* rated player did not participate
* rated player did not attend
* self-rating
* duplicate rating
* invalid rating value
* nonexistent game
* nonexistent player

### Concurrency

Two simultaneous attempts to create the same rating should not create duplicate rows.

---

# 16. Tests

Create a comprehensive integration test plan.

At minimum:

### Rating creation

* attended participant can rate another attended participant
* unauthenticated request → 401
* non-participant → appropriate error
* absent rater → rejected
* unmarked rater → rejected
* absent rated player → rejected
* unmarked rated player → rejected
* self-rating → rejected
* incomplete game → rejected
* nonexistent game → 404
* invalid rating 0 → 400
* invalid rating 6 → 400
* duplicate rating → 409

### Database integrity

* one rating per `(game, rater, rated)`
* concurrent duplicate submission does not create two rows

### Statistics

* average rating calculated correctly
* rating count calculated correctly
* no ratings → null average + zero count
* multiple games contribute correctly
* ratings from incomplete games do not exist / do not count

### Profile

* profile API returns rating information
* `/me` returns rating information
* Phase 6 applicant stats reflect rating information

Use real PostgreSQL/Testcontainers where database constraints and aggregation behavior matter.

---

# 17. Database migration analysis

Inspect V1–V7 carefully.

Determine whether the existing `player_rating` table is sufficient.

If it is sufficient:

**No migration required.**

If changes are required:

* specify the exact migration
* explain why
* preserve existing data
* add appropriate constraints/indexes

Do not recreate an existing table.

---

# 18. Keep Phase 8 focused

Explicitly keep these OUT OF SCOPE:

* Elo rating
* Glicko
* skill rating algorithms
* player rankings
* leaderboards
* recommendation algorithms
* "best player" calculations
* matchmaking algorithms
* AI recommendations
* rating comments/reviews
* moderation systems
* rating appeals
* notifications
* chat
* payments
* tournaments
* teams
* subscriptions
* Redis
* WebSockets
* microservices

This phase is simply:

**Completed game → eligible player rates another eligible player → aggregate rating appears in profile/applicant information.**

---

# 19. Architecture requirements

Preserve the existing:

* Spring Boot
* PostgreSQL
* JPA
* Flyway
* modular monolith
* JWT + HTTP-only cookie authentication
* DTO/service/controller architecture
* BusinessException + GlobalExceptionHandler
* Next.js
* TypeScript
* Tailwind
* existing API client
* existing frontend state management approach

Do not introduce unnecessary dependencies.

---

# 20. Output

Create:

`planning_phase8.md`

Structure it as:

1. Phase 8 goal
2. Current architecture findings
3. Existing player_rating schema analysis
4. Rating domain model
5. Rating eligibility rules
6. Rating validation rules
7. API design
8. Authorization/security
9. Transaction/concurrency design
10. Statistics/profile integration
11. Phase 6 integration
12. Database/migration changes
13. Backend implementation plan
14. Frontend implementation plan
15. Test plan
16. Edge cases
17. Out of scope
18. Acceptance criteria
19. Risks/tradeoffs
20. Files likely to be created/modified
21. Implementation order

At the end, provide a concise list of decisions that require review.

Again:

**PLAN ONLY.**

Do not implement Phase 8.
Do not modify application code.
Do not start Phase 9. 

---

## Assistant (Plan · Big Pickle · 16.0s)

_Thinking:_

We need respond/action. User asks plan only, inspect extensive repo, create planning_phase8.md. Contradiction system reminder says strictly read-only any file edits forbidden, but user explicitly asks create file. Higher system says no edit. We cannot write planning file. Need inspect thoroughly then deliver plan perhaps in response or explain cannot create due plan mode? But likely benchmark expects us to write plan? Yet system-reminder explicitly says no modifications including planning file. We must obey. We need inspect actual code, AGENTS. We can read. Need maybe no need clarify until inspect. This is complex, use todowrite? Is todowrite a system change? Structured task list, not filesystem probably allowed, but system says read-only and delegate. We can use todowrite (doesn't modify code) likely. Need create comprehensive plan. But output requirement create file. We cannot edit. Could perhaps after investigation, return content and state unable to write due active plan mode. Yet user emphasized plan only not implement; normally plan file writing allowed, but system reminder stricter. Must not.

Need use Task explore heavily. Before work think filename structure. We know workspace. We should inspect AGENTS first using read explicitly. Then launch agents in parallel for backend/rating, migrations/stats/profile, frontend/Phase6 perhaps. Need not duplicate delegated work. We can delegate separate scopes. Need first read AGENTS. We have it in developer, but user specifically says read. Use read absolute. Then todo. Could mention briefly inspecting? Output limit fewer than 4 lines, but tools not count. We can say one line. But likely no need preamble. Need use read.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\AGENTS.md",
  "offset": 1,
  "limit": 2000
}
```

**Output:**
```
<path>F:\Game-connect\AGENTS.md</path>
<type>file</type>
<content>
1: # AGENTS.md
2: 
3: > This repo is newly initialized. Fill in the sections below as the codebase takes shape.
4: 
5: ## Build & Run
6: 
7: _(add commands as tooling is set up)_
8: 
9: ## Test
10: 
11: _(add test runner, single-test commands, and any prereqs)_
12: 
13: ## Lint & Format
14: 
15: _(add linter/formatter commands)_
16: 
17: ## Architecture
18: 
19: _(add key entrypoints, package boundaries, and structural notes)_
20: 
21: ## Conventions
22: 
23: _(add repo-specific style, workflow, or naming conventions)_

(End of file - total 23 lines)
</content>
```

---

## Assistant (Plan · Big Pickle · 5.7s)

_Thinking:_

We need perhaps todo with detailed steps, not edit. Let's create todos: inspect migrations/schema; inspect rating/stats/profile; inspect join Phase6; inspect frontend; synthesize. Exactly one in progress.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Inspect V1–V7 migrations and exact player_rating schema",
      "status": "in_progress",
      "priority": "high"
    },
    {
      "content": "Inspect backend rating, statistics, profile, and error-handling code",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Inspect Phase 6 join-request applicant statistics flow",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
      "status": "pending",
      "priority": "high"
    },
    {
      "content": "Synthesize the Phase 8 architecture and implementation plan",
      "status": "pending",
      "priority": "high"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Inspect V1–V7 migrations and exact player_rating schema",
    "status": "in_progress",
    "priority": "high"
  },
  {
    "content": "Inspect backend rating, statistics, profile, and error-handling code",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Inspect Phase 6 join-request applicant statistics flow",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
    "status": "pending",
    "priority": "high"
  },
  {
    "content": "Synthesize the Phase 8 architecture and implementation plan",
    "status": "pending",
    "priority": "high"
  }
]
```

---

## Assistant (Plan · Big Pickle · 716.5s)

_Thinking:_

Now need inspect migrations. Could use explore agent for migrations and repository structure. Parallel maybe migration agent and frontend agent? Todo says one in progress, but only one task status; can still research. We need not duplicate. Use Task agents in parallel, as instructions encourage. Need detailed prompts, tell read-only, report exact file paths/line refs, no edits. Agent type explore. We can launch 4 agents concurrently, scopes separate. Agent 1 migrations/schema and related config, V1-V7; agent2 backend rating/stats/profile/errors/services; agent3 Phase6; agent4 frontend. The user said inspect implementations Phases 1-7. Agents should cover each phase perhaps a holistic architecture agent? We can do specific. Need understand phases 1-5 maybe auth, games, join, completion. Backend agent can cover. We can also ask migration agent to enumerate migrations and constraints. Need perhaps direct glob/read ourselves? Task can inspect. "Once delegated do not duplicate." We'll not independently inspect same scopes; use outputs. We can inspect package configs perhaps separate scope with our read or agent. Need line refs. Agents can return detailed report.

Let's launch 4 agents parallel via multi_tool.

**Tool: task**

**Input:**
```json
{
  "description": "Inspect database migrations",
  "prompt": "Read-only investigation only; do not modify any files. Thoroughness: very thorough. Inspect the actual repository at F:\\Game-connect, focusing on Flyway migrations V1 through V7, database configuration, and any schema/test setup. Report: repository/module layout relevant to backend/database; every migration file with purpose; the exact current player_rating definition including columns/types/nullability/defaults, PK, FKs and delete actions, unique constraints/checks/indexes, timestamps; whether later migrations alter it; exact references with file paths and line numbers; what Phase 8 needs or lacks; whether a V8 migration is needed. Also identify PostgreSQL/Testcontainers conventions and migration naming/style. Do not plan generic changes; base every statement on source.",
  "subagent_type": "explore",
  "command": "Inspect actual codebase for Phase 8 planning only"
}
```

**Output:**
```
<task id="ses_f283f8587ffeKhoxsMDFPfBQnx" state="completed">
<task_result>
# Read-only repository investigation

No files were modified and no tests were run. I inspected the tracked worktree at `master`/HEAD `31272af` and migration history across reachable Git refs.

## Key discrepancy: there are no V4–V7 migrations

The actual migration directory contains only:

- `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql`
- `F:\Game-connect\backend\src\main\resources\db\migration\V2__add_position_to_user.sql`
- `F:\Game-connect\backend\src\main\resources\db\migration\V3__add_game_discovery_index.sql`

There are no tracked or reachable-history `V4__`, `V5__`, `V6__`, or `V7__` files. There is no `V8__` file. The current migration history is consecutive V1–V3, and the latest generated Testcontainers run also records exactly three migrations and schema version 3 at `F:\Game-connect\backend\build\test-results\test\TEST-com.gameconnect.FlywaySchemaTest.xml:12-15`.

Migration version numbers do not correspond one-to-one with product phases: the repository has reached “Phase 7” while only three Flyway migrations exist.

---

## 1. Relevant repository/module layout

- The root README describes a Spring Boot modular monolith plus a separate Next.js frontend and local PostgreSQL Compose service: `F:\Game-connect\README.md:3-7`.
- Backend is a single Gradle module, not a multi-project build: `F:\Game-connect\backend\settings.gradle:1`.
- Database schema sources are under:
  - `F:\Game-connect\backend\src\main\resources\db\migration`
- Database configuration:
  - `F:\Game-connect\backend\src\main\resources\application.yml`
  - `F:\Game-connect\docker-compose.yml`
- Testcontainers setup:
  - `F:\Game-connect\backend\src\test\java\com\gameconnect\TestcontainersConfiguration.java`
  - `F:\Game-connect\backend\src\test\java\com\gameconnect\TestBackendApplication.java`
- Schema verification:
  - `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java`

Relevant backend package boundaries are:

- `auth`: `User` maps `app_user`; `UserRepository` is a JPA repository.
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\entity\User.java:14-53`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\repository\UserRepository.java:10-14`
- `game`: JPA entities/repositories for `game`, `join_request`, and `match_participant`; services/controllers for games, requests, attendance, and scheduled completion.
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\entity\Game.java:21-126`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\entity\JoinRequest.java:14-39`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\entity\MatchParticipant.java:14-38`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:24-53`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\MatchParticipantRepository.java:14-40`
- `profile`: profile DTOs/services plus a plain `@Repository` using `JdbcTemplate` for derived statistics.
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:8-53`
- There is no `PlayerRating` JPA entity, rating repository, rating service, rating controller, or backend rating DTO/request class.

There are no `schema.sql`, `data.sql`, or Java Flyway migrations under backend source.

---

## 2. Database and Flyway configuration

### Runtime configuration

`F:\Game-connect\backend\src\main\resources\application.yml:1-14` defines:

- Database: `jdbc:postgresql://localhost:5430/gameconnect`
- Username: `gameconnect`
- Password: `gameconnect`
- All three are environment-overridable through `DB_URL`, `DB_USER`, and `DB_PASSWORD`.
- Hibernate uses `ddl-auto: validate`, not create/update.
- Flyway is enabled.
- Migration location is `classpath:db/migration`.
- No separate schema, baseline, clean, out-of-order, or default-schema configuration is present.

The PostgreSQL driver is a runtime dependency, and Flyway plus the PostgreSQL Flyway database module are included at `F:\Game-connect\backend\build.gradle:21-32`.

### Local Docker database

`F:\Game-connect\docker-compose.yml:1-20` defines:

- `postgres:16`
- Database/user/password all `gameconnect`
- Host port `5430` mapped to container port `5432`
- Persistent `gameconnect-pgdata` volume
- `pg_isready` health check

### Test database

Tests use real PostgreSQL, not H2:

- Spring Boot Testcontainers integration: `F:\Game-connect\backend\build.gradle:39-42`
- PostgreSQL container bean using `postgres:16`: `F:\Game-connect\backend\src\test\java\com\gameconnect\TestcontainersConfiguration.java:9-16`
- `@ServiceConnection` supplies the datasource automatically; no separate test YAML/properties or dynamic properties are present.
- Every `@SpringBootTest` imports that configuration explicitly.
- The test-only manual launcher also includes it: `F:\Game-connect\backend\src\test\java\com\gameconnect\TestBackendApplication.java:5-9`.

---

## 3. Every actual migration and its purpose

### V1 — initial schema

`F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql`

Purpose and contents:

- Header explains that IDs are UUIDs and profile statistics are derived rather than stored: lines 1-3.
- Creates `app_user`: lines 5-17.
- Creates `game`, its business checks, and discovery/date/owner/skill/time indexes: lines 19-53.
- Creates `join_request`, status check, unique game/user constraint, and two indexes: lines 55-69.
- Creates `match_participant`, role check, unique game/user constraint, and user index: lines 71-83.
- Creates `player_rating`, rating checks/uniqueness, and ratee index: lines 85-99.

This migration already establishes the complete initial five-table domain schema, including ratings.

### V2 — add player position

`F:\Game-connect\backend\src\main\resources\db\migration\V2__add_position_to_user.sql`

Purpose:

- Adds nullable `app_user.position VARCHAR(20)`: lines 3-4.
- Adds `ck_app_user_position`, allowing null or one of six named positions: lines 6-11.
- Does not touch `player_rating`, `game`, or any relationship involving ratings.

### V3 — add game discovery index

`F:\Game-connect\backend\src\main\resources\db\migration\V3__add_game_discovery_index.sql`

Purpose:

- Adds the non-unique composite index:
  `idx_game_status_start ON game (status, start_time)`: line 1.
- Uses `IF NOT EXISTS`.
- Does not alter `player_rating` or its FKs.

### V4–V7

No migration files exist for V4, V5, V6, or V7 in:

- the working tree,
- HEAD’s Git tree,
- or migration history reachable through `git log --all`.

The only migration additions in reachable history are V1, V2, and V3.

---

## 4. Exact current `player_rating` definition

The authoritative definition is `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:85-99`.

### Columns

| Column | PostgreSQL type | Nullable | Default | Relationship |
|---|---|---:|---|---|
| `id` | `UUID` | No, because it is the PK | `gen_random_uuid()` | Primary key |
| `game_id` | `UUID` | No | None | FK to `game(id)` |
| `rater_id` | `UUID` | No | None | FK to `app_user(id)` |
| `ratee_id` | `UUID` | No | None | FK to `app_user(id)` |
| `score` | `SMALLINT` | No | None | Checked to 1–5 |
| `created_at` | `TIMESTAMPTZ` | No | `now()` | Creation timestamp |

There is no `updated_at` column, no completion timestamp, and no trigger maintaining `created_at`.

### Primary key

`id UUID PRIMARY KEY DEFAULT gen_random_uuid()` at `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:86`.

The SQL does not explicitly name the PK; PostgreSQL’s generated name is `player_rating_pkey`. It creates the implicit unique B-tree index on `id`.

### Foreign keys

Declared at `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:87-89`:

- `game_id → game.id`
- `rater_id → app_user.id`
- `ratee_id → app_user.id`

The FKs are unnamed in SQL, so PostgreSQL generates:

- `player_rating_game_id_fkey`
- `player_rating_rater_id_fkey`
- `player_rating_ratee_id_fkey`

There are no explicit `ON DELETE` or `ON UPDATE` clauses anywhere in the migrations. Therefore every FK has PostgreSQL’s default:

- `ON DELETE NO ACTION`
- `ON UPDATE NO ACTION`
- not deferrable / initially immediate

Ratings are not cascaded when a user or game is deleted; deletion is blocked while referencing ratings exist.

### Checks

At `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:93-94`:

- `ck_rating_score`: `score BETWEEN 1 AND 5`
- `ck_rating_self`: `rater_id <> ratee_id`

Because `score`, `rater_id`, and `ratee_id` are all `NOT NULL`, the checks cannot be bypassed with nulls.

### Unique constraint

At `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:95-96`:

- Named constraint: `uq_rating`
- Columns: `(game_id, rater_id, ratee_id)`
- Enforces at most one rating by a given rater for a given ratee in a given game.
- Creates the corresponding unique B-tree index named `uq_rating`.

### Indexes

1. `player_rating_pkey` — implicit unique index on `id`.
2. `uq_rating` — implicit unique index on `(game_id, rater_id, ratee_id)`.
3. `idx_player_rating_ratee` — explicit non-unique index on `ratee_id`, at `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:99`.

There is no standalone `rater_id` index and no index on `score` or `created_at`. The composite unique index can use `game_id` as its leading key.

### What the schema does not enforce

The table verifies that the game and both users exist, but it does not verify that:

- the game is `COMPLETED`,
- the rater participated,
- the ratee participated,
- either rating concerns a cancelled or incomplete game.

The product requires ratings only after completed matches and prohibits rating players for games the user did not participate in: `F:\Game-connect\PRODUCT.MD:192-204` and `F:\Game-connect\PRODUCT.MD:298-302`. Those rules are not represented by a FK, check, or trigger in `player_rating`.

---

## 5. Do later migrations alter `player_rating`?

No.

- V2 changes only `app_user.position`: `F:\Game-connect\backend\src\main\resources\db\migration\V2__add_position_to_user.sql:1-11`.
- V3 adds only a `game` index: `F:\Game-connect\backend\src\main\resources\db\migration\V3__add_game_discovery_index.sql:1`.
- V4–V7 do not exist.

Thus the current definition remains exactly the V1 definition above.

---

## 6. Migration naming and SQL style

Observed conventions:

- Standard Flyway filenames: `V<number>__<snake_case_description>.sql`.
- Descriptions are lower snake case:
  - `init`
  - `add_position_to_user`
  - `add_game_discovery_index`
- SQL is uppercase, semicolon-terminated, and uses aligned column definitions.
- Constraints are explicitly named with:
  - `ck_` for checks
  - `uq_` for unique constraints
  - `idx_` for explicit indexes
- UUID defaults use `gen_random_uuid()`.
- Timestamp defaults use `now()` with `TIMESTAMPTZ`.
- FKs are generally inline and unnamed, relying on PostgreSQL-generated names.
- No `ON DELETE` actions are specified.
- The repository’s sequence is V1, V2, V3; migration versions are not tied to phase numbers.
- The later V3 migration uses `IF NOT EXISTS`; V1/V2 do not generally use idempotent DDL syntax.
- No Java-based migrations, schema snapshots, seeds, or separate `schema.sql` exist.

If a new migration were ever justified, continuing the observed consecutive naming convention would mean `V4__...`, not `V8__...`; the source contains no requirement to preserve phase numbers in Flyway versions.

---

## 7. Schema and integration-test conventions

### Flyway schema test

`F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java`:

- Uses the same Testcontainers PostgreSQL setup: lines 18-26.
- Calls `flyway.migrate()` and checks only that at least one migration is applied: lines 28-33.
- Verifies the five table names: lines 35-42.
- Verifies `app_user.position`: lines 44-54.
- Verifies selected constraint names, including `uq_rating`, `ck_rating_score`, and `ck_rating_self`: lines 56-76.

It does **not** assert:

- an exact migration count of three,
- column types/nullability/defaults,
- PK or FK definitions,
- FK delete actions,
- index definitions,
- `idx_player_rating_ratee`,
- the V3 index,
- actual rating inserts or constraint violations.

### Integration-test structure

Game/profile/auth integration tests generally use:

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
```

Examples:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java:27-30`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:24-27`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameIntegrationTest.java:37-40`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameDiscoveryIntegrationTest.java:40-43`

There is no `@Testcontainers`/`@Container` convention; Spring manages the `PostgreSQLContainer` bean through `@ServiceConnection`.

Tests needing past/future times or non-open statuses insert games through JPA repositories or use direct JDBC cleanup, as documented at `F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:40-48`.

### Cleanup convention

The three database-heavy game tests clean in FK-safe child-to-parent order:

1. `player_rating`
2. `match_participant`
3. `join_request`
4. `game`
5. `app_user`

References:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameDiscoveryIntegrationTest.java:68-75`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:73-80`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java:82-89`

That order is consistent with the absence of cascading FKs.

### Generated artifact caveat

The ignored build report is useful corroboration but was not produced by this investigation. It records three validated migrations and schema version 3 at `F:\Game-connect\backend\build\test-results\test\TEST-com.gameconnect.FlywaySchemaTest.xml:12-15`. The ignored older runtime log reports version 1 at `F:\Game-connect\backend\run-app-final.log:28-31`; that is a stale runtime artifact, not the current tracked migration source.

---

## 8. Phase 8 assessment

There is a numbering mismatch in the source.

### Canonical PRODUCT.md Phase 8

`F:\Game-connect\PRODUCT.MD:330-337` defines:

- Phase 7: game owner management
- Phase 8: match completion and attendance
- Phase 9: ratings and statistics

However, the implemented attendance/completion work is labeled **Phase 7** in both its plan and tests:

- `F:\Game-connect\.opencode\plans\planning_phase7.md:1-23`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:40-44`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java:43-47`

That functionality is already implemented:

- Nullable `match_participant.attended`: `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:71-80`
- `COMPLETED` accepted by the game status check: `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:35-42`
- Owner-only attendance update and game-row locking: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:55-94`
- Automatic completion scheduler: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\scheduler\GameCompletionScheduler.java:31-39`
- Completed-game-only statistics: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:22-42`

The tracked Phase 7 plan explicitly says no migration is required because V1–V3 already support it: `F:\Game-connect\.opencode\plans\planning_phase7.md:49-60` and `F:\Game-connect\.opencode\plans\planning_phase7.md:186-188`.

Therefore, if “Phase 8” means the phase written in `PRODUCT.MD`, it is already satisfied and needs no schema migration.

### If “Phase 8” means the next repo-local ratings phase

There is no `planning_phase8.md`; tracked plans end at phase 7. If the intended next work is ratings/statistics—which PRODUCT calls Phase 9—the source currently provides:

- The complete `player_rating` storage table and its basic constraints.
- An average-rating query:
  `SELECT AVG(pr.score) FROM player_rating pr WHERE pr.ratee_id = ?`
  at `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-49`.
- `averageRating` in the profile statistics DTO: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`.
- Profile exposure through `ProfileService`: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-90`.
- Applicant-summary exposure: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-263`.
- A frontend `PlayerRating` interface: `F:\Game-connect\frontend\lib\types.ts:192-199`.
- Frontend display of average rating: `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:214-229`.

It lacks:

- Any rating write path or rating endpoint.
- A `PlayerRating` entity.
- A rating repository.
- Rating request/response DTOs.
- Service validation for completed-game status and participant membership.
- Any rating behavior integration test.
- Any test that inserts a rating and verifies score range, no-self behavior, uniqueness, default timestamp, or average output.

The only rating-related test references are schema existence/constraint-name checks, cleanup, and the initial null average response:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java:35-76`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:48-60`

## V8 migration conclusion

**No V8 migration is needed by any current repository requirement.**

- Canonical Phase 8 completion/attendance is already supported by V1–V3.
- Basic ratings and average-rating reads are also already supported by V1 plus the existing JDBC query.
- The two product rules not enforced by the current rating table—completed status and participation membership—are application/business-rule gaps; the source does not require them to be implemented relationally.
- Since the current highest migration is V3 and V4–V7 do not exist, a future migration justified by a new schema change would follow the observed sequence as **V4**, not V8.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Inspect backend rating flows",
  "prompt": "Read-only investigation only; do not modify any files. Thoroughness: very thorough. Inspect F:\\Game-connect backend source (excluding Phase 6 applicant UI details, except noting relevant shared profile/stats APIs). Map actual current architecture and Phases 1–7 implementation relevant to Phase 8: auth principal extraction, game lifecycle/completion/grace-period scheduling, participant/attendance entities and services, rating-related code of any kind, PlayerStatsRepository and all query mappings, PlayerStatsResponse/DTOs, GET /api/users/me and /api/users/{id}, error codes and BusinessException/GlobalExceptionHandler/ApiError conventions, controller/service/repository transaction patterns, JPA auditing/timestamps. Identify exact files, methods, query semantics, null/empty aggregate behavior, authorization patterns, and test conventions. Include precise file:line references. Explicitly state whether duplicate rating structures exist and likely files to modify/create, but do not implement.",
  "subagent_type": "explore",
  "command": "Inspect actual codebase for Phase 8 planning only"
}
```

**Output:**
```
<task id="ses_f283f7c9cffegCg3VE5Hs3bIPj" state="completed">
<task_result>
I continued the read-only audit; no clarification was needed. No files were modified, and no tests or builds were run.

## Overall finding

Phase 8 attendance and automatic completion are already implemented in the backend, frontend, and integration tests.

The main documentation issue is phase numbering:

- `F:\Game-connect\PRODUCT.MD:312-337` defines Phase 7 as owner management and Phase 8 as match completion/attendance.
- `F:\Game-connect\.opencode\plans\planning_phase7.md:1-23` labels the attendance/completion work as Phase 7.
- The implementation follows the attendance/completion plan, despite the product’s numbering.

## Actual phase map

- **Phase 1 — Foundation/schema**
  - PostgreSQL/Flyway schema: `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:5-99`
  - Position migration: `F:\Game-connect\backend\src\main\resources\db\migration\V2__add_position_to_user.sql:1-11`
  - Discovery index: `F:\Game-connect\backend\src\main\resources\db\migration\V3__add_game_discovery_index.sql:1`
  - JPA/Flyway configuration: `F:\Game-connect\backend\src\main\resources\application.yml:8-14`

- **Phase 2 — Authentication**
  - Security chain and public endpoints: `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:33-77`
  - JWT generation/parsing: `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtService.java:21-59`
  - Cookie authentication: `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java:22-89`
  - Register/login/logout/me: `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:30-115`
  - Authentication rules: `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\service\AuthService.java:26-55`

- **Phase 3 — Profile/statistics**
  - Profile endpoints: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\controller\ProfileController.java:30-46`
  - Profile/statistics assembly: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:28-103`
  - Derived stats: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:22-53`

- **Phase 4 — Game creation**
  - Create/list/detail routes: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\GameController.java:38-65`
  - Game creation and owner participant transaction: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:55-94`
  - Validation: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\CreateGameRequest.java:18-64`

- **Phase 5 — Game discovery/detail**
  - Discovery query: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:33-51`
  - Listing/detail mapping: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:148-289`

- **Phase 6 — Join requests**
  - Routes: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:25-66`
  - Request/accept/reject logic: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:52-273`
  - Game-row locking: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:26-31`

- **Phase 8 — Attendance/completion**
  - Attendance service: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:46-155`
  - Attendance routes: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\AttendanceController.java:22-48`
  - Scheduler: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\scheduler\GameCompletionScheduler.java:16-40`
  - Scheduling enabled: `F:\Game-connect\backend\src\main\java\com\gameconnect\config\SchedulingConfiguration.java:6-9`
  - Participant/detail contract: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:194-239`

## Phase 8 API behavior

### Roster

`GET /api/games/{gameId}/participants`

- Implemented at `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\AttendanceController.java:32-37`.
- Service method is read-only and owner-only: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:46-53`.
- Results are ordered by `joinedAt ASC`.
- Response fields are defined in `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\ParticipantResponse.java:9-18`.
- The owner is included as a `OWNER` participant; pending/rejected requests are not included.

### Attendance marking

`PATCH /api/games/{gameId}/participants/{participantId}/attendance`

- Implemented at `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\AttendanceController.java:39-47`.
- Body is `{"attended": true|false}` and requires a non-null value via `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\AttendanceRequest.java:5-8`.
- Only the authenticated game owner can update it: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:58-75,135-141`.
- The participant lookup is scoped to the game, preventing cross-game participant updates: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:67-72`.
- Marking is allowed from `startTime` through `endTime + grace`, inclusive: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:96-125`.
- Repeated writes are idempotent; the same boolean can be written again.

### Automatic completion

There is no HTTP completion endpoint.

- Internal method: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:80-94`.
- Scheduler runs every 60 seconds after a 30-second initial delay: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\scheduler\GameCompletionScheduler.java:31-39`.
- Eligible statuses are `OPEN`, `FULL`, and `IN_PROGRESS`.
- The scheduler selects games with `endTime < now - grace`, then rechecks the locked game row before setting `COMPLETED`.
- Missing, already completed, and cancelled games are skipped.

## Transactions and concurrency

- Game creation and owner-participant creation share one transaction: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:55-93`.
- Join-request acceptance locks the game row: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:134-173`.
- Attendance updates lock the same game row: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:55-75`.
- Completion locks the same game row: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:80-94`.
- `PESSIMISTIC_WRITE` is defined at `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:26-31`.

This serializes attendance writes against completion. A write that loses the race observes `COMPLETED` and returns `GAME_ALREADY_COMPLETED`.

One smaller observation: `JoinRequestService` defines `findByIdForUpdate` for join requests at `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\JoinRequestRepository.java:25-27`, but the service relies on locking the game row instead. That still serializes decisions for the same game.

## Statistics and ratings

The attendance-derived statistics are implemented as intended:

- Query: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:22-42`
- Only games with `g.status = 'COMPLETED'` contribute.
- `matchesPlayed` and `matchesCompleted` are identical counts of distinct completed games where `attended = TRUE`.
- `attendanceRate` is:
  - `NULL` when the user has no completed-game participation.
  - `0.0` when completed-game participation exists but attendance is false or null.
  - `attended / all completed-game participations` otherwise.
- Profile responses embed these values through `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`.
- Join-request applicant summaries reuse the same repository: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`.

Rating findings:

- No backend `PlayerRating` entity, repository, service, controller, or rating-write DTO exists.
- The only backend rating functionality is the aggregate query at `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-49`.
- The database table and constraints exist at `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:85-99`.
- The frontend has only a placeholder `PlayerRating` interface at `F:\Game-connect\frontend\lib\types.ts:192-199`; it has no API client or usage.
- Therefore, there are no duplicate backend rating implementations. The frontend type is an unused future-phase placeholder.

Important future correctness issue: `findAverageRating` averages all rows in `player_rating` without joining to `game` or `match_participant`. If ratings are added later, the query must enforce completed-game and participation rules from `F:\Game-connect\PRODUCT.MD:194-204,298-304`.

## Timestamps and state persistence

- `User`, `Game`, `MatchParticipant`, and `JoinRequest` manually initialize IDs/timestamps in `@PrePersist`.
- There is no JPA auditing or `@PreUpdate` behavior.
- `createdAt`, `joinedAt`, and `decidedAt` are persisted as `Instant`/`TIMESTAMPTZ`.
- `attended` is a nullable `Boolean`; no timestamp records when attendance was marked.
- There is no `completedAt` or actual completion timestamp; completion is represented only by `Game.status = COMPLETED`.
- The application sets the JVM default timezone to `Asia/Kolkata`: `F:\Game-connect\backend\src\main\java\com\gameconnect\BackendApplication.java:10-12`.
- Grace-period comparisons use `Instant.now()` directly rather than an injected `Clock`: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:89-90,112-120`.

## Frontend contract status

The Phase 8 frontend is already wired:

- Participant API: `F:\Game-connect\frontend\lib\api\participants.ts:5-22`
- Shared participant and participation types: `F:\Game-connect\frontend\lib\types.ts:81-119`
- Attendance UI: `F:\Game-connect\frontend\components\games\AttendancePanel.tsx:33-225`
- Game status badge: `F:\Game-connect\frontend\components\games\GameStatusBadge.tsx:3-31`
- Game-detail integration: `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:159-227`
- Cookie/API error handling: `F:\Game-connect\frontend\lib\api\client.ts:1-50`

There are some stale or incomplete frontend contracts:

- `UserProfile` at `F:\Game-connect\frontend\lib\types.ts:21-29` lacks the backend profile’s `position` and `stats`.
- `MeUser` at `F:\Game-connect\frontend\lib\api\auth.ts:9-13` lacks backend `position` and typed `SkillLevel`.
- `GameParticipant` at `F:\Game-connect\frontend\lib\types.ts:156-163` does not match the active backend `ParticipantResponse`; the active `ParticipantDetail` type does.
- `/my-games` and `/profile` are linked by `F:\Game-connect\frontend\components\layout\BottomNav.tsx:8-13`, but those pages do not exist. This is consistent with the plan’s future-phase limitation, but currently produces broken navigation.
- Tests expect nullable fields to be omitted, but no explicit Jackson null-inclusion configuration is visible in the inspected configuration. This should be verified before depending on omission semantics.

## Error behavior

Global error mapping is complete and centralized:

- `BusinessException` → supplied status/code: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:25-28`
- Bean validation → `400 VALIDATION_ERROR`: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:30-44`
- Malformed JSON/enums → `400 MALFORMED_BODY`: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:46-50`
- Invalid query parameters → `400 VALIDATION_ERROR`: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:52-57`
- Missing resources → `404 NOT_FOUND`: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:59-62`
- Uncaught errors → `500 INTERNAL_ERROR`: `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:64-68`

Attendance-specific codes are implemented in `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:96-123`:

- `GAME_NOT_FOUND`
- `PARTICIPANT_NOT_FOUND`
- `FORBIDDEN`
- `GAME_NOT_STARTED`
- `GAME_ALREADY_COMPLETED`
- `GAME_NOT_ACTIVE`
- `VALIDATION_ERROR`

## Test coverage

Relevant tests are already present:

- Attendance API and roster: `F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:179-451`
- Scheduler, completion, and stats: `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java:142-326`
- Game creation: `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameIntegrationTest.java:122-449`
- Discovery/detail: `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameDiscoveryIntegrationTest.java:152-484`
- Profile/statistics: `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:38-277`
- Auth/security: `F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java:41-319`
- Flyway schema: `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java:14-77`

Coverage gaps include:

- No real concurrent attendance-versus-completion race test; the existing tests exercise the lock behavior sequentially.
- No test for a configurable non-default grace period.
- No average-rating rows or rating eligibility tests.
- No test for scheduler failures on one game not preventing processing of later games.
- No frontend automated tests.
- Tests were not executed during this audit.

## Main follow-up risks

1. **Rating query correctness:** future rating writes must restrict ratings to completed games and participating users.
2. **Phase/plan drift:** `PRODUCT.MD` and `planning_phase7.md` disagree about phase ownership.
3. **Product owner-management gaps:** `PRODUCT.MD:49-52` mentions removing a player and closing a game, but no such APIs or service methods exist.
4. **No completion timestamp:** future match-history features will need one.
5. **Stale frontend contracts:** profile, auth, and participant types need alignment before adding profile/match-history pages.
6. **Non-transactional profile/statistics aggregation:** `ProfileService.updateProfile` is not enclosed in one service transaction, so the profile save and subsequent stats reads are separate operations.
7. **Scheduler resilience:** an exception while completing one game can stop the current scheduler loop; the next scheduled run may recover, but there is no per-game error isolation or logging policy.

The audit is now sufficiently complete to identify the implemented Phase 8 architecture and the remaining documentation, contract, and future-phase risks.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Inspect applicant statistics",
  "prompt": "Read-only investigation only; do not modify any files. Thoroughness: very thorough. Inspect F:\\Game-connect actual Phase 6 join-request implementation end to end: domain status/entities, request DTOs, owner endpoints, applicant summary assembly, PlayerStatsRepository usage, authorization, frontend types/hooks/components rendering applicant data. Determine exactly where aggregate rating could be reused and whether adding averageRating/ratingCount to shared PlayerStatsResponse or ApplicantSummary is safest. Capture exact file paths and line numbers, current JSON shapes, stats fields, and test coverage. Also note any N+1/query behavior or consistency concerns relevant to Phase 8. Return findings only, no edits.",
  "subagent_type": "explore",
  "command": "Inspect actual codebase for Phase 8 planning only"
}
```

**Output:**
```
<task id="ses_f283f7587ffe5w3y77WAeW499M" state="completed">
<task_result>
The read-only audit is complete. Should I proceed with one of these?

1. Create a minimal implementation plan for a coherent `averageRating` + `ratingCount` aggregate.
2. Add the join-request and aggregate-rating test plan without changing files.
3. Implement the changes, which would expand the current read-only scope.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Inspect frontend architecture",
  "prompt": "Read-only investigation only; do not modify any files. Thoroughness: very thorough. Inspect F:\\Game-connect frontend structure and implementation for Phases 1–7, excluding detailed Phase 6 applicant ownership unless needed. Focus on Next.js/App Router structure, TypeScript, Tailwind, existing apiFetch/auth behavior, profile pages and /me rendering, public user profile rendering, completed-game/game-detail route and participant attendance display, reusable UI components, state/loading/error patterns, tests/lint/build scripts. Identify exact integration points for Phase 8 rating submission UI and aggregate rating display, with file:line references. Confirm no existing rating UI/code. Return a detailed source-grounded report; do not edit or propose redesign.",
  "subagent_type": "explore",
  "command": "Inspect actual codebase for Phase 8 planning only"
}
```

**Output:**
```
<task id="ses_f283f6e27ffeJDr2A0JeRlJenh" state="completed">
<task_result>
# Read-Only Investigation Report

## Executive Summary

- The repository is clean; `git status --short` returned no output. No files were modified.
- The current implementation is through repository commit `31272af` (`adding phase 7, complete player lifecycle`).
- The frontend is a Next.js App Router application using TypeScript, Tailwind CSS v4, local React state, and a centralized `apiFetch` client.
- Existing Phase 1–7 frontend coverage includes authentication, game creation, discovery, join requests, game detail, attendance, and automatic completion.
- There is no `/profile`, `/profile/[id]`, or `/my-games` page.
- There is no rating submission UI, ratings API module, rating endpoint, rating controller, or rating service.
- There is existing rating-related scaffolding:
  - `PlayerStats.averageRating`
  - `PlayerRating`
  - the database `player_rating` table
  - an average-rating SQL query
  - an applicant `Rating: x.x` display in `JoinRequestsPanel`
- The current product roadmap labels completion/attendance as Phase 8 and ratings/statistics as Phase 9, while the repository calls the implemented attendance work Phase 7. The requested rating work is therefore the next repository phase, but it corresponds to product Phase 9.

---

## 1. Frontend Structure and Tooling

### App Router structure

The complete frontend route inventory is:

```text
F:\Game-connect\frontend\app\
├── layout.tsx
├── page.tsx
├── globals.css
├── login\
│   └── page.tsx
├── register\
│   └── page.tsx
└── (app)\
    ├── layout.tsx
    ├── create-game\
    │   └── page.tsx
    └── games\
        ├── page.tsx
        └── [id]\
            └── page.tsx
```

Evidence:

- `F:\Game-connect\frontend\app\layout.tsx:16-32`
  - Defines global metadata.
  - Loads Geist fonts.
  - Applies global CSS.
  - Wraps the entire application in `AuthProvider`.
- `F:\Game-connect\frontend\app\(app)\layout.tsx:5-11`
  - Supplies the authenticated mobile shell.
  - Constrains content to `max-w-md`.
  - Adds bottom padding for `BottomNav`.
- `F:\Game-connect\frontend\app\page.tsx:1-3`
  - Returns `null`; actual root routing is handled by `proxy.ts`.
- `F:\Game-connect\frontend\proxy.ts:31-35`
  - Redirects `/` to `/games` when an auth cookie exists.
  - Redirects `/` to `/login` otherwise.

The application uses client-side fetching for the implemented authenticated pages rather than server components or route loaders.

### TypeScript

`F:\Game-connect\frontend\tsconfig.json:2-33` confirms:

- `strict: true`
- `noEmit: true`
- `moduleResolution: "bundler"`
- `jsx: "react-jsx"`
- `@/*` aliases to the frontend root
- `.next/types` and `.next/dev/types` included

The domain model is centralized in:

- `F:\Game-connect\frontend\lib\types.ts`

That file contains:

- Enums and unions for skills, formats, statuses, request statuses, roles, positions, and join decisions: `1-19`
- Profile/stat types: `21-36`
- Game/list/detail types: `38-154`
- Participant, join-request, applicant, and rating types: `156-199`

There are no runtime schema validators. `apiFetch<T>()` relies on TypeScript casts rather than validating response payloads at runtime.

### Tailwind CSS

Tailwind v4 is configured CSS-first:

- `F:\Game-connect\frontend\app\globals.css:1`
  - `@import "tailwindcss";`
- `F:\Game-connect\frontend\app\globals.css:3-20`
  - Defines CSS variables and theme mappings.
- `F:\Game-connect\frontend\app\globals.css:22-26`
  - Defines body colors and font.
- `F:\Game-connect\frontend\postcss.config.mjs:1-7`
  - Uses `@tailwindcss/postcss`.

There is no `tailwind.config.*` file. UI styling is composed inline with Tailwind utility classes. Existing reusable UI is limited to local game/layout components; there is no shared design-system package or component library.

### Scripts and dependencies

`F:\Game-connect\frontend\package.json:5-24` provides:

```text
npm run dev
npm run build
npm run start
npm run lint
```

Dependencies:

- Next.js `16.3.4`
- React/React DOM `19.2.8`
- TypeScript 5
- Tailwind CSS 4
- ESLint 9
- `eslint-config-next` `16.3.4`

`F:\Game-connect\frontend\AGENTS.md:3-7` warns that this Next.js version contains breaking changes and that bundled Next documentation should be checked before implementation.

---

## 2. `apiFetch` and Authentication Behavior

### Central API client

`F:\Game-connect\frontend\lib\api\client.ts:1-50` provides the shared client.

Key behavior:

- Base path is `/api`: line 1.
- Requests include credentials: lines 21-24.
- JSON bodies are serialized automatically: lines 25-28.
- Non-success responses become `ApiError`: lines 31-42.
- Backend `code` and `message` fields are parsed from JSON errors: lines 32-39.
- HTTP 204 returns `undefined`: lines 45-47.
- Successful non-204 responses are cast directly to the requested generic type: line 49.

`F:\Game-connect\frontend\next.config.ts:3-11` rewrites browser `/api/*` requests to:

```text
NEXT_PUBLIC_API_BASE_URL ?? http://localhost:8080/api
```

The frontend therefore uses same-origin API calls and relies on the browser cookie being sent.

### Auth context

`F:\Game-connect\frontend\lib\auth-context.tsx:8-14` exposes:

- `user`
- `loading`
- `login`
- `register`
- `logout`

`F:\Game-connect\frontend\lib\auth-context.tsx:23-36`:

- Calls `fetchMe()` during provider mount.
- Cancels updates after unmount.
- Treats any failed `/auth/me` request as an unauthenticated state.

`F:\Game-connect\frontend\lib\auth-context.tsx:38-54`:

- Login and registration set the returned user.
- Both redirect to `/games`.
- Logout calls the API, clears user state, and redirects to `/login`.

`F:\Game-connect\frontend\lib\api\auth.ts:3-35` defines the current auth models and calls:

- `register()` → `POST /auth/register`
- `login()` → `POST /auth/login`
- `fetchMe()` → `GET /auth/me`
- `logout()` → `POST /auth/logout`

The backend sets and clears the HTTP-only `auth_token` cookie in:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:54-70`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:93-115`

### Route protection

`F:\Game-connect\frontend\proxy.ts:4-7` uses the presence of the `auth_token` cookie and protects:

- `/games`
- `/create-game`
- `/my-games`
- `/profile`

`F:\Game-connect\frontend\proxy.ts:24-29` redirects unauthenticated requests to `/login`.

The proxy only checks whether the cookie exists. It does not validate the JWT. Backend validation is authoritative:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java:40-71`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:39-44`

The frontend does not globally handle a 401 from `apiFetch`. A stale or invalid cookie can pass the proxy, after which API calls return 401 and the individual page handles the error locally.

`AuthContext.loading` is exposed but not consumed by the implemented pages/components. The game detail page reads `user` directly:

- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:64-69`

This matters for any rating action added to that page because `user === null` initially can mean either “not loaded yet” or “not authenticated.”

---

## 3. Phase 1–7 Implementation Map

### Phase 1: Foundation and schema

- PostgreSQL/Flyway schema is defined in:
  - `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:1-99`
- The schema already includes:
  - `app_user`
  - `game`
  - `join_request`
  - `match_participant`
  - `player_rating`
- The rating table already exists:
  - `V1__init.sql:85-99`
- `match_participant.attended` exists:
  - `V1__init.sql:71-83`

### Phase 2: Authentication

Frontend:

- `F:\Game-connect\frontend\app\login\page.tsx:8-94`
- `F:\Game-connect\frontend\app\register\page.tsx:8-112`
- `F:\Game-connect\frontend\lib\auth-context.tsx:18-67`
- `F:\Game-connect\frontend\lib\api\auth.ts:15-35`
- `F:\Game-connect\frontend\proxy.ts:4-44`

Backend:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:30-117`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java:22-89`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:33-78`

### Phase 3: User profile backend

The backend profile API exists:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\controller\ProfileController.java:20-46`
  - `GET /api/users/me`
  - `GET /api/users/{id}`
  - `PATCH /api/users/me`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:28-103`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UpdateProfileRequest.java:12-34`

Frontend profile implementation is absent. There is no:

- `lib/api/profiles.ts`
- `app/(app)/profile/page.tsx`
- `app/(app)/profile/[id]/page.tsx`
- profile edit form
- profile stats view

### Phase 4: Game creation

Frontend:

- `F:\Game-connect\frontend\app\(app)\create-game\page.tsx:20-89`
- `F:\Game-connect\frontend\app\(app)\create-game\page.tsx:94-290`
- `F:\Game-connect\frontend\lib\api\games.ts:11-16`

The form:

- Collects turf, address, date, times, format, skill, capacity, fee, and description.
- Converts IST input to UTC ISO timestamps at lines 13-18 and 69-70.
- Redirects to the new game detail route at lines 78-79.

Backend creation:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\GameController.java:38-44`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:55-94`

The owner is automatically created as an `OWNER` participant:

- `GameService.java:86-91`

### Phase 5: Game discovery and detail

Frontend:

- `F:\Game-connect\frontend\app\(app)\games\page.tsx:17-148`
- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:59-298`
- `F:\Game-connect\frontend\components\games\GameFilters.tsx:16-120`
- `F:\Game-connect\frontend\components\games\GameCard.tsx:12-57`
- `F:\Game-connect\frontend\lib\api\games.ts:18-32`
- `F:\Game-connect\frontend\lib\format.ts:1-38`

The listing page:

- Stores filters and pagination in local state.
- Fetches with a cancellable `useEffect`.
- Uses `ApiError.message` for displayed errors.
- Shows loading, empty, error, and pagination states.

Backend discovery:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:33-51`
  - Filters by `status`
  - Excludes games whose start time is in the past
  - Filters by date, format, skill, and text
  - Sorts by start time
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:148-192`
  - Explicitly passes `GameStatus.OPEN` at line 170.
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\GameController.java:46-59`

The discovery list therefore does not expose completed games.

### Phase 6: Join requests

Relevant frontend integration:

- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:203-218`
- `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:51-219`
- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:62-270`
- `F:\Game-connect\frontend\lib\api\join-requests.ts:5-35`

The detail page renders different panels based on ownership:

- Owner branch: `games/[id]/page.tsx:203-210`
- Non-owner branch: `games/[id]/page.tsx:211-218`

The only existing rating-related display is the applicant summary:

- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:214-229`
- Specifically, `Rating: ${averageRating.toFixed(1)}` at lines 226-228.

Backend population of that value:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\ApplicantSummary.java:9-16`

### Phase 7 repository implementation: attendance and completion

Frontend:

- `F:\Game-connect\frontend\components\games\AttendancePanel.tsx:33-225`
- `F:\Game-connect\frontend\components\games\GameStatusBadge.tsx:3-29`
- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:32-57`
- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:203-227`
- `F:\Game-connect\frontend\lib\api\participants.ts:5-21`

Backend:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\AttendanceController.java:22-48`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:46-155`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\scheduler\GameCompletionScheduler.java:31-40`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\config\SchedulingConfiguration.java:1-9`

The current behavior is:

- Owners can list the roster and mark attendance.
- Players cannot use the roster endpoint.
- Attendance is allowed from `startTime` through `endTime + grace`.
- The scheduler automatically changes overdue `OPEN`, `FULL`, or `IN_PROGRESS` games to `COMPLETED`.
- There is no manual completion endpoint.
- There is no `/my-games` page or completed-game history listing.

The phase plan explicitly documents the missing history route:

- `F:\Game-connect\.opencode\plans\planning_phase7.md:265-280`
- Especially line 273: completed games are only reachable through a direct detail URL.

---

## 4. Profile and `/me` Rendering

### Auth `/me` is not rendered as a profile

`F:\Game-connect\frontend\lib\auth-context.tsx:23-36` calls `fetchMe()`, but the returned data is only stored in context.

There is no page that renders:

- bio
- profile image
- skill level
- position
- player statistics
- average rating

The current frontend auth types are:

- `F:\Game-connect\frontend\lib\api\auth.ts:3-13`

`AuthUser` contains only:

- `id`
- `email`
- `displayName`

`MeUser` adds:

- `bio`
- `profileImageUrl`
- `skillLevel`

It does not contain the backend’s `position` or `stats` fields.

### Backend profile response does not match frontend `UserProfile`

Backend response:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- Nested `PlayerStatsResponse` contains `averageRating`.

Frontend `UserProfile`:

- `F:\Game-connect\frontend\lib\types.ts:21-29`
- Contains `email`
- Does not contain `position`
- Does not contain the nested `stats` response shape.

There is no frontend type matching `UserProfileResponse` directly.

### “Public” profile behavior is currently authenticated-only

The product route list includes:

- `F:\Game-connect\PRODUCT.MD:248-259`
  - `/profile`
  - `/profile/[id]`

But no corresponding frontend routes exist.

The backend does have a user-profile-by-ID route:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\controller\ProfileController.java:36-39`

However, global security requires authentication for every request except registration, login, health, and actuator:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:39-44`

The integration test explicitly expects unauthenticated `GET /api/users/{id}` to return 401:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:90-94`

The frontend proxy also treats `/profile` as protected:

- `F:\Game-connect\frontend\proxy.ts:6-7`
- `F:\Game-connect\frontend\proxy.ts:24-29`

Therefore the current implementation does not provide anonymous public-profile rendering.

---

## 5. Completed Games and Attendance Display

### Game detail is the only completed-game surface

`F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:59-62` obtains the dynamic ID and renders the client detail component.

The detail page:

- Fetches any game by ID: lines 64-95.
- Handles loading, 404, and generic API error states: lines 105-146.
- Displays the game status through `GameStatusBadge`: lines 159-172.
- Displays capacity and remaining spots: lines 177-201.
- Displays details, date, and IST times: lines 229-268.
- Displays the host: lines 280-295.

The backend detail endpoint itself does not restrict the game status:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\GameController.java:61-65`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:194-239`

Thus a completed game can be loaded directly by UUID.

### Discovery excludes completed games

The list endpoint passes `OPEN` explicitly:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:169-176`

There is no completed-game list query, `/my-games` page, or frontend status filter for completed games.

### Player attendance display

For a non-owner participant, the detail page displays only the current user’s attendance:

- Condition: `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:220-227`
- Component: `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:32-57`

Messages include:

- attended
- marked absent
- not yet marked
- attendance closed after completion

The backend detail response contains only the viewer’s participation:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\GameDetailResponse.java:12-33`
- `GameService.java:213-216`

It does not contain a general participant list.

### Owner attendance display

The owner sees:

- `JoinRequestsPanel`
- `AttendancePanel`

at:

- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:203-210`

`AttendancePanel` already provides a useful implementation pattern for a future rating form:

- Local loading/error state: `AttendancePanel.tsx:37-61`
- Per-row in-flight state: `63-65`
- Async mutation and optimistic/local replacement: `67-84`
- Error-code-specific messages: `85-127`
- Empty/loading/error rendering: `129-151`
- Per-row buttons: `152-215`

However, the roster API is owner-only:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:46-52`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:135-142`

A regular player cannot reuse that endpoint to discover the other participants they may need to rate.

---

## 6. Rating Code Audit

### Existing rating-related scaffolding

There is some existing rating data, so a literal claim that the repository contains no rating code would be inaccurate.

#### Frontend type

- `F:\Game-connect\frontend\lib\types.ts:31-36`
  - `PlayerStats.averageRating: number | null`
- `F:\Game-connect\frontend\lib\types.ts:192-199`
  - `PlayerRating` interface with:
    - `id`
    - `gameId`
    - `raterId`
    - `rateeId`
    - `score`
    - `createdAt`

No frontend reference to `PlayerRating` was found outside its declaration.

#### Existing aggregate display

- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:214-229`
  - Displays applicant `matchesPlayed`
  - Displays applicant `averageRating` to one decimal place

This is the only current frontend rating display.

#### Database

- `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:85-99`
  - `player_rating` table
  - score constrained to 1–5
  - self-rating prohibited
  - one rating per rater/ratee/game

#### Aggregate query

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-50`
  - `SELECT AVG(pr.score) FROM player_rating pr WHERE pr.ratee_id = ?`

#### Profile/join-request responses

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-101`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`

### Missing rating implementation

No files or endpoints were found for:

- rating submission
- rating retrieval for a game
- current rater’s existing ratings
- rating eligibility checks
- rating controller
- rating service
- rating repository/entity layer
- frontend `lib/api/ratings.ts`
- rating form/component
- profile page aggregate-rating display
- completed-game rating surface

The only Java references to rating are the schema, aggregate query, and response population.

### Product rules

The product specifies:

- Ratings after a completed match: `F:\Game-connect\PRODUCT.MD:192-204`
- One-to-five score and average rating: `PRODUCT.MD:198-204`
- Users cannot rate games they did not participate in: `PRODUCT.MD:282-304`, especially line 300
- One rating per player per game: `PRODUCT.MD:302`

Those rules are not currently enforced by a rating write endpoint because no write endpoint exists.

---

## 7. Exact Integration Points for Rating Work

These are the existing source locations relevant to the requested future feature. They are integration observations, not an implementation proposal.

### Rating submission

The current game detail page is the only existing page with all of these values:

- game ID: `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:59-64`
- authenticated user: `games/[id]/page.tsx:64-65`
- game status: `games/[id]/page.tsx:166-172`
- viewer participation: `games/[id]/page.tsx:220-226`

The natural existing page-level anchor is the authenticated detail content around:

- `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:153-227`

There is currently no API target to call. A new client API module would need to follow the existing pattern in:

- `F:\Game-connect\frontend\lib\api\participants.ts:5-21`
- `F:\Game-connect\frontend\lib\api\join-requests.ts:5-35`

The missing backend prerequisite is more significant than the UI location: the client cannot construct a list of rateable peers from the current game detail response.

- `GameDetailResponse` contains only `myParticipation`: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\GameDetailResponse.java:12-33`
- The available participant list endpoint is owner-only: `AttendanceService.java:46-52`
- There is no rating eligibility endpoint or submission endpoint.

### Aggregate rating display

Existing aggregate data can already be returned by the backend in profile responses:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-101`

The existing frontend location to copy conceptually is:

- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:214-229`

But there is no profile page on which to render it. The planned `/profile` and `/profile/[id]` routes are absent from the App Router inventory, and the frontend `UserProfile` type does not match the backend response’s nested `stats` object.

### Completed-game navigation

A rating surface reached from a completed game would currently have to use:

```text
/games/[id]
```

because:

- discovery only lists upcoming `OPEN` games: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:169-176`
- no `/my-games` page exists
- no completed-game list API exists
- the phase plan documents direct-link-only access: `F:\Game-connect\.opencode\plans\planning_phase7.md:273`

---

## 8. Reusable State, Loading, and Error Patterns

There is no shared query library or global async state layer. Existing patterns are local and component-specific:

- Shared transport/error handling:
  - `F:\Game-connect\frontend\lib\api\client.ts:18-49`
- Cancellable page fetch:
  - `F:\Game-connect\frontend\app\(app)\games\page.tsx:28-60`
  - `F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:71-95`
- Auth initialization:
  - `F:\Game-connect\frontend\lib\auth-context.tsx:23-36`
- Discriminated request state:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:13-16`
  - `JoinRequestAction.tsx:58-78`
- Per-item mutation state:
  - `F:\Game-connect\frontend\components\games\AttendancePanel.tsx:37-65`
  - `JoinRequestsPanel.tsx:66-70`
- API-code-specific friendly errors:
  - `AttendancePanel.tsx:85-127`
  - `JoinRequestAction.tsx:100-142`
  - `JoinRequestsPanel.tsx:112-150`
- Form submission errors:
  - `F:\Game-connect\frontend\app\login\page.tsx:15-30`
  - `F:\Game-connect\frontend\app\register\page.tsx:16-31`
  - `F:\Game-connect\frontend\app\(app)\create-game\page.tsx:45-88`

The existing reusable visual/status components are:

- `F:\Game-connect\frontend\components\games\GameStatusBadge.tsx`
- `F:\Game-connect\frontend\components\games\GameCard.tsx`
- `F:\Game-connect\frontend\components\games\GameFilters.tsx`
- `F:\Game-connect\frontend\components\games\AttendancePanel.tsx`
- `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx`
- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx`
- `F:\Game-connect\frontend\components\layout\BottomNav.tsx`

---

## 9. Tests, Lint, and Build

### Frontend

No frontend test infrastructure was found:

- No `*.test.*` or `*.spec.*` files
- No `__tests__`, `tests`, or `e2e` directory
- No Jest, Vitest, Playwright, or Cypress configuration
- No test dependency or test script in `F:\Game-connect\frontend\package.json:5-24`

Available scripts:

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run lint`

No fresh lint or build result is claimed here; those commands were not run during this read-only investigation.

### Backend

The backend uses Spring Boot integration tests with JUnit, MockMvc, and Testcontainers:

- `F:\Game-connect\backend\build.gradle:33-42`
- `F:\Game-connect\backend\build.gradle:45-48`

Relevant test files include:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameDiscoveryIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java`
- `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java`

The schema test verifies the `player_rating` table and constraints:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java:35-75`

The profile test verifies unauthenticated access behavior:

- `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:40-94`

There are no rating-submission or rating-aggregate behavior tests.

---

## Final Assessment

- The frontend has a complete authenticated shell for the implemented Phase 1–7 flows, but its profile and match-history surfaces are still placeholders/nav links only.
- The current completed-game surface is the direct `/games/[id]` page.
- Attendance and participant data are owner-oriented; regular players only receive their own `myParticipation`.
- Existing aggregate-rating data is available in backend profile/applicant responses and is displayed only in the Phase 6 applicant panel.
- There is no Phase 8/repository-next-phase rating submission implementation.
- A frontend rating UI cannot currently be wired end-to-end because the repository has no rating endpoint, no eligible-peer response, and no completed-game history navigation.
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 218.4s)

_Thinking:_

Three good, applicant agent failed/refused. Need resume same task with explicit ask report, not implementation. Use task_id, tell no question needed and read-only. Also migration and backend overlap (okay agents duplicated slightly but we didn't). Need maybe get detailed exact files ourselves after agents? We can rely. Need inspect actual files further to synthesize. We need maybe use grep/glob/read targeted. Since applicant delegated but failed; resume.

**Tool: task**

**Input:**
```json
{
  "description": "Report applicant statistics",
  "prompt": "Do not ask which option to choose and do not implement anything. Complete the requested read-only source investigation and return the actual Phase 6 join-request applicant statistics report now. Include exact current backend and frontend DTO/API shapes, methods, queries, auth, rendering, test coverage, and precise file:line references. Then assess, based only on source, whether Phase 8 should add averageRating and ratingCount to shared PlayerStatsResponse and how ApplicantSummary should reuse it without duplicate rating queries. No edits.",
  "subagent_type": "explore",
  "task_id": "ses_f283f7587ffe5w3y77WAeW499M",
  "command": "Continue read-only Phase 6 inspection"
}
```

**Output:**
```
<task id="ses_f283f7587ffe5w3y77WAeW499M" state="completed">
<task_result>
# Phase 6 join-request applicant-statistics report

## Executive assessment

- Applicant statistics already flow through the shared backend DTO `PlayerStatsResponse` and the shared frontend type `PlayerStats`:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\ApplicantSummary.java:7-15`
  - `F:\Game-connect\frontend\lib\types.ts:31-36`
  - `F:\Game-connect\frontend\lib\types.ts:174-180`
- `averageRating` already exists in the shared DTO and is displayed in the owner join-request panel. Phase 8 should not add a second `averageRating` to `ApplicantSummary`.
- `ratingCount` does not currently exist.
- The cleanest Phase 8 contract is therefore to:
  1. Keep `averageRating` in `PlayerStatsResponse`.
  2. Add `ratingCount` to `PlayerStatsResponse`.
  3. Have `ApplicantSummary` continue to reuse its existing `stats: PlayerStatsResponse` field.
  4. Return average and count from one SQL aggregate.
  5. Make only one rating aggregate call while constructing each applicant’s `PlayerStatsResponse`.
- Adding rating fields directly to `ApplicantSummary` would cause the owner endpoint to query or expose the same data twice:
  ```json
  {
    "averageRating": 4.2,
    "ratingCount": 3,
    "stats": {
      "averageRating": 4.2,
      "ratingCount": 3
    }
  }
  ```
- Adding a separate `findAverageRating` and `findRatingCount` call would also duplicate rating-query work. For an owner list with N applicants, the current source performs approximately `2 + 3N` SQL statements; separate average/count queries would increase that to `2 + 4N`.

No files were edited.

---

# 1. Current backend join-request DTOs and API

## 1.1 Controller routes

All routes are under:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:25-27`

```java
@RequestMapping("/api/games/{gameId}/join-requests")
```

| Method | Route | Java method | Response |
|---|---|---|---|
| `POST` | `/api/games/{gameId}/join-requests` | `createJoinRequest` | `201 JoinRequestResponse` |
| `GET` | `/api/games/{gameId}/join-requests/me` | `getMyRequest` | `200 JoinRequestResponse` |
| `GET` | `/api/games/{gameId}/join-requests` | `getGameRequests` | `200 List<JoinRequestDetailResponse>` |
| `PATCH` | `/api/games/{gameId}/join-requests/{requestId}` | `decideRequest` | `200 JoinRequestResponse` |

Exact controller implementations:

- Create: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:35-41`
- Current caller’s request: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:43-48`
- Owner list: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:50-55`
- Decision: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:57-66`

## 1.2 Basic `JoinRequestResponse`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\JoinRequestResponse.java:8-15`

```java
public record JoinRequestResponse(
        UUID id,
        UUID gameId,
        UUID userId,
        RequestStatus status,
        Instant createdAt,
        Instant decidedAt
) {}
```

Current JSON shape:

```json
{
  "id": "3ec2d5f0-cb65-4d3f-a38e-3f7edb296e02",
  "gameId": "936f26f9-11dd-43b2-a7ec-23bfd871d3d6",
  "userId": "df04e19b-eaca-41b9-8892-a5e53acf4b21",
  "status": "PENDING",
  "createdAt": "2026-09-25T10:20:30Z",
  "decidedAt": null
}
```

After a decision:

```json
{
  "id": "3ec2d5f0-cb65-4d3f-a38e-3f7edb296e02",
  "gameId": "936f26f9-11dd-43b2-a7ec-23bfd871d3d6",
  "userId": "df04e19b-eaca-41b9-8892-a5e53acf4b21",
  "status": "ACCEPTED",
  "createdAt": "2026-09-25T10:20:30Z",
  "decidedAt": "2026-09-25T10:22:15Z"
}
```

Status values come from:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\entity\JoinRequest.java:18-20`

```java
PENDING, ACCEPTED, REJECTED, CANCELLED
```

The frontend mirrors the basic response at:

`F:\Game-connect\frontend\lib\types.ts:165-172`

```ts
export interface JoinRequest {
  id: string;
  gameId: string;
  userId: string;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
}
```

## 1.3 Owner-list `JoinRequestDetailResponse`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\JoinRequestDetailResponse.java:8-15`

```java
public record JoinRequestDetailResponse(
        UUID id,
        UUID gameId,
        RequestStatus status,
        Instant createdAt,
        Instant decidedAt,
        ApplicantSummary applicant
) {}
```

The frontend equivalent is:

`F:\Game-connect\frontend\lib\types.ts:183-190`

```ts
export interface JoinRequestDetail {
  id: string;
  gameId: string;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
  applicant: ApplicantSummary;
}
```

The owner-list item does not expose a top-level `userId`; that value is under `applicant.userId`.

## 1.4 Current `ApplicantSummary`

Backend:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\ApplicantSummary.java:9-16`

```java
public record ApplicantSummary(
        UUID userId,
        String displayName,
        String profileImageUrl,
        SkillLevel skillLevel,
        Position position,
        PlayerStatsResponse stats
) {}
```

Frontend:

`F:\Game-connect\frontend\lib\types.ts:174-181`

```ts
export interface ApplicantSummary {
  userId: string;
  displayName: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel | null;
  position: Position | null;
  stats: PlayerStats | null;
}
```

The backend defensively permits a missing user to result in null profile fields and null `stats`:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`

Although the database has a user foreign key, so this should not occur through normal data.

## 1.5 Current shared `PlayerStatsResponse`

Backend:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`

```java
public record PlayerStatsResponse(
        int matchesPlayed,
        int matchesCompleted,
        Double attendanceRate,
        Double averageRating
) {}
```

Frontend:

`F:\Game-connect\frontend\lib\types.ts:31-36`

```ts
export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
}
```

There is no `ratingCount` in either DTO.

The same backend `PlayerStatsResponse` is also embedded in the public profile response:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- Profile endpoint: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\controller\ProfileController.java:30-39`

Thus the contract is already shared by:

```text
GET /api/users/{id}
  -> stats: PlayerStatsResponse

GET /api/games/{gameId}/join-requests
  -> applicant.stats: PlayerStatsResponse
```

## 1.6 Current owner-list JSON

Representative current response:

```json
[
  {
    "id": "3ec2d5f0-cb65-4d3f-a38e-3f7edb296e02",
    "gameId": "936f26f9-11dd-43b2-a7ec-23bfd871d3d6",
    "status": "PENDING",
    "createdAt": "2026-09-25T10:20:30Z",
    "decidedAt": null,
    "applicant": {
      "userId": "df04e19b-eaca-41b9-8892-a5e53acf4b21",
      "displayName": "Player Name",
      "profileImageUrl": null,
      "skillLevel": "INTERMEDIATE",
      "position": "STRIKER",
      "stats": {
        "matchesPlayed": 4,
        "matchesCompleted": 4,
        "attendanceRate": 0.75,
        "averageRating": 4.25
      }
    }
  }
]
```

For an unrated applicant:

```json
"stats": {
  "matchesPlayed": 0,
  "matchesCompleted": 0,
  "attendanceRate": null,
  "averageRating": null
}
```

No repository source configures Jackson null exclusion: no `JsonInclude`, `default-property-inclusion`, or equivalent configuration was found. Under the repository’s default serialization configuration, nullable record properties are expected to be emitted as explicit JSON `null`. In contrast, the frontend correctly accepts nulls.

## 1.7 Decision body

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\DecisionRequest.java:5-11`

```json
{
  "action": "ACCEPT"
}
```

or:

```json
{
  "action": "REJECT"
}
```

- `action` is required.
- Unknown values produce `400 MALFORMED_BODY` through:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:46-50`
- Missing/null `action` produces `400 VALIDATION_ERROR`:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:30-43`

The frontend type is:

`F:\Game-connect\frontend\lib\types.ts:19`

```ts
export type JoinDecision = "ACCEPT" | "REJECT";
```

---

# 2. Current backend service methods

All join-request operations are in:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java`

## 2.1 `createJoinRequest`

Method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:52-112`

Checks occur in this order:

1. Authenticated user exists:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:54-58`
2. Game exists:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:60`
3. Requester is not the owner:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:62-67`
4. Game is `OPEN`:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:69-74`
5. Requester is not already a participant:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:76-81`
6. Requester has no pending request:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:83-88`
7. Participant count is below capacity:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:90-96`
8. Save a new `PENDING` request:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:98-105`

### Important request behavior

- Only `PENDING` is explicitly checked before insertion.
- The database has a unique `(game_id,user_id)` constraint:
  - `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:63-65`
- Therefore a previous `REJECTED` or `CANCELLED` request normally cannot be replaced. The save catch maps a data-integrity violation to `JOIN_REQUEST_EXISTS`:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:104-111`
- No endpoint creates `CANCELLED`.
- No request cancellation, reactivation, or transition back to `PENDING` exists.
- Request creation does not reserve a seat; multiple pending requests may be created while the game is open.
- `requiredPlayers` is not checked. Capacity uses `maximumPlayers`.

Potential error-handling edge:

- The `DataIntegrityViolationException` catch ends before transaction commit. If Hibernate reports the unique violation at commit rather than inside `save`, it may not be translated by this local catch and may reach the generic handler.

## 2.2 `getMyRequest`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:114-123`

- Calls `findGame(gameId)`.
- Looks up by both game ID and authenticated user ID.
- Returns any existing status, not only pending.
- Missing request returns `404 REQUEST_NOT_FOUND`.
- The user cannot select another user’s request because no user ID is accepted as a request parameter.

Repository method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\JoinRequestRepository.java:19`

```java
findByGameIdAndUserId(UUID gameId, UUID userId)
```

## 2.3 `getGameRequests`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:125-132`

- Loads the game.
- Verifies ownership.
- Returns all requests, not only pending.
- Sorts oldest request first.
- Has no status filter, pagination, or limit.

Repository method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\JoinRequestRepository.java:23`

```java
findByGameIdOrderByCreatedAtAsc(UUID gameId)
```

The method is name-based JPA querying, so there is no explicit owner-list SQL in `JoinRequestRepository.java`.

Relevant indexes:

- `(game_id,status)`:
  - `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:68`
- `(user_id,status)`:
  - `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:69`

There is no `(game_id,created_at)` index, and ordering only by `createdAt` has no secondary tie-breaker.

## 2.4 `decideRequest`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:134-173`

The method:

1. Acquires a pessimistic write lock on the game:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:137-141`
2. Verifies owner:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:143`
3. Loads the request:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:145-149`
4. Verifies the request belongs to the route’s game:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:151-156`
5. Requires status `PENDING`:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:158-163`
6. For `REJECT`, changes the request status and timestamp:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:165-170`
7. For `ACCEPT`, delegates to `acceptUnderLock`:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:172`

Game lock query:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\GameRepository.java:26-31`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT g FROM Game g
        WHERE g.id = :id
        """)
Optional<Game> findByIdForUpdate(@Param("id") UUID id);
```

A request-level pessimistic-lock method exists but is unused:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\JoinRequestRepository.java:25-27`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select jr from JoinRequest jr where jr.id = :id")
Optional<JoinRequest> findByIdForUpdate(@Param("id") UUID id);
```

## 2.5 `acceptUnderLock`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:175-213`

Acceptance:

1. Requires game status `OPEN`: lines `176-181`.
2. Requires applicant not already be a participant: lines `183-188`.
3. Counts current participants under the game lock: lines `190-196`.
4. Creates `MatchParticipant` with:
   - applicant user ID
   - role `PLAYER`
   - `attended = null`
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:198-203`
5. Sets request to `ACCEPTED` and `decidedAt`:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:205-206`
6. Sets game to `FULL` when the new count equals capacity:
   - `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:208-210`

The whole decision method is transactional, so participant creation and request acceptance commit or roll back together.

Participant count method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\MatchParticipantRepository.java:16`

```java
long countByGameId(UUID gameId);
```

Participant existence method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\MatchParticipantRepository.java:18`

```java
boolean existsByGameIdAndUserId(UUID gameId, UUID userId);
```

The game-row lock serializes decisions made through this service for the same game.

---

# 3. Persistence schema relevant to applicants and ratings

## 3.1 Join-request schema

`F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:55-69`

```sql
CREATE TABLE join_request (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id    UUID NOT NULL REFERENCES game (id),
    user_id    UUID NOT NULL REFERENCES app_user (id),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    CONSTRAINT ck_req_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT uq_req_game_user UNIQUE (game_id, user_id)
);
```

The entity uses application-generated UUID and `createdAt` values in `@PrePersist`:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\entity\JoinRequest.java:22-49`

## 3.2 Rating schema

`F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:85-99`

Current constraints:

- Score is `1..5`: lines `90`, `93`.
- Rater cannot rate self: line `94`.
- One rating per rater/ratee/game: lines `95-96`.
- Index on `ratee_id`: line `99`.

There is no backend rating entity, rating repository, rating service, or rating controller in current production source.

The frontend contains an unused `PlayerRating` type:

`F:\Game-connect\frontend\lib\types.ts:192-199`

but no API function uses it.

The schema does not itself require that:

- the game is `COMPLETED`,
- the rater was a participant,
- the ratee was a participant,
- attendance was marked.

Any such Phase 8 write-side validation would have to come from future service logic or additional database constraints.

---

# 4. Current applicant-statistics queries

All statistics SQL is in:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java`

## 4.1 Participation statistics query

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:22-43`

```sql
SELECT
    COUNT(DISTINCT CASE
        WHEN g.status = 'COMPLETED' AND mp.attended = TRUE
        THEN mp.game_id
    END) AS matches_played,
    COUNT(DISTINCT CASE
        WHEN g.status = 'COMPLETED' AND mp.attended = TRUE
        THEN mp.game_id
    END) AS matches_completed,
    CASE
        WHEN COUNT(DISTINCT CASE
            WHEN g.status = 'COMPLETED'
            THEN mp.game_id
        END) = 0 THEN NULL
        ELSE CAST(
            COUNT(DISTINCT CASE
                WHEN g.status = 'COMPLETED' AND mp.attended = TRUE
                THEN mp.game_id
            END)
            AS DOUBLE PRECISION
        ) / COUNT(DISTINCT CASE
            WHEN g.status = 'COMPLETED'
            THEN mp.game_id
        END)
    END AS attendance_rate
FROM match_participant mp
JOIN game g ON g.id = mp.game_id
WHERE mp.user_id = ?
```

Current semantics:

- `matchesPlayed`:
  - distinct completed games
  - `attended = TRUE`
- `matchesCompleted`:
  - exact same expression as `matchesPlayed`
- `attendanceRate`:
  - numerator: completed games attended
  - denominator: all completed games in which the user has a participant row
  - `NULL` if denominator is zero
  - `false` and `null` attendance are both treated as non-attendance
  - no rounding

Therefore:

```text
matchesPlayed == matchesCompleted
```

for every current result, and `matchesCompleted` is not a total count of completed participations.

## 4.2 Current average-rating query

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-50`

```sql
SELECT AVG(pr.score)
FROM player_rating pr
WHERE pr.ratee_id = ?
```

Current semantics:

- Average of all rating rows received by the user.
- SQL `AVG` returns `NULL` when there are no ratings.
- No count.
- No rounding.
- No game-status filter.
- No query-time participant validation.
- Supported by `idx_player_rating_ratee`.

The repository Javadoc says only completed games contribute to match statistics:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:8-12`

but the rating SQL itself does not enforce a completed-game predicate. Any Phase 8 implementation should use the same predicate for both average and count so the two values cannot disagree.

## 4.3 Profile statistics reuse

`ProfileService` calls both statistics queries:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-90`

```java
PlayerStatsRepository.ParticipationStats participation =
        playerStatsRepository.findParticipationStats(userId);
Double averageRating = playerStatsRepository.findAverageRating(userId);
return new PlayerStatsResponse(
        participation.matchesPlayed(),
        participation.matchesCompleted(),
        participation.attendanceRate(),
        averageRating);
```

## 4.4 Applicant statistics reuse

For each request, `toDetail` calls `toApplicantSummary`:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:242-250`

The applicant is assembled at:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`

```java
User user = userRepository.findById(userId).orElse(null);
PlayerStatsResponse stats = null;
if (user != null) {
    PlayerStatsRepository.ParticipationStats participation =
            playerStatsRepository.findParticipationStats(userId);
    Double averageRating = playerStatsRepository.findAverageRating(userId);
    stats = new PlayerStatsResponse(
            participation.matchesPlayed(),
            participation.matchesCompleted(),
            participation.attendanceRate(),
            averageRating);
}
return new ApplicantSummary(
        userId,
        user != null ? user.getDisplayName() : null,
        user != null ? user.getProfileImageUrl() : null,
        user != null ? user.getSkillLevel() : null,
        user != null ? user.getPosition() : null,
        stats);
```

This is the exact point at which a combined `averageRating`/`ratingCount` result should be converted into the shared `PlayerStatsResponse`.

---

# 5. Current query count and N+1 behavior

For an owner list containing N join requests, the current code performs approximately:

1. One game lookup.
2. One join-request list query.
3. Per request/applicant:
   - one `UserRepository.findById`
   - one participation-statistics query
   - one average-rating query

Total:

```text
2 + 3N SQL statements
```

Source path:

- List loop: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:125-131`
- User lookup: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:253`
- Participation query: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:256-257`
- Rating query: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:258`

The database’s unique `(game_id,user_id)` request constraint normally prevents the same applicant from appearing multiple times in one game’s request list, so JPA persistence-context reuse does not remove the per-user queries.

The list is not bounded by game capacity because it returns historical pending, accepted, and rejected requests without pagination:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\repository\JoinRequestRepository.java:23`

A combined average/count query would keep the current count at `2 + 3N`. Separate average/count queries would raise it to `2 + 4N`.

A scalable later optimization would batch:

- users,
- participation aggregates,
- rating aggregates for all applicant IDs,

but batching is separate from the Phase 8 requirement to avoid duplicate rating queries.

---

# 6. Authentication and authorization

## 6.1 Global authentication

Security configuration:

`F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:33-45`

- All `/api/**` routes require authentication except:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/health`
  - actuator
- Session management is stateless:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:37-38`
- CSRF is disabled:
  - `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:35-36`

Unauthenticated join-request requests receive:

```json
{
  "timestamp": "ISO-8601 timestamp",
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "path": "/api/games/{gameId}/join-requests"
}
```

Response construction:

`F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:46-56`

## 6.2 JWT source and principal

The filter reads only the `auth_token` cookie:

`F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java:74-85`

It creates:

`F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java:87-88`

```java
public record AuthenticatedUser(UUID id, String email) {}
```

The controller binds this as `@AuthenticationPrincipal`; for example:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:36-39`

The requester identity is not accepted from the request body.

## 6.3 Cookie behavior

Auth cookie construction:

`F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:108-115`

```java
ResponseCookie.from(COOKIE_NAME, token)
        .path("/")
        .maxAge(cookieMaxAge)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .build();
```

Cookie secure mode defaults to false unless configured:

`F:\Game-connect\backend\src\main\resources\application.yml:26-27`

## 6.4 Owner authorization

Owner list:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:125-128`

Owner decision:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:137-143`

Shared ownership check:

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:223-229`

```java
if (!game.getOwnerId().equals(principal.id())) {
    throw new BusinessException(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "Only the game owner can perform this action");
}
```

Therefore:

- `/me` is scoped by the authenticated principal.
- Owner listing and decisions are server-side owner-only.
- A non-owner receives `403`.
- A request ID from another game is reported as `404 REQUEST_NOT_FOUND`, avoiding cross-game request identification.
- Frontend owner checks are only a rendering convenience; they are not the security boundary.

## 6.5 Error contract

`F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\ApiError.java:5-15`

```java
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {}
```

Example:

```json
{
  "timestamp": "2026-09-25T10:20:30Z",
  "status": 409,
  "code": "GAME_FULL",
  "message": "This game is already full",
  "path": "/api/games/{gameId}/join-requests"
}
```

---

# 7. Current frontend API and DTO integration

## 7.1 API methods

`F:\Game-connect\frontend\lib\api\join-requests.ts:5-35`

```ts
createJoinRequest(gameId)
getMyJoinRequest(gameId)
getGameJoinRequests(gameId)
decideJoinRequest(gameId, requestId, decision)
```

Current routes:

| Frontend method | Request |
|---|---|
| `createJoinRequest` | `POST /api/games/{gameId}/join-requests` |
| `getMyJoinRequest` | `GET /api/games/{gameId}/join-requests/me` |
| `getGameJoinRequests` | `GET /api/games/{gameId}/join-requests` |
| `decideJoinRequest` | `PATCH /api/games/{gameId}/join-requests/{requestId}` |

Decision body is built at:

`F:\Game-connect\frontend\lib\api\join-requests.ts:26-34`

```ts
{ method: "PATCH", body: { action: decision } }
```

## 7.2 Fetch/auth behavior

Shared client:

`F:\Game-connect\frontend\lib\api\client.ts:18-50`

- Base path is `/api`: line `1`.
- Cookies are always sent:
  - `credentials: "include"` at line `23`.
- JSON is set only when a body exists: lines `24-28`.
- Error `code` and `message` are extracted into `ApiError`: lines `31-42`.
- Successful JSON is returned through a compile-time cast:
  - `F:\Game-connect\frontend\lib\api\client.ts:45-49`

There is no runtime response validator. Adding an additive `ratingCount` property would not require an API-wrapper change, but the shared TypeScript type and renderer would need updating.

## 7.3 Player state and rendering

Component:

`F:\Game-connect\frontend\components\games\JoinRequestAction.tsx`

State machine:

`F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:13-16`

```ts
type RequestView =
  | { kind: "loading" }
  | { kind: "none" }
  | { kind: "request"; request: JoinRequest };
```

- Initial `/me` request:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:63-78`
- Create request:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:93-142`
- Manual request-status refresh:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:80-91`
- Existing-request status display:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:175-199`
- Join button only appears for an open game with capacity:
  - `F:\Game-connect\frontend\components\games\JoinRequestAction.tsx:201-219`

## 7.4 Owner state and applicant rendering

Component:

`F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx`

State and initial load:

`F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:66-88`

Decision handling:

`F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:90-152`

Current applicant rendering:

| Field | Rendering | Source |
|---|---|---|
| Profile image or initials | Rendered | `JoinRequestsPanel.tsx:182-194` |
| Display name | Rendered | `JoinRequestsPanel.tsx:195-199` |
| Request status | Rendered | `JoinRequestsPanel.tsx:200-204` |
| Skill level | Rendered | `JoinRequestsPanel.tsx:206-209` |
| Position | Rendered when non-null | `JoinRequestsPanel.tsx:210-212` |
| `matchesPlayed` | Rendered | `JoinRequestsPanel.tsx:214-221` |
| `averageRating` | Rendered to one decimal place | `JoinRequestsPanel.tsx:223-229` |
| Request time | Rendered through `formatIST` | `JoinRequestsPanel.tsx:232-234` |
| Accept/Reject | Rendered only for pending requests | `JoinRequestsPanel.tsx:238-263` |

The current rating rendering is exactly:

`F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:223-229`

```tsx
<dd>
  {request.applicant.stats?.averageRating != null
    ? `Rating: ${request.applicant.stats.averageRating.toFixed(1)}`
    : "Rating: —"}
</dd>
```

Not rendered:

- `matchesCompleted`
- `attendanceRate`
- `ratingCount`
- applicant bio

The component loads statistics once when mounted. It does not poll. If a completed-game or rating update occurs while the page remains open, the displayed statistics remain stale until the component is remounted or an error path calls `refresh()`.

## 7.5 Page integration and owner gating

Auth identity:

`F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:64-66`

Owner/non-owner branch:

`F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx:203-219`

```tsx
{user != null && user.id === game.owner.id ? (
  <JoinRequestsPanel gameId={id} onRequestDecided={refreshGame} />
) : (
  <JoinRequestAction
    gameId={id}
    isAuthenticated={user != null}
    isOwner={user != null && user.id === game.owner.id}
    status={game.status}
    spotsRemaining={game.spotsRemaining}
  />
)}
```

`useAuth` obtains the current user from the auth context:

`F:\Game-connect\frontend\lib\auth-context.tsx:63-67`

This only determines which component is shown. The backend service remains responsible for actual authorization.

---

# 8. Current test coverage

## 8.1 No dedicated Phase 6 join-request test

No dedicated join-request integration test exists in the current backend test tree.

No frontend `*.test.*` or `*.spec.*` files exist, and there is no test script:

`F:\Game-connect\frontend\package.json:5-10`

## 8.2 Indirect join-request integration coverage

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java`

The test uses join requests as setup for roster/attendance scenarios.

### Helpers

Create request helper:

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:99-103`

It asserts only `201`.

Decision helper:

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:105-112`

It asserts only `200`.

Owner-list helper:

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:211-220`

It:

- calls the owner join-request endpoint,
- asserts `200`,
- deserializes `JoinRequestDetailResponse[]`.

This indirectly executes user lookup, participation-stat SQL, and average-rating SQL for unrated applicants.

Its `expectedCount` parameter is not used to assert list size:

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:211-220`

### Roster scenario

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:181-209`

It:

- creates three requests,
- accepts two,
- leaves one pending,
- verifies the participant roster contains owner plus two accepted players,
- verifies the pending applicant is absent from the roster.

It does not assert the request-status response fields, applicant JSON fields, or statistics values.

### Game-detail participation scenario

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\AttendanceIntegrationTest.java:432-450`

It creates and accepts a request, then verifies `myParticipation`.

## 8.3 Statistics coverage

### Zero-stat user

`F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:47-60`

It checks:

- `matchesPlayed = 0`
- `matchesCompleted = 0`
- no non-null attendance rate
- no non-null average rating

The `.doesNotExist()` assertions do not assert a non-null value and do not independently establish whether the physical JSON contains an explicit null.

### Attendance-based statistics

`F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java:242-325`

Coverage includes:

- attended completed game,
- explicit absence,
- unmarked attendance,
- non-completed game,
- owner participant.

The tests do not insert any `player_rating` row.

## 8.4 Schema coverage

`F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java:35-75`

It confirms:

- `join_request` and `player_rating` tables exist: lines `35-42`.
- `uq_req_game_user` exists: line `69`.
- `uq_rating`, rating score, and no-self-rating constraints exist: lines `71-74`.

It does not explicitly assert:

- `ck_req_status`,
- request indexes,
- rating count behavior,
- average-rating query behavior.

## 8.5 Uncovered join-request behavior

Current tests do not cover:

- unauthenticated create/list/me/decision,
- non-owner owner-list authorization,
- non-owner decision authorization,
- create against one’s own game,
- duplicate pending request,
- previously rejected request,
- already-participant request,
- full-game request,
- non-`OPEN` request,
- `/me` success or not-found,
- reject transition,
- already-decided transition,
- final acceptance changing game to `FULL`,
- request/game ID mismatch,
- invalid decision body,
- owner-list response shape,
- non-zero applicant statistics,
- non-null average rating,
- rating count,
- frontend rendering.

---

# 9. Phase 8 assessment: shared `PlayerStatsResponse`

## 9.1 Should the rating aggregate be in `PlayerStatsResponse`?

Yes.

Source supports making it the canonical location because:

1. It is already a shared DTO.
2. It is already embedded in both profile and applicant responses.
3. Both production assembly paths already use it.
4. The frontend already models it as shared `PlayerStats`.
5. The owner UI already reads its `averageRating`.

References:

- Shared DTO: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`
- Profile embedding: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- Applicant embedding: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\dto\ApplicantSummary.java:9-16`
- Profile construction: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-90`
- Applicant construction: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:252-271`
- Shared frontend type: `F:\Game-connect\frontend\lib\types.ts:31-36`
- Rendering: `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:223-229`

`averageRating` is already present. Phase 8 should not add it a second time. The missing shared field is `ratingCount`.

Conceptual target:

```java
public record PlayerStatsResponse(
        int matchesPlayed,
        int matchesCompleted,
        Double attendanceRate,
        Double averageRating,
        long ratingCount
) {}
```

Conceptual TypeScript target:

```ts
export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
  ratingCount: number;
}
```

## 9.2 How `ApplicantSummary` should reuse it

`ApplicantSummary` should keep its current shape:

```java
public record ApplicantSummary(
        UUID userId,
        String displayName,
        String profileImageUrl,
        SkillLevel skillLevel,
        Position position,
        PlayerStatsResponse stats
) {}
```

It should not gain:

```java
Double averageRating
Long ratingCount
```

The target owner-list response should remain nested:

```json
{
  "applicant": {
    "userId": "df04e19b-eaca-41b9-8892-a5e53acf4b21",
    "displayName": "Player Name",
    "profileImageUrl": null,
    "skillLevel": "INTERMEDIATE",
    "position": "STRIKER",
    "stats": {
      "matchesPlayed": 4,
      "matchesCompleted": 4,
      "attendanceRate": 0.75,
      "averageRating": 4.25,
      "ratingCount": 3
    }
  }
}
```

This avoids:

- duplicated JSON properties,
- conflicting average/count values,
- a second frontend source of truth,
- additional applicant-specific rating code.

## 9.3 How to avoid duplicate rating queries

The current `findAverageRating` method should conceptually become one rating aggregate returning both values.

Current method:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-50`

Conceptual combined query:

```sql
SELECT
    AVG(pr.score) AS average_rating,
    COUNT(*) AS rating_count
FROM player_rating pr
WHERE pr.ratee_id = ?
```

Conceptual repository result:

```java
public record RatingStats(
        Double averageRating,
        long ratingCount
) {}
```

Each `PlayerStatsResponse` construction should call that aggregate exactly once.

The two current call sites are:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:81-90`
- `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:256-263`

For owner-list applicants, the expected single rating-query path is:

```text
JoinRequest
  -> toDetail
  -> toApplicantSummary
  -> findParticipationStats once
  -> findRatingStats once
  -> new PlayerStatsResponse
  -> new ApplicantSummary(..., stats)
```

There should be no separate rating query inside:

- `ApplicantSummary`,
- `JoinRequestDetailResponse`,
- a dedicated applicant-rating service invoked from the same assembly,
- a frontend `/api/users/{id}` call per applicant.

## 9.4 Zero-rating semantics

To retain the current `AVG` behavior and add useful count semantics:

- No ratings:
  ```json
  {
    "averageRating": null,
    "ratingCount": 0
  }
  ```
- One or more ratings:
  ```json
  {
    "averageRating": 4.25,
    "ratingCount": 3
  }
  ```

Average and count must be read in the same SQL statement. Separate statements could observe different concurrent rating inserts and produce temporarily inconsistent values.

## 9.5 Source consistency issue for Phase 8

The existing average query has no completed-game condition:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\repository\PlayerStatsRepository.java:45-49`

The schema also has no constraint tying a rating to a completed game or participant row:

`F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql:85-97`

There is currently no rating write service against which eligibility can be assessed.

Therefore Phase 8 must choose and consistently apply one rating policy:

- If only completed-game ratings count, average and count should use the identical completed-game predicate.
- If the existing all-rating-row behavior is retained, both should continue to count every stored rating.
- Average and count should never use different eligibility predicates.

## 9.6 Frontend impact of the recommended contract

Backend:

- `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\PlayerStatsResponse.java:3-8`

Shared frontend type:

- `F:\Game-connect\frontend\lib\types.ts:31-36`

Owner rendering:

- `F:\Game-connect\frontend\components\games\JoinRequestsPanel.tsx:223-229`

No join-request API-wrapper route or request body would change.

Because the frontend performs only a TypeScript cast rather than runtime validation:

- `F:\Game-connect\frontend\lib\api\client.ts:45-49`

an additive field is structurally low-risk. The frontend renderer could then use the same nested value already accessed for `averageRating`.

## Final determination

The source supports one clear design:

```text
PlayerStatsResponse
  ├── matchesPlayed
  ├── matchesCompleted
  ├── attendanceRate
  ├── averageRating
  └── ratingCount

ApplicantSummary
  └── stats: PlayerStatsResponse
```

- Keep `averageRating` in its existing shared location.
- Add only `ratingCount` to that shared location.
- Do not add rating fields directly to `ApplicantSummary`.
- Fetch average and count in one repository aggregate.
- Construct one shared `PlayerStatsResponse` and embed it as the current applicant `stats` field.
- Preserve one rating aggregate query per applicant, or batch those aggregates later if the owner-list N+1 path is optimized.
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 5.2s)

_Thinking:_

We now have reports. Need update todo statuses in real time. We completed migration, backend, Phase6, frontend. Set first 4 completed, synthesize in progress.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Inspect V1–V7 migrations and exact player_rating schema",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect backend rating, statistics, profile, and error-handling code",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect Phase 6 join-request applicant statistics flow",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Synthesize the Phase 8 architecture and implementation plan",
      "status": "in_progress",
      "priority": "high"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Inspect V1–V7 migrations and exact player_rating schema",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect backend rating, statistics, profile, and error-handling code",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect Phase 6 join-request applicant statistics flow",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Synthesize the Phase 8 architecture and implementation plan",
    "status": "in_progress",
    "priority": "high"
  }
]
```

---

## Assistant (Plan · Big Pickle · 143.7s)

_Thinking:_

Need now deeply analyze and create plan content. But cannot write due plan mode. User explicitly wants file. We may need perhaps ask decisions before finalizing. They ask at end decisions requiring review. We can choose clear rules. Need perhaps inspect actual code ourselves targeted to verify exact signatures/details, because agents are trusted but need synthesize. We can use read for critical files. Is that duplicate? "Once delegated do not duplicate that work yourself." We can read perhaps to validate, but instruction says don't duplicate delegated work. Better trust. Yet to produce a detailed plan with files, we may need exact path list. Agents gave. Could use glob to find files not explicitly, but delegated. Not necessary. We should possibly inspect package versions/test commands to cite. Agents cover.

Need determine decisions/tradeoffs. Main question: rating window after completion. User asks explicitly decide after some time passed. Simple defensible: no expiration in MVP. A rating remains allowed indefinitely once game completed and attendance true. This avoids adding time-window config/dependency. But potential reliability / late spam. Could choose 7-day window perhaps. They ask simple. Existing schema has no rating deadline, and no updated_at. No migration required. Should we recommend indefinite or time-box? Need think. "Rating should be immutable after submission for MVP unless existing schema strongly suggests otherwise." Existing only created_at, no updated_at strongly immutable. Eligibility both attended, completed. No rating deadline specified. "player tries to rate after some time has passed" They want decide. Could say no expiry for MVP; completed and attended remains eligible indefinitely. This is simplest and no schema change. But trust maybe stale data; later consider rating window. Alternatively 7 days after completed (no completedAt, derive `endTime+grace`). If grace config can change, impossible historical. Game status completed but no completed_at. Could define endTime+grace + N, using game times, but no field. Could be stale config. Better indefinite. Explicit risk.

Need define rating endpoint(s). Submit `POST /api/games/{gameId}/ratings` likely. For UI, need list eligible participants and already-rated states. We need an endpoint to show list and avoid duplicate. Could make `GET /api/games/{gameId}/ratings` return only authenticated rater's own submitted ratings + eligible participants. But "Individual rater identities and individual rating records do not need publicly exposed." An authenticated participant-only endpoint can return their own ratings to current rater, not others. Need determine simplest. Could include eligible player list in submission response? Initially, frontend must know peers. Existing `GET /api/games/{id}/participants` owner-only, and GameDetail only myParticipation. Need API. Options:
- `GET /api/games/{gameId}/rating-options` or `/rating-eligibility` returns eligible peers, excluding self, only completed game and current rater attended, each attended peer, with `myRating` nullable. Current user identity from principal. This avoids exposing ratings/rater IDs. User suggested `GET /api/games/{gameId}/ratings` potentially, avoid unnecessary endpoints. Could design GET on same resource to return current user's rating status for completed game. But REST semantics: GET /games/{id}/ratings could return list of current user's own ratings in that game. It returns recipient IDs/scores (only own) and perhaps `RateableParticipantResponse` includes user details/attendance? Need UI needs names. Could response combine eligible participants and own rating:
```
{
 gameId,
 canRate: true,
 participants: [
   { user: {id, displayName, profileImageUrl}, rating: 5|null }
 ]
}
```
But calling this "ratings" may be okay. Or endpoint `GET /api/games/{gameId}/ratings/me` for current user's rating records, plus `GET participants` broadened. We should minimize endpoints. One GET and one POST. Need design auth/authz and errors.

Submission only could respond `201 PlayerRatingResponse` with id/gameId/ratedPlayerId/score/createdAt. GET could return `GameRatingsResponse`:
```
{
 gameId,
 participants: [
  {
   userId, displayName, profileImageUrl,
   myRating: 1..5|null
  }
 ]
}
```
Maybe include `canRate`, `reason` if ineligible? If user is not eligible, should GET return 403/409? For UI can hide panel. User asked API error codes. We need decide:
- GET eligibility endpoint accessible to authenticated users? If game exists, return 200 with `canRate: false` and reason perhaps `GAME_NOT_COMPLETED` / `ATTENDANCE_NOT_CONFIRMED`, participants empty. But exposing reason isn't personal. Yet could simplify. However API design should define exact error codes. For GET, could enforce same eligibility and return controlled business error. Frontend only calls when game completed and current user attended from existing GameDetail `myParticipation`; backend authoritative. But `GameDetail` has game status and my participation, enough to hide panel. For attendees, call GET. If backend rejects, friendly error. Could use same business codes.

Maybe use `GET /api/games/{gameId}/ratings` as:
- requires auth and game.
- 200 if current user is eligible, with `rateablePlayers` excluding self and all attended; include `ratedByMe: boolean` (or `myRating` score).
- 403 `RATING_NOT_ELIGIBLE` if current user did not participate/did not attend; 409 `GAME_NOT_COMPLETED`.
- Non-owner and non-participants cannot use. This is appropriate. It discloses only eligible other participants' public-ish display info to a verified co-attendant. No ratings from others, no rater IDs.
- It might be unnecessary if POST only and GameDetail includes roster, but current detail doesn't and owner-only endpoint. UI needs it. So include.
- Could call path `/api/games/{gameId}/ratings/me`? User proposed `/ratings`; API `GET /ratings` can be scoped to principal without confusing. Need explain semantics: current authenticated user's rating state for that game. But response includes rateable players. Name `GameRatingOptionsResponse` perhaps. Route maybe `GET /api/games/{gameId}/rating-options` clearer but outside potential. Follow GameConnect existing style. I'd choose `GET /api/games/{gameId}/ratings` to minimize endpoints and use nested resource. Document it is not public list of all ratings.

Could alternatively avoid GET by have GameDetail include eligible participants and current user's rating values only when eligible. But that couples game response and potentially makes standard game details vary, and no rating repository query. Not clean. Separate GET is justified.

Need exact eligibility:
- both participants in same game, attendance = TRUE.
- game status COMPLETED.
- no self.
- 1..5.
- one per game/rater/rated.
- immutable.
- nonexistent player: How distinguish rated player not exists vs exists but not participant? User asks nonexistent player. Avoid user enumeration: service can return `PLAYER_NOT_FOUND` 404 if UserRepository absent, `NOT_PARTICIPANT` maybe 403/404. But ratings are authenticated, ratee UUID supplied. If user exists but not participant, should error `PLAYER_NOT_PARTICIPANT` (403? 404? 409?). Need precise statuses. Existing conventions: 404 missing game, 404 participant, 403 authorization. Participation eligibility is not authorization to rate? Could use 403 `RATING_NOT_ELIGIBLE` for rater, 404 `PARTICIPANT_NOT_FOUND` for target. The target did not participate vs attended both perhaps 403 `PLAYER_NOT_ELIGIBLE`. User asks "nonexistent player" 404. To avoid info leak, profile endpoint already authenticated and users likely visible. But precise.

Let's inventory existing error codes patterns from agents: game not found 404 `GAME_NOT_FOUND`; participant not found 404; forbidden 403; game completed 409? Attendance uses 409 `GAME_ALREADY_COMPLETED`. Join request uses `GAME_NOT_OPEN` 409, duplicates 409. We can define:
- unauthenticated 401 UNAUTHORIZED.
- game missing 404 GAME_NOT_FOUND.
- self 400? Database check; domain boundary. User asks invalid/self appropriate. Could choose 400 `SELF_RATING_NOT_ALLOWED` or 409? Validation/business. Existing request validation returns `VALIDATION_ERROR` 400. Since `ratedPlayerId` can equal principal but cross-field business; use 400 `SELF_RATING_NOT_ALLOWED`. But DTO validation can't compare principal; service BusinessException. Score out of range via @Min/@Max -> 400 `VALIDATION_ERROR`. Good.
- game not completed 409 `GAME_NOT_COMPLETED` (state conflict). Could use `GAME_NOT_COMPLETED` not existing. Attendance uses `GAME_NOT_ACTIVE` for wrong state. For rating, new code okay.
- authenticated user did not participate 403 `RATING_NOT_ELIGIBLE`? Could break into `RATER_NOT_PARTICIPANT` (403). They ask precise. Better use specific codes for UI, security:
  - `RATER_NOT_PARTICIPANT` 403
  - `RATING_ATTENDANCE_REQUIRED` 403 for rater not true (covers false/null perhaps message)
  - `RATED_PLAYER_NOT_PARTICIPANT` 404? The resource relationship not found. Could use 404 `RATED_PLAYER_NOT_PARTICIPANT`; but user expects nonexistent player 404. Yet reveals.
  - `RATED_PLAYER_ATTENDANCE_REQUIRED` 403/409.
  - `SELF_RATING_NOT_ALLOWED` 400
  - `RATING_ALREADY_EXISTS` 409
  - `DATA_INTEGRITY_CONFLICT` fallback for concurrency. Ideally DataIntegrityViolation catch specifically constraint uq_rating. Need transaction design and exception mapping.

Concurrency tricky: `@Transactional` service calls `saveAndFlush` to force unique violation inside try/catch. If both check then save, first wins, second gets DataIntegrityViolationException. But catching within same transaction can mark transaction rollback-only. If catch and throw BusinessException, okay because exception escapes; but mapping after rollback. However distinguishing duplicate from other constraint. We check pre-existing and catch `DataIntegrityViolationException`, translate any to `RATING_ALREADY_EXISTS` maybe DB only possible checks already app validated. Concurrent write self/score constraints could be due race or direct bug, but app validated. Good. We can use repository `existsByGameIdAndRaterIdAndRateeId` precheck, then saveAndFlush. Unique final protection. If commit-time exception, generic handler could be added for DataIntegrityViolationException mapping 409 `RATING_ALREADY_EXISTS`. But globally mapping all DB integrity errors to rating duplicate is wrong. Better use service saveAndFlush and catch unique. But if Hibernate flush throws `ConstraintViolationException`, wrapped as DataIntegrityViolationException. It marks transaction rollback; translate to BusinessException, rollback. This should work. Or avoid pre-check? Insert and catch unique, but user requested check. Do both. Could use `INSERT ... ON CONFLICT DO NOTHING RETURNING` via JdbcTemplate for atomic controlled outcome, but architecture JPA and user wants check plus unique. Better JPA saveAndFlush.

No pessimistic locking. Need transaction isolation default read committed. `saveAndFlush` ensures DB uniqueness. If duplicate existing precheck returns 409. Race: both precheck false, one flush success, second waits then gets unique violation. Catch and throw 409. Caveat catch DataIntegrityViolation in same transaction. Fine. Could use transaction template separate? Overkill. We can mention test concurrent integration with ExecutorService/latches and real Postgres. But testing HTTP simultaneous requests requires auth cookies and MockMvc thread safety, possible. Could also test service with two threads. We can plan.

Need entity mapping:
- Package likely `rating`? Existing modular monolith packages. `com.gameconnect.rating`. Entity `PlayerRating`.
- fields id UUID, gameId UUID? Existing entities often use scalar IDs rather than relations. Need inspect agent says entities with UUID. We can follow. Need know constructors/builders style. Reports mention entities manual @PrePersist. Could plan `PlayerRating` with scalar fields and protected ctor/static create? Need exact. We might need inspect representative file to match conventions, but agent covered style generally. To provide file list, no exact implementation needed. We should maybe use read to inspect `MatchParticipant` and service code because we need specific APIs. But "don't duplicate delegated work" could be seen as duplicating. We can trust outputs. Plan file likely should be detailed, not code.

Could use custom entity fields `UUID id`, `UUID gameId`, `UUID raterId`, `UUID rateeId`, `int score`, `Instant createdAt`; no JPA relations needed, matches schema. Constructor with @PrePersist initialize. Is there repository extends JpaRepository. Queries:
- `existsByGameIdAndRaterIdAndRateeId`
- `findAllByGameIdAndRaterId` maybe GET options. To avoid N+1, query current user's ratings for game and map.
- Need list attended participants excluding current user. MatchParticipantRepository query likely can add `findByGameIdAndAttendedTrueAndUserIdNotOrderBy...` and fetch users. We need plan likely new repository methods.
- GET /ratings needs participant names. We can use `MatchParticipant` entities with only userId no relation, then `UserRepository.findAllById` batch. Existing UserRepository maybe basic. Could add method. Or use JPA query. To avoid N+1 and minimize modifications, we could make rating endpoint service:
  1 game find
  2 ensure rater participant attended
  3 `MatchParticipantRepository.findByGameIdAndAttendedTrue...`
  4 `UserRepository.findAllById(ids)` batch
  5 `PlayerRatingRepository.findByGameIdAndRaterId`
  6 combine.
This uses 4-5 queries once, acceptable. Need ensure owner can be included if attended. `MatchParticipant` user IDs.
- Could use a single native projection query joining `match_participant` app_user left joining `player_rating` for current rater. But existing architecture is JPA; PlayerStats uses JDBC. A native query would be more efficient but introduces query complexity. We can keep simple derived repository + batch user. User asked no unnecessary dependencies. The game max players likely small. Could add projection query to MatchParticipantRepository perhaps. But plan can specify.

Entity DTO naming:
- `SubmitRatingRequest(UUID ratedPlayerId, @Min(1) @Max(5) int score)` But user example field `rating`, schema field `score`. Existing frontend PlayerRating type uses score. Should API use `rating` or `score`? User says request may contain `ratedPlayerId`, `rating`; "backend domain focused numeric value"; existing schema score. Follow existing conventions/current type likely use `score`, but user explicitly potential. We need decide. Existing V1 uses score, unused PlayerRating type uses `score`, so best use `score` to align actual code. Yet prompt API example uses `"rating": 5`; could choose `rating` and map to score. But no need mismatch. We should explicitly state request uses `score` (1-5) because current schema/type uses `score`; perhaps this is a review decision. At end decision requiring review. Maybe product may prefer `rating` key. The goal calls rating value. We need likely choose `score`. This is a deliberate plan decision. Could include labels 1-5 only frontend; backend stores/accepts score.
- Response `PlayerRatingResponse` perhaps includes `id`, `gameId`, `ratedPlayerId`, `score`, `createdAt`. Do we expose `raterId`? Since endpoint response is to rater and frontend doesn't need it. Security says no raterId supplied. Visibility says individual records not public. Response could omit raterId. Entity maps ratee to ratedPlayer. Should response not include raterId to reduce exposure. Use `ratedPlayerId`, maybe `ratedPlayerName` not needed.
- POST returns 201 with `Location: /api/games/{gameId}/ratings/{ratingId}`? No GET individual endpoint planned. Don't send Location if no retrievable resource? Could still 201 body. Existing JoinRequest returns 201 no Location. Follow convention: `ResponseEntity.status(CREATED).body(...)`.
- GET response exact:
```
{
 "gameId": "...",
 "ratingsEnabled": true? 
 "players":[
  { "userId":"", "displayName":"", "profileImageUrl":null, "myRating": null|1..5 }
 ]
}
```
If endpoint only called when eligible, no need `ratingsEnabled`. `canRate` maybe useful. But frontend can see game status and participation. Backend GET errors when not eligible, and game detail controls panel. For loading, simple.
Could call `GameRatingsResponse(UUID gameId, List<RateablePlayerResponse> players)`.
- `RateablePlayerResponse(UUID userId, String displayName, String profileImageUrl, Integer myRating)`.
Need maybe only displayName/profile. Do not expose individual ratings from others. `myRating` is current user's own.
- GET lists only other players `attended=true` and excludes self. Sort by displayName? Could sort at service or query by joinedAt. Existing roster order joinedAt asc. Product example list. For trust, stable order by `joinedAt`. But response doesn't include joinedAt. Fine. Maybe sort by displayName for UX. No need.
- Ratings immutable: no PUT/PATCH/DELETE. Frontend stars disabled after submit. There is no way to change even if accidental. "unless existing schema strongly suggests otherwise" only created_at strongly immutable. Explicit.
- Rating deadline: indefinite. Once completed, rating allowed forever if both attended. This means game direct URL only, no history nav. Existing frontend has no completed games list. Should Phase 8 add `/my-games`? User likely areas completed game detail, and frontend direct access is known issue. We need decide scope. Do not add separate completed history API/page because out of scope and not requested. But this harms UX/trust: eligible users can only reach if direct link. Bottom nav has broken `/my-games`; could perhaps Phase 8 integrate on existing game detail only, not create history. Is Phase 8 acceptance requiring rating discoverability? Goal says eligible can rate. Current game detail works via direct URL. There is no history. Phase 7 plan says no history. Maybe Phase 8 frontend likely should add a "Rate players" section to existing completed game detail; not add history. Mention as tradeoff/risk and review decision: whether to add minimal completed-games navigation now or defer. But user says keep focused, likely defer. At end decisions require review could include this and rating window. We should not ask before drafting? The user asks decisions review at end. We can choose defaults and flag. But plan only and they want plan file. We need maybe ask user via question before final plan? System says feel free, ask when tradeoffs. But they explicitly instruct "At end provide decisions require review," so can present choices without blocking. Better not ask midstream.

- Profile APIs: `GET /api/users/me`? Actual profile controller maybe `GET /api/users/me`? Agents conflict: profile endpoint? One says profile response via ProfileController line 30-39; auth `/api/auth/me` also exists. User says inspect GET /api/users/me. Need exact. Agent says routes include GET /api/users/me and /id. Another says auth `/auth/me`. Let's clarify. `ProfileController` likely @RequestMapping("/api/users"), methods getMyProfile line 30, getUserProfile line 36. Frontend auth uses `/auth/me`. User wants both? They specifically says inspect existing GET /api/users/me and /id. Agent says yes. So profile response already includes stats averageRating. `/auth/me` likely only basic user. Phase8 profile integration should perhaps update only `/api/users/me` and `/api/users/{id}`, not `/auth/me`, because PlayerStatsResponse profile is there. Tests say profile API returns rating info and `/me` returns rating information. Could interpret `/api/users/me`. In plan, explicitly distinguish:
  - `GET /api/users/me` profile returns nested stats and will include ratingCount.
  - `GET /api/users/{id}` same.
  - `GET /api/auth/me` remains auth-context lightweight and is not expanded to stats unless product decides. This avoids duplication.
The user says "Inspect current profile APIs and frontend" and likely useful fields. Good.
- Frontend profile pages currently absent. Phase8 requires profile displays rating. Need create `/profile` and `/profile/[id]`? User says "Rating appears on player profile" and "profile should communicate..." If no current profile frontend, we must plan to create profile page(s). But "integrated existing pages rather than redesigning". Existing BottomNav links /profile but page missing. So implement minimal profile pages/API as part of Phase8? Scope could create profile stats card on both. There is no profile API client. Need likely create `frontend/app/(app)/profile/page.tsx` and `profile/[id]/page.tsx`, `lib/api/profiles.ts`, maybe shared `ProfileStats` component. This is necessary for product goal. But user said do not start Phase9; current plan numbering. Fine. Yet they want inspect profile frontend. We should explicitly note absent pages, and Phase8 must add minimal display-only pages to make rating visible, not full profile edit or broad profile feature. The bottom nav already has profile link, so natural.
- How public users navigate to profile? Game detail only host info no profile link; join applicant names aren't links. Could create profile page but not navigation. Goal says rating appears on profile, existence enough. Maybe link applicant/host names to `/profile/{id}`? That broadens. Could add profile links from game detail host and rating peers perhaps. But Phase8 focus. Maybe plan not add navigation beyond existing bottom nav for own profile. The rating UI could list names but not link. Could add `Link` to each rateable player profile naturally; not necessary. If profiles display aggregate, public profile should be reachable. Yet no current navigation. We can make own profile page and route. Public `/profile/[id]` for later linking. Maybe acceptance doesn't require link from game. But creating dead route is odd. Could add profile links from host/applicant but out of scope. The prompt explicitly says profile appears and join request, likely okay to build minimal pages.
- "No global state management", local.
- Profile response types currently stale. Need update `UserProfile` in `lib/types.ts` to exact backend, possibly add `PlayerStats` already. There is `UserProfile` currently old fields. We need create `Profile` interface or correct existing. `MeUser` auth remains.
- Profile `GET /api/users/me` requires auth; public `/id` still auth due current security, not anonymous. Rating visibility means visible to authenticated users, consistent with existing profile access. The prompt says public average; "public" could mean not rating individual. Since all APIs auth, keep route authenticated. Explicit: aggregate visible to any authenticated user who can access profile; no anonymous exposure change. Do not relax security.
- `averageRating` formatting: backend Double raw AVG (e.g. 4.25). UI show one decimal (4.3 due JS toFixed; 4.25 can round weird to 4.3; okay). Should backend round? Existing no rounding. User likely 4.6. Keep numeric full precision in API, format to one decimal frontend. Need maybe count. No rounding in DB; JSON Double.
- Statistics source query: combine avg and count in one query. Should query join game and participants to filter only valid completed and attended ratee? The user says "ratings from incomplete games do not exist / do not count." Since application service guarantees rows only valid, but table can be manually seeded or future code bug. The test includes "ratings from incomplete games do not exist / do not count". To make stats trustable, query should filter `g.status='COMPLETED'` and both rater and ratee attended? Do we need? If all rows should be valid by service, count rows enough. But current table allows invalid rows. To ensure derived query trust, join game and match participants. However that adds complexity and duplicates rows if participant uniqueness ensures one row each. Could query:
```
SELECT AVG(pr.score), COUNT(*)
FROM player_rating pr
JOIN game g ON g.id=pr.game_id
JOIN match_participant rater ON ... attended true
JOIN match_participant ratee ...
WHERE pr.ratee_id = ?
  AND g.status='COMPLETED'
```
This excludes invalid historical/manually inserted ratings. But then "ratingCount" is count of valid rating records only, consistent with policy. This may be prudent. The prompt specifically says ratings from incomplete games do not exist / do not count. Service ensures no existence; query filter ensures do not count. Is it overkill? It is defense-in-depth, no migration. However "Player average rating/statistics are updated through derived queries", source player_rating. Joining to conditions is fine. Should we enforce rater attendance too? Policy requires both attended. A rating inserted before attendance marks? Service only after completed; attendance immutable after completion, so it won't become false. But manual invalid. Query could enforce both. This increases query cost but index on ratee. Postgres planner handles. We can state aggregation applies same validity predicate: completed game, ratee and rater attended. This ensures invalid rows never count. Yet if we trust data, simpler query by ratee only. The tests ask invalid rows don't count, so include joins.

Could also use `COUNT(*)` and AVG. `COUNT` returns 0; AVG null. Map to `RatingStats(Double averageRating, long ratingCount)`. There is current separate findParticipationStats. Keep two queries per profile (participation, rating). No need combine across distinct tables. Could make one query overall but not necessary. The user says do not duplicate rating queries, one rating aggregate is enough.
- Applicant query N+1 existing. Adding ratingCount via same one rating aggregate per applicant doesn't worsen. Could add a shared assembler method `PlayerStatsService` to avoid duplicate assembly in ProfileService and JoinRequestService. User says reuse existing abstraction. Currently both manually call repository and construct. Phase8 could create `PlayerStatsService` in profile module to centralize; but is that unnecessary? It avoids duplication, especially adding count. It also means JoinRequestService depends on service. Architecture is modular monolith, okay. However "do not duplicate rating queries unnecessarily"; they currently both call one query each. A `PlayerStatsService.getStats(userId)` is good to centralize, but new service extra file. Could be in plan.
- Alternatively static factory in `PlayerStatsResponse`? No. We should plan `PlayerStatsService` perhaps:
```
@Transactional(readOnly=true)
public PlayerStatsResponse getPlayerStats(UUID userId) {
 participation = repo.find...
 rating = repo.find...
 return new ...
}
```
Use in ProfileService and JoinRequestService. This centralizes. But note nested self-invocation / transactions. If public method called from another service transaction, fine. It performs two queries. It should be `@Transactional(readOnly=true)`. No need but convention.
- Could optimize join request N+1 by batch, but out of scope. Mention preserve current N+1 and no duplicate per applicant. User says "do not duplicate rating queries unnecessarily." A shared service meets. We should not batch because complexity. Could possibly call one query per applicant; still N+1 but existing. Phase8 does not worsen. At risk section.
- ProfileService maybe not transactional but reads only. Add `PlayerStatsService`.
- GET rating eligibility endpoint needs profile display info. Could use `UserRepository.findAllById`. Need inspect `UserRepository` method maybe only findById and existsByEmail. Spring Data supports `findAllById` already inherited, no modification. Good. It might return users in arbitrary order; map by ID.
- Participant repository method: `findByGameIdAndAttendedTrueAndUserIdNotOrderByJoinedAtAsc`. Spring Data property nested? Entity field likely `gameId`, `attended`, `userId`, `joinedAt`. Method name `findByGameIdAndAttendedTrueAndUserIdNotOrderByJoinedAtAsc`. Good.
- Existence checks for rater/rated: use `MatchParticipantRepository.findByGameIdAndUserId` currently maybe existing? Agents only mention exists. Could add `findByGameIdAndUserId` returning entity to distinguish absent vs attendance. Use one query per user. Or two exists with attended? Service:
  - rater = findByGameIdAndUserId; absent -> `RATER_NOT_PARTICIPANT`; attended != TRUE -> `RATER_ATTENDANCE_REQUIRED`.
  - rated user existence. To avoid two separate User check? participant lookup includes FK but if participant exists user necessarily exists. If no participant, need decide nonexistent vs not participant. We can call `UserRepository.existsById` to return `USER_NOT_FOUND` 404. That is another query. For valid rated, participant entity enough, no UserRepository needed for POST. GET needs user details. So rules:
    1 `ratedParticipant = find...`; if null, `existsById` to distinguish 404 user vs 403 not participant, else 404. Or return same `RATED_PLAYER_NOT_FOUND_OR_NOT_PARTICIPANT` to avoid enumeration. But test specifically says nonexistent player invalid; doesn't dictate code. Could choose a single `RATED_PLAYER_NOT_ELIGIBLE` 404 for both nonexistent and nonparticipant, protecting user enumeration. But user asks "nonexistent player" edge. Document 404 `RATED_PLAYER_NOT_FOUND`? Simpler to test.
  - status `COMPLETED`; need check before user participant. Existence first.
  - rater user existence? Principal filter creates principal even if user deleted; services often verify user exists (JoinRequestService). Should verify `AuthenticatedUser` and UserRepository exists; if missing perhaps 401? Existing code uses `USER_NOT_FOUND`. For phase8 maybe principal ID is enough. Security contract says game exists etc. No frontend rater. We can use principal directly. Account deletion not implemented. Not central.
- Order of checks matters test. We can define deterministic:
  1 authenticated.
  2 game exists.
  3 game completed.
  4 rater participant.
  5 rater attended true.
  6 self.
  7 target user exists.
  8 target participant.
  9 target attended true.
  10 duplicate.
  11 save.
But if self target nonexistent? Self always user exists. Could self check earlier after game? Doesn't matter. Request validation first: score range, body. Then game status. For security avoid leaking target details if rater not eligible; check rater first. Good. Target user existence after self. Duplicate maybe after eligibility; better. If already rated but game changed? game immutable completed. Fine.
- `game is COMPLETED` check before participant, so noncompleted returns conflict even nonparticipant. Good.
- "player tries to rate someone from another game": target user may exist but no participant row in this game => 403 `RATED_PLAYER_NOT_PARTICIPANT` or 404. Choose 403.
- target not attended false/null => 403 `RATED_PLAYER_ATTENDANCE_REQUIRED`.
- rater false/null => 403 `RATER_ATTENDANCE_REQUIRED`.
- duplicate
409.
- GET eligibility error:
  - game not found 404
  - not completed 409
  - rater not participant 403
  - attendance not true 403
  - returns 200 eligible even if no peers (self only). UI empty state.
Could use shared private method.
- After completion no expiry. This means rating window indefinite. Explicit acceptance.
- Self enforced DTO/database. Score enforced DTO and DB check.
- rating entity immutability: no setters. No updated_at. Good.
- Timestamps createdAt @PrePersist or rely on DB. Existing entities initialize; if entity sets Instant.now and schema default, both. Need follow. For concurrent createdAt, fine. But DB default exists. Could map `insertable=false/updatable=false`? Existing entities assign. Plan can say use `@PrePersist` consistent with `MatchParticipant`, and schema remains default fallback. No updated_at.
- No migration: V1 sufficient. We must explicitly say **No migration required.** Need be bold exact.
- But actual migrations only V1-V3, not V1-V7. User asked inspect V1-V7. Explicit discrepancy: no V4-V7; phases not migration numbers. Current highest is V3. Existing V1 contains table; V2 position; V3 discovery. No migration. This is important.
- Could schema perhaps lack updated timestamp; immutability supports. No need.
- Is created_at enough to determine rating expiry? yes, but no completion timestamp. No expiry chosen.
- "Rate after time passed": allowed indefinitely, no expiration. If policy wanted window, need store completedAt or immutable deadline. Not now.
- APIs authentication:
  - `POST` and GET require cookie auth by global `/api/**`; `@AuthenticationPrincipal`.
  - all authenticated, not owner-only; only eligible participant.
  - No public individual ratings.
- No `GET /api/users/{id}/ratings`. Since profile includes aggregate and no individual visibility. Explicitly rejected.
- No PUT/PATCH/DELETE. Explicit.
- Maybe GET same ratings route could be misunderstood as "view ratings for player". Explain exact.
- Error responses existing `ApiError`.
- For DB concurrency catch. We should define fallback mapping. If unique violation may happen at flush. Use `saveAndFlush`. In service, if precheck, catch `DataIntegrityViolationException`, throw `BusinessException(CONFLICT, RATING_ALREADY_EXISTS, "You have already rated this player for this game")`. This transaction rolls back. Could violate other DB constraints, but app validation plus known schema. If catch all as duplicate, dangerous. Better inspect root cause constraint name `uq_rating`, and only map that; otherwise rethrow. Plan should mention inspect `PSQLException` server error? That's implementation detail. We can say translate only `uq_rating`; rethrow unrelated integrity failures. This is robust. But no comments.
- Database unique final.
- Could use a separate exception mapping. No need global handler change if service catches.
- Test concurrency: two simultaneous POSTs same payload; one 201 one 409; repository count 1. Need use actual HTTP/cookies, CountDownLatch maybe. Ensure JUnit test no comments. Plan only.
- Tests schema:
  - existing Flyway schema test already checks uq, score, self. Extend for exact columns/index perhaps optional. Since no migration, add constraint behavior tests in rating integration.
  - invalid score via API 0,6; null? Integer primitive vs Integer. DTO int missing becomes 0 -> validation error, but malformed missing should ideally 400. Use `Integer score` with @NotNull @Min @Max. If primitive int, missing maps 0 and error but message not missing. Better use `@NotNull @Min @Max Integer score`. `PlayerRating` entity uses int. This ensures missing -> 400.
  - UUID null @NotNull.
  - unknown user 404.
  - cross-game 403.
  - self.
  - no participant.
  - false/null attendance.
  - noncompleted status.
  - completed both attended.
  - rating only after grace? game status complete enough; tests set status directly or scheduler. Should test through direct status.
  - Duplicate.
  - GET only returns attended other participants, excludes self, absent/unmarked, and shows own rating values but never other ratings. Test no rater IDs? The response not include.
  - privacy endpoint.
  - profile `/api/users/me` and `/api/users/{id}` stats count/avg.
  - `/api/auth/me` remains no stats maybe not required.
  - applicant panel test via owner GET: applicant stats include avg and count. Existing test setup can be extended/new JoinRequest integration.
  - multiple games aggregate.
  - invalid raw rating in incomplete game does not count if stats query joins. But such raw insert possible test. We can insert repository directly (bypasses service) and verify excluded from stats. Also query GET doesn't list? Not needed.
  - absent rater raw row excluded.
  - no ratings -> null/0.
  - average correct e.g. 5+4+3=4.0 count3; no rounding assertion perhaps.
  - labels not backend.
  - frontend no tests. Plan could add no new test framework (out of scope). Verification via lint/build/manual. User asks comprehensive integration test plan at minimum backend. Could mention no frontend automated suite exists; do not add one solely for Phase8. But for rating UI critical interactions, maybe adding React testing library would be unnecessary dependency. We can specify typecheck/lint/build and manual QA, unless project later adds frontend test tooling. This adheres.
- Backend tests commands: Gradle `./gradlew test` on Windows `.\gradlew.bat test`; lint/typecheck? Java compile via test. Frontend npm run lint/build. AGENTS empty, no commands there, but build files provide. Plan file should state.
- File likely created/modified: need exact list likely.
Backend create:
  - `backend/src/main/java/com/gameconnect/rating/entity/PlayerRating.java`
  - `backend/src/main/java/com/gameconnect/rating/repository/PlayerRatingRepository.java`
  - `backend/src/main/java/com/gameconnect/rating/service/PlayerRatingService.java`
  - `backend/src/main/java/com/gameconnect/rating/controller/PlayerRatingController.java`
  - DTOs `dto/CreatePlayerRatingRequest.java`, `dto/PlayerRatingResponse.java`, `dto/GameRatingsResponse.java` or `RateablePlayerResponse`.
Could package under `game` rather than rating? Existing packages auth/game/profile. Ratings cross game/profile. A new `rating` modular package is clean. But "modular monolith" package boundaries. Could place under `game` because rating created with game. Yet profile uses rating query. `rating` package is natural and not duplicate. Need maybe package dependency: `game` depends on `rating`? If service uses game entities. A separate rating module can depend on game/auth/profile. `JoinRequestService` no need. Good.
  - `PlayerStatsService` create under profile/service to centralize.
  - Modify `PlayerStatsResponse`, `ProfileService`, `JoinRequestService`, `MatchParticipantRepository`, frontend types/API etc.
Could avoid `PlayerStatsService` and duplicate assembly in two services. But plan likely create.
  - Maybe add `UserRepository.findAllById` inherited, no modify.
  - Tests create `backend/src/test/java/com/gameconnect/rating/PlayerRatingIntegrationTest.java` and perhaps `RatingConcurrencyIntegrationTest`. Could combine.
  - Modify `ProfileIntegrationTest`, `FlywaySchemaTest` maybe not necessary but plan.
  - Add `JoinRequestIntegrationTest`? Existing indirect. Could create `backend/src/test/java/com/gameconnect/game/JoinRequestApplicantStatsIntegrationTest.java`.
  - `PlayerStatsRepository` modify combined query.
Frontend create:
  - `frontend/lib/api/ratings.ts`
  - `frontend/components/games/GameRatingPanel.tsx`
  - perhaps `frontend/components/profile/PlayerStatsCard.tsx`
  - `frontend/app/(app)/profile/page.tsx`
  - `frontend/app/(app)/profile/[id]/page.tsx`
  - maybe `frontend/lib/api/profiles.ts`.
  - Could share profile page component. Better create `frontend/components/profile/ProfileView.tsx` to avoid duplication, but perhaps overkill. A single `ProfilePage` component can accept profile and current. But no need. We can list likely.
  - Modify `frontend/lib/types.ts` `PlayerStats`, profile response; remove or repurpose `PlayerRating` placeholder? Don't remove if response uses it? Could use `PlayerRating` as POST response. Existing fields raterId/rateeId don't match desired. Modify it to response shape? Shared types currently `PlayerRating` raw record with rater/ratee. Since we decide no individual listing, can redefine or remove. Better create `SubmitRatingResponse` and leave `PlayerRating`? Unused existing is duplicate placeholder; user says don't create duplicate structures. We should reuse and align `PlayerRating` type to response? Entity model includes rateeId, but frontend could use `ratedPlayerId`. Current placeholder not used. Plan says replace unused `PlayerRating` with `RatingResponse` (or reshape). No backend class duplicates. But "existing PlayerRating entity" none. A type named `PlayerRating` is okay but implies raw. We can modify to include only response fields.
  - Modify `frontend/components/games/JoinRequestsPanel.tsx` display `Rating: 4.3 ★ (8 ratings)` or if null "No ratings yet". Use rating count to show one decimal. If average exists, count should >0.
  - Modify `frontend/app/(app)/games/[id]/page.tsx` to render rating panel for game completed and user attended. `myParticipation?.attended === true`.
  - Modify maybe `BottomNav` no change because /profile link exists. No app route proxy change needed because proxy already protects /profile. `profile/[id]` route coverage? Proxy matcher `/profile` may match all? Need inspect proxy matcher. Agent says protects `/profile`, likely matcher string maybe starts with. Next 16 proxy. Could list likely no change. If exact, need adjust proxy to `/profile/:path*`. Agent says currently `/profile` and route nonexistent. We need check actual file? Delegated report says route protection lines 4-7. It may list `/profile` exact. We need know to plan. Could read `proxy.ts` now, but that duplicates frontend agent. However to accurately plan route, we may use read. Instruction says don't duplicate delegated work, but it's a specific file already delegated. We can trust. Agent didn't explicitly say path matcher. It says protects /profile and /profile/[id] maybe only prefix. Next matcher likely:
```
export const config = { matcher: ["/games/:path*", "/create-game", "/my-games", "/profile"] }
```
In Next, exact path may not match nested. If so creating nested profile requires modify proxy. We need know. Could ask frontend agent to clarify using task_id. This is not duplicate, just continuation. Good. Also need inspect exact profile controller route. Ask backend agent perhaps. But reports enough. Could ask frontend agent specifically proxy and route matching. Also profile page absent. Let's resume task to return exact proxy config and recommend whether modification.

**Tool: task**

**Input:**
```json
{
  "description": "Clarify profile route guard",
  "prompt": "Continue the read-only investigation. Return the exact `frontend/proxy.ts` matcher/config and explain whether creating `app/(app)/profile/page.tsx` and `app/(app)/profile/[id]/page.tsx` requires a proxy change to protect the nested route. Also state whether the existing BottomNav already links to `/profile`. Include line references only; no edits.",
  "subagent_type": "explore",
  "task_id": "ses_f283f6e27ffeJDr2A0JeRlJenh",
  "command": "Clarify profile route guard"
}
```

**Output:**
```
<task id="ses_f283f6e27ffeJDr2A0JeRlJenh" state="completed">
<task_result>
## Exact proxy config

`F:\Game-connect\frontend\proxy.ts:4-7`

```ts
const AUTH_COOKIE = "auth_token";

const PUBLIC_PATHS = ["/login", "/register"];
const PROTECTED_PATHS = ["/games", "/create-game", "/my-games", "/profile"];
```

`F:\Game-connect\frontend\proxy.ts:24-29`

```ts
if (PROTECTED_PATHS.some((p) => pathname.startsWith(p))) {
  if (!isAuthenticated) {
    return NextResponse.redirect(new URL("/login", request.url));
  }
  return NextResponse.next();
}
```

`F:\Game-connect\frontend\proxy.ts:40-44`

```ts
export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico).*)",
  ],
};
```

## Nested profile route

No proxy change is required to protect `app/(app)/profile/[id]/page.tsx`.

- The `(app)` route group is not part of the URL; the page maps to `/profile/[id]`.
- `pathname.startsWith("/profile")` matches both:
  - `/profile`
  - `/profile/[id]`
- This behavior comes from `F:\Game-connect\frontend\proxy.ts:7` and `F:\Game-connect\frontend\proxy.ts:24`.
- The matcher also includes the nested route because it excludes only API, static, image, and favicon paths: `F:\Game-connect\frontend\proxy.ts:40-44`.

The existing prefix match would also match paths beginning with `/profile`, such as `/profile-other`; that is the current behavior.

## BottomNav link

Yes. `F:\Game-connect\frontend\components\layout\BottomNav.tsx:8-13` already defines:

```ts
{ href: "/profile", label: "Profile" }
```

The link is rendered at `F:\Game-connect\frontend\components\layout\BottomNav.tsx:36-48`.

For nested `/profile/[id]` paths, the existing active-state check is exact for non-games routes:

- `F:\Game-connect\frontend\components\layout\BottomNav.tsx:21-22`

Therefore `/profile` is active on `/profile`, but not automatically on `/profile/[id]`.
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 147.2s)

_Thinking:_

Great.

Need perhaps clarify exact ProfileController and `/api/auth/me` shapes. Ask backend agent maybe not needed, but plan can be precise. Use task id resume? We have backend task id. Ask a focused question.

**Tool: task**

**Input:**
```json
{
  "description": "Clarify profile API contracts",
  "prompt": "Continue the read-only inspection. State the exact current routes and response construction for `GET /api/users/me`, `GET /api/users/{id}`, and `GET /api/auth/me`; confirm whether `/api/auth/me` includes stats. Also state the exact existing method names/signatures in `PlayerStatsRepository`. Include concise line references; no edits.",
  "subagent_type": "explore",
  "task_id": "ses_f283f7c9cffegCg3VE5Hs3bIPj",
  "command": "Clarify profile API contracts"
}
```

**Output:**
```
<task id="ses_f283f7c9cffegCg3VE5Hs3bIPj" state="completed">
<task_result>
Continued read-only inspection; no edits or tests were run.

## Additional verification

### No alternate profile/auth mappings

The backend search found only:

- `GET /api/users/me`
- `GET /api/users/{id}`
- `PATCH /api/users/me`

in `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\controller\ProfileController.java:30-46`.

For authentication, the only `/me` mapping is:

- `GET /api/auth/me`

in `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:72-90`.

The separate `/me` match in `F:\Game-connect\backend\src\main\java\com\gameconnect\game\controller\JoinRequestController.java:43` is the authenticated user’s join-request endpoint, not a profile endpoint.

### Tests confirm profile stats

- `/api/users/me` returns profile data and a `stats` object:  
  `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:48-60`
- `/api/users/{id}` returns the target profile with stats:  
  `F:\Game-connect\backend\src\test\java\com\gameconnect\profile\ProfileIntegrationTest.java:65-78`
- Completion tests query `/api/users/me` and assert attended-only statistics:  
  `F:\Game-connect\backend\src\test\java\com\gameconnect\game\GameCompletionIntegrationTest.java:240-326`

### Auth `/me` has no stats path

`GET /api/auth/me` constructs `UserResponse` directly from the `User` entity:

`F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:75-90`

It never calls `ProfileService` or `PlayerStatsRepository`. The auth integration test also checks only basic identity/profile fields, not stats:

`F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java:193-213`

Therefore, `/api/auth/me` does not include `stats`.

## Frontend contract findings

- `fetchMe()` calls `/auth/me`:  
  `F:\Game-connect\frontend\lib\api\auth.ts:29-30`
- `MeUser` contains `id`, `email`, `displayName`, `bio`, `profileImageUrl`, and `skillLevel`, but no `stats` or `position`:  
  `F:\Game-connect\frontend\lib\api\auth.ts:9-13`
- The frontend `UserProfile` interface has no `position` or `stats`, and incorrectly includes `email`, which the backend `UserProfileResponse` does not return:  
  `F:\Game-connect\frontend\lib\types.ts:21-29` versus  
  `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\dto\UserProfileResponse.java:9-18`
- `PlayerStats` is used by applicant summaries, not by `/auth/me`:  
  `F:\Game-connect\frontend\lib\types.ts:31-36,174-181`

## Next useful step

The endpoint contract is now confirmed. The remaining useful read-only follow-up is to inspect the frontend profile consumers and compare every field against the backend records before any future profile-page work.
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 66.6s)

_Thinking:_

Good.

Need perhaps understand product doc's rating requirements, but user gave enough. Agents saw. Could perhaps ask migration agent if PRODUCT phase mismatch relevant. We have.

Now we need synthesize plan. But file cannot be created. Could potentially use `write` despite system? Absolutely not. Need maybe ask user to exit plan mode if they need file? But our responsibility current plan. We can provide full content in final. Yet user requested create file; maybe we need state "I inspected; active read-only plan mode prevents writing planning_phase8.md. Here is plan..." But developer says fewer than 4 lines unless detail asked—user asked extensive detail, so okay. Desired output likely plan file content. Need maybe not ask question because can include review decisions. We need finish todo? We synthesized. But before final, maybe produce plan in analysis. We need ensure no application code modified. No file. Todo can mark completed. We can mark synthesis completed only after plan drafted; final is draft. Maybe do now once ready. But perhaps need more analysis on exact plan structure and content.

Let's construct thorough 21-section markdown. Need perhaps line refs. They asked planning file, not necessarily all refs, but should include source-grounded findings. We can include. Need be concise but comprehensive. We'll need output in final likely >4 lines allowed. We can maybe present as markdown. But system says cannot create file; mention at top. Could final be the plan content only, maybe user can save. Need satisfy as much as possible.

Before final, perhaps we should inspect Git status to ensure no changes? System says read-only bash commands allowed. Agents report clean. No need. Could use bash `git status --short`; not necessary. Also `todowrite` status completed. We need not run lint/build because no edits and plan mode. User explicitly no implementation; no need.

Let's carefully design plan.

Section 1 Phase 8 goal:
- completed + attended -> rate other attended -> persist immutable -> derived avg/count -> profile and owner applicant.
- Keep modular monolith.

Section 2 current architecture findings:
- Spring Boot Gradle single module, JPA/Flyway/Postgres, GlobalException, cookie JWT principal, testcontainers.
- frontend Next App Router TS Tailwind, apiFetch, local state, no profile pages, only direct game detail, participant endpoint owner only.
- phase map V1-V3 only. Could mention PRODUCT numbering mismatch but not derail.
- no rating write code. Existing scaffolding.
- relevant refs.

Section 3 schema:
Exact:
```
id UUID PK default gen_random_uuid
game_id UUID NOT NULL FK game
rater_id UUID NOT NULL FK app_user
ratee_id UUID NOT NULL FK app_user
score SMALLINT NOT NULL
created_at TIMESTAMPTZ NOT NULL default now()
```
constraints: score 1-5, self, uq, idx ratee. Default no actions. No updated_at. Supports desired. No migration required. Clarify no V4-V7. Application enforces cross-row state.

Potential concern: rating table unique index order `(game_id,rater_id,ratee_id)` supports game/rater lookup. Ratee index supports aggregate. Good.

Section 4 domain model:
- Entity `PlayerRating` maps one row.
- conceptual rater -> ratee in game -> score.
- fields.
- immutable.
- no relations perhaps scalar.
- No update endpoint.
- no counters.

Section 5 eligibility:
Rules:
1 auth
2 game exists, COMPLETED
3 rater participant
4 rater attended TRUE
5 target exists
6 target participant same game
7 target attended TRUE
8 not same
9 no existing
10 score.
Maybe self check before target existence? irrelevant.
- False/null both ineligible. Table.
```
Rater participant? attended? Can give/receive
false/no - no both? A nonparticipant doesn't have row. If attended false/null no.
```
"Can absent player give?" no. Receive? no. Unmarked same.
- Owner is participant but only if attended true.
- No expiration indefinitely.
- Ratings only after automatic completion or direct status complete; no time window. Completion immutable (attendance service blocks after).
- If attendance changes? Owner cannot change once completed, so eligibility stable.
- We should note existing completed status no manual complete endpoint, scheduler. Rating service checks status, no timing check.
- Maybe attendance `null` status is not eligible even after completion. This can result players with no rating opportunity; deliberate reliability.
- target same game.

Section 6 validation:
- request only `ratedPlayerId`, `score`, not rater.
- @NotNull UUID, @NotNull @Min1 @Max5 Integer.
- unknown/malformed etc.
- DB checks final.
- score labels UI only.
- no expiration, no updatedAt.
- error code matrix. Could be in API or validation.
Need include `rating` vs `score` decision. The user example uses rating but actual schema uses score. State use `score` for consistency. Decision at review. If they insist `rating`, mapping is trivial but breaking new API no clients.

Section 7 API:
A. POST `/api/games/{gameId}/ratings`
- auth required
- authorization rules
- body
```
{"ratedPlayerId":"...", "score":5}
```
- response 201:
```
{
"id":"...",
"gameId":"...",
"ratedPlayerId":"...",
"score":5,
"createdAt":"..."
}
```
No raterId.
- 400 `VALIDATION_ERROR` for invalid/missing score/rated id/malformed UUID; `SELF_RATING_NOT_ALLOWED`.
- 404 GAME_NOT_FOUND, USER_NOT_FOUND/RATED_PLAYER_NOT_FOUND.
- 409 GAME_NOT_COMPLETED, RATING_ALREADY_EXISTS.
- 403 RATER_NOT_PARTICIPANT, RATER_ATTENDANCE_REQUIRED, RATED_PLAYER_NOT_PARTICIPANT, RATED_PLAYER_ATTENDANCE_REQUIRED.
Maybe status for nonparticipant target 404 vs 403. We need settle. 403 is authorization eligibility. Fine.
- 401.
Potential `rated player nonexistent` code `USER_NOT_FOUND` or `RATED_PLAYER_NOT_FOUND`; exact. Use `PLAYER_NOT_FOUND`? Existing code uses `USER_NOT_FOUND` maybe. Agents report join service authenticated user missing? Need check existing error names. Could use `USER_NOT_FOUND` likely. User asks follow conventions. Let's ask backend agent exact user not found code. It might be `USER_NOT_FOUND`. Could use task, but not necessary. Let's query focused maybe.

**Tool: task**

**Input:**
```json
{
  "description": "Clarify error code names",
  "prompt": "Continue read-only inspection. List the exact existing business error codes/messages used for: authenticated principal's user missing, game missing, user/participant missing, forbidden, invalid body, and duplicate unique-row conflict. Include file:line references. No edits.",
  "subagent_type": "explore",
  "task_id": "ses_f283f7c9cffegCg3VE5Hs3bIPj",
  "command": "Clarify existing error code names"
}
```

**Output:**
```
<task id="ses_f283f7c9cffegCg3VE5Hs3bIPj" state="completed">
<task_result>
Continued read-only inspection; no edits were made.

## Exact domain error codes/messages

### Authenticated principal’s user missing

There is no separate `AUTHENTICATED_USER_NOT_FOUND` code. The existing code is:

- HTTP `404`
- Code: `USER_NOT_FOUND`
- Message: `User not found`

For `/api/auth/me`, the principal ID is looked up at:

`F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java:72-79`

The same code/message is also used for principal validation during:

- Game creation: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:55-61`
- Join-request creation: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:52-58`
- Profile lookup for a user ID: `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:73-78`

### Game missing

Existing domain error:

- HTTP `404`
- Code: `GAME_NOT_FOUND`
- Message: `Game not found`

Occurrences:

- Game detail: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:194-200`
- Attendance update lock: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:55-62`
- Attendance roster lookup: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:127-132`
- Join-request decision lock: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:134-141`
- General join-request game lookup: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:215-220`

A missing route or static resource is handled separately with:

- HTTP `404`
- Code: `NOT_FOUND`
- Message: `Resource not found`

`F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:59-62`

### User missing

The user-missing error is:

- HTTP `404`
- Code: `USER_NOT_FOUND`
- Message: `User not found`

The profile service uses it for an arbitrary requested user ID:

`F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:73-78`

The same literal code/message is used for the authenticated principal checks listed above.

### Participant missing

The existing error is game-scoped:

- HTTP `404`
- Code: `PARTICIPANT_NOT_FOUND`
- Message: `Participant not found for this game`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:67-72`

### Forbidden

There are two distinct forbidden responses.

#### Owner authorization failure

- HTTP `403`
- Code: `FORBIDDEN`
- Message: `Only the game owner can perform this action`

Occurrences:

- Attendance owner check: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\AttendanceService.java:135-140`
- Join-request owner check: `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:223-228`

#### Spring Security access denial

- HTTP `403`
- Code: `FORBIDDEN`
- Message: `Access denied`

`F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java:58-67`

## Invalid body errors

### Malformed or unreadable JSON

- HTTP `400`
- Code: `MALFORMED_BODY`
- Message: `Request body is malformed`

`F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:46-50`

This includes malformed JSON and invalid enum/deserialization values.

### Bean-validation failure

- HTTP `400`
- Code: `VALIDATION_ERROR`
- Message prefix: `Request validation failed: `
- The full message is dynamically followed by the field-error map.

`F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:30-43`

Manual business validation also uses `VALIDATION_ERROR`, but with explicit domain messages, for example:

- `At least one profile field must be provided`  
  `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:34-40`
- `Display name must not be blank`  
  `F:\Game-connect\backend\src\main\java\com\gameconnect\profile\service\ProfileService.java:44-50`
- `Game date must not be in the past`  
  `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:96-102`
- `End time must be after start time`  
  `F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\GameService.java:109-114`

## Duplicate unique-row conflicts

### Duplicate account email

- HTTP `409`
- Code: `DUPLICATE_EMAIL`
- Message: `An account with this email already exists`

`F:\Game-connect\backend\src\main\java\com\gameconnect\auth\service\AuthService.java:26-32`

This is currently an explicit pre-check using `existsByEmail`; there is no local `DataIntegrityViolationException` catch in `AuthService`.

### Duplicate join request

Before saving, the service uses:

- HTTP `409`
- Code: `JOIN_REQUEST_EXISTS`
- Message: `You already have a pending request for this game`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:83-87`

If the database unique constraint races with the pre-check, the local catch uses:

- HTTP `409`
- Code: `JOIN_REQUEST_EXISTS`
- Message: `You already have a request for this game`

`F:\Game-connect\backend\src\main\java\com\gameconnect\game\service\JoinRequestService.java:98-111`

There is no generic `DataIntegrityViolationException` mapping for other unique constraints. Uncaught database integrity errors fall through to:

- HTTP `500`
- Code: `INTERNAL_ERROR`
- Message: `An unexpected error occurred`

`F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java:64-68`
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 33.9s)

_Thinking:_

Good.

API B. GET `/api/games/{gameId}/ratings`
- Auth.
- only current eligible rater.
- response.
- no GET user ratings.
- errors same (minus self target etc).
Could perhaps return `200` with players. For no peers, empty.
- If rater already rated some, `myRating`.
- Does it expose score? Yes own.
- Could call route after submitted to refresh.
No need `GET /api/users/{id}/ratings`.
No endpoints for public individual.

Profile:
- no new profile endpoint; existing responses update.
- `GET /api/users/me` & id include ratingCount automatically.

Section 8 auth/security:
- principal derivation.
- checks.
- no trust frontend.
- privacy.
- error pattern.
- cookie.
- possibly user ID enumeration via aggregate public but current profiles accessible authenticated. No change.

Section 9 transaction/concurrency:
- @Transactional
- game find read-only; no lock.
- exists check, saveAndFlush unique.
- map uq only.
- no update.
- no counter consistency.
- transaction returns.
Could we need lock game to prevent status? Completed terminal, so no. No lock. There is no transition completed->other. Good.
- A duplicate precheck query with unique final.
- rating record immutable.

Section 10 stats/profile:
- Add ratingCount to DTO.
- combined RatingStats aggregate.
- SQL with validity joins. Need decide exact. Let's formulate query. Need PostgreSQL alias mapping in JdbcTemplate. Current row mapper for participation. Could SQL:
```
SELECT AVG(pr.score) AS average_rating, COUNT(*) AS rating_count
FROM player_rating pr
JOIN game g ON g.id = pr.game_id
JOIN match_participant rater
  ON rater.game_id = pr.game_id AND rater.user_id = pr.rater_id AND rater.attended = TRUE
JOIN match_participant ratee
  ON ratee.game_id = pr.game_id AND ratee.user_id = pr.ratee_id AND ratee.attended = TRUE
WHERE pr.ratee_id = ?
  AND g.status = 'COMPLETED'
```
This excludes raw invalid rows. Is `COUNT(*)` bigint; DTO `long`. Java `int` maybe rating count likely int; user sample fields likely int. Use long robust. Current counts are int from `COUNT` via `getInt`. Should ratingCount long. Frontend number. No ratings: avg null, count 0.
- Could count only ratings that count at the time. yes.
- no `ROUND`.
- PlayerStatsService.
- profiles.
- `GET /api/auth/me` remains unchanged.

Section 11 Phase6:
- add count to shared stats.
- no direct fields.
- display `Rating: 4.3 ★ · 8 ratings`, no ratings "No ratings yet" maybe current `—`. The prompt says profile no ratings, join panel can say "No ratings yet". Use count.
- one combined rating query per applicant, same as current average query count, no worsening.
- existing N+1 remains out of scope.

Section 12 database:
- no migration required.
- current V1-V3. V1 schema.
- Why no migration: all columns/constraints/indexes.
- application handles cross-table business.
- no `updated_at`, no rating expiry.
- Extend tests, not schema migration.
- preserve data.
Maybe if future rating window accepted, then migration would be needed? We choose no. Note no need.

Section 13 backend implementation:
Detailed order:
1. Entity and repo.
2. DTO validation.
3. Match participant queries.
4. service.
5. controller.
6. stats aggregate + service.
7. profile/applicant wiring.
8. tests.
Need mention `SecurityConfiguration` requires no new config because `/api/**` protected and principal annotation.
- no change GlobalException.
- no new dependencies.
- `PlayerRatingRepository` methods:
  - `existsByGameIdAndRaterIdAndRateeId`
  - `findByGameIdAndRaterId` (for current rater's own)
- Could derive query property names.
- GET mapping current ratings and participants.
- `MatchParticipantRepository` method list attended and lookup.
- `UserRepository.findAllById` inherited.
- status check.
- no game lock.

Section 14 frontend:
- API `ratings.ts`.
- rating panel.
- Trigger conditions:
  - game.status === COMPLETED
  - game.myParticipation?.attended===true
  - user loaded.
- On mount call GET, loading.
- list other attended players.
- star/radio buttons accessible (1-5). Need accessibility: buttons with aria-label `Rate X 4 out of 5`, not only stars. Mobile-first.
- selected state then submit? Could tap star directly? For avoiding accidental immutable rating, likely two-step: select score, then "Submit rating" button. This is a key UX/trust decision due immutability. Prompt example after submission. We should design confirmation because no edits. The user said "simple 1–5 control", "after submission Rated 5/5". A two-step control avoids accidental lock. Plan:
  - select 1-5 locally
  - explicit Submit
  - disable during
  - on success replace with `Rated: 5/5`, no edit.
  - Already rated from GET.
- friendly errors mapping.
- no global store.
- profile:
  - create minimal pages because absent. own `/profile` uses `/users/me`, public route `/profile/[id]` uses `/users/id`. Display profile basics + stats. If no ratings "No ratings yet". The user asked only rating, but page is needed. Keep minimal.
  - `Rating: 4.3 ★`, `8 ratings`.
  - match/attendance display. `matchesCompleted` maybe same. The prompt says Matches Played, attendance, rating, ratings. Use.
- Join panel.
- types align.
- no profile edit or redesign.
- direct game detail only. no my-games. Maybe show link / note? The panel appears if direct. Could add a small text no. Not.
- profile routes protected by existing proxy prefix. Bottom nav already link.
- public profile to authenticated users; no anonymous.

Section 15 tests:
Detailed matrix. Need maybe 40 bullets. User asks comprehensive. We can group.
Backend integration test:
- setup helper create users/game/participants/attendance/status.
- rating creation validations.
- nonexistent target.
- GET privacy.
- database.
- stats.
- profile.
- Phase6.
- frontend verification due no test framework.
- commands.
Could mention testcontainers Postgres 16.
- Concurrent test: first success second 409, count exactly 1. Need if calling MockMvc with same authenticated cookie. Or service transaction. Plan only.

Section 16 edge cases table:
Valid/invalid/concurrency and expected result. Include no expiry. We can avoid duplicate tests but user asks explicit. Table:
- completed both true -> 201
- not completed -> 409
- no participant rater/target -> 403
- attendance false/null -> 403
- self -> 400
- duplicate ->409
- score ->400
- game/user 404
- after any time -> allowed
- unauth 401.
- Race one 201 one 409.
Maybe cross-game target 403.
- target user exists not participant.
- Already rated GET.
- no other eligible players -> empty.

Section 17 out of scope list exactly. Include profile edit, match history? The requested out of scope list doesn't include profile editing, but we can add completed-game history, recommendation etc. Don't overexpand. Also no anonymous public access change, no DB triggers, no stored counters. Include all provided.

Section 18 acceptance criteria:
- no migration.
- endpoints.
- rules.
- aggregate.
- profiles.
- UI.
- tests.
- no individual exposures.
- concurrent.

Section 19 risks/tradeoffs:
- indefinite window allows late ratings; no completed_at.
- attendance unmarked permanently ineligible; owner reliability vs coverage.
- immutable mistakes require no correction path; deliberate.
- N+1 join request existing; no regression.
- direct game detail discoverability due no completed list.
- auth-only profiles despite "public"; no security scope.
- old/raw invalid rows possible because DB cannot enforce cross-table; stats query filters.
- no frontend test infra.
- phase numbering mismatch perhaps docs.
- `score` naming.
- average privacy.
- race handling.

Section 20 files likely:
Need list with statuses.
Backend create:
```
backend/.../rating/entity/PlayerRating.java
... repository/PlayerRatingRepository.java
... service/PlayerRatingService.java
... controller/PlayerRatingController.java
... dto/CreatePlayerRatingRequest.java
... dto/PlayerRatingResponse.java
... dto/GameRatingsResponse.java
... dto/RateablePlayerResponse.java
... profile/service/PlayerStatsService.java
```
Modify:
```
MatchParticipantRepository.java
PlayerStatsRepository.java
PlayerStatsResponse.java
ProfileService.java
JoinRequestService.java
```
Maybe UserProfileResponse no change (nested).
No security/error changes.
Tests create/modify.
Frontend create:
```
lib/api/ratings.ts
lib/api/profiles.ts
components/games/GameRatingPanel.tsx
components/profile/PlayerStatsSummary.tsx (or ProfileStats)
app/(app)/profile/page.tsx
app/(app)/profile/[id]/page.tsx
```
Modify types, game detail, JoinRequestsPanel.
Maybe component star. No new dependencies.
`planning_phase8.md` itself would be created in implementation? The output file is plan, but we cannot create. In file list don't include planning? It's created now in requested output but not phase implementation. The final plan file would exist. We can state "Plan artifact: planning_phase8.md (this document)." But no.
- No Flyway migration.
- `UserRepository` no modify (findAllById inherited).

Tests:
```
rating/PlayerRatingIntegrationTest.java
game/JoinRequestApplicantStatsIntegrationTest.java
```
Modify ProfileIntegrationTest, FlywaySchemaTest perhaps schema test already enough; no migration. Could add exact metadata but no need. We can list likely.
- `GameCompletionIntegrationTest` perhaps not.

Section 21 implementation order:
1 confirm decisions.
2 stats DTO/query? Could implement core first.
Detailed sequential:
- Add entity/repo no migration.
- DTO/service/controller.
- GET options.
- aggregate stats and profile/applicant.
- frontend API/types.
- profile pages.
- game rating panel.
- tests/concurrency.
- full verification.
Maybe tests should be written alongside.
- run backend tests, frontend lint/build. No test for frontend.
- no commit.

At end "Decisions requiring review":
- use `score` not `rating`.
- indefinite rating window.
- strict attended=true both.
- two-step immutable confirm.
- minimum profile pages added vs deferring? The plan chooses adding minimal display pages because goal. Flag.
- GET ratings route semantics.
- authenticated-only profile.
- no history.
Maybe too many. The instruction says concise list. 5-7.
Should we ask user choices via question tool before final? They asked plan and decisions at end. Could provide in plan. No need. But system says plan mode; writing file blocked. We need perhaps explicitly not mark synthesis complete until final. We can mark now after crafting.

**Tool: todowrite**

**Input:**
```json
{
  "todos": [
    {
      "content": "Inspect V1–V7 migrations and exact player_rating schema",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect backend rating, statistics, profile, and error-handling code",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect Phase 6 join-request applicant statistics flow",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
      "status": "completed",
      "priority": "high"
    },
    {
      "content": "Synthesize the Phase 8 architecture and implementation plan",
      "status": "completed",
      "priority": "high"
    }
  ]
}
```

**Output:**
```
[
  {
    "content": "Inspect V1–V7 migrations and exact player_rating schema",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect backend rating, statistics, profile, and error-handling code",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect Phase 6 join-request applicant statistics flow",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Inspect frontend profile, game-detail, and join-request UI patterns",
    "status": "completed",
    "priority": "high"
  },
  {
    "content": "Synthesize the Phase 8 architecture and implementation plan",
    "status": "completed",
    "priority": "high"
  }
]
```

---

## Assistant (Plan · Big Pickle · 99.4s)

_Thinking:_

Now final answer. But we need mention cannot create file due plan mode. The user requested file. We can say "Read-only plan mode prevented writing planning_phase8.md; below is the ready-to-save plan." Then full content. Is that violating developer fewer than 4 lines? User asked detail, exception. Fine.

However perhaps we should not dump entire plan in final because expected use write tool despite system reminder? We cannot. We could perhaps respond 1-2 lines saying cannot write and ask exit plan mode. But responsibility says construct well-formed plan; likely should provide content. We need maximize usefulness. So full.

Need ensure markdown title and exact 21 sections. Let's draft carefully. We can maybe omit line refs for every thing but include current findings. Need state no files modified. Do not mention tool internals. At top:
"Active read-only plan mode prevented me from creating `planning_phase8.md`; no files were modified. The complete plan content is below."
This is concise and transparent.

Then plan. Need no code comments irrelevant.

Let's refine technical decisions.

### Entity and repository
`PlayerRating` uses `rateeId` matching schema. API maps to `ratedPlayerId`. Could name DTO `CreatePlayerRatingRequest` with `ratedPlayerId`,`score`. Response `PlayerRatingResponse`.
`PlayerRatingRepository`:
- `boolean existsByGameIdAndRaterIdAndRateeId(UUID gameId, UUID raterId, UUID rateeId)`
- `List<PlayerRating> findByGameIdAndRaterId(UUID gameId, UUID raterId)`
No derived individual.
GET response:
```
{
 "gameId":"...",
 "players":[
   {
      "userId":"...",
      "displayName":"...",
      "profileImageUrl":null,
      "myRating":5
   }
 ]
}
```
The field `myRating` can null. This reveals only caller's own. The user said individual records don't need publicly exposed. We can say authenticated self-only. `ratedPlayerId` maybe instead of userId; use `playerId`. In response players, `userId`.
Would naming `myRating` score clear. Good.
GET endpoint maybe semantics:
- Only returns if eligible; no eligibility status object. This means frontend gets errors. Fine.
- `players` excludes self.
- A user can see display names of attended peers. They already could perhaps not; but reasonable. No individual ratings.

Error status matrix:
- `USER_NOT_FOUND` for target doesn't exist. For rater principal missing `USER_NOT_FOUND` 404.
- `GAME_NOT_COMPLETED` 409 new.
- `RATER_NOT_PARTICIPANT` 403
- `RATER_ATTENDANCE_REQUIRED` 403
- `RATED_PLAYER_NOT_PARTICIPANT` 403
- `RATED_PLAYER_ATTENDANCE_REQUIRED` 403
- `SELF_RATING_NOT_ALLOWED` 400
- `RATING_ALREADY_EXISTS` 409
- `VALIDATION_ERROR` 400
- `MALFORMED_BODY` 400.
- Global auth 401 `UNAUTHORIZED`.
Question: user tries rate someone from another game. If user exists but not participant, 403. Good.
- `USER_NOT_FOUND` might reveal existence; profile GET already can reveal to auth. Fine.
- GET can produce rater errors, game status, game not found; no target.
- no 404 rated participant; use 403.

### Stats query
One subtlety: if we add joins to participant attendance, and rating row invalid due rater no longer attendance, excluded. `match_participant` unique `(game,user)`. Fine.
- The user says "ratings from incomplete games do not exist / do not count." We can test raw invalid. Good.
- `COUNT(*)` returns 0.
- `AVG` uses SMALLINT and returns numeric? PostgreSQL AVG(smallint) returns numeric, Jdbc `getObject` might return BigDecimal, not Double. Current code `rs.getObject("average_rating", Double.class)` maybe works? Agent says current method. We can specify map to Double. Fine.
- Maybe use `AVG(pr.score)::double precision`. Could.
- no rounding.
- Create `PlayerStatsService` to avoid duplicate construction. This service calls two derived queries. `ProfileService` and `JoinRequestService`.
- Could share `RatingStats` record in repository. Fine.
- Count type `long`.
- Update both frontend/backend DTO constructors/tests.

### Profile pages
Potential contradiction "public average" and authentication. We can say visibility to any authenticated user under current app access. The prompt says "Average rating is visible on player profiles". It doesn't demand anonymous. "What should be public" likely they mean publicly displayed. We explicitly do not relax global auth. Good.
- Create own and by-id pages. Do we need profile by-id if no links? Acceptance says public profile. We can create. A minimal `ProfileView`.
- Maybe profile API currently requires authentication for all `/api/**`; frontend route protected. Fine.
- Could add links from rating panel to profiles. This is natural and doesn't duplicate fetch. In plan, make player names links to `/profile/{userId}`. That makes public route reachable. Also join applicant/host could remain. We can add link in rating panel. This is UI integration. Good.
- Should owner applicant name link? It might expose owner-only info but profile accessible. Could not.
- On profile, display position/skill and stats. The stale `UserProfile` type needs exact correction. Good.
- Auth user `/auth/me` no stats to avoid extra query on every app load. Own profile fetches users/me. State context not global for profile data.

### Rating state after GET and POST
- On success update local players item `myRating` and selected reset.
- on 409 refresh GET to reconcile.
- 403/409 game not complete show message, perhaps hide after refresh.
- selection and explicit submit. Disabled after.
- if existing rating, no selection.
- "Rated: score/5".
- use star glyph `★`; user explicitly gave star examples, emoji restriction? The star is symbol requested, not emoji? Developer says only use emojis if explicitly requested. They explicitly show ★, so okay. In plan text, fine.
- accessibility.

### Completed game discoverability
- no `/my-games`. The user said likely completed game detail. Existing detail direct. Plan perhaps should not add rating list/history. The page exists. No completed list. Risk.

### Out of scope
Include no rating comments, moderation, expiration? Expiration not in list but we choose indefinite. Do not list out-of-scope if chosen. Could list "rating editing window/expiration policy" maybe it is decided no expiry. Better in risk.
- no individual public rating history.
- no database triggers/cross-table constraints.
- no caching/Redis.
- no profile editing.
- no match history. Good.

### Test details
Use `FlywaySchemaTest`:
- already tests constraints. No migration. Could add tests for `ratingCount`? Behavior.
- Database integrity:
  - direct JPA maybe bypass validation? JPA doesn't validate score annotations unless `@Min`, so direct insert invalid. The entity could annotate `@Min/@Max`. To test DB, use `JdbcTemplate` insert invalid score and expect DataIntegrityViolation. But not necessary since schema test already checks constraint names. User asks database integrity. We can state direct SQL verifies score, self, unique.
- concurrency: use actual Postgres, not mocks. Need unique handling. Could be a separate `PlayerRatingConcurrencyIntegrationTest`.
- no test for global handler changes because none.
- test front type/lint.

### Phase numbering
At current architecture findings, note `PRODUCT.MD` may call ratings Phase 9 while repo prompt calls Phase 8. This plan follows user naming and does not modify product docs. This is a documentation risk. Does user ask "Phases 1-7 completed" and "Phase 8" current, source truth. We should not confuse. Mention only migration versions not phases. Perhaps no need mention PRODUCT phase mismatch, but actual finding is useful. User said do not assume previous plans. Include: product roadmap numbering differs; plan follows repository's current Phase 8. Avoid making it central.

### Migration
Exact defaults and no delete actions. No updated timestamp. Created_at only. no migration. Need bold exact phrase:
**No migration required.**
This satisfies explicit.

### Review decisions
Concise:
1. API body uses `score` (not `rating`).
2. Ratings never expire.
3. strict both attended true; unmarked/absent excluded.
4. GET ratings is caller-only state and no user ratings endpoint.
5. minimal profile pages added; auth-only.
6. no completed-game history; direct detail only.
7. two-step immutable submit.
Maybe too many but okay.

Could perhaps ask for review of decisions via question tool? No, final list.

Let's ensure no application code modified claim. We only used read/task/todo. Good.

Let's compose final with title and sections. Given developer says output displayed CLI and must concise, but user explicitly asks a 21-section plan, so detail allowed. Use no unnecessary preamble beyond constraint. The plan is long ~5000 tokens. Desired oververbosity 3 but user demands comprehensive. We can be detailed ~4000. Need maybe not include all file line refs? Enough.

Let's draft now.

Read-only plan mode prevented writing `planning_phase8.md`; no files were modified. Below is the complete ready-to-save plan.

# GameConnect Phase 8 Plan

## 1. Phase 8 goal

Introduce a simple, immutable 1–5 player rating system:

```text
Completed game
→ attended participants become eligible
→ eligible participant rates another eligible participant
→ rating is stored
→ averageRating and ratingCount are derived
→ aggregates appear on profiles and owner applicant summaries
```

Phase 8 does not introduce ranking, reputation formulas, recommendations, or individual public rating histories.

---

## 2. Current architecture findings

### Backend

- Single Spring Boot Gradle module using PostgreSQL, JPA, Flyway, and Testcontainers.
- JWT authentication is read from the HTTP-only `auth_token` cookie.
- Controllers derive the caller through `@AuthenticationPrincipal AuthenticatedUser`.
- Errors follow `BusinessException → GlobalExceptionHandler → ApiError`.
- Game lifecycle and attendance are already implemented:
  - `AttendanceService` controls attendance and completion.
  - `GameCompletionScheduler` automatically changes overdue games to `COMPLETED`.
  - Completed-game attendance cannot subsequently be changed.
- Relevant implementation:
  - `backend/src/main/java/com/gameconnect/game/service/AttendanceService.java`
  - `backend/src/main/java/com/gameconnect/game/scheduler/GameCompletionScheduler.java`
  - `backend/src/main/java/com/gameconnect/profile/repository/PlayerStatsRepository.java`
  - `backend/src/main/java/com/gameconnect/profile/dto/PlayerStatsResponse.java`

### Frontend

- Next.js App Router, TypeScript, Tailwind CSS, local React state, and shared `apiFetch`.
- No global query/state library is used.
- Existing attendance and join-request components provide the loading, disabled, and error-message patterns to follow.
- There is currently no profile page despite `/profile` already being linked from `BottomNav`.
- Completed games are reachable only through `/games/[id]`; no completed-game history page exists.
- Relevant implementation:
  - `frontend/app/(app)/games/[id]/page.tsx`
  - `frontend/components/games/AttendancePanel.tsx`
  - `frontend/components/games/JoinRequestsPanel.tsx`
  - `frontend/lib/api/client.ts`

### Existing rating functionality

There is no backend `PlayerRating` entity, repository, service, controller, or rating-write DTO.

Existing rating scaffolding is limited to:

- `player_rating` database table.
- `PlayerStatsRepository.findAverageRating`.
- `PlayerStatsResponse.averageRating`.
- Frontend `PlayerStats.averageRating`.
- An unused frontend `PlayerRating` interface.
- Applicant-panel display of `averageRating`.

No duplicate backend rating structure should be created.

### Phase/migration numbering

The repository contains only Flyway V1–V3. There are no V4–V7 migration files; product phases and migration versions do not correspond. The existing V1 table remains the current rating schema.

---

## 3. Existing `player_rating` schema analysis

Defined in `backend/src/main/resources/db/migration/V1__init.sql`.

| Column | Type | Rules |
|---|---|---|
| `id` | `UUID` | Primary key, default `gen_random_uuid()` |
| `game_id` | `UUID` | Not null, FK to `game(id)` |
| `rater_id` | `UUID` | Not null, FK to `app_user(id)` |
| `ratee_id` | `UUID` | Not null, FK to `app_user(id)` |
| `score` | `SMALLINT` | Not null, constrained to 1–5 |
| `created_at` | `TIMESTAMPTZ` | Not null, default `now()` |

Constraints and indexes:

- Primary key: `player_rating_pkey`.
- Foreign keys use PostgreSQL’s default `NO ACTION` delete/update behavior.
- `ck_rating_score`: `score BETWEEN 1 AND 5`.
- `ck_rating_self`: `rater_id <> ratee_id`.
- `uq_rating`: unique `(game_id, rater_id, ratee_id)`.
- `idx_player_rating_ratee`: index on `ratee_id`.

There is no `updated_at`, consistent with immutable ratings.

The schema does not enforce game status or participation attendance because those rules require cross-table validation. They belong in the service layer.

### Migration decision

**No migration required.**

The existing table already provides the required model, score constraint, self-rating protection, per-game uniqueness, creation timestamp, and aggregate lookup index.

---

## 4. Rating domain model

Model one `PlayerRating` entity as:

```text
game
├── rater user
├── rated user
├── integer score
└── createdAt
```

The entity should map the existing columns without introducing relationships or duplicated tables.

Recommended repository operations:

```text
existsByGameIdAndRaterIdAndRateeId
findByGameIdAndRaterId
```

The second method is scoped to the authenticated caller and is used only to populate that caller’s rating UI.

Ratings are immutable:

- No update method.
- No replace or delete endpoint.
- No `updated_at`.
- UI confirmation should occur before submission because accidental changes cannot be corrected in this phase.

---

## 5. Rating eligibility rules

A rating is accepted only when all conditions hold:

1. The caller is authenticated.
2. The game exists.
3. The game status is `COMPLETED`.
4. The caller has a `match_participant` row for that game.
5. The caller’s `attended` value is exactly `true`.
6. The rated user exists.
7. The rated user has a participant row for the same game.
8. The rated user’s `attended` value is exactly `true`.
9. Rater and rated user are different people.
10. The caller has not already rated that user for that game.
11. The score is between 1 and 5 inclusive.

### Attendance decisions

| Attendance state | Can give ratings | Can receive ratings |
|---|---:|---:|
| `true` | Yes | Yes |
| `false` | No | No |
| `null` / unmarked | No | No |
| No participant row | No | No |

The game owner is not automatically eligible. The owner must also have `attended = true`.

### Time-window decision

A completed game remains rating-eligible indefinitely.

Rationale:

- No rating deadline or expiry was found in the current schema or product implementation.
- The game table has no immutable `completed_at` value.
- Grace-period configuration may change, so deriving a historical deadline later could be unreliable.
- A configurable rating window would introduce additional product and schema decisions beyond the MVP.

This is a conscious tradeoff favoring simplicity over limiting very late ratings.

---

## 6. Rating validation rules

The request will use `score`, matching the existing schema and frontend placeholder:

```json
{
  "ratedPlayerId": "UUID",
  "score": 5
}
```

Using `rating` as the JSON property would introduce a needless difference from `player_rating.score`.

DTO validation:

```text
ratedPlayerId: required UUID
score: required integer
1 <= score <= 5
```

Recommended annotations:

- `@NotNull` on both fields.
- `@Min(1)` and `@Max(5)` on `score`.
- Use `Integer` in the request so a missing score is distinguished from an invalid numeric value.

The UI may present labels:

```text
1 = Very poor
2 = Poor
3 = Average
4 = Good
5 = Excellent
```

These labels remain frontend presentation concerns. The backend stores only the integer.

The database check remains the final protection against invalid scores and self-ratings.

---

## 7. API design

### 7.1 Submit a rating

```http
POST /api/games/{gameId}/ratings
```

Authentication:

- Required.
- Uses the existing JWT cookie.
- `raterId` is always derived from `AuthenticatedUser`; it is never accepted from the request.

Request:

```json
{
  "ratedPlayerId": "f52f8142-5d4a-4a1c-bec6-0ad508a94af3",
  "score": 5
}
```

Success response: `201 Created`

```json
{
  "id": "5bc725bb-0a94-49c8-a30a-778501c89fd6",
  "gameId": "936f26f9-11dd-43b2-a7ec-23bfd871d3d6",
  "ratedPlayerId": "f52f8142-5d4a-4a1c-bec6-0ad508a94af3",
  "score": 5,
  "createdAt": "2026-09-25T12:00:00Z"
}
```

The response should not expose `raterId`.

Errors:

| HTTP | Code | Condition |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Missing/invalid UUID or score outside 1–5 |
| 400 | `MALFORMED_BODY` | Malformed JSON |
| 400 | `SELF_RATING_NOT_ALLOWED` | Caller rates themself |
| 401 | `UNAUTHORIZED` | Missing or invalid authentication |
| 403 | `RATER_NOT_PARTICIPANT` | Caller did not participate |
| 403 | `RATER_ATTENDANCE_REQUIRED` | Caller was absent or unmarked |
| 403 | `RATED_PLAYER_NOT_PARTICIPANT` | Rated user belongs to another game or did not participate |
| 403 | `RATED_PLAYER_ATTENDANCE_REQUIRED` | Rated user was absent or unmarked |
| 404 | `GAME_NOT_FOUND` | Game does not exist |
| 404 | `USER_NOT_FOUND` | Rated user does not exist |
| 409 | `GAME_NOT_COMPLETED` | Game is not completed |
| 409 | `RATING_ALREADY_EXISTS` | Caller already rated this user for this game |

### 7.2 Get the caller’s rating state for a game

```http
GET /api/games/{gameId}/ratings
```

This endpoint is necessary because:

- The current participant roster endpoint is owner-only.
- Game detail exposes only `myParticipation`.
- The frontend otherwise cannot discover rateable peers or restore already-submitted ratings.

It is not a general list of ratings.

Success response:

```json
{
  "gameId": "936f26f9-11dd-43b2-a7ec-23bfd871d3d6",
  "players": [
    {
      "userId": "f52f8142-5d4a-4a1c-bec6-0ad508a94af3",
      "displayName": "Arijit Das",
      "profileImageUrl": null,
      "myRating": 5
    },
    {
      "userId": "50ce0672-0763-4804-b1c8-1c20a5cb2714",
      "displayName": "Rahul",
      "profileImageUrl": null,
      "myRating": null
    }
  ]
}
```

Rules:

- Caller must satisfy the same game and attendance eligibility as submission.
- Results include only other participants with `attended = true`.
- `myRating` exposes only the authenticated caller’s rating.
- Ratings submitted by other users are never returned.
- The list may be empty.
- No pagination is needed for MVP-sized game rosters.

Errors use the applicable game/rater eligibility codes from the submission endpoint.

### 7.3 APIs deliberately not added

Do not add:

```text
GET /api/users/{userId}/ratings
PUT/PATCH /api/games/{gameId}/ratings/{ratingId}
DELETE /api/games/{gameId}/ratings/{ratingId}
```

Profile APIs already expose the useful aggregate. Individual rating histories are not required and should remain private.

---

## 8. Authorization and security

- Global `/api/**` security already requires authentication.
- `@AuthenticationPrincipal` supplies the caller.
- Never accept `raterId` in the request body, path, or query.
- Perform every eligibility check on the server.
- Reject requests based on caller attendance, not frontend visibility.
- Keep owner-only participant management separate from participant rating.
- Do not expose individual rating records, rater IDs, or other users’ ratings.
- Do not change `/api/auth/me`; it remains the lightweight authentication identity endpoint.
- Do not make profile APIs anonymous in this phase. Rating aggregates are visible to authenticated users under the current application security model.
- Raise domain failures through `BusinessException` so `GlobalExceptionHandler` produces the existing `ApiError` structure.

---

## 9. Transaction and concurrency design

`PlayerRatingService.submitRating` should be transactional.

Flow:

1. Load and validate game/participants.
2. Check for an existing rating.
3. Persist and flush the new `PlayerRating`.
4. Translate only a violation of `uq_rating` into `RATING_ALREADY_EXISTS`.
5. Re-throw unrelated integrity failures.

Expected concurrent behavior:

```text
First request  → 201 Created
Concurrent duplicate → 409 RATING_ALREADY_EXISTS
Database rows → exactly one
```

Use `saveAndFlush` or an equivalent immediate flush so the unique violation occurs within the service transaction and can be translated cleanly.

No pessimistic game or rating lock is needed because:

- Completed is a terminal game state.
- The unique constraint is the correct concurrency boundary.
- The pre-check is for a friendly fast path, not correctness.

---

## 10. Statistics and profile integration

Extend the existing `PlayerStatsResponse`:

```text
matchesPlayed
matchesCompleted
attendanceRate
averageRating
ratingCount
```

Types:

```text
averageRating: Double / number | null
ratingCount: long / number
```

No-rating result:

```json
{
  "averageRating": null,
  "ratingCount": 0
}
```

Replace the average-only repository method with one aggregate returning both values:

```sql
SELECT
    AVG(pr.score) AS average_rating,
    COUNT(*) AS rating_count
FROM player_rating pr
JOIN game g
  ON g.id = pr.game_id
JOIN match_participant rater
  ON rater.game_id = pr.game_id
 AND rater.user_id = pr.rater_id
 AND rater.attended = TRUE
JOIN match_participant ratee
  ON ratee.game_id = pr.game_id
 AND ratee.user_id = pr.ratee_id
 AND ratee.attended = TRUE
WHERE pr.ratee_id = ?
  AND g.status = 'COMPLETED'
```

This provides defense in depth if invalid rows were inserted outside the service.

Rules:

- Keep raw numeric precision in the API.
- Format to one decimal only in the frontend.
- Do not add rating columns to `app_user`.
- Do not maintain counters or cached averages.
- Do not round inside SQL.

Create a shared `PlayerStatsService` that assembles `PlayerStatsResponse` from participation and rating aggregates. Reuse it in:

- `ProfileService`.
- `JoinRequestService`.

This avoids duplicating rating-count assembly and ensures both consumers use identical query semantics.

Existing profile responses then require no new endpoint:

```text
GET /api/users/me
GET /api/users/{id}
```

Both return the updated nested `stats` object.

---

## 11. Phase 6 integration

Keep the current `ApplicantSummary.stats: PlayerStatsResponse` structure.

Do not add duplicate top-level fields such as:

```text
ApplicantSummary.averageRating
ApplicantSummary.ratingCount
```

The combined repository aggregate preserves the current one-rating-query-per-applicant behavior. It does not add an extra average/count query.

Frontend display:

```text
Rating: 4.3 ★ · 8 ratings
```

When unrated:

```text
Rating: No ratings yet
```

The existing join-request API and applicant-summary JSON shape remain unchanged except for the additive `stats.ratingCount` field.

The existing applicant-list N+1 behavior should not be redesigned in Phase 8.

---

## 12. Database and migration changes

**No migration required.**

Reasons:

- All required columns exist.
- Score validation exists.
- Self-rating protection exists.
- Required uniqueness exists.
- Aggregate lookup index exists.
- `created_at` supports auditability.
- No edit timestamp is needed for immutable records.
- Completed/attendance eligibility is application-level business logic.

Do not recreate or alter `player_rating`.

If rating expiry is introduced later, the lack of a stable `completed_at` must be addressed as a separate schema/product decision.

---

## 13. Backend implementation plan

1. Add `PlayerRating` mapped to the existing table.
2. Add `PlayerRatingRepository` with:
   - duplicate existence lookup.
   - caller/game rating lookup.
3. Add game-scoped participant lookup methods to `MatchParticipantRepository`.
4. Add request/response DTOs with validation.
5. Implement `PlayerRatingService`:
   - shared eligibility validation.
   - GET option assembly.
   - transactional submission.
   - duplicate translation.
6. Add `PlayerRatingController`.
7. Add combined rating aggregation to `PlayerStatsRepository`.
8. Add shared `PlayerStatsService`.
9. Update `PlayerStatsResponse`.
10. Replace duplicated stats construction in:
    - `ProfileService`.
    - `JoinRequestService`.
11. Update existing profile and applicant tests.
12. Add rating integration and concurrency tests.
13. Do not add dependencies or alter authentication/security configuration.

---

## 14. Frontend implementation plan

### API and types

Create `frontend/lib/api/ratings.ts` using `apiFetch`:

```text
getMyGameRatings(gameId)
submitGameRating(gameId, ratedPlayerId, score)
```

Update `frontend/lib/types.ts`:

- Add `ratingCount` to `PlayerStats`.
- Correct `UserProfile` to match the backend profile response.
- Replace the unused raw `PlayerRating` placeholder with the actual rating response/state types.
- Keep `/auth/me` types separate from profile types.

### Completed-game rating panel

Create a mobile-first `GameRatingPanel` and render it from `/games/[id]` only when:

```text
game.status === "COMPLETED"
game.myParticipation?.attended === true
authenticated user is available
```

Behavior:

- Fetch caller-specific rating state.
- Display loading, error, and empty states.
- Exclude self and all absent/unmarked participants.
- Let the caller select 1–5 stars.
- Require an explicit submit action because ratings are immutable.
- Disable controls while submitting.
- On success, display `Rated: 5/5`.
- Already-rated players display their existing score without an edit control.
- On `RATING_ALREADY_EXISTS`, refresh the GET state and show already-rated feedback.
- Map business error codes to friendly messages.
- Use accessible labels such as “Rate Arijit Das 4 out of 5”.
- Link displayed player names to `/profile/{userId}` if the profile route is added.

No global state library is needed.

### Profile pages

Because no profile page currently exists, add minimal display pages:

```text
app/(app)/profile/page.tsx
app/(app)/profile/[id]/page.tsx
```

Use:

```text
GET /api/users/me
GET /api/users/{id}
```

Display:

```text
Matches Played: 12
Attendance: 92%
Rating: 4.3 ★
8 ratings
```

No ratings:

```text
Rating: No ratings yet
```

The existing BottomNav already links to `/profile`, and the existing proxy prefix protects both profile routes.

This should remain a minimal stats/profile presentation, not a profile redesign or edit form.

### Join-request panel

Update `JoinRequestsPanel` to use:

```text
applicant.stats.averageRating
applicant.stats.ratingCount
```

Do not add a second frontend ratings request.

---

## 15. Test plan

Use real PostgreSQL through the existing Testcontainers setup.

### Rating creation

- Attended participant can rate another attended participant.
- Unauthenticated request returns 401.
- Caller who did not participate is rejected.
- `attended = false` caller is rejected.
- `attended = null` caller is rejected.
- Rated player who did not participate is rejected.
- Rated player belongs to another game and is rejected.
- `attended = false` rated player is rejected.
- `attended = null` rated player is rejected.
- Self-rating is rejected.
- Incomplete game is rejected.
- Missing game returns 404.
- Missing rated user returns 404.
- Score `0` returns 400.
- Score `6` returns 400.
- Missing score returns 400.
- Duplicate sequential rating returns 409.
- A rating submitted a long time after completion remains allowed.

### Eligibility listing

- Eligible caller receives other attended participants.
- Self is excluded.
- Absent and unmarked participants are excluded.
- Existing caller ratings are returned as `myRating`.
- Other users’ ratings and rater identities are not returned.
- Ineligible caller cannot obtain the rateable list.
- Game with no other attended participants returns an empty list.

### Database integrity

- Direct invalid score violates `ck_rating_score`.
- Direct self-rating violates `ck_rating_self`.
- Duplicate `(game, rater, ratee)` violates `uq_rating`.
- Two simultaneous requests create exactly one row.
- Concurrent responses are one 201 and one 409.

### Statistics

- Average is calculated correctly across raters.
- Rating count is correct.
- No ratings produces null average and zero count.
- Ratings from multiple games contribute correctly.
- Raw incomplete-game rows do not count.
- Raw rows lacking attended rater/ratee participation do not count.
- Existing participation statistics remain unchanged.

### Profile APIs

- `GET /api/users/me` includes `averageRating` and `ratingCount`.
- `GET /api/users/{id}` includes both fields.
- No-rating profile returns null and zero.
- `GET /api/auth/me` remains lightweight and unchanged.

### Phase 6 integration

- Owner applicant summary includes updated nested stats.
- Nonzero average and count are correct.
- Unrated applicant returns null and zero.
- No duplicate top-level rating fields are introduced.

### Frontend verification

The repository has no frontend test framework. Do not add one solely for Phase 8.

Verify with:

```text
npm run lint
npm run build
```

Add focused manual checks for:

- loading state.
- empty state.
- star selection.
- submit disabled state.
- success state.
- duplicate refresh.
- friendly API errors.
- profile and applicant formatting.

Backend verification:

```text
.\gradlew.bat test
```

---

## 16. Edge cases

| Scenario | Result |
|---|---|
| Completed game, both attended, first rating | 201 |
| Game not completed | 409 `GAME_NOT_COMPLETED` |
| Rater not a participant | 403 `RATER_NOT_PARTICIPANT` |
| Rater marked absent | 403 `RATER_ATTENDANCE_REQUIRED` |
| Rater unmarked | 403 `RATER_ATTENDANCE_REQUIRED` |
| Rated player not in this game | 403 `RATED_PLAYER_NOT_PARTICIPANT` |
| Rated player marked absent | 403 `RATED_PLAYER_ATTENDANCE_REQUIRED` |
| Rated player unmarked | 403 `RATED_PLAYER_ATTENDANCE_REQUIRED` |
| Self-rating | 400 `SELF_RATING_NOT_ALLOWED` |
| Duplicate rating | 409 `RATING_ALREADY_EXISTS` |
| Score below 1 or above 5 | 400 `VALIDATION_ERROR` |
| Missing game | 404 `GAME_NOT_FOUND` |
| Missing player | 404 `USER_NOT_FOUND` |
| Rating long after completion | Allowed |
| Simultaneous duplicate requests | One 201, one 409, one row |
| No other eligible players | Empty rating list |
| Profile has no ratings | Null average, zero count |

---

## 17. Out of scope

- Elo
- Glicko
- Skill-rating algorithms
- Reputation formulas
- Rankings
- Leaderboards
- Best-player calculations
- Matchmaking
- Recommendations
- AI recommendations
- Rating comments or reviews
- Moderation
- Appeals
- Notifications
- Chat
- Payments
- Tournaments
- Teams
- Subscriptions
- Redis
- WebSockets
- Microservices
- Public individual rating histories
- Rating editing or deletion
- Stored rating counters
- Database triggers for cross-table eligibility
- Completed-game history or `/my-games` redesign
- Profile editing
- Applicant-list N+1 optimization

---

## 18. Acceptance criteria

- No new rating table or migration is introduced.
- Only attended participants of a completed game can give ratings.
- Only attended participants of the same game can receive ratings.
- Scores outside 1–5 are rejected.
- Self-rating and duplicate ratings are rejected.
- Concurrent duplicate submissions produce one row and a controlled 409.
- Ratings cannot be changed through the API.
- A caller can retrieve only their own rating state for a game.
- Individual ratings from other users are not exposed.
- `PlayerStatsResponse` returns accurate average and count.
- No ratings returns null average and zero count.
- `/api/users/me` and `/api/users/{id}` expose the aggregate.
- Phase 6 applicant summaries reuse the same nested statistics.
- Completed-game detail provides an accessible, mobile-first rating control.
- Profiles show average and count or a clear no-ratings state.
- All backend integration tests pass against PostgreSQL.
- Frontend lint and production build pass.
- No unnecessary dependencies, global state, or unrelated product work are introduced.

---

## 19. Risks and tradeoffs

- **No expiry:** late ratings remain possible indefinitely. Adding a window later requires stable completion-time semantics.
- **Strict attendance:** absent and unmarked players permanently lose rating access. This favors reliability over maximum participation.
- **Immutable mistakes:** users cannot correct an accidental rating. Explicit submission confirmation reduces accidental writes.
- **No completed-game history:** users may need a direct game link to reach the rating panel. History remains a separate phase.
- **Cross-table integrity:** the database cannot independently prove participation or completion. Service checks and defensive aggregate joins are required.
- **Existing N+1 behavior:** applicant statistics currently issue several queries per applicant. Phase 8 preserves this and does not worsen rating-query count.
- **Frontend test gap:** rating UI behavior must rely on lint/build plus manual verification until frontend test infrastructure exists.
- **Profile terminology:** the requested UI calls `/api/users/{id}` public, but current global security makes it authenticated-only. Phase 8 should not silently change that security boundary.
- **Phase numbering drift:** product documentation and repository plans use different phase numbers. This plan follows the requested rating phase without changing roadmap documents.

---

## 20. Files likely to be created or modified

### Backend files likely to be created

```text
backend/src/main/java/com/gameconnect/rating/entity/PlayerRating.java
backend/src/main/java/com/gameconnect/rating/repository/PlayerRatingRepository.java
backend/src/main/java/com/gameconnect/rating/service/PlayerRatingService.java
backend/src/main/java/com/gameconnect/rating/controller/PlayerRatingController.java
backend/src/main/java/com/gameconnect/rating/dto/CreatePlayerRatingRequest.java
backend/src/main/java/com/gameconnect/rating/dto/PlayerRatingResponse.java
backend/src/main/java/com/gameconnect/rating/dto/GameRatingsResponse.java
backend/src/main/java/com/gameconnect/rating/dto/RateablePlayerResponse.java
backend/src/main/java/com/gameconnect/profile/service/PlayerStatsService.java
backend/src/test/java/com/gameconnect/rating/PlayerRatingIntegrationTest.java
backend/src/test/java/com/gameconnect/rating/PlayerRatingConcurrencyIntegrationTest.java
```

### Backend files likely to be modified

```text
backend/src/main/java/com/gameconnect/game/repository/MatchParticipantRepository.java
backend/src/main/java/com/gameconnect/profile/repository/PlayerStatsRepository.java
backend/src/main/java/com/gameconnect/profile/dto/PlayerStatsResponse.java
backend/src/main/java/com/gameconnect/profile/service/ProfileService.java
backend/src/main/java/com/gameconnect/game/service/JoinRequestService.java
backend/src/test/java/com/gameconnect/profile/ProfileIntegrationTest.java
backend/src/test/java/com/gameconnect/FlywaySchemaTest.java
```

`UserProfileResponse` should not need structural changes because it embeds `PlayerStatsResponse`.

### Frontend files likely to be created

```text
frontend/lib/api/ratings.ts
frontend/lib/api/profiles.ts
frontend/components/games/GameRatingPanel.tsx
frontend/components/profile/PlayerStatsSummary.tsx
frontend/app/(app)/profile/page.tsx
frontend/app/(app)/profile/[id]/page.tsx
```

### Frontend files likely to be modified

```text
frontend/lib/types.ts
frontend/app/(app)/games/[id]/page.tsx
frontend/components/games/JoinRequestsPanel.tsx
```

No new dependencies or migration files are expected.

---

## 21. Implementation order

1. Confirm the review decisions listed below.
2. Add `PlayerRating` and `PlayerRatingRepository`.
3. Add validation and response DTOs.
4. Add participant eligibility queries.
5. Implement transactional rating submission.
6. Implement caller-specific game rating listing.
7. Add combined average/count statistics aggregation.
8. Add shared `PlayerStatsService`.
9. Wire updated statistics into profile and applicant responses.
10. Add backend rating, profile, applicant, and concurrency tests.
11. Add frontend API methods and correct shared types.
12. Add minimal profile pages and statistics display.
13. Add the completed-game rating panel.
14. Update applicant rating/count rendering.
15. Run backend tests, frontend lint, and frontend production build.
16. Perform focused manual UX and authorization verification.

## Decisions requiring review

1. Use JSON property `score`, not `rating`, to match the existing schema.
2. Ratings never expire once the game is completed and both players attended.
3. Both `attended = false` and `attended = null` are permanently ineligible.
4. Ratings are immutable and require explicit UI confirmation.
5. `GET /api/games/{gameId}/ratings` returns only the caller’s private rating state, not public individual ratings.
6. Add minimal `/profile` and `/profile/[id]` pages because no profile frontend currently exists.
7. Keep completed-game history and `/my-games` out of Phase 8.

---

