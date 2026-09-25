# GameConnect Phase 7 planning — Attendance tracking + game completion

## Resolved decisions (confirmed with owner)

1. **Completion = automatic only** — a scheduled job transitions `OPEN/FULL/IN_PROGRESS → COMPLETED`; there is **no manual "Complete Game" button or endpoint**.
2. **`matchesPlayed`/`matchesCompleted` = attended-only** — a no-show or unmarked participant gets no "played" credit. Applies **everywhere**, including the Phase 6 owner join-panel "Matches" stat.
3. **Attendance completeness = optional** — unmarked `NULL` participants are allowed on completion and are treated as **not attended** in statistics.
4. **Attendance window = `[startTime, endTime + grace]`** — a grace period (default **6 hours**, configurable) is added after `endTime` so owners can mark results after the match; the scheduler completes games only once `endTime + grace` has passed.

---

## 1. Phase 7 goal

Make `match_participant` meaningful as the authoritative participation-history record:

```
Game created → players request → owner accepts → game takes place
→ owner records attendance (during the match, or within the post-endTime grace period)
→ scheduler completes the game once endTime + grace passes
→ attended players accrue played/attendance statistics
```

Attendance reuses the **existing** `match_participant.attended` column. Statistics are **derived** from it (no new counter columns). There is **no manual completion**; the backend's scheduled job is the single writer of `COMPLETED`.

---

## 2. Current architecture findings

### Backend
- **Game entity** (`game/entity/Game.java`): `GameStatus {OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED}` exists. `startTime`/`endTime` are `Instant`. No `completedAt`/actual-end column. `status` defaults `OPEN`; **nothing in the codebase ever sets `COMPLETED` today** (join-request accept sets `FULL` only).
- **MatchParticipant entity** (`game/entity/MatchParticipant.java`): `attended Boolean` (nullable), `Role {PLAYER, OWNER}`, `joinedAt`, unique `(game_id, user_id)`. Owner is created as a participant on game creation; accepted requests add `Role.PLAYER` participants.
- **Repositories**: `GameRepository` has `findByIdForUpdate` (PESSIMISTIC_WRITE) and discovery queries. `MatchParticipantRepository` has `findByGameId`, `countByGameId`, `existsByGameIdAndUserId`, `countByGameIds`. No ordering/scope/owner helpers for attendance yet.
- **JoinRequestService** already demonstrates the canonical concurrency pattern: `@Transactional` + `findByIdForUpdate` on the Game row + status/count checks under lock. Phase 7 reuses this pattern.
- **PlayerStatsRepository** (native `JdbcTemplate`): computes `matchesPlayed`, `matchesCompleted`, `attendanceRate`, `averageRating` from `match_participant` + `game`. Today the denominator is "any participant in a COMPLETED game" and the numerator is `attended=TRUE`. Currently always returns zeros because no game ever reaches COMPLETED.
- **Profile API**: `GET /api/users/me` and `GET /api/users/{id}` return `UserProfileResponse` embedding `PlayerStatsResponse`. **Statistics flow automatically once games are completed — no profile API change required.**
- **Security**: cookie `auth_token` JWT; `SecurityConfiguration` requires auth on everything except register/login/health/actuator. Authorization is done in services via `AuthenticatedUser` + owner checks (`verifyOwner` pattern).
- **Errors**: `BusinessException(status, code, message)` → `GlobalExceptionHandler` → `ApiError`. Codes are uppercase snake, e.g., `GAME_NOT_FOUND`, `FORBIDDEN`.
- **Scheduling**: **`@EnableScheduling`/`@Scheduled` does not exist anywhere.** It must be added for the auto-completion job.
- **Migrations**: V1 (all tables incl. `match_participant.attended`, `join_request`, `app_user`, `player_rating`), V2 (position), V3 (discovery index). `ddl-auto: validate`.

### Frontend
- Pages: `/games`, `/games/[id]`, `/create-game`, `/login`, `/register`, `/`. **`/profile`, `/profile/[id]`, and `/my-games` do not exist** (BottomNav links to them → 404).
- `/games/[id]` integrates Phase 6: owner sees `JoinRequestsPanel`; player sees `JoinRequestAction`. It also renders `game.currentPlayers / spotsRemaining` but **does not surface `game.status`** to the user.
- API client: `lib/api/client.ts` (`apiFetch`, credentials cookie), `lib/api/games.ts`, `lib/api/join-requests.ts`, `lib/api/auth.ts`. `lib/types.ts` already has `GameStatus`, `GameParticipant {id, gameId, userId, role, attended, joinedAt}`, `PlayerStats`.
- Auth: `AuthProvider` context; `user.id` is available for owner checks.

