-- Create course_access_codes table
CREATE TABLE course_access_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMP,
    generated_by UUID NOT NULL,
    consumed_by UUID,
    is_single_use BOOLEAN NOT NULL DEFAULT true,
    
    CONSTRAINT fk_course_access_codes_course 
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_access_codes_generated_by 
        FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_access_codes_consumed_by 
        FOREIGN KEY (consumed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create index for faster code lookups
CREATE INDEX idx_course_access_codes_hash ON course_access_codes(code_hash);
CREATE INDEX idx_course_access_codes_course ON course_access_codes(course_id);
CREATE INDEX idx_course_access_codes_consumed_by ON course_access_codes(consumed_by);

