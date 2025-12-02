-- =====================================================
-- MIGRATION: Class-Specific Multi-Use Access Codes
-- Date: 2025-11-06
-- Description: Update access code system to be class-specific
-- =====================================================

-- Run this as a database user with ALTER TABLE permissions

BEGIN;

-- =====================================================
-- 1. Update module_access_code table
-- =====================================================

-- Add class_id column (nullable at first to allow existing data)
ALTER TABLE module_access_code 
ADD COLUMN IF NOT EXISTS class_id UUID;

-- Add plain_code column if missing
ALTER TABLE module_access_code 
ADD COLUMN IF NOT EXISTS plain_code VARCHAR(20);

-- Add is_active column if missing
ALTER TABLE module_access_code 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Add foreign key constraint to classes table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_module_access_code_class'
    ) THEN
        ALTER TABLE module_access_code 
        ADD CONSTRAINT fk_module_access_code_class 
        FOREIGN KEY (class_id) REFERENCES classes(id);
    END IF;
END $$;

-- Remove old single-use columns (optional - comment out if you want to keep data)
-- ALTER TABLE module_access_code DROP COLUMN IF EXISTS consumed_at;
-- ALTER TABLE module_access_code DROP COLUMN IF EXISTS consumed_by;
-- ALTER TABLE module_access_code DROP COLUMN IF EXISTS consumed_by_class_id;
-- ALTER TABLE module_access_code DROP COLUMN IF EXISTS is_single_use;

-- =====================================================
-- 2. Update class_enrollment_code table
-- =====================================================

-- Add plain_code column if missing
ALTER TABLE class_enrollment_code 
ADD COLUMN IF NOT EXISTS plain_code VARCHAR(20);

-- Add is_active column if missing  
ALTER TABLE class_enrollment_code 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- =====================================================
-- 3. Update users table (if columns are missing)
-- =====================================================

-- Add instagram column if missing
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS instagram VARCHAR(255);

-- Add linkedin column if missing
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS linkedin VARCHAR(500);

-- Add title column if missing
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS title VARCHAR(255);

-- =====================================================
-- 4. Clean up old access codes (OPTIONAL)
-- =====================================================

-- Deactivate all old codes without class_id
-- UPDATE module_access_code SET is_active = FALSE WHERE class_id IS NULL;

-- =====================================================
-- 5. Verify the changes
-- =====================================================

-- Check module_access_code table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'module_access_code' 
ORDER BY ordinal_position;

-- Check class_enrollment_code table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'class_enrollment_code' 
ORDER BY ordinal_position;

-- Check users table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;

COMMIT;

-- =====================================================
-- NOTES:
-- =====================================================
-- 1. Run this script with a database admin user
-- 2. After running, restart your Spring Boot application
-- 3. The class_id column is nullable to allow migration of existing data
-- 4. You may want to make class_id NOT NULL after migrating existing codes
-- 5. Old codes without class_id will be inactive and not usable
-- =====================================================

