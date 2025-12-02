# Manual Migration Guide - V53 Update Live Sessions Per Class

## Database Connection Info
- **Database**: `xperience2_db`
- **Host**: `localhost:5432`
- **Username**: `xperience_user`
- **Password**: `Islam1997@ali`

---

## Method 1: Using psql Command Line

### Step 1: Connect to PostgreSQL
```bash
psql -h localhost -p 5432 -U xperience_user -d xperience2_db
```

When prompted, enter the password: `Islam1997@ali`

### Step 2: Run the Migration
Once connected, you can either:

**Option A: Copy and paste the entire migration file content**
```sql
-- Just copy the entire content from V53__Update_Live_Sessions_Per_Class.sql
-- and paste it into psql
```

**Option B: Run from file**
```bash
# Exit psql first (type \q)
# Then run:
psql -h localhost -p 5432 -U xperience_user -d xperience2_db -f "src/main/resources/db/migration/V53__Update_Live_Sessions_Per_Class.sql"
```

---

## Method 2: Using pgAdmin (GUI)

1. **Open pgAdmin**
2. **Connect to your database server**
3. **Navigate to**: `Servers` → `PostgreSQL` → `Databases` → `xperience2_db`
4. **Right-click on `xperience2_db`** → **Query Tool**
5. **Open the migration file**: 
   - Click the folder icon or File → Open
   - Navigate to: `D:\Ali projet\the-xperience-back\src\main\resources\db\migration\V53__Update_Live_Sessions_Per_Class.sql`
6. **Click Execute** (F5) or press the play button

---

## Method 3: Using DBeaver or Other SQL Client

1. **Connect to your database**
2. **Open SQL Editor**
3. **Open the migration file**:
   - File → Open SQL Script
   - Navigate to: `D:\Ali projet\the-xperience-back\src\main\resources\db\migration\V53__Update_Live_Sessions_Per_Class.sql`
4. **Execute the script** (Ctrl+Enter or F5)

---

## Method 4: Using PowerShell (Windows)

```powershell
# Navigate to the backend directory
cd "D:\Ali projet\the-xperience-back"

# Run the migration using psql
$env:PGPASSWORD="Islam1997@ali"
psql -h localhost -p 5432 -U xperience_user -d xperience2_db -f "src\main\resources\db\migration\V53__Update_Live_Sessions_Per_Class.sql"
```

---

## Verify Migration Success

After running the migration, verify it worked:

```sql
-- Check that class_id column exists
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'live_sessions' 
AND column_name IN ('class_id', 'is_archived');

-- Check that foreign key constraint exists
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'live_sessions'
AND constraint_name = 'fk_live_sessions_class';

-- Check that unique constraint exists
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

**Expected Results:**
- `class_id` column should exist (UUID, NOT NULL)
- `is_archived` column should exist (BOOLEAN, NOT NULL, DEFAULT false)
- `fk_live_sessions_class` foreign key should exist
- `uk_live_sessions_module_class` unique constraint should exist
- Both indexes should exist

---

## Troubleshooting

### Issue: "column class_id does not exist"
**Solution**: The migration should handle this automatically. If it fails, check the error message.

### Issue: "duplicate key value violates unique constraint"
**Solution**: The migration tries to handle duplicates, but if you have existing data, you may need to manually clean it up first:

```sql
-- Check for existing live sessions
SELECT id, training_module_id, class_id, created_at
FROM live_sessions
ORDER BY training_module_id, created_at;

-- If you need to manually set class_id for existing sessions:
-- First, find which class should be associated:
SELECT ls.id, ls.training_module_id, c.id as class_id, c.name as class_name
FROM live_sessions ls
CROSS JOIN classes c
INNER JOIN training_modules tm ON tm.id = ls.training_module_id
WHERE c.module_id = tm.training_id
AND ls.class_id IS NULL;

-- Then update (replace the IDs with actual values):
UPDATE live_sessions
SET class_id = 'your-class-id-here'
WHERE id = 'your-session-id-here';
```

### Issue: "relation 'classes' does not exist"
**Solution**: Make sure all previous migrations have run. The `classes` table should exist from earlier migrations.

---

## Quick Copy-Paste Version

If you just want to copy-paste the migration directly, here's the file path:
```
D:\Ali projet\the-xperience-back\src\main\resources\db\migration\V53__Update_Live_Sessions_Per_Class.sql
```

Open this file and copy its entire content, then paste it into your SQL client.

---

## After Migration

1. **Restart your Spring Boot application** - Flyway will recognize the migration as already applied
2. **Test the API endpoints** - Try creating a live session with a classId
3. **Check logs** - Look for any errors related to live sessions

---

## Need Help?

If you encounter any errors, check:
1. Database connection is working
2. You have proper permissions (ALTER TABLE, CREATE INDEX)
3. Previous migrations have all run successfully
4. There are no active transactions blocking the migration

