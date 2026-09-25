# Phase 3 Implementation Plan

**Session ID:** ses_f6f929faaffepHzP1T62Eze4BO
**Created:** 9/11/2026, 5:54:42 PM
**Updated:** 9/11/2026, 6:11:22 PM

---

## User

We have completed Phase 1 and Phase 2.

Phase 1:
- Foundation/database
- PostgreSQL + Flyway
- Core schema
- Backend/frontend shell

Phase 2:
- JWT authentication
- HTTP-only auth cookie
- Register/login/logout/me
- Spring Security
- Frontend login/register/auth flow
- Backend tests passing
- Frontend build passing

Now create the implementation plan for Phase 3 only.

Before planning:
- Inspect the current repository.
- Read AGENTS.md and PRODUCT.md.
- Inspect the actual Phase 1 and Phase 2 implementation.
- Do not modify any files.
- Do not implement Phase 3 yet.
- Do not introduce functionality belonging to later phases.

Produce a concrete Phase 3 implementation plan including:
- backend changes
- frontend changes
- database changes/migrations if required
- API endpoints
- DTOs
- validation
- authorization rules
- tests
- files to create/modify
- acceptance criteria

Keep the existing modular-monolith architecture. 

---

## Assistant (Plan · Big Pickle · 14.3s)

_Thinking:_

I'm in READ-ONLY mode. I need to inspect the repository thoroughly before creating Phase 3 plan. Let me start by exploring the current state of the codebase. First, let me read AGENTS.md and PRODUCT.md.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\AGENTS.md"
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

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\PRODUCT.md"
}
```

**Output:**
```
<path>F:\Game-connect\PRODUCT.MD</path>
<type>file</type>
<content>
1: # Game Connect
2: 
3: ## Product
4: 
5: Football Connect is a pickup football matchmaking platform.
6: 
7: Users who have already booked a football turf can create a game on the platform and invite random players to join.
8: 
9: Players can discover games and request to join.
10: 
11: The game owner has authority to accept or reject players based on their profile, experience, skill level and previous participation.
12: 
13: The platform does NOT initially handle turf booking.
14: Users manually enter the turf and booking information.
15: 
16: ---
17: 
18: # Core User Roles
19: 
20: ## Player
21: 
22: A player can:
23: 
24: - Register/login
25: - Create and edit profile
26: - Set skill level
27: - Browse available games
28: - Filter games
29: - Request to join a game
30: - Cancel their request
31: - View accepted games
32: - View match history
33: - View their football statistics
34: 
35: ## Game Owner
36: 
37: A game owner is also a player.
38: 
39: They can:
40: 
41: - Create a game
42: - Define game format
43: - Define maximum players
44: - Define required players
45: - Set skill level
46: - Set joining fee
47: - View join requests
48: - Accept players
49: - Reject players
50: - Remove a player before the match
51: - Mark player attendance after the match
52: - Close the game
53: 
54: ---
55: 
56: # MVP
57: 
58: The MVP should contain:
59: 
60: 1. Authentication
61: 2. Player profiles
62: 3. Create game
63: 4. Browse games
64: 5. Game details
65: 6. Request to join
66: 7. Accept/reject requests
67: 8. Player statistics
68: 9. Match history
69: 10. Attendance
70: 
71: Do NOT implement:
72: 
73: - Turf owner integration
74: - Online payments
75: - Chat
76: - Push notifications
77: - Advanced recommendation algorithms
78: - Teams
79: - Tournaments
80: - Subscriptions
81: - Microservices
82: 
83: Keep the MVP small.
84: 
85: ---
86: 
87: # Player Profile
88: 
89: A player has:
90: 
91: - id
92: - name
93: - profile image
94: - bio
95: - skill level
96: - matches played
97: - matches completed
98: - attendance rate
99: - average rating
100: 
101: Skill levels:
102: 
103: BEGINNER
104: INTERMEDIATE
105: ADVANCED
106: 
107: Positions:
108: 
109: GOALKEEPER
110: DEFENDER
111: MIDFIELDER
112: WINGER
113: STRIKER
114: FLEXIBLE
115: 
116: ---
117: 
118: # Game
119: 
120: A game contains:
121: 
122: - id
123: - owner
124: - turf name
125: - turf address
126: - latitude
127: - longitude
128: - date
129: - start time
130: - end time
131: - format
132: - skill level
133: - maximum players
134: - joining fee
135: - description
136: - status
137: 
138: Formats:
139: 
140: 5V5
141: 6V6
142: 7V7
143: 8V8
144: 9V9
145: 11V11
146: 
147: Game statuses:
148: 
149: OPEN
150: FULL
151: IN_PROGRESS
152: COMPLETED
153: CANCELLED
154: 
155: ---
156: 
157: # Join Request
158: 
159: A player can request to join an OPEN game.
160: 
161: Request states:
162: 
163: PENDING
164: ACCEPTED
165: REJECTED
166: CANCELLED
167: 
168: A player cannot have multiple active requests for the same game.
169: 
170: A player cannot request to join their own game.
171: 
172: A player cannot request to join a FULL game.
173: 
174: Once the maximum number of players is reached, the game becomes FULL.
175: 
176: ---
177: 
178: # Match Participation
179: 
180: After the game:
181: 
182: The game owner marks whether each accepted player attended.
183: 
184: Attendance affects the player's statistics.
185: 
186: No-show should negatively affect attendance percentage.
187: 
188: Only completed games should affect match statistics.
189: 
190: ---
191: 
192: # Ratings
193: 
194: For MVP:
195: 
196: Players can rate other players after a completed match.
197: 
198: Rating:
199: 
200: 1-5
201: 
202: Calculate average player rating.
203: 
204: Do not implement complex skill-rating algorithms yet.
205: 
206: ---
207: 
208: # Backend
209: 
210: Use:
211: 
212: Java
213: Spring Boot
214: Spring Security
215: JWT
216: Spring Data JPA
217: PostgreSQL
218: Flyway
219: Bean Validation
220: 
221: Use a modular monolith.
222: 
223: DO NOT use microservices.
224: 
225: Architecture:
226: 
227: controller
228: service
229: repository
230: entity
231: dto
232: mapper
233: security
234: exception
235: 
236: Use DTOs rather than exposing JPA entities directly.
237: 
238: ---
239: 
240: # Frontend
241: 
242: Use:
243: 
244: Next.js
245: TypeScript
246: Tailwind CSS
247: 
248: Pages:
249: 
250: /login
251: /register
252: /games
253: /games/[id]
254: /create-game
255: /my-games
256: /profile
257: /profile/[id]
258: 
259: The UI should be mobile-first because most users will use this on their phone.
260: 
261: ---
262: 
263: # Database
264: 
265: Use PostgreSQL.
266: 
267: Important relationships:
268: 
269: User 1 -> N Game
270: User 1 -> N JoinRequest
271: Game 1 -> N JoinRequest
272: Game N -> N User through MatchParticipant
273: Game 1 -> N PlayerRating
274: User 1 -> N PlayerRating
275: 
276: Use UUIDs for primary keys.
277: 
278: Use database constraints wherever appropriate.
279: 
280: ---
281: 
282: # Important business rules
283: 
284: 1. Only the game owner can accept/reject requests.
285: 
286: 2. A user cannot join their own game through a join request.
287: 
288: 3. A player cannot join a FULL game.
289: 
290: 4. A player cannot have duplicate active requests.
291: 
292: 5. Accepting a player must atomically verify that a slot is still available.
293: 
294: 6. Prevent race conditions when two requests are accepted simultaneously.
295: 
296: 7. Only the game owner can mark attendance.
297: 
298: 8. Statistics should be derived from match participation records.
299: 
300: 9. Users cannot rate players for games they did not participate in.
301: 
302: 10. A player can submit only one rating per player per game.
303: 
304: 11. Completed/cancelled games cannot accept new players.
305: 
306: ---
307: 
308: # Development strategy
309: 
310: Build the system incrementally.
311: 
312: Phase 1:
313: Project setup and database
314: 
315: Phase 2:
316: Authentication
317: 
318: Phase 3:
319: User profile
320: 
321: Phase 4:
322: Game creation
323: 
324: Phase 5:
325: Game discovery
326: 
327: Phase 6:
328: Join requests
329: 
330: Phase 7:
331: Game owner management
332: 
333: Phase 8:
334: Match completion and attendance
335: 
336: Phase 9:
337: Ratings and statistics
338: 
339: Phase 10:
340: Frontend polish
341: 
342: After every phase:
343: 
344: - Run tests
345: - Run the application
346: - Verify APIs
347: - Fix compilation errors
348: - Commit changes to git
349: 
350: Do not implement future features unless explicitly requested.

(End of file - total 350 lines)
</content>
```

---

## Assistant (Plan · Big Pickle · 5.8s)

_Thinking:_

Now I need to inspect the actual Phase 1 and Phase 2 implementation. Let me start by exploring the repository structure.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect"
}
```

**Output:**
```
<path>F:\Game-connect</path>
<type>directory</type>
<entries>
.git/
.gitignore
.vscode/
AGENTS.md
backend/
docker-compose.yml
frontend/
planning_phase2.md
PRODUCT.MD
README.md

(10 entries)
</entries>
```

---

## Assistant (Plan · Big Pickle · 5.7s)

_Thinking:_

Let me explore the backend and frontend structures in parallel.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend"
}
```

**Output:**
```
<path>F:\Game-connect\backend</path>
<type>directory</type>
<entries>
.gitattributes
.gitignore
.gradle/
bin/
build.gradle
build/
gradle/
gradlew
gradlew.bat
HELP.md
run-app-final.log
settings.gradle
src/

(13 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend"
}
```

**Output:**
```
<path>F:\Game-connect\frontend</path>
<type>directory</type>
<entries>
.gitignore
.next/
AGENTS.md
app/
CLAUDE.md
components/
eslint.config.mjs
lib/
next-env.d.ts
next.config.ts
node_modules/
package-lock.json
package.json
postcss.config.mjs
proxy.ts
public/
README.md
tsconfig.json

(18 entries)
</entries>
```

---

## Assistant (Plan · Big Pickle · 5.8s)

_Thinking:_

Let me explore the backend source structure more deeply.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src</path>
<type>directory</type>
<entries>
main/
test/

(2 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend\\app"
}
```

**Output:**
```
<path>F:\Game-connect\frontend\app</path>
<type>directory</type>
<entries>
(app)/
favicon.ico
globals.css
layout.tsx
login/
page.tsx
register/

(7 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend\\components"
}
```

**Output:**
```
<path>F:\Game-connect\frontend\components</path>
<type>directory</type>
<entries>
layout/

(1 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend\\lib"
}
```

**Output:**
```
<path>F:\Game-connect\frontend\lib</path>
<type>directory</type>
<entries>
api/
auth-context.tsx
types.ts

(3 entries)
</entries>
```

---

## Assistant (Plan · Big Pickle · 6.2s)

_Thinking:_

Let me dive deeper into the backend source structure to understand the existing codebase.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\main"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\main</path>
<type>directory</type>
<entries>
java/
resources/

