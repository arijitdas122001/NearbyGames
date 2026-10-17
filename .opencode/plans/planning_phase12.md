# Phase 12 — MVP Stabilization & End-to-End Validation

> Plan-only deliverable. No new features. No DB migration unless a verified defect
> forces one (audit says none is needed). Scope stays at the football pickup MVP.

## 0. Scope & constraints

- **Deliverable:** this plan, `.opencode/plans/planning_phase12.md`.
- **In scope:** baseline verification runs, close the Phase 11 carryovers
  (F-05 docs/config, F-06 tests, F-07–F-10 frontend UX), the login-input
  regression, and a full manual end-to-end pass.
- **Out of scope:** any new feature (chat, payments, notifications, teams,
  tournaments, turf booking). Committed deferrals are listed in §9.
- **Preserve:** default-deny security, JWT principal identity, pessimistic
  game-row lock on accept, `assertCanView` restriction for non-OPEN/FULL games,
  cookie-based session, `proxy.ts` cookie gate.

## 1. Evidence-based Phase 11 audit

Phase 11 commit `9cf7197` (2026-10-09) changed only 8 files. Verified against
current code, not assumed:

| Item | Planned severity | Actual | Evidence |
|---|---|---|---|
| F-01 ratings prefix | P0 | DONE | `frontend/lib/api/ratings.ts` now uses `/games/...` (no double `/api`) |
| F-02 IN_PROGRESS transition | P1 | DONE | `GameCompletionScheduler.startDueGames()` (`GameCompletionScheduler.java:44-60`); `GameRepository.findByStatusInAndStartTimeBefore` |
| F-03 rejected re-request reset | P1 | DONE | `JoinRequestService.java:102-108` resets existing row to PENDING + `saveAndFlush`; `uq_req_game_user` classified at `:130-140` |
| F-04 discovery tiebreaker | P1 | DONE | `GameRepository` discovery query `ORDER BY g.startTime ASC, g.id ASC` |
| F-05 docs/config | P1 | MISSING | no `.env.example`; `README.md:50` still "Phase 1 scope"; default secret `application.yml:20` has no startup WARN |
| F-06 tests | P1 | MISSING | 12 test classes, zero matches for `GAME_FULL`, `CANNOT_JOIN_OWN_GAME`, `REQUEST_ALREADY_DECIDED`, `JOIN_REQUEST_EXISTS`, `ALREADY_PARTICIPANT`, `startDueGames`, `CountDownLatch` |
| F-07 session expiry | P2 | MISSING | only `frontend/app/(app)/profile/page.tsx` handles 401 |
| F-08 error+empty double render | P2 | PRESENT | `games/page.tsx:97-109`, `my-games/page.tsx:115-133` render both states |
| F-09 action gating | P2 | MISSING | `AttendancePanel.tsx:193-218` ungated; `JoinRequestsPanel.tsx:246` shows buttons for any PENDING |
| F-10 closed-game copy | P2 | MISSING | `JoinRequestAction.tsx:201` silent `return null` |
| F-12 actuator matcher | Deferred | STILL DEAD | `SecurityConfiguration.java:42` |
| Scheduling test toggle | bonus | DONE | `@ConditionalOnProperty(app.scheduling.enabled, matchIfMissing=true)` + test `application.properties` |
| **NEW suspected regression** | — | SUSPECTED | `login/page.tsx:56` email input `text-white`; password `:71` and register still `text-zinc-900` |

## 2. Verification status (evidence)

There is **no passing verification of any Phase 11 change**:

- Phase 11 committed 2026-10-09; all backend test artifacts are from 2026-09-28.
- `backend/build/test-results/test` holds a single XML
  (`TEST-com.gameconnect.game.GameCompletionIntegrationTest.xml`, 2026-09-28
  21:11): tests=1, failures=1 — `Failed to load ApplicationContext`, caused by
  Docker being unavailable at that moment.
- Frontend `.next` production build is dated 2026-09-28 (pre-change); no lint
  evidence after the changes.
- Tooling is now available: Docker 27.3.1, Java 21, `backend/gradlew.bat`.

**Conclusion:** the first work item must be a real baseline run (§5, G0).

## 3. Classification

### A. Confirmed defects

1. Login email input `text-white` — invisible typed text on the default white
   input background; inconsistent with password and register inputs. (P1)
