# Live Session Per Class - Backend Implementation Complete ✅

## Summary
Successfully updated the backend to support creating and managing live sessions per class for each training module. This allows multiple classes (e.g., November class, December class) to have their own live sessions for the same training module.

---

## ✅ Changes Completed

### 1. **LiveSessionEntity.java** - Added Class Relationship and Archive Support
   - **Added `ClassEntity classEntity`** field with `@ManyToOne` relationship
   - **Added `Boolean isArchived`** field (default: false) to hide old sessions
   - **Updated constructors** to include `ClassEntity` parameter
   - **Added getters/setters** for new fields

### 2. **LiveSessionRepository.java** - Updated Query Methods
   - **Added `findByTrainingModuleAndClassEntity()`** - Find session by module AND class
   - **Added `findByTrainingModuleAndClassEntityAndIsArchivedFalse()`** - Find active session
   - **Added `findByTrainingModuleAndIsArchivedFalse()`** - Backward compatibility
   - **Added `findByTrainingModuleAndClassEntityOrderByStartsAtAsc()`** - List sessions
   - **Added `findSessionsByTrainingModuleAndClassAndDateRange()`** - Date range queries
   - **Added `countByTrainingModuleAndClassEntityAndIsArchivedFalse()`** - Count active sessions
   - **Deprecated `findByTrainingModule()`** - Use class-specific methods instead

### 3. **LiveSessionController.java** - Updated All Endpoints
   - **POST `/api/admin/modules/{moduleId}/live-session?classId={classId}`**
     - Accepts `classId` as query parameter or in request body
     - Validates that class belongs to the training
     - Creates/updates session for specific module+class combination
     - Resets `isArchived` to false when updating
   
   - **GET `/api/admin/modules/{moduleId}/live-session?classId={classId}`**
     - Accepts optional `classId` parameter
     - Returns session for specific class if classId provided
     - Falls back to first active session if no classId (backward compatibility)
   
   - **DELETE `/api/admin/modules/{moduleId}/live-session?classId={classId}`**
     - Requires `classId` parameter
     - Deletes session for specific module+class combination
   
   - **GET `/api/modules/{moduleId}/live-session?classId={classId}`** (Student endpoint)
     - Requires `classId` parameter
     - Returns only active (non-archived) sessions
     - Filters by module AND class
   
   - **Updated `CreateSessionRequest` DTO** to include `classId` field
   - **Updated `convertToResponse()`** to include class information and `isArchived` status

### 4. **Database Migration V53** - Schema Updates
   - **Added `class_id` column** with foreign key to `classes` table
   - **Added `is_archived` column** (BOOLEAN, default false)
   - **Removed UNIQUE constraint** on `training_module_id`
   - **Added UNIQUE constraint** on `(training_module_id, class_id)` - ensures one session per module per class
   - **Created indexes** on `class_id` and `is_archived` for better query performance
   - **Handles existing data** by attempting to populate `class_id` from related classes

---

## 🔧 Key Features

### Per-Class Live Sessions
- Each class can have its own live session for the same training module
- Example: November class and December class can both have Module 1 live sessions with different schedules

### Archive Support
- Old live sessions can be marked as `isArchived = true`
- Archived sessions are hidden from students
- Admins can still see archived sessions

### Backward Compatibility
- Endpoints still work without `classId` (with deprecation warnings)
- Existing code will continue to function
- Migration handles existing data gracefully

### Data Integrity
- UNIQUE constraint ensures no duplicate sessions for same module+class
- Foreign key constraints maintain referential integrity
- Validation ensures class belongs to the training

---

## 📋 API Endpoints

### Admin Endpoints

#### Create/Update Live Session
```http
POST /api/admin/modules/{moduleId}/live-session?classId={classId}
Content-Type: application/json

{
  "title": "Week 1 Live Session",
  "description": "Introduction",
  "startsAt": "2025-12-01T10:00:00Z",
  "endsAt": "2025-12-01T11:30:00Z",
  "location": "Online",
  "meetingUrl": "https://zoom.us/j/123456",
  "meetingPassword": "password123",
  "durationMinutes": 90,
  "instructorName": "John Doe",
  "classId": "uuid-of-class"
}
```

