-- V43: Set all access codes to multi-use (not single-use)
-- This fixes the issue where codes were defaulting to single-use and failing after first student

-- Update training module access codes
UPDATE module_access_code
SET is_single_use = false
WHERE is_single_use = true;

-- Update class enrollment codes  
UPDATE class_enrollment_code
SET is_single_use = false
WHERE is_single_use = true;

-- Update course access codes (if they exist)
UPDATE course_access_code
SET is_single_use = false
WHERE is_single_use = true;

