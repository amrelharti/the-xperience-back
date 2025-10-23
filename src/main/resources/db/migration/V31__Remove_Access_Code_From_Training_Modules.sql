-- Remove access_code column from training_modules table
-- This migration removes the access code functionality completely

-- Drop the access_code column from training_modules table
ALTER TABLE training_modules DROP COLUMN IF EXISTS access_code;


