# Run Migration V53 on Server

## Step 1: Copy Migration File to Server

You need to get the migration file to your server. Options:

### Option A: Using SCP/SFTP
```bash
# From your local machine
scp "D:\Ali projet\the-xperience-back\src\main\resources\db\migration\V53__Update_Live_Sessions_Per_Class.sql" user@your-server:/path/to/destination/
```

### Option B: Using Git
```bash
# If your server has git access
git pull origin main  # or your branch name
```

### Option C: Copy file content
- Open the migration file locally
- Copy all content
- Create new file on server and paste

---

## Step 2: Connect to Your Server Database

### Using SSH + psql
```bash
# SSH into your server
ssh user@your-server-ip

# Connect to PostgreSQL (adjust connection details)
psql -h localhost -U your_db_user -d your_database_name
```

### Using Direct psql Connection
```bash
# From your local machine, connect directly to server database
psql -h your-server-ip -p 5432 -U your_db_user -d your_database_name
```

---

## Step 3: Run the Migration

### Method 1: From psql command line
```sql
-- Once connected to psql, run:
\i /path/to/V53__Update_Live_Sessions_Per_Class.sql
```

### Method 2: Direct execution
```bash
# From server command line
psql -h localhost -U your_db_user -d your_database_name -f /path/to/V53__Update_Live_Sessions_Per_Class.sql
```

### Method 3: Copy-paste content
```sql
-- Connect to psql, then copy-paste the entire migration content
-- The migration file content starts with:
-- Migration V53: Update live_sessions to support per-class live sessions
```

---

## Step 4: Verify Migration Success

After running, verify in psql:

```sql
-- Check columns were added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'live_sessions' 
AND column_name IN ('class_id', 'is_archived');

-- Check foreign key constraint
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'live_sessions'
AND constraint_name = 'fk_live_sessions_class';

-- Check unique constraint
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'live_sessions'
AND constraint_name = 'uk_live_sessions_module_class';

-- Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'live_sessions'
AND indexname IN ('idx_live_sessions_class', 'idx_live_sessions_archived');
```

**Expected Output:**
- `class_id` column: UUID, NOT NULL
- `is_archived` column: BOOLEAN, NOT NULL, DEFAULT false
- `fk_live_sessions_class` constraint exists
- `uk_live_sessions_module_class` constraint exists
- Both indexes exist

---

## Step 5: Update Existing Live Sessions (if needed)

If you have existing live sessions, you may need to set their `class_id`:

```sql
-- First, check existing live sessions
SELECT ls.id, ls.training_module_id, ls.class_id, tm.training_id
FROM live_sessions ls
INNER JOIN training_modules tm ON tm.id = ls.training_module_id
WHERE ls.class_id IS NULL;

-- Find appropriate class for each session
SELECT 
    ls.id as session_id,
    ls.training_module_id,
    c.id as class_id,
    c.name as class_name,
    tm.training_id
FROM live_sessions ls
INNER JOIN training_modules tm ON tm.id = ls.training_module_id
INNER JOIN classes c ON c.module_id = tm.training_id
WHERE ls.class_id IS NULL
ORDER BY ls.created_at DESC;

-- Update live sessions with class_id (example - adjust IDs)
UPDATE live_sessions ls
SET class_id = (
    SELECT c.id 
    FROM classes c
    INNER JOIN training_modules tm ON tm.training_id = c.module_id
    WHERE tm.id = ls.training_module_id
    ORDER BY c.created_at DESC
    LIMIT 1
)
WHERE ls.class_id IS NULL;
```

---

## Important Notes

1. **Backup First**: Always backup your database before running migrations:
   ```bash
   pg_dump -h localhost -U your_db_user -d your_database_name > backup_before_v53.sql
   ```

2. **Test on Staging**: If you have a staging environment, test there first

3. **Check Application Logs**: After migration, restart your Spring Boot app and check for errors

4. **Flyway**: If Flyway is enabled, it should recognize the migration as already applied. You can check:
   ```sql
   SELECT * FROM flyway_schema_history WHERE version = '53';
   ```

---

## Troubleshooting

### If migration fails partway through:
The migration uses `DO $$` blocks with checks, so it's safe to re-run. It will skip steps that already completed.

### If you get permission errors:
Make sure your database user has:
- `ALTER TABLE` permission
- `CREATE INDEX` permission
- `ALTER CONSTRAINT` permission

### If class_id population fails:
You may need to manually update existing live sessions (see Step 5 above).

---

## Quick Command Reference

```bash
# 1. Copy file to server
scp V53__Update_Live_Sessions_Per_Class.sql user@server:/tmp/

# 2. SSH to server
ssh user@server

# 3. Connect to database
psql -U your_db_user -d your_database_name

# 4. Run migration
\i /tmp/V53__Update_Live_Sessions_Per_Class.sql

# 5. Verify
SELECT column_name FROM information_schema.columns 
WHERE table_name = 'live_sessions' AND column_name IN ('class_id', 'is_archived');
```

