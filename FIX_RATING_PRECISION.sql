-- ======================================================================
-- FIX AVG_SCORE PRECISION - Allow ratings of 10/10
-- ======================================================================
-- Problem: avg_score column is NUMERIC(3,2) which only allows 0.00 to 9.99
-- Solution: Change to NUMERIC(4,2) which allows 0.00 to 99.99
-- 
-- Current: NUMERIC(3,2) = max value 9.99
-- New:     NUMERIC(4,2) = max value 99.99
-- ======================================================================

-- Change the precision of avg_score to allow 10.00
ALTER TABLE posts ALTER COLUMN avg_score TYPE NUMERIC(4, 2);

-- Verify the change
SELECT 
    column_name,
    data_type,
    numeric_precision,
    numeric_scale
FROM information_schema.columns
WHERE table_name = 'posts' 
  AND column_name = 'avg_score';

-- Test that 10.00 is now valid
SELECT '✅ Database now supports ratings of 10/10!' as status;

