-- ============================================
-- Roulette Winners Table
-- Version 60: Store daily roulette winners (2 per day limit)
-- ============================================

CREATE TABLE IF NOT EXISTS roulette_winners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prize VARCHAR(255) NOT NULL,
    prize_index INTEGER NOT NULL,
    user_identifier VARCHAR(255) NOT NULL,
    win_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on win_date for fast daily queries
CREATE INDEX idx_roulette_winners_win_date ON roulette_winners(win_date);

-- Create index on user_identifier and win_date for user-specific queries
CREATE INDEX idx_roulette_winners_user_date ON roulette_winners(user_identifier, win_date);

-- Add comment
COMMENT ON TABLE roulette_winners IS 'Stores daily roulette winners (max 2 per day)';