(2 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\test"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\test</path>
<type>directory</type>
<entries>
java/

(1 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend\\app\\(app)"
}
```

**Output:**
```
<path>F:\Game-connect\frontend\app\(app)</path>
<type>directory</type>
<entries>
games/
layout.tsx

(2 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\frontend\\lib\\types.ts"
}
```

**Output:**
```
<path>F:\Game-connect\frontend\lib\types.ts</path>
<type>file</type>
<content>
1: export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
2: 
3: export type GameFormat = "5V5" | "6V6" | "7V7" | "8V8" | "9V9" | "11V11";
4: 
5: export type GameStatus = "OPEN" | "FULL" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
6: 
7: export type RequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";
8: 
9: export type ParticipantRole = "PLAYER" | "OWNER";
10: 
11: export interface UserProfile {
12:   id: string;
13:   email: string;
14:   displayName: string;
15:   bio: string | null;
16:   profileImageUrl: string | null;
17:   skillLevel: SkillLevel;
18:   createdAt: string;
19: }
20: 
21: export interface PlayerStats {
22:   matchesPlayed: number;
23:   matchesCompleted: number;
24:   attendanceRate: number | null;
25:   averageRating: number | null;
26: }
27: 
28: export interface Game {
29:   id: string;
30:   ownerId: string;
31:   turfName: string;
32:   turfAddress: string;
33:   latitude: number;
34:   longitude: number;
35:   gameDate: string;
36:   startTime: string;
37:   endTime: string;
38:   format: GameFormat;
39:   skillLevel: SkillLevel;
40:   maximumPlayers: number;
41:   requiredPlayers: number | null;
42:   joiningFee: number | null;
43:   description: string | null;
44:   status: GameStatus;
45:   createdAt: string;
46: }
47: 
48: export type GameSummary = Game;
49: 
50: export interface GameParticipant {
51:   id: string;
52:   gameId: string;
53:   userId: string;
54:   role: ParticipantRole;
55:   attended: boolean | null;
56:   joinedAt: string;
57: }
58: 
59: export interface JoinRequest {
60:   id: string;
61:   gameId: string;
62:   userId: string;
63:   status: RequestStatus;
64:   createdAt: string;
65:   decidedAt: string | null;
66: }
67: 
68: export interface PlayerRating {
69:   id: string;
70:   gameId: string;
71:   raterId: string;
72:   rateeId: string;
73:   score: number;
74:   createdAt: string;
75: }

(End of file - total 75 lines)
</content>

<system-reminder>
Instructions from: F:\Game-connect\frontend\AGENTS.md
<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

</system-reminder>
```

---

## Assistant (Plan · Big Pickle · 5.1s)

_Thinking:_

Let me explore the backend Java package structure and the existing entities, controllers, etc.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\main\\java"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\main\java</path>
<type>directory</type>
<entries>
com/

(1 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\test\\java"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\test\java</path>
<type>directory</type>
<entries>
com/

(1 entries)
</entries>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\main\\resources"
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\main\resources</path>
<type>directory</type>
<entries>
application.yml
db/
static/
templates/

(4 entries)
</entries>
```

---

## Assistant (Plan · Big Pickle · 645.3s)

_Thinking:_

Let me explore more deeply into the backend Java source and test directories.

**Tool: task**

**Input:**
```json
{
  "description": "Explore backend Java sources",
  "prompt": "Thoroughly explore the backend Java source tree at F:\\Game-connect\\backend\\src\\main\\java\\com and F:\\Game-connect\\backend\\src\\test\\java\\com. \n\nI need you to:\n1. List ALL Java source files (main and test) recursively\n2. Read the FULL contents of every Java file in the project\n3. Read F:\\Game-connect\\backend\\src\\main\\resources\\application.yml\n4. Read ALL files under F:\\Game-connect\\backend\\src\\main\\resources\\db\\\n5. Read the build.gradle file at F:\\Game-connect\\backend\\build.gradle\n6. Read F:\\Game-connect\\backend\\settings.gradle\n\nReturn the COMPLETE contents of every file you find, organized by directory. This is critical - I need to see every single file.",
  "subagent_type": "explore"
}
```

**Output:**
```
<task id="ses_f6f91e1c6ffeAjTlnjZ5tZIJl7" state="completed">
<task_result>
All files have been read. Here is the complete inventory and contents organized by directory.

---

## COMPLETE FILE INVENTORY

### Summary

| Location | Count | Files |
|---|---|---|
| `backend/src/main/java/com/gameconnect/` | 16 | Java source files |
| `backend/src/test/java/com/gameconnect/` | 5 | Java test files |
| `backend/src/main/resources/` | 1 | `application.yml` |
| `backend/src/main/resources/db/migration/` | 1 | `V1__init.sql` |
| `backend/` | 2 | `build.gradle`, `settings.gradle` |
| **Total** | **25** | |

---

## BUILD / PROJECT FILES

### `F:\Game-connect\backend\settings.gradle`
```groovy
rootProject.name = 'backend'
```

### `F:\Game-connect\backend\build.gradle`
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.1.1'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.gameconnect'
version = '0.0.1-SNAPSHOT'
description = 'Game Connect MVP'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-flyway'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.flywaydb:flyway-database-postgresql'
	implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
	runtimeOnly 'org.postgresql:postgresql'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-flyway-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
	testImplementation 'org.testcontainers:testcontainers-postgresql'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
	useJUnitPlatform()
	systemProperty 'user.timezone', 'Asia/Kolkata'
}
```

---

## RESOURCES

### `F:\Game-connect\backend\src\main\resources\application.yml`
```yaml
spring:
  application:
    name: backend
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5430/gameconnect}
    username: ${DB_USER:gameconnect}
    password: ${DB_PASSWORD:gameconnect}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${PORT:8080}

jwt:
  secret: ${JWT_SECRET:myDefaultDevSecretKeyMustBeAtLeast32BytesLongForHs256}
  expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}

cookie:
  secure: ${COOKIE_SECURE:false}

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### `F:\Game-connect\backend\src\main\resources\db\migration\V1__init.sql`
```sql
-- Game Connect MVP - initial schema
-- All primary keys are UUIDs. Statistics are derived from match_participant / player_rating
-- and are intentionally NOT stored as columns on app_user.

CREATE TABLE app_user (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    display_name      VARCHAR(80)  NOT NULL,
    bio               VARCHAR(2000),
    profile_image_url VARCHAR(500),
    skill_level       VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_skill CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE game (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id         UUID NOT NULL REFERENCES app_user (id),
    turf_name        VARCHAR(120) NOT NULL,
    turf_address     VARCHAR(255) NOT NULL,
    latitude         NUMERIC(9, 6) NOT NULL,
    longitude        NUMERIC(9, 6) NOT NULL,
    game_date        DATE         NOT NULL,
    start_time       TIMESTAMPTZ  NOT NULL,
    end_time         TIMESTAMPTZ  NOT NULL,
    format           VARCHAR(10)  NOT NULL,
    skill_level      VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    maximum_players  INTEGER      NOT NULL,
    required_players INTEGER,
    joining_fee      INTEGER,
    description      TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_game_status   CHECK (status IN ('OPEN', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_game_format   CHECK (format IN ('5V5', '6V6', '7V7', '8V8', '9V9', '11V11')),
    CONSTRAINT ck_game_skill    CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_game_capacity CHECK (maximum_players BETWEEN 5 AND 22),
    CONSTRAINT ck_game_window   CHECK (end_time > start_time),
    CONSTRAINT ck_req_players   CHECK (
        required_players IS NULL
        OR (required_players >= 1 AND required_players <= maximum_players)
    )
);

CREATE INDEX idx_game_status     ON game (status);
CREATE INDEX idx_game_date       ON game (game_date);
CREATE INDEX idx_game_owner      ON game (owner_id);
CREATE INDEX idx_game_skill_fmt  ON game (skill_level, format);
CREATE INDEX idx_game_window     ON game (start_time, end_time);

CREATE TABLE join_request (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id    UUID NOT NULL REFERENCES game (id),
    user_id    UUID NOT NULL REFERENCES app_user (id),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,

    CONSTRAINT ck_req_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT uq_req_game_user UNIQUE (game_id, user_id)
);

CREATE INDEX idx_join_request_game ON join_request (game_id, status);
CREATE INDEX idx_join_request_user ON join_request (user_id, status);

CREATE TABLE match_participant (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id   UUID NOT NULL REFERENCES game (id),
    user_id   UUID NOT NULL REFERENCES app_user (id),
    role      VARCHAR(10) NOT NULL DEFAULT 'PLAYER',
    attended  BOOLEAN,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_role          CHECK (role IN ('PLAYER', 'OWNER')),
    CONSTRAINT uq_participant   UNIQUE (game_id, user_id)
);

CREATE INDEX idx_participant_user ON match_participant (user_id);

CREATE TABLE player_rating (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id    UUID NOT NULL REFERENCES game (id),
    rater_id   UUID NOT NULL REFERENCES app_user (id),
    ratee_id   UUID NOT NULL REFERENCES app_user (id),
    score      SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT ck_rating_self  CHECK (rater_id <> ratee_id),
    CONSTRAINT uq_rating UNIQUE (game_id, rater_id, ratee_id)
);

CREATE INDEX idx_player_rating_ratee ON player_rating (ratee_id);
```

---

## MAIN JAVA SOURCES -- `com.gameconnect`

### `F:\Game-connect\backend\src\main\java\com\gameconnect\BackendApplication.java`
```java
package com.gameconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\web\HealthController.java`
```java
package com.gameconnect.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "game-connect",
                "timestamp", Instant.now()
        );
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\security\SecurityConfiguration.java`
```java
package com.gameconnect.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;
import com.gameconnect.common.exception.ApiError;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                                 ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/api/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            var body = ApiError.of(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "UNAUTHORIZED",
                                    "Authentication required",
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                            response.getWriter().flush();
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            var body = ApiError.of(
                                    HttpStatus.FORBIDDEN.value(),
                                    "FORBIDDEN",
                                    "Access denied",
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                            response.getWriter().flush();
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtService.java`
```java
package com.gameconnect.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationMinutes * 60 * 1000;
    }

    public String generateToken(String userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\security\JwtAuthenticationFilter.java`
```java
package com.gameconnect.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String COOKIE_NAME = "auth_token";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var claims = jwtService.parseToken(token);

            if (jwtService.isTokenExpired(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = jwtService.extractUserId(claims);
            String email = jwtService.extractEmail(claims);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            var principal = new AuthenticatedUser(UUID.fromString(userId), email);
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities);
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record AuthenticatedUser(UUID id, String email) {
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\ApiError.java`
```java
package com.gameconnect.common.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path);
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\BusinessException.java`
```java
package com.gameconnect.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level business rule violation. Map to a specific HTTP status/code.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\common\exception\GlobalExceptionHandler.java`
```java
package com.gameconnect.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ApiError body = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed: " + errors,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Request body is malformed", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\dto\AuthResponse.java`
```java
package com.gameconnect.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String displayName
) {
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\dto\LoginRequest.java`
```java
package com.gameconnect.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\dto\RegisterRequest.java`
```java
package com.gameconnect.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Display name is required")
        @Size(max = 80, message = "Display name must be at most 80 characters")
        String displayName
) {
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\dto\UserResponse.java`
```java
package com.gameconnect.auth.dto;

import java.util.UUID;

import com.gameconnect.auth.entity.User.SkillLevel;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String bio,
        String profileImageUrl,
        SkillLevel skillLevel
) {
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\entity\User.java`
```java
package com.gameconnect.auth.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User {

    public enum SkillLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(length = 2000)
    private String bio;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    private SkillLevel skillLevel = SkillLevel.BEGINNER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    protected User() {
    }

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\controller\AuthController.java`
```java
package com.gameconnect.auth.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameconnect.auth.dto.AuthResponse;
import com.gameconnect.auth.dto.LoginRequest;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.auth.dto.UserResponse;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.auth.service.AuthService;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.gameconnect.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "auth_token";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final long cookieMaxAge;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          JwtService jwtService,
                          @Value("${jwt.expiration-minutes}") long cookieMaxAgeMinutes,
                          @Value("${cookie.secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.cookieMaxAge = cookieMaxAgeMinutes * 60;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        String token = jwtService.generateToken(response.id().toString(), response.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        String token = jwtService.generateToken(response.id().toString(), response.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString())
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfileImageUrl(),
                user.getSkillLevel());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearedCookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }

    private ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .path("/")
                .maxAge(cookieMaxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();
    }
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\repository\UserRepository.java`
```java
package com.gameconnect.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gameconnect.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

---

### `F:\Game-connect\backend\src\main\java\com\gameconnect\auth\service\AuthService.java`
```java
package com.gameconnect.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gameconnect.auth.dto.AuthResponse;
import com.gameconnect.auth.dto.LoginRequest;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_EMAIL",
                    "An account with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), hashedPassword, request.displayName());
        userRepository.save(user);

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
```

---

## TEST JAVA SOURCES -- `com.gameconnect`

### `F:\Game-connect\backend\src\test\java\com\gameconnect\TestcontainersConfiguration.java`
```java
package com.gameconnect;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

}
```

---

### `F:\Game-connect\backend\src\test\java\com\gameconnect\TestBackendApplication.java`
```java
package com.gameconnect;

import org.springframework.boot.SpringApplication;

public class TestBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
```

---

### `F:\Game-connect\backend\src\test\java\com\gameconnect\BackendApplicationTests.java`
```java
package com.gameconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
```

---

### `F:\Game-connect\backend\src\test\java\com\gameconnect\FlywaySchemaTest.java`
```java
package com.gameconnect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that Flyway applies the full V1 schema against a real PostgreSQL
 * container, so later phases can rely on the migrations being correct.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywaySchemaTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesInitialMigration() {
        flyway.migrate();
        var applied = flyway.info().applied().length;
        assertThat(applied).as("at least the V1 migration is applied").isGreaterThanOrEqualTo(1);
    }

    @Test
    void coreTablesExist() {
        var tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables).contains(
                "app_user", "game", "join_request", "match_participant", "player_rating");
    }

    @Test
    void uniqueAndCheckConstraintsArePresent() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = tc.constraint_name
                WHERE tc.table_schema = 'public'
                """,
                String.class);
        assertThat(constraints).contains(
                "uq_app_user_email",
                "uq_req_game_user",
                "uq_participant",
                "uq_rating",
                "ck_game_status",
                "ck_rating_score",
                "ck_rating_self");
    }
}
```

---

### `F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java`
```java
package com.gameconnect.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.dto.LoginRequest;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    // -- Registration ------------------------------------------------

    @Test
    void register_withValidData_returns200AndCookie() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().sameSite("auth_token", "Lax"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("dup@example.com", "password123", "User One");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        RegisterRequest duplicate = new RegisterRequest("dup@example.com", "password456", "User Two");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "ab", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withBlankDisplayName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "  ");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // -- Login -------------------------------------------------------

    @Test
    void login_withValidCredentials_returns200AndCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest("login@example.com", "password123", "Login User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest("login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.displayName").value("Login User"))
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().sameSite("auth_token", "Lax"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("wrong@example.com", "password123", "Wrong User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest("wrong@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withNonExistentEmail_returns401() throws Exception {
        LoginRequest login = new LoginRequest("nonexistent@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        LoginRequest login = new LoginRequest("not-an-email", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // -- GET /api/auth/me --------------------------------------------

    @Test
    void me_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withValidJwt_returns200AndUserProfile() throws Exception {
        RegisterRequest reg = new RegisterRequest("me@example.com", "password123", "Me User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        String token = jwtService.generateToken(userId, "me@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.displayName").value("Me User"))
                .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    void me_withExpiredJwt_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("expired@example.com", "password123", "Expired User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        com.gameconnect.security.JwtService expiredJwtService =
                new com.gameconnect.security.JwtService(
                        "myDefaultDevSecretKeyMustBeAtLeast32BytesLongForHs256", 0);

        String expiredToken = expiredJwtService.generateToken(userId, "expired@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", "totally.invalid.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // -- Logout ------------------------------------------------------

    @Test
    void logout_clearsCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest("logout@example.com", "password123", "Logout User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = regResult.getResponse().getCookie("auth_token");

        mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().maxAge("auth_token", 0));
    }

    // -- Public endpoint ---------------------------------------------

    @Test
    void health_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // -- Protected endpoint without auth -----------------------------

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withValidJwt_returns200() throws Exception {
        RegisterRequest reg = new RegisterRequest("protected@example.com", "password123", "Protected User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();

...2836 bytes truncated...

The tool call succeeded but the output was truncated. Full output saved to: C:\Users\KIIT\.local\share\opencode\tool-output\tool_0907201020016Dc7g6jYE8MhSP
Use the Task tool to have explore agent process this file with Grep and Read (with offset/limit). Do NOT read the full file yourself - delegate to save context.
```

**Tool: task**

**Input:**
```json
{
  "description": "Explore frontend source files",
  "prompt": "Thoroughly explore the frontend source tree at F:\\Game-connect\\frontend.\n\nI need you to:\n1. List ALL source files recursively (app/, components/, lib/ directories)\n2. Read the FULL contents of every TypeScript/TSX file in app/, components/, and lib/\n3. Read package.json at F:\\Game-connect\\frontend\\package.json\n4. Read next.config.ts\n5. Read tsconfig.json\n6. Read globals.css\n7. Read the proxy.ts file\n8. Read any configuration files like postcss.config.mjs, eslint.config.mjs\n\nReturn the COMPLETE contents of every file you find, organized by directory. This is critical - I need to see every single file.",
  "subagent_type": "explore"
}
```

**Output:**
```
<task id="ses_f6f91dba0ffezUhTM2L9UIg5nq" state="completed">
<task_result>
I have explored the complete frontend source tree. Here is the full, organized report of every file found.

---

# Frontend Source Tree — F:\Game-connect\frontend

## Directory Structure

```
frontend/
├── .gitignore
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── eslint.config.mjs
├── next-env.d.ts
├── next.config.ts
├── package.json
├── package-lock.json          (locked deps, not source)
├── postcss.config.mjs
├── proxy.ts
├── tsconfig.json
├── app/
│   ├── globals.css
│   ├── layout.tsx
│   ├── page.tsx
│   ├── favicon.ico            (binary)
│   ├── (app)/
│   │   ├── layout.tsx
│   │   └── games/
│   │       ├── page.tsx
│   │       └── [id]/
│   │           └── page.tsx
│   ├── login/
│   │   └── page.tsx
│   └── register/
│       └── page.tsx
├── components/
│   └── layout/
│       └── BottomNav.tsx
├── lib/
│   ├── auth-context.tsx
│   ├── types.ts
│   └── api/
│       ├── auth.ts
│       └── client.ts
├── public/                    (5 SVG assets + favicon, not source)
└── node_modules/              (excluded)
```

---

## 1. Configuration Files (Repository Root)

### F:\Game-connect\frontend\package.json

```json
{
  "name": "gc-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "eslint"
  },
  "dependencies": {
    "next": "16.3.4",
    "react": "19.2.8",
    "react-dom": "19.2.8"
  },
  "devDependencies": {
    "@tailwindcss/postcss": "^4",
    "@types/node": "^20",
    "@types/react": "^19",
    "@types/react-dom": "^19",
    "eslint": "^9",
    "eslint-config-next": "16.3.4",
    "tailwindcss": "^4",
    "typescript": "^5"
  }
}
```

### F:\Game-connect\frontend\next.config.ts

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"}/:path*`,
      },
    ];
  },
};

export default nextConfig;
```

### F:\Game-connect\frontend\tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "react-jsx",
    "incremental": true,
    "plugins": [
      {
        "name": "next"
      }
    ],
    "paths": {
      "@/*": ["./*"]
    }
  },
  "include": [
    "next-env.d.ts",
    "**/*.ts",
    "**/*.tsx",
    ".next/types/**/*.ts",
    ".next/dev/types/**/*.ts",
    "**/*.mts"
  ],
  "exclude": ["node_modules"]
}
```

### F:\Game-connect\frontend\postcss.config.mjs

```js
const config = {
  plugins: {
    "@tailwindcss/postcss": {},
  },
};

export default config;
```

### F:\Game-connect\frontend\eslint.config.mjs

```js
import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
```

### F:\Game-connect\frontend\next-env.d.ts

```ts
/// <reference types="next" />
/// <reference types="next/image-types/global" />
import "./.next/dev/types/routes.d.ts";
import "./.next/dev/types/root-params.d.ts";

// NOTE: This file should not be edited
// see https://nextjs.org/docs/app/api-reference/config/typescript for more information.
```

### F:\Game-connect\frontend\proxy.ts

```ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const AUTH_COOKIE = "auth_token";

const PUBLIC_PATHS = ["/login", "/register"];
const PROTECTED_PATHS = ["/games", "/create-game", "/my-games", "/profile"];

function hasAuthCookie(request: NextRequest): boolean {
  return request.cookies.has(AUTH_COOKIE);
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isAuthenticated = hasAuthCookie(request);

  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p))) {
    if (isAuthenticated) {
      return NextResponse.redirect(new URL("/games", request.url));
    }
    return NextResponse.next();
  }

  if (PROTECTED_PATHS.some((p) => pathname.startsWith(p))) {
    if (!isAuthenticated) {
      return NextResponse.redirect(new URL("/login", request.url));
    }
    return NextResponse.next();
  }

  if (pathname === "/") {
    return NextResponse.redirect(
      new URL(isAuthenticated ? "/games" : "/login", request.url),
    );
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico).*)",
  ],
};
```

### F:\Game-connect\frontend\.gitignore

```
# See https://help.github.com/articles/ignoring-files/ for more about ignoring files.

# dependencies
/node_modules
/.pnp
.pnp.*
.yarn/*
!.yarn/patches
!.yarn/plugins
!.yarn/releases
!.yarn/versions

# testing
/coverage

# next.js
/.next/
/out/

# production
/build

# misc
.DS_Store
*.pem

# debug
npm-debug.log*
yarn-debug.log*
yarn-error.log*
.pnpm-debug.log*

# env files (can opt-in for committing if needed)
.env*

# vercel
.vercel

# typescript
*.tsbuildinfo
next-env.d.ts
```

### F:\Game-connect\frontend\README.md

```
This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
```

### F:\Game-connect\frontend\AGENTS.md

```
<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
```

### F:\Game-connect\frontend\CLAUDE.md

```
@AGENTS.md
```

---

## 2. app/ Directory

### F:\Game-connect\frontend\app\globals.css

```css
@import "tailwindcss";

:root {
  --background: #ffffff;
  --foreground: #171717;
}

@theme inline {
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --font-sans: var(--font-geist-sans);
  --font-mono: var(--font-geist-mono);
}

@media (prefers-color-scheme: dark) {
  :root {
    --background: #0a0a0a;
    --foreground: #ededed;
  }
}

body {
  background: var(--background);
  color: var(--foreground);
  font-family: Arial, Helvetica, sans-serif;
}
```

### F:\Game-connect\frontend\app\layout.tsx

```tsx
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/lib/auth-context";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Game Connect",
  description: "Pickup football matchmaking",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
```

### F:\Game-connect\frontend\app\page.tsx

```tsx
export default function Home() {
  return null;
}
```

### F:\Game-connect\frontend\app\(app)\layout.tsx

```tsx
import type { ReactNode } from "react";

import { BottomNav } from "@/components/layout/BottomNav";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex min-h-full w-full max-w-md flex-col">
      <main className="flex-1 pb-20">{children}</main>
      <BottomNav />
    </div>
  );
}
```

### F:\Game-connect\frontend\app\(app)\games\page.tsx

```tsx
import Link from "next/link";

import type { GameSummary } from "@/lib/types";

const PLACEHOLDER_GAMES: GameSummary[] = [];

export default function GamesPage() {
  return (
    <div className="p-4">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Games</h1>
        <Link
          href="/create-game"
          className="rounded-full bg-emerald-600 px-4 py-2 text-sm font-medium text-white"
        >
          Create
        </Link>
      </header>

      {PLACEHOLDER_GAMES.length === 0 && (
        <div className="mt-10 flex flex-col items-center gap-2 text-center text-zinc-500">
          <p className="text-sm">No games available yet.</p>
          <p className="text-xs">
            Game discovery arrives in a later phase; this is the Phase 1 shell.
          </p>
        </div>
      )}
    </div>
  );
}
```

### F:\Game-connect\frontend\app\(app)\games\[id]\page.tsx

```tsx
import { notFound } from "next/navigation";

import type PageProps from "next";

export default async function GameDetailPage(props: PageProps<"/games/[id]">) {
  const { id } = await props.params;

  if (!id) {
    notFound();
  }

  return (
    <div className="p-4">
      <h1 className="text-xl font-semibold">Game</h1>
      <p className="mt-2 text-sm text-zinc-600">
        Game <span className="font-mono">{id}</span>. Detail view arrives in a
        later phase.
      </p>
    </div>
  );
}
```

### F:\Game-connect\frontend\app\login\page.tsx

```tsx
"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api/client";

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-full flex-col items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-8 text-center text-2xl font-bold text-zinc-900">
          Game Connect
        </h1>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-zinc-700">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-zinc-700">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              placeholder="At least 8 characters"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 w-full rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
          >
            {submitting ? "Signing in..." : "Sign In"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-zinc-500">
          Don&apos;t have an account?{" "}
          <Link href="/register" className="font-medium text-emerald-600 hover:text-emerald-700">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
```

### F:\Game-connect\frontend\app\register\page.tsx

```tsx
"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api/client";

export default function RegisterPage() {
  const { register } = useAuth();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(email, password, displayName);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-full flex-col items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <h1 className="mb-8 text-center text-2xl font-bold text-zinc-900">
          Game Connect
        </h1>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && (
            <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="displayName" className="mb-1 block text-sm font-medium text-zinc-700">
              Display Name
            </label>
            <input
              id="displayName"
              type="text"
              required
              maxLength={80}
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              placeholder="Your name"
            />
          </div>

          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-zinc-700">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-zinc-700">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              placeholder="At least 8 characters"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 w-full rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
          >
            {submitting ? "Creating account..." : "Create Account"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-zinc-500">
          Already have an account?{" "}
          <Link href="/login" className="font-medium text-emerald-600 hover:text-emerald-700">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
```

Note: `app/favicon.ico` is a binary file (25,931 bytes), not text, so its contents are not included.

---

## 3. components/ Directory

### F:\Game-connect\frontend\components\layout\BottomNav.tsx

```tsx
"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { useState } from "react";

const NAV_ITEMS = [
  { href: "/games", label: "Games" },
  { href: "/create-game", label: "Create" },
  { href: "/my-games", label: "My Games" },
  { href: "/profile", label: "Profile" },
] as const;

export function BottomNav() {
  const pathname = usePathname();
  const { logout } = useAuth();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  const isActive = (href: string) =>
    href === "/games" ? pathname.startsWith("/games") : pathname === href;

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      router.push("/login");
    }
  }

  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 border-t border-zinc-200 bg-white">
      <ul className="mx-auto flex w-full max-w-md items-stretch justify-around">
        {NAV_ITEMS.map((item) => (
          <li key={item.href} className="flex-1">
            <Link
              href={item.href}
              className={`flex flex-col items-center gap-0.5 py-2.5 text-xs font-medium transition-colors ${
                isActive(item.href)
                  ? "text-emerald-600"
                  : "text-zinc-500 hover:text-zinc-800"
              }`}
            >
              <span>{item.label}</span>
            </Link>
          </li>
        ))}
        <li className="flex-1">
          <button
            onClick={handleLogout}
            disabled={loggingOut}
            className="flex w-full flex-col items-center gap-0.5 py-2.5 text-xs font-medium text-zinc-500 transition-colors hover:text-zinc-800 disabled:opacity-50"
          >
            <span>{loggingOut ? "Signing out..." : "Sign Out"}</span>
          </button>
        </li>
      </ul>
    </nav>
  );
}
```

---

## 4. lib/ Directory

### F:\Game-connect\frontend\lib\types.ts

```ts
export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export type GameFormat = "5V5" | "6V6" | "7V7" | "8V8" | "9V9" | "11V11";

export type GameStatus = "OPEN" | "FULL" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type RequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";

export type ParticipantRole = "PLAYER" | "OWNER";

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel;
  createdAt: string;
}

export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
}

export interface Game {
  id: string;
  ownerId: string;
  turfName: string;
  turfAddress: string;
  latitude: number;
  longitude: number;
  gameDate: string;
  startTime: string;
  endTime: string;
  format: GameFormat;
  skillLevel: SkillLevel;
  maximumPlayers: number;
  requiredPlayers: number | null;
  joiningFee: number | null;
  description: string | null;
  status: GameStatus;
  createdAt: string;
}

export type GameSummary = Game;

export interface GameParticipant {
  id: string;
  gameId: string;
  userId: string;
  role: ParticipantRole;
  attended: boolean | null;
  joinedAt: string;
}

export interface JoinRequest {
  id: string;
  gameId: string;
  userId: string;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
}

export interface PlayerRating {
  id: string;
  gameId: string;
  raterId: string;
  rateeId: string;
  score: number;
  createdAt: string;
}
```

### F:\Game-connect\frontend\lib\auth-context.tsx

```tsx
"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchMe as apiFetchMe, login as apiLogin, logout as apiLogout, register as apiRegister } from "./api/auth";
import type { AuthUser } from "./api/auth";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    apiFetchMe()
      .then((u) => {
        if (!cancelled) setUser(u);
      })
      .catch(() => {
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const u = await apiLogin(email, password);
    setUser(u);
    router.push("/games");
  }, [router]);

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    const u = await apiRegister(email, password, displayName);
    setUser(u);
    router.push("/games");
  }, [router]);

  const logout = useCallback(async () => {
    await apiLogout();
    setUser(null);
    router.push("/login");
  }, [router]);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
```

### F:\Game-connect\frontend\lib\api\client.ts

```ts
const API_BASE_URL = "/api";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    credentials: "include",
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const payload = await response.text();
    let code = "UNKNOWN";
    let message = `Request failed with status ${response.status}`;
    try {
      const parsed = JSON.parse(payload);
      if (parsed?.code) code = parsed.code;
      if (parsed?.message) message = parsed.message;
    } catch {
      // non-JSON error body; keep defaults
    }
    throw new ApiError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
```

### F:\Game-connect\frontend\lib\api\auth.ts

```ts
import { apiFetch } from "./client";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
}

export interface MeUser extends AuthUser {
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: string | null;
}

export async function register(email: string, password: string, displayName: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/register", {
    method: "POST",
    body: { email, password, displayName },
  });
}

export async function login(email: string, password: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export async function fetchMe(): Promise<MeUser> {
  return apiFetch<MeUser>("/auth/me");
}

export async function logout(): Promise<void> {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}
```

---

## 5. public/ Directory (Static Assets — non-source)

- `F:\Game-connect\frontend\public\file.svg` (391 bytes)
- `F:\Game-connect\frontend\public\globe.svg` (1,035 bytes)
- `F:\Game-connect\frontend\public\next.svg` (1,375 bytes)
- `F:\Game-connect\frontend\public\vercel.svg` (128 bytes)
- `F:\Game-connect\frontend\public\window.svg` (385 bytes)

These are default `create-next-app` SVG assets; none are referenced by the current source.

---

## Summary of Key Findings

- **Framework**: Next.js 16.3.4 (App Router), React 19.2.8, Tailwind CSS v4, TypeScript 5. The repo's `AGENTS.md` warns this is Next.js 16 with breaking changes vs. older versions.
- **Routing structure**: 
  - Public root layout wraps everything in `AuthProvider`.
  - `/(app)` route group holds authenticated pages (`/games`, `/games/[id]`) with the fixed `BottomNav` and a mobile-style `max-w-md` column.
  - `/login` and `/register` are standalone auth pages (client components).
- **Auth model**: Cookie-based (`auth_token`), enforced two ways — middleware via `proxy.ts` (redirects unauthenticated users from `/games`, `/create-game`, `/my-games`, `/profile` to `/login`) and client-side via `AuthProvider`/`useAuth`.
- **API layer**: `lib/api/client.ts` provides `apiFetch` (wraps `fetch` against `/api`, `credentials: "include"`, parses `{code, message}` errors into `ApiError`). `next.config.ts` rewrites `/api/*` to a backend at `NEXT_PUBLIC_API_BASE_URL` or `http://localhost:8080/api`. Current endpoints: `/auth/register`, `/auth/login`, `/auth/me`, `/auth/logout`.
- **Placeholder state**: The games list is empty by design (`PLACEHOLDER_GAMES: GameSummary[] = []` — "Phase 1 shell"); game detail is also a stub. `/create-game`, `/my-games`, and `/profile` are referenced in nav and proxy config but have **no route files yet**.
- **Type model**: `lib/types.ts` defines domain types for the game/player domain (UserProfile, Game, GameParticipant, JoinRequest, PlayerRating, etc.), anticipating a Java-style enum backend (OPEN/FULL, BEGINNER/INTERMEDIATE/ADVANCED, etc.).

No other TypeScript/TSX files exist in `app/`, `components/`, or `lib/` beyond those listed above.
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 98.3s)

