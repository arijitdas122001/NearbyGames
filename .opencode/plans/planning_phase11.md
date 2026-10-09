# Phase 11 — GameConnect MVP Hardening & End-to-End Audit

Status: PLAN ONLY. No code, no migrations, no features were changed while producing
this document. Every finding below was verified directly against the repository
(sources cited with `file:line`). Planning docs for Phases 2–10 live in this same
folder; where they conflict with the code, the code wins.

---

## 1. Executive summary

Phases 1–10 delivered a working, well-structured MVP: authorization is default-deny,
identity always comes from the JWT principal (no client-supplied user IDs are
trusted anywhere), the accept path is correctly serialized under a pessimistic
game-row lock, ratings are belt-and-braces guarded by both service validation and
DB constraints, My Games and discovery are cleanly separated, and the DTO contract
between frontend and backend matches field-for-field.

The audit found **no authorization bypass, no IDOR, and no P0 security flaw in the
backend**. However, the integrated product has **one P0 blocking bug, five P1
correctness/integrity gaps, and a cluster of P2 UX/ops issues**:

| # | Sev | One-line finding |
|---|-----|------------------|
| 1 | **P0** | Ratings are unreachable from the UI: `ratings.ts` double-prefixes `/api`, so every rating GET/POST 404s (`frontend/lib/api/ratings.ts:5-6` + `client.ts:1,21`) |
| 2 | **P1** | `IN_PROGRESS` is never set anywhere; join/accept only check `status == OPEN`, so players can be requested/accepted into a game that has already started or finished (until `endTime+6h`) — corrupting attendance, stats and ratings |
| 3 | **P1** | A REJECTED applicant who retries hits `UNIQUE(game_id,user_id)` at commit, outside the `try` → deterministic **500**, and the row can never be cleared → player permanently locked out of that game |
| 4 | **P1** | Discovery pagination has no `id` tiebreaker (`GameRepository.java:43`) → rows can repeat/vanish across pages when `start_time`s tie |
| 5 | **P1** | Committed default HS256 JWT signing secret (`application.yml:20`); no production config docs |
| 6 | **P1** | Zero tests for join-request business rules, the REJECT path, the `OPEN→FULL` transition, and **zero concurrency tests** — the PRODUCT.MD rules 1–6 are unverified |

Everything else is P2/P3 and is explicitly separated into MUST FIX vs CAN DEFER
(§17–18). **No migration is required in Phase 11** — the chosen fixes (start-time
transition, re-request reset, pagination tiebreaker) are all code-only.

---

## 2. Current architecture audit

### 2.1 Structure (verified)

```
backend/   Spring Boot 3 / Java 21 modular monolith, package-by-feature
  auth/        register, login, me, logout        (cookie `auth_token`, httpOnly, SameSite=Lax)
  game/        games CRUD+discovery, join requests, attendance, My Games, completion scheduler
  rating/      per-game player ratings
  profile/     own/other profile + derived stats (native SQL, no stats table)
  security/    JwtService (HS256), JwtAuthenticationFilter, SecurityConfiguration
  common/      BusinessException(HttpStatus+code), GlobalExceptionHandler, ApiError
  db/migration V1 init, V2 position, V3 discovery index
frontend/  Next.js 16 (App Router) + TS + Tailwind, mobile-first
  proxy.ts     route guard (cookie presence) for /games, /create-game, /my-games, /profile
  lib/api/*    thin fetch client; base "/api" prepended in client.ts:1,21
  app/(app)/*  games, games/[id], create-game, my-games, profile
```

### 2.2 Endpoint inventory & principal source (verified)

| Endpoint | Controller | Principal | Tx boundary |
|---|---|---|---|
| POST `/api/auth/register`/`login` | AuthController:54,63 | n/a (public) | **none** (check-then-insert not atomic) |
| GET `/api/auth/me`, POST `/logout` | AuthController:72,93 | JWT | none |
| POST `/api/games` | GameController:43 | JWT owner | `@Transactional` GameService:60 |
| GET `/api/games` (discovery) | GameController:51 | JWT (unused) | readOnly GameService:153 |
| GET `/api/games/{id}` | GameController:66 | JWT viewer | readOnly GameService:199 |
| GET `/api/games/my` | GameController:78 | JWT | readOnly MyGamesService:49 |
| POST `/api/games/{id}/join-requests` | JoinRequestController:35 | JWT requester | `@Transactional`:52 |
| GET `.../join-requests/me` | :43 | JWT (own row only) | readOnly:114 |
| GET `.../join-requests` (owner list) | :50 | JWT owner | readOnly:125 |
| PATCH `.../join-requests/{rid}` | :57 | JWT owner | `@Transactional`:134, game `PESSIMISTIC_WRITE`:137 |
| GET `.../participants` (roster) | AttendanceController:32 | JWT owner | readOnly:46 |
| PATCH `.../participants/{pid}/attendance` | :39 | JWT owner | `@Transactional`:55, game lock:58 |
| GET `/api/games/{id}/ratings[/eligible]` | PlayerRatingController:33 | JWT rater | readOnly:50 / `@Transactional`:83 |
| POST `/api/games/{id}/ratings` | :40 | JWT rater | `@Transactional`:83 |
| GET `/api/users/me`, GET `/api/users/{id}`, PATCH `/api/users/me` | ProfileController:30,36,41 | JWT (path `id` is read-only) | **none** (ProfileService has no `@Transactional`) |

No endpoint reads a user ID to act on from a query param or body
(grep for `userId()/playerId()/ownerId()` in DTOs = 0). `open-in-view: false`
(`application.yml:9`). Security is default-deny
(`SecurityConfiguration.java:39-44`): only register/login/`GET /api/health` are
public. No `@PreAuthorize` anywhere — ownership checks are hand-rolled
(`verifyOwner` at JoinRequestService:223, AttendanceService:135) and covered by
tests for game detail, roster and attendance.

### 2.3 Transactions & locks (verified)

- All game/join/attendance/rating/MyGames service methods are `@Transactional`
  (write) or `@Transactional(readOnly=true)` (read). **ProfileService
  (ProfileService.java:27,33) and AuthService.register (AuthService.java:26) have
  none** — each repository call runs in its own transaction; the profile update is
  a detached read-modify-write with no `@Version` anywhere in the codebase.
- Two pessimistic locks exist, both `PESSIMISTIC_WRITE` on the `game` row
  (`GameRepository.java:26-31`), taken at JoinRequestService:137 (decide),
  AttendanceService:58 (attendance), AttendanceService:82 (completion).
  **Lock order is always game-row-first; no deadlock cycle exists.** This is
  correct and should not be redesigned (see §7).

---

## 3. End-to-end flow audit

Walked against the actual code, step by step:

