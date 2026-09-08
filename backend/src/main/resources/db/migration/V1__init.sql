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
