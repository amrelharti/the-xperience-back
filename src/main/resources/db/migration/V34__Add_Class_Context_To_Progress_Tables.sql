-- Add class_id column to course_progress table
ALTER TABLE course_progress ADD COLUMN class_id UUID;

-- Add class_id column to live_session_attendance table  
ALTER TABLE live_session_attendance ADD COLUMN class_id UUID;

-- Add foreign key constraints
ALTER TABLE course_progress ADD CONSTRAINT fk_course_progress_class 
    FOREIGN KEY (class_id) REFERENCES classes(id);

ALTER TABLE live_session_attendance ADD CONSTRAINT fk_live_session_attendance_class 
    FOREIGN KEY (class_id) REFERENCES classes(id);

-- Update existing records to have class_id (set to first available class for each user)
-- This is a temporary migration - in production, you'd want to handle this more carefully
UPDATE course_progress 
SET class_id = (
    SELECT ce.class_id 
    FROM class_enrollments ce 
    WHERE ce.student_id = course_progress.user_id 
    LIMIT 1
)
WHERE class_id IS NULL;

UPDATE live_session_attendance 
SET class_id = (
    SELECT ce.class_id 
    FROM class_enrollments ce 
    WHERE ce.student_id = live_session_attendance.user_id 
    LIMIT 1
)
WHERE class_id IS NULL;

-- Make class_id NOT NULL after populating existing records
ALTER TABLE course_progress ALTER COLUMN class_id SET NOT NULL;
ALTER TABLE live_session_attendance ALTER COLUMN class_id SET NOT NULL;

-- Drop old unique constraints and create new ones that include class_id
ALTER TABLE course_progress DROP CONSTRAINT IF EXISTS uk_course_progress_user_course;
ALTER TABLE course_progress ADD CONSTRAINT uk_course_progress_user_course_class 
    UNIQUE (user_id, course_id, class_id);

ALTER TABLE live_session_attendance DROP CONSTRAINT IF EXISTS uk_live_session_attendance_session_user;
ALTER TABLE live_session_attendance ADD CONSTRAINT uk_live_session_attendance_session_user_class 
    UNIQUE (session_id, user_id, class_id);
