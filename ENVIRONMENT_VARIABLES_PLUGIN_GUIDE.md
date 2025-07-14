# Environment Variables Plugin for Keycloak

## Overview

The Environment Variables Plugin is a custom Keycloak authenticator that logs all environment variables and system properties during authentication. This plugin is particularly useful for debugging, environment verification, and understanding the Keycloak runtime environment.

## Features

- **Comprehensive Logging**: Logs all system environment variables and their values
- **Security**: Automatically masks sensitive environment variables (passwords, keys, tokens)
- **Flexible Configuration**: Can be used in any authentication flow
- **Multiple Logging Options**: Supports different logging levels and filtering
- **Easy Integration**: Simple to add to existing authentication flows
- **Debugging Support**: Helps identify environment-related issues

## Architecture

### Components

1. **EnvVarsAuthenticator**: Main authenticator class that implements the authentication logic
2. **EnvVarsAuthenticatorFactory**: Factory class for creating authenticator instances
3. **EnvVarsLogger**: Utility class for logging environment variables with security features

### Class Structure

```
kf.keycloak.plugin.provider
├── EnvVarsAuthenticator.java          # Main authenticator implementation
├── EnvVarsAuthenticatorFactory.java   # Factory for creating authenticators
└── util/
    └── EnvVarsLogger.java            # Logging utility with security features
```

### Key Features

- **Initialization Tracking**: Ensures environment variables are logged only once per authenticator instance
- **Sensitive Data Masking**: Automatically masks passwords, keys, and other sensitive information
- **Comprehensive Logging**: Logs both environment variables and system properties
- **Flexible Integration**: Can be added to any authentication flow as a required or optional step

## Installation

### Prerequisites

- Keycloak 21.0 or higher
- Java 11 or higher
- Maven 3.6 or higher

### Build the Plugin

1. **Clone the repository** (if not already done):
   ```bash
   git clone <repository-url>
   cd keycloak-auth-plugins
   ```

2. **Build the plugin**:
   ```bash
   mvn clean package
   ```

3. **Copy the JAR file** to Keycloak's providers directory:
   ```bash
   cp target/keycloak-auth-plugins-1.0.0.jar /path/to/keycloak/providers/
   ```

### Deploy to Keycloak

1. **Stop Keycloak** (if running):
   ```bash
   # For standalone mode
   ./kc.sh stop
   
   # For Docker
   docker stop keycloak
   ```

2. **Copy the plugin JAR**:
   ```bash
   cp target/keycloak-auth-plugins-1.0.0.jar /path/to/keycloak/providers/
   ```

3. **Start Keycloak**:
   ```bash
   # For standalone mode
   ./kc.sh start-dev
   
   # For Docker
   docker start keycloak
   ```

## Configuration

### Using the Configuration Script

The easiest way to configure the plugin is using the provided script:

```bash
# Make the script executable
chmod +x configure-env-vars-plugin.sh

# Run the configuration script
./configure-env-vars-plugin.sh
```

This script will:
- Create a test realm (`env-vars-test`)
- Create a test client (`env-vars-client`)
- Create a test user (`env-vars-user`)
- Create an authentication flow with the Environment Variables Authenticator
- Configure the client to use the new flow

### Manual Configuration

#### 1. Create a Test Realm

```bash
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "env-vars-test",
    "enabled": true,
    "displayName": "Environment Variables Test Realm"
  }' \
  "http://localhost:8080/admin/realms"
```

#### 2. Create a Test Client

```bash
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "env-vars-client",
    "enabled": true,
    "publicClient": true,
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": true,
    "redirectUris": ["http://localhost:3000/*"],
    "webOrigins": ["http://localhost:3000"]
  }' \
  "http://localhost:8080/admin/realms/env-vars-test/clients"
```

#### 3. Create a Test User

```bash
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
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
  }' \
  "http://localhost:8080/admin/realms/env-vars-test/users"
```

#### 4. Create Authentication Flow

```bash
# Create the flow
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alias": "env-vars-flow",
    "description": "Authentication flow with Environment Variables Logger",
    "providerId": "basic-flow",
    "topLevel": true,
    "builtIn": false
  }' \
  "http://localhost:8080/admin/realms/env-vars-test/authentication/flows"

# Add the Environment Variables Authenticator
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "env-vars-authenticator",
    "requirement": "REQUIRED"
  }' \
  "http://localhost:8080/admin/realms/env-vars-test/authentication/flows/env-vars-flow/executions/execution"
```

