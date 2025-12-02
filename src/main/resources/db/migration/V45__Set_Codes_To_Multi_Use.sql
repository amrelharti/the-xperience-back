-- V45: Set all access codes to multi-use (not single-use)
-- This fixes the issue where codes were defaulting to single-use and failing after first student

-- Update training module access codes
UPDATE module_access_code
SET is_single_use = false
WHERE is_single_use = true AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'module_access_code' AND column_name = 'is_single_use');

-- Update class enrollment codes  
UPDATE class_enrollment_code
SET is_single_use = false
WHERE is_single_use = true AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'class_enrollment_code' AND column_name = 'is_single_use');

-- Update course access codes (handle both singular and plural table names)
DO $$
BEGIN
    -- Try plural version first (course_access_codes)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_access_codes') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_access_codes' AND column_name = 'is_single_use') THEN
            UPDATE course_access_codes SET is_single_use = false WHERE is_single_use = true;
        END IF;
    END IF;
    
    -- Try singular version (course_access_code)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_access_code') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_access_code' AND column_name = 'is_single_use') THEN
            UPDATE course_access_code SET is_single_use = false WHERE is_single_use = true;
        END IF;
    END IF;
END $$;

