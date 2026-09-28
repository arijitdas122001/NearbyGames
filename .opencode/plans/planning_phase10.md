# GameConnect Phase 10 — Profile & Player Stats

**Status:** PLAN ONLY — nothing in this document has been implemented.
**Source of truth:** the repository at commit `4bf3a83` ("adding phase-9, attendance marking").
**Related:** `.opencode/plans/planning_phase9.md` (My Games), `planning_phase8.md` (ratings), `planning_phase7.md` (attendance + completion).

**Decisions taken during planning (do not re-litigate during build):**

| # | Decision | Consequence |
|---|---|---|
| D1 | No "games missed" counter. Render `attendanceRate` instead. | **Zero backend change.** |
| D2 | Edit form exposes `displayName`, `bio`, `profileImageUrl`, `skillLevel`. **Not `position`.** | `position` renders read-only and is unset for every UI-registered user. See §15 R4. |
| D3 | Own email is read-only, sourced from `useAuth().user.email`. | Zero backend change; no cross-user leak. |
| D4 | Host contact details (email + phone) for accepted players are **deferred**. | Needs a `V4` migration; documented in §16. |

---

## 1. Current-state audit

### 1.1 Backend — the profile package is complete and unchanged in spirit

| Concern | Location | Actual behaviour |
|---|---|---|
| Controller | `profile/controller/ProfileController.java:30-46` | `GET /api/users/me`, `GET /api/users/{id}`, `PATCH /api/users/me` |
| Service | `profile/service/ProfileService.java:27-70` | `getProfile(userId)` / `updateProfile(userId, request)` |
| Principal | `profile/controller/ProfileController.java:31-33` | `@AuthenticationPrincipal AuthenticatedUser`; the user id is **never** client-supplied |
| Update mapping | `profile/service/ProfileService.java:43-64` | null-check-then-set, one branch per field |
| Response mapping | `profile/service/ProfileService.java:84-94` | id, displayName, bio, profileImageUrl, skillLevel, position, createdAt, stats |
| Empty-patch guard | `profile/service/ProfileService.java:34-39` | all-null body → `400 VALIDATION_ERROR` |
| Blank-name guard | `profile/service/ProfileService.java:43-52` | trims, then rejects empty → `400 VALIDATION_ERROR` |

**There is no `UserMapper` class** — mapping is private methods inside `ProfileService`. Do not introduce a mapper layer in this phase.

### 1.2 The exact DTO contract

`UserProfileResponse` — `profile/dto/UserProfileResponse.java:9-18`

```
id: UUID
displayName: String          (NOT NULL in DB)
bio: String                  nullable
profileImageUrl: String      nullable
skillLevel: SkillLevel       NOT NULL (BEGINNER|INTERMEDIATE|ADVANCED)
position: Position           nullable (GOALKEEPER|DEFENDER|MIDFIELDER|WINGER|STRIKER|FLEXIBLE)
createdAt: Instant           NOT NULL, immutable
stats: PlayerStatsResponse   never null
```

`PlayerStatsResponse` — `profile/dto/PlayerStatsResponse.java:3-9`

```
matchesPlayed: int
matchesCompleted: int
attendanceRate: Double       nullable, 0.0..1.0
averageRating: Double        nullable, 1..5
ratingCount: long            never null (0 when there are no ratings)
```

`UpdateProfileRequest` — `profile/dto/UpdateProfileRequest.java:12-33`

| Field | Bean validation | Service behaviour | Notes |
|---|---|---|---|
| `displayName` | `@Size(max = 80)` | trims; blank → `400 VALIDATION_ERROR` | |
| `bio` | `@Size(max = 2000)` | set as-is, no trim | `""` clears it |
| `profileImageUrl` | `@Size(max = 500)` | set as-is, **no URL validation** | `""` clears it |
| `skillLevel` | enum → `400 MALFORMED_BODY` if unknown | set as-is | |
| `position` | enum | set as-is | **not in our form — see D2** |

### 1.3 The gap that matters: `null` means "do not change"

`ProfileService.updateProfile` applies a field only when it is non-null (`ProfileService.java:43-64`). Therefore **a nullable field can never be unset** — sending `null` is indistinguishable from omitting it.

- `bio` and `profileImageUrl` can still be cleared, by sending `""` (empty string), which *is* non-null and is applied verbatim.
- The UI must therefore send `""`, never `null`, and must treat `null` / `""` / absent identically for display purposes.

Corollary that the form depends on: because the form always sends all four fields, `UpdateProfileRequest.isEmpty()` is always `false` and the `400` at `ProfileService.java:34-39` is unreachable from our UI. **Client-side blank-name validation is what prevents a `400`**, since `displayName: ""` trips the service's blank guard before the client could ever recover.

### 1.4 Email is not in the profile DTO — and must not be added

- `UserProfileResponse` has **no `email` field** (verified: `UserProfileResponse.java:9-18`).
- Email is available from `GET /api/auth/me` → `UserResponse` (`auth/controller/AuthController.java:72-91`), already fetched once at app start into `AuthProvider` (`lib/auth-context.tsx:23-36`) and typed as `AuthUser.email: string` (`lib/api/auth.ts:3-7`).
- It is also in the JWT principal (`security/JwtAuthenticationFilter.java:87-88`).
- **Do not add email to `UserProfileResponse`.** That record is shared with `GET /api/users/{id}`, which is reachable by any authenticated caller (`SecurityConfiguration.java:43` → `anyRequest().authenticated()`). Adding email there would publish every user's address. As it stands the DTO is already email-free, which is correct — leave it that way.

### 1.5 Statistics semantics (native SQL — `profile/repository/PlayerStatsRepository.java:22-72`)

Participation query (`:22-43`), scoped to games with `status = 'COMPLETED'`:

