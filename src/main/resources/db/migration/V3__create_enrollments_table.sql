-- Check if enrollments table exists and has the correct structure
DO $$
BEGIN
    -- Add enrolled_at column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'enrollments' AND column_name = 'enrolled_at'
    ) THEN
        ALTER TABLE enrollments ADD COLUMN enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_enrolled_at ON enrollments(enrolled_at);

-- Insert some sample enrollments for the admin user (only if not already enrolled)
INSERT INTO enrollments (user_id, course_id, enrolled_at)
SELECT 
    u.id,
    c.id,
    CURRENT_TIMESTAMP
FROM users u
CROSS JOIN courses c
WHERE u.email = 'admin@thexperience.com' 
  AND c.is_published = true
  AND NOT EXISTS (
    SELECT 1 FROM enrollments e 
    WHERE e.user_id = u.id AND e.course_id = c.id
  );