_Thinking:_

Now I have a comprehensive understanding of the codebase. Let me also check the truncated test file to get the full AuthIntegrationTest.

Now let me read the remaining truncated portion of the auth integration test and the docker-compose for completeness.

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\backend\\src\\test\\java\\com\\gameconnect\\auth\\AuthIntegrationTest.java",
  "offset": 135
}
```

**Output:**
```
<path>F:\Game-connect\backend\src\test\java\com\gameconnect\auth\AuthIntegrationTest.java</path>
<type>file</type>
<content>
135:                 .andExpect(status().isOk())
136:                 .andExpect(jsonPath("$.email").value("login@example.com"))
137:                 .andExpect(jsonPath("$.displayName").value("Login User"))
138:                 .andExpect(cookie().exists("auth_token"))
139:                 .andExpect(cookie().httpOnly("auth_token", true))
140:                 .andExpect(cookie().path("auth_token", "/"))
141:                 .andExpect(cookie().sameSite("auth_token", "Lax"));
142:     }
143: 
144:     @Test
145:     void login_withWrongPassword_returns401() throws Exception {
146:         RegisterRequest reg = new RegisterRequest("wrong@example.com", "password123", "Wrong User");
147:         mockMvc.perform(post("/api/auth/register")
148:                         .contentType(MediaType.APPLICATION_JSON)
149:                         .content(objectMapper.writeValueAsString(reg)))
150:                 .andExpect(status().isOk());
151: 
152:         LoginRequest login = new LoginRequest("wrong@example.com", "wrongpassword");
153: 
154:         mockMvc.perform(post("/api/auth/login")
155:                         .contentType(MediaType.APPLICATION_JSON)
156:                         .content(objectMapper.writeValueAsString(login)))
157:                 .andExpect(status().isUnauthorized())
158:                 .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
159:     }
160: 
161:     @Test
162:     void login_withNonExistentEmail_returns401() throws Exception {
163:         LoginRequest login = new LoginRequest("nonexistent@example.com", "password123");
164: 
165:         mockMvc.perform(post("/api/auth/login")
166:                         .contentType(MediaType.APPLICATION_JSON)
167:                         .content(objectMapper.writeValueAsString(login)))
168:                 .andExpect(status().isUnauthorized())
169:                 .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
170:     }
171: 
172:     @Test
173:     void login_withInvalidEmailFormat_returns400() throws Exception {
174:         LoginRequest login = new LoginRequest("not-an-email", "password123");
175: 
176:         mockMvc.perform(post("/api/auth/login")
177:                         .contentType(MediaType.APPLICATION_JSON)
178:                         .content(objectMapper.writeValueAsString(login)))
179:                 .andExpect(status().isBadRequest())
180:                 .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
181:     }
182: 
183:     // ── GET /api/auth/me ──────────────────────────────────────
184: 
185:     @Test
186:     void me_withoutAuth_returns401() throws Exception {
187:         mockMvc.perform(get("/api/auth/me"))
188:                 .andExpect(status().isUnauthorized())
189:                 .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
190:     }
191: 
192:     @Test
193:     void me_withValidJwt_returns200AndUserProfile() throws Exception {
194:         RegisterRequest reg = new RegisterRequest("me@example.com", "password123", "Me User");
195:         MvcResult regResult = mockMvc.perform(post("/api/auth/register")
196:                         .contentType(MediaType.APPLICATION_JSON)
197:                         .content(objectMapper.writeValueAsString(reg)))
198:                 .andExpect(status().isOk())
199:                 .andReturn();
200: 
201:         String responseBody = regResult.getResponse().getContentAsString();
202:         String userId = objectMapper.readTree(responseBody).get("id").asText();
203: 
204:         String token = jwtService.generateToken(userId, "me@example.com");
205: 
206:         mockMvc.perform(get("/api/auth/me")
207:                         .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
208:                 .andExpect(status().isOk())
209:                 .andExpect(jsonPath("$.email").value("me@example.com"))
210:                 .andExpect(jsonPath("$.displayName").value("Me User"))
211:                 .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
212:                 .andExpect(jsonPath("$.id").value(userId));
213:     }
214: 
215:     @Test
216:     void me_withExpiredJwt_returns401() throws Exception {
217:         RegisterRequest reg = new RegisterRequest("expired@example.com", "password123", "Expired User");
218:         MvcResult regResult = mockMvc.perform(post("/api/auth/register")
219:                         .contentType(MediaType.APPLICATION_JSON)
220:                         .content(objectMapper.writeValueAsString(reg)))
221:                 .andExpect(status().isOk())
222:                 .andReturn();
223: 
224:         String responseBody = regResult.getResponse().getContentAsString();
225:         String userId = objectMapper.readTree(responseBody).get("id").asText();
226: 
227:         com.gameconnect.security.JwtService expiredJwtService =
228:                 new com.gameconnect.security.JwtService(
229:                         "myDefaultDevSecretKeyMustBeAtLeast32BytesLongForHs256", 0);
230: 
231:         String expiredToken = expiredJwtService.generateToken(userId, "expired@example.com");
232: 
233:         mockMvc.perform(get("/api/auth/me")
234:                         .cookie(new jakarta.servlet.http.Cookie("auth_token", expiredToken)))
235:                 .andExpect(status().isUnauthorized())
236:                 .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
237:     }
238: 
239:     @Test
240:     void me_withInvalidJwt_returns401() throws Exception {
241:         mockMvc.perform(get("/api/auth/me")
242:                         .cookie(new jakarta.servlet.http.Cookie("auth_token", "totally.invalid.token")))
243:                 .andExpect(status().isUnauthorized())
244:                 .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
245:     }
246: 
247:     // ── Logout ────────────────────────────────────────────────
248: 
249:     @Test
250:     void logout_clearsCookie() throws Exception {
251:         RegisterRequest reg = new RegisterRequest("logout@example.com", "password123", "Logout User");
252:         MvcResult regResult = mockMvc.perform(post("/api/auth/register")
253:                         .contentType(MediaType.APPLICATION_JSON)
254:                         .content(objectMapper.writeValueAsString(reg)))
255:                 .andExpect(status().isOk())
256:                 .andReturn();
257: 
258:         Cookie authCookie = regResult.getResponse().getCookie("auth_token");
259: 
260:         mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
261:                 .andExpect(status().isNoContent())
262:                 .andExpect(cookie().exists("auth_token"))
263:                 .andExpect(cookie().maxAge("auth_token", 0));
264:     }
265: 
266:     // ── Public endpoint ───────────────────────────────────────
267: 
268:     @Test
269:     void health_withoutAuth_returns200() throws Exception {
270:         mockMvc.perform(get("/api/health"))
271:                 .andExpect(status().isOk())
272:                 .andExpect(jsonPath("$.status").value("UP"));
273:     }
274: 
275:     // ── Protected endpoint without auth ───────────────────────
276: 
277:     @Test
278:     void protectedEndpoint_withoutAuth_returns401() throws Exception {
279:         mockMvc.perform(get("/api/auth/me"))
280:                 .andExpect(status().isUnauthorized())
281:                 .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
282:     }
283: 
284:     @Test
285:     void protectedEndpoint_withValidJwt_returns200() throws Exception {
286:         RegisterRequest reg = new RegisterRequest("protected@example.com", "password123", "Protected User");
287:         MvcResult regResult = mockMvc.perform(post("/api/auth/register")
288:                         .contentType(MediaType.APPLICATION_JSON)
289:                         .content(objectMapper.writeValueAsString(reg)))
290:                 .andExpect(status().isOk())
291:                 .andReturn();
292: 
293:         String responseBody = regResult.getResponse().getContentAsString();
294:         String userId = objectMapper.readTree(responseBody).get("id").asText();
295: 
296:         String token = jwtService.generateToken(userId, "protected@example.com");
297: 
298:         mockMvc.perform(get("/api/auth/me")
299:                         .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
300:                 .andExpect(status().isOk())
301:                 .andExpect(jsonPath("$.email").value("protected@example.com"));
302:     }
303: 
304:     // ── Cookie security attributes ────────────────────────────
305: 
306:     @Test
307:     void register_setsCookieWithSecureFalseByDefault() throws Exception {
308:         RegisterRequest request = new RegisterRequest("cookie@example.com", "password123", "Cookie User");
309: 
310:         mockMvc.perform(post("/api/auth/register")
311:                         .contentType(MediaType.APPLICATION_JSON)
312:                         .content(objectMapper.writeValueAsString(request)))
313:                 .andExpect(status().isOk())
314:                 .andExpect(cookie().exists("auth_token"))
315:                 .andExpect(cookie().httpOnly("auth_token", true))
316:                 .andExpect(cookie().path("auth_token", "/"))
317:                 .andExpect(cookie().sameSite("auth_token", "Lax"));
318:     }
319: }

(End of file - total 319 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\docker-compose.yml"
}
```

**Output:**
```
<path>F:\Game-connect\docker-compose.yml</path>
<type>file</type>
<content>
1: services:
2:   db:
3:     image: postgres:16
4:     container_name: gameconnect-db
5:     environment:
6:       POSTGRES_DB: gameconnect
7:       POSTGRES_USER: gameconnect
8:       POSTGRES_PASSWORD: gameconnect
9:     ports:
10:       - "5430:5432"
11:     volumes:
12:       - gameconnect-pgdata:/var/lib/postgresql/data
13:     healthcheck:
14:       test: ["CMD-SHELL", "pg_isready -U gameconnect -d gameconnect"]
15:       interval: 5s
16:       timeout: 5s
17:       retries: 10
18: 
19: volumes:
20:   gameconnect-pgdata:

(End of file - total 20 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "F:\\Game-connect\\planning_phase2.md"
}
```

**Output:**
```
<path>F:\Game-connect\planning_phase2.md</path>
<type>file</type>
<content>
1: # Planning Phase 2 Authentication
2: 
3: **Session ID:** ses_f7b239a03ffervlUyYo7IcLmHD
4: **Created:** 9/9/2026, 12:00:31 PM
5: **Updated:** 9/9/2026, 12:03:43 PM
6: 
7: ---
8: 
9: ## User
10: 
11: Phase 1 is complete and verified.
12: 
13: Now plan Phase 2: Authentication only.
14: 
15: Before planning:
16: 
17: * Inspect the current repository.
18: * Read AGENTS.md and PRODUCT.md.
19: * Inspect what was actually implemented in Phase 1.
20: * Do not redesign the existing architecture.
21: * Preserve the modular monolith structure.
22: 
23: Phase 2 should implement:
24: 
25: 1. User authentication
26: 
27:    * User registration
28:    * User login
29:    * Password hashing using BCrypt
30:    * JWT-based authentication
31:    * Secure HTTP-only cookie-based authentication
32:    * GET /api/auth/me
33: 
34: 2. Spring Security
35: 
36:    * Security configuration
37:    * Public vs authenticated endpoints
38:    * JWT authentication filter/mechanism
39:    * Current authenticated user resolved from the JWT
40:    * Never trust a client-provided userId for the current user
41: 
42: 3. Validation and errors
43: 
44:    * Duplicate email handling
45:    * Invalid credentials handling
46:    * Request validation
47:    * Consistent API error responses
48: 
49: 4. Configuration
50: 
51:    * JWT secret and expiration from environment variables
52:    * No secrets hardcoded in source code
53: 
54: 5. Testing
55: 
56:    * Registration success/failure
57:    * Duplicate email
58:    * Login success/failure
59:    * Protected endpoint without authentication
60:    * Protected endpoint with valid JWT
61:    * Invalid/expired JWT
62:    * /api/auth/me
63: 
64: Keep the implementation minimal and MVP-focused.
65: 
66: Do NOT implement:
67: 
68: * Game functionality
69: * Join requests
70: * Ratings
71: * Notifications
72: * Payments
73: * Chat
74: * Refresh tokens unless the existing architecture specifically requires them
75: * OAuth/social login
76: * Redis
77: * Additional infrastructure
78: 
79: First give me the revised Phase 2 implementation plan and the files/classes that will be created or modified.
80: 
81: Do not write code yet. 
82: 
83: ---
84: 
85: ## Assistant (Plan · Big Pickle · 23.8s)
86: 
87: _Thinking:_
88: 
89: I'll start by exploring the repository structure to understand what was implemented in Phase 1. First, let me check the current directory and read AGENTS.md and PRODUCT.md.
90: 
91: **Tool: read**
92: 
93: **Input:**
94: ```json
95: {
96:   "filePath": "F:\\Game-connect\\AGENTS.md"
97: }
98: ```
99: 
100: **Output:**
101: ```
102: <path>F:\Game-connect\AGENTS.md</path>
103: <type>file</type>
104: <content>
105: 1: # AGENTS.md
106: 2: 
107: 3: > This repo is newly initialized. Fill in the sections below as the codebase takes shape.
108: 4: 
109: 5: ## Build & Run
110: 6: 
111: 7: _(add commands as tooling is set up)_
112: 8: 
113: 9: ## Test
114: 10: 
115: 11: _(add test runner, single-test commands, and any prereqs)_
116: 12: 
117: 13: ## Lint & Format
118: 14: 
119: 15: _(add linter/formatter commands)_
120: 16: 
121: 17: ## Architecture
122: 18: 
123: 19: _(add key entrypoints, package boundaries, and structural notes)_
124: 20: 
125: 21: ## Conventions
126: 22: 
127: 23: _(add repo-specific style, workflow, or naming conventions)_
128: 
129: (End of file - total 23 lines)
130: </content>
131: ```
132: 
133: **Tool: read**
134: 
135: **Input:**
136: ```json
137: {
138:   "filePath": "F:\\Game-connect\\PRODUCT.md"
139: }
140: ```
141: 
142: **Output:**
143: ```
144: <path>F:\Game-connect\PRODUCT.MD</path>
145: <type>file</type>
146: <content>
147: 1: # Game Connect
148: 2: 
149: 3: ## Product
150: 4: 
151: 5: Football Connect is a pickup football matchmaking platform.
152: 6: 
153: 7: Users who have already booked a football turf can create a game on the platform and invite random players to join.
154: 8: 
155: 9: Players can discover games and request to join.
156: 10: 
157: 11: The game owner has authority to accept or reject players based on their profile, experience, skill level and previous participation.
158: 12: 
159: 13: The platform does NOT initially handle turf booking.
160: 14: Users manually enter the turf and booking information.
161: 15: 
162: 16: ---
163: 17: 
164: 18: # Core User Roles
165: 19: 
166: 20: ## Player
167: 21: 
168: 22: A player can:
169: 23: 
170: 24: - Register/login
171: 25: - Create and edit profile
172: 26: - Set skill level
173: 27: - Browse available games
174: 28: - Filter games
175: 29: - Request to join a game
176: 30: - Cancel their request
177: 31: - View accepted games
178: 32: - View match history
179: 33: - View their football statistics
180: 34: 
181: 35: ## Game Owner
182: 36: 
183: 37: A game owner is also a player.
184: 38: 
185: 39: They can:
186: 40: 
187: 41: - Create a game
188: 42: - Define game format
189: 43: - Define maximum players
190: 44: - Define required players
191: 45: - Set skill level
192: 46: - Set joining fee
193: 47: - View join requests
194: 48: - Accept players
195: 49: - Reject players
196: 50: - Remove a player before the match
197: 51: - Mark player attendance after the match
198: 52: - Close the game
199: 53: 
200: 54: ---
201: 55: 
202: 56: # MVP
203: 57: 
204: 58: The MVP should contain:
205: 59: 
206: 60: 1. Authentication
207: 61: 2. Player profiles
208: 62: 3. Create game
209: 63: 4. Browse games
210: 64: 5. Game details
211: 65: 6. Request to join
212: 66: 7. Accept/reject requests
213: 67: 8. Player statistics
214: 68: 9. Match history
215: 69: 10. Attendance
216: 70: 
217: 71: Do NOT implement:
218: 72: 
219: 73: - Turf owner integration
220: 74: - Online payments
221: 75: - Chat
222: 76: - Push notifications
223: 77: - Advanced recommendation algorithms
224: 78: - Teams
225: 79: - Tournaments
226: 80: - Subscriptions
227: 81: - Microservices
228: 82: 
229: 83: Keep the MVP small.
230: 84: 
231: 85: ---
232: 86: 
233: 87: # Player Profile
234: 88: 
235: 89: A player has:
236: 90: 
237: 91: - id
238: 92: - name
239: 93: - profile image
240: 94: - bio
241: 95: - skill level
242: 96: - matches played
243: 97: - matches completed
244: 98: - attendance rate
245: 99: - average rating
246: 100: 
247: 101: Skill levels:
248: 102: 
249: 103: BEGINNER
250: 104: INTERMEDIATE
251: 105: ADVANCED
252: 106: 
253: 107: Positions:
254: 108: 
255: 109: GOALKEEPER
256: 110: DEFENDER
257: 111: MIDFIELDER
258: 112: WINGER
259: 113: STRIKER
260: 114: FLEXIBLE
261: 115: 
262: 116: ---
263: 117: 
264: 118: # Game
265: 119: 
266: 120: A game contains:
267: 121: 
268: 122: - id
269: 123: - owner
270: 124: - turf name
271: 125: - turf address
272: 126: - latitude
273: 127: - longitude
274: 128: - date
275: 129: - start time
276: 130: - end time
277: 131: - format
278: 132: - skill level
279: 133: - maximum players
280: 134: - joining fee
281: 135: - description
282: 136: - status
283: 137: 
284: 138: Formats:
285: 139: 
286: 140: 5V5
287: 141: 6V6
288: 142: 7V7
289: 143: 8V8
290: 144: 9V9
291: 145: 11V11
292: 146: 
293: 147: Game statuses:
294: 148: 
295: 149: OPEN
296: 150: FULL
297: 151: IN_PROGRESS
298: 152: COMPLETED
299: 153: CANCELLED
300: 154: 
301: 155: ---
302: 156: 
303: 157: # Join Request
304: 158: 
305: 159: A player can request to join an OPEN game.
306: 160: 
307: 161: Request states:
308: 162: 
309: 163: PENDING
310: 164: ACCEPTED
311: 165: REJECTED
312: 166: CANCELLED
313: 167: 
314: 168: A player cannot have multiple active requests for the same game.
315: 169: 
316: 170: A player cannot request to join their own game.
317: 171: 
318: 172: A player cannot request to join a FULL game.
319: 173: 
320: 174: Once the maximum number of players is reached, the game becomes FULL.
321: 175: 
322: 176: ---
323: 177: 
324: 178: # Match Participation
325: 179: 
326: 180: After the game:
327: 181: 
328: 182: The game owner marks whether each accepted player attended.
329: 183: 
330: 184: Attendance affects the player's statistics.
331: 185: 
332: 186: No-show should negatively affect attendance percentage.
333: 187: 
334: 188: Only completed games should affect match statistics.
335: 189: 
336: 190: ---
337: 191: 
338: 192: # Ratings
339: 193: 
340: 194: For MVP:
341: 195: 
342: 196: Players can rate other players after a completed match.
343: 197: 
344: 198: Rating:
345: 199: 
346: 200: 1-5
347: 201: 
348: 202: Calculate average player rating.
349: 203: 
350: 204: Do not implement complex skill-rating algorithms yet.
351: 205: 
352: 206: ---
353: 207: 
354: 208: # Backend
355: 209: 
356: 210: Use:
357: 211: 
358: 212: Java
359: 213: Spring Boot
360: 214: Spring Security
361: 215: JWT
362: 216: Spring Data JPA
363: 217: PostgreSQL
364: 218: Flyway
365: 219: Bean Validation
366: 220: 
367: 221: Use a modular monolith.
368: 222: 
369: 223: DO NOT use microservices.
370: 224: 
371: 225: Architecture:
372: 226: 
373: 227: controller
374: 228: service
375: 229: repository
376: 230: entity
377: 231: dto
378: 232: mapper
379: 233: security
380: 234: exception
381: 235: 
382: 236: Use DTOs rather than exposing JPA entities directly.
383: 237: 
384: 238: ---
385: 239: 
386: 240: # Frontend
387: 241: 
388: 242: Use:
389: 243: 
390: 244: Next.js
391: 245: TypeScript
392: 246: Tailwind CSS
393: 247: 
394: 248: Pages:
395: 249: 
396: 250: /login
397: 251: /register
398: 252: /games
399: 253: /games/[id]
400: 254: /create-game
401: 255: /my-games
402: 256: /profile
403: 257: /profile/[id]
404: 258: 
405: 259: The UI should be mobile-first because most users will use this on their phone.
406: 260: 
407: 261: ---
408: 262: 
409: 263: # Database
410: 264: 
411: 265: Use PostgreSQL.
412: 266: 
413: 267: Important relationships:
414: 268: 
415: 269: User 1 -> N Game
416: 270: User 1 -> N JoinRequest
417: 271: Game 1 -> N JoinRequest
418: 272: Game N -> N User through MatchParticipant
419: 273: Game 1 -> N PlayerRating
420: 274: User 1 -> N PlayerRating
421: 275: 
422: 276: Use UUIDs for primary keys.
423: 277: 
424: 278: Use database constraints wherever appropriate.
425: 279: 
426: 280: ---
427: 281: 
428: 282: # Important business rules
429: 283: 
430: 284: 1. Only the game owner can accept/reject requests.
431: 285: 
432: 286: 2. A user cannot join their own game through a join request.
433: 287: 
434: 288: 3. A player cannot join a FULL game.
435: 289: 
436: 290: 4. A player cannot have duplicate active requests.
437: 291: 
438: 292: 5. Accepting a player must atomically verify that a slot is still available.
439: 293: 
440: 294: 6. Prevent race conditions when two requests are accepted simultaneously.
441: 295: 
442: 296: 7. Only the game owner can mark attendance.
443: 297: 
444: 298: 8. Statistics should be derived from match participation records.
445: 299: 
446: 300: 9. Users cannot rate players for games they did not participate in.
447: 301: 
448: 302: 10. A player can submit only one rating per player per game.
449: 303: 
450: 304: 11. Completed/cancelled games cannot accept new players.
451: 305: 
452: 306: ---
453: 307: 
454: 308: # Development strategy
455: 309: 
456: 310: Build the system incrementally.
457: 311: 
458: 312: Phase 1:
459: 313: Project setup and database
460: 314: 
461: 315: Phase 2:
462: 316: Authentication
463: 317: 
464: 318: Phase 3:
465: 319: User profile
466: 320: 
467: 321: Phase 4:
468: 322: Game creation
469: 323: 
470: 324: Phase 5:
471: 325: Game discovery
472: 326: 
473: 327: Phase 6:
474: 328: Join requests
475: 329: 
476: 330: Phase 7:
477: 331: Game owner management
478: 332: 
479: 333: Phase 8:
480: 334: Match completion and attendance
481: 335: 
482: 336: Phase 9:
483: 337: Ratings and statistics
484: 338: 
485: 339: Phase 10:
486: 340: Frontend polish
487: 341: 
488: 342: After every phase:
489: 343: 
490: 344: - Run tests
491: 345: - Run the application
492: 346: - Verify APIs
493: 347: - Fix compilation errors
494: 348: - Commit changes to git
495: 349: 
496: 350: Do not implement future features unless explicitly requested.
497: 
498: (End of file - total 350 lines)
499: </content>
500: ```
501: 
502: **Tool: read**
503: 
504: **Input:**
505: ```json
506: {
507:   "filePath": "F:\\Game-connect"
508: }
509: ```
510: 
511: **Output:**
512: ```
513: <path>F:\Game-connect</path>
514: <type>directory</type>
515: <entries>
516: .git/
517: .gitignore
518: AGENTS.md
519: backend/
520: docker-compose.yml
521: frontend/
522: PRODUCT.MD
523: README.md
524: 
525: (8 entries)
526: </entries>
527: ```
528: 
529: ---
530: 
531: ## Assistant (Plan · Big Pickle · 93.7s)
532: 
533: _Thinking:_
534: 
535: Let me explore the backend structure to understand what was implemented in Phase 1.
536: 
537: **Tool: task**
538: 
539: **Input:**
540: ```json
541: {
542:   "description": "Explore backend codebase structure",
543:   "subagent_type": "explore",
544:   "prompt": "Very thorough exploration of the backend codebase at F:\\Game-connect\\backend.\n\nI need to understand EVERYTHING that was implemented in Phase 1. Please:\n\n1. Read the full directory structure under backend/src/\n2. Read EVERY Java source file - every entity, every DTO, every repository, every service, every controller, every configuration class, every migration file\n3. Read pom.xml for dependencies\n4. Read application.properties / application.yml\n5. Read docker-compose.yml at F:\\Game-connect\\docker-compose.yml\n6. Read any Flyway migration files\n7. Read any test files\n\nReturn the COMPLETE contents of every single file you find. Do not summarize - I need the full source code of every file."
545: }
546: ```
547: 
548: **Output:**
549: ```
550: <task id="ses_f7b231b87ffeKW4D368UNkZnW5" state="completed">
551: <task_result>
552: I have now read every single file in the backend codebase. Here is the complete report of everything implemented in Phase 1.
553: 
554: ---
555: 
556: # COMPLETE BACKEND CODEBASE REPORT -- Phase 1
557: 
558: ## 1. Directory Structure
559: 
560: ```
561: backend/
562: ├── .gitattributes
563: ├── .gitignore
564: ├── .gradle/                          (Gradle cache -- not source)
565: ├── bin/                              (Compiled output -- not source)
566: ├── build/                            (Build output -- not source)
567: ├── build.gradle
568: ├── gradle/
569: │   └── wrapper/
570: │       ├── gradle-wrapper.jar
571: │       └── gradle-wrapper.properties
572: ├── gradlew
573: ├── gradlew.bat
574: ├── HELP.md
575: ├── settings.gradle
576: └── src/
577:     ├── main/
578:     │   ├── java/
579:     │   │   └── com/gameconnect/
580:     │   │       ├── BackendApplication.java
581:     │   │       ├── common/
582:     │   │       │   └── exception/
583:     │   │       │       ├── ApiError.java
584:     │   │       │       ├── BusinessException.java
585:     │   │       │       └── GlobalExceptionHandler.java
586:     │   │       └── web/
587:     │   │           └── HealthController.java
588:     │   └── resources/
589:     │       ├── application.yml
590:     │       └── db/
591:     │           └── migration/
592:     │               └── V1__init.sql
593:     └── test/
594:         └── java/
595:             └── com/gameconnect/
596:                 ├── BackendApplicationTests.java
597:                 ├── FlywaySchemaTest.java
598:                 ├── TestBackendApplication.java
599:                 └── TestcontainersConfiguration.java
600: ```
601: 
602: **Notable: There are NO entity classes, NO DTO classes, NO repository interfaces, NO service classes, and NO domain-specific controllers yet.** Phase 1 established only the infrastructure skeleton.
603: 
604: ---
605: 
606: ## 2. Build System -- `build.gradle`
607: 
608: ```groovy
609: plugins {
610: 	id 'java'
611: 	id 'org.springframework.boot' version '4.1.1'
612: 	id 'io.spring.dependency-management' version '1.1.7'
613: }
614: 
615: group = 'com.gameconnect'
616: version = '0.0.1-SNAPSHOT'
617: description = 'Game Connect MVP'
618: 
619: java {
620: 	toolchain {
621: 		languageVersion = JavaLanguageVersion.of(21)
622: 	}
623: }
624: 
625: repositories {
626: 	mavenCentral()
627: }
628: 
629: dependencies {
630: 	implementation 'org.springframework.boot:spring-boot-starter-actuator'
631: 	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
632: 	implementation 'org.springframework.boot:spring-boot-starter-flyway'
633: 	implementation 'org.springframework.boot:spring-boot-starter-validation'
634: 	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
635: 	implementation 'org.flywaydb:flyway-database-postgresql'
636: 	runtimeOnly 'org.postgresql:postgresql'
637: 	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
638: 	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
639: 	testImplementation 'org.springframework.boot:spring-boot-starter-flyway-test'
640: 	testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
641: 	testImplementation 'org.springframework.boot:spring-boot-starter-validation-test'
642: 	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
643: 	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
644: 	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
645: 	testImplementation 'org.testcontainers:testcontainers-postgresql'
646: 	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
647: }
648: 
649: tasks.named('test') {
650: 	useJUnitPlatform()
651: }
652: ```
653: 
654: **Key observations:**
655: - Spring Boot **4.1.1** (cutting-edge)
656: - Java **21** toolchain
657: - Gradle **9.7.1** wrapper
658: - Runtime: **PostgreSQL** via `postgresql` driver
659: - Production starters: webmvc, data-jpa, flyway, validation, actuator
660: - Test starters: testcontainers with postgres, all module-specific test starters
661: - Spring Security starter is NOT in `implementation` (only `testImplementation` for security-test), confirming it is intentionally excluded in Phase 1
662: 
663: ---
664: 
665: ## 3. `settings.gradle`
666: 
667: ```groovy
668: rootProject.name = 'backend'
669: ```
670: 
671: Single-module project named `backend`.
672: 
673: ---
674: 
675: ## 4. `application.yml`
676: 
677: ```yaml
678: spring:
679:   application:
680:     name: backend
681:   datasource:
682:     url: ${DB_URL:jdbc:postgresql://localhost:5430/gameconnect}
683:     username: ${DB_USER:gameconnect}
684:     password: ${DB_PASSWORD:gameconnect}
685:   jpa:
686:     open-in-view: false
687:     hibernate:
688:       ddl-auto: validate
689:   flyway:
690:     enabled: true
691:     locations: classpath:db/migration
692: 
693: server:
694:   port: ${PORT:8080}
695: 
696: management:
697:   endpoints:
698:     web:
699:       exposure:
700:         include: health,info
701: ```
702: 
703: **Key observations:**
704: - Database: PostgreSQL on port **5430** (mapped from container port 5432)
705: - Database name/user/password: `gameconnect` / `gameconnect` / `gameconnect`
706: - JPA `ddl-auto` is `validate` -- schema managed entirely by Flyway, Hibernate only validates
707: - `open-in-view` is explicitly **false** (best practice)
708: - Flyway enabled, migrations at `classpath:db/migration`
709: - Server port: **8080**
710: - Actuator exposes only `health` and `info` endpoints
711: - All values are overridable via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`)
712: 
713: ---
714: 
715: ## 5. `docker-compose.yml`
716: 
717: ```yaml
718: services:
719:   db:
720:     image: postgres:16
721:     container_name: gameconnect-db
722:     environment:
723:       POSTGRES_DB: gameconnect
724:       POSTGRES_USER: gameconnect
725:       POSTGRES_PASSWORD: gameconnect
726:     ports:
727:       - "5430:5432"
728:     volumes:
729:       - gameconnect-pgdata:/var/lib/postgresql/data
730:     healthcheck:
731:       test: ["CMD-SHELL", "pg_isready -U gameconnect -d gameconnect"]
732:       interval: 5s
733:       timeout: 5s
734:       retries: 10
735: 
736: volumes:
737:   gameconnect-pgdata:
738: ```
739: 
740: **Key observations:**
741: - Single service: `db` (PostgreSQL 16)
742: - Host port **5430** mapped to container port **5432**
743: - Persistent volume `gameconnect-pgdata` for data durability
744: - Healthcheck using `pg_isready` with 5s interval, 5s timeout, 10 retries
745: - No application service container yet (only the database)
746: 
747: ---
748: 
749: ## 6. Flyway Migration -- `V1__init.sql`
750: 
751: ```sql
752: -- Game Connect MVP - initial schema
753: -- All primary keys are UUIDs. Statistics are derived from match_participant / player_rating
754: -- and are intentionally NOT stored as columns on app_user.
755: 
756: CREATE TABLE app_user (
757:     id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
758:     email             VARCHAR(255) NOT NULL,
759:     password_hash     VARCHAR(255) NOT NULL,
760:     display_name      VARCHAR(80)  NOT NULL,
761:     bio               VARCHAR(2000),
762:     profile_image_url VARCHAR(500),
763:     skill_level       VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
764:     created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
765: 
766:     CONSTRAINT uq_app_user_email UNIQUE (email),
767:     CONSTRAINT ck_app_user_skill CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
768: );
769: 
770: CREATE TABLE game (
771:     id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
772:     owner_id         UUID NOT NULL REFERENCES app_user (id),
773:     turf_name        VARCHAR(120) NOT NULL,
774:     turf_address     VARCHAR(255) NOT NULL,
775:     latitude         NUMERIC(9, 6) NOT NULL,
776:     longitude        NUMERIC(9, 6) NOT NULL,
777:     game_date        DATE         NOT NULL,
778:     start_time       TIMESTAMPTZ  NOT NULL,
779:     end_time         TIMESTAMPTZ  NOT NULL,
780:     format           VARCHAR(10)  NOT NULL,
781:     skill_level      VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
782:     maximum_players  INTEGER      NOT NULL,
783:     required_players INTEGER,
784:     joining_fee      INTEGER,
785:     description      TEXT,
786:     status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
787:     created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
788: 
789:     CONSTRAINT ck_game_status   CHECK (status IN ('OPEN', 'FULL', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
790:     CONSTRAINT ck_game_format   CHECK (format IN ('5V5', '6V6', '7V7', '8V8', '9V9', '11V11')),
791:     CONSTRAINT ck_game_skill    CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
792:     CONSTRAINT ck_game_capacity CHECK (maximum_players BETWEEN 5 AND 22),
793:     CONSTRAINT ck_game_window   CHECK (end_time > start_time),
794:     CONSTRAINT ck_req_players   CHECK (
795:         required_players IS NULL
796:         OR (required_players >= 1 AND required_players <= maximum_players)
797:     )
798: );
799: 
800: CREATE INDEX idx_game_status     ON game (status);
801: CREATE INDEX idx_game_date       ON game (game_date);
802: CREATE INDEX idx_game_owner      ON game (owner_id);
803: CREATE INDEX idx_game_skill_fmt  ON game (skill_level, format);
804: CREATE INDEX idx_game_window     ON game (start_time, end_time);
805: 
806: CREATE TABLE join_request (
807:     id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
808:     game_id    UUID NOT NULL REFERENCES game (id),
809:     user_id    UUID NOT NULL REFERENCES app_user (id),
810:     status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
811:     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
812:     decided_at TIMESTAMPTZ,
813: 
814:     CONSTRAINT ck_req_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
815:     -- A player may have at most one request per game, in any state.
816:     CONSTRAINT uq_req_game_user UNIQUE (game_id, user_id)
817: );
818: 
819: CREATE INDEX idx_join_request_game ON join_request (game_id, status);
820: CREATE INDEX idx_join_request_user ON join_request (user_id, status);
821: 
822: CREATE TABLE match_participant (
823:     id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
824:     game_id   UUID NOT NULL REFERENCES game (id),
825:     user_id   UUID NOT NULL REFERENCES app_user (id),
826:     role      VARCHAR(10) NOT NULL DEFAULT 'PLAYER',
827:     attended  BOOLEAN,
828:     joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
829: 
830:     CONSTRAINT ck_role          CHECK (role IN ('PLAYER', 'OWNER')),
831:     CONSTRAINT uq_participant   UNIQUE (game_id, user_id)
832: );
833: 
834: CREATE INDEX idx_participant_user ON match_participant (user_id);
835: 
836: CREATE TABLE player_rating (
837:     id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
838:     game_id    UUID NOT NULL REFERENCES game (id),
839:     rater_id   UUID NOT NULL REFERENCES app_user (id),
840:     ratee_id   UUID NOT NULL REFERENCES app_user (id),
841:     score      SMALLINT NOT NULL,
842:     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
843: 
844:     CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5),
845:     CONSTRAINT ck_rating_self  CHECK (rater_id <> ratee_id),
846:     -- At most one rating per rater per ratee per game.
847:     CONSTRAINT uq_rating UNIQUE (game_id, rater_id, ratee_id)
848: );
849: 
850: CREATE INDEX idx_player_rating_ratee ON player_rating (ratee_id);
851: ```
852: 
853: **Schema Summary -- 5 tables with rigorous constraints:**
854: 
855: | Table | Purpose | PK | FKs | Unique Constraints | Check Constraints | Indexes |
856: |---|---|---|---|---|---|---|
857: | `app_user` | User accounts | UUID | -- | `email` | `skill_level` enum | -- |
858: | `game` | Match/game listings | UUID | `owner_id -> app_user` | -- | `status`, `format`, `skill_level`, `capacity`, `time_window`, `required_players` | 5 indexes |
859: | `join_request` | Request to join a game | UUID | `game_id -> game`, `user_id -> app_user` | `(game_id, user_id)` | `status` enum | 2 indexes |
860: | `match_participant` | Confirmed participants | UUID | `game_id -> game`, `user_id -> app_user` | `(game_id, user_id)` | `role` enum | 1 index |
861: | `player_rating` | Peer ratings (1-5) | UUID | `game_id -> game`, `rater_id -> app_user`, `ratee_id -> app_user` | `(game_id, rater_id, ratee_id)` | `score` range, no self-rating | 1 index |
862: 
863: **Game statuses:** `OPEN`, `FULL`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
864: **Game formats:** `5V5`, `6V6`, `7V7`, `8V8`, `9V9`, `11V11`
865: **Skill levels:** `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
866: **Join request statuses:** `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`
867: **Participant roles:** `PLAYER`, `OWNER`
868: 
869: ---
870: 
871: ## 7. Main Application -- `BackendApplication.java`
872: 
873: ```java
874: package com.gameconnect;
875: 
876: import org.springframework.boot.SpringApplication;
877: import org.springframework.boot.autoconfigure.SpringBootApplication;
878: import java.util.TimeZone;
879: 
880: /**
881:  * Spring Security is intentionally disabled during Phase 1. A real security
882:  * configuration (JWT via HTTP-only cookie) is introduced in Phase 2.
883:  */
884: // // @SpringBootApplication(exclude = {
885: // // 		SecurityAutoConfiguration.class,
886: // // 		UserDetailsServiceAutoConfiguration.class
887: // // })
888: @SpringBootApplication
889: public class BackendApplication {
890: 
891: 	public static void main(String[] args) {
892: 		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
893: 		SpringApplication.run(BackendApplication.class, args);
894: 	}
895: 
896: }
897: ```
898: 
899: **Key observations:**
900: - Default timezone set to **Asia/Kolkata** (IST) at JVM level
901: - Spring Security exclusion is commented out -- the project simply does not include the security starter dependency in `implementation`
902: - Comment explicitly states JWT via HTTP-only cookie comes in Phase 2
903: 
904: ---
905: 
906: ## 8. Health Controller -- `HealthController.java`
907: 
908: ```java
909: package com.gameconnect.web;
910: 
911: import java.time.Instant;
912: import java.util.Map;
913: 
914: import org.springframework.web.bind.annotation.GetMapping;
915: import org.springframework.web.bind.annotation.RequestMapping;
916: import org.springframework.web.bind.annotation.RestController;
917: 
918: @RestController
919: @RequestMapping("/api")
920: public class HealthController {
921: 
922:     @GetMapping("/health")
923:     public Map<String, Object> health() {
924:         System.out.println("I am in the controller");
925:         return Map.of(
926:                 "status", "UP",
927:                 "service", "game-connect",
928:                 "timestamp", Instant.now()
929:         );
930:     }
931: }
932: ```
933: 
934: **Key observations:**
935: - Base path: `/api`
936: - Endpoint: `GET /api/health`
937: - Returns JSON: `{ "status": "UP", "service": "game-connect", "timestamp": "..." }`
938: - Contains a `System.out.println` debug statement (likely to be removed later)
939: 
940: ---
941: 
942: ## 9. Exception Handling Framework
943: 
944: ### `ApiError.java`
945: 
946: ```java
947: package com.gameconnect.common.exception;
948: 
949: import java.time.Instant;
950: 
951: public record ApiError(
952:         Instant timestamp,
953:         int status,
954:         String code,
955:         String message,
956:         String path
957: ) {
958: 
959:     public static ApiError of(int status, String code, String message, String path) {
960:         return new ApiError(Instant.now(), status, code, message, path);
961:     }
962: }
963: ```
964: 
965: A standard API error response record with `timestamp`, `status` (int), `code` (string enum-like), `message` (human-readable), and `path` (request URI). Factory method `of()` stamps the current instant.
966: 
967: ### `BusinessException.java`
968: 
969: ```java
970: package com.gameconnect.common.exception;
971: 
972: import org.springframework.http.HttpStatus;
973: 
974: /**
975:  * Domain-level business rule violation. Map to a specific HTTP status/code.
976:  */
977: public class BusinessException extends RuntimeException {
978: 
979:     private final HttpStatus status;
980:     private final String code;
981: 
982:     public BusinessException(HttpStatus status, String code, String message) {
983:         super(message);
984:         this.status = status;
985:         this.code = code;
986:     }
987: 
988:     public HttpStatus getStatus() {
989:         return status;
990:     }
991: 
992:     public String getCode() {
993:         return code;
994:     }
995: }
996: ```
997: 
998: A domain exception carrying an HTTP status and error code string. Services will throw this for business rule violations (e.g., "game is full", "user already requested to join").
999: 
1000: ### `GlobalExceptionHandler.java`
1001: 
1002: ```java
1003: package com.gameconnect.common.exception;
1004: 
1005: import java.util.LinkedHashMap;
1006: import java.util.Map;
1007: 
1008: import org.slf4j.Logger;
1009: import org.slf4j.LoggerFactory;
1010: import org.springframework.http.HttpStatus;
1011: import org.springframework.http.ResponseEntity;
1012: import org.springframework.http.converter.HttpMessageNotReadableException;
1013: import org.springframework.validation.FieldError;
1014: import org.springframework.web.bind.MethodArgumentNotValidException;
1015: import org.springframework.web.bind.annotation.ExceptionHandler;
1016: import org.springframework.web.bind.annotation.RestControllerAdvice;
1017: import org.springframework.web.servlet.resource.NoResourceFoundException;
1018: 
1019: import jakarta.servlet.http.HttpServletRequest;
1020: 
1021: @RestControllerAdvice
1022: public class GlobalExceptionHandler {
1023: 
1024:     private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
1025: 
1026:     @ExceptionHandler(BusinessException.class)
1027:     public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
1028:         return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
1029:     }
1030: 
1031:     @ExceptionHandler(MethodArgumentNotValidException.class)
1032:     public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
1033:                                                      HttpServletRequest request) {
1034:         Map<String, String> errors = new LinkedHashMap<>();
1035:         for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
1036:             errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
1037:         }
1038:         ApiError body = ApiError.of(
1039:                 HttpStatus.BAD_REQUEST.value(),
1040:                 "VALIDATION_ERROR",
1041:                 "Request validation failed: " + errors,
1042:                 request.getRequestURI()
1043:         );
1044:         return ResponseEntity.badRequest().body(body);
1045:     }
1046: 
1047:     @ExceptionHandler(HttpMessageNotReadableException.class)
1048:     public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
1049:                                                      HttpServletRequest request) {
1050:         return build(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Request body is malformed", request);
1051:     }
1052: 
1053:     @ExceptionHandler(NoResourceFoundException.class)
1054:     public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
1055:         return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request);
1056:     }
1057: 
1058:     @ExceptionHandler(Exception.class)
1059:     public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
1060:         log.error("Unhandled exception", ex);
1061:         return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
1062:     }
1063: 
1064:     private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
1065:                                            HttpServletRequest request) {
1066:         ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI());
1067:         return ResponseEntity.status(status).body(body);
1068:     }
1069: }
1070: ```
1071: 
1072: **Exception handling matrix:**
1073: 
1074: | Exception | HTTP Status | Code | Behavior |
1075: |---|---|---|---|
1076: | `BusinessException` | From exception | From exception | Forward status+code from exception |
1077: | `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | Collects all field errors into message |
1078: | `HttpMessageNotReadableException` | 400 | `MALFORMED_BODY` | Generic body parse failure |
1079: | `NoResourceFoundException` | 404 | `NOT_FOUND` | Spring MVC 404 catch-all |
1080: | `Exception` (catch-all) | 500 | `INTERNAL_ERROR` | Logs full stack trace, returns generic message |
1081: 
1082: ---
1083: 
1084: ## 10. Test Files
1085: 
1086: ### `BackendApplicationTests.java`
1087: 
1088: ```java
1089: package com.gameconnect;
1090: 
1091: import org.junit.jupiter.api.Test;
1092: import org.springframework.boot.test.context.SpringBootTest;
1093: import org.springframework.context.annotation.Import;
1094: 
1095: @Import(TestcontainersConfiguration.class)
1096: @SpringBootTest
1097: class BackendApplicationTests {
1098: 
1099: 	@Test
1100: 	void contextLoads() {
1101: 	}
1102: 
1103: }
1104: ```
1105: 
1106: Basic context-loads test. Imports the Testcontainers configuration so a real PostgreSQL container starts before the Spring context boots.
1107: 
1108: ### `TestcontainersConfiguration.java`
1109: 
1110: ```java
1111: package com.gameconnect;
1112: 
1113: import org.springframework.boot.test.context.TestConfiguration;
1114: import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
1115: import org.springframework.context.annotation.Bean;
1116: import org.testcontainers.postgresql.PostgreSQLContainer;
1117: import org.testcontainers.utility.DockerImageName;
1118: 
1119: @TestConfiguration(proxyBeanMethods = false)
1120: class TestcontainersConfiguration {
1121: 
1122: 	@Bean
1123: 	@ServiceConnection
1124: 	PostgreSQLContainer postgresContainer() {
1125: 		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
1126: 	}
1127: 
1128: }
1129: ```
1130: 
1131: Creates a `postgres:16` Testcontainer with `@ServiceConnection` so Spring Boot auto-configures the datasource URL/credentials from the container's dynamic mapping.
1132: 
1133: ### `TestBackendApplication.java`
1134: 
1135: ```java
1136: package com.gameconnect;
1137: 
1138: import org.springframework.boot.SpringApplication;
1139: 
1140: public class TestBackendApplication {
1141: 
1142: 	public static void main(String[] args) {
1143: 		SpringApplication.from(BackendApplication::main).with(TestcontainersConfiguration.class).run(args);
1144: 	}
1145: 
1146: }
1147: ```
1148: 
1149: A test-only main class for running the full app with Testcontainers (useful for local development without Docker Compose).
1150: 
1151: ### `FlywaySchemaTest.java`
1152: 
1153: ```java
1154: package com.gameconnect;
1155: 
1156: import static org.assertjj.api.Assertions.assertThat;
1157: 
1158: import java.util.List;
1159: 
1160: import org.flywaydb.core.Flyway;
1161: import org.junit.jupiter.api.Test;
1162: import org.springframework.beans.factory.annotation.Autowired;
1163: import org.springframework.boot.test.context.SpringBootTest;
1164: import org.springframework.context.annotation.Import;
1165: import org.springframework.jdbc.core.JdbcTemplate;
1166: 
1167: /**
1168:  * Verifies that Flyway applies the full V1 schema against a real PostgreSQL
1169:  * container, so later phases can rely on the migrations being correct.
1170:  */
1171: @Import(TestcontainersConfiguration.class)
1172: @SpringBootTest
1173: class FlywaySchemaTest {
1174: 
1175:     @Autowired
1176:     private Flyway flyway;
1177: 
1178:     @Autowired
1179:     private JdbcTemplate jdbcTemplate;
1180: 
1181:     @Test
1182:     void flywayAppliesInitialMigration() {
1183:         flyway.migrate();
1184:         var applied = flyway.info().applied().length;
1185:         assertThat(applied).as("at least the V1 migration is applied").isGreaterThanOrEqualTo(1);
1186:     }
1187: 
1188:     @Test
1189:     void coreTablesExist() {
1190:         var tables = jdbcTemplate.queryForList(
1191:                 "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
1192:                 String.class);
1193:         assertThat(tables).contains(
1194:                 "app_user", "game", "join_request", "match_participant", "player_rating");
1195:     }
1196: 
1197:     @Test
1198:     void uniqueAndCheckConstraintsArePresent() {
1199:         List<String> constraints = jdbcTemplate.queryForList(
1200:                 """
1201:                 SELECT tc.constraint_name
1202:                 FROM information_schema.table_constraints tc
1203:                 JOIN information_schema.constraint_column_usage ccu
1204:                   ON ccu.constraint_name = tc.constraint_name
1205:                 WHERE tc.table_schema = 'public'
1206:                 """,
1207:                 String.class);
1208:         assertThat(constraints).contains(
1209:                 "uq_app_user_email",
1210:                 "uq_req_game_user",
1211:                 "uq_participant",
1212:                 "uq_rating",
1213:                 "ck_game_status",
1214:                 "ck_rating_score",
1215:                 "ck_rating_self");
1216:     }
1217: }
1218: ```
1219: 
1220: Three integration tests against a real PostgreSQL container:
1221: 1. **flywayAppliesInitialMigration** -- asserts at least 1 migration was applied
1222: 2. **coreTablesExist** -- queries `information_schema.tables` and asserts all 5 tables exist
1223: 3. **uniqueAndCheckConstraintsArePresent** -- queries `information_schema.table_constraints` and asserts 7 specific named constraints exist
1224: 
1225: ---
1226: 
1227: ## 11. What Was Implemented in Phase 1 -- Summary
1228: 
1229: Phase 1 was an **infrastructure/skeleton phase**. Here is exactly what was built:
1230: 
1231: ### What EXISTS:
1232: | Layer | What | Files |
1233: |---|---|---|
1234: | **Build** | Gradle build with Spring Boot 4.1.1, Java 21, all required starters | `build.gradle`, `settings.gradle`, `gradle/wrapper/*` |
1235: | **Database** | PostgreSQL 16 via Docker Compose with health check and persistent volume | `docker-compose.yml` |
1236: | **Schema** | Complete Flyway migration with 5 tables, 15+ indexes, 12+ constraints | `V1__init.sql` |
1237: | **Configuration** | `application.yml` with env-var overridable datasource, Flyway, actuator | `application.yml` |
1238: | **Application entry** | Spring Boot main class with IST timezone default | `BackendApplication.java` |
1239: | **Health endpoint** | `GET /api/health` returning status/service/timestamp | `HealthController.java` |
1240: | **Error handling** | `ApiError` record, `BusinessException`, `GlobalExceptionHandler` with 5 handlers | 3 files in `common/exception/` |
1241: | **Tests** | Context load, Flyway schema verification (tables + constraints) with Testcontainers | 4 test files |
1242: | **Docker** | Compose with Postgres only (no app container yet) | `docker-compose.yml` |
1243: 
1244: ### What does NOT EXIST yet (intentionally deferred to later phases):
1245: - **No JPA Entity classes** -- the 5 tables have no corresponding `@Entity` POJOs
1246: - **No DTO/Request/Response records**
1247: - **No Repository interfaces** (no Spring Data JPA repositories)
1248: - **No Service classes** (no business logic)
1249: - **No domain controllers** (no GameController, UserController, etc.)
1250: - **No Security configuration** (explicitly noted as Phase 2 with JWT/HTTP-only cookie)
1251: - **No Lombok or MapStruct** dependencies
1252: - **No API documentation** (no Swagger/OpenAPI)
1253: - **No Dockerfile** for the application itself
1254: - **No CI/CD configuration**
1255: 
1256: Phase 1 delivered a runnable Spring Boot skeleton with a fully-defined database schema, a health check, a global error-handling framework, and integration tests proving the schema migrates correctly against real PostgreSQL.
1257: </task_result>
1258: </task>
1259: ```
1260: 
1261: **Tool: task**
1262: 
1263: **Input:**
1264: ```json
1265: {
1266:   "description": "Explore frontend codebase structure",
1267:   "subagent_type": "explore",
1268:   "prompt": "Quick exploration of the frontend codebase at F:\\Game-connect\\frontend.\n\n1. Read the full directory structure under frontend/\n2. Read package.json\n3. Read next.config.js/ts if it exists\n4. Read any configuration files\n5. Read a few key source files to understand the setup\n\nReturn the complete contents of configuration files and a summary of the frontend structure."
1269: }
1270: ```
1271: 
1272: **Output:**
1273: ```
1274: <task id="ses_f7b231440ffe357i0NnvaVMbvq" state="completed">
1275: <task_result>
1276: I have now explored the entire frontend codebase thoroughly. Here is my report.
1277: 
1278: ## Frontend Structure Summary
1279: 
1280: `F:\Game-connect\frontend` is a **Next.js 16 (App Router)** application built with **React 19**, **TypeScript**, and **Tailwind CSS v4**. It's a "Game Connect" app for **pickup football matchmaking** — currently at an early "Phase 1 shell" stage.
1281: 
1282: ### Complete directory structure
1283: 
1284: ```
1285: frontend/
1286: ├── .gitignore
1287: ├── AGENTS.md                    (contains auto-generated nextjs-agent-rules block)
1288: ├── CLAUDE.md                    (@AGENTS.md reference)
1289: ├── README.md                    (default create-next-app)
1290: ├── eslint.config.mjs
1291: ├── next-env.d.ts
1292: ├── next.config.ts
1293: ├── package.json
1294: ├── package-lock.json
1295: ├── postcss.config.mjs
1296: ├── tsconfig.json
1297: ├── app/
1298: │   ├── (app)/                   [route group — mobile app shell w/ bottom nav]
1299: │   │   ├── games/
1300: │   │   │   ├── [id]/page.tsx    (dynamic game detail route)
1301: │   │   │   └── page.tsx         (game list)
1302: │   │   └── layout.tsx
1303: │   ├── favicon.ico
1304: │   ├── globals.css
1305: │   ├── layout.tsx               (root layout)
1306: │   └── page.tsx                 (redirects "/" → "/games")
1307: ├── components/
1308: │   └── layout/
1309: │       └── BottomNav.tsx        ("use client" bottom navigation bar)
1310: ├── lib/
1311: │   ├── api/
1312: │   │   └── client.ts            (API fetch wrapper)
1313: │   └── types.ts                 (domain type definitions)
1314: ├── public/                      (default SVG assets)
1315: └── node_modules/                (next/ docs available at node_modules/next/dist/docs/)
1316: ```
1317: 
1318: ---
1319: 
1320: ### Configuration Files (complete contents)
1321: 
1322: **`package.json`** — name `gc-frontend`, v0.1.0
1323: - Scripts: `dev`, `build`, `start`, `lint` (eslint)
1324: - Dependencies: `next@16.3.4`, `react@19.2.8`, `react-dom@19.2.8`
1325: - DevDependencies: `@tailwindcss/postcss@^4`, `tailwindcss@^4`, `eslint@^9`, `eslint-config-next@16.3.4`, `typescript@^5`, `@types/node@^20`, `@types/react@^19`, `@types/react-dom@^19`
1326: 
1327: **`next.config.ts`** — essentially empty/stock config (only the `NextConfig` type import and empty object export).
1328: 
1329: **`tsconfig.json`** — strict TS, `target: ES2017`, `jsx: react-jsx`, `moduleResolution: bundler`, `paths: "@/ *" → "./*"`, includes `next-env.d.ts` + all `.ts/.tsx/.mts` (excludes `node_modules`).
1330: 
1331: **`postcss.config.mjs`** — plugin `@tailwindcss/postcss` (Tailwind v4 PostCSS setup).
1332: 
1333: **`eslint.config.mjs`** — uses `eslint-config-next/core-web-vitals` + `eslint-config-next/typescript`; `globalIgnores` for `.next/**`, `out/**`, `build/**`, `next-env.d.ts`.
1334: 
1335: **`next-env.d.ts`** — typed references (`next`, `next/image-types/global`) plus `.next/types` route/param imports.
1336: 
1337: **`.gitignore`** — ignores `node_modules`, `.next`, `out`, `build`, coverage, `.env*`, `*.tsbuildinfo`, `next-env.d.ts`, etc.
1338: 
1339: **`globals.css`** — imports `tailwindcss`, defines `--background`/`--foreground` CSS vars with dark-mode `prefers-color-scheme` media query, and `@theme inline` mapping to Geist font variables.
1340: 
1341: ---
1342: 
1343: ### Key Source Files
1344: 
1345: **`app/layout.tsx`** — Root layout. Loads `Geist` and `Geist_Mono` fonts via `next/font/google`, sets metadata title **"Game Connect"** / description **"Pickup football matchmaking"**, and configures `html`/`body` with `h-full antialiased` and `min-h-full flex flex-col` classes.
1346: 
1347: **`app/page.tsx`** — `Home` component that just calls `redirect("/games")`.
1348: 
1349: **`app/(app)/layout.tsx`** — Route-group layout. Wraps content in a mobile-centered column (`mx-auto max-w-md flex-col`), with a `<main className="flex-1 pb-20">` area plus `<BottomNav />`.
1350: 
1351: **`app/(app)/games/page.tsx`** — Games list page. Uses a `PLACEHOLDER_GAMES` empty array and shows a "No games available yet" message noting that "Game discovery arrives in a later phase; this is the Phase 1 shell." Has a "Create" link to `/create-game`.
1352: 
1353: **`app/(app)/games/[id]/page.tsx`** — Server-rendered dynamic route. Uses the typed `PageProps<"/games/[id]">`, awaits `params`, calls `notFound()` if `id` missing, and shows a placeholder detail view.
1354: 
1355: **`components/layout/BottomNav.tsx`** — Client component. Renders a fixed bottom nav with 4 links: **Games** (`/games`), **Create** (`/create-game`), **My Games** (`/my-games`), **Profile** (`/profile`). Active state highlighted in `text-emerald-600` using `usePathname()` (with special handling so `/games` matches any `/games/*` path).
1356: 
1357: **`lib/api/client.ts`** — API helper module:
1358: - `API_BASE_URL` = `process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"`
1359: - Exports `ApiError` class (with `status` and `code` fields)
1360: - Exports `apiFetch<T>()` generic wrapper handling JSON body, `credentials: "include"`, error parsing (`{code, message}`), and `204` handling.
1361: 
1362: **`lib/types.ts`** — Domain types: `SkillLevel`, `GameFormat`, `GameStatus`, `RequestStatus`, `ParticipantRole` enums, plus `UserProfile`, `PlayerStats`, `Game`/`GameSummary`, `GameParticipant`, `JoinRequest`, `PlayerRating` interfaces.
1363: 
1364: ---
1365: 
1366: ### Notable observations
1367: 
1368: - **Route structure**: The `(app)` route group represents the authenticated mobile app shell, while routes like `/create-game`, `/my-games`, and `/profile` are referenced in the nav but **do not yet have page files** — they're planned for later phases.
1369: - **Branching routes**: Several planned features (game discovery, game detail, create-game, my-games, profile, messaging) are not yet implemented; the current pages are placeholder shells.
1370: - **Backend integration**: The frontend is wired to expect a backend at `http://localhost:8080/api` (configurable via `NEXT_PUBLIC_API_BASE_URL`), and typed domain models exist that mirror a backend API contract.
1371: - **Stack**: Next.js 16.3 App Router + React 19 + TypeScript strict + Tailwind CSS v4 (`@tailwindcss/postcss`), ESLint 9 flat config.
1372: - **No `.env` files** are currently present in the repo (they're gitignored).
1373: </task_result>
1374: </task>
1375: ```
1376: 
1377: ---
1378: 
1379: ## Assistant (Plan · Big Pickle · 74.4s)
1380: 
1381: _Thinking:_
1382: 
1383: Now I have a thorough understanding of the entire codebase. Let me plan Phase 2: Authentication.
1384: 
1385: Key facts from Phase 1:
1386: - Spring Boot 4.1.1, Java 21, Gradle
1387: - PostgreSQL 16, Flyway migrations
1388: - No JPA entities, no repositories, no services, no security config yet
1389: - The `app_user` table already has `email`, `password_hash`, `display_name`, `bio`, `profile_image_url`, `skill_level`, `created_at`
1390: - `password_hash` column already exists in the schema - perfect for BCrypt
1391: - No Spring Security starter in `implementation` dependencies yet
1392: - Exception handling framework exists with `ApiError`, `BusinessException`, `GlobalExceptionHandler`
1393: - GlobalExceptionHandler already handles validation errors, business exceptions, etc.
1394: - No Lombok or MapStruct
1395: 
1396: For Phase 2, I need to:
1397: 
1398: 1. **Dependencies** - Add `spring-boot-starter-security`, `jjwt` (or `spring-security-oauth2-jose`), `bcrypt` (included with spring-security)
1399: 
1400: 2. **Entity** - Create `User` JPA entity mapped to `app_user` table
1401: 
1402: 3. **Repository** - Create `UserRepository` interface
1403: 
1404: 4. **DTOs** - Create request/response records for registration, login, auth response
1405: 
1406: 5. **Service** - Create `AuthService` for registration/login, `UserService` for user lookup
1407: 
1408: 6. **Controller** - Create `AuthController` with endpoints
1409: 
1410: 7. **Security Config** - Create `SecurityConfiguration` defining public/protected endpoints
1411: 
1412: 8. **JWT** - Create JWT utility/service for token generation/validation
1413: 
1414: 9. **JWT Filter** - Create authentication filter that extracts JWT from cookie and sets SecurityContext
1415: 
1416: 10. **Configuration** - JWT secret and expiration from env vars
1417: 
1418: 11. **Tests** - Integration tests for all auth flows
1419: 
1420: Let me think about the JWT library choice. For Spring Boot 4.1.1, the most common choices are:
1421: - `io.jsonwebtoken:jjwt` (JJWT) - most popular, mature
1422: - `com.auth0:java-jwt` - simpler API
1423: - Spring Security OAuth2 Resource Server (built-in JWT support)
1424: 
1425: Since the requirement is minimal MVP, I'll go with JJWT as it's the most standard choice for Spring Boot JWT implementations.
1426: 
1427: For HTTP-only cookies:
1428: - Set the JWT as an HTTP-only, Secure, SameSite cookie on login
1429: - The JWT filter reads from the cookie (not Authorization header)
1430: - This is more secure than localStorage tokens
1431: 
1432: Let me also think about what the auth flow looks like:
1433: 1. Register: POST /api/auth/register → creates user, returns JWT in cookie
1434: 2. Login: POST /api/auth/login → validates credentials, returns JWT in cookie
1435: 3. Me: GET /api/auth/me → reads JWT from cookie, returns current user profile
1436: 4. Logout: POST /api/auth/logout → clears the cookie
1437: 
1438: Now let me plan the exact files:
1439: 
1440: ### New files to create:
1441: 
1442: **Entities:**
1443: - `backend/src/main/java/com/gameconnect/auth/entity/User.java`
1444: 
1445: **DTOs:**
1446: - `backend/src/main/java/com/gameconnect/auth/dto/RegisterRequest.java`
1447: - `backend/src/main/java/com/gameconnect/auth/dto/LoginRequest.java`
1448: - `backend/src/main/java/com/gameconnect/auth/dto/AuthResponse.java`
1449: - `backend/src/main/java/com/gameconnect/auth/dto/UserResponse.java`
1450: 
1451: **Repositories:**
1452: - `backend/src/main/java/com/gameconnect/auth/repository/UserRepository.java`
1453: 
1454: **Services:**
1455: - `backend/src/main/java/com/gameconnect/auth/service/AuthService.java`
1456: - `backend/src/main/java/com/gameconnect/auth/service/UserDetailsServiceImpl.java`
1457: 
1458: **Controllers:**
1459: - `backend/src/main/java/com/gameconnect/auth/controller/AuthController.java`
1460: 
1461: **Security:**
1462: - `backend/src/main/java/com/gameconnect/security/SecurityConfiguration.java`
1463: - `backend/src/main/java/com/gameconnect/security/JwtService.java`
1464: - `backend/src/main/java/com/gameconnect/security/JwtAuthenticationFilter.java`
1465: 
1466: **Flyway Migration:**
1467: - No new migration needed - the `app_user` table with `password_hash` already exists
1468: 
1469: **Tests:**
1470: - `backend/src/test/java/com/gameconnect/auth/AuthIntegrationTest.java`
1471: 
1472: ### Files to modify:
1473: 
1474: **`build.gradle`** - Add `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
1475: 
1476: **`application.yml`** - Add JWT secret and expiration configuration
1477: 
1478: Let me think about whether I should organize under `auth/` or follow the PRODUCT.md architecture pattern (controller, service, repository, entity, dto, mapper, security, exception).
1479: 
1480: The PRODUCT.md says:
1481: ```
1482: controller
1483: service
1484: repository
1485: entity
1486: dto
1487: mapper
1488: security
1489: exception
1490: ```
1491: 
1492: This could be interpreted as either:
1493: 1. Flat packages: `controller`, `service`, `repository`, etc. (feature-agnostic)
1494: 2. Or feature-based packages with these sub-layers
1495: 
1496: Looking at the existing code, we have `com.gameconnect.common.exception` and `com.gameconnect.web` - so the existing pattern is domain-based packages. But the architecture mentions flat packages. Since this is a modular monolith, I think the most practical approach is to use a feature-based module `auth` containing its own controller/service/repository/entity/dto. This keeps things organized as the codebase grows.
1497: 
1498: Actually, let me reconsider. The PRODUCT.md architecture says:
1499: ```
1500: controller
1501: service
1502: repository
1503: entity
1504: dto
1505: mapper
1506: security
1507: exception
1508: ```
1509: 
1510: This looks like it wants flat packages. But for a modular monolith, feature-based packaging is more practical. The existing code has `common.exception` and `web` packages, which are cross-cutting concerns.
1511: 
1512: I'll organize it as:
1513: - `com.gameconnect.auth` - the auth module
1514:   - `entity` - User entity
1515:   - `dto` - request/response DTOs
1516:   - `repository` - UserRepository
1517:   - `service` - AuthService
1518:   - `controller` - AuthController
1519: - `com.gameconnect.security` - cross-cutting security
1520:   - SecurityConfiguration
1521:   - JwtService
1522:   - JwtAuthenticationFilterThis preserves the modular monolith structure while keeping things clean.
1523: 
1524: Now let me think about the detailed implementation plan:
1525: 
1526: ### Security Configuration
1527: - `/api/auth/**` is public (register, login)
1528: - `/api/health` is public
1529: - All other `/api/**` endpoints require authentication
1530: - CSRF disabled (API-only, cookie-based JWT)
1531: - Stateless session management
1532: - Custom JWT filter before `UsernamePasswordAuthenticationFilter`
1533: 
1534: ### JWT Strategy
1535: - Secret from `JWT_SECRET` env var
1536: - Expiration from `JWT_EXPIRATION_MINUTES` env var (default: 60 minutes)
1537: - Token contains: `sub` (user ID as UUID), `email`
1538: - Signed with HMAC-SHA256 (HS256)
1539: 
1540: ### Cookie Strategy
1541: - Cookie name: `auth_token`
1542: - HTTP-only: true
1543: - Secure: true (production) / false (development)
1544: - SameSite: Lax
1545: - Path: /
1546: - Max age: matches JWT expiration
1547: 
1548: ### AuthController Endpoints
1549: - `POST /api/auth/register` - Register a new user
1550:   - Body: `{ email, password, displayName }`
1551:   - Response: Sets JWT cookie, returns `{ id, email, displayName }`
1552:   - Errors: 409 for duplicate email, 400 for validation
1553:   
1554: - `POST /api/auth/login` - Login
1555:   - Body: `{ email, password }`
1556:   - Response: Sets JWT cookie, returns `{ id, email, displayName }`
1557:   - Errors: 401 for invalid credentials
1558: 
1559: - `GET /api/auth/me` - Get current user
1560:   - Requires: valid JWT cookie
1561:   - Response: `{ id, email, displayName, bio, profileImageUrl, skillLevel }`
1562:   - Errors: 401 if not authenticated
1563: 
1564: - `POST /api/auth/logout` - Logout
1565:   - Clears the JWT cookie
1566:   - Response: 204 No Content
1567: 
1568: ### Validation
1569: - Email: @Email, @NotBlank
1570: - Password: @NotBlank, @Size(min = 8)
1571: - DisplayName: @NotBlank, @Size(max = 80)
1572: 
1573: ### Error Handling Additions
1574: - `DUPLICATE_EMAIL` - 409 Conflict
1575: - `INVALID_CREDENTIALS` - 401 Unauthorized
1576: - Need to update `GlobalExceptionHandler` or add a new handlerWait, the `GlobalExceptionHandler` already handles `BusinessException`. I can just throw `BusinessException` from the service layer for these cases.
1577: 
1578: For Spring Security's authentication entry point (401 on unauthenticated access), I need to customize it to return the same `ApiError` format instead of the default Spring Security HTML/text response.
1579: 
1580: Also need to handle Spring Security's access denied (403) to return `ApiError` format.
1581: 
1582: Let me also think about whether there are any concerns with Spring Boot 4.1.1. This is a very new version. The Spring Security APIs might have some changes. But the core patterns should be the same.
1583: 
1584: Actually, I should also check - does the `app_user` table already have a `password_hash` column? Yes it does! From V1__init.sql:
1585: ```sql
1586: password_hash     VARCHAR(255) NOT NULL,
1587: ```
1588: 
1589: This was clearly designed for Phase 2 auth. Good.
1590: 
1591: Let me finalize the plan and present it clearly.
1592: 

(Output capped at 50 KB. Showing lines 1-1592. Use offset=1593 to continue.)
</content>
```

---

## Assistant (Plan · Big Pickle · 209.6s)

_Thinking:_

I now have a thorough understanding of the codebase. Let me summarize what I know:

**Phase 1 & 2 are complete.** The codebase has:

**Backend:**
- Spring Boot 4.1.1, Java 21, Gradle
- PostgreSQL 16 via Docker Compose (port 5430)
- Complete V1__init.sql with 5 tables: app_user, game, join_request, match_participant, player_rating
- JWT authentication with HTTP-only cookies
- SecurityConfiguration with public/protected endpoints
- JwtService, JwtAuthenticationFilter
- Auth module: User entity, UserRepository, AuthService, AuthController
- DTOs: RegisterRequest, LoginRequest, AuthResponse, UserResponse
- Common exception framework: ApiError, BusinessException, GlobalExceptionHandler
- HealthController
- AuthIntegrationTest, FlywaySchemaTest, BackendApplicationTests with Testcontainers

**Frontend:**
- Next.js 16.3.4, React 19.2.8, TypeScript, Tailwind CSS v4
- Login and register pages (fully functional with auth context)
- App layout with BottomNav
- Games page (placeholder)
- Game detail page (placeholder)
- API client with fetch wrapper
- Auth context with login/register/logout/fetchMe
- Types defined for all domain objects
- Middleware (proxy.ts) for route protection

**Database:**
- app_user: id, email, password_hash, display_name, bio, profile_image_url, skill_level, created_at
- game, join_request, match_participant, player_rating tables all definedNow I need to plan **Phase 3: User Profile** based on the PRODUCT.md.

From PRODUCT.md, Phase 3 is "User profile." The Player Profile section says:

A player has:
- id
- name
- profile image
- bio
- skill level
- matches played
- matches completed
- attendance rate
- average rating

The MVP player abilities related to profile:
- Create and edit profile
- Set skill level
- View their football statisticsLooking at the existing code:
- The `app_user` table already has: id, email, password_hash, display_name, bio, profile_image_url, skill_level, created_at
- The frontend `UserProfile` type already has: id, email, displayName, bio, profileImageUrl, skillLevel, createdAt
- The frontend `PlayerStats` type already has: matchesPlayed, matchesCompleted, attendanceRate, averageRating
- The `GET /api/auth/me` endpoint already returns profile data

So what does Phase 3 need to add?

1. **Edit profile** - ability to update display name, bio, profile image URL, skill level
2. **View any user's public profile** - GET /api/users/{id} for viewing other players
3. **Player statistics** - computed from match_participant and player_rating tables (matches played, matches completed, attendance rate, average rating)

The statistics are derived from:
- matchesPlayed: COUNT from match_participant where user_id = X
- matchesCompleted: COUNT from match_participant where user_id = X AND game.status = 'COMPLETED'
- attendanceRate: COUNT(match_participant where user_id = X AND attended = true AND game.status = 'COMPLETED') / COUNT(match_participant where user_id = X AND game.status = 'COMPLETED')
- averageRating: AVG(player_rating.score) where ratee_id = X

But wait - statistics depend on match_participant and game tables. The game entity and match_participant entity don't exist as JPA entities yet. Phase 3 needs to compute statistics, but the game/match_participant tables exist in the schema even if the JPA entities for them don't.

For statistics, I have two options:
1. Use native SQL queries in the repository
2. Create the Game and MatchParticipant entities now (minimal versions)

Since we should only introduce functionality belonging to Phase 3, and we don't want to introduce game creation (Phase 4) or match completion (Phase 8), I think the best approach is to use native SQL queries for statistics. This way we compute the stats from existing tables without needing full JPA entities for Game and MatchParticipant.

Actually, let me reconsider. We could create minimal JPA entities for Game and MatchParticipant just for the statistics queries, without creating any game-related services or controllers. But that feels premature.

I think the cleanest approach for Phase 3 is:
- Use a native query or a projection query for statistics
- The statistics will naturally be 0/empty when no games exist yet

Let me also think about what frontend pages are needed:
- `/profile` - view/edit own profile
- `/profile/[id]` - view another player's public profile

Both of these are referenced in PRODUCT.md pages and in the proxy.ts protected paths.

Let me think about the full plan:

### Backend Changes for Phase 3:

**New files:**
1. `com.gameconnect.profile.dto.UpdateProfileRequest.java` - DTO for profile updates
2. `com.gameconnect.profile.dto.PlayerProfileResponse.java` - Full public profile with stats
3. `com.gameconnect.profile.dto.PlayerStatsResponse.java` - Statistics response
4. `com.gameconnect.profile.service.ProfileService.java` - Business logic for profile operations
5. `com.gameconnect.profile.controller.ProfileController.java` - REST endpoints
6. `com.gameconnect.profile.repository.ProfileRepository.java` - Stats query repository**Modified files:**
1. `SecurityConfiguration.java` - No changes needed (already has general auth requirement for non-public endpoints)
2. `AuthController.java` - Minor: potentially move the /me logic to ProfileController, or keep it in AuthController

Wait, let me think about this more carefully. The existing `GET /api/auth/me` returns the current user's profile. Phase 3 adds:
- `PUT /api/profile` or `PATCH /api/profile` - update own profile
- `GET /api/users/{id}` or `GET /api/profile/{id}` - get any user's public profile with stats
- Statistics computation

Should I keep `/api/auth/me` as is and add new profile endpoints, or refactor? I think keeping `/api/auth/me` as-is is cleaner (it's a lightweight auth response), and adding profile-specific endpoints:
- `GET /api/users/me` - full profile with stats (or extend `/api/auth/me`)
- `PUT /api/users/me` - update profile
- `GET /api/users/{id}` - public profile of any user with stats

Actually, thinking about it more, the simplest and most conventional approach is:

- `GET /api/users/{id}` - public profile of any user (including stats)
- `PUT /api/users/me` - update own profile (or PATCH)
- Keep `/api/auth/me` as-is for lightweight auth response

Or maybe:
- `GET /api/users/{id}` - public profile with stats
- `PUT /api/users/me` - update own profile
- Keep `GET /api/auth/me` for the auth context

Let me settle on the API design:

**Endpoints:**
1. `GET /api/users/{id}` - Get any user's public profile (includes stats). Auth required.
2. `PUT /api/users/me` - Update own profile. Auth required. Body: UpdateProfileRequest
3. Keep `GET /api/auth/me` unchanged - lightweight current user info

This is clean and conventional. The auth/me is for auth context, users/{id} is for profile viewing.

Actually, let me also think about whether `GET /api/users/{id}` should include stats. Stats computation involves multiple tables and could be expensive. For the MVP, it's fine to compute inline. If performance becomes an issue, we can cache later.

For the frontend, Phase 3 needs:
1. `/profile` page - shows own profile with edit capability
2. `/profile/[id]` page - shows another user's public profile
3. API functions for profile operations
4. Update auth context if needed

Let me also think about profile image. The schema has `profile_image_url` as a VARCHAR(500). For MVP, we can accept a URL string (no file upload). Users can paste a URL to their profile image. This is the simplest approach and matches the schema.

Now let me formalize the full plan.

### Database Changes
None required. The `app_user` table already has all profile fields. Statistics are derived from existing `match_participant` and `player_rating` tables.

### Backend

**Module: profile** (new)

Files to create:
1. `src/main/java/com/gameconnect/profile/dto/UpdateProfileRequest.java`
2. `src/main/java/com/gameconnect/profile/dto/UserProfileResponse.java` (public profile with stats)
3. `src/main/java/com/gameconnect/profile/dto/PlayerStatsResponse.java`
4. `src/main/java/com/gameconnect/profile/service/ProfileService.java`
5. `src/main/java/com/gameconnect/profile/controller/ProfileController.java`
6. `src/main/java/com/gameconnect/profile/repository/PlayerStatsRepository.java`

Files to modify:
None strictly needed. The existing auth module stays as-is.

**API Design:**

```
GET    /api/users/{id} → 200 UserProfileResponse  (auth required)
PUT    /api/users/me       → 200 UserProfileResponse  (auth required)
GET    /api/auth/me        → unchanged (lightweight)
```

**UpdateProfileRequest:**
```java
public record UpdateProfileRequest(
    @Size(max = 80) String displayName,
    @Size(max = 2000) String bio,
    @Size(max = 500) String profileImageUrl,
    SkillLevel skillLevel
) {}
```

All fields optional for partial update (PATCH semantics via PUT, or use @Nullable).

**UserProfileResponse:**
```java
public record UserProfileResponse(
    UUID id,
    String displayName,
    String bio,
    String profileImageUrl,
    SkillLevel skillLevel,
    Instant createdAt,
    PlayerStatsResponse stats
) {}
```

**PlayerStatsResponse:**
```java
public record PlayerStatsResponse(
    int matchesPlayed,
    int matchesCompleted,
    Double attendanceRate,
    Double averageRating
) {}
```

**PlayerStatsRepository:**
Uses native SQL to compute stats from match_participant and player_rating tables without needing JPA entities for those tables.

```sql
-- matchesPlayed: COUNT of distinct games in match_participant
-- matchesCompleted: COUNT where game is COMPLETED
-- attendanceRate: COUNT(attended=true AND COMPLETED) / COUNT(COMPLETED)
SELECT    COUNT(DISTINCT mp.game_id) as matches_played,
    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) as matches_completed,
    CASE 
        WHEN COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) = 0 THEN NULL
        ELSE CAST(COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = true THEN mp.game_id END) AS DOUBLE PRECISION) 
             / COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)
    END as attendance_rate
