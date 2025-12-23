-- Manual fix for missing notification_type column
-- Run this script directly on your database if the migration hasn't been applied

-- Check if column exists, and add it if it doesn't
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
        
        RAISE NOTICE 'notification_type column added successfully';
    ELSE
        RAISE NOTICE 'notification_type column already exists';
    END IF;
END $$;








