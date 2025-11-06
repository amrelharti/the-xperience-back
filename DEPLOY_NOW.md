# Deploy to Production - Exact Commands

## Your Production Server Details
- **SSH:** `ssh root@srv1092189`
- **JAR Location:** `/srv`
- **Service:** `systemctl start the-xperience-back`

---

## Deployment Steps (Copy & Paste Each Command)

### Step 1: Open PowerShell and navigate to project

```powershell
cd "D:\Ali projet\the-xperience-back"
```

### Step 2: Verify the new JAR exists

```powershell
ls target\the-xperience-back-0.0.1-SNAPSHOT.jar
```

You should see the file with today's date (Nov 6, 2025).

### Step 3: Backup the current JAR on production

```powershell
ssh root@srv1092189 "cd /srv && cp the-xperience-back-0.0.1-SNAPSHOT.jar the-xperience-back-backup-$(date +%Y%m%d-%H%M%S).jar"
```

### Step 4: Stop the backend service

```powershell
ssh root@srv1092189 "systemctl stop the-xperience-back"
```

### Step 5: Upload the new JAR

```powershell
scp target\the-xperience-back-0.0.1-SNAPSHOT.jar root@srv1092189:/srv/
```

### Step 6: Start the backend service

```powershell
ssh root@srv1092189 "systemctl start the-xperience-back"
```

### Step 7: Check the service status

```powershell
ssh root@srv1092189 "systemctl status the-xperience-back"
```

### Step 8: View the logs (watch for errors)

```powershell
ssh root@srv1092189 "journalctl -u the-xperience-back -n 100 --no-pager"
```

Look for:
- ✅ "Started TheXperienceBackApplication"
- ✅ "Tomcat started on port 8080"
- ❌ No "PUT is not supported" errors

### Step 9: Test in browser

1. Open: https://app.the-xperience.com/profile
2. Click "Edit Profile"
3. Make a change (e.g., update your title)
4. Click "Save Changes"
5. ✅ You should see "Profile updated successfully"

---

## If Something Goes Wrong

### Rollback to previous version:

```powershell
ssh root@srv1092189 "systemctl stop the-xperience-back && cd /srv && cp the-xperience-back-backup-*.jar the-xperience-back-0.0.1-SNAPSHOT.jar && systemctl start the-xperience-back"
```

### View live logs:

```powershell
ssh root@srv1092189 "journalctl -u the-xperience-back -f"
```

Press Ctrl+C to exit log viewing.

---

## Quick One-Liner (All at Once)

If you want to do it all in one go:

```powershell
cd "D:\Ali projet\the-xperience-back"; ssh root@srv1092189 "cd /srv && cp the-xperience-back-0.0.1-SNAPSHOT.jar the-xperience-back-backup.jar && systemctl stop the-xperience-back"; scp target\the-xperience-back-0.0.1-SNAPSHOT.jar root@srv1092189:/srv/; ssh root@srv1092189 "systemctl start the-xperience-back && systemctl status the-xperience-back"
```

**Note:** PowerShell may have issues with semicolons. If this doesn't work, run the commands one by one from Steps 1-8.

