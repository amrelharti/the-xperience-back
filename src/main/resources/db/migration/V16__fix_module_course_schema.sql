-- Fix the database schema to match the new Module -> Course hierarchy
-- This migration restructures the existing data to work with the new entity structure

-- First, let's see what we're working with and fix the schema step by step

-- Step 1: Add missing columns to modules table if they don't exist
DO $$ 
BEGIN
    -- Add thumbnail_url column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'modules' AND column_name = 'thumbnail_url') THEN
        ALTER TABLE modules ADD COLUMN thumbnail_url VARCHAR(255);
    END IF;
    
    -- Add is_published column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'modules' AND column_name = 'is_published') THEN
        ALTER TABLE modules ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- Step 2: Add missing columns to courses table if they don't exist
DO $$ 
BEGIN
    -- Add module_id column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'courses' AND column_name = 'module_id') THEN
        ALTER TABLE courses ADD COLUMN module_id UUID;
    END IF;
    
    -- Add video_url column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'courses' AND column_name = 'video_url') THEN
        ALTER TABLE courses ADD COLUMN video_url VARCHAR(255);
    END IF;
    
    -- Add duration_seconds column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'courses' AND column_name = 'duration_seconds') THEN
        ALTER TABLE courses ADD COLUMN duration_seconds INTEGER NOT NULL DEFAULT 0;
    END IF;
    
    -- Add order_index column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'courses' AND column_name = 'order_index') THEN
        ALTER TABLE courses ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;

-- Step 3: Handle the course_id column in modules table
-- If modules table has a course_id column, we need to restructure the data
DO $$ 
BEGIN
    -- Check if course_id column exists in modules table
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'modules' AND column_name = 'course_id') THEN
        
        -- Create a temporary table to store the mapping
        CREATE TEMP TABLE temp_module_course_mapping AS
        SELECT m.id as module_id, m.course_id, c.title as course_title
        FROM modules m
        LEFT JOIN courses c ON m.course_id = c.id;
        
        -- Update courses to reference the correct module
        UPDATE courses 
        SET module_id = temp.module_id
        FROM temp_module_course_mapping temp
        WHERE courses.id = temp.course_id;
        
        -- Drop the course_id column from modules
        ALTER TABLE modules DROP COLUMN IF EXISTS course_id;
        
        -- Drop the temporary table
        DROP TABLE temp_module_course_mapping;
    END IF;
END $$;

-- Step 4: Add foreign key constraint for courses.module_id -> modules.id
DO $$ 
BEGIN
    -- Check if the foreign key constraint already exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_course_module' 
                   AND table_name = 'courses') THEN
        
        -- Add the foreign key constraint
        ALTER TABLE courses 
        ADD CONSTRAINT fk_course_module 
        FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Step 5: Make module_id NOT NULL after ensuring all courses have a module
UPDATE courses SET module_id = (SELECT id FROM modules LIMIT 1) WHERE module_id IS NULL;

-- Now make module_id NOT NULL
ALTER TABLE courses ALTER COLUMN module_id SET NOT NULL;

-- Step 6: Database schema is now ready for real data
-- No sample data will be inserted - the system will start with empty tables