| Step | Works? | Evidence / issue |
|---|---|---|
| A registers | ✅ | AuthController:54; 200 + cookie (200 not 201 — cosmetic, P3) |
| A completes profile | ✅ | PATCH `/users/me` self-only, body carries no userId (ProfileController:41) |
| A creates game | ✅ | Owner participant row written atomically (GameService:91-96); lat/lng hardcoded `0,0` by the form (create-game/page.tsx:66-67) — P3, no location feature in MVP |
| B registers/profile | ✅ | same |
| B discovers game | ✅ | Discovery = OPEN + `startTime >= now` + filters (GameRepository:33-44) |
| B requests to join | ⚠️ | Works for OPEN games, **but also succeeds after kick-off** because status stays OPEN until `endTime+6h` (no time guard at JoinRequestService:69-74) → **P1-2** |
| A accepts B | ⚠️ | Correctly lock-serialized (JoinRequestService:137→190), **but no time guard** at :176-181 → A can accept B into a game that already happened → **P1-2** |
| Game starts | ❌ | **No transition occurs.** Only writers of `Game.status` in `src/main` are `GameService:85` (OPEN), `JoinRequestService:209` (FULL), `AttendanceService:93` (COMPLETED). `IN_PROGRESS` is never set → **P1-2** |
| Attendance recorded | ✅ (with caveats) | Owner-only, window = `startTime .. endTime+6h` (AttendanceService:96-125); UI copy states marking is open "during the game and for 6 hours after" — intentional simplification. Owner may mark own row (no self-mark guard) — accepted |
| Game completes | ✅ | Scheduler every 60 s, `endTime + 6h` grace, lock + re-check (GameCompletionScheduler:31-40, AttendanceService:80-94). A game never completes early and never double-completes |
| B finds it in My Games | ✅ | `EXISTS` + owner-branch query, deterministic sort with `id` tiebreaker (GameRepository:69-80, MyGamesService:101-108); pending/rejected requesters excluded (tested, MyGamesIntegrationTest:233-269) |
| B rates players | ❌ **P0** | Backend fine, but the UI calls `/api/api/games/{id}/ratings` (ratings.ts:5-6 under client prefix) → 404 → "We could not load the rating list." Rating feature is dead end-to-end → **P0-1** |
| Rating appears in profile | ✅ (if API called directly) | Avg/count via native SQL (PlayerStatsRepository:45-72), joins enforce completed+attended |
| Game remains in history | ✅ | COMPLETED bucket in My Games |
| **Rejected B retries** | ❌ | Deterministic 500 + permanent lockout → **P1-3** |

### User-stuck points identified

1. **B rejected → retries → 500**, no way to clear the row (no withdraw endpoint). Stuck. (P1-3)
2. **B pending when game fills** → owner's Accept button fails with `GAME_NOT_JOINABLE`
   (JoinRequestsPanel:246 renders Accept for any PENDING on any status); B's request
   stays PENDING forever; B has no withdraw. Owner has a remedy (Reject). (P2)
3. **Session expired** → games/my-games/detail show raw "Authentication required"
   with no Sign-in action (only profile handles 401, profile/page.tsx:41-43,109-116). (P2)
4. **Rating panel** → generic failure copy forever (P0-1).
5. **Non-OPEN game, no request** → `JoinRequestAction.tsx:201` renders *nothing* —
   the player gets no explanation ("game started/full/closed"). (P3)

---

## 4. Authorization matrix

Expected vs **actual** (all verified in source; "—" = role not applicable).
Unauthenticated access to anything except register/login/health returns
401 `UNAUTHORIZED` (SecurityConfiguration:47-57).

| Endpoint / role | unauth | unrelated user | owner | accepted participant | pending requester | rejected requester |
|---|---|---|---|---|---|---|
| GET `/games` discovery | 401 | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| GET `/games/{id}` OPEN/FULL | 401 | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| GET `/games/{id}` COMPLETED/IN_PROGRESS/CANCELLED | 401 | **403** `GAME_ACCESS_DENIED` (GameService:268) | ALLOW | ALLOW | ALLOW | **403** |
| POST `/join-requests` (game OPEN, has slot) | 401 | ALLOW (201) | **409** `CANNOT_JOIN_OWN_GAME`:62 | **409** `ALREADY_PARTICIPANT`:76 | **409** `JOIN_REQUEST_EXISTS`:83 | ⚠️ **500** — should be 409; also permanently locked out (**P1-3**) |
| POST `/join-requests` game not OPEN | 401 | 409 `GAME_NOT_JOINABLE`:69 | 409 | 409 | 409 | 409 (reached before insert) |
| POST `/join-requests` game **started but still OPEN** | 401 | ⚠️ **ALLOW** — should be DENY (**P1-2**) | ⚠️ ALLOW (accept path too, :176) | — | — | — |
| GET `/join-requests` (owner list) | 401 | **403** `FORBIDDEN`:128 | ALLOW | **403** | **403** | **403** |
| GET `/join-requests/me` | 401 | own row or 404 (own-row only, :117) | own row or 404 | own row | own row | own row |
| PATCH `/join-requests/{id}` ACCEPT/REJECT | 401 | **403** `FORBIDDEN`:143 | ALLOW (+404 if request belongs to other game :151; 409 `REQUEST_ALREADY_DECIDED` :158) | **403** | **403** | **403** |
| GET `/participants` (roster) | 401 | **403**:49 | ALLOW | **403** | **403** | **403** (roster is owner-only by design; product silent — note only) |
| PATCH `.../attendance` | 401 | **403**:64 | ALLOW if `startTime ≤ now ≤ endTime+6h` (else 409), cross-game pid → 404 :67 | **403** | **403** | **403** |
| GET `/ratings/eligible` | 401 | 403 `RATER_NOT_PARTICIPANT`:154 | 403 if not participant | 403 `RATER_ATTENDANCE_REQUIRED`:158 unless `attended=TRUE`; game must be COMPLETED (409 `GAME_NOT_COMPLETED`:142) | 403 | 403 |
| POST `/ratings` | 401 | 403 | as above + 400 `SELF_RATING_NOT_ALLOWED`:91 + 403 `RATED_PLAYER_NOT_PARTICIPANT`/`..._ATTENDANCE_REQUIRED`:104-111 + 409 `RATING_ALREADY_EXISTS`:113 | as left | as left | as left |
| GET `/users/{id}` (profile) | 401 | ALLOW (no email exposed, UserProfileResponse:9-18) | ALLOW | ALLOW | ALLOW | ALLOW |
| PATCH `/users/me` | 401 | self only — no cross-user write path exists (ProfileController:41, JWT principal) | — | — | — | — |
| GET `/games/my` | 401 | own rows only (JPQL filters `:userId`, GameRepository:69-80; spoofed `userId` param ignored — tested MyGamesIntegrationTest:274-300) | own | own | **excluded** (only owner/participant rows count) | **excluded** |

**Deviations from expected:** exactly three — rejected-retry 500 (P1-3),
post-kickoff join/accept ALLOW (P1-2), and (cosmetic) `FULL` games answer
`GAME_NOT_JOINABLE` instead of `GAME_FULL` at JoinRequestService:69-74 (P3).
Everything else matches the intended matrix and is test-backed.

---

