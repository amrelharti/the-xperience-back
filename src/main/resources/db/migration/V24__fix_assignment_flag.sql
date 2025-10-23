-- Fix the assignment flag for the post that has a rating but isn't marked as assignment
UPDATE posts 
SET is_assignment = true 
WHERE id = '01ff8acb-40b5-4608-84cb-d83bfdf0afdf' 
AND avg_score IS NOT NULL 
AND score_count > 0;
