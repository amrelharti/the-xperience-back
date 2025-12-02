-- Step 1: Check what columns exist
SELECT 
    'class_enrollment_code' as table_name,
    column_name,
    data_type
FROM information_schema.columns 
WHERE table_name = 'class_enrollment_code' 
  AND column_name IN ('is_active', 'is_single_use')
UNION ALL
SELECT 
    'module_access_code' as table_name,
    column_name,
    data_type
FROM information_schema.columns 
WHERE table_name = 'module_access_code' 
  AND column_name IN ('is_active', 'is_single_use');

-- Step 2: Check if code_usage_log table exists
SELECT EXISTS (
   SELECT FROM information_schema.tables 
   WHERE table_name = 'code_usage_log'
) as code_usage_log_exists;

-- Step 3: Add missing columns (run these one by one if they fail)

-- Add is_active to class_enrollment_code (if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'class_enrollment_code' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE class_enrollment_code 
        ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
        RAISE NOTICE 'Added is_active to class_enrollment_code';
    ELSE
        RAISE NOTICE 'is_active already exists in class_enrollment_code';
    END IF;
END $$;

-- Add is_active to module_access_code (if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'module_access_code' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE module_access_code 
        ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
        RAISE NOTICE 'Added is_active to module_access_code';
    ELSE
        RAISE NOTICE 'is_active already exists in module_access_code';
    END IF;
END $$;

-- Step 4: Create indexes (only if they don't exist)
CREATE INDEX IF NOT EXISTS idx_code_usage_log_code_id ON code_usage_log(code_id);
CREATE INDEX IF NOT EXISTS idx_code_usage_log_user_id ON code_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_code_usage_log_resource_id ON code_usage_log(resource_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollment_code_active ON class_enrollment_code(class_id, is_active);
CREATE INDEX IF NOT EXISTS idx_module_access_code_active ON module_access_code(training_module_id, is_active);

-- Step 5: Set initial values for existing codes
UPDATE class_enrollment_code SET is_active = TRUE WHERE consumed_at IS NULL AND is_active IS NULL;
UPDATE class_enrollment_code SET is_active = FALSE WHERE consumed_at IS NOT NULL AND is_active IS NULL;
UPDATE module_access_code SET is_active = TRUE WHERE consumed_at IS NULL AND is_active IS NULL;
UPDATE module_access_code SET is_active = FALSE WHERE consumed_at IS NOT NULL AND is_active IS NULL;

-- Step 6: Verify everything is set up
SELECT 'Migration Complete!' as status;
SELECT 
    'class_enrollment_code has is_active: ' || 
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='class_enrollment_code' AND column_name='is_active')::text as check1;
SELECT 
    'module_access_code has is_active: ' || 
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='module_access_code' AND column_name='is_active')::text as check2;
SELECT 
    'code_usage_log exists: ' || 
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name='code_usage_log')::text as check3;




