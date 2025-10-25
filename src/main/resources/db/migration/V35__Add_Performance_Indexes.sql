-- Add performance indexes for frequently queried columns

-- Indexes for posts table (most frequently queried)
CREATE INDEX IF NOT EXISTS idx_posts_class_created ON posts(class_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_module ON posts(module_id);
CREATE INDEX IF NOT EXISTS idx_posts_type ON posts(post_type);
CREATE INDEX IF NOT EXISTS idx_posts_hidden ON posts(is_hidden);
CREATE INDEX IF NOT EXISTS idx_posts_pinned ON posts(is_pinned DESC, created_at DESC);

-- Indexes for course_progress table
CREATE INDEX IF NOT EXISTS idx_course_progress_user_class ON course_progress(user_id, class_id);
CREATE INDEX IF NOT EXISTS idx_course_progress_course ON course_progress(course_id);
CREATE INDEX IF NOT EXISTS idx_course_progress_status ON course_progress(status);

-- Indexes for live_session_attendance table
CREATE INDEX IF NOT EXISTS idx_live_session_attendance_user_class ON live_session_attendance(user_id, class_id);
CREATE INDEX IF NOT EXISTS idx_live_session_attendance_session ON live_session_attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_live_session_attendance_attended ON live_session_attendance(attended_at DESC);

-- Indexes for classes table
CREATE INDEX IF NOT EXISTS idx_classes_module ON classes(module_id);
CREATE INDEX IF NOT EXISTS idx_classes_active ON classes(is_active);
CREATE INDEX IF NOT EXISTS idx_classes_archived ON classes(is_archived);
CREATE INDEX IF NOT EXISTS idx_classes_dates ON classes(start_date, end_date);

-- Indexes for class_enrollments table
CREATE INDEX IF NOT EXISTS idx_class_enrollments_student ON class_enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollments_class ON class_enrollments(class_id);
CREATE INDEX IF NOT EXISTS idx_class_enrollments_enrolled ON class_enrollments(enrolled_at DESC);

-- Indexes for courses table
CREATE INDEX IF NOT EXISTS idx_courses_training_module ON courses(training_module_id);
CREATE INDEX IF NOT EXISTS idx_courses_order ON courses(order_index);

-- Indexes for training_modules table
CREATE INDEX IF NOT EXISTS idx_training_modules_training ON training_modules(training_id);
CREATE INDEX IF NOT EXISTS idx_training_modules_order ON training_modules(order_index);

-- Indexes for live_sessions table
CREATE INDEX IF NOT EXISTS idx_live_sessions_training_module ON live_sessions(training_module_id);
CREATE INDEX IF NOT EXISTS idx_live_sessions_starts ON live_sessions(starts_at);
CREATE INDEX IF NOT EXISTS idx_live_sessions_ends ON live_sessions(ends_at);

-- Indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Indexes for comments table
CREATE INDEX IF NOT EXISTS idx_comments_post ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_created ON comments(created_at DESC);

-- Indexes for post_likes table
CREATE INDEX IF NOT EXISTS idx_post_likes_post ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_user ON post_likes(user_id);

-- Note: assignments and assignment_submissions tables don't exist yet
-- These indexes will be added when those tables are created
