-- Clear ALL data from the database
-- This migration removes everything to start fresh

-- Delete all data in the correct order (respecting foreign key constraints)

-- Delete all class enrollments first
DELETE FROM class_enrollments;

-- Delete all classes
DELETE FROM classes;

-- Delete all courses
DELETE FROM courses;

-- Delete all modules
DELETE FROM modules;

-- Delete all post likes
DELETE FROM post_likes;

-- Delete all comment likes
DELETE FROM comment_likes;

-- Delete all comments
DELETE FROM comments;

-- Delete all posts
DELETE FROM posts;

-- Delete all users (this will remove all user data)
DELETE FROM users;