## 5. Game lifecycle audit

### 5.1 Actual transitions (grep over `src/main`, complete)

```
createGame        → OPEN         GameService.java:85
accept last slot  → FULL         JoinRequestService.java:208-210   (count+1 == maxPlayers, under game lock)
scheduler         → COMPLETED    AttendanceService.java:93         (now ≥ endTime + 6h, under game lock)
IN_PROGRESS       → NEVER SET    (0 writers in src/main; only test fixtures insert it)
CANCELLED         → NEVER SET    (0 writers; no cancel endpoint exists)
```

`ck_game_status` allows all five values (V1:38); enum matches (Game.java:71).
Downstream code that *depends* on `IN_PROGRESS`: attendance window
(AttendanceService:103-105), completion due-set (GameCompletionScheduler:35),
detail access rules (GameService:258-272 javadoc), My Games categories
(MyGameCategory:17), frontend tabs (MyGamesTabs:18-23), and two test fixtures.
All of it is unreachable today.

### 5.2 Verdict on IN_PROGRESS: **A — actual MVP bug** (user decision: add start-time transition)

Rationale:
- Because status stays OPEN after kick-off, `createJoinRequest` (:69-74) and
  `acceptUnderLock` (:176-181) only test `status == OPEN`, so **a game that has
  started — or already ended, within the 6 h grace — still accepts new players via
  API or direct link** (discovery hides them via `startTime >= :now`, but nothing
  else does). A late-accepted player then appears in the roster with
  `attended=null`, is counted as a no-show in `attendanceRate`
  (PlayerStatsRepository:28-33), and participates in ratings — data corruption.
- Meanwhile the My Games "In progress" tab is permanently dead, and started games
  remain labelled "Upcoming".

**Chosen fix (user decision):** extend the existing scheduler to transition
`OPEN|FULL → IN_PROGRESS` when `startTime` has passed. No migration needed.
Consequences (all already supported by existing code): join/accept blocked by the
`status == OPEN` checks; discovery unaffected (already time-filtered); detail-page
access rules activate as documented (GameService:248-257); My Games moves the game
from UPCOMING to IN_PROGRESS; attendance allowed; completion already includes
IN_PROGRESS in its due-set.

Accepted residual risk: scheduler granularity is 60 s, so a join can slip through
in the ≤60–90 s after kick-off before the tick runs. Accepted for MVP (user chose
transition-only over transition+guards).

### 5.3 CANCELLED

Unreachable: no owner "close game" endpoint (PRODUCT.MD:52 lists the capability).
Consequences: the My Games "Cancelled" tab is dead UI; `assertCanView`'s
CANCELLED branch is dead. Adding a cancel endpoint is a **new feature → out of
scope** (§21). Mitigation in this phase: hide the dead tab (§16 UX-4).

### 5.4 Time/grace/window consistency (verified)

| Concern | Implementation | Verdict |
|---|---|---|
| Completion grace | `game-completion.grace-period: 6h` (`application.yml:24`), injected into both scheduler and AttendanceService (:35) | single source ✅ |
| Scheduler cutoff | `endTime < now - grace` (Scheduler:33) | ✅ |
| Completion guard | `now < endTime+grace → return` (AttendanceService:90) | same predicate, one-instant boundary difference vs scheduler (P3) |
| Attendance window | `startTime ≤ now ≤ endTime+grace` (AttendanceService:113-124) | ✅ (opens *during* the match — matches UI copy AttendancePanel:226) |
| `requiredPlayers` | never consulted by completion (AttendanceService:89-93) | accepted: game completes regardless; unmarked = not-attended in stats (P3 note) |
| Discovery visibility | OPEN + `startTime ≥ now` (GameRepository:35-36) + V3 index | ✅ |
| My Games visibility | all statuses, owner OR participant, `EXISTS` (GameRepository:69-80) | ✅ |

---

## 6. Timezone audit

Chain verified end-to-end; **the core chain is correct**:

1. **Input (create-game):** IST wall time → UTC ISO via fixed −330 min offset
   (`create-game/page.tsx:13-18`). IST has no DST, so a fixed offset is correct.
2. **Backend validation:** `gameDate ≥ LocalDate.now(Asia/Kolkata)`
   (GameService:41,102); `startTime/endTime` compared as `Instant` (GameService:108-114). Correct split.
3. **Storage:** `TIMESTAMPTZ` for every timestamp column (V1:13,27,28,36,60,61,77,91);
   entities use `java.time.Instant` throughout; `game_date DATE ↔ LocalDate`. ✅
4. **Serialization:** Jackson 3 defaults → ISO-8601 (`...Z`) strings; frontend
   `new Date(value)` parses correctly.
5. **Display:** all formatting pinned to `Asia/Kolkata` via `Intl`
   (`lib/format.ts:1-37`) — IST rendering regardless of browser TZ. ✅
6. **Scheduler:** `Instant.now()` everywhere (Scheduler:33, AttendanceService:89) —
   TZ-independent. ✅
7. **My Games ordering:** by absolute `startTime` + `id` (MyGamesService:101-108) — TZ-independent. ✅
8. **Tests:** JVM pinned to `Asia/Kolkata` (`build.gradle:47`).

**Real issues (not speculative):**