#### 5. Configure Client to Use the Flow

```bash
# Get the client ID first
CLIENT_ID=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://localhost:8080/admin/realms/env-vars-test/clients?clientId=env-vars-client" | \
  jq -r '.[0].id')

# Update the client
curl -X PUT \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "authenticationFlowBindingOverrides": {
      "browser": "env-vars-flow",
      "direct_grant": "env-vars-flow"
    }
  }' \
  "http://localhost:8080/admin/realms/env-vars-test/clients/$CLIENT_ID"
```

## Testing

### Using the Test Script

The provided test script makes it easy to verify the plugin functionality:

```bash
# Make the script executable
chmod +x test-env-vars-plugin.sh

# Run the test
./test-env-vars-plugin.sh
```

### Manual Testing

#### 1. Direct Grant Flow Test

```bash
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=env-vars-client" \
  -d "username=env-vars-user" \
  -d "password=password123" \
  "http://localhost:8080/realms/env-vars-test/protocol/openid-connect/token"
```

#### 2. Browser Flow Test

Visit the following URL in a browser:
```
http://localhost:8080/realms/env-vars-test/account
```

Login with:
- Username: `env-vars-user`
- Password: `password123`

### Checking Logs

The plugin will log environment variables to Keycloak's standard logging system. Look for:

1. **Log entries starting with**:
   - `=== ENVIRONMENT VARIABLES START ===`
   - `ENV: VARIABLE_NAME = value`
   - `Environment Variables Authenticator - Authentication requested`

2. **Common log locations**:
   - Console output (if running in foreground)
   - Keycloak server logs directory
   - Docker logs (if running in container)

3. **Example log output**:
   ```
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) === ENVIRONMENT VARIABLES START ===
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) ================================================================================
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) Total environment variables found: 45
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) 
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) ENV: HOME = /home/user
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) ENV: JAVA_HOME = /usr/lib/jvm/java-11
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) ENV: PATH = /usr/local/bin:/usr/bin:/bin
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) ENV: KC_DB_PASSWORD = pass***word
   ```

## Security Features

### Sensitive Data Masking

The plugin automatically masks sensitive environment variables to prevent exposure of confidential information:

**Masked Keywords**:
- `password`, `passwd`
- `secret`, `key`, `token`
- `credential`, `auth`
- `private`, `certificate`, `cert`
- `keystore`, `truststore`, `jwt`
- `api_key`, `apikey`
- `access_key`, `secret_key`, `private_key`

**Masking Examples**:
- `DB_PASSWORD=secret123` → `DB_PASSWORD=sec***123`
- `API_KEY=abcdef123456` → `API_KEY=abcd***3456`
- `SECRET=short` → `SECRET=***`

### Logging Levels

The plugin supports different logging levels:

- **INFO**: Standard environment variable logging
- **DEBUG**: Detailed debugging information
- **WARN**: Warning messages
- **ERROR**: Error messages with stack traces

## Advanced Usage

### Custom Logging Patterns

The `EnvVarsLogger` class provides methods for custom logging:

```java
// Log all environment variables
logger.logAllEnvironmentVariables();

// Log only Keycloak-related variables
logger.logKeycloakEnvironmentVariables();

// Log variables matching specific patterns
logger.logEnvironmentVariablesByPattern("JAVA", "KC_", "DB_");

// Log all system properties
logger.logAllSystemProperties();
```

### Integration with Existing Flows

The Environment Variables Authenticator can be integrated into existing authentication flows:

1. **Add to existing flow**:
   - Go to Authentication → Flows
   - Select your existing flow
   - Add execution → Environment Variables Logger
   - Set requirement to "REQUIRED" or "ALTERNATIVE"

2. **Use as debugging step**:
   - Add to flow temporarily for debugging
   - Set requirement to "DISABLED" when not needed
   - Re-enable when debugging is required

### Conditional Logging

You can modify the authenticator to log only under certain conditions:

```java
// Example: Log only for specific users
if (context.getUser() != null && 
    context.getUser().getUsername().equals("admin")) {
    logger.logAllEnvironmentVariables();
}
```

## Troubleshooting

### Common Issues

1. **Plugin not appearing in Keycloak**:
   - Verify the JAR file is in the correct providers directory
   - Check that Keycloak was restarted after deployment
   - Verify the plugin is listed in the services file

2. **No environment variables logged**:
   - Check that the authenticator is added to the authentication flow
   - Verify the flow is assigned to the client
   - Check Keycloak logs for any errors

