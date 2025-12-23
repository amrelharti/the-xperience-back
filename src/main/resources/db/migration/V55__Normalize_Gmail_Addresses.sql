-- ============================================
-- Normalize Gmail Addresses (Remove Dots)
-- Version 55: Fix Gmail dot equivalence issue
-- ============================================

-- Gmail treats dots in the local part as equivalent
-- e.g., youssef.aajiba@gmail.com = youssefaajiba@gmail.com
-- This migration normalizes Gmail addresses by removing dots from local part

-- Update Gmail addresses: remove dots from local part
UPDATE users 
SET email = REPLACE(LOWER(SPLIT_PART(email, '@', 1)), '.', '') || '@' || LOWER(SPLIT_PART(email, '@', 2))
WHERE LOWER(SPLIT_PART(email, '@', 2)) IN ('gmail.com', 'googlemail.com')
  AND SPLIT_PART(email, '@', 1) LIKE '%.%';

-- The above query:
-- 1. Finds emails ending with @gmail.com or @googlemail.com
-- 2. That have dots in the local part (before @)
-- 3. Removes all dots from the local part using REPLACE()
-- Example: youssef.aajiba@gmail.com -> youssefaajiba@gmail.com

-- Note: This will update the email but keep the same user record
-- The email_lowercase_check constraint will still be satisfied

