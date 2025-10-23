-- Rename type column to post_type in posts table
ALTER TABLE posts RENAME COLUMN type TO post_type;

-- Add missing columns to posts table
ALTER TABLE posts ADD COLUMN IF NOT EXISTS like_count INTEGER DEFAULT 0;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS comment_count INTEGER DEFAULT 0;

-- Add missing columns to comments table
ALTER TABLE comments ADD COLUMN IF NOT EXISTS like_count INTEGER DEFAULT 0;

-- Update the check constraint to use the new column name
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_type;
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'QUESTION', 'ANNOUNCEMENT'));
