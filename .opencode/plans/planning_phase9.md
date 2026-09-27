# GameConnect Phase 9 — My Games & Game History

**Status:** PLAN ONLY — nothing in this document has been implemented.
**Source of truth:** the repository at commit `d947e9b` ("adding phase 8 - player rating").
**Related:** `.opencode/plans/planning_phase8.md` (rating system), `.opencode/plans/planning_phase7.md` (attendance + completion).

---

## 1. Current-state audit

### 1.1 Backend — game domain

| Concern | Location | Actual behaviour |
|---|---|---|
| `Game` entity | `game/entity/Game.java:23` | UUID id, `ownerId`, `gameDate` (`LocalDate`), `startTime`/`endTime` (`Instant`), `format`, `skillLevel`, `maximumPlayers`, `status`, `createdAt`. No `completedAt`. |
| Status enum | `game/entity/Game.java:70-72` | `OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED` |
| Discovery query | `game/repository/GameRepository.java:33-51` | `status = :status AND startTime >= :now`, optional date/format/skill/`q`, `ORDER BY startTime ASC` |
| Discovery service | `game/service/GameService.java:148-192` | `listOpenGames(...)`, hardcoded `GameStatus.OPEN`, `PageRequest.of(page, size)` |
| Game detail service | `game/service/GameService.java:195-239` | `getGameDetail(gameId, viewerId)` — **no authorization check at all** |
| Game detail controller | `game/controller/GameController.java:61-66` | `GET /api/games/{id}`, principal id passed to the mapper only |
| Completion scheduler | `game/scheduler/GameCompletionScheduler.java:31-40` | every 60s; `findByStatusInAndEndTimeBefore([OPEN, FULL, IN_PROGRESS], now - grace)` → `completeGame` |
| Completion service | `game/service/AttendanceService.java:80-94` | pessimistic-write lock on the game row, then `status = COMPLETED` |
| Attendance window | `game/service/AttendanceService.java:96-125` | markable from `startTime` to `endTime + grace`; grace = `6h` (`application.yml:23-24`) |

### 1.2 Backend — participation domain

- `GameService.createGame` creates the owner as a `MatchParticipant` with `role = OWNER` in the same transaction — `game/service/GameService.java:86-91`.
- `JoinRequestService.acceptUnderLock` creates a `MatchParticipant` with `role = PLAYER` — `game/service/JoinRequestService.java:198-203`.
- `MatchParticipant` — `game/entity/MatchParticipant.java:16`: `gameId`, `userId`, `role (PLAYER|OWNER)`, `attended (Boolean, nullable)`, `joinedAt`.
- `JoinRequest` — `game/entity/JoinRequest.java:16`: `gameId`, `userId`, `status (PENDING|ACCEPTED|REJECTED|CANCELLED)`, `createdAt`, `decidedAt`.

**Consequence:** `match_participant` is already the single authoritative "this user is involved in this game" record, and it carries `role` **and** `attended` — exactly the two things My Games needs.

### 1.3 Backend — rating (Phase 8)

- `GET /api/games/{gameId}/ratings` / `GET .../ratings/eligible` — `rating/controller/PlayerRatingController.java:33-38`
- `POST /api/games/{gameId}/ratings` — `rating/controller/PlayerRatingController.java:40-47`
- Guards: `assertCompleted` (must be `COMPLETED`) and `requireEligibleRater` (must have a `match_participant` row **and** `attended = TRUE`) — `rating/service/PlayerRatingService.java:142-161`.
- **Neither endpoint performs any game-access authorization beyond participation.** So a completed game reached from My Games already works: the detail fetch succeeds, eligibility returns 200, submission returns 201.

### 1.4 Frontend

- `app/(app)/games/page.tsx` — discovery. Loading / error banner / empty state / Previous–Next pager, all `useState`+`useEffect`, no library.
- `app/(app)/games/[id]/page.tsx` — detail. Rating panel gate at `:230-234`:
  `user != null && game.status === "COMPLETED" && game.myParticipation?.attended === true`.
  Back link at `:156-158` is hardcoded to `/games`.
- `components/games/GameStatusBadge.tsx` — already renders all 5 statuses.
- `components/games/GameCard.tsx` — discovery card, takes `GameSummary`.
- `components/games/GameRatingPanel.tsx` — Phase 8 rating UI, standalone component keyed only on `gameId`.
- `components/layout/BottomNav.tsx:8-13` — **already links to `/my-games` and `/profile`.**
- `proxy.ts:7` — **already lists `/my-games` in `PROTECTED_PATHS`.**

### 1.5 Database / migrations