FROM match_participant mp
JOIN game g ON g.id = mp.game_id
WHERE mp.user_id = ?

-- averageRating: 
SELECT AVG(score) FROM player_rating WHERE ratee_id = ?
```

Actually, combining these into a single native query might be tricky. Let me think...

I could do it in a single query with subqueries, or just run two separate queries. For simplicity and clarity, two separate queries is fine for MVP.

Or, even simpler: use two separate @Query methods in the repository:
1. `@Query(value = "SELECT ... FROM match_participant mp JOIN game g ... WHERE mp.user_id = :userId", nativeQuery = true)` for participation stats
2. `@Query(value = "SELECT AVG(pr.score) FROM player_rating pr WHERE pr.ratee_id = :userId", nativeQuery = true)` for rating

And in the service, combine them into a PlayerStatsResponse.

Wait, actually for the participation stats, since we have just3 numbers, we could do it in a single native query:

```sql
SELECT 
    COUNT(DISTINCT mp.game_id) as matches_played,
    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) as matches_completed,
    CASE 
        WHEN COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) = 0 THEN NULL
        ELSE CAST(COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = true THEN mp.game_id END) AS DOUBLE PRECISION) 
             / COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)
    END as attendance_rate
FROM match_participant mp
JOIN game g ON g.id = mp.game_id
WHERE mp.user_id = :userId
```

For a user with no match_participant records, this returns 0, 0, NULL (because no rows → COUNT = 0).

Hmm, actually with no rows, COUNT returns 0 and the CASE returns NULL. Let me verify:
- If there are no rows for the user, the query returns one row with all zeros and NULL.- `COUNT(DISTINCT mp.game_id)` = 0
- `COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)` = 0
- `CASE WHEN 0 = 0 THEN NULL ELSE ...` = NULLSo for a new user: matchesPlayed = 0, matchesCompleted = 0, attendanceRate = null, averageRating = null. That's correct.

Let me also think about whether I need a `Game` JPA entity for the join. Since I'm using native SQL, no.

### Frontend

**New files:**
1. `app/(app)/profile/page.tsx` - Own profile page with edit form
2. `app/(app)/profile/[id]/page.tsx` - Other user's profile page (read-only)
3. `lib/api/users.ts` - API functions for user profile endpoints

**Modified files:**
1. `lib/auth-context.tsx` - Potentially update to use full MeUser after profile update (refresh user data)

**Pages:**

`/profile` (own profile):
- Fetches GET /api/users/me (or uses auth context)
- Shows profile info: display name, bio, profile image, skill level, stats
- Edit button that toggles edit mode
- Edit form: display name, bio, profile image URL, skill level dropdown
- Saves via PUT /api/users/me
- Mobile-first layout

`/profile/[id]` (other user):
- Fetches GET /api/users/{id}
- Shows profile info (read-only)
- Shows stats: matches played, completed, attendance rate, average rating

Let me now think about validation rules:
- displayName: required on update? No, it should be optional in the update. If provided, max 80 chars.
- bio: optional, max 2000 chars
- profileImageUrl: optional, max 500 chars, must be a valid URL format
- skillLevel: optional, must be BEGINNER/INTERMEDIATE/ADVANCED if provided

Actually, let me reconsider: should displayName be required? If the user is updating their profile, they should be able to change their name. But it shouldn't be empty. So @NotBlank if provided, but nullable (for partial updates).

Hmm, for simplicity with PUT (full replacement semantics), maybe all fields should be optional. The frontend sends only the fields that changed. The backend merges.

Let me think about this differently. For a PUT to update a profile, the cleanest approach for MVP is to make all fields nullable/optional in the request body. The service layer applies only non-null fields:

```java
public record UpdateProfileRequest(
    @Nullable @Size(max = 80, message = "Display name must be at most 80 characters") String displayName,
    @Nullable @Size(max = 2000, message = "Bio must be at most 2000 characters") String bio,
    @Nullable @Size(max = 500, message = "Profile image URL must be at most 500 characters") String profileImageUrl,
    @Nullable SkillLevel skillLevel
) {}
```

If the user submits an empty display name, we should validate: if displayName is not null but blank, reject.

Actually, let me add @NotBlank when the value is present. With Jakarta Validation, we can use `@NotBlank` but that doesn't play well with nullable optional fields. The standard approach is to validate only when not null:

Option 1: Use `@Validated` with group validation
Option 2: Validate in the service layer manually
Option 3: Make all fields @Nullable and validate non-blank in service

I think the cleanest for MVP is: validate @Size constraints (which only fire if not null) in the DTO, and validate that displayName is not blank in the service if provided.

Or even simpler: just use a combination of @Size and custom validation. Let me keep it simple:

```java
public record UpdateProfileRequest(
    @Size(max = 80, message = "Display name must be at most 80 characters") String displayName,
    @Size(max = 2000, message = "Bio must be at most 2000 characters") String bio,
    @Size(max = 500, message = "Profile image URL must be at most 500 characters") String profileImageUrl,
    SkillLevel skillLevel
) {}
```

And in the service:
- If displayName is not null and blank → throw BusinessException
- Apply non-null fields to the user entity

### Authorization Rules for Phase 3:

1. Any authenticated user can view any user's public profile
2. Any authenticated user can update only their own profile
3. Profile statistics are public to authenticated users

### Tests:

Backend:
1. `ProfileIntegrationTest.java`:
   - GET /api/users/{id} - returns profile with stats
   - GET /api/users/{id} - returns 404 for non-existent user
   - GET /api/users/{id} - returns 401 without auth
   - PUT /api/users/me - updates display name
   - PUT /api/users/me - updates bio
   - PUT /api/users/me - updates skill level
   - PUT /api/users/me - updates profile image URL
   - PUT /api/users/me - validates display name length
   - PUT /api/users/me - validates bio length
   - PUT /api/users/me - returns 401 without auth
   - PUT /api/users/me - validates skill level enum
   - Profile stats are zero for new user
   - Profile stats reflect actual data (create test data)

### Files to create/modify summary:

**Backend - New:**
1. `src/main/java/com/gameconnect/profile/dto/UpdateProfileRequest.java`
2. `src/main/java/com/gameconnect/profile/dto/UserProfileResponse.java`
3. `src/main/java/com/gameconnect/profile/dto/PlayerStatsResponse.java`
4. `src/main/java/com/gameconnect/profile/service/ProfileService.java`
5. `src/main/java/com/gameconnect/profile/controller/ProfileController.java`
6. `src/main/java/com/gameconnect/profile/repository/PlayerStatsRepository.java`
7. `src/test/java/com/gameconnect/profile/ProfileIntegrationTest.java`

**Backend - Modified:**
None. The existing auth module stays as-is.

Wait, actually I should double-check: does SecurityConfiguration need any updates? The current config has:
- Public: POST /api/auth/register, POST /api/auth/login, GET /api/health, /api/actuator/**
- Everything else requires authentication

The new endpoints (GET /api/users/{id}, PUT /api/users/me) will be automatically protected by the `anyRequest().authenticated()` rule. No changes needed.

**Frontend - New:**
1. `app/(app)/profile/page.tsx` - Own profile with edit
2. `app/(app)/profile/[id]/page.tsx` - Public profile view
3. `lib/api/users.ts` - User profile API functions

**Frontend - Modified:**
1. `lib/auth-context.tsx` - Add method to refresh user data after profile update
2. Possibly `lib/api/auth.ts` - May need to update MeUser type

Actually, looking at `lib/api/auth.ts`, the `MeUser` interface already includes `bio`, `profileImageUrl`, `skillLevel`, `createdAt`. And the profile API returns these plus stats. So I'll create a separate `lib/api/users.ts` for the new endpoints.

Let me also think about whether the auth context needs updating. Currently, after login/register, the context stores the `AuthUser` (id, email, displayName). After a profile update on the `/profile` page, the display name in the auth context should be updated too. But the auth context only has `AuthUser` (without bio/skillLevel etc.), and the profile page would independently refetch data. So the auth context stays as-is, and the profile page manages its own state.

Actually, if the user changes their display name, the BottomNav and other parts of the UI that use auth context should reflect that. Let me check... the BottomNav doesn't display the user's name. The auth context is used for login/logout state. So it's fine to leave it as-is for MVP.

But wait, if the user updates their displayName, and then navigates to other pages that might use `useAuth().user.displayName`, it would show the stale name. For MVP, this is acceptable. The user can refresh the page. In a later phase, we could add optimistic updates or context refresh.

OK, I think I have a comprehensive plan. Let me write it up.

One more thought: for the frontend profile pages, I need to make sure the routes are registered in proxy.ts. Looking at the existing proxy.ts:

```ts
const PROTECTED_PATHS = ["/games", "/create-game", "/my-games", "/profile"];
```

`/profile` is already listed as a protected path. 

And `/profile/[id]` would match `/profile/` prefix, so it's covered too.

Let me finalize the plan now.

Actually, let me also consider: should I create a `/api/users/me` endpoint or just reuse the existing `GET /api/auth/me`? 

The current `/api/auth/me` returns:
```json
{
  "id": "...",
  "email": "...",
  "displayName": "...",
  "bio": "...",
  "profileImageUrl": "...",
  "skillLevel": "BEGINNER"
}
```

This is already a complete profile response (minus stats). For the own profile page, I could:
1. Use GET /api/auth/me for basic profile data (already exists)
2. Add a separate GET /api/users/me that includes stats

Or:
1. Add GET /api/users/{id} that includes stats
2. Use GET /api/users/{id} with own ID for the profile page

The second approach is cleaner because it's a single endpoint for both viewing own and others' profiles. Let me go with that.

So the frontend would:
- `/profile` page: call GET /api/users/{ownId} (get ownId from auth context)
- `/profile/[id]` page: call GET /api/users/{id}

Both use the same endpoint. Clean.

Actually, there's an even simpler approach for the own profile: `GET /api/users/me` as a convenience alias for `GET /api/users/{ownId}`. This is a common pattern.

Let me add that as well:
- `GET /api/users/me` → same as GET /api/users/{ownId} (convenience)
- `GET /api/users/{id}` → public profile with stats

This way the frontend doesn't need to know the current user's ID to fetch the full profile.

Final plan coming together. Let me write it up now.

I now have a thorough understanding of the entire codebase. Here is the Phase 3 implementation plan.

---

# Phase 3 Implementation Plan: User Profile

## Scope

Phase 3 delivers **player profile viewing, editing, and statistics**. No game creation, join requests, or other future features.

Per PRODUCT.md, a player profile includes: id, display name, profile image, bio, skill level, matches played, matches completed, attendance rate, average rating.

Statistics are derived from `match_participant` and `player_rating` tables (already exist in V1 schema) via native SQL — no new JPA entities for Game/MatchParticipant needed.

---

## 1. Database Changes

**None.** The `app_user` table already has all profile columns (`display_name`, `bio`, `profile_image_url`, `skill_level`, `created_at`). Statistics are computed on-the-fly from existing `match_participant`, `game`, and `player_rating` tables.

---

## 2. API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/users/me` | Required | Current user's full profile with stats |
| `GET` | `/api/users/{id}` | Required | Any user's public profile with stats |
| `PUT` | `/api/users/me` | Required | Update own profile |

