-- Add consumed_by_class_id column to module_access_code table if it doesn't exist
-- This column tracks which class a user was in when they consumed an access code

DO $$ 
BEGIN
    -- Check if column already exists
    IF NOT EXISTS (
        SELECT FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'module_access_code' 
        AND column_name = 'consumed_by_class_id'
    ) THEN
        -- Add the column
        ALTER TABLE module_access_code 
        ADD COLUMN consumed_by_class_id UUID;
        
        -- Add foreign key constraint if class table exists
        IF EXISTS (
            SELECT FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name = 'class'
        ) THEN
            ALTER TABLE module_access_code 
            ADD CONSTRAINT fk_module_access_code_consumed_by_class 
            FOREIGN KEY (consumed_by_class_id) 
            REFERENCES class(id) ON DELETE SET NULL;
        END IF;
        
        -- Add index for class-specific queries
        CREATE INDEX IF NOT EXISTS idx_module_access_code_consumed_by_class 
        ON module_access_code(consumed_by_class_id);
        
        RAISE NOTICE 'Added consumed_by_class_id column to module_access_code table';
    ELSE
        RAISE NOTICE 'consumed_by_class_id column already exists in module_access_code table';
    END IF;
END $$;

