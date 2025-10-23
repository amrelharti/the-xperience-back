-- Add missing columns to existing tables to match the new entity structure

-- Add missing columns to courses table
ALTER TABLE courses ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS duration_seconds INTEGER NOT NULL DEFAULT 0;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS order_index INTEGER NOT NULL DEFAULT 0;

-- Add missing columns to modules table  
ALTER TABLE modules ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);
ALTER TABLE modules ADD COLUMN IF NOT EXISTS is_published BOOLEAN NOT NULL DEFAULT false;

-- Update existing records to have default values
UPDATE courses SET duration_seconds = 0 WHERE duration_seconds IS NULL;
UPDATE courses SET order_index = 0 WHERE order_index IS NULL;
UPDATE modules SET is_published = false WHERE is_published IS NULL;


