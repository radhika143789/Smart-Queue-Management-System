-- V2: Add body column to notifications table
-- Required for RetryScheduler to re-send actual notification content instead of a placeholder.
-- Also adds retry_index for efficient FAILED retry queries.
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS body TEXT;

-- Composite index for the RetryScheduler query: WHERE status='FAILED' AND retry_count < 3
CREATE INDEX IF NOT EXISTS idx_notifications_retry
    ON notifications(status, retry_count)
    WHERE status = 'FAILED';
