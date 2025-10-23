-- Rollback V27 migration
-- This reverts the database back to the original structure

-- Step 1: Drop new tables created by V27
DROP TABLE IF EXISTS module_access_codes CASCADE;
DROP TABLE IF EXISTS module_access CASCADE;

-- Step 2: Drop new modules table (the one for Week 1, 2, 3...)
DROP TABLE IF EXISTS modules CASCADE;

-- Step 3: Rename trainings back to modules
ALTER TABLE trainings RENAME TO modules;

-- Step 4: Rename training_id back to module_id in classes
ALTER TABLE classes RENAME COLUMN training_id TO module_id;

-- Step 5: Remove module_id from courses if it exists
-- (V27 added this column)
ALTER TABLE courses DROP COLUMN IF EXISTS module_id CASCADE;

-- Step 6: Rename training_id back to module_id in courses
-- (V27 renamed this)
ALTER TABLE courses RENAME COLUMN training_id TO module_id;

-- Step 7: Fix live_sessions table
-- Remove new module_id column if it exists
ALTER TABLE live_sessions DROP COLUMN IF EXISTS module_id CASCADE;

-- Rename training_id back to module_id
ALTER TABLE live_sessions RENAME COLUMN training_id TO module_id;

-- Step 8: Update Flyway schema history to mark V27 as not applied
DELETE FROM flyway_schema_history WHERE version = '27';

-- Done! Database is back to V26 state