- Flyway history: **V1, V2, V3 only.** `V1__init.sql`, `V2__add_position_to_user.sql`, `V3__add_game_discovery_index.sql`. Next version, if ever needed, is **V4**.
- `match_participant` (V1:71-83): `uq_participant UNIQUE (game_id, user_id)`, `idx_participant_user ON (user_id)`.
- `join_request` (V1:55-69): `uq_req_game_user UNIQUE (game_id, user_id)`, `idx_join_request_user ON (user_id, status)`.
- `game` indexes: `idx_game_status`, `idx_game_date`, `idx_game_owner`, `idx_game_skill_fmt`, `idx_game_window (start_time, end_time)` (V1:49-53) and `idx_game_status_start (status, start_time)` (V3:1).
- `player_rating` (V1:85-99): `uq_rating (game_id, rater_id, ratee_id)`, `idx_player_rating_ratee (ratee_id)`.
- Hibernate `ddl-auto: validate` (`application.yml:11`) — schema and entities must stay in lockstep.

---

## 2. Exact problem identified

1. **A game vanishes from the product once its start time passes.** `findDiscoveryGames` requires `startTime >= :now` **and** `status = OPEN`. The only navigation surface is the bottom nav, whose `/my-games` link resolves to a **404** because `app/(app)/my-games/page.tsx` does not exist.
2. **Consequence — the Phase 8 rating flow is effectively unreachable by normal use.** `GameRatingPanel` lives only on `app/(app)/games/[id]/page.tsx`, and a completed game is absent from discovery. To rate, a player must already have the game's UUID bookmarked.
3. **No history.** A player cannot review past games, which `PRODUCT.MD:32` and `PRODUCT.MD:68` list as MVP requirements.
4. **A 6-hour blind window.** Between `startTime` and `endTime + 6h` the game is still `OPEN`/`FULL` with a past `startTime`, so it is already gone from discovery while attendance is still being marked.
5. **`IN_PROGRESS` is dead code.** The enum value is read by `AttendanceService.java:105` and `GameCompletionScheduler.java:35`, but **no production code ever assigns it.** Games go `OPEN`/`FULL` → `COMPLETED`. `CANCELLED` is likewise never assigned (no cancel feature exists).

**The fix is a new surface, not a change to discovery.** `/games` keeps its single job: "what can I join?". `My Games` gets a new job: "what am I involved in?".

---

## 3. Product behavior

### 3.1 Definition of "my game"

A game belongs to the authenticated user's My Games list **iff** the user has a `match_participant` row for it, **or** the user is `game.owner_id` (defensive fallback for data written outside `createGame`).

| Relationship | In My Games? | Reason |
|---|---|---|
| Owner | ✅ | Owner is also a `match_participant` with `role = OWNER`. |
| ACCEPTED → participant row | ✅ | Became a participant at accept time. |
| PENDING join request | ❌ | Not yet involved. Still discoverable while `OPEN` + upcoming, and the request state is visible on the game detail page via the existing `GET /api/games/{gameId}/join-requests/me` (`JoinRequestController.java:43-48`). |
| REJECTED join request | ❌ | Explicitly not involved. |
| CANCELLED join request | ❌ | Explicitly withdrawn. |
| Unrelated user | ❌ | Isolation. |

**Decided with the user:** pending/rejected/cancelled requests are excluded from My Games.

### 3.2 Categories

Derived **purely from `status`** — no time arithmetic, so there is no second source of truth that can disagree with the scheduler.

| Category | Statuses | Tab label |
|---|---|---|
| `UPCOMING` | `OPEN`, `FULL` | Upcoming |
| `IN_PROGRESS` | `IN_PROGRESS` | In progress |
| `COMPLETED` | `COMPLETED` | Completed |
| `CANCELLED` | `CANCELLED` | Cancelled |

4 tabs map 1:1 onto the 4 API categories, so the frontend needs no special-casing and no hidden remapping. Each tab's empty state is one line.

**Documented limitation:** because nothing sets `IN_PROGRESS` (§2.5), the In-progress tab is empty in practice, and a game whose start time has passed but which the scheduler has not yet completed sits under **Upcoming**. This was confirmed with the user: the Phase 7 scheduler is left untouched. Fixing the missing transition is deferred, not forgotten.

### 3.3 Invariants

- A game **never** disappears from My Games because its start time passed.
- `/api/games` is unchanged: still `OPEN` + `startTime >= now` only. Started and completed games do **not** leak into discovery.
- No second rating system, no duplicated Phase 8 API.

---

## 4. Backend API changes

### 4.1 `GET /api/games/my`

Added to the existing `GameController` (`@RequestMapping("/api/games")`) — the natural home, no new controller class.

```
GET /api/games/my
```

