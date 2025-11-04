-- Add profile fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS instagram VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS linkedin VARCHAR(500);

-- Create index for better search performance
CREATE INDEX IF NOT EXISTS idx_users_title ON users(title);

