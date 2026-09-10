# Planning Phase 2 Authentication

**Session ID:** ses_f7b239a03ffervlUyYo7IcLmHD
**Created:** 9/9/2026, 12:00:31 PM
**Updated:** 9/9/2026, 12:03:43 PM

---

## User

Phase 1 is complete and verified.

Now plan Phase 2: Authentication only.

Before planning:

* Inspect the current repository.
* Read AGENTS.md and PRODUCT.md.
* Inspect what was actually implemented in Phase 1.
* Do not redesign the existing architecture.
* Preserve the modular monolith structure.

Phase 2 should implement:

1. User authentication

   * User registration
   * User login
   * Password hashing using BCrypt
   * JWT-based authentication
   * Secure HTTP-only cookie-based authentication
   * GET /api/auth/me

2. Spring Security

   * Security configuration
   * Public vs authenticated endpoints
   * JWT authentication filter/mechanism
   * Current authenticated user resolved from the JWT
   * Never trust a client-provided userId for the current user

3. Validation and errors

   * Duplicate email handling
   * Invalid credentials handling
   * Request validation
   * Consistent API error responses

4. Configuration

   * JWT secret and expiration from environment variables
   * No secrets hardcoded in source code

5. Testing

   * Registration success/failure
   * Duplicate email
   * Login success/failure
   * Protected endpoint without authentication
   * Protected endpoint with valid JWT
   * Invalid/expired JWT
   * /api/auth/me

Keep the implementation minimal and MVP-focused.

Do NOT implement:

* Game functionality
* Join requests
* Ratings
* Notifications
* Payments
* Chat
* Refresh tokens unless the existing architecture specifically requires them
* OAuth/social login
* Redis
* Additional infrastructure

First give me the revised Phase 2 implementation plan and the files/classes that will be created or modified.

Do not write code yet. 

---

## Assistant (Plan · Big Pickle · 23.8s)

_Thinking:_

I'll start by exploring the repository structure to understand what was implemented in Phase 1. First, let me check the current directory and read AGENTS.md and PRODUCT.md.

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
AGENTS.md
backend/
docker-compose.yml
frontend/
PRODUCT.MD
README.md

(8 entries)
</entries>
```

---

## Assistant (Plan · Big Pickle · 93.7s)

_Thinking:_

Let me explore the backend structure to understand what was implemented in Phase 1.

**Tool: task**

**Input:**
```json
{
  "description": "Explore backend codebase structure",
  "subagent_type": "explore",
  "prompt": "Very thorough exploration of the backend codebase at F:\\Game-connect\\backend.\n\nI need to understand EVERYTHING that was implemented in Phase 1. Please:\n\n1. Read the full directory structure under backend/src/\n2. Read EVERY Java source file - every entity, every DTO, every repository, every service, every controller, every configuration class, every migration file\n3. Read pom.xml for dependencies\n4. Read application.properties / application.yml\n5. Read docker-compose.yml at F:\\Game-connect\\docker-compose.yml\n6. Read any Flyway migration files\n7. Read any test files\n\nReturn the COMPLETE contents of every single file you find. Do not summarize - I need the full source code of every file."
}
```

**Output:**
```
<task id="ses_f7b231b87ffeKW4D368UNkZnW5" state="completed">
<task_result>
I have now read every single file in the backend codebase. Here is the complete report of everything implemented in Phase 1.

---

# COMPLETE BACKEND CODEBASE REPORT -- Phase 1

## 1. Directory Structure

```
backend/
├── .gitattributes
├── .gitignore
├── .gradle/                          (Gradle cache -- not source)
├── bin/                              (Compiled output -- not source)
├── build/                            (Build output -- not source)
├── build.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── HELP.md
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/gameconnect/
    │   │       ├── BackendApplication.java
    │   │       ├── common/
    │   │       │   └── exception/
    │   │       │       ├── ApiError.java
    │   │       │       ├── BusinessException.java
    │   │       │       └── GlobalExceptionHandler.java
    │   │       └── web/
    │   │           └── HealthController.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │           └── migration/
    │               └── V1__init.sql
    └── test/
        └── java/
            └── com/gameconnect/
                ├── BackendApplicationTests.java
                ├── FlywaySchemaTest.java
                ├── TestBackendApplication.java
                └── TestcontainersConfiguration.java
```

**Notable: There are NO entity classes, NO DTO classes, NO repository interfaces, NO service classes, and NO domain-specific controllers yet.** Phase 1 established only the infrastructure skeleton.

---

