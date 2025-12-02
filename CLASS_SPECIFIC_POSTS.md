# Class-Specific Posts Implementation

## Summary
**ALL post types** (POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT) are now **class-specific** and will only be visible to users enrolled in that specific class.

## Post Types & Visibility

| Post Type | Visibility | Class Assignment |
|-----------|-----------|------------------|
| POST | 👥 Class-specific | ✅ Required |
| ASSIGNMENT | 👥 Class-specific | ✅ Required |
| WEEKLY_SUMMARY | 👥 Class-specific | ✅ Required |
| FEEDBACK | 👥 Class-specific | ✅ Required |
| ANNOUNCEMENT | 👥 Class-specific | ✅ Required |

## Key Features

### 1. Class Assignment (Required for All Posts)
All posts **must** be associated with a class. The system will:

1. **If classId is provided:**
   - **Admins**: Can post to ANY class (even if not enrolled)
   - **Students**: Can only post to classes they're enrolled in

2. **If no classId provided:**
   - Attempts to auto-assign based on module (if moduleId provided)
   - Falls back to user's first enrollment
   - **Rejects the post** if no class can be assigned

### 2. Feed Visibility
Users will only see posts from classes they are enrolled in:
- Student in Class A → Sees only Class A posts
- Student in Class B → Sees only Class B posts
- Student enrolled in both → Sees posts from both classes

### 3. Admin Posting Privileges
Admins have special permissions:
- ✅ Can post to any class (regardless of enrollment)
- ✅ Can create announcements (students cannot)
- ✅ Must select a class when posting from admin feed page

## Technical Implementation

### Feed Filtering (`GET /api/feed`)
```java
// Users with no enrollments see nothing
if (userClassIds.isEmpty()) {
    return Page.empty(pageable);
}

// Filter posts by user's enrolled classes only
posts = postRepository.findByClassEntityIdInAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(
    classesToFilter, 
    pageable
);
```

### Post Creation (`POST /api/feed/posts`)
```java
// All posts require class assignment
if (request.getClassId() != null) {
    if (isAdmin) {
        // Admin can post to any class
        post.setClassEntity(classEntity);
    } else {
        // Student must be enrolled in the class
        // ... enrollment check ...
    }
} else {
    // Auto-assign based on module or enrollment
    // Or reject if no class can be assigned
}
```

## Expected Behavior

### Admin Creates Announcement in Class A:
✅ Announcement is created with `class_id = Class A`
✅ Only appears in Class A feed
✅ Visible only to students enrolled in Class A

### Student Creates Post:
✅ Post is assigned to a class (selected or auto-assigned)
✅ Only visible to users in that specific class
✅ Cannot post without class enrollment

### Viewing Feeds:
✅ Each class has its own isolated feed
✅ Posts don't leak between classes
✅ Users without enrollments see empty feed

## Database Schema
All posts in the `posts` table will have:
- `class_id` column: **NOT NULL** for all post types
- Each post belongs to exactly one class
- Class determines visibility scope

## Testing Checklist

- [ ] Admin creates announcement in Class A → only visible in Class A
- [ ] Admin creates announcement in Class B → only visible in Class B
- [ ] Student in Class A sees only Class A posts
- [ ] Student in Class B sees only Class B posts
- [ ] Student enrolled in both classes sees posts from both
- [ ] Admin can post to any class from admin feed page
- [ ] Students cannot create announcements
- [ ] All post types (POST, ASSIGNMENT, WEEKLY_SUMMARY, FEEDBACK, ANNOUNCEMENT) work correctly
- [ ] Posts can be deleted
- [ ] Attempting to post without class selection shows error

## Migration Notes
If you previously had announcement posts with `class_id = NULL` (from the global implementation), you may need to:
1. Assign them to a specific class, OR
2. Delete them if they're no longer needed

SQL to check for orphaned posts:
```sql
SELECT * FROM posts WHERE class_id IS NULL;
```

