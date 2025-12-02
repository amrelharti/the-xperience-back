-- ======================================================================
-- FIX DUPLICATE CHECK CONSTRAINT - Remove old posts_type_check constraint
-- ======================================================================
-- Problem: There are TWO check constraints on the posts table:
--   1. posts_type_check (OLD - blocks WEEKLY_SUMMARY and FEEDBACK)
--   2. chk_posts_post_type (NEW - allows all post types)
--
-- The old constraint is still active and blocking post creation!
-- ======================================================================

-- Drop the OLD constraint that's blocking WEEKLY_SUMMARY and FEEDBACK
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_type_check;

-- Verify the constraints after the fix
SELECT 
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conrelid = 'posts'::regclass 
  AND contype = 'c'
ORDER BY conname;

-- Show a success message
SELECT '✅ Old constraint removed! Now testing with a WEEKLY_SUMMARY post type validation...' as status;

-- Verify that post_type column accepts new values (this is just a query, won't insert)
SELECT 
    CASE 
        WHEN 'WEEKLY_SUMMARY' = ANY(
            SELECT unnest(string_to_array(
                replace(replace(regexp_replace(pg_get_constraintdef(oid), '.*\(', ''), ')', ''), '''', ''),
                ', '
            ))
            FROM pg_constraint
            WHERE conrelid = 'posts'::regclass 
              AND contype = 'c'
              AND conname LIKE '%post%type%'
        ) 
        THEN '✅ WEEKLY_SUMMARY is now allowed'
        ELSE '❌ WEEKLY_SUMMARY is still blocked'
    END as validation_check;

SELECT '🎉 Database fix complete! Try creating a WEEKLY_SUMMARY or FEEDBACK post now.' as final_status;

