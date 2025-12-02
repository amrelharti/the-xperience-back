-- Add RESOURCES post type to the database constraint
-- This migration allows admins to create RESOURCES posts for sharing links, PDFs, and images
-- 
-- Previous constraint allowed: POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT, QUESTION
-- New constraint allows:         POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT, QUESTION, RESOURCES

-- Drop the old constraint
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_post_type;

-- Recreate the constraint with RESOURCES post type included
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION', 'RESOURCES'));

