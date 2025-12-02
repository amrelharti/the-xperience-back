-- PRODUCTION HOTFIX: Fix user deletion foreign key constraints
-- Run this directly on your production database to allow user deletion

BEGIN;

-- Drop existing foreign key constraints that prevent user deletion
ALTER TABLE module_access_code 
    DROP CONSTRAINT IF EXISTS module_access_code_consumed_by_fkey CASCADE;

ALTER TABLE module_access_code 
    DROP CONSTRAINT IF EXISTS module_access_code_generated_by_fkey CASCADE;

ALTER TABLE course_access_codes 
    DROP CONSTRAINT IF EXISTS fk_course_access_codes_consumed_by CASCADE;

ALTER TABLE course_access_codes 
    DROP CONSTRAINT IF EXISTS course_access_codes_generated_by_fkey CASCADE;

ALTER TABLE course_access_codes 
    DROP CONSTRAINT IF EXISTS fk_course_access_codes_generated_by CASCADE;

ALTER TABLE class_enrollment_code 
    DROP CONSTRAINT IF EXISTS class_enrollment_code_consumed_by_fkey CASCADE;

ALTER TABLE class_enrollment_code 
    DROP CONSTRAINT IF EXISTS class_enrollment_code_generated_by_fkey CASCADE;

-- Recreate constraints with ON DELETE SET NULL to allow user deletion
ALTER TABLE module_access_code 
    ADD CONSTRAINT module_access_code_consumed_by_fkey 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE module_access_code 
    ADD CONSTRAINT module_access_code_generated_by_fkey 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE course_access_codes 
    ADD CONSTRAINT fk_course_access_codes_consumed_by 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE course_access_codes 
    ADD CONSTRAINT fk_course_access_codes_generated_by 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE class_enrollment_code 
    ADD CONSTRAINT class_enrollment_code_consumed_by_fkey 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE class_enrollment_code 
    ADD CONSTRAINT class_enrollment_code_generated_by_fkey 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

COMMIT;

-- Verify the changes
SELECT 
    tc.constraint_name, 
    tc.table_name, 
    kcu.column_name,
    rc.delete_rule
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.referential_constraints rc 
    ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND (kcu.column_name = 'consumed_by' OR kcu.column_name = 'generated_by')
    AND tc.table_name IN ('module_access_code', 'course_access_codes', 'class_enrollment_code')
ORDER BY tc.table_name, kcu.column_name;