| Statistic | SQL | Meaning |
|---|---|---|
| `matchesPlayed` | `COUNT(DISTINCT CASE WHEN status='COMPLETED' AND attended = TRUE THEN game_id END)` (`:25`) | Completed games where the host marked you **present** |
| `matchesCompleted` | **the exact same expression** (`:26`) | Identical value. Redundant. Do not render. |
| `attendanceRate` | attended ÷ `COUNT(DISTINCT CASE WHEN status='COMPLETED' THEN game_id END)` (`:27-33`) | Fraction of your completed-game participations you attended. **`null` when you have no completed-game participation at all.** |

Attendance tri-state, as the backend actually treats it — `attended = TRUE` / `FALSE` / `NULL`:

| `match_participant.attended` | counts toward `matchesPlayed`? | counts in the `attendanceRate` denominator? | net effect |
|---|---|---|---|
| `TRUE` | yes | yes | attended |
| `FALSE` | no | yes | **counted as not attended** |
| `NULL` (never marked) | no | yes | **counted as not attended** |

This is asserted by `GameCompletionIntegrationTest.java:260-293`: `attended = FALSE` → `matchesPlayed = 0`, `attendanceRate = 0.0`; `attended = NULL` → `matchesPlayed = 0`, `attendanceRate = 0.0`.

