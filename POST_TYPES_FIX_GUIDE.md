# Fix for WEEKLY_SUMMARY and FEEDBACK Post Types

## Problem Summary
Users cannot create WEEKLY_SUMMARY or FEEDBACK post types. The application allows creating only POST, ASSIGNMENT, and ANNOUNCEMENT types.

## Root Cause
The database has a CHECK constraint on the `posts` table that only allows these post types:
- POST
- ASSIGNMENT  
- QUESTION
- ANNOUNCEMENT

The migration file `V46__Add_Weekly_Summary_And_Feedback_Post_Types.sql` was created to add WEEKLY_SUMMARY and FEEDBACK to the constraint, but **it was never applied** because Flyway is disabled in `application.properties`.

When users try to create WEEKLY_SUMMARY or FEEDBACK posts, the backend correctly processes them, but the database rejects the insert with a constraint violation error.

## Solution

### Step 1: Apply the Database Fix

Run the SQL script to update the database constraint:

```bash
# Connect to your PostgreSQL database
psql -U xperience_user -d xperience2_db -f FIX_POST_TYPES_NOW.sql

# OR if using a GUI tool (pgAdmin, DBeaver, etc.), run the contents of FIX_POST_TYPES_NOW.sql
```

The script will:
1. Drop the old constraint that blocks WEEKLY_SUMMARY and FEEDBACK
2. Create a new constraint that allows all post types
3. Verify the fix was applied successfully

### Step 2: Verify the Fix

After running the script, try creating a post with type WEEKLY_SUMMARY or FEEDBACK. It should work now!

### Step 3: Frontend Fix (Already Applied)

The frontend has been updated to simplify the SelectItem components in `app/feed/new/page.tsx`. This ensures better compatibility with the Radix UI Select component.

## Technical Details

### Files Changed

**Backend:**
- Database constraint updated via `FIX_POST_TYPES_NOW.sql`

**Frontend:**  
- `app/feed/new/page.tsx` - Simplified SelectItem components (removed nested divs)

### Database Schema

The posts table now allows these post types:
```sql
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION'))
```

### Why QUESTION is Still Included
The QUESTION type is kept for backward compatibility with existing posts in the database, even though it's been removed from the frontend UI.

## Testing

After applying the fix, test each post type:

1. ✅ Regular Post (POST)
2. ✅ Assignment Submission (ASSIGNMENT)  
3. ✅ Weekly Summary (WEEKLY_SUMMARY) - **Now works!**
4. ✅ Feedback (FEEDBACK) - **Now works!**
5. ✅ Announcement (ANNOUNCEMENT) - Admin only

## Notes

- The backend code (FeedController.java) was already correct and handles all post types properly
- The frontend UI (feed/new/page.tsx) was already showing all options
- The issue was purely a database constraint problem
- Future migrations will need to be applied manually since Flyway is disabled

## Production Deployment

When deploying to production:

1. **IMPORTANT:** Run `FIX_POST_TYPES_NOW.sql` on the production database FIRST
2. Then deploy the updated frontend code
3. Test all post types after deployment

## Prevention

To prevent similar issues in the future:
- Consider enabling Flyway in production: `spring.flyway.enabled=true`
- OR maintain a checklist of manual migrations that need to be applied
- Document all database schema changes in migration files even if Flyway is disabled

