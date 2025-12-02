-- Migration V41: Add Active Status and Usage Tracking
-- Run this SQL script manually in your PostgreSQL database

-- Add is_active column to class_enrollment_code table
ALTER TABLE class_enrollment_code 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add is_active column to module_access_code table
ALTER TABLE module_access_code 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create code usage log table for tracking
CREATE TABLE IF NOT EXISTS code_usage_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_id UUID NOT NULL,
    user_id UUID NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    code_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_code_usage_log_code_id ON code_usage_log(code_id);
CREATE INDEX IF NOT EXISTS idx_code_usage_log_user_id ON code_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_code_usage_log_resource_id ON code_usage_log(resource_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollment_code_active ON class_enrollment_code(class_id, is_active);
CREATE INDEX IF NOT EXISTS idx_module_access_code_active ON module_access_code(training_module_id, is_active);

-- Set all existing codes to active
UPDATE class_enrollment_code SET is_active = TRUE WHERE consumed_at IS NULL;
UPDATE class_enrollment_code SET is_active = FALSE WHERE consumed_at IS NOT NULL;
UPDATE module_access_code SET is_active = TRUE WHERE consumed_at IS NULL;
UPDATE module_access_code SET is_active = FALSE WHERE consumed_at IS NOT NULL;

-- Verify migration
SELECT 'Migration V41 completed successfully!' AS status;




