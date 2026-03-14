ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_data BYTEA,
    ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS avatar_file_name VARCHAR(255);

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED';

CREATE TABLE IF NOT EXISTS event_timeline_items (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    planned_time TIME NOT NULL,
    description VARCHAR(1000) NOT NULL,
    position INTEGER NOT NULL,
    UNIQUE (event_id, position)
);

CREATE INDEX IF NOT EXISTS idx_event_timeline_items_event_position
    ON event_timeline_items(event_id, position);

CREATE TABLE IF NOT EXISTS event_program_items (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    planned_time TIME,
    duration_text VARCHAR(32),
    notes VARCHAR(1000),
    position INTEGER NOT NULL,
    UNIQUE (event_id, position)
);

CREATE INDEX IF NOT EXISTS idx_event_program_items_event_position
    ON event_program_items(event_id, position);