### Request/Response Bodies

**`GET /api/users/{id}` and `GET /api/users/me`** → `200`
```json
{
  "id": "uuid",
  "displayName": "string",
  "bio": "string|null",
  "profileImageUrl": "string|null",
  "skillLevel": "BEGINNER|INTERMEDIATE|ADVANCED",
  "createdAt": "2026-09-10T...",
  "stats": {
    "matchesPlayed": 0,
    "matchesCompleted": 0,
    "attendanceRate": null,
    "averageRating": null
  }
}
```

**`PUT /api/users/me`** → `200` (same response as GET)
```json
{
  "displayName": "New Name",       // optional, max 80 chars, non-blank if provided
  "bio": "About me...",            // optional, max 2000 chars
  "profileImageUrl": "https://...", // optional, max 500 chars
  "skillLevel": "INTERMEDIATE"     // optional, enum
}
```

### Error Responses

| Scenario | Status | Code |
|----------|--------|------|
| Unauthenticated | 401 | `UNAUTHORIZED` |
| User not found | 404 | `USER_NOT_FOUND` |
| Blank displayName submitted | 400 | `VALIDATION_ERROR` |
| Display name too long | 400 | `VALIDATION_ERROR` |
| Invalid skill level | 400 | `VALIDATION_ERROR` |