## 2. Build System -- `build.gradle`

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
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.flywaydb:flyway-database-postgresql'
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
}
```

**Key observations:**
- Spring Boot **4.1.1** (cutting-edge)
- Java **21** toolchain
- Gradle **9.7.1** wrapper
- Runtime: **PostgreSQL** via `postgresql` driver
- Production starters: webmvc, data-jpa, flyway, validation, actuator
- Test starters: testcontainers with postgres, all module-specific test starters
- Spring Security starter is NOT in `implementation` (only `testImplementation` for security-test), confirming it is intentionally excluded in Phase 1

---

## 3. `settings.gradle`

```groovy
rootProject.name = 'backend'
```

Single-module project named `backend`.

---

## 4. `application.yml`

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

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**Key observations:**
- Database: PostgreSQL on port **5430** (mapped from container port 5432)
- Database name/user/password: `gameconnect` / `gameconnect` / `gameconnect`
- JPA `ddl-auto` is `validate` -- schema managed entirely by Flyway, Hibernate only validates
- `open-in-view` is explicitly **false** (best practice)
- Flyway enabled, migrations at `classpath:db/migration`
- Server port: **8080**
- Actuator exposes only `health` and `info` endpoints
- All values are overridable via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`)

---

## 5. `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:16
    container_name: gameconnect-db
    environment:
      POSTGRES_DB: gameconnect
      POSTGRES_USER: gameconnect
      POSTGRES_PASSWORD: gameconnect
    ports:
      - "5430:5432"
    volumes:
      - gameconnect-pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gameconnect -d gameconnect"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  gameconnect-pgdata:
```

**Key observations:**
- Single service: `db` (PostgreSQL 16)
- Host port **5430** mapped to container port **5432**
- Persistent volume `gameconnect-pgdata` for data durability
- Healthcheck using `pg_isready` with 5s interval, 5s timeout, 10 retries
- No application service container yet (only the database)

---

## 6. Flyway Migration -- `V1__init.sql`

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
    -- A player may have at most one request per game, in any state.
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
    -- At most one rating per rater per ratee per game.
    CONSTRAINT uq_rating UNIQUE (game_id, rater_id, ratee_id)
);

CREATE INDEX idx_player_rating_ratee ON player_rating (ratee_id);
```

**Schema Summary -- 5 tables with rigorous constraints:**

| Table | Purpose | PK | FKs | Unique Constraints | Check Constraints | Indexes |
|---|---|---|---|---|---|---|
| `app_user` | User accounts | UUID | -- | `email` | `skill_level` enum | -- |
| `game` | Match/game listings | UUID | `owner_id -> app_user` | -- | `status`, `format`, `skill_level`, `capacity`, `time_window`, `required_players` | 5 indexes |
| `join_request` | Request to join a game | UUID | `game_id -> game`, `user_id -> app_user` | `(game_id, user_id)` | `status` enum | 2 indexes |
| `match_participant` | Confirmed participants | UUID | `game_id -> game`, `user_id -> app_user` | `(game_id, user_id)` | `role` enum | 1 index |
| `player_rating` | Peer ratings (1-5) | UUID | `game_id -> game`, `rater_id -> app_user`, `ratee_id -> app_user` | `(game_id, rater_id, ratee_id)` | `score` range, no self-rating | 1 index |

**Game statuses:** `OPEN`, `FULL`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
**Game formats:** `5V5`, `6V6`, `7V7`, `8V8`, `9V9`, `11V11`
**Skill levels:** `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
**Join request statuses:** `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`
**Participant roles:** `PLAYER`, `OWNER`

---

## 7. Main Application -- `BackendApplication.java`

```java
package com.gameconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

/**
 * Spring Security is intentionally disabled during Phase 1. A real security
 * configuration (JWT via HTTP-only cookie) is introduced in Phase 2.
 */
// // @SpringBootApplication(exclude = {
// // 		SecurityAutoConfiguration.class,
// // 		UserDetailsServiceAutoConfiguration.class
// // })
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
```

**Key observations:**
- Default timezone set to **Asia/Kolkata** (IST) at JVM level
- Spring Security exclusion is commented out -- the project simply does not include the security starter dependency in `implementation`
- Comment explicitly states JWT via HTTP-only cookie comes in Phase 2

---

## 8. Health Controller -- `HealthController.java`

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
        System.out.println("I am in the controller");
        return Map.of(
                "status", "UP",
                "service", "game-connect",
                "timestamp", Instant.now()
        );
    }
}
```

**Key observations:**
- Base path: `/api`
- Endpoint: `GET /api/health`
- Returns JSON: `{ "status": "UP", "service": "game-connect", "timestamp": "..." }`
- Contains a `System.out.println` debug statement (likely to be removed later)

---

## 9. Exception Handling Framework

### `ApiError.java`

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

A standard API error response record with `timestamp`, `status` (int), `code` (string enum-like), `message` (human-readable), and `path` (request URI). Factory method `of()` stamps the current instant.

### `BusinessException.java`

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

