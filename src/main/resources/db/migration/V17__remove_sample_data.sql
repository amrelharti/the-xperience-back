-- Remove all sample/mock data from the database
-- This migration cleans up test data that was previously inserted

-- Delete all sample classes first (due to foreign key constraints)
DELETE FROM classes WHERE name LIKE '%Class%' OR name LIKE '%sample%' OR name LIKE '%test%';

-- Delete all sample courses
DELETE FROM courses WHERE title LIKE '%Sample%' OR title LIKE '%sample%' OR title LIKE '%test%' 
    OR description LIKE '%sample%' OR description LIKE '%test%';

-- Delete all sample modules  
DELETE FROM modules WHERE title LIKE '%Sample%' OR title LIKE '%sample%' OR title LIKE '%test%'
    OR description LIKE '%sample%' OR description LIKE '%test%';

-- Delete all sample posts
DELETE FROM posts WHERE title LIKE '%Sample%' OR title LIKE '%sample%' OR title LIKE '%test%' 
    OR content LIKE '%sample%' OR content LIKE '%test%';

-- Delete all sample comments
DELETE FROM comments WHERE content LIKE '%Sample%' OR content LIKE '%sample%' OR content LIKE '%test%';

-- Delete any test users (keep admin users)
DELETE FROM users WHERE email LIKE '%test%' OR email LIKE '%sample%' 
    OR (name LIKE '%test%' AND role != 'ADMIN') 
    OR (name LIKE '%sample%' AND role != 'ADMIN');

