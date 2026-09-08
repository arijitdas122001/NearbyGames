# Game Connect

## Product

Football Connect is a pickup football matchmaking platform.

Users who have already booked a football turf can create a game on the platform and invite random players to join.

Players can discover games and request to join.

The game owner has authority to accept or reject players based on their profile, experience, skill level and previous participation.

The platform does NOT initially handle turf booking.
Users manually enter the turf and booking information.

---

# Core User Roles

## Player

A player can:

- Register/login
- Create and edit profile
- Set skill level
- Browse available games
- Filter games
- Request to join a game
- Cancel their request
- View accepted games
- View match history
- View their football statistics

## Game Owner

A game owner is also a player.

They can:

- Create a game
- Define game format
- Define maximum players
- Define required players
- Set skill level
- Set joining fee
- View join requests
- Accept players
- Reject players
- Remove a player before the match
- Mark player attendance after the match
- Close the game

---

# MVP

The MVP should contain:

1. Authentication
2. Player profiles
3. Create game
4. Browse games
5. Game details
6. Request to join
7. Accept/reject requests
8. Player statistics
9. Match history
10. Attendance

Do NOT implement:

- Turf owner integration
- Online payments
- Chat
- Push notifications
- Advanced recommendation algorithms
- Teams
- Tournaments
- Subscriptions
- Microservices

Keep the MVP small.

---

# Player Profile

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

Skill levels:

BEGINNER
INTERMEDIATE
ADVANCED

Positions:

GOALKEEPER
DEFENDER
MIDFIELDER
WINGER
STRIKER
FLEXIBLE

---

# Game

A game contains:

- id
- owner
- turf name
- turf address
- latitude
- longitude
- date
- start time
- end time
- format
- skill level
- maximum players
- joining fee
- description
- status

Formats:

5V5
6V6
7V7
8V8
9V9
11V11

Game statuses:

OPEN
FULL
IN_PROGRESS
COMPLETED
CANCELLED

---

# Join Request

A player can request to join an OPEN game.

Request states:

PENDING
ACCEPTED
REJECTED
CANCELLED

A player cannot have multiple active requests for the same game.

A player cannot request to join their own game.

A player cannot request to join a FULL game.

Once the maximum number of players is reached, the game becomes FULL.

---

# Match Participation

After the game:

The game owner marks whether each accepted player attended.

Attendance affects the player's statistics.

No-show should negatively affect attendance percentage.

Only completed games should affect match statistics.

---

# Ratings

For MVP:

Players can rate other players after a completed match.

Rating:

1-5

Calculate average player rating.

Do not implement complex skill-rating algorithms yet.

---

# Backend

Use:

Java
Spring Boot
Spring Security
JWT
Spring Data JPA
PostgreSQL
Flyway
Bean Validation

Use a modular monolith.

DO NOT use microservices.

Architecture:

controller
service
repository
entity
dto
mapper
security
exception

Use DTOs rather than exposing JPA entities directly.

---

# Frontend

Use:

Next.js
TypeScript
Tailwind CSS

Pages:

/login
/register
/games
/games/[id]
/create-game
/my-games
/profile
/profile/[id]

The UI should be mobile-first because most users will use this on their phone.

---

# Database

Use PostgreSQL.

Important relationships:

User 1 -> N Game
User 1 -> N JoinRequest
Game 1 -> N JoinRequest
Game N -> N User through MatchParticipant
Game 1 -> N PlayerRating
User 1 -> N PlayerRating

Use UUIDs for primary keys.

Use database constraints wherever appropriate.

---

# Important business rules

1. Only the game owner can accept/reject requests.

2. A user cannot join their own game through a join request.

3. A player cannot join a FULL game.

4. A player cannot have duplicate active requests.

5. Accepting a player must atomically verify that a slot is still available.

6. Prevent race conditions when two requests are accepted simultaneously.

7. Only the game owner can mark attendance.

8. Statistics should be derived from match participation records.

9. Users cannot rate players for games they did not participate in.

10. A player can submit only one rating per player per game.

11. Completed/cancelled games cannot accept new players.

---

# Development strategy

Build the system incrementally.

Phase 1:
Project setup and database

Phase 2:
Authentication

Phase 3:
User profile

Phase 4:
Game creation

Phase 5:
Game discovery

Phase 6:
Join requests

Phase 7:
Game owner management

Phase 8:
Match completion and attendance

Phase 9:
Ratings and statistics

Phase 10:
Frontend polish

After every phase:

- Run tests
- Run the application
- Verify APIs
- Fix compilation errors
- Commit changes to git

Do not implement future features unless explicitly requested.