2. Error + empty state double render on `games` and `my-games`. (P2)
3. Owner attendance buttons rendered for every game status (no time/status
   gate). (P2)
4. Accept/Reject shown for any PENDING request regardless of game status. (P2)
5. Closed/full game with no request renders nothing (no explanation). (P2)
6. Missing session-expired affordance on games / my-games / detail. (P2)
7. `console.log("in the attendance component")` at `AttendancePanel.tsx:72`. (P3)
8. F-05 docs/config gaps and F-06 zero coverage of the Phase 11 logic. (P1)

### B. Untested behavior (code looks correct)

- IN_PROGRESS transition (`startDueGames`).
- Rejected re-request reset path.
- Full join-request rule matrix (open / full / own game / already participant /
  duplicate pending).
- Concurrent last-slot accept.
- Tied-`startTime` discovery pagination stability.
- Rating edge codes: `SELF_RATING_NOT_ALLOWED`, `RATER_NOT_PARTICIPANT`,
  `RATER_ATTENDANCE_REQUIRED`, `RATED_PLAYER_ATTENDANCE_REQUIRED`,
  `RATING_ALREADY_EXISTS`.
- Attendance marking after grace while game still OPEN.

### C. Optional improvements

- Dead `/api/actuator/**` matcher, `next.config.ts` build-time rewrites, dead
  "Cancelled" tab — see §9 deferrals.

## 4. Implementation groups (small, independently buildable/testable)

### G0 — Baseline verification (no code changes)
- `docker compose up -d`
- `backend\gradlew.bat test`
- `frontend: npm run lint`, `npm run build`
- Record raw results; triage any pre-existing failures before attributing them
  to Phase 11.

### G1 — Backend test gaps (F-06, test-only)
- New `JoinRequestIntegrationTest`:
  - create: success, `GAME_FULL`, `CANNOT_JOIN_OWN_GAME`, non-OPEN →
    `GAME_NOT_JOINABLE`, `ALREADY_PARTICIPANT`, duplicate PENDING →
    `JOIN_REQUEST_EXISTS`;
  - `GET /me` → `REQUEST_NOT_FOUND` (404);
  - re-request after REJECT resets to PENDING; re-request when FULL →
    `GAME_FULL`; re-request after ACCEPT → `ALREADY_PARTICIPANT`;
  - decide: accept success, reject success, `REQUEST_ALREADY_DECIDED`,
    owner-only `FORBIDDEN`, `REQUEST_NOT_FOUND`; last-slot accept → game `FULL`.
- Concurrency test: two threads accept two PENDING requests for the final slot
  (`ExecutorService` + `CountDownLatch`); assert exactly one participant added,
  participants ≤ maximum, loser gets 409.
- `GameCompletionIntegrationTest`: `startDueGames` flips OPEN/FULL with past
  start to IN_PROGRESS; future games unchanged; idempotent.
- `GameDiscoveryIntegrationTest`: two games with identical `startTime` paginate
  by `id` with no duplicates or omissions.
- `MyGamesIntegrationTest` / completion: rating edge codes; attendance after
  grace while OPEN.

### G2 — Frontend session & data states (F-07, F-08) + login fix
- Shared 401 handling for games / my-games / detail, mirroring the existing
  pattern in `frontend/app/(app)/profile/page.tsx` (re-login prompt/redirect).
- Make error and empty states mutually exclusive in `games/page.tsx` and
  `my-games/page.tsx`.
- Revert `login/page.tsx:56` email input to `text-zinc-900` so it matches the
  password field and register page.

### G3 — Frontend action gating & copy (F-09, F-10)
- Gate `AttendancePanel` buttons on game status + post-start (pass `status`
  from the detail page).
- Gate Accept/Reject in `JoinRequestsPanel` on `status === OPEN`.
- Replace `JoinRequestAction.tsx:201` `return null` with closed/full copy.
- Remove the `console.log` at `AttendancePanel.tsx:72`.

### G4 — Config & docs (F-05)
- Add `.env.example` (DB URL/user/password, JWT secret + expiry, completion
  grace, cookie secure, frontend API base).
- Rewrite `README.md`: Windows commands (`gradlew.bat`), current feature set,
  config and production notes (replace "Phase 1 scope").
- Emit a startup WARN when the JWT secret equals the built-in default dev value.

### G5 — Manual E2E validation run (no code changes)
- Execute the checklist in §7 and capture evidence.

