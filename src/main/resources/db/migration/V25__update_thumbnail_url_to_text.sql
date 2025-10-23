-- Update thumbnail_url column to TEXT to support data URLs
ALTER TABLE modules ALTER COLUMN thumbnail_url TYPE TEXT;

-- Update image_url column to TEXT to support data URLs
ALTER TABLE users ALTER COLUMN image_url TYPE TEXT;

-- Update video_url column to TEXT to support longer URLs
ALTER TABLE courses ALTER COLUMN video_url TYPE TEXT;