| Property | Value |
|---|---|
| Auth | Required. Derived from the `AuthenticatedUser` principal. |
| `userId` param | **Never accepted.** There is no way to ask for another user's games. |
| `page` | Optional, default `0`, min `0`. `page < 0` → 400 `VALIDATION_ERROR`. |
| `size` | Optional, default `20`, range `1..100`. Out of range → 400 `VALIDATION_ERROR`. |
| `category` | Optional, default `ALL`. One of `ALL`, `UPCOMING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. Unknown value → 400 `VALIDATION_ERROR`. |
| Success | `200` with `PagedMyGamesResponse` (§4.3). |
| Unauthenticated | `401` `UNAUTHORIZED` (from `SecurityConfiguration.java:46-57`, before the controller runs). |

**Why `category` and not `status`:** `status` is a single enum value, but `UPCOMING` must mean `OPEN` **or** `FULL`, and `UPCOMING` is not a real status. A separate `category` parameter is honest about the mapping and keeps `PagedGamesResponse` metadata meaningful per tab (the frontend can show "Page 1 of 3" *inside* a tab). Every item **also** carries its raw `status`, so a client could group client-side instead. Both options from the brief are satisfied with one endpoint and no specialized routes.

**Why `/api/games/my` is safe next to `GET /api/games/{id}`:** Spring's `RequestMappingInfo` pattern comparator ranks the literal segment `my` above the `{id}` variable pattern, so `/api/games/my` binds to the new mapping. A regression test (§11) asserts this explicitly.

### 4.2 `MyGameCategory` (new enum, `game/dto/`)

`ALL, UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED`. Serialized as a plain string, matching how `GameStatus` is exposed.

### 4.3 `MyGameSummaryResponse` (new record, `game/dto/`)

A flat superset of `GameSummaryResponse` plus participation context. Flat rather than nested so the card component reads `item.turfName` exactly like `GameCard` reads `game.turfName`. The field overlap is a DTO declaration cost only — no duplicated runtime data or extra queries.

```
UUID   id
String turfName
String turfAddress
LocalDate gameDate
Instant startTime
Instant endTime
GameFormat format
SkillLevel skillLevel
int    maximumPlayers
int    currentPlayers
int    spotsRemaining
Integer joiningFee
GameStatus   status          // raw lifecycle status
MyGameCategory category      // derived tab bucket
MatchParticipant.Role myRole  // OWNER | PLAYER  (never null)
Boolean myAttended           // from match_participant.attended
boolean canRate              // status == COMPLETED && myAttended == TRUE
```

`canRate` is exactly the predicate the detail page already uses to mount `GameRatingPanel` (`games/[id]/page.tsx:230-234`), so the card can advertise rating availability without a second request. A "N players still to rate" counter is deliberately **not** included — it would need an extra aggregate query per page and `GameRatingPanel` already renders per-player rated/unrated state.

### 4.4 `PagedMyGamesResponse` (new record, `game/dto/`)

```
List<MyGameSummaryResponse> content
int page, int size
long totalElements
int totalPages
boolean first, last
```

Field-for-field identical to `PagedGamesResponse` (`game/dto/PagedGamesResponse.java:5-13`), which keeps the frontend contract and the pagination control copy-pasteable. A generic `PagedGamesResponse<T>` refactor would touch Phase 4/5 code and is explicitly deferred.

### 4.5 `MyGamesService` (new, `game/service/`)

A separate service, matching the existing one-concern-per-service layout (`AttendanceService`, `JoinRequestService`, `PlayerRatingService`). Owns `DEFAULT_PAGE_SIZE = 20` and `MAX_PAGE_SIZE = 100` locally, mirroring `GameService.java:40-41` without cross-class coupling.

```java
@Transactional(readOnly = true)
public PagedMyGamesResponse listMyGames(AuthenticatedUser principal,
                                        int page, int size,
                                        MyGameCategory category)
```

Mapping, per page (3 queries total, **no N+1**):
1. `gameRepository.findMyGames(userId, statuses, PageRequest.of(page, size, sort))`
2. `matchParticipantRepository.countByGameIds(gameIds)` — reuse the existing batched projection at `MatchParticipantRepository.java:30-36`
3. `matchParticipantRepository.findByUserIdAndGameIdIn(userId, gameIds)` — new, one query for the whole page

`myRole` resolution: the participant row's role; if the user has no row but `game.ownerId` matches, fall back to `OWNER` (covers the defensive `ownerId` branch of the query).

`canRate` = `status == COMPLETED && Boolean.TRUE.equals(myAttended)`.

---

## 5. Database / query changes

### 5.1 Query — no new table, no duplicates

Added to `GameRepository`:

```java
@Query("""
        SELECT g FROM Game g
        WHERE (g.ownerId = :userId
               OR EXISTS (SELECT mp.id FROM MatchParticipant mp
                          WHERE mp.gameId = g.id AND mp.userId = :userId))
          AND (:statuses IS NULL OR g.status IN :statuses)
        """)
Page<Game> findMyGames(@Param("userId") UUID userId,
                       @Param("statuses") Collection<GameStatus> statuses,
                       Pageable pageable);