---

## 3. DTOs

**`UpdateProfileRequest`** — `com.gameconnect.profile.dto`
```java
public record UpdateProfileRequest(
    @Size(max = 80, message = "Display name must be at most 80 characters")
    String displayName,

    @Size(max = 2000, message = "Bio must be at most 2000 characters")
    String bio,

    @Size(max = 500, message = "Profile image URL must be at most 500 characters")
    String profileImageUrl,

    SkillLevel skillLevel
) {}
```

**`UserProfileResponse`** — `com.gameconnect.profile.dto`
```java
public record UserProfileResponse(
    UUID id,
    String displayName,
    String bio,
    String profileImageUrl,
    User.SkillLevel skillLevel,
    Instant createdAt,
    PlayerStatsResponse stats
) {}
```

**`PlayerStatsResponse`** — `com.gameconnect.profile.dto`
```java
public record PlayerStatsResponse(
    int matchesPlayed,
    int matchesCompleted,
    Double attendanceRate,
    Double averageRating
) {}
```

---

## 4. Backend Files

### Files to Create

| File | Purpose |
|------|---------|
| `src/main/java/com/gameconnect/profile/dto/UpdateProfileRequest.java` | PUT request body |
| `src/main/java/com/gameconnect/profile/dto/UserProfileResponse.java` | Full profile response |
| `src/main/java/com/gameconnect/profile/dto/PlayerStatsResponse.java` | Stats sub-response |
| `src/main/java/com/gameconnect/profile/controller/ProfileController.java` | REST endpoints |
| `src/main/java/com/gameconnect/profile/service/ProfileService.java` | Business logic |
| `src/main/java/com/gameconnect/profile/repository/PlayerStatsRepository.java` | Native SQL for stats |
| `src/test/java/com/gameconnect/profile/ProfileIntegrationTest.java` | Integration tests |

