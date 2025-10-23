-- V30: Fix training_modules to link to training (modules table) instead of classes
-- This makes more sense: Modules are curriculum content, shared across all classes in a training

-- Drop the old constraint and column linking to classes
ALTER TABLE training_modules DROP CONSTRAINT IF EXISTS training_modules_class_id_fkey;
ALTER TABLE training_modules DROP COLUMN IF EXISTS class_id;

-- Add new column linking to training (modules table)
ALTER TABLE training_modules ADD COLUMN training_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE;

-- Update the unique constraint to be per training, not per class
ALTER TABLE training_modules DROP CONSTRAINT IF EXISTS unique_class_order;
ALTER TABLE training_modules ADD CONSTRAINT unique_training_order UNIQUE (training_id, order_index);

-- Update indexes
DROP INDEX IF EXISTS idx_training_modules_class_id;
CREATE INDEX idx_training_modules_training_id ON training_modules(training_id);
CREATE INDEX idx_training_modules_order ON training_modules(training_id, order_index);

-- Update comments to reflect the new structure
COMMENT ON TABLE modules IS 'TRAININGS - Top-level learning programs (e.g., "Full Stack Development")';
COMMENT ON TABLE classes IS 'CLASSES/PROMOTIONS - Student cohorts that enroll in a training (e.g., "2025-A")';
COMMENT ON TABLE training_modules IS 'MODULES - Curriculum units within a training, shared by all classes (e.g., "Module 1", "Module 2")';
COMMENT ON TABLE courses IS 'COURSES - Individual video lessons within a module';
COMMENT ON COLUMN training_modules.training_id IS 'Links to the training (modules table) - all classes in this training share these modules';



