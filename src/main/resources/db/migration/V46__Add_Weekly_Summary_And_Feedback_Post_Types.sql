-- Update the check constraint to include WEEKLY_SUMMARY and FEEDBACK post types
-- This migration fixes the constraint violation when creating WEEKLY_SUMMARY or FEEDBACK posts
-- 
-- Old constraint allowed: POST, ASSIGNMENT, QUESTION, ANNOUNCEMENT
-- New constraint allows:  POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT, QUESTION (for backward compatibility)

-- Drop the old constraint
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_post_type;

-- Recreate the constraint with all valid post types (including QUESTION for backward compatibility)
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION'));

