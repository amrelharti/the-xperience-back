-- ============================================
-- Normalize Email Addresses to Lowercase
-- Version 54: Fix case-sensitivity issues with email lookup
-- ============================================

-- Update all existing emails to lowercase
UPDATE users SET email = LOWER(TRIM(email));

-- Add a constraint to ensure future emails are always lowercase
-- This will prevent insertion of mixed-case emails
ALTER TABLE users 
ADD CONSTRAINT email_lowercase_check 
CHECK (email = LOWER(email));

-- Create a case-insensitive index for better performance (optional but recommended)
-- This allows fast lookups regardless of case
DROP INDEX IF EXISTS idx_users_email_lower;
CREATE INDEX idx_users_email_lower ON users (LOWER(email));