### Files to Modify

**None.** The existing auth module (`AuthController`, `AuthService`, `SecurityConfiguration`) remains unchanged. The new endpoints are automatically protected by `anyRequest().authenticated()`.

---

## 5. Key Implementation Details

### ProfileController

```java
@RestController
@RequestMapping("/api/users")
public class ProfileController {

    @GetMapping("/me")
    public UserProfileResponse getOwnProfile(@AuthenticationPrincipal AuthenticatedUser principal)

    @GetMapping("/{id}")
    public UserProfileResponse getUserProfile(@PathVariable UUID id, @AuthenticationPrincipal ...)

    @PutMapping("/me")
    public UserProfileResponse updateOwnProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser principal)
}
```

### ProfileService

- `getProfile(UUID userId)` — fetches User from UserRepository, computes stats via PlayerStatsRepository, returns UserProfileResponse
- `updateProfile(UUID userId, UpdateProfileRequest request)` — applies non-null fields, validates displayName not blank, saves, returns updated profile

### PlayerStatsRepository (native SQL)

**Query 1 — Participation stats:**
```sql
SELECT
    COUNT(DISTINCT mp.game_id) AS matches_played,
    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) AS matches_completed,
    CASE
        WHEN COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) = 0 THEN NULL
        ELSE CAST(COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = TRUE THEN mp.game_id END) AS DOUBLE PRECISION)
             / COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)
    END AS attendance_rate
FROM match_participant mp
JOIN game g ON g.id = mp.game_id
WHERE mp.user_id = :userId
```

