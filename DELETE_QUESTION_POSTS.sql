-- Script to delete all posts with post_type = 'QUESTION'
-- Run this script before deploying the updated backend code
-- This will remove all question-type posts from the database

BEGIN;

-- Delete all likes associated with question posts
DELETE FROM post_likes 
WHERE post_id IN (
    SELECT id FROM posts WHERE post_type = 'QUESTION'
);

-- Delete all comments associated with question posts
DELETE FROM comments 
WHERE post_id IN (
    SELECT id FROM posts WHERE post_type = 'QUESTION'
);

-- Delete all question posts
DELETE FROM posts 
WHERE post_type = 'QUESTION';

COMMIT;

-- Verify deletion
SELECT 
    'Posts with QUESTION type remaining: ' || COUNT(*) as verification
FROM posts 
WHERE post_type = 'QUESTION';

