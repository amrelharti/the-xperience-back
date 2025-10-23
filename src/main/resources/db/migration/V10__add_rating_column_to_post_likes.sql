-- Add rating column to post_likes table
ALTER TABLE post_likes ADD COLUMN IF NOT EXISTS rating INTEGER;

-- Update existing records to have a default rating of 5 (for likes without ratings)
UPDATE post_likes SET rating = 5 WHERE rating IS NULL;

