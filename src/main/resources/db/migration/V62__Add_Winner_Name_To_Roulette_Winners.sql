-- ============================================
-- Add Winner Name to Roulette Winners
-- Version 62: Add winner_name column
-- ============================================

ALTER TABLE roulette_winners 
ADD COLUMN IF NOT EXISTS winner_name VARCHAR(255);

-- Add comment
COMMENT ON COLUMN roulette_winners.winner_name IS 'Name of the roulette winner';