**Query 2 — Average rating:**
```sql
SELECT AVG(pr.score) FROM player_rating pr WHERE pr.ratee_id = :userId
```

For a new user with no participation: `matchesPlayed = 0`, `matchesCompleted = 0`, `attendanceRate = null`, `averageRating = null`.

### Validation Rules

| Field | Rule |
|-------|------|
| `displayName` | If provided: non-blank, max 80 chars |
| `bio` | If provided: max 2000 chars |
| `profileImageUrl` | If provided: max 500 chars |
| `skillLevel` | If provided: must be `BEGINNER`, `INTERMEDIATE`, or `ADVANCED` |
| At least one field | Request body must contain at least one non-null field |

---

## 6. Authorization Rules

1. Any authenticated user can view any user's public profile (`GET /api/users/{id}`)
2. Users can only update their own profile (`PUT /api/users/me`)
3. No role-based restrictions beyond authentication

---

## 7. Frontend

### Files to Create

| File | Purpose |
|------|---------|
| `app/(app)/profile/page.tsx` | Own profile page with edit capability |
| `app/(app)/profile/[id]/page.tsx` | Public profile page (read-only) |
| `lib/api/users.ts` | API functions for user profile endpoints |

### Files to Modify

**None strictly required.** The auth context, types, proxy config, and BottomNav already support the `/profile` route.

### `/profile` Page (Own Profile)

- Client component (`"use client"`)
- Fetches own profile from `GET /api/users/me` on mount
- Displays: profile image, display name, bio, skill level, stats card
- "Edit Profile" button toggles edit mode
- Edit mode shows form: display name input, bio textarea, profile image URL input, skill level dropdown
- "Save" button calls `PUT /api/users/me`, then refetches profile
- "Cancel" button discards changes
- Mobile-first layout (max-w-md, consistent with app shell)

### `/profile/[id]` Page (Public Profile)

- Client component (`"use client"`)
- Fetches user profile from `GET /api/users/{id}` on mount
- Displays: profile image, display name, bio, skill level, stats card
- Read-only — no edit button
- Shows "404" or redirect if user not found

### `lib/api/users.ts`

```typescript
export async function getOwnProfile(): Promise<UserProfile> { ... }
export async function getUserProfile(id: string): Promise<UserProfile> { ... }
export async function updateOwnProfile(data: UpdateProfileRequest): Promise<UserProfile> { ... }
```

### Types to Add (in `lib/types.ts`)

```typescript
export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
}

export interface UserProfile {
  id: string;
  displayName: string;
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel;
  createdAt: string;
  stats: PlayerStats;
}
```

Note: `PlayerStats` already exists in `lib/types.ts`. The `UserProfile` type already exists but needs `stats` added.

---

## 8. Tests

### `ProfileIntegrationTest.java`

| Test | Description |
|------|-------------|
| `getOwnProfile_returns200WithStats` | Authenticated user fetches own profile, verify all fields including zero stats |
| `getOwnProfile_withoutAuth_returns401` | Unauthenticated request returns 401 |
| `getUserProfile_existingUser_returns200` | Fetch another user's profile by ID |
| `getUserProfile_nonExistentUser_returns404` | Fetch non-existent UUID returns 404 USER_NOT_FOUND |
| `updateProfile_validDisplayName_returns200` | Update display name, verify change persists |
| `updateProfile_validBio_returns200` | Update bio |
| `updateProfile_validSkillLevel_returns200` | Update skill level |
| `updateProfile_validProfileImageUrl_returns200` | Update profile image URL |
| `updateProfile_blankDisplayName_returns400` | Submit blank displayName returns 400 |
| `updateProfile_longDisplayName_returns400` | Display name > 80 chars returns 400 |
| `updateProfile_longBio_returns400` | Bio > 2000 chars returns 400 |
| `updateProfile_invalidSkillLevel_returns400` | Invalid enum value returns 400 |
| `updateProfile_withoutAuth_returns401` | Unauthenticated update returns 401 |
| `updateProfile_partialUpdate_preservesOtherFields` | Update only bio, verify displayName unchanged |

---

## 9. Acceptance Criteria

1. Authenticated user can view their own profile at `/profile` with display name, bio, image, skill level, and zero-initialized stats
2. Authenticated user can edit their profile (display name, bio, image URL, skill level) and see changes persist
3. Authenticated user can view any other user's profile at `/profile/[id]` (read-only)
4. Profile statistics (matches played, completed, attendance rate, average rating) are computed correctly from game participation and rating data
5. Validation errors return consistent `ApiError` format with `VALIDATION_ERROR` code
6. Non-existent user returns 404 with `USER_NOT_FOUND` code
7. All endpoints require authentication (401 without JWT cookie)
8. Frontend builds without errors (`npm run build`)
9. All backend tests pass
10. No game creation, join requests, ratings, or other future-phase features introduced

---

## 10. Execution Order

1. Create DTOs (`UpdateProfileRequest`, `PlayerStatsResponse`, `UserProfileResponse`)
2. Create `PlayerStatsRepository` with native queries
3. Create `ProfileService`
4. Create `ProfileController`
5. Write `ProfileIntegrationTest`
6. Run backend tests and verify
7. Update `lib/types.ts` (add stats to UserProfile)
8. Create `lib/api/users.ts`
9. Create `app/(app)/profile/page.tsx`
10. Create `app/(app)/profile/[id]/page.tsx`
11. Run `npm run build` and verify
12. Run full backend + frontend test suites
13. Commit

---

