ALTER TABLE events
  ADD COLUMN IF NOT EXISTS parent_id BIGINT NULL
  REFERENCES events(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_events_parent ON events(parent_id);