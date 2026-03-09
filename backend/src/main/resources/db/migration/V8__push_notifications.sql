-- Push subscriptions table for Web Push API
CREATE TABLE push_subscriptions
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   TEXT         NOT NULL,
    p256dh_key TEXT         NOT NULL,
    auth_key   VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500),
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_push_subscriptions_user_active ON push_subscriptions (user_id, active);
CREATE UNIQUE INDEX idx_push_subscriptions_endpoint ON push_subscriptions (endpoint);

-- Update event_notifications table for push notifications
ALTER TABLE event_notifications
    ADD COLUMN IF NOT EXISTS notification_type VARCHAR(32) NOT NULL DEFAULT 'REMINDER_24H';

ALTER TABLE event_notifications
    ADD COLUMN IF NOT EXISTS action_url VARCHAR(500);

ALTER TABLE event_notifications
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE event_notifications
    ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Rename subject to title for push notifications
ALTER TABLE event_notifications
    RENAME COLUMN subject TO title;

-- Add CANCELLED status support (update existing data if needed)
UPDATE event_notifications
SET status = 'PENDING'
WHERE status NOT IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED');

-- Create index for notification processing
CREATE INDEX IF NOT EXISTS idx_event_notifications_status_sendAt_retry
    ON event_notifications (status, send_at, retry_count);

-- Create index for user notifications lookup
CREATE INDEX IF NOT EXISTS idx_event_notifications_user_sendAt
    ON event_notifications (user_id, send_at DESC);