> **UI rule (directly from the brief's warning about NULL):** the backend *conflates* "marked absent" and "never marked" into one bucket. The profile page must therefore **not** present a "missed" count, and must **not** claim a missed game was a no-show. The only honest framing is: *attendance counts only games where the host marked you present; games you were not marked for count against your rate.* Note this differs from `MyGameCard.tsx:20-35`, which still says "Attendance not marked" for the `NULL` case on the *list* — that is a per-game statement and remains accurate; the profile is a *rate* and must not inherit the distinction.

Only `COMPLETED` games contribute. `stats_nonCompletedGame_attendanceHasNoEffect` (`GameCompletionIntegrationTest.java:295-310`) pins this.

Rating query (`:45-72`), joined over completed games with `attended = TRUE` on **both** rater and ratee:

| Statistic | Meaning |
|---|---|
| `averageRating` | Mean `player_rating.score` (1–5) left by players who themselves attended that game. `null` when no qualifying rating exists. |
| `ratingCount` | Count of those same rows. `0` when none. |

### 1.6 Frontend — what already exists

| Concern | Location | Status |
|---|---|---|
| `/profile` route | `app/(app)/` contains only `create-game`, `games`, `games/[id]`, `my-games`, `layout` | **MISSING → 404** |
| BottomNav entry | `components/layout/BottomNav.tsx:8-13` (`/profile` at `:12`) | **already linked** |
| Route protection | `proxy.ts:7` `PROTECTED_PATHS = [..., "/profile"]` | **already protected** |
| App shell | `app/(app)/layout.tsx:7-9` — `mx-auto max-w-md flex-col`, `main` = `flex-1 pb-20` | reuse as-is |
| `UserProfile` type | `lib/types.ts:33-42` | **already exists**, mirrors `UserProfileResponse` exactly |
| `PlayerStats` type | `lib/types.ts:44-50` | **already exists**, mirrors `PlayerStatsResponse` exactly |
| `UpdateProfileInput` type | — | **missing** |
| Profile API module | `lib/api/` has `auth`, `games`, `join-requests`, `participants`, `ratings` | **missing** |
| API client | `lib/api/client.ts` — `API_BASE_URL = "/api"`, `credentials: "include"`, throws `ApiError{status,code,message}` | reuse unchanged |
| Form/input/button components | none — no shared UI kit | follow `app/(app)/create-game/page.tsx` inline markup |
| Loading pattern | `app/(app)/my-games/page.tsx:121-122` — `Loading games…` | reuse |
| Error banner | `app/(app)/my-games/page.tsx:115-119` — `rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700` | reuse |
| Success banner | `components/games/GameRatingPanel.tsx:204-211` — emerald, `role="status" aria-live="polite"` | reuse |
| Refresh/retry pattern | `components/games/JoinRequestsPanel.tsx:80-98` (`useCallback` + `refresh`) | reuse for the retry button |
| Saving state | `app/(app)/create-game/page.tsx:97-100`, `GameRatingPanel.tsx:301` — `disabled` + `Saving…` | reuse |
| Input class | `app/(app)/create-game/page.tsx:91-92` `inputClass` const | copy verbatim into the form |
| Select pattern | `app/(app)/create-game/page.tsx:186-215` | reuse for the skill-level picker |
| Avatar initials | duplicated in `games/[id]/page.tsx:26-33`, `GameRatingPanel.tsx:12-19`, `JoinRequestsPanel.tsx:48-56` | add a 4th local copy; consolidating is out of scope (§16) |
| `POSITION_LABELS` | `components/games/JoinRequestsPanel.tsx:25-32` | existing; a profile copy is fine |
| `SKILL_LABELS` | duplicated in 3 files | a profile copy is fine |
| Frontend tests | `package.json:5-10` — only `dev`/`build`/`start`/`lint`; no test runner, no test deps | **none. Do not add one.** |

### 1.7 Database / migrations

Flyway history is **V1, V2, V3 only** (`resources/db/migration/`): `V1__init.sql`, `V2__add_position_to_user.sql`, `V3__add_game_discovery_index.sql`. The next version, if ever needed, is **V4**.

`app_user` columns (`V1__init.sql:5-17` + `V2__add_position_to_user.sql:3-4`): `id, email, password_hash, display_name, bio, profile_image_url, skill_level, position, created_at`. Every field the Phase 3 backend already exposes is already persisted. **Phase 10 requires no migration.**

---

## 2. Exact profile gap

One gap, and it is entirely on the frontend:

> `BottomNav` links to `/profile` and `proxy.ts` protects `/profile`, but no route file exists, so the tab is a dead end (404) despite a complete, tested Phase 3 backend sitting behind it.

Everything the brief asks for is already served. The work is: route + component + API module + one input type.

---

## 3. Existing backend profile contract (reused verbatim)

- **Read:** `GET /api/users/me` → `200 UserProfileResponse`. No parameters. Principal from the `auth_token` httpOnly cookie.
- **Write:** `PATCH /api/users/me` body `UpdateProfileRequest` → `200 UserProfileResponse` (**the full updated profile, including fresh `stats`**). This single fact removes the need for a refetch after save.
- **Not used this phase:** `GET /api/users/{id}`.

Error shapes come from `GlobalExceptionHandler` (`common/exception/GlobalExceptionHandler.java:30-62`) as `{status, code, message, path}`; `ApiError` is surfaced on the client by `ApiError{status, code, message}`. Codes this form can receive: `VALIDATION_ERROR` (400), `MALFORMED_BODY` (400), `UNAUTHORIZED` (401), `USER_NOT_FOUND` (404), `INTERNAL_ERROR` (500).

---

## 4. Frontend profile architecture

```
app/(app)/profile/page.tsx          "use client" — owns all state, composes the two cards
  ├── components/profile/ProfileIdentityCard.tsx    read-only identity + stats + Edit/View My Games
  └── components/profile/EditProfileForm.tsx         the 4-field editor (mounts only while editing)

lib/api/profile.ts                  getMyProfile(), updateMyProfile(input)
lib/types.ts                        + UpdateProfileInput
```

**State machine in `page.tsx`** (one `useState` per concern, mirroring `my-games/page.tsx:38-46`):

| State | Type | Purpose |
|---|---|---|
| `profile` | `UserProfile \| null` | last successful read *or* write |
| `loading` | `boolean` | initial read in flight |
| `error` | `string \| null` | read error, already mapped to a human message |
| `editing` | `boolean` | form visibility |
| `success` | `string \| null` | save confirmation |

- Read on mount via `useEffect` + `let cancelled` guard — the exact `my-games/page.tsx:48-80` pattern.
- Wrap the read in `useCallback` so the error-state **Try again** button can re-invoke it (pattern from `JoinRequestsPanel.tsx:80-98`).
- **After a successful save, set `profile` directly from the PATCH response** and exit edit mode. Do not refetch.
- Clear `success` after ~3s with a `setTimeout` in an effect that returns a `clearTimeout` cleanup.

**Layout** — `div.p-4` (`my-games/page.tsx:99`), `h1.text-xl.font-semibold` (`my-games/page.tsx:102`), cards as `section.rounded-xl.border.border-zinc-200.bg-white.p-4` (`games/[id]/page.tsx:50-58`). Everything is already inside the `max-w-md` shell from `app/(app)/layout.tsx:7`, so there is no page-level width work. Do not add a wrapper, a grid, or a desktop breakpoint.

### 4.1 Read-only view

```
Profile
┌──────────────────────────────────────────┐
│  ( A )   Ada Lovelace                     │  ← initials avatar, or <img> when
│          Striker · Advanced               │    profileImageUrl is set
│          ada@example.com                  │    ← read-only, from useAuth()
└──────────────────────────────────────────┘

Player stats
┌──────────────────────────────────────────┐
│  Matches played        Attendance rate    │
│  12                    86%                │
│  Average rating        Ratings received   │
│  4.3 ★                 9                  │
└──────────────────────────────────────────┘
  Only completed games count, and only when the
  host marked you present.

[ Edit profile ]     [ View My Games → ]
```

**Field-by-field rendering rules** (no value is ever invented):

| Row | Source | Rule |
|---|---|---|
| Avatar | `profileImageUrl` | non-empty → `<img>` with `eslint-disable @next/next/no-img-element` (pattern: `JoinRequestsPanel.tsx:193-196`); else emerald initials circle (`games/[id]/page.tsx:344-346`) |
| Name | `displayName` | plain text. Non-null by schema. |
| Position | `position` | `POSITION_LABELS[position]`, else **"Not set"**. Read-only (D2). |
| Skill level | `skillLevel` | `SKILL_LABELS[skillLevel]`. Non-null by schema. |
| Email | `useAuth().user.email` | `user?.email ?? "—"`. Read-only, never an input (D3). |
| Member since | `createdAt` | `formatISTDate(createdAt)` via `lib/format.ts:28-30`. Optional but free — the field already ships in the DTO. |
| Matches played | `stats.matchesPlayed` | integer |
| Attendance rate | `stats.attendanceRate` | `Math.round(rate * 100)%`; **`null` → "—"** (= no completed participations yet) |
| Average rating | `stats.averageRating` | `toFixed(1)` + `★`; **`null` → "—"** (pattern: `JoinRequestsPanel.tsx:58-65`) |
| Ratings received | `stats.ratingCount` | integer, always present |
| — | `stats.matchesCompleted` | **never rendered** (§1.5 — duplicate of `matchesPlayed`) |

The one-line footnote under the stats card is required, not decorative: it is the only place the user learns that the rate only credits games the host marked them present for. Word it as behaviour, not blame.

### 4.2 Loading / error / empty states

| State | Rendering | Source of truth |
|---|---|---|
| Loading | `<p className="mt-8 text-center text-sm text-zinc-500">Loading profile…</p>` | `my-games/page.tsx:122` |
| Error | red banner (`my-games/page.tsx:116-118`) + a `Try again` button that re-runs the `useCallback` read | banner from `my-games`; button from `JoinRequestsPanel`'s refresh idiom |
| Error, 401 | same banner, message "Your session has expired. Please sign in again.", plus a `Sign in` link to `/login` | `JoinRequestsPanel.tsx:142-144` maps `UNAUTHORIZED`; the `proxy.ts` cookie check is presence-only, so an *expired* cookie still renders this page and only the API reveals the 401 |
| Partial/null profile | every field above has an explicit fallback; a `null` `stats` is impossible (record field, always populated) but guard with `profile.stats?.…` anyway so a malformed payload degrades instead of throwing | §14 R1 |
| Empty stats | a brand-new account legitimately shows `0 / — / — / 0`. That is data, not an error — render it, do not show an empty state | §1.5 |

---

## 5. Edit-profile behaviour

Toggled by an `Edit profile` button; the form unmounts on cancel or success. The form owns only its own field state and the `saving` flag; the page owns `profile`.

**Fields (D2):** display name (text), bio (textarea, `maxLength={2000}`), profile image URL (text, `maxLength={500}`), skill level (select). **Position is deliberately absent** — it is not in the payload, and because `null` means "don't change" (§1.3) it is likewise not clearable from this form.

**Load:** seed from the current `profile` when the form mounts, so the user always edits what is on screen.

**Client validation** (mirrors the server so the user gets an inline message instead of a `400` banner):

| Field | Rule | Server counterpart |
|---|---|---|
| display name | required; `.trim()` non-empty; ≤ 80 chars | `UpdateProfileRequest.java:13`, `ProfileService.java:45-50` |
| bio | ≤ 2000 chars | `UpdateProfileRequest.java:16` |
| profile image URL | ≤ 500 chars; if non-empty, must start with `http://` or `https://` | `maxLength` exists; **shape is not validated server-side** |
| skill level | always one of the three enum values from a `<select>` | enum coercion → `MALFORMED_BODY` otherwise |

> The URL prefix check is a **client-side only** affordance. It is intentionally stricter than the backend, because the value is rendered straight into an `<img src>`; the backend deliberately does not constrain it, and this must not become a reason to change the backend.

**Submit:**

1. `if (saving) return;` — hard guard against double-tap on mobile.
2. Validate. On failure, set an inline error and **do not call the API**.
3. `setSaving(true)`, `setError(null)`, `setSuccess(null)`.
4. Send **all four** fields, converting empty strings to `""` — **never `null`** (§1.3). Always include `skillLevel`, so `isEmpty()` can never be true.
5. On success: `onSaved(updated)` → the page sets `profile`, exits edit mode, shows the emerald success banner.
6. On failure: map `ApiError.code` → message. `VALIDATION_ERROR` and `MALFORMED_BODY` → "Check the highlighted fields and try again."; `UNAUTHORIZED` → the 401 copy; default → `err.message`; non-`ApiError` → "An unexpected error occurred.".
7. `finally { setSaving(false); }`.

**Saving state:** Save button `disabled={saving}`, label `Saving…` (pattern: `create-game/page.tsx:97-100`). Cancel also `disabled={saving}` so a half-finished request cannot be orphaned.

**Success feedback:** `role="status" aria-live="polite"`, emerald banner (`GameRatingPanel.tsx:204-211`), auto-cleared after ~3s.

---

## 6. API / type changes

`lib/types.ts` — add one interface only:

```ts
export interface UpdateProfileInput {
  displayName: string;
  bio: string;
  profileImageUrl: string;
  skillLevel: SkillLevel;
}
```

`UserProfile` and `PlayerStats` already exist and already match the DTOs exactly — **do not touch them** (`lib/types.ts:33-50`).

`lib/api/profile.ts` — new, following the shape of `lib/api/join-requests.ts`:

```ts
import { apiFetch } from "./client";
import type { UpdateProfileInput, UserProfile } from "@/lib/types";

export async function getMyProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>("/users/me");
}

export async function updateMyProfile(input: UpdateProfileInput): Promise<UserProfile> {
  return apiFetch<UserProfile>("/users/me", { method: "PATCH", body: input });
}
```

**Path convention (get this right):** `apiFetch` prepends `/api` (`lib/api/client.ts:1`), and `next.config.ts:6-11` rewrites `/api/:path*` → `http://localhost:8080/api/:path*`. So an argument of `"/users/me"` produces the correct final URL. Do **not** write `"/api/users/me"`.

> ⚠️ **Pre-existing bug, do not copy, and do not fix in this phase.** `lib/api/ratings.ts:5-6` builds `` `/api/games/${id}/ratings` `` — the `/api` prefix is duplicated, so ratings requests resolve to `/api/api/games/...` and 404. Every other module omits the prefix. This is a Phase 8 defect unrelated to the profile; see §15 R6.

No state library is introduced. `useState` + `useEffect` only, matching the rest of the app.

---

## 7. Navigation integration

- `BottomNav.tsx:12` already links to `/profile`, and `isActive` (`BottomNav.tsx:21-22`) already highlights it for `pathname === "/profile"`. **No change.** The tab goes live purely by adding the route.
- `proxy.ts:7` already lists `/profile` in `PROTECTED_PATHS`, and the matcher excludes `/api`, so a cookie-less visit redirects to `/login` and an authenticated one reaches the page. **No change.**
- No new nav items. No redesign.
- The page's `View My Games` link targets `/my-games` and is a plain `<Link>`, matching `my-games/page.tsx:127-131`. `/profile` is *not* a `/games` prefix, so it gets no special `isActive` handling.

---

## 8. Security considerations

- **No client-supplied identity.** `getMyProfile`/`updateMyProfile` hit `/users/me` and take no id. There is no path, query param, or prop through which a user id reaches the profile API.
- **Server-side derivation is already correct:** `ProfileController.java:31-33` and `:41-46` read `principal.id()` from the `auth_token` httpOnly cookie (`JwtAuthenticationFilter.java:40`); `security/` is untouched by this phase.
- **No secret material reaches the client.** `UserProfileResponse` contains no `passwordHash` (it is never mapped in `ProfileService.toResponse`, `ProfileService.java:84-94`), and `User.passwordHash` has no Jackson-visible path into any response DTO. The JWT lives in an httpOnly cookie and is never read by JS.
- **No other user's data is fetched.** `GET /api/users/{id}` is not called anywhere in this phase.
- **Email is own-only.** It is read from the signed-in user's own session data (D3), and `UserProfileResponse` stays email-free so it can never be served to a third party (§1.4).
- **Client-side input is untrusted, server-side validation is authoritative.** The form's checks are UX; `UpdateProfileRequest`'s Bean Validation and `ProfileService`'s blank-name guard remain the real gate and are unchanged.
- `401` is handled explicitly rather than assumed away, because `proxy.ts` only checks that a cookie *exists* — a stale cookie passes the proxy and only the API rejects it.

---

## 9. Backend changes

**None. Zero. This phase modifies no file under `backend/src/main`.**

| Brief requirement | Already satisfied by | Change needed |
|---|---|---|
| Authenticated user retrieves own profile | `ProfileController.java:30-34`, tested `ProfileIntegrationTest.java:48-61` | none |
| Unauthenticated rejected | `SecurityConfiguration.java:43`, tested `ProfileIntegrationTest.java:41-45` | none |
| Statistics correct | `PlayerStatsRepository.java:22-72`, tested `GameCompletionIntegrationTest.java:242-325` | none |
| Editable fields update | `ProfileService.java:43-64`, tested `ProfileIntegrationTest.java:99-171` | none |
| Invalid input rejected | `UpdateProfileRequest.java:12-25`, tested `ProfileIntegrationTest.java:174-245` | none |
| Other user's profile unchanged | `ProfileController.java:36-39`, tested `ProfileIntegrationTest.java:66-94` | none |
| Name / position / email / stats displayable | `UserProfileResponse.java:9-18` (+ `auth/me` for email) | none |
| Matches / attended / missed / rating displayable | `PlayerStatsResponse.java:3-9`; "missed" is intentionally represented by `attendanceRate` per D1 | none |
| Migration | — | **none required; schema unchanged** |

The only backend-adjacent item proposed is a **single test** (§10.1), and it is optional.

---

## 10. Tests

### 10.1 Backend

`ProfileIntegrationTest` already contains 15 tests covering every case the brief enumerates. Do **not** duplicate them. Mapping:

| Brief case | Existing test |
|---|---|
| authenticated user retrieves own profile | `ProfileIntegrationTest.java:48` |
| unauthenticated rejected (read) | `:41` |
| unauthenticated rejected (write) | `:248` |
| profile statistics correct (zero case) | `:56-60` |
| profile statistics correct (non-zero, TRUE/FALSE/NULL/non-completed/owner) | `GameCompletionIntegrationTest.java:242, 260, 278, 295, 312` |
| editable fields update | `:99` (partial preserves rest), `:113` (name), `:125` (bio), `:137` (skill), `:149` (image) |
| invalid input rejected | `:173` (blank name), `:185` (>80), `:198` (>2000), `:211` (bad skill → `MALFORMED_BODY`), `:223` (bad position), `:235` (empty body) |
| other user's profile unchanged | `:66` (200 + correct id), `:80` (404 `USER_NOT_FOUND`), `:90` (401) |

**One genuinely uncovered case, and it is worth adding because the new UI renders it:** `averageRating` and `ratingCount` are only ever asserted as absent/zero. No test anywhere creates a `player_rating` row and asserts the resulting non-zero values. Since the profile page will put `4.3 ★` and `9` on screen, add exactly one test to `GameCompletionIntegrationTest` (which already has `registerAndAuth`, `insertGame`, `insertParticipant`, `cleanDatabase` and an autowired `JdbcTemplate`):

```
stats_completedGame_rateeAverageAndCount — COMPLETED game, rater and ratee both
  participants with attended = TRUE, two player_rating rows (4 and 5) for the ratee;
  GET /api/users/me as the ratee asserts
  stats.averageRating == 4.5 and stats.ratingCount == 2.
```

Insert the rating with `jdbcTemplate.update("INSERT INTO player_rating (game_id, rater_id, ratee_id, score) VALUES (?,?,?,?)", ...)` — the table has no surrogate-key requirement beyond the default UUID. Add **no** other test. Do not add tests for the `bio`/`profileImageUrl` `""` clearing behaviour; that is a documented consequence of existing code, not new behaviour.

### 10.2 Frontend

`package.json` has no test runner and no test dependency. **Do not introduce one.** Verification is:

```
cd frontend
npm run lint      # script "eslint", eslint-config-next core-web-vitals + typescript
npm run build     # next build; also runs the TypeScript compiler
```

Then manual, against `docker compose up -d` + `./gradlew bootRun` + `npm run dev`.

---

## 11. Manual verification checklist

Setup: a user with **at least one** completed game they attended, **at least one** they did not, and **at least one rating received** — otherwise the stats card is verified only in its zero state.

**Read path**
1. BottomNav `Profile` is highlighted on `/profile` and no longer 404s.
2. Name, position (or "Not set"), skill level, email, and "Member since" render from live data.
3. Avatar: initials by default; `<img>` appears after a URL is saved in step 8.
4. Matches played, attendance rate, average rating, ratings received match the DB. Confirm `attendanceRate × attended-count ÷ all-completed-count` reproduces the number.
5. Footnote about the host marking attendance is present.

**Edit path**
6. `Edit profile` reveals the form pre-filled with current values; position is **not** editable.
7. Submitting an empty/whitespace name is blocked inline and issues no request (check DevTools).
8. Save name + bio + image URL + skill level → all four persist, the view reflects them, and a green confirmation appears then clears.
9. `Saving…` shows and Save/Cancel are disabled during the request; a rapid double-tap produces exactly one `PATCH` in the Network tab.
10. Clear the bio to empty and save → the bio reads "Not set" after reload.
11. Enter a `ftp://` image URL → blocked inline, no request.
12. Enter a 500+ char image URL → blocked inline, no request.
13. A rejected save (e.g. 500, or stop the backend) shows the red banner and keeps the form open with typed values intact.

**States & navigation**
14. Stop the backend → red banner with a working `Try again`; restart → it recovers.
15. Delete the `auth_token` cookie value manually but bypass the proxy (`/profile` still renders) → 401 copy with a `Sign in` link.
16. `View My Games` navigates to `/my-games`, which is unchanged and still works.
17. All four nav tabs still work from and into `/profile`.
18. A brand-new account shows `0 / — / — / 0` and does not look broken.
19. Signed out → `/profile` redirects to `/login` (existing `proxy.ts` behaviour, regression check).

**Mobile**
20. DevTools at 360×640: no horizontal scroll, the form is fully usable, the stats grid does not clip.

---

## 12. Implementation order

1. `lib/api/profile.ts` — the two functions. (Independently verifiable via the Network tab.)
2. `lib/types.ts` — add `UpdateProfileInput`.
3. `components/profile/ProfileIdentityCard.tsx` — pure presentational; takes `UserProfile`.
4. `components/profile/EditProfileForm.tsx` — local state, validation, `updateMyProfile`, `saving` guard.
5. `app/(app)/profile/page.tsx` — wire the load, the state machine, the toggle, and `View My Games`.
6. `npm run lint` then `npm run build`.
7. Optional backend test: the single rating-stats test in `GameCompletionIntegrationTest` → `cd backend && ./gradlew test`.
8. Run the manual checklist (§11).

Steps 1–5 are frontend-only and independently shippable. Do step 7 last and separately: it touches `backend/`, is optional, and must not be allowed to block the UI work.

---

## 13. Files likely to be created / modified

**Created**
- `frontend/app/(app)/profile/page.tsx` — the route that closes the 404.
- `frontend/components/profile/ProfileIdentityCard.tsx` — read-only identity + stats.
- `frontend/components/profile/EditProfileForm.tsx` — the 4-field editor.
- `frontend/lib/api/profile.ts` — `getMyProfile`, `updateMyProfile`.
- `.opencode/plans/planning_phase10.md` — this document.

**Modified**
- `frontend/lib/types.ts` — **additive only**: one new `UpdateProfileInput` interface. `UserProfile` / `PlayerStats` untouched.
- `backend/src/test/java/com/gameconnect/game/GameCompletionIntegrationTest.java` — *optional, one test* (§10.1).

**Deliberately NOT modified**
`BottomNav.tsx`, `proxy.ts`, `app/(app)/layout.tsx`, `lib/api/client.ts`, `lib/api/auth.ts`, `lib/auth-context.tsx`, `lib/format.ts`, `next.config.ts`, and **every file under `backend/src/main`**. No migration. No `app/(app)/profile/[id]/`.

---

## 14. Risks / edge cases

- **R1 — A malformed `stats` must not crash the page.** `stats` is a record component and always serialised, but read defensively (`profile.stats?.attendanceRate`). A thrown render here is a hard 404-equivalent for the user.
- **R2 — `null` vs `""` on the wire.** Sending `null` for a nullable field is a **no-op** on the server (§1.3), not a clear. A form that "clears" the bio by sending `null` would appear to succeed and change nothing. Always send `""`. This is the single easiest mistake to make in this phase.
- **R3 — Blank name is a `400`, not a `422`.** `displayName: ""` trips the service's own guard (`ProfileService.java:45-50`) and comes back as `VALIDATION_ERROR`; an over-length name comes back as `VALIDATION_ERROR` from Bean Validation. Both must be caught client-side first. `MALFORMED_BODY` is a *different* code and means the enum/JSON was unparseable — a bug, not user error.
- **R4 — `position` is a dead field in the product (consequence of D2).** Excluding it from the form means no user can ever set a position through the UI, so `position` will read "Not set" for every account created in-app, and the position line in `JoinRequestsPanel.tsx:220-222` can never render. `PATCH /api/users/me` will happily set it if called directly. This is a known, accepted MVP gap — **not** a bug to fix in Phase 10, and the reason it is called out here is that it will look like one. If it is later deemed wrong, adding a 6-option `<select>` is a ~10-line change to the same form.
- **R5 — Stale identity in `AuthContext` after a rename.** `AuthProvider` fetches `/auth/me` once on mount (`lib/auth-context.tsx:23-36`) and never refreshes, so `user.displayName` goes stale after a save. **Currently harmless** — `user.displayName` is not rendered anywhere in the app; only `user.id` is consumed (`games/[id]/page.tsx:128, 258`). The page itself is always correct because it renders from the PATCH response. Do not add a `refreshUser()` to the auth context in this phase; the profile page must simply never read the name from `useAuth()`.
- **R6 — Pre-existing `ratings.ts` path bug.** `lib/api/ratings.ts:5-6` double-prefixes `/api` (§6). Out of scope, but do not replicate the pattern in `profile.ts`, and do not let a Phase 8 ratings failure be misread as a Phase 10 regression. Worth a separate one-line fix.
- **R7 — Expired cookie renders an empty shell.** `proxy.ts` checks cookie *presence* only. An expired token passes the proxy, the page mounts, and only the `GET` returns `401`. The 401 branch (§4.2) is the only thing standing between the user and a silent dead end.
- **R8 — "Attendance rate" is easy to misread as a score.** 86% is a *reliability* number, not a grade. Label it "Attendance rate", never "Attendance score" or "Reliability rating", and keep the footnote.
- **R9 — Duplicated label maps and `initials()`.** A 4th local `initials()` and a 3rd `SKILL_LABELS` will exist. This matches the codebase's existing (unconsolidated) convention. Hoisting them into `lib/` is a cross-cutting refactor and stays out of scope — but it must not grow to a 5th and 4th copy in a later phase either.
- **R10 — `attendanceRate` is `0.0`, not `null`, for a user whose only completed games went unmarked.** Both "no completed games" (`null` → "—") and "all missed" (`0.0` → "0%") are real and render differently. Verify step 4 against both.

---

## 15. Explicitly out of scope

**Not in Phase 10:**

- Public profiles, `/profile/[id]`, profile discovery, player search.
- Followers/friends, social feed, leaderboards, ranking, badges/achievements.
- Avatar/profile-image **uploads** or any file storage. `profileImageUrl` is edited as a plain text URL because that is exactly what `PATCH /api/users/me` already accepts.
- **Position editing** (D2), and any attempt to make the `NULL`-means-no-change limitation of §1.3 go away. Fixing that is a backend API redesign.
- Duplicating My Games, game history, rating, attendance, or pagination inside Profile.
- Any new statistic, and any frontend re-derivation of `matchesPlayed` / `attendanceRate` / rating figures. The backend is the single source of truth.
- Email editing, password change, account deletion.
- A frontend test runner, React Query, SWR, Zustand, Redux, a shared UI/form kit, a new design system, or a desktop layout.
- `BottomNav` redesign, new nav items, new routes beyond `/profile`.
- Redis, WebSockets, microservices, CI/CD, deployment. Phases 11 (hardening) and 12 (deployment) are untouched.

**Deferred host-contact-sharing rule (D4).** The requirement — *a player may see the host's email and phone number once their join request has been ACCEPTED, and not otherwise* — is a real product rule but it is **not profile work** and it is **not implementable** in Phase 10. Audited gaps, for whoever plans it:

- **There is no `phone_number` column.** `app_user` is `V1__init.sql:5-17` + `V2__add_position_to_user.sql:3-4`. This requires a **V4 migration**, plus a `User` field and getter/setter, plus a `RegisterRequest` field with validation, plus a `UserResponse` field, plus a change to the `TestcontainersConfiguration`-backed schema assertions in `FlywaySchemaTest`.
- **It needs a new game-scoped authorization rule.** "ACCEPTED" is a `join_request.status` value (`V1__init.sql:60`) for *that player on that game*; the host is `game.owner_id`. Nothing in `PlayerStatsService`/`ProfileService` is game-scoped, so a new endpoint (e.g. `GET /api/games/{id}/host-contact`) and a new `BusinessException(FORBIDDEN, ...)` guard are required. `SecurityConfiguration` needs no change — `anyRequest().authenticated()` already covers it.
- **It must not be hung off `GET /api/users/{id}`.** That endpoint is callable by any authenticated user for any id (§1.4). Contact details need a game-scoped endpoint so the ACCEPTED check is actually enforceable.
- It touches the registration, game, join-request, and security domains — roughly 4× the size of this phase, and it changes the data model.

Recommend it as its own phase (Phase 11 candidate), before hardening.

---

## 16. Build-mode implementation sequence

Copy/paste for the implementing agent. Do not deviate; do not expand scope.

```
CONTEXT
Repo: F:\Game-connect. Phase 10 = build the missing /profile frontend.
Plan: .opencode/plans/planning_phase10.md — read it fully first.
HEAD is 4bf3a83. Working tree must stay clean apart from this phase's files.

HARD CONSTRAINTS
- ZERO changes under backend/src/main. No migration. No V4.
- Do NOT touch BottomNav.tsx, proxy.ts, app/(app)/layout.tsx, lib/api/client.ts,
  lib/api/auth.ts, lib/auth-context.tsx, lib/format.ts, next.config.ts.
- Editable fields are displayName, bio, profileImageUrl, skillLevel ONLY.
  Position is READ-ONLY. Never send null for a nullable field — send "".
- No test runner, no state library, no new dependencies, no shared UI kit.
- Never read the profile name from useAuth(); only from the API response.

STEP 1 — frontend/lib/api/profile.ts   (create)
  import { apiFetch } from "./client";
  import type { UpdateProfileInput, UserProfile } from "@/lib/types";
  export async function getMyProfile(): Promise<UserProfile> {
    return apiFetch<UserProfile>("/users/me");
  }
  export async function updateMyProfile(input: UpdateProfileInput): Promise<UserProfile> {
    return apiFetch<UserProfile>("/users/me", { method: "PATCH", body: input });
  }
  NOTE: the path argument is "/users/me" with NO "/api" prefix — client.ts:1 adds it.
  Do NOT copy the pattern in lib/api/ratings.ts:5-6; it is buggy (plan §15 R6).

STEP 2 — frontend/lib/types.ts   (modify: add only, at the end)
  export interface UpdateProfileInput {
    displayName: string;
    bio: string;
    profileImageUrl: string;
    skillLevel: SkillLevel;
  }
  Do not alter the existing UserProfile or PlayerStats interfaces.

STEP 3 — frontend/components/profile/ProfileIdentityCard.tsx   (create)
  Server-safe (no "use client" needed) presentational component.
  Props: { profile: UserProfile; email: string | null; onEdit: () => void }
  Layout: div.p-4 wrapper belongs to the PAGE, not this component.
    - header  h1.text-xl.font-semibold  "Profile"
    - identity card  section.rounded-xl.border.border-zinc-200.bg-white.p-4
        avatar: profileImageUrl non-empty ? <img> (needs the
        eslint-disable-next-line @next/next/no-img-element comment, as in
        JoinRequestsPanel.tsx:193-196) : initials circle
        (copy the local initials() from games/[id]/page.tsx:26-33; do not refactor)
        name = profile.displayName
        sub   = [POSITION_LABELS[position] ?? "Not set", SKILL_LABELS[skillLevel]]
                .join(" · ")
        email = email ?? "—"   (read-only, never an input)
        "Member since" row using formatISTDate(profile.createdAt)
    - stats card  same section classes
        grid-cols-2, 4 tiles:
          "Matches played"     -> profile.stats?.matchesPlayed
          "Attendance rate"    -> stats.attendanceRate == null ? "—"
                                  : `${Math.round(rate * 100)}%`
          "Average rating"     -> stats.averageRating == null ? "—"
                                  : `${v.toFixed(1)} ★`
          "Ratings received"   -> stats.ratingCount
        DO NOT render matchesCompleted (it is a duplicate of matchesPlayed).
        Footnote below the grid, verbatim in spirit:
        "Only completed games count, and only when the host marked you present."
    - two full-width buttons: "Edit profile" (solid emerald) and
      "View My Games" (outlined, <Link href="/my-games">) —
      button classes copied from create-game/page.tsx:97 and :226-232
  Local maps: SKILL_LABELS and POSITION_LABELS copied from JoinRequestsPanel.tsx:19-32.

STEP 4 — frontend/components/profile/EditProfileForm.tsx   (create)
  "use client". Props: { profile: UserProfile; onCancel: () => void;
                          onSaved: (updated: UserProfile) => void }
  Local state: displayName, bio, profileImageUrl, skillLevel (seeded from
  `profile` on mount), error: string | null, saving: boolean.
  Use the exact inputClass string from create-game/page.tsx:91-92.
  Fields: display name input (maxLength 80, required)
          bio textarea (rows 3, maxLength 2000)
          profile image URL input (maxLength 500, placeholder https://…)
          skill level <select> of BEGINNER/INTERMEDIATE/ADVANCED
  NO position field. NO email field.
  handleSubmit(e):
    e.preventDefault(); if (saving) return;      // duplicate-submit guard
    validate; on failure set error and RETURN WITHOUT calling the API:
      - displayName.trim() === ""        -> "Display name is required."
      - displayName.trim().length > 80    -> "Display name must be at most 80 characters."
      - bio.length > 2000                -> "Bio must be at most 2000 characters."
      - url.trim() !== "" && !/^https?:\/\//i.test(url.trim())
                                          -> "Image URL must start with http:// or https://."
    setError(null); setSaving(true);
    try {
      const updated = await updateMyProfile({
        displayName: displayName.trim(),
        bio,                                  // "" when cleared, NEVER null
        profileImageUrl: profileImageUrl.trim(), // "" when cleared, NEVER null
        skillLevel,
      });
      onSaved(updated);                       // sets profile, exits edit mode, shows success
    } catch (err) {
      setError(err instanceof ApiError
        ? (err.code === "VALIDATION_ERROR" || err.code === "MALFORMED_BODY"
            ? "Check the highlighted fields and try again."
            : err.code === "UNAUTHORIZED"
              ? "Your session has expired. Please sign in again."
              : err.message)
        : "An unexpected error occurred. Please try again.");
    } finally { setSaving(false); }
  Save button: type=submit, disabled={saving}, label saving ? "Saving…" : "Save"
               (create-game/page.tsx:97-100)
  Cancel button: type=button, disabled={saving}, calls onCancel
  Error banner: rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700, role="alert"

STEP 5 — frontend/app/(app)/profile/page.tsx   (create)
  "use client". This is the file that closes the 404.
  State: profile: UserProfile | null, loading, error: string | null,
         editing, success: string | null
  const { user } = useAuth();   // use ONLY for user.email. Never user.displayName.
  const load = useCallback(() => { setLoading(true);
      getMyProfile().then(p => { setProfile(p); setError(null); })
        .catch(err => setProfile(null))
        .finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { if (!success) return;
      const t = setTimeout(() => setSuccess(null), 3000);
      return () => clearTimeout(t); }, [success]);
  render div.p-4, then:
    loading  -> <p className="mt-8 text-center text-sm text-zinc-500">Loading profile…</p>
    !profile -> red banner (my-games/page.tsx:116-118) + a "Try again" button calling
                load(); if the error was a 401 ApiError, the message is
                "Your session has expired. Please sign in again." and also render a
                <Link href="/login">Sign in</Link>
    else     -> success banner (emerald, role=status aria-live=polite, GameRatingPanel.tsx:204-211)
                then, when !editing: <ProfileIdentityCard profile={profile}
                                       email={user?.email ?? null} onEdit={...} />
                when  editing: <EditProfileForm profile={profile}
                                   onCancel={() => setEditing(false)}
                                   onSaved={(u) => { setProfile(u);
                                       setEditing(false); setSuccess("Profile updated."); }} />
  Note: PATCH returns the full updated profile INCLUDING fresh stats — assign it
  directly. Do not refetch after a save.

STEP 6 — verify
  cd frontend
  npm run lint
  npm run build
  Then run the manual checklist in plan §11 (all 20 items) against
  `docker compose up -d`, `cd backend && ./gradlew bootRun`, `npm run dev`.

STEP 7 — OPTIONAL, separate, do not block steps 1-6
  Add exactly ONE test to
  backend/src/test/java/com/gameconnect/game/GameCompletionIntegrationTest.java:
    stats_completedGame_rateeAverageAndCount
    COMPLETED game; rater and ratee both inserted as participants with attended = TRUE;
    two player_rating rows for the ratee with scores 4 and 5 (insert with the
    already-autowired jdbcTemplate);
    GET /api/users/me as the ratee asserts
      $.stats.averageRating == 4.5  and  $.stats.ratingCount == 2
  Then: cd backend && ./gradlew test
  Do not add any other test. ProfileIntegrationTest is already complete (15 tests).

STEP 8 — commit
  One commit: "adding phase-10, profile and player stats"
  Stage only the files in plan §13. Do not commit build/ or .next/ output.
  Do not commit unless the user explicitly asks.
```

---

**Summary of what Phase 10 is:** one new frontend route, two new components, one new API module, one added TypeScript interface, and — optionally — one backend test. The Phase 3 profile backend is already sufficient, so the correct answer to "what backend work does the profile page need?" is **none**.