```

Added to `MatchParticipantRepository` (derived query, no `@Query` needed):

```java
List<MatchParticipant> findByUserIdAndGameIdIn(UUID userId, Collection<UUID> gameIds);
```

### 5.2 Why `EXISTS` and not a `JOIN`

- **No duplicates, by construction.** The `OR g.ownerId = :userId` branch and the participant branch can both match, but because the participant check is an `EXISTS` **subquery** rather than a `FROM MatchParticipant mp JOIN`, the `game` row is never multiplied. A `JOIN match_participant` would emit the owner's game twice — precisely the Phase 4 trap called out in the brief.
- The trap is doubly safe here: `uq_participant UNIQUE (game_id, user_id)` (V1:80) makes it *impossible* for a user to hold both an `OWNER` and a `PLAYER` row on the same game.
- `ownerId = :userId` is retained as a defensive `OR` so an owner always finds their own game even if a `match_participant` row is ever missing.
- The subquery predicate `(game_id, user_id)` is covered exactly by `uq_participant`; the reverse direction `user_id` is covered by `idx_participant_user`. Both already exist.

### 5.3 Sorting — deterministic in every case

`Sort` is built in `MyGamesService` and passed via `PageRequest`, so one repository method serves all categories. `id` is always the final tiebreaker, and `id` is the UUID primary key, so **every ordering is a total order** — no reliance on unspecified database ordering.

| `category` | `statuses` | Sort |
|---|---|---|
| `UPCOMING` | `{OPEN, FULL}` | `startTime ASC, id ASC` — nearest upcoming first; an overdue game naturally sorts to the top of the tab |
| `IN_PROGRESS` | `{IN_PROGRESS}` | `startTime ASC, id ASC` — earliest-started active game first |
| `COMPLETED` | `{COMPLETED}` | `startTime DESC, id ASC` — most recently played first |
| `CANCELLED` | `{CANCELLED}` | `startTime DESC, id ASC` |
| `ALL` (default) | *(no filter)* | `startTime DESC, id ASC` — deterministic; the frontend always passes an explicit `category`, so this is an API-consumer fallback, not the primary UX |

There is **no `completed_at` column** (§1.5), so `startTime DESC` is the recency proxy. This matches the brief's "most recently completed / recently started first" and needs no migration. If `completed_at` is ever added, it would be `V4`.

### 5.4 Migration decision

**No migration is required.** Every predicate and sort key is already indexed:

| Need | Existing index |
|---|---|
| `g.ownerId = ?` | `idx_game_owner` (V1:51) |
| participant subquery `(game_id, user_id)` | `uq_participant` (V1:80) |
| participant subquery `user_id` direction | `idx_participant_user` (V1:83) |
| `g.status IN (...)` | `idx_game_status` (V1:49), `idx_game_status_start` (V3:1) |
| `ORDER BY start_time` | `idx_game_window` (V1:53) |

**Phase 9 creates zero migrations.** If a later phase needs one, the correct next version is `V4__<snake_case_description>.sql` — never named after a product phase, and never `V9`/`V10`.

---

## 6. Authorization rules

### 6.1 `GET /api/games/my`

The principal is the only source of identity. There is no `userId` request parameter and no cross-user read path. Isolation is structural: the query is always `WHERE ... mp.userId = :principalId`.

### 6.2 `GET /api/games/{id}` — access matrix

Today this endpoint performs **no** authorization (`GameService.java:195-239`): any authenticated user can fetch any game by UUID, including completed ones. **Confirmed with the user:** Phase 9 keeps `OPEN`/`FULL` browsable (discovery depends on it) and restricts the remaining statuses.

| Viewer | `OPEN` / `FULL` | `IN_PROGRESS` / `COMPLETED` / `CANCELLED` |
|---|---|---|
| Owner | 200 | 200 |
| Accepted participant (`match_participant` row) | 200 | 200 |
| PENDING requester | 200 | 200 |
| REJECTED requester | 200 | **403** |
| CANCELLED requester | 200 | **403** |
| Unrelated authenticated user | 200 | **403** |
| Unauthenticated | 401 | 401 |

Implementation — a new private guard in `GameService.getGameDetail`:

```java
private void assertCanView(Game game, UUID viewerId) {
    if (game.getStatus() == GameStatus.OPEN || game.getStatus() == GameStatus.FULL) {
        return;
    }
    if (game.getOwnerId().equals(viewerId)
            || matchParticipantRepository.existsByGameIdAndUserId(game.getId(), viewerId)
            || joinRequestRepository.existsByGameIdAndUserIdAndStatus(
                    game.getId(), viewerId, RequestStatus.PENDING)) {
        return;
    }
    throw new BusinessException(HttpStatus.FORBIDDEN, "GAME_ACCESS_DENIED",
            "You do not have access to this game");
}
```

`existsByGameIdAndUserIdAndStatus` already exists at `JoinRequestRepository.java:21` — no new query is needed for the pending branch. This is a **security improvement**, not a regression: access strictly narrows for `IN_PROGRESS`/`COMPLETED`/`CANCELLED` games and is byte-identical for `OPEN`/`FULL`.

No other endpoint changes. `AttendanceService` is already owner-only (`AttendanceService.java:135-141`), and `PlayerRatingService.requireEligibleRater` already requires a participant row with `attended = TRUE`.

---

## 7. Game-detail access changes

**None beyond the guard in §6.2.** `GameDetailResponse` and its `myParticipation` field are unchanged, and `myParticipation` (`ParticipantSummary(participantId, role, attended)` — `game/dto/ParticipantSummary.java:7`) is what already feeds the rating gate on the detail page.

Consequences to note:
- A rejected requester who bookmarked a completed game now gets 403 instead of 200. Intentional and consistent with the chosen matrix.
- A player holding a detail page open across an `OPEN → COMPLETED` transition who was never a participant can hit 403 on the next refetch (`refreshGame` in `games/[id]/page.tsx:98-104`). Acceptable; the page keeps its current data and the error banner explains it.
- The `404 GAME_NOT_FOUND` path is unchanged and still precedes the 403.

---

## 8. Frontend — My Games architecture

### 8.1 New route

`app/(app)/my-games/page.tsx` — currently a **404**. Placing it inside the `(app)` group inherits the authenticated shell (`app/(app)/layout.tsx`: `max-w-md` column + `BottomNav`) and the `proxy.ts` auth guard automatically.

### 8.2 Page structure

```
My Games
[ Upcoming ] [ In progress ] [ Completed ] [ Cancelled ]

