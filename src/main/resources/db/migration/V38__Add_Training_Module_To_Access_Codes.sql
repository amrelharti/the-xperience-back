-- Add training_module_id column to module_access_code table to scope access codes to specific training modules

ALTER TABLE module_access_code 
ADD COLUMN training_module_id UUID;

-- Add foreign key constraint
ALTER TABLE module_access_code
ADD CONSTRAINT fk_module_access_code_training_module
FOREIGN KEY (training_module_id) REFERENCES training_modules(id);

-- Add index for performance
CREATE INDEX idx_module_access_code_training_module ON module_access_code(training_module_id);

-- Add index for the query by training module, code hash, and consumed status
CREATE INDEX idx_module_access_code_training_module_code_consumed 
ON module_access_code(training_module_id, code_hash, consumed_at);

-- Add consumed_by_class_id to track which class the user was in when consuming the code
-- This enables class-specific module access (unlocks only work for that specific class)
ALTER TABLE module_access_code
ADD COLUMN consumed_by_class_id UUID;

-- Add index for class-specific access checking
CREATE INDEX idx_module_access_code_class ON module_access_code(consumed_by_class_id);