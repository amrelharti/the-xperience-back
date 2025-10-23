-- Migration V28: Update live_sessions table to reference training_modules instead of modules
-- This migration renames the foreign key column from module_id to training_module_id

-- Step 1: Check if the old column exists and rename it
DO $$
BEGIN
    -- Check if module_id column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'live_sessions' 
        AND column_name = 'module_id'
    ) THEN
        -- Drop the old foreign key constraint if it exists
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_name = 'live_sessions'
            AND constraint_type = 'FOREIGN KEY'
            AND constraint_name LIKE '%module%'
        ) THEN
            EXECUTE 'ALTER TABLE live_sessions DROP CONSTRAINT ' || (
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'live_sessions'
                AND constraint_type = 'FOREIGN KEY'
                AND constraint_name LIKE '%module%'
                LIMIT 1
            );
        END IF;

        -- Rename the column
        ALTER TABLE live_sessions RENAME COLUMN module_id TO training_module_id;
        
        -- Clean up orphaned records (records that reference non-existent training modules)
        DELETE FROM live_sessions 
        WHERE training_module_id NOT IN (SELECT id FROM training_modules);
        
        RAISE NOTICE 'Cleaned up orphaned live session records';
        
        -- Add new foreign key constraint to training_modules table
        ALTER TABLE live_sessions 
        ADD CONSTRAINT fk_live_sessions_training_module 
        FOREIGN KEY (training_module_id) 
        REFERENCES training_modules(id) 
        ON DELETE CASCADE;
        
        RAISE NOTICE 'Successfully renamed module_id to training_module_id';
    ELSE
        -- If training_module_id already exists, do nothing
        IF EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'live_sessions' 
            AND column_name = 'training_module_id'
        ) THEN
            RAISE NOTICE 'Column training_module_id already exists, skipping migration';
        ELSE
            -- If neither column exists, create the new column
            ALTER TABLE live_sessions 
            ADD COLUMN training_module_id UUID NOT NULL 
            REFERENCES training_modules(id) ON DELETE CASCADE;
            
            RAISE NOTICE 'Created new training_module_id column';
        END IF;
    END IF;
END $$;

-- Step 2: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_live_sessions_training_module 
ON live_sessions(training_module_id);

-- Step 3: Verify the migration
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'live_sessions' 
        AND column_name = 'training_module_id'
    ) THEN
        RAISE NOTICE 'Migration V28 completed successfully: live_sessions now references training_modules';
    ELSE
        RAISE EXCEPTION 'Migration V28 failed: training_module_id column does not exist';
    END IF;
END $$;

