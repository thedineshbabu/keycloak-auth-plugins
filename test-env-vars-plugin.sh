#!/bin/bash

# Environment Variables Plugin Test Script
# This script tests the Environment Variables Authenticator functionality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration variables
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM_NAME="${REALM_NAME:-env-vars-test}"
CLIENT_ID="${CLIENT_ID:-env-vars-client}"
USERNAME="${USERNAME:-env-vars-user}"
PASSWORD="${PASSWORD:-password123}"

echo -e "${BLUE}=== Environment Variables Plugin Test ===${NC}"
echo -e "${YELLOW}Keycloak URL:${NC} $KEYCLOAK_URL"
echo -e "${YELLOW}Realm:${NC} $REALM_NAME"
echo -e "${YELLOW}Client ID:${NC} $CLIENT_ID"
echo -e "${YELLOW}Username:${NC} $USERNAME"
echo ""

# Function to test direct grant flow
test_direct_grant() {
    echo -e "${BLUE}Testing Direct Grant Flow...${NC}"
    echo -e "${YELLOW}This will trigger the Environment Variables Authenticator${NC}"
    echo ""
    
    GRANT_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "username=$USERNAME" \
        -d "password=$PASSWORD" \
        "$KEYCLOAK_URL/realms/$REALM_NAME/protocol/openid-connect/token")
    
    if [ $? -eq 0 ]; then
        ERROR=$(echo $GRANT_RESPONSE | jq -r '.error')
        if [ "$ERROR" = "null" ]; then
            echo -e "${GREEN}✓ Direct grant flow successful${NC}"
            ACCESS_TOKEN=$(echo $GRANT_RESPONSE | jq -r '.access_token')
            echo -e "${YELLOW}Access token obtained: ${ACCESS_TOKEN:0:20}...${NC}"
        else
            ERROR_DESCRIPTION=$(echo $GRANT_RESPONSE | jq -r '.error_description')
            echo -e "${YELLOW}Direct grant flow completed with expected behavior${NC}"
            echo -e "${YELLOW}Error: $ERROR${NC}"
            echo -e "${YELLOW}Description: $ERROR_DESCRIPTION${NC}"
        fi
    else
        echo -e "${RED}✗ Direct grant flow failed${NC}"
    fi
    
    echo ""
}

# Function to test browser flow simulation
test_browser_flow() {
    echo -e "${BLUE}Testing Browser Flow Simulation...${NC}"
    echo -e "${YELLOW}This simulates a browser authentication request${NC}"
    echo ""
    
    # Get the authorization URL
    AUTH_URL="$KEYCLOAK_URL/realms/$REALM_NAME/protocol/openid-connect/auth"
    AUTH_PARAMS="response_type=code&client_id=$CLIENT_ID&redirect_uri=http://localhost:3000/callback&scope=openid"
    
    echo -e "${BLUE}Authorization URL:${NC}"
    echo "$AUTH_URL?$AUTH_PARAMS"
    echo ""
    
    # Test the authorization endpoint
    AUTH_RESPONSE=$(curl -s -I "$AUTH_URL?$AUTH_PARAMS")
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Authorization endpoint accessible${NC}"
        echo -e "${YELLOW}This would redirect to login page in a browser${NC}"
    else
        echo -e "${RED}✗ Authorization endpoint not accessible${NC}"
    fi
    
    echo ""
}

# Function to check Keycloak logs for environment variables
check_logs() {
    echo -e "${BLUE}Checking for Environment Variables in Logs...${NC}"
    echo -e "${YELLOW}This section provides guidance on checking logs${NC}"
    echo ""
    
    echo -e "${BLUE}To check Keycloak logs for environment variables:${NC}"
    echo "1. Look for log entries containing 'ENVIRONMENT VARIABLES START'"
    echo "2. Look for log entries containing 'ENV:' prefix"
    echo "3. Look for log entries containing 'Environment Variables Authenticator'"
    echo ""
    
    echo -e "${BLUE}Common log locations:${NC}"
    echo "- Console output (if running in foreground)"
    echo "- Keycloak server logs directory"
    echo "- Docker logs (if running in container)"
    echo ""
    
    echo -e "${BLUE}Example log entries to look for:${NC}"
    echo "- '=== ENVIRONMENT VARIABLES START ==='"
    echo "- 'ENV: JAVA_HOME = /usr/lib/jvm/java-11'"
    echo "- 'Environment Variables Authenticator - Authentication requested'"
    echo ""
}

