-- Safely add notification_type column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'notifications' 
        AND column_name = 'notification_type'
    ) THEN
        ALTER TABLE notifications 
        ADD COLUMN notification_type VARCHAR(50) NOT NULL DEFAULT 'POST_CREATED';
        
        -- Update existing notifications to have POST_CREATED type
        UPDATE notifications SET notification_type = 'POST_CREATED' WHERE notification_type IS NULL;
        
        -- Add comment
        COMMENT ON COLUMN notifications.notification_type IS 'Type of notification: POST_CREATED, COMMENT, LIKE, or RATING';
    END IF;
END $$;





