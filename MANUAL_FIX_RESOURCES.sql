-- Manual Fix for RESOURCES Post Type
-- Run these commands directly in your PostgreSQL database

-- Step 1: Check current constraint (to see what we have)
SELECT pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'chk_posts_post_type';

-- Step 2: Drop the old constraint
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_post_type;

-- Step 3: Add new constraint with RESOURCES included
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION', 'RESOURCES'));

-- Step 4: Verify the new constraint
SELECT pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'chk_posts_post_type';

-- You should see output like:
-- CHECK ((post_type = ANY (ARRAY['POST'::text, 'ASSIGNMENT'::text, 'WEEKLY_SUMMARY'::text, 'FEEDBACK'::text, 'ANNOUNCEMENT'::text, 'QUESTION'::text, 'RESOURCES'::text])))




