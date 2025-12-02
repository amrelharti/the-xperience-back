-- =====================================================
-- MIGRATION: Lock/Unlock System for Training Modules
-- Date: 2025-11-07
-- Description: Replace access codes with simple lock/unlock toggles per class
-- =====================================================

-- Run this as database owner (postgres)

BEGIN;

-- =====================================================
-- 1. Create new training_module_class_access table
-- =====================================================

CREATE TABLE IF NOT EXISTS training_module_class_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_module_id UUID NOT NULL REFERENCES training_modules(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),
    CONSTRAINT uk_tm_class UNIQUE (training_module_id, class_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_tm_class_access_module ON training_module_class_access(training_module_id);
CREATE INDEX IF NOT EXISTS idx_tm_class_access_class ON training_module_class_access(class_id);

-- =====================================================
-- 2. Optional: Migrate existing access code data
-- =====================================================

-- If you want to auto-unlock modules where students have used codes:
-- INSERT INTO training_module_class_access (training_module_id, class_id, is_unlocked, updated_at)
-- SELECT DISTINCT 
--     c.training_module_id, 
--     c.class_id, 
--     TRUE,
--     CURRENT_TIMESTAMP
-- FROM code_usage_log ul
-- JOIN module_access_code c ON ul.code_id = c.id
-- WHERE ul.code_type = 'TRAINING_MODULE'
--   AND c.training_module_id IS NOT NULL
--   AND c.class_id IS NOT NULL
-- ON CONFLICT (training_module_id, class_id) DO NOTHING;

-- =====================================================
-- 3. Verify
-- =====================================================

SELECT 
    'training_module_class_access' as table_name,
    count(*) as row_count
FROM training_module_class_access;

COMMIT;

-- =====================================================
-- NOTES:
-- =====================================================
-- 1. Run this script with postgres user
-- 2. The module_access_code table is no longer used for training modules
-- 3. Access is now controlled by lock/unlock toggles per class
-- 4. Default is LOCKED (is_unlocked = FALSE)
-- 5. Admin can toggle access on/off for each class
-- =====================================================




