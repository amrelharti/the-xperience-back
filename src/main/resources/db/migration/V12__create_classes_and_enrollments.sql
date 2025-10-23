-- Create classes table
CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    max_students INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create class_enrollments table
CREATE TABLE class_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(student_id, class_id)
);

-- Add class_id column to posts table
ALTER TABLE posts ADD COLUMN class_id UUID REFERENCES classes(id) ON DELETE SET NULL;

-- Create indexes for better performance
CREATE INDEX idx_classes_module_id ON classes(module_id);
CREATE INDEX idx_classes_created_by ON classes(created_by);
CREATE INDEX idx_classes_start_date ON classes(start_date);
CREATE INDEX idx_classes_is_active ON classes(is_active);
CREATE INDEX idx_classes_is_archived ON classes(is_archived);

CREATE INDEX idx_class_enrollments_student_id ON class_enrollments(student_id);
CREATE INDEX idx_class_enrollments_class_id ON class_enrollments(class_id);
CREATE INDEX idx_class_enrollments_is_active ON class_enrollments(is_active);

CREATE INDEX idx_posts_class_id ON posts(class_id);


