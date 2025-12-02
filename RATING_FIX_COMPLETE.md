# Rating System Fixes - Complete Guide

## Problems Fixed

### 1. ❌ **Can't Rate 10/10**
**Error:** `numeric field overflow - A field with precision 3, scale 2 must round to an absolute value less than 10^1`

**Root Cause:** The `avg_score` column was defined as `NUMERIC(3,2)` which only allows values from 0.00 to 9.99

**Solution:** Changed to `NUMERIC(4,2)` to allow values from 0.00 to 99.99

### 2. ❌ **Weekly Summary Ratings Not Visible on Feed**
**Problem:** Ratings only showed for ASSIGNMENT posts, not WEEKLY_SUMMARY posts on the feed page

**Solution:** Updated feed display logic to show ratings for both ASSIGNMENT and WEEKLY_SUMMARY posts

---

## 🚀 How to Apply the Fixes

### Step 1: Fix the Database

Run the SQL script to update the column precision:

```bash
# As database superuser
sudo -u postgres psql -d xperience2_db -f FIX_RATING_PRECISION.sql

# OR as database owner
psql -U <owner> -d xperience2_db -f FIX_RATING_PRECISION.sql
```

**Or run this single command:**

```sql
ALTER TABLE posts ALTER COLUMN avg_score TYPE NUMERIC(4, 2);
```

### Step 2: Restart the Backend

Since we updated the entity annotation, restart your Spring Boot application:

```bash
# Stop the backend (Ctrl+C if running in terminal)
# Then restart it
```

The backend will pick up the new precision setting.

### Step 3: Verify the Frontend Changes

The frontend has been updated automatically. Refresh your browser to see:
- ✅ Weekly Summary ratings now show on feed cards
- ✅ Ratings display with yellow badge and TrendingUp icon

---

## ✅ Files Modified

### Backend
1. **`PostEntity.java`** - Changed `avg_score` precision from 3,2 to 4,2
2. **`FIX_RATING_PRECISION.sql`** - SQL script to update database

### Frontend
1. **`app/feed/page.tsx`** - Added WEEKLY_SUMMARY to rating display condition
2. **`app/feed/new/page.tsx`** - Fixed SelectItem components (already done)

---

## 🧪 Testing

### Test Rating 10/10:
1. Create a WEEKLY_SUMMARY or ASSIGNMENT post
2. Open the post detail page
3. Click the "10" button to rate
4. ✅ Should succeed without error
5. ✅ Should show "10.0/10" in the rating display

### Test Feed Display:
1. Go to the feed page
2. Find a WEEKLY_SUMMARY or ASSIGNMENT post that has been rated
3. ✅ Should see yellow badge with rating (e.g., "8.5/10 (3)")

---

## 📊 Rating Scale Reference

All rateable posts (ASSIGNMENT and WEEKLY_SUMMARY) use a 1-10 scale:

- **1-3:** Needs improvement
- **4-6:** Satisfactory
- **7-8:** Good
- **9-10:** Excellent

**Important Notes:**
- Users cannot rate their own posts
- Ratings cannot be changed once submitted
- Both post types contribute to the leaderboard

---

## 🎯 What's Now Working

✅ Users can rate WEEKLY_SUMMARY posts with any score from 1-10 (including 10!)
✅ Weekly Summary ratings display on feed cards
✅ Assignment ratings display on feed cards
✅ Both types show average score and rating count
✅ Ratings contribute to leaderboard calculations

---

## Database Schema

```sql
-- posts table
CREATE TABLE posts (
    ...
    avg_score NUMERIC(4, 2) DEFAULT 0.00,  -- Now allows 10.00!
    score_count INTEGER DEFAULT 0,
    post_type VARCHAR(50) CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION')),
    ...
);
```

---

## Common Issues

### "Still can't rate 10"
- **Check:** Did you run the SQL migration?
- **Check:** Did you restart the backend after updating PostEntity.java?
- **Check:** Clear browser cache

### "Ratings not showing on feed"
- **Check:** Is the post actually rated? (avgScore > 0)
- **Check:** Refresh the page after the frontend update
- **Check:** Look for the yellow badge on ASSIGNMENT or WEEKLY_SUMMARY posts

---

## Next Steps (Optional Enhancements)

If you want to improve the rating system further:

1. **Add Rating Breakdown:** Show how many people rated each score (histogram)
2. **Add Rating Comments:** Allow users to add text feedback with ratings
3. **Add Rating Filters:** Filter feed by highly-rated posts
4. **Add Rating Notifications:** Notify post authors when they receive ratings
5. **Add Rating Badges:** Award badges for consistently high-rated posts

Let me know if you'd like to implement any of these! 🚀

