-- Add notification_type column to notifications table
ALTER TABLE notifications 
ADD COLUMN notification_type VARCHAR(50) NOT NULL DEFAULT 'POST_CREATED';

-- Update existing notifications to have POST_CREATED type
UPDATE notifications SET notification_type = 'POST_CREATED' WHERE notification_type IS NULL;

-- Add comment
COMMENT ON COLUMN notifications.notification_type IS 'Type of notification: POST_CREATED, COMMENT, LIKE, or RATING';

