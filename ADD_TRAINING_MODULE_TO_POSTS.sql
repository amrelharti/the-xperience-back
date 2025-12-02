-- Migration: Add training_module_id column to posts table
-- This allows posts to be associated with specific training modules (weeks/modules within a training)

-- Add the training_module_id column
ALTER TABLE posts 
ADD COLUMN IF NOT EXISTS training_module_id UUID;

-- Add foreign key constraint
ALTER TABLE posts 
ADD CONSTRAINT fk_posts_training_module 
FOREIGN KEY (training_module_id) 
REFERENCES training_modules(id) 
ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_posts_training_module_id 
ON posts(training_module_id);

-- Verify the column was added
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'posts' 
AND column_name = 'training_module_id';

-- Check the constraint was added
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'posts' 
AND constraint_name = 'fk_posts_training_module';

COMMIT;




