# Resources Post Type - Backend Implementation Summary

## Changes Made

### 1. PostEntity.java - Added RESOURCES to Enum
**File:** `src/main/java/com/thexperience/backend/entities/PostEntity.java`

**Change:** Added `RESOURCES` to the PostType enum on line 100:
```java
public enum PostType {
    POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT, RESOURCES
}
```

---

### 2. Database Migration - V47
**File:** `src/main/resources/db/migration/V47__Add_Resources_Post_Type.sql`

**Created new migration file to update the database constraint:**
```sql
-- Drop the old constraint
ALTER TABLE posts DROP CONSTRAINT IF EXISTS chk_posts_post_type;

-- Recreate the constraint with RESOURCES post type included
ALTER TABLE posts ADD CONSTRAINT chk_posts_post_type 
CHECK (post_type IN ('POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION', 'RESOURCES'));
```

This migration will automatically run when you restart the Spring Boot application.

---

### 3. FeedController.java - Admin Authorization
**File:** `src/main/java/com/thexperience/backend/feed/FeedController.java`

**Change:** Updated authorization logic on lines 365-371 to restrict RESOURCES posts to admins only:
```java
// Restrict ANNOUNCEMENT and RESOURCES creation to admins only
if (postType == PostEntity.PostType.ANNOUNCEMENT || postType == PostEntity.PostType.RESOURCES) {
    if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
        String postTypeName = postType == PostEntity.PostType.ANNOUNCEMENT ? "announcements" : "resources";
        return ResponseEntity.status(403).body(Map.of("error", "Only administrators can create " + postTypeName));
    }
}
```

---

## How to Deploy

### Step 1: Restart the Backend
Simply restart your Spring Boot application. The Flyway migration V47 will automatically execute and update the database constraint.

### Step 2: Verify the Changes

1. **Check the database constraint:**
```sql
SELECT pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conname = 'chk_posts_post_type';
```

Expected output should include: `'POST', 'ASSIGNMENT', 'WEEKLY_SUMMARY', 'FEEDBACK', 'ANNOUNCEMENT', 'QUESTION', 'RESOURCES'`

2. **Test creating a RESOURCES post:**
   - As an admin, try creating a RESOURCES post from the frontend
   - As a regular user, verify you get a 403 error when trying to create a RESOURCES post

---

## Features

✅ **Admin-Only Access:** Only users with ADMIN role can create RESOURCES posts
✅ **Database Validation:** The database constraint ensures only valid post types are accepted
✅ **File Support:** RESOURCES posts support links, PDFs, and images through the existing attachment system
✅ **Full Integration:** Works with all existing feed filtering, searching, and display features

---

## Testing Checklist

- [ ] Restart Spring Boot backend
- [ ] Verify migration V47 executed successfully (check Flyway schema history)
- [ ] Login as admin and create a RESOURCES post with a PDF attachment
- [ ] Login as admin and create a RESOURCES post with a link in the content
- [ ] Login as a regular student and verify they cannot select RESOURCES type
- [ ] Verify RESOURCES posts appear correctly in the feed
- [ ] Test filtering feed by RESOURCES type
- [ ] Verify RESOURCES posts display with correct icon and color (teal)

---

## Frontend Changes (Already Completed)

All frontend changes have been implemented:
- ✅ Added RESOURCES option in post creation page (admin only)
- ✅ Added RESOURCES icon (FolderOpen) and styling (teal color)
- ✅ Updated all feed pages to display RESOURCES posts
- ✅ Added RESOURCES to filter dropdowns

---

Date: November 11, 2025