#### Get Live Session
```http
GET /api/admin/modules/{moduleId}/live-session?classId={classId}
```

#### Delete Live Session
```http
DELETE /api/admin/modules/{moduleId}/live-session?classId={classId}
```

### Student Endpoints

#### Get Live Session (Active Only)
```http
GET /api/modules/{moduleId}/live-session?classId={classId}
```

**Response:**
- Only returns non-archived sessions
- Returns 404 if no active session found

---

## 🗄️ Database Schema

### Updated `live_sessions` Table
```sql
Column                | Type      | Constraints
---------------------|-----------|---------------------------
id                   | UUID      | PRIMARY KEY
training_module_id   | UUID      | FK → training_modules(id)
class_id             | UUID      | FK → classes(id) ✅ NEW
title                | VARCHAR   | NOT NULL
description          | TEXT      |
starts_at            | TIMESTAMP | NOT NULL
ends_at              | TIMESTAMP | NOT NULL
location             | VARCHAR   |
meeting_url          | TEXT      |
meeting_password     | VARCHAR   |
duration_minutes     | INTEGER   |
instructor_name      | VARCHAR   |
is_archived          | BOOLEAN   | DEFAULT false ✅ NEW
created_at           | TIMESTAMP | AUTO
updated_at           | TIMESTAMP | AUTO

UNIQUE (training_module_id, class_id) ✅ NEW
```

---

## 🚀 Migration Instructions

1. **Run the migration:**
   ```bash
   # The migration will run automatically on next application start
   # Or manually run: V53__Update_Live_Sessions_Per_Class.sql
   ```

2. **Verify migration:**
   ```sql
   -- Check that class_id column exists
   SELECT column_name FROM information_schema.columns 
   WHERE table_name = 'live_sessions' AND column_name = 'class_id';
   
   -- Check that is_archived column exists
   SELECT column_name FROM information_schema.columns 
   WHERE table_name = 'live_sessions' AND column_name = 'is_archived';
   
   -- Check unique constraint
   SELECT constraint_name FROM information_schema.table_constraints 
   WHERE table_name = 'live_sessions' 
   AND constraint_name = 'uk_live_sessions_module_class';
   ```

3. **Update existing data (if needed):**
   - If `class_id` is NULL for existing sessions, you may need to manually update them
   - The migration attempts to populate `class_id` automatically, but may need manual intervention

---

## ✅ Testing Checklist

### Backend Tests
- [ ] Create live session for a class
- [ ] Create live session for another class (same module)
- [ ] Update existing live session
- [ ] Delete live session
- [ ] Get live session by classId
- [ ] Archive old live session (set isArchived = true)
- [ ] Verify archived sessions are hidden from students
- [ ] Verify UNIQUE constraint prevents duplicates
- [ ] Verify class validation works correctly

### Integration Tests
- [ ] Frontend can create live sessions per class
- [ ] Frontend can view live sessions per class
- [ ] Students only see their class's live session
- [ ] Old sessions can be archived
- [ ] Archived sessions don't appear to students

---

## 📝 Notes

- **Breaking Change**: Existing live sessions without `class_id` will need to be updated
- **Migration Safety**: The migration attempts to populate `class_id` automatically
- **Backward Compatibility**: Endpoints work without `classId` but will be deprecated
- **Performance**: Indexes added for optimal query performance

---

## 🔗 Related Files

- `LiveSessionEntity.java` - Entity with class relationship
- `LiveSessionRepository.java` - Repository with class-specific queries
- `LiveSessionController.java` - Controller with classId support
- `V53__Update_Live_Sessions_Per_Class.sql` - Database migration

---

## ✨ Next Steps

1. Test the migration on a development database
2. Update any existing live sessions with appropriate `class_id` values
3. Test the API endpoints with the frontend
4. Archive old live sessions when new classes start
5. Monitor for any issues with the UNIQUE constraint




