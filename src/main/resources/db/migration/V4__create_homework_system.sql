-- Add roles to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'STUDENT' CHECK (role IN ('STUDENT', 'PROFESSOR', 'ADMIN'));

-- Create homeworks table
CREATE TABLE IF NOT EXISTS homeworks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    attachments TEXT,
    avg_score DECIMAL(3,2) DEFAULT 0.0,
    score_count INTEGER DEFAULT 0,
    is_hidden BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create homework_scores table
CREATE TABLE IF NOT EXISTS homework_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id UUID NOT NULL REFERENCES homeworks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value INTEGER NOT NULL CHECK (value >= 0 AND value <= 10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(homework_id, user_id)
);

-- Create live_sessions table
CREATE TABLE IF NOT EXISTS live_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create live_session_attendance table
CREATE TABLE IF NOT EXISTS live_session_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES live_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(session_id, user_id)
);

-- Update module_progress table to support courses + sessions
ALTER TABLE module_progress ADD COLUMN IF NOT EXISTS courses_done INTEGER DEFAULT 0;
ALTER TABLE module_progress ADD COLUMN IF NOT EXISTS sessions_done INTEGER DEFAULT 0;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_homeworks_author_id ON homeworks(author_id);
CREATE INDEX IF NOT EXISTS idx_homeworks_module_id ON homeworks(module_id);
CREATE INDEX IF NOT EXISTS idx_homeworks_created_at ON homeworks(created_at);
CREATE INDEX IF NOT EXISTS idx_homeworks_avg_score ON homeworks(avg_score);

CREATE INDEX IF NOT EXISTS idx_homework_scores_homework_id ON homework_scores(homework_id);
CREATE INDEX IF NOT EXISTS idx_homework_scores_user_id ON homework_scores(user_id);

CREATE INDEX IF NOT EXISTS idx_live_sessions_module_id ON live_sessions(module_id);
CREATE INDEX IF NOT EXISTS idx_live_sessions_starts_at ON live_sessions(starts_at);

CREATE INDEX IF NOT EXISTS idx_live_session_attendance_session_id ON live_session_attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_live_session_attendance_user_id ON live_session_attendance(user_id);

-- Update existing users to have proper roles
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@thexperience.com';
UPDATE users SET role = 'STUDENT' WHERE role IS NULL OR role = '';

-- Insert some sample live sessions
INSERT INTO live_sessions (module_id, title, description, starts_at, ends_at, location)
SELECT 
    m.id,
    'Live Session: ' || m.title,
    'Interactive live session for ' || m.title,
    CURRENT_TIMESTAMP + INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '1 day' + INTERVAL '2 hours',
    'Room 101'
FROM modules m
WHERE m.title LIKE '%Database%'
LIMIT 2;

-- Insert some sample homeworks
INSERT INTO homeworks (author_id, module_id, title, body, avg_score, score_count)
SELECT 
    u.id,
    m.id,
    'Sample Homework: ' || m.title,
    'This is a sample homework submission for ' || m.title || '. Please review and provide feedback.',
    0.0,
    0
FROM users u
CROSS JOIN modules m
WHERE u.role = 'STUDENT'
LIMIT 3;
