-- Group 1: notification foundation.
-- Recipient address is intentionally not stored here; it is resolved from
-- app_user when a notification is processed.

CREATE TABLE notification (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id             UUID         NOT NULL REFERENCES game (id),
    user_id             UUID         NOT NULL REFERENCES app_user (id),
    notification_type   VARCHAR(30)  NOT NULL,
    scheduled_at        TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMPTZ,
    provider_message_id VARCHAR(255),
    attempt_count       INTEGER      NOT NULL DEFAULT 0,
    last_error          TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- At most one notification of a given type per game and user.
    CONSTRAINT uq_notification_game_user_type UNIQUE (game_id, user_id, notification_type),
    CONSTRAINT ck_notification_type CHECK (notification_type IN ('GAME_REMINDER')),
    CONSTRAINT ck_notification_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_notification_attempt_count CHECK (attempt_count >= 0)
);

-- Primary access path for future due-notification polling.
CREATE INDEX idx_notification_status_scheduled ON notification (status, scheduled_at);
CREATE INDEX idx_notification_game_user ON notification (game_id, user_id);
CREATE INDEX idx_notification_user ON notification (user_id);
