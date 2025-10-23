-- V28: Rollback V27 changes and restore original structure
-- This migration automatically detects and fixes the database state

-- Step 0: Drop the NEW modules table if it exists (created by V27 for Week 1, 2, 3...)
DO $$
BEGIN
    -- Check if modules table has class_id column (means it's the NEW one from V27)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'modules' AND column_name = 'class_id'
    ) THEN
        DROP TABLE IF EXISTS modules CASCADE;
        RAISE NOTICE 'Dropped new modules table created by V27';
    END IF;
END $$;

-- Step 1: Check if trainings table exists and rename it back to modules
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trainings') THEN
        -- Rename trainings back to modules
        ALTER TABLE trainings RENAME TO modules;
        RAISE NOTICE 'Renamed trainings table back to modules';
    END IF;
END $$;

-- Step 2: Fix classes table - rename training_id back to module_id if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'classes' AND column_name = 'training_id'
    ) THEN
        ALTER TABLE classes RENAME COLUMN training_id TO module_id;
        RAISE NOTICE 'Renamed classes.training_id back to module_id';
    END IF;
END $$;

-- Step 3: (Moved to Step 0 - already done)

-- Step 4: Fix courses table
DO $$
BEGIN
    -- If there's a training_id column, rename it back to module_id
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'courses' AND column_name = 'training_id'
    ) THEN
        -- Drop module_id if it exists (V27 might have added it)
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'courses' AND column_name = 'module_id'
        ) THEN
            ALTER TABLE courses DROP COLUMN module_id CASCADE;
        END IF;
        
        -- Rename training_id back to module_id
        ALTER TABLE courses RENAME COLUMN training_id TO module_id;
        RAISE NOTICE 'Fixed courses table columns';
    END IF;
END $$;

-- Step 5: Fix live_sessions table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'live_sessions' AND column_name = 'training_id'
    ) THEN
        -- Drop module_id if exists
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'live_sessions' AND column_name = 'module_id'
        ) THEN
            ALTER TABLE live_sessions DROP COLUMN module_id CASCADE;
        END IF;
        
        -- Rename training_id back to module_id
        ALTER TABLE live_sessions RENAME COLUMN training_id TO module_id;
        RAISE NOTICE 'Fixed live_sessions table columns';
    END IF;
END $$;

-- Step 6: Drop tables created by V27
DROP TABLE IF EXISTS module_access_codes CASCADE;
DROP TABLE IF EXISTS module_access CASCADE;

-- Step 7: Remove V27 from Flyway history
DELETE FROM flyway_schema_history WHERE version = '27';

-- Done! Database is back to original structure

