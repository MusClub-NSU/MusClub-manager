CREATE TABLE event_notifications
(
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    send_at    TIMESTAMPTZ NOT NULL,
    sent_at    TIMESTAMPTZ,
    status     VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    subject    TEXT        NOT NULL,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_notifications_status_send_at
    ON event_notifications (status, send_at);
