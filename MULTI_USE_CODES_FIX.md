# Multi-Use Access Codes Fix

## Problem
Access codes were being treated as **single-use** instead of **multi-use**, causing:
- ✗ Code works for first student, fails for second student
- ✗ Module unlock fails with 400 Bad Request after first use
- ✗ Class enrollment fails with 400 Bad Request after first use

## Root Cause
The entity classes (`ModuleAccessCodeEntity` and `ClassEnrollmentCodeEntity`) have a field:
```java
@Column(name = "is_single_use", nullable = false)
private boolean singleUse = true;  // ❌ Defaults to TRUE!
```

When generating codes, the controllers were NOT setting `singleUse = false`, so codes defaulted to single-use.

## Fixes Applied

### 1. Training Module Access Code Controller
**File**: `src/main/java/com/thexperience/backend/codes/TrainingModuleAccessCodeController.java`

**Line 72**: Added `codeEntity.setSingleUse(false);` to make codes multi-use

```java
// Generate ONE new active code (multi-use by default)
String plain = codeUtils.generatePlainCode();
String hash = codeUtils.sha256(plain);

ModuleAccessCodeEntity codeEntity = new ModuleAccessCodeEntity();
codeEntity.setModule(training);
codeEntity.setTrainingModule(trainingModule);
codeEntity.setCodeHash(hash);
codeEntity.setPlainCode(plain);
codeEntity.setGeneratedBy(admin);
codeEntity.setCreatedAt(Instant.now());
codeEntity.setActive(true);
codeEntity.setSingleUse(false); // ✅ CRITICAL FIX: Set to multi-use!

codes.save(codeEntity);
```

**Line 41**: Also fixed to accept request body (required by frontend)

### 2. Class Enrollment Code Controller
**File**: `src/main/java/com/thexperience/backend/codes/ClassEnrollmentCodeController.java`

**Line 58**: Added `e.setSingleUse(false);` to make codes multi-use

```java
// Generate ONE new active code (multi-use by default)
String plain = codeUtils.generatePlainCode();
String hash = codeUtils.sha256(plain);
ClassEnrollmentCodeEntity e = new ClassEnrollmentCodeEntity();
e.setClassEntity(classEntity);
e.setCodeHash(hash);
e.setPlainCode(plain);
e.setGeneratedBy(admin);
e.setActive(true);
e.setSingleUse(false); // ✅ CRITICAL FIX: Set to multi-use!
codes.save(e);
```

**Line 38**: Also fixed to accept request body (required by frontend)

### 3. Database Migration
**File**: `src/main/resources/db/migration/V43__Set_Codes_To_Multi_Use.sql`

Updates ALL existing codes in the database to be multi-use:

```sql
-- Update training module access codes
UPDATE module_access_code
SET is_single_use = false
WHERE is_single_use = true;

-- Update class enrollment codes  
UPDATE class_enrollment_code
SET is_single_use = false
WHERE is_single_use = true;

-- Update course access codes
UPDATE course_access_code
SET is_single_use = false
WHERE is_single_use = true;
```

## How It Works Now

### Multi-Use Code Flow
1. **Admin** generates a code (e.g., `D7K7MQGC3`)
2. **Student 1** uses code → ✅ Success (logs usage in `code_usage_log`)
3. **Student 2** uses SAME code → ✅ Success (logs usage again)
4. **Student 3** uses SAME code → ✅ Success (logs usage again)
5. **Code remains active** until admin generates a new one

### Code Deactivation
When admin generates a **new** code:
- All previous codes for that module/class are deactivated (`is_active = false`)
- New code becomes the only active code
- Students using old codes will get "Invalid or inactive code" error

## Testing

### Before Fix
```
Student 1: Uses code "ABC123" → ✅ Success
Student 2: Uses code "ABC123" → ❌ 400 Bad Request
```

### After Fix
```
Student 1: Uses code "ABC123" → ✅ Success
Student 2: Uses code "ABC123" → ✅ Success
Student 3: Uses code "ABC123" → ✅ Success
...
```

## Deployment Steps

1. **Commit the backend changes**
2. **Restart the Spring Boot backend**
   - Flyway will automatically apply migration V43
   - All existing codes will be set to multi-use
3. **Generate new codes** in admin panel (optional but recommended)
4. **Test** with multiple students using the same code

## Verification

Check the database after deployment:
```sql
-- All codes should have is_single_use = false
SELECT id, plain_code, is_single_use, is_active 
FROM module_access_code 
WHERE is_active = true;

SELECT id, plain_code, is_single_use, is_active 
FROM class_enrollment_code 
WHERE is_active = true;
```

## Notes
- Codes are tracked via `code_usage_log` table
- Each student's usage is logged separately
- Admin can see usage statistics in the admin panel
- The `is_single_use` field is now explicitly set to `false` for all new codes

