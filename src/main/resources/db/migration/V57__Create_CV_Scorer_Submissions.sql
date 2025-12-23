-- ============================================
-- CV Scorer Submissions Table
-- Version 57: Store contact information from non-authenticated CV scorer users
-- ============================================

CREATE TABLE IF NOT EXISTS cv_scorer_submission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    final_score INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX idx_cv_scorer_submission_email ON cv_scorer_submission(email);
CREATE INDEX idx_cv_scorer_submission_created_at ON cv_scorer_submission(created_at);

-- Add comment
COMMENT ON TABLE cv_scorer_submission IS 'Stores contact information from non-authenticated users who used CV scorer';

