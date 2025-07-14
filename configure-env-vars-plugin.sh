#!/bin/bash

# Environment Variables Plugin Configuration Script
# This script configures the Environment Variables Authenticator in Keycloak

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration variables
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"
REALM_NAME="${REALM_NAME:-master}"
CLIENT_ID="${CLIENT_ID:-admin-cli}"

echo -e "${BLUE}=== Environment Variables Plugin Configuration ===${NC}"
echo -e "${YELLOW}Keycloak URL:${NC} $KEYCLOAK_URL"
echo -e "${YELLOW}Admin Username:${NC} $ADMIN_USERNAME"
echo -e "${YELLOW}Realm:${NC} $REALM_NAME"
echo ""

# Function to get access token
get_access_token() {
    echo -e "${BLUE}Getting access token...${NC}"
    
    TOKEN_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "username=$ADMIN_USERNAME" \
        -d "password=$ADMIN_PASSWORD" \
        "$KEYCLOAK_URL/realms/$REALM_NAME/protocol/openid-connect/token")
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to get access token${NC}"
        exit 1
    fi
    
    ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')
    
    if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
        echo -e "${RED}Failed to extract access token from response${NC}"
        echo "Response: $TOKEN_RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}Access token obtained successfully${NC}"
}

# Function to create a test realm
create_test_realm() {
    echo -e "${BLUE}Creating test realm 'env-vars-test'...${NC}"
    
    REALM_DATA='{
        "realm": "env-vars-test",
        "enabled": true,
        "displayName": "Environment Variables Test Realm"
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$REALM_DATA" \
        "$KEYCLOAK_URL/admin/realms")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}Test realm created successfully${NC}"
    elif [ "$HTTP_CODE" = "409" ]; then
        echo -e "${YELLOW}Test realm already exists${NC}"
    else
        echo -e "${RED}Failed to create test realm. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
    fi
}

# Function to create a test client
create_test_client() {
    echo -e "${BLUE}Creating test client 'env-vars-client'...${NC}"
    
    CLIENT_DATA='{
        "clientId": "env-vars-client",
        "enabled": true,
        "publicClient": true,
        "standardFlowEnabled": true,
        "directAccessGrantsEnabled": true,
        "redirectUris": ["http://localhost:3000/*"],
        "webOrigins": ["http://localhost:3000"]
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$CLIENT_DATA" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/clients")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}Test client created successfully${NC}"
    elif [ "$HTTP_CODE" = "409" ]; then
        echo -e "${YELLOW}Test client already exists${NC}"
    else
        echo -e "${RED}Failed to create test client. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
    fi
}

# Function to create a test user
create_test_user() {
    echo -e "${BLUE}Creating test user 'env-vars-user'...${NC}"
    
    USER_DATA='{
        "username": "env-vars-user",
        "enabled": true,
        "emailVerified": true,
        "firstName": "Environment",
        "lastName": "Variables",
        "email": "env-vars-user@example.com",
        "credentials": [{
            "type": "password",
            "value": "password123",
            "temporary": false
        }]
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$USER_DATA" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/users")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}Test user created successfully${NC}"
    elif [ "$HTTP_CODE" = "409" ]; then
        echo -e "${YELLOW}Test user already exists${NC}"
    else
        echo -e "${RED}Failed to create test user. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
    fi
}

# Function to create authentication flow with environment variables authenticator
create_auth_flow() {
    echo -e "${BLUE}Creating authentication flow with Environment Variables Authenticator...${NC}"
    
    # Create the authentication flow
    FLOW_DATA='{
        "alias": "env-vars-flow",
        "description": "Authentication flow with Environment Variables Logger",
        "providerId": "basic-flow",
        "topLevel": true,
        "builtIn": false
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$FLOW_DATA" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/authentication/flows")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}Authentication flow created successfully${NC}"
    elif [ "$HTTP_CODE" = "409" ]; then
        echo -e "${YELLOW}Authentication flow already exists${NC}"
    else
        echo -e "${RED}Failed to create authentication flow. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
        return 1
    fi
    
    # Add the Environment Variables Authenticator to the flow
    EXECUTION_DATA='{
        "provider": "env-vars-authenticator",
        "requirement": "REQUIRED"
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$EXECUTION_DATA" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/authentication/flows/env-vars-flow/executions/execution")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}Environment Variables Authenticator added to flow successfully${NC}"
    else
        echo -e "${RED}Failed to add Environment Variables Authenticator to flow. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
        return 1
    fi
}