## 5. Verification commands

```powershell
docker compose up -d
backend\gradlew.bat test
backend\gradlew.bat test --tests "com.gameconnect.game.JoinRequestIntegrationTest"

# frontend
npm run lint
npm run build

# health
curl http://localhost:8080/api/health

# short lifecycle / expiry for manual pass
$env:GAME_COMPLETION_GRACE="1m"; $env:JWT_EXPIRATION_MINUTES="1"; backend\gradlew.bat bootRun
```

## 6. Acceptance criteria

- Full backend suite green (0 failures); new tests pass and cover §3B.
- `npm run lint` clean; `npm run build` succeeds (includes typecheck).
- Login/register input text is visible in light mode; both login inputs match.
- Session expiry surfaces a re-login affordance on games / my-games / detail.
- No screen renders an error banner and an empty state at the same time.
- Owner actions appear/enable only when actionable; closed/full games show copy.
- Manual E2E checklist passes end-to-end.
- No DB migration is introduced.

## 7. Manual E2E checklist (accounts: Owner O, Players P and P2)

1. Register/login O, P, P2.
2. O creates a future game.
3. P sees it in discovery; filters and detail page work.
4. P requests to join → PENDING.
5. O accepts → P becomes participant.
6. P2 requests → O rejects → P2 re-requests → PENDING again (F-03).
7. Fill game to max → FULL; a new user cannot join; no join button shown.
8. Start time passes → scheduler flips to IN_PROGRESS; hidden from discovery;
   appears in My Games "In progress"; unrelated user denied on detail.
9. Attendance blocked pre-start; O marks Present/Absent after start.
10. `GAME_COMPLETION_GRACE=1m` → game auto-completes after end + grace.
11. P (attended) rates O; duplicate / self / attendance-required codes verified.
12. My Games tabs (Upcoming / In progress / Completed) and stats are correct.
13. `JWT_EXPIRATION_MINUTES=1` → session-expiry flow on games/my-games/detail.
14. ~390px viewport: no horizontal scroll; login inputs visible.
15. Loading / empty / error states verified on games, my-games, detail, panels.

## 8. Files likely to change

**New**
- `backend/src/test/java/com/gameconnect/game/JoinRequestIntegrationTest.java`
- `.env.example`

**Backend**
- `backend/src/test/java/com/gameconnect/game/GameCompletionIntegrationTest.java`
- `backend/src/test/java/com/gameconnect/game/GameDiscoveryIntegrationTest.java`
- `backend/src/test/java/com/gameconnect/game/MyGamesIntegrationTest.java`
- JWT config/properties class (locate the existing one) — default-secret WARN

**Docs**
- `README.md`

**Frontend**
- `frontend/app/login/page.tsx`
- `frontend/app/(app)/games/page.tsx`
- `frontend/app/(app)/my-games/page.tsx`
- `frontend/app/(app)/games/[id]/page.tsx`
- `frontend/components/games/AttendancePanel.tsx`
- `frontend/components/games/JoinRequestsPanel.tsx`
- `frontend/components/games/JoinRequestAction.tsx`
- shared 401 helper (`frontend/lib/api/client.ts` or
  `frontend/lib/auth-context.tsx`)

## 9. Explicit deferrals (with rationale)

- **Dead `/api/actuator/**` matcher** (`SecurityConfiguration.java:42`) —
  actuator maps under `/actuator` and only `health,info` are exposed; the rule
  is inert, not a security hole.
- **`next.config.ts` build-time rewrites** — only relevant to production
  hosting; local development goes through `frontend/proxy.ts`.
- **Dead "Cancelled" tab** — cancellation is not an MVP feature; the tab shows
  a correct empty state, so it is cosmetic, not a bug.
- **F-13 register duplicate-email race / F-14 `@Transactional` cleanup /
  F-16 `GAME_FULL` vs `GAME_NOT_JOINABLE` for FULL games** — low impact; the
  frontend maps both responses to the same blocked copy.

## 10. Blockers & risks

- Testcontainers requires Docker running (available now: 27.3.1).
- Lifecycle E2E needs a short `GAME_COMPLETION_GRACE` and a scheduler tick
  (60s); create-game requires a future start time.
- No frontend test framework → frontend is verified via lint + build + manual.
- Baseline suite may surface pre-existing failures → triage in G0 before
  attributing them to Phase 11.
