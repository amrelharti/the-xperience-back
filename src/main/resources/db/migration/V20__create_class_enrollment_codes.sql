-- Create class enrollment codes table
CREATE TABLE class_enrollment_code (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    consumed_at TIMESTAMP WITH TIME ZONE,
    generated_by UUID REFERENCES users(id),
    consumed_by UUID REFERENCES users(id),
    is_single_use BOOLEAN NOT NULL DEFAULT true
);

-- Create indexes for better performance
CREATE INDEX idx_class_enrollment_code_class_id ON class_enrollment_code(class_id);
CREATE INDEX idx_class_enrollment_code_hash ON class_enrollment_code(code_hash);
CREATE INDEX idx_class_enrollment_code_consumed ON class_enrollment_code(consumed_at);