── active tab ──
  count line ("3 upcoming games")
  [MyGameCard] …
  Previous · Page 1 of 2 · Next
```

- `"use client"`, `useState` + `useEffect` with a `cancelled` flag, exactly as `app/(app)/games/page.tsx:28-60`.
- Local `const [category, setCategory] = useState<MyGameCategory>("UPCOMING")` and `const [page, setPage] = useState(0)`. Changing tab resets `page` to `0` and clears the error.
- Each tab is an independent request: `listMyGames({ page, size: 20, category })`.
- Tab labels are shortened on small screens (`In progress` → `In progress`; no truncation needed at `max-w-md`).

### 8.3 `MyGamesTabs` (`components/games/MyGamesTabs.tsx`)

Horizontal, scrollable if needed, `role="tablist"` with `aria-selected`. Active tab: emerald (`text-emerald-600`, matching `BottomNav.tsx:43`) with an underline indicator. Follows the visual language of `GameFilters.tsx` / `GameStatusBadge.tsx`. No new dependency, no state library.

### 8.4 `MyGameCard` (`components/games/MyGameCard.tsx`)

Whole card is a `Link` to `/games/{id}?from=my-games`, exactly like `GameCard.tsx:14-18`. Shows:

- `turfName` + `turfAddress`, `format` chip, `GameStatusBadge status`
- `formatIST(startTime)` (`lib/format.ts:36`), `SKILL_LABELS[skillLevel]`, `{currentPlayers}/{maximumPlayers} players`
- `myRole`: `OWNER` → "Host" chip; `PLAYER` → "Playing" chip
- `myAttended`: only when `status === "COMPLETED"` — "Attended" / "Absent" / "Not marked"
- **Completed tab only:** when `canRate`, an amber "Rate players" hint chip. It is **not** an interactive control — the card navigates, and `GameRatingPanel` on the detail page remains the single rating surface. This is the brief's §13 requirement: advertise, don't duplicate.
- CANCELLED cards use the existing `GameStatusBadge` "Cancelled" styling.

### 8.5 Empty / loading / error states

| State | Rendering |
|---|---|
| Loading | `Loading your games…` (mirrors `games/page.tsx:104`) |
| Error | `bg-red-50 … text-red-700` banner with `ApiError.message` (mirrors `:97-101`) |
| Upcoming empty | "You don't have any upcoming games." + hint "Browse Games to find one." |
| In progress empty | "No games are currently in progress." |
| Completed empty | "No completed games yet." |
| Cancelled empty | "No cancelled games." |
| Page beyond range | Previous/Next disabled via `first` / `last` from the response |

### 8.6 Navigation

**No navigation component changes are required.** `BottomNav.tsx:11` already contains `{ href: "/my-games", label: "My Games" }` and `proxy.ts:7` already lists `/my-games` in `PROTECTED_PATHS`. `isActive` (`BottomNav.tsx:21-22`) works correctly because `"/my-games".startsWith("/games")` is `false` and `"/games".startsWith("/my-games")` is `false`, so the two tabs never highlight each other.

Creating the route **is** the navigation fix. The bottom nav stays 5 items wide (`max-w-md`, `justify-around`) — no redesign, no new items, no removed items.

> Pre-existing and out of scope: the `/profile` nav link is also a 404. See §15.

### 8.7 Detail-page back navigation (small, required)

`app/(app)/games/[id]/page.tsx:156-158` hardcodes `← Back to games`. Coming from My Games that link drops the user into discovery, where the game is not listed — the exact dead-end this phase exists to remove.

- My Games cards link to `/games/{id}?from=my-games`.
- The detail page reads `from` via `useSearchParams()` and renders `← Back to My Games` / `← Back to games` accordingly. The **error** and **not-found** branches get the same treatment.
- Discovery cards keep linking to `/games/{id}` — unchanged.

---

## 9. Phase 8 rating integration

**Reuse only. No new rating endpoint, no new DTO, no new service.**

```
My Games → Completed tab → MyGameCard ("Rate players" hint)
   → /games/{id}?from=my-games
   → GameDetail (games/[id]/page.tsx)
   → existing gate: status == COMPLETED && myParticipation.attended === true
   → GET /api/games/{id}/ratings/eligible   (Phase 8, unchanged)
   → <GameRatingPanel gameId>               (Phase 8, unchanged)
   → POST /api/games/{id}/ratings           (Phase 8, unchanged)
