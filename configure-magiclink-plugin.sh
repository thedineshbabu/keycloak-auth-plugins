#!/bin/bash

# Autologin Plugin Configuration Script
# This script configures the autologin plugin in Keycloak

set -e

# Configuration variables
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM_NAME="${REALM_NAME:-myrealm}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"

# Get admin token
echo "Getting admin token..."
ADMIN_TOKEN=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USERNAME}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "Failed to get admin token. Check your credentials and Keycloak URL."
    exit 1
fi

echo "Admin token obtained successfully."

# Configure autologin plugin
echo "Configuring autologin plugin..."

# Set magiclink configuration attributes
curl -s -X PUT \
  "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "magiclink.enabled": "true",
      "magiclink.external.api.endpoint": "http://localhost:3001/api/v1/magiclink/send",
      "magiclink.external.api.token": "your-api-token",
      "magiclink.external.api.type": "bearer",
      "magiclink.base.url": "'${KEYCLOAK_URL}'",
      "magiclink.token.expiry.minutes": "15",
      "magiclink.rate.limit.enabled": "true",
      "magiclink.rate.limit.requests": "10",
      "magiclink.rate.limit.window": "60",
      "magiclink.allowed.redirect.urls": "http://localhost:3000,https://myapp.com,http://127.0.0.1:3000"
    }
  }'

echo "Autologin plugin configured successfully!"

# Test the configuration
echo "Testing autologin configuration..."
curl -s -X GET \
  "${KEYCLOAK_URL}/realms/${REALM_NAME}/magiclink/health" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq '.'

echo "Configuration complete!"
echo ""
echo "Allowed redirect URLs:"
echo "- http://localhost:3000"
echo "- https://myapp.com" 
echo "- http://127.0.0.1:3000"
echo ""
echo "To add more URLs, update the magiclink.allowed.redirect.urls attribute in Keycloak Admin Console"
echo "or modify this script and run it again." 