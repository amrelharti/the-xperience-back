# Testing Guide: Training Module Access Code Fix

## Quick Test

### 1. Start the Backend
```bash
cd D:\Ali projet\the-xperience-back
./mvnw spring-boot:run
```

### 2. Generate Test Codes

**In the Admin Panel:**
1. Go to a training (e.g., "Web Development")
2. Navigate to its training modules
3. Generate 2 codes for **Module 1** (e.g., "Frontend Basics")
4. Generate 2 codes for **Module 2** (e.g., "Backend Basics")

### 3. Test Module 1 Access

**As a Student:**
1. Go to the training page
2. Both modules should show as LOCKED
3. Use a Module 1 code to unlock
4. **Expected:** Only Module 1 becomes UNLOCKED
5. **Verify:** Module 2 is still LOCKED ✅

### 4. Test Module 2 Access

**As the same Student:**
1. Use a Module 2 code
2. **Expected:** Module 2 becomes UNLOCKED
3. **Verify:** Both modules now unlocked ✅

### 5. Browser Console Check

**Open DevTools Console:**
- Look for messages when unlocking
- Should see: `🔓 Unlocking module: [module-id]`
- Should see: `📚 Target module: [module name]`
- If backend bug persists, will show: `⚠️ WARNING: Multiple modules unlocked`

## Expected Behavior

### Before Fix ❌
- Generate code for Module 1
- Use code → Unlocks BOTH Module 1 AND Module 2

### After Fix ✅
- Generate code for Module 1
- Use code → Unlocks ONLY Module 1
- Generate code for Module 2  
- Use code → Unlocks ONLY Module 2

## Database Check

Run this query to verify the fix:
```sql
SELECT 
    mac.code_hash,
    tm.title as module_name,
    tm.id as module_id,
    u.name as consumed_by,
    mac.consumed_at
FROM module_access_code mac
JOIN training_modules tm ON tm.id = mac.training_module_id
LEFT JOIN users u ON u.id = mac.consumed_by_id
WHERE mac.training_module_id IS NOT NULL;
```

**Expected:**
- Each code should have a `training_module_id`
- Each code should only unlock its specific module

## Troubleshooting

### If multiple modules still unlock:
1. Check backend logs for errors
2. Verify migration ran: `SELECT * FROM information_schema.columns WHERE table_name = 'module_access_code' AND column_name = 'training_module_id'`
3. Check if codes have `training_module_id` set: `SELECT training_module_id, code_hash FROM module_access_code LIMIT 10`
4. Regenerate access codes in admin panel

### If code doesn't work:
1. Check console for error messages
2. Verify code was consumed: `SELECT * FROM module_access_code WHERE consumed_at IS NOT NULL`
3. Try generating a new code
4. Check backend logs for exceptions

