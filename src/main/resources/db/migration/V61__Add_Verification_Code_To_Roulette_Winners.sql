-- ============================================
-- Add Verification Code to Roulette Winners
-- Version 61: Add verification_code column for one-time codes
-- ============================================

ALTER TABLE roulette_winners 
ADD COLUMN IF NOT EXISTS verification_code VARCHAR(8) UNIQUE;

-- Create index on verification_code for fast lookups
CREATE INDEX IF NOT EXISTS idx_roulette_winners_verification_code ON roulette_winners(verification_code);

-- Add comment
COMMENT ON COLUMN roulette_winners.verification_code IS 'One-time verification code sent to winner via WhatsApp';