```

Verification against the real code:

| Requirement | Status | Evidence |
|---|---|---|
| Completed game fetchable by an authorized participant/owner | ✅ already | `GameService.getGameDetail` returns 200; the new guard permits participants on `COMPLETED` |
| Eligible-player endpoint works for that game | ✅ already | `PlayerRatingController.java:33-38` → `PlayerRatingService.getEligibleRatings`; only requires `COMPLETED` + participant + `attended = TRUE` |
| Rating submission works | ✅ already | `PlayerRatingController.java:40-47` → `createRating`; all Phase 8 rules enforced |
| Phase 8 rules unchanged | ✅ | No file under `rating/` is modified |
| **Any required change?** | **None** | — |

The only Phase 9 work on the rating path is the **navigation entry point**: `canRate` on the card, the `?from=my-games` back link, and the new route. The rating backend already did the right thing; it was simply unreachable.

---

## 10. Tests

New file `backend/src/test/java/com/gameconnect/game/MyGamesIntegrationTest.java`, following `GameDiscoveryIntegrationTest` exactly: `@Import(TestcontainersConfiguration.class)`, `@SpringBootTest`, `@AutoConfigureMockMvc`, `registerAndAuth(...)` helper, `authCookie(...)`, `insertGame(...)` / `insertParticipant(...)` helpers, and the FK-safe cleanup order in `@BeforeEach` (`player_rating → match_participant → join_request → game → app_user`).

**Authentication**
- `myGames_withoutAuth_returns401`

**Ownership / participation**
- `myGames_owner_seesOwnedGame_withOwnerRole`
- `myGames_acceptedParticipant_seesGame_withPlayerRole`
- `myGames_participant_seesOwnParticipation_attendedFlag`

**Pending / rejected / cancelled** (per the decided rule)
- `myGames_pendingRequester_doesNotSeeGame`
- `myGames_rejectedRequester_doesNotSeeGame`
- `myGames_cancelledRequester_doesNotSeeGame`

**Isolation**
- `myGames_unrelatedUser_doesNotSeeOtherUsersGame`
- `myGames_never_acceptsUserIdParameter` — `?userId=<other>` is ignored; result set is unchanged

**Lifecycle**
- `myGames_upcomingOpenGame_appears_underUpcoming`
- `myGames_fullGame_appears_underUpcoming`
- `myGames_inProgressGame_appears_underInProgress`
- `myGames_completedGame_appears_underCompleted`
- `myGames_cancelledGame_appears_underCancelled`
- `myGames_statusNotInCategory_isExcluded`

**The regression this phase exists for**
- `myGames_completedGameWithPastStartTime_isStillReturned` — game with `startTime` yesterday, `endTime` 7h ago, `status = COMPLETED`; assert it is returned and simultaneously assert `GET /api/games` returns `totalElements = 0` for the same user.
- `myGames_gameInAttendanceWindow_pastStartTime_stillVisible` — `status = OPEN`, `startTime` 1h ago, `endTime` 1h from now; present under Upcoming even though discovery hides it.

**Deduplication**
- `myGames_ownerHasOwnerParticipantRow_returnsGameOnce` — owner has `role = OWNER`; assert exactly 1 result and `totalElements = 1`.

**Category / filter validation**
- `myGames_unknownCategory_returns400_VALIDATION_ERROR`
- `myGames_invalidPagination_returns400_VALIDATION_ERROR` (`page=-1`, `size=0`, `size=101`, `page=abc`)

**Pagination / ordering**
- `myGames_pagination_returnsExpectedPages` — 5 completed games, `size=2` → 3 pages, correct `first`/`last`; `page=99` → empty content with correct `totalElements`
- `myGames_upcoming_orderedByStartTimeAscending`
- `myGames_completed_orderedByStartTimeDescending`
- `myGames_ordering_isStableAcrossRepeatedCalls` — same page twice, identical id sequence (proves the `id` tiebreaker)

**Game detail access** — new `GameDetailAccessIntegrationTest.java`
- `detail_owner_canViewCompletedGame`
- `detail_acceptedParticipant_canViewCompletedGame`
- `detail_pendingRequester_canViewCompletedGame`
- `detail_rejectedRequester_cannotViewCompletedGame` → 403 `GAME_ACCESS_DENIED`
- `detail_unrelatedAuthenticatedUser_cannotViewCompletedGame` → 403
- `detail_unrelatedAuthenticatedUser_canViewOpenGame` → 200 (discovery unaffected)
- `detail_unrelatedAuthenticatedUser_canViewFullGame` → 200
- `detail_inProgressGame_restrictedLikeCompleted`
- `detail_cancelledGame_restrictedLikeCompleted`
- `detail_withoutAuth_returns401`
- `detail_unknownId_returns404` (precedence over 403)

**Routing regression** (the `/my-games` vs `/{id}` concern)
- `myGames_pathIsNotCapturedByGameIdVariable` — `GET /api/games/my` returns 200 + paged body, not 400/500

**Phase 8 integration**
- `myGames_completedGameFromMyGames_isRateable` — game from `/api/games/my` → `GET /api/games/{id}/ratings/eligible` → 200
- `myGames_eligibleParticipant_canSubmitRating` — 201
- `myGames_absentParticipant_cannotSubmitRating` — 403 `RATER_ATTENDANCE_REQUIRED`
- `myGames_duplicateRating_returns409` — Phase 8 rules still enforced through the My Games path
- `myGames_nonParticipant_cannotSubmitRating` — 403 `RATER_NOT_PARTICIPANT`

**Discovery unchanged** — `GameDiscoveryIntegrationTest` needs **no edits**; its detail tests use the owner. Its existing `listing_excludesPastGames` and `listing_excludesNonOpenGames` already lock the discovery contract. Optionally add one assertion that a `COMPLETED` game the caller *participated* in is still absent from `/api/games`.

**Frontend** (no test runner exists in this repo — manual + static verification)
- `npm run lint` and `npm run build` pass in `frontend/`
- Manual: My Games renders 4 tabs; switching tabs refetches and resets to page 1
- Manual: Upcoming empty state, Completed empty state, Cancelled empty state
- Manual: Completed card shows "Rate players" only when `canRate`
- Manual: card → detail → `GameRatingPanel` lists eligible players → rating saves
- Manual: `← Back to My Games` returns to the Completed tab
- Manual: loading and error states render and clear correctly

**Full backend suite:** `cd backend; ./gradlew test` (Java 21 toolchain, `user.timezone=Asia/Kolkata`, real PostgreSQL via Testcontainers).

---

## 11. Implementation order

| # | Step | Notes |
|---|---|---|
| 1 | `game/dto/MyGameCategory.java` | enum |
| 2 | `game/dto/MyGameSummaryResponse.java` | flat superset record |
| 3 | `game/dto/PagedMyGamesResponse.java` | mirrors `PagedGamesResponse` |
| 4 | `GameRepository.findMyGames` | `EXISTS` form, no new indexes |
| 5 | `MatchParticipantRepository.findByUserIdAndGameIdIn` | derived query |
| 6 | `game/service/MyGamesService.java` | validation, category→statuses+sort, 3-query mapping |
| 7 | `GameController` — `GET /my` | `@AuthenticationPrincipal` only |
| 8 | `GameService.assertCanView` + wire into `getGameDetail` | §6.2 matrix |
| 9 | **Backend gate:** `./gradlew test` | discovery tests must still pass untouched |
| 10 | `MyGamesIntegrationTest.java` | §10 |
| 11 | `GameDetailAccessIntegrationTest.java` | §10 |
| 12 | **Backend gate:** `./gradlew test` | all green |
| 13 | `lib/types.ts` | `MyGame`, `MyGameCategory`, `MyGamesListQuery` |
| 14 | `lib/api/games.ts` | `listMyGames` |
| 15 | `components/games/MyGamesTabs.tsx` | tab bar |
| 16 | `components/games/MyGameCard.tsx` | card |
| 17 | `app/(app)/my-games/page.tsx` | page |
| 18 | `app/(app)/games/[id]/page.tsx` | `?from=` back link only |
| 19 | **Frontend gate:** `npm run lint && npm run build` | |
| 20 | Manual end-to-end walkthrough | §10 frontend list |

Steps 1-9 are independently shippable and contain the entire product fix on the API side. 10-12 harden it. 13-20 make it reachable.

---

## 12. Files likely to be created / modified

**Backend — create**
- `backend/src/main/java/com/gameconnect/game/dto/MyGameCategory.java`
- `backend/src/main/java/com/gameconnect/game/dto/MyGameSummaryResponse.java`
- `backend/src/main/java/com/gameconnect/game/dto/PagedMyGamesResponse.java`
- `backend/src/main/java/com/gameconnect/game/service/MyGamesService.java`
- `backend/src/test/java/com/gameconnect/game/MyGamesIntegrationTest.java`
- `backend/src/test/java/com/gameconnect/game/GameDetailAccessIntegrationTest.java`

**Backend — modify**
- `backend/src/main/java/com/gameconnect/game/repository/GameRepository.java` (+1 method)
- `backend/src/main/java/com/gameconnect/game/repository/MatchParticipantRepository.java` (+1 method)
- `backend/src/main/java/com/gameconnect/game/service/GameService.java` (+`assertCanView`, +1 call site)
- `backend/src/main/java/com/gameconnect/game/controller/GameController.java` (+1 mapping)

**Backend — unchanged (verified)**
- everything under `rating/` (Phase 8 is reused verbatim)
- `GameCompletionScheduler`, `AttendanceService`, `JoinRequestService` (pending/rejected exclusion is achieved purely by not querying `join_request` in My Games)
- all Flyway migrations; `db/migration/` gains **no** file

**Frontend — create**
- `frontend/app/(app)/my-games/page.tsx`
- `frontend/components/games/MyGamesTabs.tsx`
- `frontend/components/games/MyGameCard.tsx`

**Frontend — modify**
- `frontend/lib/types.ts` (+3 types)
- `frontend/lib/api/games.ts` (+`listMyGames`)
- `frontend/app/(app)/games/[id]/page.tsx` (`?from=` back link, 3 branches)

**Frontend — unchanged**
- `components/layout/BottomNav.tsx` (link already present, `isActive` already correct)
- `proxy.ts` (`/my-games` already protected)
- `components/games/GameRatingPanel.tsx`, `GameStatusBadge.tsx`, `GameCard.tsx`

---

## 13. Risks and edge cases

| Risk / edge case | Handling |
|---|---|
| `IN_PROGRESS` never assigned → In-progress tab always empty | Documented (§3.2) and confirmed with the user. Tab stays because the transition may land in a later phase. |
| `CANCELLED` never assigned → Cancelled tab always empty | Same. 1:1 with the API, no special-casing, no hidden mapping. |
| Game with a past `startTime` still `OPEN` (attendance window) shows under Upcoming | Intentional. Sorting it to the top of the tab is useful — that player needs to mark attendance. |
| Owner holds both `game.owner_id` and a `role = OWNER` participant row | Structurally impossible to duplicate — `EXISTS` subquery, not a join, plus `uq_participant`. Test: `myGames_ownerHasOwnerParticipantRow_returnsGameOnce`. |
| `my` path shadowed by `/{id}` | Spring ranks the literal higher; regression test `myGames_pathIsNotCapturedByGameIdVariable`. |
| Rejected requester bookmarked a completed game → now 403 | Intentional consequence of the confirmed matrix. |
| Detail page open across an `OPEN → COMPLETED` transition for a non-participant → 403 on refresh | Acceptable; existing data stays on screen, error banner explains. |
| Unbounded result set for a heavy user | Hard cap: `size ≤ 100`, `DEFAULT_PAGE_SIZE = 20`, always paged. No unbounded query. |
| `startTime DESC` for `category=ALL` looks odd to a human | Documented API fallback (§5.3). The frontend always sends an explicit category, so users never see it. |
| `Page` count query on an `EXISTS` subquery | Standard Spring Data behaviour; covered by the pagination tests. |
| Completed games with `attended = null` for everyone → `canRate = false` everywhere | Correct: nobody is eligible to rate, matching `PlayerRatingService.requireEligibleRater`. |
| `myAttended` is `null` for upcoming games | Rendered only on Completed cards. `Boolean`, not `boolean` — must stay nullable. |
| Index bloat | Zero new indexes, zero new tables. |
| `ddl-auto: validate` | No schema change, so no entity/schema drift risk. |

---

## 14. Explicitly out of scope

Turf booking integration · payments · chat · push notifications · teams · tournaments · subscriptions · recommendations · ranking/leaderboards · Redis · WebSockets · microservices · social feed · friends/following · **profile redesign** · advanced analytics.

Also out of scope for Phase 9, deliberately:

- **Changing the Phase 7 scheduler** to add the missing `OPEN`/`FULL → IN_PROGRESS` transition (confirmed with the user).
- **Any Phase 10 work** (frontend polish, `/profile`, `/profile/[id]`).
- **`/profile` route** — still a 404, still linked in `BottomNav`. The `ProfileController` backend (`GET /api/users/me`, `GET /api/users/{id}`, `PATCH /api/users/me`) is fully implemented and untouched. Building a profile page would let a profile redesign leak in through the nav. Left for its own phase.
- **A pending-requests list** in My Games. Pending requesters already have `GET /api/games/{gameId}/join-requests/me` and the game detail page.
- **Cancelling a game** — no write path sets `CANCELLED` today.
- **A generic `PagedGamesResponse<T>` refactor** — deferred to avoid touching Phase 4/5 code.
- **Anonymous public game access**, and any relaxation of existing authorization.
- **Additional indexes or migrations.**

---

## 15. Review decisions

1. **`IN_PROGRESS` stays dead** — the In-progress tab ships empty. Confirmed; the transition is deferred.
2. **Pending join requests excluded** from My Games. Confirmed.
3. **Game detail tightened** for `IN_PROGRESS`/`COMPLETED`/`CANCELLED` only; `OPEN`/`FULL` stay browsable by any authenticated user. Confirmed.
4. **Four tabs**, not three, so the UI is 1:1 with the API `category` values. Drop the Cancelled tab whenever you prefer a leaner surface.
5. **No migration.** The correct next Flyway version, if one is ever needed, is `V4` — never named after a product phase.
6. **The back-link `?from=my-games` query param** is the smallest fix for the detail-page dead-end; the alternative is `router.back()`, which breaks deep links and refreshes.