A domain exception carrying an HTTP status and error code string. Services will throw this for business rule violations (e.g., "game is full", "user already requested to join").

### `GlobalExceptionHandler.java`

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

**Exception handling matrix:**

| Exception | HTTP Status | Code | Behavior |
|---|---|---|---|
| `BusinessException` | From exception | From exception | Forward status+code from exception |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | Collects all field errors into message |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_BODY` | Generic body parse failure |
| `NoResourceFoundException` | 404 | `NOT_FOUND` | Spring MVC 404 catch-all |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` | Logs full stack trace, returns generic message |

---

## 10. Test Files

### `BackendApplicationTests.java`

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

Basic context-loads test. Imports the Testcontainers configuration so a real PostgreSQL container starts before the Spring context boots.

### `TestcontainersConfiguration.java`

```java
package com.gameconnect;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

}
```

Creates a `postgres:16` Testcontainer with `@ServiceConnection` so Spring Boot auto-configures the datasource URL/credentials from the container's dynamic mapping.

### `TestBackendApplication.java`

```java
package com.gameconnect;

import org.springframework.boot.SpringApplication;

public class TestBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
```

A test-only main class for running the full app with Testcontainers (useful for local development without Docker Compose).

### `FlywaySchemaTest.java`

```java
package com.gameconnect;

import static org.assertjj.api.Assertions.assertThat;

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

Three integration tests against a real PostgreSQL container:
1. **flywayAppliesInitialMigration** -- asserts at least 1 migration was applied
2. **coreTablesExist** -- queries `information_schema.tables` and asserts all 5 tables exist
3. **uniqueAndCheckConstraintsArePresent** -- queries `information_schema.table_constraints` and asserts 7 specific named constraints exist

---

## 11. What Was Implemented in Phase 1 -- Summary

Phase 1 was an **infrastructure/skeleton phase**. Here is exactly what was built:

### What EXISTS:
| Layer | What | Files |
|---|---|---|
| **Build** | Gradle build with Spring Boot 4.1.1, Java 21, all required starters | `build.gradle`, `settings.gradle`, `gradle/wrapper/*` |
| **Database** | PostgreSQL 16 via Docker Compose with health check and persistent volume | `docker-compose.yml` |
| **Schema** | Complete Flyway migration with 5 tables, 15+ indexes, 12+ constraints | `V1__init.sql` |
| **Configuration** | `application.yml` with env-var overridable datasource, Flyway, actuator | `application.yml` |
| **Application entry** | Spring Boot main class with IST timezone default | `BackendApplication.java` |
| **Health endpoint** | `GET /api/health` returning status/service/timestamp | `HealthController.java` |
| **Error handling** | `ApiError` record, `BusinessException`, `GlobalExceptionHandler` with 5 handlers | 3 files in `common/exception/` |
| **Tests** | Context load, Flyway schema verification (tables + constraints) with Testcontainers | 4 test files |
| **Docker** | Compose with Postgres only (no app container yet) | `docker-compose.yml` |

### What does NOT EXIST yet (intentionally deferred to later phases):
- **No JPA Entity classes** -- the 5 tables have no corresponding `@Entity` POJOs
- **No DTO/Request/Response records**
- **No Repository interfaces** (no Spring Data JPA repositories)
- **No Service classes** (no business logic)
- **No domain controllers** (no GameController, UserController, etc.)
- **No Security configuration** (explicitly noted as Phase 2 with JWT/HTTP-only cookie)
- **No Lombok or MapStruct** dependencies
- **No API documentation** (no Swagger/OpenAPI)
- **No Dockerfile** for the application itself
- **No CI/CD configuration**

Phase 1 delivered a runnable Spring Boot skeleton with a fully-defined database schema, a health check, a global error-handling framework, and integration tests proving the schema migrates correctly against real PostgreSQL.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Explore frontend codebase structure",
  "subagent_type": "explore",
  "prompt": "Quick exploration of the frontend codebase at F:\\Game-connect\\frontend.\n\n1. Read the full directory structure under frontend/\n2. Read package.json\n3. Read next.config.js/ts if it exists\n4. Read any configuration files\n5. Read a few key source files to understand the setup\n\nReturn the complete contents of configuration files and a summary of the frontend structure."
}
```

**Output:**
```
<task id="ses_f7b231440ffe357i0NnvaVMbvq" state="completed">
<task_result>
I have now explored the entire frontend codebase thoroughly. Here is my report.

## Frontend Structure Summary

`F:\Game-connect\frontend` is a **Next.js 16 (App Router)** application built with **React 19**, **TypeScript**, and **Tailwind CSS v4**. It's a "Game Connect" app for **pickup football matchmaking** — currently at an early "Phase 1 shell" stage.

### Complete directory structure

```
frontend/
├── .gitignore
├── AGENTS.md                    (contains auto-generated nextjs-agent-rules block)
├── CLAUDE.md                    (@AGENTS.md reference)
├── README.md                    (default create-next-app)
├── eslint.config.mjs
├── next-env.d.ts
├── next.config.ts
├── package.json
├── package-lock.json
├── postcss.config.mjs
├── tsconfig.json
├── app/
│   ├── (app)/                   [route group — mobile app shell w/ bottom nav]
│   │   ├── games/
│   │   │   ├── [id]/page.tsx    (dynamic game detail route)
│   │   │   └── page.tsx         (game list)
│   │   └── layout.tsx
│   ├── favicon.ico
│   ├── globals.css
│   ├── layout.tsx               (root layout)
│   └── page.tsx                 (redirects "/" → "/games")
├── components/
│   └── layout/
│       └── BottomNav.tsx        ("use client" bottom navigation bar)
├── lib/
│   ├── api/
│   │   └── client.ts            (API fetch wrapper)
│   └── types.ts                 (domain type definitions)
├── public/                      (default SVG assets)
└── node_modules/                (next/ docs available at node_modules/next/dist/docs/)
```

---

### Configuration Files (complete contents)

**`package.json`** — name `gc-frontend`, v0.1.0
- Scripts: `dev`, `build`, `start`, `lint` (eslint)
- Dependencies: `next@16.3.4`, `react@19.2.8`, `react-dom@19.2.8`
- DevDependencies: `@tailwindcss/postcss@^4`, `tailwindcss@^4`, `eslint@^9`, `eslint-config-next@16.3.4`, `typescript@^5`, `@types/node@^20`, `@types/react@^19`, `@types/react-dom@^19`

**`next.config.ts`** — essentially empty/stock config (only the `NextConfig` type import and empty object export).

**`tsconfig.json`** — strict TS, `target: ES2017`, `jsx: react-jsx`, `moduleResolution: bundler`, `paths: "@/ *" → "./*"`, includes `next-env.d.ts` + all `.ts/.tsx/.mts` (excludes `node_modules`).

**`postcss.config.mjs`** — plugin `@tailwindcss/postcss` (Tailwind v4 PostCSS setup).

**`eslint.config.mjs`** — uses `eslint-config-next/core-web-vitals` + `eslint-config-next/typescript`; `globalIgnores` for `.next/**`, `out/**`, `build/**`, `next-env.d.ts`.

**`next-env.d.ts`** — typed references (`next`, `next/image-types/global`) plus `.next/types` route/param imports.

**`.gitignore`** — ignores `node_modules`, `.next`, `out`, `build`, coverage, `.env*`, `*.tsbuildinfo`, `next-env.d.ts`, etc.

**`globals.css`** — imports `tailwindcss`, defines `--background`/`--foreground` CSS vars with dark-mode `prefers-color-scheme` media query, and `@theme inline` mapping to Geist font variables.

---

### Key Source Files

**`app/layout.tsx`** — Root layout. Loads `Geist` and `Geist_Mono` fonts via `next/font/google`, sets metadata title **"Game Connect"** / description **"Pickup football matchmaking"**, and configures `html`/`body` with `h-full antialiased` and `min-h-full flex flex-col` classes.

**`app/page.tsx`** — `Home` component that just calls `redirect("/games")`.

**`app/(app)/layout.tsx`** — Route-group layout. Wraps content in a mobile-centered column (`mx-auto max-w-md flex-col`), with a `<main className="flex-1 pb-20">` area plus `<BottomNav />`.

**`app/(app)/games/page.tsx`** — Games list page. Uses a `PLACEHOLDER_GAMES` empty array and shows a "No games available yet" message noting that "Game discovery arrives in a later phase; this is the Phase 1 shell." Has a "Create" link to `/create-game`.

**`app/(app)/games/[id]/page.tsx`** — Server-rendered dynamic route. Uses the typed `PageProps<"/games/[id]">`, awaits `params`, calls `notFound()` if `id` missing, and shows a placeholder detail view.

**`components/layout/BottomNav.tsx`** — Client component. Renders a fixed bottom nav with 4 links: **Games** (`/games`), **Create** (`/create-game`), **My Games** (`/my-games`), **Profile** (`/profile`). Active state highlighted in `text-emerald-600` using `usePathname()` (with special handling so `/games` matches any `/games/*` path).

**`lib/api/client.ts`** — API helper module:
- `API_BASE_URL` = `process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"`
- Exports `ApiError` class (with `status` and `code` fields)
- Exports `apiFetch<T>()` generic wrapper handling JSON body, `credentials: "include"`, error parsing (`{code, message}`), and `204` handling.

**`lib/types.ts`** — Domain types: `SkillLevel`, `GameFormat`, `GameStatus`, `RequestStatus`, `ParticipantRole` enums, plus `UserProfile`, `PlayerStats`, `Game`/`GameSummary`, `GameParticipant`, `JoinRequest`, `PlayerRating` interfaces.

---

### Notable observations

- **Route structure**: The `(app)` route group represents the authenticated mobile app shell, while routes like `/create-game`, `/my-games`, and `/profile` are referenced in the nav but **do not yet have page files** — they're planned for later phases.
- **Branching routes**: Several planned features (game discovery, game detail, create-game, my-games, profile, messaging) are not yet implemented; the current pages are placeholder shells.
- **Backend integration**: The frontend is wired to expect a backend at `http://localhost:8080/api` (configurable via `NEXT_PUBLIC_API_BASE_URL`), and typed domain models exist that mirror a backend API contract.
- **Stack**: Next.js 16.3 App Router + React 19 + TypeScript strict + Tailwind CSS v4 (`@tailwindcss/postcss`), ESLint 9 flat config.
- **No `.env` files** are currently present in the repo (they're gitignored).
</task_result>
</task>
```

---

## Assistant (Plan · Big Pickle · 74.4s)

_Thinking:_

Now I have a thorough understanding of the entire codebase. Let me plan Phase 2: Authentication.

Key facts from Phase 1:
- Spring Boot 4.1.1, Java 21, Gradle
- PostgreSQL 16, Flyway migrations
- No JPA entities, no repositories, no services, no security config yet
- The `app_user` table already has `email`, `password_hash`, `display_name`, `bio`, `profile_image_url`, `skill_level`, `created_at`
- `password_hash` column already exists in the schema - perfect for BCrypt
- No Spring Security starter in `implementation` dependencies yet
- Exception handling framework exists with `ApiError`, `BusinessException`, `GlobalExceptionHandler`
- GlobalExceptionHandler already handles validation errors, business exceptions, etc.
- No Lombok or MapStruct

For Phase 2, I need to:

1. **Dependencies** - Add `spring-boot-starter-security`, `jjwt` (or `spring-security-oauth2-jose`), `bcrypt` (included with spring-security)

2. **Entity** - Create `User` JPA entity mapped to `app_user` table

3. **Repository** - Create `UserRepository` interface

4. **DTOs** - Create request/response records for registration, login, auth response

5. **Service** - Create `AuthService` for registration/login, `UserService` for user lookup

6. **Controller** - Create `AuthController` with endpoints

7. **Security Config** - Create `SecurityConfiguration` defining public/protected endpoints

8. **JWT** - Create JWT utility/service for token generation/validation

9. **JWT Filter** - Create authentication filter that extracts JWT from cookie and sets SecurityContext

10. **Configuration** - JWT secret and expiration from env vars

11. **Tests** - Integration tests for all auth flows

Let me think about the JWT library choice. For Spring Boot 4.1.1, the most common choices are:
- `io.jsonwebtoken:jjwt` (JJWT) - most popular, mature
- `com.auth0:java-jwt` - simpler API
- Spring Security OAuth2 Resource Server (built-in JWT support)

Since the requirement is minimal MVP, I'll go with JJWT as it's the most standard choice for Spring Boot JWT implementations.

For HTTP-only cookies:
- Set the JWT as an HTTP-only, Secure, SameSite cookie on login
- The JWT filter reads from the cookie (not Authorization header)
- This is more secure than localStorage tokens

Let me also think about what the auth flow looks like:
1. Register: POST /api/auth/register → creates user, returns JWT in cookie
2. Login: POST /api/auth/login → validates credentials, returns JWT in cookie
3. Me: GET /api/auth/me → reads JWT from cookie, returns current user profile
4. Logout: POST /api/auth/logout → clears the cookie

Now let me plan the exact files:

### New files to create:

**Entities:**
- `backend/src/main/java/com/gameconnect/auth/entity/User.java`

**DTOs:**
- `backend/src/main/java/com/gameconnect/auth/dto/RegisterRequest.java`
- `backend/src/main/java/com/gameconnect/auth/dto/LoginRequest.java`
- `backend/src/main/java/com/gameconnect/auth/dto/AuthResponse.java`
- `backend/src/main/java/com/gameconnect/auth/dto/UserResponse.java`

**Repositories:**
- `backend/src/main/java/com/gameconnect/auth/repository/UserRepository.java`

**Services:**
- `backend/src/main/java/com/gameconnect/auth/service/AuthService.java`
- `backend/src/main/java/com/gameconnect/auth/service/UserDetailsServiceImpl.java`

**Controllers:**
- `backend/src/main/java/com/gameconnect/auth/controller/AuthController.java`

**Security:**
- `backend/src/main/java/com/gameconnect/security/SecurityConfiguration.java`
- `backend/src/main/java/com/gameconnect/security/JwtService.java`
- `backend/src/main/java/com/gameconnect/security/JwtAuthenticationFilter.java`

**Flyway Migration:**
- No new migration needed - the `app_user` table with `password_hash` already exists

**Tests:**
- `backend/src/test/java/com/gameconnect/auth/AuthIntegrationTest.java`

### Files to modify:

**`build.gradle`** - Add `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

**`application.yml`** - Add JWT secret and expiration configuration

Let me think about whether I should organize under `auth/` or follow the PRODUCT.md architecture pattern (controller, service, repository, entity, dto, mapper, security, exception).

The PRODUCT.md says:
```
controller
service
repository
entity
dto
mapper
security
exception
```

This could be interpreted as either:
1. Flat packages: `controller`, `service`, `repository`, etc. (feature-agnostic)
2. Or feature-based packages with these sub-layers

Looking at the existing code, we have `com.gameconnect.common.exception` and `com.gameconnect.web` - so the existing pattern is domain-based packages. But the architecture mentions flat packages. Since this is a modular monolith, I think the most practical approach is to use a feature-based module `auth` containing its own controller/service/repository/entity/dto. This keeps things organized as the codebase grows.

Actually, let me reconsider. The PRODUCT.md architecture says:
```
controller
service
repository
entity
dto
mapper
security
exception
```

This looks like it wants flat packages. But for a modular monolith, feature-based packaging is more practical. The existing code has `common.exception` and `web` packages, which are cross-cutting concerns.

I'll organize it as:
- `com.gameconnect.auth` - the auth module
  - `entity` - User entity
  - `dto` - request/response DTOs
  - `repository` - UserRepository
  - `service` - AuthService
  - `controller` - AuthController
- `com.gameconnect.security` - cross-cutting security
  - SecurityConfiguration
  - JwtService
  - JwtAuthenticationFilterThis preserves the modular monolith structure while keeping things clean.

Now let me think about the detailed implementation plan:

### Security Configuration
- `/api/auth/**` is public (register, login)
- `/api/health` is public
- All other `/api/**` endpoints require authentication
- CSRF disabled (API-only, cookie-based JWT)
- Stateless session management
- Custom JWT filter before `UsernamePasswordAuthenticationFilter`

### JWT Strategy
- Secret from `JWT_SECRET` env var
- Expiration from `JWT_EXPIRATION_MINUTES` env var (default: 60 minutes)
- Token contains: `sub` (user ID as UUID), `email`
- Signed with HMAC-SHA256 (HS256)

### Cookie Strategy
- Cookie name: `auth_token`
- HTTP-only: true
- Secure: true (production) / false (development)
- SameSite: Lax
- Path: /
- Max age: matches JWT expiration

### AuthController Endpoints
- `POST /api/auth/register` - Register a new user
  - Body: `{ email, password, displayName }`
  - Response: Sets JWT cookie, returns `{ id, email, displayName }`
  - Errors: 409 for duplicate email, 400 for validation
  
- `POST /api/auth/login` - Login
  - Body: `{ email, password }`
  - Response: Sets JWT cookie, returns `{ id, email, displayName }`
  - Errors: 401 for invalid credentials

- `GET /api/auth/me` - Get current user
  - Requires: valid JWT cookie
  - Response: `{ id, email, displayName, bio, profileImageUrl, skillLevel }`
  - Errors: 401 if not authenticated

- `POST /api/auth/logout` - Logout
  - Clears the JWT cookie
  - Response: 204 No Content

### Validation
- Email: @Email, @NotBlank
- Password: @NotBlank, @Size(min = 8)
- DisplayName: @NotBlank, @Size(max = 80)

### Error Handling Additions
- `DUPLICATE_EMAIL` - 409 Conflict
- `INVALID_CREDENTIALS` - 401 Unauthorized
- Need to update `GlobalExceptionHandler` or add a new handlerWait, the `GlobalExceptionHandler` already handles `BusinessException`. I can just throw `BusinessException` from the service layer for these cases.

For Spring Security's authentication entry point (401 on unauthenticated access), I need to customize it to return the same `ApiError` format instead of the default Spring Security HTML/text response.

Also need to handle Spring Security's access denied (403) to return `ApiError` format.

Let me also think about whether there are any concerns with Spring Boot 4.1.1. This is a very new version. The Spring Security APIs might have some changes. But the core patterns should be the same.

Actually, I should also check - does the `app_user` table already have a `password_hash` column? Yes it does! From V1__init.sql:
```sql
password_hash     VARCHAR(255) NOT NULL,
```

This was clearly designed for Phase 2 auth. Good.

Let me finalize the plan and present it clearly.

One more consideration: should I use `BCryptPasswordEncoder` or `DelegatingPasswordEncoder`? For MVP, `BCryptPasswordEncoder` is simpler and sufficient.

For the JWT library, I need to check compatibility with Java 21 and Spring Boot 4.1.1. JJWT 0.12.x should work fine.

Let me think about test coverage:
1. Register success → 200 + cookie set
2. Register duplicate email → 409
3. Register validation error → 400
4. Login success → 200 + cookie set
5. Login wrong password → 401
6. Login non-existent email → 401
7. GET /api/auth/me without cookie → 401
8. GET /api/auth/me with valid cookie → 200 + user data
9. GET /api/auth/me with expired JWT → 401
10. GET /api/auth/me with invalid JWT → 401
11. Logout → cookie cleared
12. GET /api/health without auth → 200 (public)

Now let me present this plan.

I now have a complete understanding of the codebase. Let me formulate the Phase 2 plan.

---

# Phase 2 Implementation Plan: Authentication

## Current State

Phase 1 delivered:
- Spring Boot 4.1.1, Java 21, Gradle
- PostgreSQL 16 via Docker Compose
- Complete Flyway schema (`V1__init.sql`) with 5 tables including `app_user` which already has `password_hash VARCHAR(255) NOT NULL`
- Global exception handling (`ApiError`, `BusinessException`, `GlobalExceptionHandler`)
- Health endpoint at `GET /api/health`
- Testcontainers integration tests
- **No JPA entities, no repositories, no services, no Spring Security**

---

## Dependencies to Add (`build.gradle`)

```
spring-boot-starter-security
jjwt-api          0.12.6
jjwt-impl         0.12.6  (runtime)
jjwt-jackson      0.12.6  (runtime)
```

No new Flyway migration needed — `password_hash` column already exists.

---

## Files to Create

### Entity

| File | Description |
|---|---|
| `src/main/java/com/gameconnect/auth/entity/User.java` | JPA `@Entity` mapped to `app_user`. Fields: `id` (UUID), `email`, `passwordHash`, `displayName`, `bio`, `profileImageUrl`, `skillLevel` (enum), `createdAt`. No Lombok — manual getters/constructors to stay dependency-light. |

### DTOs

| File | Description |
|---|---|
| `auth/dto/RegisterRequest.java` | Record: `email`, `password`, `displayName` with `@Email`, `@NotBlank`, `@Size` validation |
| `auth/dto/LoginRequest.java` | Record: `email`, `password` with `@NotBlank`, `@Email` validation |
| `auth/dto/AuthResponse.java` | Record: `id`, `email`, `displayName` — returned on register/login/me |
| `auth/dto/MeResponse.java` | Record: `id`, `email`, `displayName`, `bio`, `profileImageUrl`, `skillLevel` — full profile for `/me` |

### Repository

| File | Description |
|---|---|
| `auth/repository/UserRepository.java` | `JpaRepository<User, UUID>` with `Optional<User> findByEmail(String email)` and `boolean existsByEmail(String email)` |

### Service

| File | Description |
|---|---|
| `auth/service/AuthService.java` | `register()` — validate unique email, hash password with BCrypt, persist, generate JWT, return `AuthResponse`. `login()` — look up by email, verify BCrypt, generate JWT, return `AuthResponse`. Throws `BusinessException` for duplicate email (409) and invalid credentials (401). |

### Controller

| File | Description |
|---|---|
| `auth/controller/AuthController.java` | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`. Sets/clears `auth_token` HTTP-only cookie. `/me` resolves user from `SecurityContext` (never from request body). |

### Security

| File | Description |
|---|---|
| `security/SecurityConfiguration.java` | `@Configuration`. Public: `/api/auth/**`, `/api/health`. All other `/api/**` requires authentication. CSRF disabled (stateless API). Stateless session. Registers `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`. Custom `AuthenticationEntryPoint` returning `ApiError` JSON on 401. Custom `AccessDeniedHandler` returning `ApiError` JSON on 403. |
| `security/JwtService.java` | `@Component`. Generates JWT (HS256, subject = user UUID, claim = email). Validates and parses JWT. Reads secret from `JWT_SECRET` env var, expiration from `JWT_EXPIRATION_MINUTES` env var (default 60). |
| `security/JwtAuthenticationFilter.java` | Extends `OncePerRequestFilter`. Reads `auth_token` cookie from request. If present, validates JWT, loads `UserDetails`, sets `SecurityContext`. On failure, continues chain unauthenticated (filter is non-blocking). |

---

## Files to Modify

| File | Change |
|---|---|
| `build.gradle` | Add `spring-boot-starter-security` to `implementation`, add `jjwt-api`/`jjwt-impl`/`jjwt-jackson` |
| `application.yml` | Add `jwt.secret` and `jwt.expiration-minutes` properties (env-var backed) |
| `BackendApplication.java` | Remove commented-out Security exclusion code (clean up) |
| `HealthController.java` | Remove `System.out.println` debug line |

---

## Auth Flow Design

```
Register:
  POST /api/auth/register
  Body: { "email": "...", "password": "...", "displayName": "..." }
  → BCrypt hash password
  → Save User to DB
  → Generate JWT (sub=user UUID, email)
  → Set auth_token cookie (HttpOnly, Secure, SameSite=Lax, Path=/)
  → Return 200: { "id", "email", "displayName" }

Login:
  POST /api/auth/login
  Body: { "email": "...", "password": "..." }
  → Look up user by email
  → Verify password with BCrypt
  → Generate JWT
  → Set auth_token cookie
  → Return 200: { "id", "email", "displayName" }

Me:
  GET /api/auth/me
  Cookie: auth_token=<jwt>
  → Filter extracts JWT from cookie → validates → loads User into SecurityContext
  → Controller reads authenticated user from SecurityContext
  → Return 200: { "id", "email", "displayName", "bio", "profileImageUrl", "skillLevel" }

Logout:
  POST /api/auth/logout
  → Clear auth_token cookie
  → Return 204 No Content
```

---

## JWT Cookie Configuration

| Property | Value |
|---|---|
| Name | `auth_token` |
| HttpOnly | `true` |
| Secure | `true` (controlled by env var `COOKIE_SECURE`, default `true`) |
| SameSite | `Lax` |
| Path | `/` |
| Max Age | Matches JWT expiration |

---

## Error Responses

All errors use the existing `ApiError` format:

| Scenario | Status | Code |
|---|---|---|
| Duplicate email on register | 409 | `DUPLICATE_EMAIL` |
| Invalid credentials on login | 401 | `INVALID_CREDENTIALS` |
| Validation errors | 400 | `VALIDATION_ERROR` |
| Unauthenticated access to protected endpoint | 401 | `UNAUTHORIZED` |
| Forbidden access | 403 | `FORBIDDEN` |
| Expired/invalid JWT | 401 | `UNAUTHORIZED` |

Spring Security 401/403 responses are routed through custom `AuthenticationEntryPoint` and `AccessDeniedHandler` to return `ApiError` JSON (not Spring's default HTML).

---

## Security Endpoint Matrix

| Endpoint | Auth Required |
|---|---|
| `GET /api/health` | No |
| `POST /api/auth/register` | No |
| `POST /api/auth/login` | No |
| `POST /api/auth/logout` | Yes (to clear cookie contextually) |
| `GET /api/auth/me` | Yes |
| All other `/api/**` | Yes |

---

## Test Plan

| # | Test | Expected |
|---|---|---|
| 1 | Register with valid data | 200, cookie set, user returned |
| 2 | Register with duplicate email | 409 `DUPLICATE_EMAIL` |
| 3 | Register with invalid email | 400 `VALIDATION_ERROR` |
| 4 | Register with short password | 400 `VALIDATION_ERROR` |
| 5 | Login with correct credentials | 200, cookie set, user returned |
| 6 | Login with wrong password | 401 `INVALID_CREDENTIALS` |
| 7 | Login with non-existent email | 401 `INVALID_CREDENTIALS` |
| 8 | GET /api/auth/me without cookie | 401 `UNAUTHORIZED` |
| 9 | GET /api/auth/me with valid JWT | 200, full user profile |
| 10 | GET /api/auth/me with expired JWT | 401 `UNAUTHORIZED` |
| 11 | GET /api/auth/me with invalid JWT | 401 `UNAUTHORIZED` |
| 12 | POST /api/auth/logout | 204, cookie cleared |
| 13 | GET /api/health without auth | 200 (public) |
| 14 | Protected endpoint without auth | 401 |

All tests use Testcontainers with real PostgreSQL (same pattern as Phase 1).

---

## Summary of All File Operations

**Create (11 files):**
```
auth/entity/User.java
auth/dto/RegisterRequest.java
auth/dto/LoginRequest.java
auth/dto/AuthResponse.java
auth/dto/MeResponse.java
auth/repository/UserRepository.java
auth/service/AuthService.java
auth/controller/AuthController.java
security/SecurityConfiguration.java
security/JwtService.java
security/JwtAuthenticationFilter.java
```

**Modify (4 files):**
```
build.gradle
application.yml
BackendApplication.java
HealthController.java
```

**Tests (1 new file):**
```
test/.../auth/AuthIntegrationTest.java
```

---

No database migrations needed. No Redis, no refresh tokens, no OAuth. Pure MVP JWT-over-HTTP-only-cookie auth.

Shall I proceed with implementation?

---

