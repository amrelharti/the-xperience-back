-- Fix attachments column type from JSONB to TEXT
ALTER TABLE homeworks ALTER COLUMN attachments TYPE TEXT;