- **TZ-1 (P3):** `gameDate` and `startTime` are never cross-checked for IST-date
  consistency (GameService:101-120 only checks "date not past" and "start in
  future"). A mismatched pair makes the game invisible under its own `?date=`
  discovery filter (GameRepository:37 compares `gameDate` raw). Our UI always sends
  a consistent pair (create-game:68-70), so this only affects raw API clients.
- **TZ-2 (P3):** discovery's optional date filter sends whatever the browser's
  `type=date` produces (browser-local), while `gameDate` is an IST-bucketed value.
  Identical for the target IST audience; differs only for users whose device TZ is
  not IST. Filter is opt-in (GameFilters.tsx:36 default `""`), limiting exposure.
- **TZ-3 (P3):** `Asia/Kolkata` is hard-coded in three places
  (GameService:41, format.ts:1, create-game:13) and the "6 hours" attendance copy
  is hard-coded separately from config (AttendancePanel:226 vs application.yml:24) — drift risk only.

No IST/UTC conversion bug was found in the shipped flow. Do not change the
timezone design.

---

## 7. Concurrency audit

### 7.1 The required scenario — verified correct (design-level)

Game has 1 slot; two owners' requests accepted concurrently:

1. Both `decideRequest` calls first execute
   `SELECT ... FOR UPDATE` on the game row (JoinRequestService:137,
   GameRepository:26-31). Under READ COMMITTED, the second transaction blocks, then
   re-reads the **latest committed** row after the lock is granted.
2. Tx A: request still PENDING (:158) → `acceptUnderLock` → count = max-1 (:190) →
   inserts participant (:198-203), sets request ACCEPTED (:205), sets status FULL
   (:208-210) → commits.
3. Tx B (different request): acquires lock → request still PENDING →
   `count == max` → either `GAME_FULL` (:191-196) or — because A also flipped the
   status — `GAME_NOT_JOINABLE` (:176-181). Either way **exactly one participant is
   accepted; `participants ≤ maxPlayers` never violated**; participant/`join_request`
   rows can never duplicate because `uq_participant` (V1:80) plus the lock make
   double-insert unreachable.
4. Tx B (same request, double-click): post-lock re-read sees ACCEPTED →
   409 `REQUEST_ALREADY_DECIDED` (:158). Correct.
5. **Lock ordering is consistent** (game row always first; no other lock exists) →
   no deadlock. Attendance and completion also lock the game first and re-validate
   state after acquiring it (AttendanceService:82-93) → no completion/attendance race.

**Verdict: do not redesign.** The only gaps are:

- **CON-1 (P1): none of this is tested** — zero matches for
  `CountDownLatch|ExecutorService|CompletableFuture|concurrent` in `src/test`.
  PRODUCT.MD rules 5–6 are unverified by the suite (§13).
- **CON-2 (P3):** the losing concurrent accept receives `GAME_NOT_JOINABLE` rather
  than `GAME_FULL` when the status already flipped (JoinRequestService:176 vs
  :191). Both are 409; frontend maps both to friendly copy (JoinRequestAction:118-123).
- **CON-3 (P3):** `createJoinRequest` performs an **unlocked** capacity check
  (:90-96). Two users can both pass it when one slot remains → both PENDING rows
  created (legal per schema); only one can ever be accepted, the other must be
  rejected. No corruption; stuck-pending is the same issue as §3-2 (P2 UX).

### 7.2 Other duplicate/race surfaces (verified)

| Surface | DB guard | App guard | Backstop | Verdict |
|---|---|---|---|---|
| duplicate join request | `uq_req_game_user` (V1:65) | PENDING-only precheck :83 | `catch` on plain `save()` :104-111 — **unreachable** (violation fires at commit) | ❌ P1-3 (see fix) |
| duplicate participant | `uq_participant` (V1:80) | exists-check + game lock | lock makes violation unreachable | ✅ |
| duplicate rating | `uq_rating` (V1:96) | precheck :113 | `saveAndFlush` inside `try` + constraint-name sniff :123-131,176-186 | ✅ reference implementation |
| duplicate attendance | attendance is a column; `uq_participant` | game lock + `findByIdAndGameId` :67-72 | — | ✅ |
| duplicate registration (email race) | `uq_app_user_email` (V1:15) | `existsByEmail` check (AuthService:27) | **none** → 500 on race | P2 (defer) |
| completion vs attendance | game lock both sides | re-check after lock | — | ✅ |

---

## 8. Rating audit

All requirements verified against `PlayerRatingService.java` + `V1__init.sql`:

| Requirement | App layer | DB layer | Verdict |
|---|---|---|---|
| Only COMPLETED games rated | `assertCompleted` :142-149 → 409 | n/a | ✅ |
| Only eligible (participant) rater | `requireEligibleRater` :151-157 → 403 | FKs | ✅ |
| Only attended rater | `requireAttended` :158-166 → 403 | n/a | ✅ |
| Self-rating impossible | :91-96 → 400 | `ck_rating_self` (V1:94) | ✅ belt & braces |
| Duplicate impossible | precheck :113 + `saveAndFlush`+catch :123-131 | `uq_rating` (V1:96) | ✅ |
| Score 1–5 | Bean `@Min/@Max` (CreatePlayerRatingRequest:14-15) | `ck_rating_score` (V1:93) | ✅ |
| Ratee must be participant + attended | :104-111 → 403 | FKs | ✅ |
| Aggregate average/count correct | native SQL AVG/COUNT (PlayerStatsRepository:45-72), joins require completed game + both attended | — | ✅ |
| Invalid-game ratings can't affect stats | stats query filters `g.status='COMPLETED'` :62 (and write path already requires it) | — | ✅ |
| Eligible list excludes self, includes only attendees | :68-78 | — | ✅ |

Also correct: `GET /ratings` and `/ratings/eligible` are aliases
(PlayerRatingController:33 — harmless duplication, P3). `MyGamesService.canRate`
(:137-138) matches the service predicate exactly.

**No rating bug found.** The only rating problem in the product is the frontend
path prefix (P0-1). One accepted limitation: ratings never expire after
completion (fine for MVP).

---

## 9. My Games audit

| Check | Result | Evidence |
|---|---|---|
| Owner games appear | ✅ | owner branch GameRepository:71 (+ owner always has participant row, GameService:91-96) |
| Accepted participant games appear | ✅ | EXISTS subquery GameRepository:72-75 |
| Pending requests do NOT appear | ✅ | only owner/participant rows count; tested MyGamesIntegrationTest:233-247 |
| Rejected / cancelled requests do NOT appear | ✅ | tested :248-269 |
| Owner not duplicated | ✅ | `EXISTS` never multiplies the game row (documented GameRepository:59-61); dedup tested :421-445 |
| Completed games remain accessible | ✅ | COMPLETED category; regression-tested against discovery :375-416 |
| Pagination deterministic | ✅ | `Sort(startTime, id)` always (MyGamesService:101-108); stability tested :557-575 |
| Ordering correct | ✅ | UPCOMING/IN_PROGRESS asc; COMPLETED/CANCELLED/ALL desc |
| Status/category filtering correct | ✅ | `MyGameCategory.statuses()` → `IN :statuses`; ALL = full set (never null, MyGamesService:66-70) |
| `/games` = discovery, `/my-games` = personal | ✅ | deliberately separate services (MyGamesService javadoc :28-33); no overlap found |

Issues are presentational only:
- **MYG-1 (P2):** the "In progress" tab is dead today — fixed by the lifecycle
  change (§5.2). The "Cancelled" tab stays dead — hide it (§16 UX-4).
- **MYG-2 (P3):** `myRole` falls back to `OWNER` when no participant row exists
  (MyGamesService:133-135) — defensive only; owner always has a row.

---

## 10. Profile audit (Phase 10)

| Check | Result | Evidence |
|---|---|---|
| Profile loads | ✅ | GET `/users/me` (ProfileController:30); loading/empty/retry states present (profile/page.tsx:92-118) |
| Editing works | ✅ | PATCH partial update; empty-string clearing documented and implemented (profile.ts:17-33, types.ts:52-59) |
| Validation matches backend | ✅ | frontend `maxLength` constants (EditProfileForm:133,151,169) mirror 80/2000/500; blank-name and empty-body checks in service (ProfileService:34-50) |
| Statistics accurate | ✅ | derived by SQL from `match_participant`/`player_rating`, completed-games-only (PlayerStatsRepository:22-72); `matchesPlayed == matchesCompleted` is known-redundant (V1 note + planning_phase10) but **only `matchesPlayed` is rendered** (ProfileIdentityCard:68, JoinRequestsPanel:229) → no user-visible nonsense |
| Rating statistics accurate | ✅ | AVG/COUNT with completed+attended joins (PlayerStatsRepository:45-72) |
| Cannot modify another user | ✅ | endpoint is `/me` only; principal from JWT; no userId in body (ProfileController:41) |
| Auth behavior correct | ✅ | 401 handled with session-expired copy + Sign in link (profile/page.tsx:41-43,109-116) — the **only** page that does this |
| Frontend types match DTOs | ✅ | UserProfile/PlayerStats field-for-field vs UserProfileResponse/PlayerStatsResponse |
| No duplication of My Games | ✅ | profile shows identity + stats only |

Minor: `GET /api/users/{id}` has no authz beyond authentication (any signed-in
user can read any profile) — intentional public-profile design, email not exposed
(ProfileController:36-39). `ProfileService` lacks `@Transactional` → detached
merge; concurrent PATCHes could lose updates (P2, defer — single-user edits).
Profile does **not** need redesign.

---

## 11. Frontend/backend contract audit

Field-by-field comparison of `lib/types.ts` / `lib/api/*` against every backend
DTO record: **all names, enums, nullability and date formats match**
(Game/GameSummary/GameDetail vs GameResponse/GameSummaryResponse/GameDetailResponse;
MyGameSummary vs MyGameSummaryResponse incl. `category/myRole/myAttended/canRate`;
JoinRequest/JoinRequestDetail vs JoinRequest…; ParticipantDetail vs
ParticipantResponse; RateablePlayer (`myRating`) vs RateablePlayerResponse;
UserProfile/PlayerStats; AuthUser vs AuthResponse). `@JsonValue` "5V5" format
round-trips (backed by GameDiscoveryIntegrationTest:432). Pagination params
(`page,size,date,format,skillLevel,q` / `page,size,category`) match controller
signatures exactly.

| # | Sev | Finding | Evidence |
|---|---|---|---|
| C-1 | **P0** | **Ratings double prefix.** `ratingPath` = `/api/games/...` while `apiFetch` prepends `/api` → `/api/api/games/...` → 404 `NOT_FOUND`. Every other module correctly omits the prefix (profile.ts:12 even documents the convention). Rating GET **and** POST broken; panel shows generic copy because `NOT_FOUND` isn't mapped (GameRatingPanel:57-78) | ratings.ts:5-6, client.ts:1,21 |
| C-2 | P2 | **401/session expiry not handled** on games, my-games, detail pages — raw "Authentication required" banner, no Sign-in path; route guard only checks cookie *presence* (proxy.ts:9-11), not validity | games/page.tsx:48-52; my-games/page.tsx:68-72; games/[id]/page.tsx:144-150 |
| C-3 | P2 | **Error + empty double render:** on failure both the red banner and "No games found / Try adjusting your filters" render (catch clears the list, then the empty branch fires) | games/page.tsx:42-52 + 97-109; my-games/page.tsx:60-72 + 115-133 |
| C-4 | P2 | **Ungated owner actions:** AttendancePanel renders Present/Absent for every status incl. pre-start (backend rejects with 409 — error surfaces, buttons should not); JoinRequestsPanel renders Accept/Reject for any PENDING on any status (accept fails `GAME_NOT_JOINABLE` once FULL/started) | AttendancePanel.tsx:193-218; JoinRequestsPanel.tsx:246-270 |
| C-5 | P3 | `JoinRequestAction` renders nothing for a non-OPEN/full game with no request — no closed/full message | JoinRequestAction.tsx:201 |
| C-6 | P3 | `refreshStatus` swallows non-404 errors → stale status silently; initial fetch failure falls back to "none" | JoinRequestAction.tsx:72-74, 85-90 |
| C-7 | P3 | `console.log` leftover in attendance handler | AttendancePanel.tsx:72 |
| C-8 | P3 | `game.owner` dereferenced unguarded although backend can emit `owner: null` (FK makes it near-impossible) | games/[id]/page.tsx:258,345 vs GameService:209-215 |
| C-9 | P3 | auth-context `loading` unused by detail page → brief "Log in to request to join" flash before hydration | auth-context.tsx:19-36, games/[id]/page.tsx:128 |
| C-10 | P3 | BottomNav logout failure still navigates to /login without clearing user | BottomNav.tsx:24-30 |
| C-11 | P3 | Backend `VALIDATION_ERROR` message embeds `Map.toString()` ("Request validation failed: {field=...}") and is displayed verbatim in error boxes | GlobalExceptionHandler.java:37-42 → create-game/page.tsx:82 |

Error-code coverage was cross-checked: every backend code the UI can receive in
normal flows has a mapped message (JoinRequestAction:102-135,
AttendancePanel:92-120, GameRatingPanel:26-78) — except the `NOT_FOUND` produced
by C-1.

---

## 12. Database audit

Migrations V1–V3 read in full; `ddl-auto: validate` + `FlywaySchemaTest` enforce
entity↔schema parity at boot.

**Verified sound:**
- Types: every timestamp is `TIMESTAMPTZ`; `Instant↔TIMESTAMPTZ`, `LocalDate↔DATE`,
  `short↔SMALLINT`, `BigDecimal(9,6)↔NUMERIC(9,6)` — no mismatches.
- Enums stored as strings with matching CHECKs: `ck_game_status` (V1:38),
  `ck_req_status` (V1:63), `ck_role` (V1:79), `ck_game_format/skill`,
  `ck_app_user_skill/position` (V1:16, V2:4-11).
- FKs on all 7 relationships; no deletes exist anywhere in `src/main`, so
  `NO ACTION` never bites today.
- Business-rule constraints: `uq_req_game_user` (V1:65), `uq_participant` (V1:80),
  `uq_rating` (V1:96), `ck_rating_self`, `ck_rating_score`, `ck_game_window`
  `end_time > start_time` (V1:42), capacity 5–22, `required ≤ max`.
- `attended BOOLEAN` is tri-state by design (null = unmarked) — used consistently.

**Index ↔ query cross-reference:**

| Query | Index support | Verdict |
|---|---|---|
| Discovery `status= ? AND startTime ≥ ? ORDER BY startTime` (GameRepository:33-44) | `idx_game_status_start` (V3) — equality, range, sort | ✅ perfect |
| Discovery `gameDate =` / `skill,format` | `idx_game_date` (V1:50), `idx_game_skill_fmt` (V1:52) | ✅ |
| My Games `owner_id =` / EXISTS on participant | `idx_game_owner` (V1:51), `uq_participant` prefix | ✅ |
| Join requests `game_id`-led | `idx_join_request_game` (V1:68), `uq_req_game_user` | ✅ |
| Participants `user_id`-led / `game_id,user_id` | `idx_participant_user` (V1:83), `uq_participant` | ✅ |
| Ratings `ratee_id` + triple unique | `idx_player_rating_ratee` (V1:99), `uq_rating` | ✅ |
| Scheduler `status IN(...) AND end_time < ?` (GameRepository:82) | only the status half indexed; no `end_time`-leading index (`idx_game_window` has it 2nd, V1:53) | P3 — scans all open games each minute; acceptable at MVP scale |
| Discovery `LOWER(...) LIKE` text search | none | P3 — narrowed by other predicates first |
| `idx_join_request_user` (V1:69), `idx_game_window` (V1:53) | **no query leads with these columns** | P3 unused (write amplification only; dropping needs a migration → defer) |

**No schema change is needed for any Phase 11 fix.** Latent loose spots (no
lat/lng range CHECK, no `joining_fee >= 0`, no `description` length CHECK — V1:24-25,33,34)
are already enforced by bean validation; not worth a migration now (P3).

---

## 13. Test coverage audit

Infrastructure: 12 classes, ~148 tests, **real Postgres 16 via Testcontainers**
(TestcontainersConfiguration.java:15), MockMvc, error `$.code` assertions
pervasive, JVM TZ pinned IST (build.gradle:47). Coverage is genuinely strong for
discovery, game-detail access matrix, My Games (27 tests incl. stability/dedup/
spoofed-userId), attendance windows, completion grace, stats math, and profile
validation.

**Gaps, prioritized by risk (P1 first):**

| Risk | Gap | Evidence |
|---|---|---|
| **P1** | **Concurrency: zero tests.** The lock-based accept guarantee (PRODUCT rules 5–6) is unverified | grep `CountDownLatch\|ExecutorService\|CompletableFuture\|concurrent` in src/test = 0 |
| **P1** | **Join-request surface: no dedicated test class.** None of `CANNOT_JOIN_OWN_GAME`, `GAME_NOT_JOINABLE`, `ALREADY_PARTICIPANT`, `JOIN_REQUEST_EXISTS`, `GAME_FULL`, `REQUEST_ALREADY_DECIDED`, cross-game requestId 404 are asserted; helpers only assert 201/200 (AttendanceIntegrationTest:99-112) | grep of those codes in src/test = 0 |
| **P1** | **REJECT decision never exercised**; `PATCH .../join-requests` owner-only 403 untested; `GET .../join-requests/me` untested though the UI calls it | grep `requests/me` in tests = 0 |
| **P1** | **`OPEN → FULL` transition never driven through the API** — capacity math (:190-210) untested; all FULL fixtures are direct DB inserts | grep `value("FULL")` = fixtures only |
| P2 | `IN_PROGRESS` start-time transition (new) — no test possible today | §5 |
| P2 | Ratings edge codes untested: `SELF_RATING_NOT_ALLOWED`, `GAME_NOT_COMPLETED`, `RATED_PLAYER_*`, score bounds, unauthenticated ratings | grep = only `RATER_NOT_PARTICIPANT`/`RATER_ATTENDANCE_REQUIRED` (MyGames:667,626) |
| P2 | Attendance beyond-grace while status still OPEN (409 window-closed, AttendanceService:119-124) untested | — |
| P2 | 8 tests assert 401 status **without** `$.code` (Profile:93,252; Attendance:238; GameDetailAccess:161; Discovery:476,482; Game:209; MyGames:152) | inconsistent with suite convention |
| P3 | 8 raw Java `assert` statements instead of AssertJ (GameIntegrationTest:156-163,172,435) — silently skipped if `-ea` off | — |
| P3 | Scheduler `@Scheduled` fires during tests (no test override; acknowledged in GameCompletionIntegrationTest:44-46) → latent flakiness | SchedulingConfiguration:7 |
| P3 | Auth/Profile test classes share one DB without cleanup → rerun-fragile on unique emails | no `@BeforeEach` cleanup |
| P3 | Discovery pagination ties untested (fixture times strictly distinct, Discovery:226-231) | — |

**Recommended additions (risk-ordered, ~6 focused test methods + 1 class — not
hundreds):** one `JoinRequestIntegrationTest` covering the rule table in §4 +
re-request-after-rejection + OPEN→FULL + `/me`; one concurrency test (two threads,
last slot, assert one 201/one 409 and `participants ≤ max`); one transition test
(`OPEN → IN_PROGRESS` at startTime, join blocked afterwards); one tied-start-time
discovery stability test.

---

## 14. Frontend UX audit

| # | Sev | Area | Issue | Evidence |
|---|---|---|---|---|
| UX-1 | **P0** | Ratings | Rating panel cannot load or submit anything (C-1) | ratings.ts:5-6 |
| UX-2 | P2 | All list pages | Session-expired = dead end (C-2) | see §11 |
| UX-3 | P2 | Games/My Games | Error banner + "No games found" shown together (C-3) | see §11 |
| UX-4 | P2 | My Games | Dead tabs: "Cancelled" (backend can never produce CANCELLED); "In progress" dead until §5 fix lands — both expose states the backend can't support | MyGamesTabs:18-23 |
| UX-5 | P2 | Detail | Owner sees enabled Present/Absent buttons before start and Accept/Reject on non-OPEN games; clicks bounce off 409s (C-4). Post-§5, started games will hit this more often | AttendancePanel:193-218, JoinRequestsPanel:246 |
| UX-6 | P3 | Detail | Non-OWNER, no-request, non-OPEN/full → component renders nothing, no explanation (C-5) | JoinRequestAction:201 |
| UX-7 | P3 | Detail | "Refresh status" swallows errors silently (C-6); owner list N+1 is invisible to users but slow on big applicant lists | JoinRequestAction:85-90 |
| UX-8 | P3 | Attendance | `console.log` leftover; hardcoded "6 hours" copy can drift from config | AttendancePanel:72,226 |
| UX-9 | P3 | Discovery | `GameCard` "Game is full" branch is dead (discovery returns OPEN only) | GameCard:41-50 vs GameRepository:35 |
| UX-10 | P3 | Nav | Logout failure still routes to /login with stale user; auth flash on detail page (C-9, C-10) | BottomNav:24-30 |
| UX-11 | P3 | Validation | Raw `VALIDATION_ERROR` map string shown in error boxes (C-11) | GlobalExceptionHandler:37-42 |

Loading states, empty states per My Games tab, 404 screen on detail, and
pagination guards are otherwise in good shape — no redesign needed.

---

## 15. Production-readiness audit

**Verified good:** git tree clean (`git status` empty); **no generated files
tracked** (`backend/bin`, `backend/build`, `backend/.gradle`, `frontend/.next`,
logs all ignored and untracked); no `.env*` files anywhere; docker-compose
(`postgres:16`, port **5430**, creds `gameconnect`) matches `application.yml:5-7`;
backend 8080 + Flyway auto-migrate + `/api/health`; frontend `npm run dev` :3000
with same-origin rewrite (no CORS needed, none configured — fail-closed);
README quickstart commands verified against actual config; Testcontainers tests
require only Docker; `ddl-auto: validate`; actuator exposure limited to
`health,info`; BCrypt cost 10; no seeded credentials; page size bounded (≤100).

**Issues:**

| # | Sev | Finding | Evidence |
|---|---|---|---|
| PR-1 | **P1** | Default HS256 secret committed; any deployment forgetting `JWT_SECRET` accepts forged tokens from the public repo value. Same literal embedded in tests | application.yml:20; AuthIntegrationTest:229 |
| PR-2 | P2 | No production config documentation: `JWT_SECRET`, `COOKIE_SECURE`, `DB_URL/DB_USER/DB_PASSWORD`, `PORT`, `JWT_EXPIRATION_MINUTES`, `GAME_COMPLETION_GRACE` all undocumented; no `.env.example` | README:15-48 |
| PR-3 | P2 | `next.config.ts:8` rewrite target falls back to `http://localhost:8080/api`; rewrites resolve at build time — a prod build without `NEXT_PUBLIC_API_BASE_URL` silently proxies to localhost | next.config.ts:4-11 |
| PR-4 | P2 | `SecurityConfiguration:42` permits `/api/actuator/**` which **matches nothing** (real base path is `/actuator`) → anonymous `/actuator/health` probes get 401; the rule is dead either way | SecurityConfiguration:42; application.yml:29-33 |
| PR-5 | P2 | README is stale: "Phase 1 scope … endpoints land in later phases" (all landed); `./gradlew` won't work on Windows without `gradlew.bat`; root `AGENTS.md` build/test/lint sections empty | README:50-53 |
| PR-6 | P2 | `cookie.secure` defaults false; dev DB creds as defaults; single non-profiled `application.yml` (dev defaults = prod defaults) | application.yml:5-7,26-27 |
| PR-7 | P3 | No backend lint/format command documented; frontend has `lint` but no `typecheck` script (`next build` typechecks); **no CI at all** | package.json:5-10; no `.github/` |
| PR-8 | P3 | CSRF disabled with cookie auth — mitigated by `SameSite=Lax` + httpOnly + no state-changing GET; worth a README security note only | SecurityConfiguration:36; AuthController:114 |
| PR-9 | P3 | Logout clears cookie only; JWT valid up to 60 min; no rate limiting on public login; email case-sensitive uniqueness | AuthController:93-106; AuthService:27 |

**Clean-machine runbook (verified):** Docker → `docker compose up -d` (DB :5430)
→ `backend/gradlew.bat bootRun` (Flyway runs at startup) → `frontend: npm install
&& npm run dev` (:3000, rewrite → :8080). No seed data needed. Tests:
`gradlew.bat test` (Docker required for Testcontainers).

---

## 16. Findings with P0/P1/P2/P3 priority

### P0 — blocking
| ID | Area | Finding (evidence) |
|---|---|---|
| **F-01** | FE contract | Ratings double `/api` prefix → whole rating feature 404s in UI (`ratings.ts:5-6` + `client.ts:1,21`) |

### P1 — important correctness
| ID | Area | Finding (evidence) |
|---|---|---|
| **F-02** | Lifecycle | No `IN_PROGRESS` transition; join/accept only check `status==OPEN` → post-kickoff/finished games still accept players (`JoinRequestService:69-74,176-181`; status writers = `GameService:85`, `JoinRequestService:209`, `AttendanceService:93` only) |
| **F-03** | Join requests | Rejected re-request: PENDING-only precheck (`:83`) vs any-state `uq_req_game_user` (V1:65) + `save()` without flush (`:104-111`) → deterministic 500 at commit (no `DataIntegrityViolation` handler, GlobalExceptionHandler:64-68) **and** permanent lockout |
| **F-04** | Discovery | Non-deterministic pagination: `ORDER BY g.startTime ASC` without `id` tiebreaker (`GameRepository:43`) + unsorted `PageRequest` (`GameService:181`) |
| **F-05** | Security/config | Committed default JWT signing secret (`application.yml:20`) + zero production config documentation |
| **F-06** | Tests | Join-request rules, REJECT path, `/join-requests/me`, `OPEN→FULL`, and **all concurrency guarantees** untested (grep evidence §13) |

### P2 — worthwhile MVP improvement
| ID | Area | Finding |
|---|---|---|
| F-07 | FE UX | Session-expired dead ends on games/my-games/detail (C-2) |
| F-08 | FE UX | Error + empty state double render on both list pages (C-3) |
| F-09 | FE UX | Ungated owner actions: attendance buttons pre-start/closed, Accept/Reject on non-OPEN (C-4); dead "Cancelled" tab (UX-4) |
| F-10 | FE UX | `JoinRequestAction` silent no-render for closed/full game (C-5) |
| F-11 | Config | `.env.example` + README production section + stale README/AGENTS (PR-2, PR-5); JWT default-secret startup WARN (decision) |
| F-12 | Security | Dead `/api/actuator/**` permitAll — align matcher so `/actuator/health` is usable (PR-4) |
| F-13 | Backend | Register duplicate-email race → 500 (no integrity catch, AuthService:35-36) |
| F-14 | Backend | `ProfileService`/`AuthService` missing `@Transactional`; detached merge, no `@Version` → lost updates (ProfileService:33-70) |
| F-15 | Tests | Scheduler enabled in test contexts → latent flakiness (SchedulingConfiguration:7); ratings/attendance-beyond-grace edge codes untested; 8 status-only 401 assertions |
| F-16 | BE contract | `FULL` game answers `GAME_NOT_JOINABLE` instead of `GAME_FULL` (`JoinRequestService:69-74` vs `:90-96`); concurrent loser gets same code (CON-2) |

### P3 — optional cleanup / future
N+1 owner request list (`JoinRequestService:242-264`, ≥3 queries/row, unbounded);
scheduler lacks per-game error isolation + `end_time`-leading index; three
different comparisons vs `endTime+grace` (Scheduler:33, AttendanceService:90,119);
unused indexes `idx_join_request_user`/`idx_game_window`; dead methods
`JoinRequestRepository.findByIdForUpdate`, `MatchParticipantRepository.findByGameId`;
`console.log` (AttendancePanel:72); `game.owner` null deref (C-8); auth flash
(C-9); logout failure nav (C-10); raw validation map in UI (C-11); `q` LIKE
wildcard escaping (GameService:299-304); `gameDate`/`startTime` IST consistency
(TZ-1); browser-local date filter (TZ-2); 200-vs-201 on register; generic
`FORBIDDEN` code vs domain codes; validation errors flattened into message
(GlobalExceptionHandler:37-42); 405/415 fall to 500 handler; email
case-sensitivity; user-enumeration timing; no login rate limiting; logout doesn't
revoke JWT; CSRF/CORS documentation notes; no CI/frontend tests; raw `assert`s in
GameIntegrationTest; `matchesPlayed == matchesCompleted` redundancy (rendered
correctly today); `MyGamesService.canRate` true when nobody else attended →
empty panel; roster owner-only (product silent); `requiredPlayers` ignored by
completion; lat/lng `0,0` placeholder (create-game:66-67); unused dead branches
(GameCard "full"); FK `NO ACTION` + no delete paths (future feature).

---

## 17. MUST FIX BEFORE MVP

1. **F-01 (P0)** — fix `ratings.ts` path prefix; verify rating load + submit in UI.
2. **F-02 (P1)** — scheduled `OPEN|FULL → IN_PROGRESS` transition at `startTime`
   (existing scheduler; no migration). Unblocks correct join/accept blocking, the
   "In progress" tab, and the documented access rules.
3. **F-03 (P1)** — join-request re-request: reuse the existing row (reset to
   `PENDING`, clear `decidedAt`) instead of inserting; `saveAndFlush` inside
   `try/catch` → genuine races return 409, never 500. (Decision: reset-to-PENDING,
   no new endpoint, no migration.)
4. **F-04 (P1)** — append `g.id ASC` tiebreaker to discovery `ORDER BY`.
5. **F-05 (P1)** — `.env.example` + README "Configuration / Production" section
   (every env var, Windows commands, current phase scope) + startup **WARN** when
   the default JWT secret is in use.
6. **F-06 (P1)** — focused tests: `JoinRequestIntegrationTest` (rule matrix of §4,
   REJECT path, `/me`, owner-only 403s, re-request-after-rejection, `OPEN→FULL`),
   one concurrent-accept test (last slot: exactly one winner, count ≤ max,
   correct 409 to the loser), one `IN_PROGRESS` transition test, one tied-start
   discovery stability test.
7. **F-07…F-10 (P2 UX)** — session-expired handling on the three pages; exclusive
   error-vs-empty rendering; status/time gating for attendance & join-request
   buttons + closed-game copy in `JoinRequestAction`; hide the dead "Cancelled"
   tab; remove `console.log`.

Everything in §18 can defer without compromising MVP correctness or security.

---

## 18. CAN DEFER

- F-12 actuator matcher, F-13 register race, F-14 profile transactions,
  F-15 test-infra hardening, F-16 error-code polish.
- All P3 items of §16 (N+1, indexes, dead code, cosmetic error codes, email
  case-normalization, rate limiting, JWT revocation, CI, frontend test harness).
- Product-listed but unimplemented capabilities: owner **cancel/close game**
  (enables CANCELLED tab + status), requester **withdraw request**, owner
  **remove player**, `/profile/[id]` page, lat/lng collection on create-game.
  All are new features — explicitly out of scope (§21).
- Any migration (indexes, CHECK tightening, FK cascade) — no demonstrated need
  at MVP scale.

---

## 19. Recommended implementation order

1. **P0 fix** — `frontend/lib/api/ratings.ts` prefix (5-min fix, biggest user
   impact); smoke-test rating load + submit.
2. **Backend join-request fixes (F-03)** in `JoinRequestService.createJoinRequest`
   (re-request reset + `saveAndFlush` + violation classification), then its tests.
3. **Lifecycle transition (F-02)** — repository query for due-to-start games +
   scheduler method setting `IN_PROGRESS` (+ guards), then transition tests; run
   the full suite (existing completion/access tests must stay green).
4. **Discovery tiebreaker (F-04)** + tied-time stability test.
5. **Remaining F-06 tests** (concurrency, join-request matrix, `OPEN→FULL`).
6. **Frontend UX batch (F-07…F-10, C-7)** — one PR across
   `games/page.tsx`, `my-games/page.tsx`, `games/[id]/page.tsx`,
   `AttendancePanel`, `JoinRequestsPanel`, `JoinRequestAction`, `MyGamesTabs`.
7. **Config/docs (F-05)** — `.env.example`, README, JWT WARN.
8. **Full verification** — `gradlew.bat test`, `npm run lint`, `next build`,
   then one manual end-to-end pass of the §3 flow including the rejected-retry
   and rating paths.

Steps 1–5 are the correctness core; 6–7 are the release polish.

---

## 20. Files likely to change

**Backend (main):**
- `game/service/JoinRequestService.java` (re-request reset, flush, optional F-16)
- `game/scheduler/GameCompletionScheduler.java` (start-time transition)
- `game/repository/GameRepository.java` (due-to-start query; discovery `ORDER BY` tiebreaker)
- `config/SchedulingConfiguration.java` or `application.yml` (JWT default-secret WARN wiring; optionally scheduling toggle for tests)
- `auth/service/AuthService.java` (only if F-13 pulled forward)

**Backend (test):**
- new `game/JoinRequestIntegrationTest.java`, new concurrency test class,
  `game/GameCompletionIntegrationTest.java` or new lifecycle test,
  `game/GameDiscoveryIntegrationTest.java` (tie stability)

**Frontend:**
- `lib/api/ratings.ts` (P0)
- `app/(app)/games/page.tsx`, `app/(app)/my-games/page.tsx`,
  `app/(app)/games/[id]/page.tsx` (401 + error/empty + gating wiring)
- `components/games/AttendancePanel.tsx`, `JoinRequestsPanel.tsx`,
  `JoinRequestAction.tsx`, `MyGamesTabs.tsx`

**Docs/config:**
- `README.md`, new `.env.example` (root), optionally root `AGENTS.md`

**No migrations. No entity/DTO changes. No new endpoints.**

---

## 21. Explicitly out of scope

- All features listed in §16 of the phase brief: chat, payments, turf booking,
  teams, tournaments, subscriptions, friends/following, feed, recommendations,
  leaderboards, Elo/Glicko, push notifications, Redis, WebSockets, microservices,
  AI matchmaking, advanced analytics.
- New product capabilities (even though PRODUCT.MD lists them): cancel/close game,
  withdraw join request, remove player, `/profile/[id]`, location/map features.
- Refactors for style; schema redesign; error-code taxonomy overhaul; CI setup;
  rate limiting; refresh tokens/revocation; CORS work (same-origin topology is the
  supported one).
- Anything beyond the MUST FIX list that would sneak features into a hardening phase.

---

## Build-mode sequence (only what is genuinely necessary for MVP-ready)

```
1. frontend/lib/api/ratings.ts          — drop the "/api" prefix (P0 rating flow)
2. backend JoinRequestService           — re-request resets existing row to PENDING
                                          + saveAndFlush → 409 not 500 (P1)
3. backend scheduler + GameRepository   — OPEN/FULL → IN_PROGRESS at startTime
                                          (P1 lifecycle; blocks post-kickoff joins)
4. backend GameRepository               — discovery ORDER BY startTime, id (P1 paging)
5. backend tests                        — JoinRequestIntegrationTest (rule matrix,
                                          REJECT, /me, re-request, OPEN→FULL),
                                          concurrent-accept test, transition test,
                                          tied-start discovery test (P1 verification)
6. frontend UX                          — 401 session-expired handling (games,
                                          my-games, detail), exclusive error/empty
                                          render, gate attendance + accept/reject
                                          buttons by status/time, closed-game copy
                                          in JoinRequestAction, hide dead
                                          "Cancelled" tab, remove console.log (P2)
7. docs/config                          — .env.example, README production/env/
                                          Windows/stale-scope updates, startup WARN
                                          on default JWT secret (P1 hygiene)
8. verify                               — gradlew.bat test, npm run lint,
                                          next build, manual E2E of §3 incl.
                                          rejected-retry and rating paths
```
