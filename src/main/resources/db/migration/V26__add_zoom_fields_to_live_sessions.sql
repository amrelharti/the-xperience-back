-- V26: Add Zoom meeting fields to live_sessions table

-- Add Zoom meeting URL (text for long URLs)
ALTER TABLE live_sessions ADD COLUMN IF NOT EXISTS meeting_url TEXT;

-- Add optional Zoom meeting password
ALTER TABLE live_sessions ADD COLUMN IF NOT EXISTS meeting_password VARCHAR(255);

-- Add session duration in minutes (defaults to 60)
ALTER TABLE live_sessions ADD COLUMN IF NOT EXISTS duration_minutes INT DEFAULT 60;

-- Add instructor name
ALTER TABLE live_sessions ADD COLUMN IF NOT EXISTS instructor_name VARCHAR(255);

-- Create index on meeting_url for faster lookups
CREATE INDEX IF NOT EXISTS idx_live_session_meeting_url ON live_sessions(meeting_url);

COMMENT ON COLUMN live_sessions.meeting_url IS 'Zoom or other video conferencing meeting URL';
COMMENT ON COLUMN live_sessions.meeting_password IS 'Optional meeting password for security';
COMMENT ON COLUMN live_sessions.duration_minutes IS 'Planned duration of the live session in minutes';
COMMENT ON COLUMN live_sessions.instructor_name IS 'Name of the instructor leading the session';



