-- ======================================================================
-- FIX POST TYPES - Add WEEKLY_SUMMARY and FEEDBACK to database constraint
-- ======================================================================
-- This script fixes the check constraint to allow all post types
-- Run this against your PostgreSQL database to enable WEEKLY_SUMMARY and FEEDBACK posts
--
-- Background: Migration V46 was never applied because Flyway is disabled
-- ======================================================================

-- Drop the old constraint that was blocking WEEKLY_SUMMARY and FEEDBACK
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_post_type;

-- Recreate the constraint with all valid post types
-- Includes QUESTION for backward compatibility (even though it's removed from frontend)
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION'));

-- Verify the constraint was created successfully
SELECT 
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conname = 'chk_posts_post_type';

-- Show all existing post types in the database for verification
SELECT 
    post_type, 
    COUNT(*) as count 
FROM posts 
GROUP BY post_type 
ORDER BY post_type;

SELECT '✅ Database constraint updated successfully! WEEKLY_SUMMARY and FEEDBACK posts are now allowed.' as status;

