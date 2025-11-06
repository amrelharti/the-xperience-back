#!/bin/bash

# Production Deployment Script for The Xperience Backend
# Usage: ./deploy-to-production.sh [production-server-user@host]

set -e  # Exit on any error

echo "======================================"
echo "The Xperience Backend Deployment"
echo "======================================"

# Configuration - UPDATE THESE
PROD_SERVER="${1:-root@api.the-xperience.com}"  # Server user@host
PROD_PATH="/opt/the-xperience-back"              # Path on production server
JAR_NAME="the-xperience-back-0.0.1-SNAPSHOT.jar"
SERVICE_NAME="the-xperience-back"                # systemd service name

# Local paths
LOCAL_JAR="target/${JAR_NAME}"
LOCAL_PROPS="src/main/resources/application.properties"

echo ""
echo "Step 1: Verifying local build..."
if [ ! -f "$LOCAL_JAR" ]; then
    echo "ERROR: JAR file not found at $LOCAL_JAR"
    echo "Please run: ./mvnw clean package -DskipTests"
    exit 1
fi
echo "✓ JAR file found: $LOCAL_JAR"

echo ""
echo "Step 2: Creating backup on production server..."
ssh "$PROD_SERVER" "cd $PROD_PATH && [ -f $JAR_NAME ] && cp $JAR_NAME ${JAR_NAME}.backup-\$(date +%Y%m%d-%H%M%S) || echo 'No existing JAR to backup'"

echo ""
echo "Step 3: Stopping production service..."
ssh "$PROD_SERVER" "sudo systemctl stop $SERVICE_NAME || pkill -f '$JAR_NAME' || echo 'Service not running'"
sleep 2

echo ""
echo "Step 4: Uploading new JAR file..."
scp "$LOCAL_JAR" "$PROD_SERVER:$PROD_PATH/$JAR_NAME"
echo "✓ JAR uploaded successfully"

echo ""
echo "Step 5: Starting production service..."
ssh "$PROD_SERVER" "sudo systemctl start $SERVICE_NAME"
sleep 3

echo ""
echo "Step 6: Checking service status..."
ssh "$PROD_SERVER" "sudo systemctl status $SERVICE_NAME --no-pager -l" || echo "Service check completed"

echo ""
echo "Step 7: Viewing recent logs..."
ssh "$PROD_SERVER" "sudo journalctl -u $SERVICE_NAME -n 50 --no-pager" || echo "Check logs manually"

echo ""
echo "======================================"
echo "✓ Deployment Complete!"
echo "======================================"
echo ""
echo "Next steps:"
echo "1. Test the profile update at: https://app.the-xperience.com/profile"
echo "2. Monitor logs: ssh $PROD_SERVER 'sudo journalctl -u $SERVICE_NAME -f'"
echo "3. If issues occur, rollback with: ssh $PROD_SERVER 'cd $PROD_PATH && sudo systemctl stop $SERVICE_NAME && cp ${JAR_NAME}.backup-* $JAR_NAME && sudo systemctl start $SERVICE_NAME'"

