-- Fix V32 migration failure by removing it from Flyway history
-- This allows the corrected migration to run again

-- Delete the failed V32 migration from Flyway schema history
DELETE FROM flyway_schema_history WHERE version = '32';

-- Verify deletion
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;



