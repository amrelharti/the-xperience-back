-- Allow multiple notifications per (user, post) so likes/comments/ratings don't overwrite each other
ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS unique_user_post_notification;

-- Add a non-unique index to keep lookups fast
CREATE INDEX IF NOT EXISTS idx_notifications_user_post ON notifications(user_id, post_id);





