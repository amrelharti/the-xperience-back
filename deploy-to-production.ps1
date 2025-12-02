# Production Deployment Script for The Xperience Backend (PowerShell)
# Usage: .\deploy-to-production.ps1

param(
    [string]$ProdServer = "root@api.the-xperience.com",
    [string]$ProdPath = "/opt/the-xperience-back",
    [string]$ServiceName = "the-xperience-back"
)

$ErrorActionPreference = "Stop"

$JarName = "the-xperience-back-0.0.1-SNAPSHOT.jar"
$LocalJar = "target\$JarName"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "The Xperience Backend Deployment" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Verify local build
Write-Host "Step 1: Verifying local build..." -ForegroundColor Yellow
if (-not (Test-Path $LocalJar)) {
    Write-Host "ERROR: JAR file not found at $LocalJar" -ForegroundColor Red
    Write-Host "Please run: .\mvnw.cmd clean package -DskipTests" -ForegroundColor Red
    exit 1
}
Write-Host "✓ JAR file found: $LocalJar" -ForegroundColor Green

# Step 2: Backup on production (via SSH)
Write-Host ""
Write-Host "Step 2: Creating backup on production server..." -ForegroundColor Yellow
Write-Host "Command: ssh $ProdServer `"cd $ProdPath && [ -f $JarName ] && cp $JarName $JarName.backup-`$(date +%Y%m%d-%H%M%S)`"" -ForegroundColor Gray
Write-Host ""

# Step 3: Stop service
Write-Host "Step 3: Stopping production service..." -ForegroundColor Yellow
Write-Host "Command: ssh $ProdServer `"sudo systemctl stop $ServiceName`"" -ForegroundColor Gray
Write-Host ""

# Step 4: Upload JAR
Write-Host "Step 4: Upload new JAR file..." -ForegroundColor Yellow
Write-Host "Command: scp $LocalJar ${ProdServer}:${ProdPath}/${JarName}" -ForegroundColor Gray
Write-Host ""

# Step 5: Start service
Write-Host "Step 5: Starting production service..." -ForegroundColor Yellow
Write-Host "Command: ssh $ProdServer `"sudo systemctl start $ServiceName`"" -ForegroundColor Gray
Write-Host ""

# Step 6: Check status
Write-Host "Step 6: Check service status..." -ForegroundColor Yellow
Write-Host "Command: ssh $ProdServer `"sudo systemctl status $ServiceName`"" -ForegroundColor Gray
Write-Host ""

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Manual Deployment Steps:" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Run these commands manually:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Backup existing JAR:" -ForegroundColor White
Write-Host "   ssh $ProdServer `"cd $ProdPath && cp $JarName ${JarName}.backup`"" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Stop the service:" -ForegroundColor White
Write-Host "   ssh $ProdServer `"sudo systemctl stop $ServiceName`"" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Upload the new JAR (using WinSCP, FileZilla, or scp):" -ForegroundColor White
Write-Host "   Local file: $LocalJar" -ForegroundColor Gray
Write-Host "   Remote location: ${ProdServer}:${ProdPath}/${JarName}" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Start the service:" -ForegroundColor White
Write-Host "   ssh $ProdServer `"sudo systemctl start $ServiceName`"" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Check the logs:" -ForegroundColor White
Write-Host "   ssh $ProdServer `"sudo journalctl -u $ServiceName -f`"" -ForegroundColor Gray
Write-Host ""
Write-Host "6. Test profile update at:" -ForegroundColor White
Write-Host "   https://app.the-xperience.com/profile" -ForegroundColor Cyan
Write-Host ""