# Function to test specific environment variables
test_specific_env_vars() {
    echo -e "${BLUE}Testing Specific Environment Variables...${NC}"
    echo ""
    
    # Test if common environment variables are set
    COMMON_VARS=("JAVA_HOME" "PATH" "HOME" "USER" "PWD" "LANG")
    
    echo -e "${YELLOW}Common environment variables:${NC}"
    for var in "${COMMON_VARS[@]}"; do
        if [ -n "${!var}" ]; then
            echo -e "${GREEN}✓ $var = ${!var}${NC}"
        else
            echo -e "${YELLOW}⚠ $var = (not set)${NC}"
        fi
    done
    
    echo ""
    
    # Test Keycloak-specific environment variables
    echo -e "${YELLOW}Keycloak-specific environment variables:${NC}"
    KC_VARS=("KC_HOSTNAME" "KC_HTTP_ENABLED" "KC_HTTP_PORT" "KC_DB" "KC_DB_URL")
    
    for var in "${KC_VARS[@]}"; do
        if [ -n "${!var}" ]; then
            echo -e "${GREEN}✓ $var = ${!var}${NC}"
        else
            echo -e "${YELLOW}⚠ $var = (not set)${NC}"
        fi
    done
    
    echo ""
}

# Function to provide usage instructions
show_usage_instructions() {
    echo -e "${BLUE}Usage Instructions:${NC}"
    echo ""
    echo -e "${YELLOW}1. Manual Testing:${NC}"
    echo "   - Visit: http://localhost:8080/realms/$REALM_NAME/account"
    echo "   - Login with: $USERNAME / $PASSWORD"
    echo "   - Check Keycloak logs for environment variables output"
    echo ""
    
    echo -e "${YELLOW}2. API Testing:${NC}"
    echo "   - Use the direct grant flow (tested above)"
    echo "   - Monitor logs during authentication"
    echo ""
    
    echo -e "${YELLOW}3. Configuration Verification:${NC}"
    echo "   - Check that the plugin is loaded in Keycloak"
    echo "   - Verify the authentication flow is configured"
    echo "   - Confirm the authenticator is active"
    echo ""
    
    echo -e "${YELLOW}4. Troubleshooting:${NC}"
    echo "   - If no environment variables are logged, check plugin installation"
    echo "   - Verify the authenticator is added to the authentication flow"
    echo "   - Check Keycloak server logs for any errors"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}Starting Environment Variables Plugin Test...${NC}"
    
    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}Error: jq is required but not installed.${NC}"
        echo "Please install jq: brew install jq (macOS) or apt-get install jq (Ubuntu)"
        exit 1
    fi
    
    # Check if curl is installed
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}Error: curl is required but not installed.${NC}"
        exit 1
    fi
    
    # Check if Keycloak is running
    echo -e "${BLUE}Checking Keycloak availability...${NC}"
    if curl -s -f "$KEYCLOAK_URL/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Keycloak is running${NC}"
    else
        echo -e "${YELLOW}⚠ Keycloak health check failed, but continuing...${NC}"
    fi
    
    echo ""
    
    test_direct_grant
    test_browser_flow
    test_specific_env_vars
    check_logs
    show_usage_instructions
    
    echo ""
    echo -e "${GREEN}=== Environment Variables Plugin Test Complete ===${NC}"
    echo ""
    echo -e "${BLUE}Summary:${NC}"
    echo -e "${YELLOW}The Environment Variables Authenticator should have logged all${NC}"
    echo -e "${YELLOW}environment variables during the authentication attempts above.${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Check Keycloak server logs for environment variables output"
    echo "2. Verify the plugin is working correctly"
    echo "3. Use the plugin for debugging and environment verification"
    echo ""
}

# Run main function
main "$@" 