# Function to configure client to use the new flow
configure_client_flow() {
    echo -e "${BLUE}Configuring client to use the new authentication flow...${NC}"
    
    # Get the client ID
    CLIENT_RESPONSE=$(curl -s -X GET \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/clients?clientId=env-vars-client")
    
    CLIENT_ID_VALUE=$(echo $CLIENT_RESPONSE | jq -r '.[0].id')
    
    if [ "$CLIENT_ID_VALUE" = "null" ] || [ -z "$CLIENT_ID_VALUE" ]; then
        echo -e "${RED}Failed to get client ID${NC}"
        return 1
    fi
    
    # Update client to use the new flow
    CLIENT_UPDATE_DATA='{
        "authenticationFlowBindingOverrides": {
            "browser": "env-vars-flow",
            "direct_grant": "env-vars-flow"
        }
    }'
    
    RESPONSE=$(curl -s -w "%{http_code}" -X PUT \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$CLIENT_UPDATE_DATA" \
        "$KEYCLOAK_URL/admin/realms/env-vars-test/clients/$CLIENT_ID_VALUE")
    
    HTTP_CODE="${RESPONSE: -3}"
    RESPONSE_BODY="${RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "204" ]; then
        echo -e "${GREEN}Client configured to use new authentication flow${NC}"
    else
        echo -e "${RED}Failed to configure client. HTTP Code: $HTTP_CODE${NC}"
        echo "Response: $RESPONSE_BODY"
        return 1
    fi
}

# Function to test the environment variables plugin
test_plugin() {
    echo -e "${BLUE}Testing the Environment Variables Plugin...${NC}"
    echo -e "${YELLOW}This will trigger the authenticator and log environment variables${NC}"
    
    # Test direct grant flow
    echo -e "${BLUE}Testing direct grant flow...${NC}"
    
    GRANT_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=env-vars-client" \
        -d "username=env-vars-user" \
        -d "password=password123" \
        "$KEYCLOAK_URL/realms/env-vars-test/protocol/openid-connect/token")
    
    if [ $? -eq 0 ]; then
        ERROR=$(echo $GRANT_RESPONSE | jq -r '.error')
        if [ "$ERROR" = "null" ]; then
            echo -e "${GREEN}Direct grant flow test successful${NC}"
            echo -e "${YELLOW}Check Keycloak logs for environment variables output${NC}"
        else
            echo -e "${YELLOW}Direct grant flow test completed (may have expected errors)${NC}"
            echo "Response: $GRANT_RESPONSE"
        fi
    else
        echo -e "${RED}Direct grant flow test failed${NC}"
    fi
}

# Main execution
main() {
    echo -e "${BLUE}Starting Environment Variables Plugin Configuration...${NC}"
    
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
    
    get_access_token
    create_test_realm
    create_test_client
    create_test_user
    create_auth_flow
    configure_client_flow
    test_plugin
    
    echo ""
    echo -e "${GREEN}=== Environment Variables Plugin Configuration Complete ===${NC}"
    echo ""
    echo -e "${BLUE}Configuration Summary:${NC}"
    echo -e "${YELLOW}Test Realm:${NC} env-vars-test"
    echo -e "${YELLOW}Test Client:${NC} env-vars-client"
    echo -e "${YELLOW}Test User:${NC} env-vars-user / password123"
    echo -e "${YELLOW}Authentication Flow:${NC} env-vars-flow"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Check Keycloak logs for environment variables output"
    echo "2. Test browser flow by visiting: http://localhost:8080/realms/env-vars-test/account"
    echo "3. Monitor logs during authentication attempts"
    echo ""
    echo -e "${YELLOW}Note: The Environment Variables Authenticator will log all environment variables${NC}"
    echo -e "${YELLOW}during authentication. Check Keycloak server logs for the output.${NC}"
}

# Run main function
main "$@" 