3. **Authentication flow errors**:
   - Ensure the authenticator is properly configured
   - Check that all required dependencies are available
   - Verify the plugin is compatible with your Keycloak version

### Debug Steps

1. **Check plugin loading**:
   ```bash
   # Look for plugin loading messages in Keycloak startup logs
   grep -i "env-vars" /path/to/keycloak/logs/server.log
   ```

2. **Verify authenticator registration**:
   - Go to Authentication → Flows
   - Try to add a new execution
   - Look for "Environment Variables Logger" in the list

3. **Test with minimal configuration**:
   - Create a simple test flow with only the Environment Variables Authenticator
   - Test with direct grant flow first
   - Gradually add other authenticators

### Log Analysis

When analyzing logs, look for:

1. **Successful plugin loading**:
   ```
   INFO  [org.keycloak.services] (main) Loaded providers from /path/to/keycloak/providers
   ```

2. **Authenticator execution**:
   ```
   INFO  [kf.keycloak.plugin.provider.EnvVarsAuthenticator] (default task-1) Environment Variables Authenticator - Authentication requested
   ```

3. **Environment variable output**:
   ```
   INFO  [kf.keycloak.plugin.util.EnvVarsLogger] (default task-1) === ENVIRONMENT VARIABLES START ===
   ```

## Performance Considerations

### Logging Impact

- **Memory usage**: Minimal impact as logging is done once per authenticator instance
- **Performance**: Negligible impact on authentication flow
- **Storage**: Logs are written to standard Keycloak log files

### Best Practices

1. **Use sparingly**: Only enable when debugging is needed
2. **Monitor log size**: Environment variable logs can be verbose
3. **Secure logging**: Ensure log files are properly secured
4. **Cleanup**: Remove from production flows after debugging

## Development

### Extending the Plugin

To extend the plugin functionality:

1. **Add new logging methods** to `EnvVarsLogger`:
   ```java
   public void logCustomEnvironmentVariables(String... patterns) {
       // Custom logging logic
   }
   ```

2. **Modify the authenticator** to use custom logging:
   ```java
   // In EnvVarsAuthenticator.authenticate()
   logger.logCustomEnvironmentVariables("CUSTOM_", "SPECIFIC_");
   ```

3. **Add configuration options** to the factory:
   ```java
   // In EnvVarsAuthenticatorFactory.getConfigProperties()
   // Add new configuration properties
   ```

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd keycloak-auth-plugins
   ```

2. **Build the project**:
   ```bash
   mvn clean package
   ```

3. **Run tests** (if available):
   ```bash
   mvn test
   ```

4. **Deploy the plugin**:
   ```bash
   cp target/keycloak-auth-plugins-1.0.0.jar /path/to/keycloak/providers/
   ```

## API Reference

### EnvVarsAuthenticator

Main authenticator class that implements the authentication logic.

**Methods**:
- `authenticate(AuthenticationFlowContext context)`: Main authentication method
- `action(AuthenticationFlowContext context)`: Handle form submissions
- `requiresUser()`: Returns false (no user required)
- `configuredFor(...)`: Returns true (always configured)

### EnvVarsAuthenticatorFactory

Factory class for creating authenticator instances.

**Constants**:
- `PROVIDER_ID`: "env-vars-authenticator"
- `DISPLAY_TYPE`: "Environment Variables Logger"
- `REFERENCE_CATEGORY`: "env-vars"

### EnvVarsLogger

Utility class for logging environment variables with security features.

**Methods**:
- `logAllEnvironmentVariables()`: Log all environment variables
- `logAllSystemProperties()`: Log all system properties
- `logKeycloakEnvironmentVariables()`: Log Keycloak-related variables
- `logEnvironmentVariablesByPattern(String... patterns)`: Log filtered variables
- `maskSensitiveValue(String key, String value)`: Mask sensitive values

## Conclusion

The Environment Variables Plugin provides a powerful tool for debugging and understanding the Keycloak runtime environment. With its comprehensive logging capabilities, security features, and easy integration, it's an essential tool for Keycloak administrators and developers.

The plugin is particularly useful for:
- Debugging environment-related issues
- Verifying configuration settings
- Understanding the Keycloak runtime environment
- Troubleshooting authentication problems
- Security auditing and compliance

By following this guide, you can successfully install, configure, and use the Environment Variables Plugin in your Keycloak environment. 