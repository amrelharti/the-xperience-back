-- Migration to handle existing codes without plain_code
-- Since we can't recover the original plain codes from hashes,
-- we'll mark old codes as inactive and require regeneration

-- Deactivate all codes that don't have plain_code stored
UPDATE class_enrollment_code 
SET is_active = false 
WHERE plain_code IS NULL AND is_active = true;

UPDATE module_access_code 
SET is_active = false 
WHERE plain_code IS NULL AND is_active = true;

-- Add comment explaining the change
COMMENT ON COLUMN class_enrollment_code.plain_code IS 'Plain text code for admin viewing. NULL for legacy codes (hashed only).';
COMMENT ON COLUMN module_access_code.plain_code IS 'Plain text code for admin viewing. NULL for legacy codes (hashed only).';




