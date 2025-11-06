-- Fix foreign key constraints for user deletion
-- Allow user deletion by setting consumed_by and generated_by to NULL when a user is deleted

-- Drop existing foreign key constraints if they exist
DO $$
BEGIN
    -- Drop module_access_code constraints
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'module_access_code_consumed_by_fkey') THEN
        ALTER TABLE module_access_code DROP CONSTRAINT module_access_code_consumed_by_fkey;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'module_access_code_generated_by_fkey') THEN
        ALTER TABLE module_access_code DROP CONSTRAINT module_access_code_generated_by_fkey;
    END IF;

    -- Drop course_access_codes constraints if they don't have ON DELETE SET NULL
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'fk_course_access_codes_consumed_by') THEN
        ALTER TABLE course_access_codes DROP CONSTRAINT fk_course_access_codes_consumed_by;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'course_access_codes_generated_by_fkey') THEN
        ALTER TABLE course_access_codes DROP CONSTRAINT course_access_codes_generated_by_fkey;
    END IF;

    -- Drop class_enrollment_code constraints
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'class_enrollment_code_consumed_by_fkey') THEN
        ALTER TABLE class_enrollment_code DROP CONSTRAINT class_enrollment_code_consumed_by_fkey;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'class_enrollment_code_generated_by_fkey') THEN
        ALTER TABLE class_enrollment_code DROP CONSTRAINT class_enrollment_code_generated_by_fkey;
    END IF;
END $$;

-- Recreate constraints with ON DELETE SET NULL

-- module_access_code
ALTER TABLE module_access_code 
    ADD CONSTRAINT module_access_code_consumed_by_fkey 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE module_access_code 
    ADD CONSTRAINT module_access_code_generated_by_fkey 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

-- course_access_codes
ALTER TABLE course_access_codes 
    ADD CONSTRAINT fk_course_access_codes_consumed_by 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE course_access_codes 
    ADD CONSTRAINT fk_course_access_codes_generated_by 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

-- class_enrollment_code
ALTER TABLE class_enrollment_code 
    ADD CONSTRAINT class_enrollment_code_consumed_by_fkey 
    FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE class_enrollment_code 
    ADD CONSTRAINT class_enrollment_code_generated_by_fkey 
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL;

