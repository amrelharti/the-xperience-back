-- Add actor reference to notifications so we can show who triggered the event
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS actor_id UUID;

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fk_notifications_actor;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_actor
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL;

-- Backfill existing rows with the post author as the actor so legacy data still shows a name
UPDATE notifications n
SET actor_id = p.author_id
FROM posts p
WHERE n.post_id = p.id
  AND n.actor_id IS NULL;





