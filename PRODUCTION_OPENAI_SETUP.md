# Production Setup: OpenAI API Key

## Overview
The CV Scorer feature requires an OpenAI API key. For production, the API key should be set as an environment variable for security.

## Configuration

The `application.properties` file is already configured to use an environment variable:
```properties
openai.apiKey=${OPENAI_API_KEY}
```

## Setting the Environment Variable

### Option 1: Systemd Service (Recommended)

If you're using systemd to run the Spring Boot application, add the environment variable to your service file.

1. **Edit your systemd service file** (usually at `/etc/systemd/system/the-xperience-back.service`):

```ini
[Unit]
Description=The Xperience Backend Service
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/the-xperience-back
ExecStart=/usr/bin/java -jar /opt/the-xperience-back/the-xperience-back-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

# Environment Variables
Environment="OPENAI_API_KEY=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"

[Install]
WantedBy=multi-user.target
```

2. **Reload systemd and restart the service:**
```bash
sudo systemctl daemon-reload
sudo systemctl restart the-xperience-back
sudo systemctl status the-xperience-back
```

### Option 2: Environment File

Create an environment file and source it before starting the application:

1. **Create `/opt/the-xperience-back/.env`** (or similar location):
```bash
export OPENAI_API_KEY=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
```

2. **Update your systemd service to source the file:**
```ini
[Service]
EnvironmentFile=/opt/the-xperience-back/.env
ExecStart=/usr/bin/java -jar /opt/the-xperience-back/the-xperience-back-0.0.1-SNAPSHOT.jar
```

3. **Make the file secure:**
```bash
chmod 600 /opt/the-xperience-back/.env
chown your-user:your-user /opt/the-xperience-back/.env
```

### Option 3: Direct Command Line

If starting manually:
```bash
export OPENAI_API_KEY=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
java -jar the-xperience-back-0.0.1-SNAPSHOT.jar
```

### Option 4: Docker (if using Docker)

In your `docker-compose.yml` or Dockerfile:
```yaml
services:
  backend:
    environment:
      - OPENAI_API_KEY=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
```

Or use a `.env` file with Docker:
```bash
# .env file
OPENAI_API_KEY=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
```

## Verification

After setting the environment variable, verify it's working:

1. **Check if the variable is set:**
```bash
echo $OPENAI_API_KEY
```

2. **Test the CV Scorer endpoint:**
```bash
curl -X POST https://api.the-xperience.com/api/score-cv \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/test-cv.pdf" \
  -F "targetRole=Software Developer"
```

3. **Check application logs:**
```bash
sudo journalctl -u the-xperience-back -f
```

If you see errors about missing `OPENAI_API_KEY`, the environment variable is not set correctly.

## Security Best Practices

1. **Never commit the API key to version control**
2. **Use environment variables, not hardcoded values**
3. **Restrict file permissions** (chmod 600) for any files containing the key
4. **Rotate the key periodically** if compromised
5. **Use secrets management** (AWS Secrets Manager, HashiCorp Vault, etc.) for enterprise deployments

## Troubleshooting

### Error: "Could not resolve placeholder 'OPENAI_API_KEY'"

**Solution:** The environment variable is not set. Use one of the methods above to set it.

### Error: "OpenAI API error: 401"

**Solution:** The API key is invalid or expired. Check the key and regenerate if needed.

### Error: "OpenAI API error: 429"

**Solution:** Rate limit exceeded. Wait a few minutes or upgrade your OpenAI plan.

## Quick Setup Script

For quick setup on a Linux server:

```bash
#!/bin/bash
# Quick setup script for OpenAI API key

API_KEY="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"
SERVICE_FILE="/etc/systemd/system/the-xperience-back.service"

# Add to systemd service
sudo sed -i "/\[Service\]/a Environment=\"OPENAI_API_KEY=$API_KEY\"" $SERVICE_FILE

# Reload and restart
sudo systemctl daemon-reload
sudo systemctl restart the-xperience-back

echo "✓ OpenAI API key configured"
```




