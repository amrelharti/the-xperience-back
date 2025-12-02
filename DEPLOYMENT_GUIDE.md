# Production Deployment Guide

## Current Issue
Your production backend is running **outdated code** that doesn't support the `PUT /api/me` endpoint for profile updates.

## Solution
Deploy the newly built JAR file that includes the profile update functionality.

---

## Quick Deployment (3 Methods)

### Method 1: Using Deployment Script (Linux/Mac)

```bash
# Make script executable
chmod +x deploy-to-production.sh

# Run deployment (update the server address if needed)
./deploy-to-production.sh root@api.the-xperience.com
```

### Method 2: Using PowerShell Script (Windows)

```powershell
# Run the PowerShell deployment helper
.\deploy-to-production.ps1
```

This will show you the exact commands to run.

### Method 3: Manual Deployment (Step by Step)

#### Step 1: Verify the JAR is built
```bash
# Check if JAR exists
ls -lh target/the-xperience-back-0.0.1-SNAPSHOT.jar
```

You should see the JAR file (built on 2025-11-06).

#### Step 2: Connect to your production server
```bash
ssh root@api.the-xperience.com
# OR whatever your production server address is
```

#### Step 3: Backup the current JAR (on production server)
```bash
cd /opt/the-xperience-back  # Or wherever your JAR is located
cp the-xperience-back-0.0.1-SNAPSHOT.jar the-xperience-back-0.0.1-SNAPSHOT.jar.backup-$(date +%Y%m%d-%H%M%S)
```

#### Step 4: Stop the backend service (on production server)
```bash
# If using systemd:
sudo systemctl stop the-xperience-back

# OR if running manually:
pkill -f "the-xperience-back.*jar"

# Verify it's stopped:
sudo systemctl status the-xperience-back
# OR
ps aux | grep the-xperience-back
```

#### Step 5: Upload the new JAR (from your local machine)

**Option A: Using SCP**
```bash
scp target/the-xperience-back-0.0.1-SNAPSHOT.jar root@api.the-xperience.com:/opt/the-xperience-back/
```

**Option B: Using WinSCP or FileZilla**
- Open WinSCP/FileZilla
- Connect to your production server
- Navigate to `/opt/the-xperience-back/`
- Upload the file from: `D:\Ali projet\the-xperience-back\target\the-xperience-back-0.0.1-SNAPSHOT.jar`

**Option C: Using PowerShell**
```powershell
# Use PSCP (part of PuTTY) or OpenSSH
scp "D:\Ali projet\the-xperience-back\target\the-xperience-back-0.0.1-SNAPSHOT.jar" root@api.the-xperience.com:/opt/the-xperience-back/
```

#### Step 6: Start the backend service (on production server)
```bash
# If using systemd:
sudo systemctl start the-xperience-back

# Verify it's running:
sudo systemctl status the-xperience-back

# Check logs:
sudo journalctl -u the-xperience-back -n 50 -f
```

#### Step 7: Verify the deployment
```bash
# Check if the PUT endpoint is now available:
curl -X OPTIONS https://api.the-xperience.com/api/me -v

# You should see PUT in the Allow header
```

#### Step 8: Test in the browser
1. Go to: https://app.the-xperience.com/profile
2. Try to edit your profile (name, email, title, etc.)
3. Click "Save Changes"
4. You should see a success message!

---

## Troubleshooting

### If the deployment fails:

**Rollback to previous version:**
```bash
ssh root@api.the-xperience.com
cd /opt/the-xperience-back
sudo systemctl stop the-xperience-back
cp the-xperience-back-0.0.1-SNAPSHOT.jar.backup-* the-xperience-back-0.0.1-SNAPSHOT.jar
sudo systemctl start the-xperience-back
```

### If you're still getting 403/PUT not supported:

1. **Verify the JAR was actually replaced:**
   ```bash
   ssh root@api.the-xperience.com
   ls -lh /opt/the-xperience-back/*.jar
   # Check the timestamp - should be recent
   ```

2. **Verify the correct JAR is being used:**
   ```bash
   ps aux | grep java
   # Make sure it's using the right JAR path
   ```

3. **Check for multiple backend instances:**
   ```bash
   ps aux | grep the-xperience-back
   # Kill any old processes
   ```

4. **Check the systemd service file:**
   ```bash
   cat /etc/systemd/system/the-xperience-back.service
   # Verify the ExecStart path points to the correct JAR
   ```

---

## What's Fixed in This Deployment

The new JAR includes:
- ✅ `PUT /api/me` endpoint for updating user profiles
- ✅ Support for updating: name, email, imageUrl, title, instagram, linkedin
- ✅ Proper authentication and authorization
- ✅ Email uniqueness validation

---

## Need Help?

If you're not sure about any of these steps or need help with deployment, let me know:
- What hosting provider are you using?
- Do you have SSH access to the production server?
- How do you currently deploy?




