CREATE TABLE IF NOT EXISTS event_members (
  event_id   BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  user_id    BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  role       VARCHAR(32) NOT NULL DEFAULT 'PERFORMER' CHECK (role IN ('ORGANIZER','PERFORMER','VOLUNTEER')),
  added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_members_event ON event_members(event_id);
CREATE INDEX IF NOT EXISTS idx_event_members_user  ON event_members(user_id);
