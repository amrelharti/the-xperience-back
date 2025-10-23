-- V29: Create proper training_modules table
-- This represents the MODULE (Week 1, Week 2, etc) within a CLASS

CREATE TABLE IF NOT EXISTS training_modules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INTEGER NOT NULL DEFAULT 0,
    access_code VARCHAR(50), -- Each module can have an access code
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_class_order UNIQUE (class_id, order_index)
);

-- Add index for faster queries
CREATE INDEX idx_training_modules_class_id ON training_modules(class_id);
CREATE INDEX idx_training_modules_order ON training_modules(class_id, order_index);

-- Now update courses table to reference training_modules instead of modules (trainings)
-- First, add the new column
ALTER TABLE courses ADD COLUMN IF NOT EXISTS training_module_id UUID REFERENCES training_modules(id) ON DELETE CASCADE;

-- Create index for the new relationship
CREATE INDEX IF NOT EXISTS idx_courses_training_module ON courses(training_module_id);

-- Note: We'll keep module_id for backward compatibility during transition
-- The frontend/backend will need to be updated to use training_module_id instead

-- Add comment to clarify the confusion
COMMENT ON TABLE modules IS 'This represents TRAININGS (top-level programs like "Full Stack Development")';
COMMENT ON TABLE classes IS 'This represents CLASSES/PROMOTIONS (cohorts like "2025-A" within a training)';
COMMENT ON TABLE training_modules IS 'This represents MODULES (Week 1, Week 2, etc within a class)';
COMMENT ON TABLE courses IS 'This represents COURSES (individual video lessons within a module)';