---

## 3. Existing schema analysis

| Concern | Finding |
|---|---|
| `match_participant.attended` | Already exists as nullable `BOOLEAN` → **no new column** |
| Game status for COMPLETED | `COMPLETED` already in enum + DB `CHECK` constraint |
| One attendance per participant | Unique `(game_id, user_id)` guarantees it |
| Roster lookup by game | Covered by the unique `(game_id, user_id)` prefix index |
| Stats query by user | Covered by `idx_participant_user (user_id)` |
| Scheduler scan `status + end_time` | `idx_game_status (status)` + small MVP dataset; no new end_time index needed at this scale |

**Conclusion: No migration required.** Phase 7 schema is fully supported by V1–V3.

---

## 4. Domain / state-machine changes

**No new states.** The existing enum is used as intended:

```
OPEN ──(owner accept fills last spot)──▶ FULL
OPEN/FULL ──(scheduler, endTime + grace passed)──▶ COMPLETED
IN_PROGRESS ──(reserved; scheduler also completes it defensively)──▶ COMPLETED
CANCELLED/COMPLETED: terminal, never touched by the scheduler
```

- `IN_PROGRESS` is **not introduced** into any transition in Phase 7 (nothing sets it). Keeping it in the scheduler's target list is cheap future-proofing with zero behavior change.
- **No manual completion path.** The frontend never calls a complete endpoint; the job is the single writer of `COMPLETED`.
- `FULL` is not a blocker for completion: a pickup game that ran with fewer-than-max players still completes.

---

## 5. Attendance rules

- The **roster** = `match_participant` rows for the game (owner `OWNER` row + accepted `PLAYER` rows). Pending/rejected join requests never appear.
- **Only the game owner** can read the roster and modify attendance (verified from the authenticated principal).
- A participant has **exactly one** attendance value (per-row `attended`).
- `NULL` semantics: **not yet marked** → in statistics, treated as **did not attend** (per decision #3).
- Allowed values: `attended=true` (ATTENDED) / `attended=false` (DID NOT ATTEND). No partial/unset path.
- Attendance may be modified **only while `now >= startTime`, `now <= endTime + GRACE`, and status ∈ {OPEN, FULL, IN_PROGRESS}`**:
  - before start → `GAME_NOT_STARTED` (409),
  - after completion → `GAME_ALREADY_COMPLETED` (409),
  - cancelled → `GAME_NOT_ACTIVE` (409).
- Effective marking window: `[startTime, endTime + GRACE]`, where `GRACE` defaults to **6 hours** (configurable via property, e.g. `game-completion.grace-period`).
- The game-row lock (§10) ensures the window closes when the scheduler flips the game to `COMPLETED`.

---

## 6. Completion rules

**Automatic only**, after `endTime + grace`.

- New `@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)` job scans:
  `status IN (OPEN, FULL, IN_PROGRESS) AND endTime < now - grace` (i.e., `endTime + grace` already passed), reusing derived query `findByStatusInAndEndTimeBefore(statuses, now.minus(grace))`.
- Each eligible game is completed inside its **own transaction** with the Game row locked (`findByIdForUpdate`), then re-checked against `COMPLETED/CANCELLED`.
- Requirement: `@EnableScheduling` must be added (new `SchedulingConfiguration`). No external infra (no Redis/Quartz/Kafka) — plain Spring scheduling.
- The job is idempotent: already-completed/cancelled games are skipped; double-runs are safe.
- **No manual complete endpoint/RPC** exists in the API.

---

## 7. Statistics impact

Rewrite `PlayerStatsRepository.findParticipationStats` to **attended-only** semantics:

```sql
SELECT
    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = TRUE THEN mp.game_id END) AS matches_played,
    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = TRUE THEN mp.game_id END) AS matches_completed,
    CASE
        WHEN COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) = 0 THEN NULL
        ELSE CAST(
            COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = TRUE THEN mp.game_id END)
            AS DOUBLE PRECISION
        ) / COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)
    END AS attendance_rate
