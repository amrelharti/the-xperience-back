-- Add plain_code column to store the actual code (encrypted or plain)
-- This allows admins to view the code after generation

ALTER TABLE class_enrollment_code 
ADD COLUMN IF NOT EXISTS plain_code VARCHAR(20);

ALTER TABLE module_access_code 
ADD COLUMN IF NOT EXISTS plain_code VARCHAR(20);

-- Note: Existing codes won't have plain_code (will be NULL)
-- Only newly generated codes will have this field populated

