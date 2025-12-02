-- Migration V53: Update live_sessions to support per-class live sessions
-- This migration:
-- 1. Adds class_id column and foreign key (if not exists)
-- 2. Adds is_archived column
-- 3. Removes UNIQUE constraint on training_module_id
-- 4. Adds UNIQUE constraint on (training_module_id, class_id)

DO $$
DECLARE
    constraint_name_var TEXT;
BEGIN
    -- Step 1: Add class_id column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'live_sessions' 
        AND column_name = 'class_id'
    ) THEN
        ALTER TABLE live_sessions ADD COLUMN class_id UUID;
        
        -- Try to populate class_id from existing data if possible
        -- This is a best-effort attempt - may need manual update for existing data
        UPDATE live_sessions ls
        SET class_id = (
            SELECT c.id 
            FROM classes c
            INNER JOIN training_modules tm ON tm.training_id = c.module_id
            WHERE tm.id = ls.training_module_id
            LIMIT 1
        )
        WHERE ls.class_id IS NULL;
        
        -- Make class_id NOT NULL after populating
        ALTER TABLE live_sessions ALTER COLUMN class_id SET NOT NULL;
        
        RAISE NOTICE 'Added class_id column to live_sessions';
    ELSE
        RAISE NOTICE 'class_id column already exists in live_sessions';
    END IF;
    
    -- Step 2: Add foreign key constraint for class_id if it doesn't exist
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'live_sessions'
        AND constraint_name = 'fk_live_sessions_class'
    ) THEN
        ALTER TABLE live_sessions 
        ADD CONSTRAINT fk_live_sessions_class 
        FOREIGN KEY (class_id) 
        REFERENCES classes(id) 
        ON DELETE CASCADE;
        
        RAISE NOTICE 'Added foreign key constraint for class_id';
    ELSE
        RAISE NOTICE 'Foreign key constraint for class_id already exists';
    END IF;
    
    -- Step 3: Add is_archived column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'live_sessions' 
        AND column_name = 'is_archived'
    ) THEN
        ALTER TABLE live_sessions 
        ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT false;
        
        RAISE NOTICE 'Added is_archived column to live_sessions';
    ELSE
        RAISE NOTICE 'is_archived column already exists in live_sessions';
    END IF;
    
    -- Step 4: Remove UNIQUE constraint on training_module_id if it exists
    SELECT constraint_name INTO constraint_name_var
    FROM information_schema.table_constraints
    WHERE table_name = 'live_sessions'
    AND constraint_type = 'UNIQUE'
    AND (
        constraint_name LIKE '%training_module%'
        OR constraint_name LIKE '%module_id%'
    )
    LIMIT 1;
    
    IF constraint_name_var IS NOT NULL THEN
        EXECUTE 'ALTER TABLE live_sessions DROP CONSTRAINT ' || constraint_name_var;
        RAISE NOTICE 'Dropped UNIQUE constraint: %', constraint_name_var;
    ELSE
        RAISE NOTICE 'No UNIQUE constraint on training_module_id found';
    END IF;
    
    -- Step 5: Add UNIQUE constraint on (training_module_id, class_id)
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'live_sessions'
        AND constraint_name = 'uk_live_sessions_module_class'
    ) THEN
        -- First, handle any duplicate entries (keep the most recent one)
        DELETE FROM live_sessions ls1
        WHERE EXISTS (
            SELECT 1
            FROM live_sessions ls2
            WHERE ls2.training_module_id = ls1.training_module_id
            AND ls2.class_id = ls1.class_id
            AND ls2.id != ls1.id
            AND ls2.created_at < ls1.created_at
        );
        
        ALTER TABLE live_sessions 
        ADD CONSTRAINT uk_live_sessions_module_class 
        UNIQUE (training_module_id, class_id);
        
        RAISE NOTICE 'Added UNIQUE constraint on (training_module_id, class_id)';
    ELSE
        RAISE NOTICE 'UNIQUE constraint on (training_module_id, class_id) already exists';
    END IF;
    
    -- Step 6: Create index for better query performance
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE tablename = 'live_sessions'
        AND indexname = 'idx_live_sessions_class'
    ) THEN
        CREATE INDEX idx_live_sessions_class ON live_sessions(class_id);
        RAISE NOTICE 'Created index on class_id';
    ELSE
        RAISE NOTICE 'Index on class_id already exists';
    END IF;
    
    -- Step 7: Create index for is_archived
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE tablename = 'live_sessions'
        AND indexname = 'idx_live_sessions_archived'
    ) THEN
        CREATE INDEX idx_live_sessions_archived ON live_sessions(is_archived);
        RAISE NOTICE 'Created index on is_archived';
    ELSE
        RAISE NOTICE 'Index on is_archived already exists';
    END IF;
    
    RAISE NOTICE 'Migration V53 completed successfully: live_sessions now supports per-class sessions';
END $$;