FROM match_participant mp
JOIN game g ON g.id = mp.game_id
WHERE mp.user_id = ?
```

Effects (all derived, no counters):
- `matchesPlayed = matchesCompleted` = distinct COMPLETED games where the player **attended**.
- `attendanceRate` = attended / distinct COMPLETED games where the player was a participant (denominator includes absent & NULL). A full no-show → `0.0`; unmarked → `0.0`.
- **No profile API change**; `GET /api/users/me`, `GET /api/users/{id}`, and Phase 6's `ApplicantSummary.stats` (owner join panel "Matches") automatically reflect the new values (confirmed: apply everywhere).
- `findAverageRating` is untouched (ratings are a future phase).

---

## 8. Backend API design

All endpoints reuse cookie auth; owner is always taken from `AuthenticatedUser`, never from the body/path.

### A. `GET /api/games/{gameId}/participants`
- Auth: authenticated. **Authorization: game owner only** (`FORBIDDEN` 403).
- Response `200`: `List<ParticipantResponse>` ordered by `joinedAt ASC`.
- `ParticipantResponse(UUID participantId, UUID userId, String displayName, String profileImageUrl, SkillLevel skillLevel, Role role, Boolean attended, Instant joinedAt)`.
- Errors: `UNAUTHORIZED` 401, `GAME_NOT_FOUND` 404, `FORBIDDEN` 403.

### B. `PATCH /api/games/{gameId}/participants/{participantId}/attendance`
- Auth: authenticated. **Authorization: game owner only**.
- Body: `AttendanceRequest(@NotNull Boolean attended)` → `{ "attended": true }`. Missing/blank → `VALIDATION_ERROR` 400.
- Response `200`: updated `ParticipantResponse`.
- Business rules: participant row must belong to the game (scoped `findByIdAndGameId`) → `PARTICIPANT_NOT_FOUND` 404; marks true/false; idempotent.
- Errors: `GAME_NOT_FOUND` 404, `PARTICIPANT_NOT_FOUND` 404, `FORBIDDEN` 403, `GAME_NOT_STARTED` 409, `GAME_ALREADY_COMPLETED` 409, `GAME_NOT_ACTIVE` 409, `VALIDATION_ERROR` 400, `UNAUTHORIZED` 401.

### C. `GET /api/games/{id}` (modified)
- Adds nullable `myParticipation: ParticipantSummary` — for any authenticated requester who is a participant: `ParticipantSummary(UUID participantId, Role role, Boolean attended)`; `null` otherwise (allows players to render their own attendance). Signature becomes `getGameDetail(UUID gameId, UUID viewerId)`.

### D. Completion — **internal only, no HTTP endpoint**
- `AttendanceService.completeGame(UUID gameId)` (@Transactional, locks Game row, sets `COMPLETED`, returns void), driven by the scheduler. The frontend never completes a game.

---

## 9. Authorization

| Actor | Can | Cannot |
|---|---|---|
| Player | View their own `myParticipation` (via detail); view game | Read other owners' rosters (403); modify attendance (403); complete a game (no endpoint) |
| Owner | `GET /participants`, `PATCH …/attendance` for their own games | Touch another owner's game (403 via `verifyOwner`) |

All checks use the `AuthenticatedUser` principal. Client-supplied user IDs are never trusted.

---

## 10. Transaction / concurrency design

Both `updateAttendance` and `completeGame` lock the **Game row** (`GameRepository.findByIdForUpdate`, PESSIMISTIC_WRITE) — mirroring `JoinRequestService.acceptUnderLock`. Because both critical operations serialize on the game row:

- Two owner attendance writes → serialized (end state = last write; idempotent values).
- Attendance write racing completion → whichever acquires the lock first wins; the loser observes `COMPLETED` and returns `GAME_ALREADY_COMPLETED`. **No attendance after completion.**
- Scheduler double-run / manual retry → second sees `COMPLETED`/`CANCELLED` and skips.
- Read-only routes (`GET /participants`, game detail) take no locks (MVCC snapshot).
- No new locking beyond the existing game-row lock — no per-participant locks.

---

## 11. Database / migration changes

**No migration required.** All needed data (attendance column, status check values, uniqueness, indexes) already exists in V1–V3. No new columns, constraints, or indexes are introduced.

---

## 12. Backend implementation plan

Grace period is a single constant resolved from a property (default `Duration.ofHours(6)`), read once at scheduler/service startup. All time comparisons use `Clock`/`Instant.now()` consistently.

**New files**
- `game/dto/ParticipantResponse.java` — record (see §8).
- `game/dto/ParticipantSummary.java` — `(participantId, role, attended)`.
- `game/dto/AttendanceRequest.java` — `(@NotNull Boolean attended)`.
- `game/service/AttendanceService.java` — `getParticipants`, `updateAttendance`, `completeGame`; private `verifyOwner`, `assertAttendable`, `isWithinAttendanceWindow`, mapper (`displayName`, profile fields via `UserRepository`).
- `game/controller/AttendanceController.java` — `@RequestMapping("/api/games/{gameId}")`, two methods: `GET /participants`, `PATCH /participants/{participantId}/attendance`. (By analogy with `JoinRequestController`.)
- `game/scheduler/GameCompletionScheduler.java` — `@Component`, `@Scheduled(fixedDelay = 60_000, initialDelay = 30_000)`, scans via `GameRepository.findByStatusInAndEndTimeBefore(statuses, now.minus(grace))`, calls `attendanceService.completeGame(id)` per game (per-game transaction).
- `config/SchedulingConfiguration.java` — `@Configuration @EnableScheduling` (only new config; no infra).

**Modified files**
- `game/repository/MatchParticipantRepository.java` — add `findByGameIdOrderByJoinedAtAsc`, `findByGameIdAndUserId` (Optional), `findByIdAndGameId` (Optional).
- `game/repository/GameRepository.java` — add `List<Game> findByStatusInAndEndTimeBefore(Collection<GameStatus> statuses, Instant endTimeBefore)` (used by scheduler with `now - grace`).
- `game/dto/GameDetailResponse.java` — add `ParticipantSummary myParticipation` component.
- `game/service/GameService.java` — `getGameDetail(gameId, viewerId)` maps `myParticipation`.
- `game/controller/GameController.java` — pass `principal.id()` into `getGameDetail`.
- `profile/repository/PlayerStatsRepository.java` — attended-only semantics (§7).

**None of the Phase 6 join-request code is touched.**

---

## 13. Frontend implementation plan

**New files**
- `lib/api/participants.ts` — `getGameParticipants(gameId)`, `setParticipantAttendance(gameId, participantId, attended)` (follows `apiFetch` pattern).
- `components/games/AttendancePanel.tsx` — owner roster: avatar/initials, name, skill, `Host` tag for `OWNER` role, Present/Absent toggle per row with per-row in-flight loading, disabled-on-race protection, empty/loading/error states; notes "Attendance is open during the game and for 6 hours after the end time, then the game completes automatically." Handles `GAME_ALREADY_COMPLETED`/`GAME_NOT_STARTED`/`PARTICIPANT_NOT_FOUND` with friendly messages + resync.
- `components/games/GameStatusBadge.tsx` — pill for `OPEN/FULL/IN_PROGRESS/COMPLETED/CANCELLED`.

**Modified files**
- `lib/types.ts` — add `ParticipantDetail` (roster row), `ParticipationSummary`; extend `GameDetail` with `myParticipation: ParticipationSummary | null`.
- `app/(app)/games/[id]/page.tsx` — add `GameStatusBadge` in header; for the **owner** render `AttendancePanel` after `JoinRequestsPanel` (controls client-side enabled after `startTime`, server authoritative); for a **participant player** show a small card from `game.myParticipation` ("You attended / You were marked absent / Not marked yet") + completed status note. Keep `refreshGame()` used to re-sync after attendance edits.

**Explicitly out of scope (frontend):** no `/profile`, `/my-games`, or `/profile/[id]` pages (they don't exist today); no polling/auto-refresh of status; no redesign. Player stats have no page of their own in this phase — see §16.

---

## 14. Test plan

New `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import(TestcontainersConfiguration.class)` tests following `GameDiscoveryIntegrationTest` conventions (register user via `/api/auth/register`, cookie auth, direct DB inserts for custom times, `@BeforeEach` cleanup with `JdbcTemplate`).

**`AttendanceIntegrationTest`**
- Owner lists roster (owner + accepted players; pending/rejected requests absent).
- Non-owner `GET /participants` → 403; anonymous → 401.
- Owner marks `attended=true` and `false` → 200 + updated DTO.
- Non-owner PATCH → 403.
- PATCH for a participant not in the game → 404 `PARTICIPANT_NOT_FOUND`; unknown game → 404 `GAME_NOT_FOUND`.
- PATCH with future `startTime` → 409 `GAME_NOT_STARTED`.
- PATCH within post-`endTime` grace window (game not yet completed) → 200 (proves grace window works).
- PATCH after completion → 409 `GAME_ALREADY_COMPLETED` (proves the lock/race guard).
- Missing `attended` in body → 400 `VALIDATION_ERROR`.

**`GameCompletionIntegrationTest`** (calls `attendanceService.completeGame` / the scheduler method directly — no waiting on real ticks)
- Past `endTime + grace` OPEN game → COMPLETED.
- Game past `endTime` but **still inside grace** → NOT completed (grace respected).
- Future-`endTime` game → untouched.
- FULL and IN_PROGRESS past games → COMPLETED.
- Already COMPLETED / CANCELLED → skipped.
- After completion, attendance PATCH → 409.

**Statistics (via `GET /api/users/me` after DB setup)**
- Completed + `attended=true` → `matchesPlayed=1`, `attendanceRate≈1.0`.
- Completed + `attended=false` → `matchesPlayed=0`, rate `0.0` (denominator 1).
- Completed + unmarked (`NULL`) → same as absent.
- Non-completed game with attendance → no effect (zeros).
- Owner participant counted when attended.
- Concurrency: sequential completion-then-PATCH yields `GAME_ALREADY_COMPLETED` (lock-ordering coverage).

---

## 15. Edge cases

- Owner + players roster; owner row markable; unmarked owner → no self-credit.
- Game ends with unmarked players → they get no played credit (per decision #3).
- Owner pages left open across completion → status flips on next fetch/refresh (`refreshGame`).
- Duplicate PATCH (same bool) → idempotent.
- Scheduler vs attendance race → serialized on game-row lock.
- Game deleted/nonexistent during scheduler run → `completeGame` skips missing rows.
- No `/my-games` → completed games only reachable by direct link/detail URL (documented limitation, out of scope).
- Timezone: `Instant` everywhere; server TZ (`Asia/Kolkata`) irrelevant to scheduling math.

---

## 16. Out of scope (kept for future phases)

Ratings/reputation, notifications, chat, payments, turf booking, teams, tournaments, recommendations, subscriptions, Redis, WebSockets, microservices, dashboards, player ranking, participant removal, join-request cancellation, attendance null-reset, and the frontend **`/profile` / `/my-games` pages** (stats currently surface only via the existing profile APIs and the game detail participation card).

---

## 17. Acceptance criteria

1. Owner can list accepted participants on a game and mark each Present/Absent.
2. Attendance is allowed during the game and within the 6h grace window; it is locked after the scheduled completion; non-owners get 403; strangers to the game's roster can't be marked.
3. Games auto-complete once `endTime + grace` passes; already-completed/cancelled games are never re-completed.
4. A player's `matchesPlayed/matchesCompleted` count only attended games; `attendanceRate` reflects skips and unmarked records.
5. `GET /api/users/me` and `/api/users/{id}` return updated stats with zero profile code changes.
6. Game detail shows status (badge), owner attendance panel, and each participant's own participation/attendance.
7. `npm run lint` and `npm run build` pass; `./gradlew test` passes.

---

## 18. Risks / tradeoffs

- **Grace period is the mitigation** for the narrow auto-only window: marking stays open ~6h after `endTime`; completion follows after. Constant is configurable.
- **Scheduler is new infra** (Spring only, no external deps): first scheduled component in the repo; needs `@EnableScheduling`. Failure modes are idempotent.
- **Unmarked players get zero played-credit** even if they did play (owner never marked) — accepted tradeoff from decision #3; stats stay conservative.
- **`matchesPlayed` semantics change** also alters the Phase 6 owner panel "Matches" (now attended-only) — intentional and confirmed.
- **No profile page** means stats aren't shown to players in the UI this phase — acknowledged gap deferred to a frontend phase.

---

## 19. Files likely to be created / modified

**Backend**
- Create: `game/dto/ParticipantResponse.java`, `game/dto/ParticipantSummary.java`, `game/dto/AttendanceRequest.java`, `game/service/AttendanceService.java`, `game/controller/AttendanceController.java`, `game/scheduler/GameCompletionScheduler.java`, `config/SchedulingConfiguration.java`, tests `AttendanceIntegrationTest.java`, `GameCompletionIntegrationTest.java`.
- Modify: `game/repository/GameRepository.java`, `game/repository/MatchParticipantRepository.java`, `game/dto/GameDetailResponse.java`, `game/service/GameService.java`, `game/controller/GameController.java`, `profile/repository/PlayerStatsRepository.java`.

**Frontend**
- Create: `lib/api/participants.ts`, `components/games/AttendancePanel.tsx`, `components/games/GameStatusBadge.tsx`.
- Modify: `lib/types.ts`, `app/(app)/games/[id]/page.tsx`.

---

## 20. Implementation order

1. Backend DTOs + repository helpers.
2. `GameDetailResponse.myParticipation` (DTO + `GameService` + `GameController`).
3. `AttendanceService` + `AttendanceController` (roster + PATCH with game-row lock + grace-window check).
4. `PlayerStatsRepository` attended-only semantics.
5. Scheduling: `SchedulingConfiguration` + `GameCompletionScheduler` + `completeGame` (grace-aware scan).
6. Backend integration tests.
7. Frontend types + `lib/api/participants.ts`.
8. `GameStatusBadge` + `AttendancePanel` + page integration.
9. `npm run lint`, `npm run build`, `./gradlew test`; review diff for scope.