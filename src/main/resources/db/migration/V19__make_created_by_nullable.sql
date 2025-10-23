-- Make created_by column nullable in classes table
ALTER TABLE classes ALTER COLUMN created_by DROP NOT NULL;


