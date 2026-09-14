-- Phase 3: Add player position to app_user

ALTER TABLE app_user
    ADD COLUMN position VARCHAR(20);

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_position
    CHECK (position IS NULL OR position IN (
        'GOALKEEPER', 'DEFENDER', 'MIDFIELDER',
        'WINGER', 'STRIKER', 'FLEXIBLE'
    ));
