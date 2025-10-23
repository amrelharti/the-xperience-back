-- Fix avg_score column precision to allow values up to 10.00
ALTER TABLE posts ALTER COLUMN avg_score TYPE NUMERIC(4,2);

