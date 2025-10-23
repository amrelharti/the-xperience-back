-- Fix attachments column type from JSONB to TEXT
-- This migration handles the case where the column was created as JSONB but should be TEXT
DO $$ 
BEGIN
    -- Check if the column exists and is JSONB type
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'homeworks' 
        AND column_name = 'attachments' 
        AND data_type = 'jsonb'
    ) THEN
        -- Convert JSONB to TEXT
        ALTER TABLE homeworks ALTER COLUMN attachments TYPE TEXT USING attachments::TEXT;
    END IF;
END $$;
