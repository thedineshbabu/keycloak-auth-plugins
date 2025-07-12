# Keycloak Magiclink Plugin

A custom Keycloak plugin that provides magiclink authentication functionality. This plugin allows users to authenticate via secure, time-limited magiclinks sent to external APIs.

## Features

- **Secure JWT-based magiclinks** with HMAC-SHA256 signing
- **External API integration** for sending magiclinks
- **Rate limiting** to prevent abuse
- **One-time use tokens** to prevent replay attacks
- **Configurable expiration** (1-60 minutes)
- **Comprehensive logging** with structured Winston-style output
- **REST API** for magiclink generation and management
- **Keycloak authentication flow** integration

## Project Structure

```
src/main/java/kf/keycloak/plugin/
├── config/
│   └── MagiclinkConfig.java          # Configuration management
├── model/
│   ├── MagiclinkRequest.java         # Request model
│   └── MagiclinkResponse.java        # Response model
├── provider/
│   ├── MagiclinkResourceProvider.java     # REST endpoints
│   ├── MagiclinkResourceProviderFactory.java
│   ├── MagiclinkAuthenticator.java        # Authentication flow
│   └── MagiclinkAuthenticatorFactory.java
├── service/
│   ├── MagiclinkService.java         # Main orchestration service
│   ├── TokenService.java             # JWT token management
│   └── ExternalApiService.java       # External API integration
└── util/
    └── MagiclinkLogger.java          # Winston-style logging
```

## API Endpoints

### Generate Magiclink
```
POST /auth/realms/{realm}/magiclink/generate
Content-Type: application/json

{
  "email": "user@example.com",
  "redirectUrl": "https://myapp.com/dashboard",
  "expirationMinutes": 15,
  "clientId": "my-app"
}
```

### Authenticate with Magiclink
```
GET /auth/realms/{realm}/magiclink/authenticate?token={jwt_token}
```

### Check Token Status
```
GET /auth/realms/{realm}/magiclink/status?token={jwt_token}
```

### Configuration Management
```
GET /auth/realms/{realm}/magiclink/config
PUT /auth/realms/{realm}/magiclink/config
```

### Test External API
```
GET /auth/realms/{realm}/magiclink/test-api
```

### Health Check
```
GET /auth/realms/{realm}/magiclink/health
```

## Configuration

Configure the plugin through realm attributes:

| Attribute | Description | Default |
|-----------|-------------|---------|
| `magiclink.enabled` | Enable/disable magiclink functionality | `true` |
| `magiclink.external.api.endpoint` | External API endpoint URL | - |
| `magiclink.external.api.token` | API authentication token | - |
| `magiclink.external.api.type` | Auth type (bearer, basic, apikey) | `bearer` |
| `magiclink.base.url` | Base URL for magiclink generation | Auto-detected |
| `magiclink.token.expiry.minutes` | Token expiration time | `15` |
| `magiclink.rate.limit.enabled` | Enable rate limiting | `true` |
| `magiclink.rate.limit.requests` | Requests per window | `10` |
| `magiclink.rate.limit.window` | Rate limit window (seconds) | `60` |
| `magiclink.allowed.redirect.urls` | Allowed redirect URLs (comma-separated) | - |

## Installation

### 1. Build the Plugin

```bash
mvn clean package
```

### 2. Deploy to Keycloak

Copy the built JAR to your Keycloak deployment:

```bash
# For standalone deployment
cp target/magiclink-keycloak-plugin-1.0.0.jar $KEYCLOAK_HOME/providers/

# For Docker deployment
# Add to Dockerfile or mount as volume
```

### 3. Restart Keycloak

```bash
# Standalone
$KEYCLOAK_HOME/bin/kc.sh start

# Docker
docker-compose restart keycloak
```

### 4. Configure Authentication Flow

1. Go to **Authentication** → **Flows** in the Keycloak admin console
2. Create a new flow or copy an existing one
3. Add the **Magiclink Authenticator** execution
4. Configure the execution as **Required** or **Alternative**

## Usage Examples

### Generate Magiclink

```bash
curl -X POST \
  http://localhost:8080/auth/realms/myrealm/magiclink/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "user@example.com",
    "redirectUrl": "https://myapp.com/dashboard",
    "expirationMinutes": 15
  }'
```

### Authenticate with Magiclink

```bash
curl -X GET \
  'http://localhost:8080/auth/realms/myrealm/magiclink/authenticate?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
```

### Check Health

```bash
curl -X GET \
  http://localhost:8080/auth/realms/myrealm/magiclink/health
```

## External API Integration

The plugin can send magiclinks to external APIs. Configure the external API settings:

```json
{
  "endpoint": "https://api.example.com/send-magiclink",
  "token": "your-api-token",
  "type": "bearer"
}
```

The plugin will POST the following payload to your external API:

```json
{
  "email": "user@example.com",
  "magiclink": "https://keycloak.example.com/auth/realms/myrealm/magiclink/authenticate?token=...",
  "tokenId": "unique-token-id",
  "userId": "keycloak-user-id",
  "timestamp": "2024-01-01T12:00:00Z",
  "source": "keycloak-magiclink-plugin"
}
```

## Security Features

- **HMAC-SHA256 signing** for JWT tokens
- **One-time use tokens** to prevent replay attacks
- **Time-based expiration** (1-60 minutes)
- **Rate limiting** to prevent abuse
- **Redirect URL validation** to prevent open redirects
- **Comprehensive logging** for security monitoring

## Error Handling

The plugin provides detailed error responses:

```json
{
  "success": false,
  "error": "User not found",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

Common error codes:
- `VALIDATION_ERROR`: Invalid request parameters
- `USER_NOT_FOUND`: Email not found in realm
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `REDIRECT_URL_NOT_ALLOWED`: Redirect URL not whitelisted
- `EXTERNAL_API_ERROR`: External API call failed
- `FEATURE_DISABLED`: Magiclink feature is disabled

## Logging

The plugin uses Winston-style structured logging:

```
2024-01-01 12:00:00.000 [INFO] MagiclinkService: Magiclink generated successfully userId=123 email=user@example.com tokenId=abc123 operation=token_generation realm=myrealm
```

## Development

### Requirements

- Java 11+
- Maven 3.6+
- Keycloak 25.0.2+

### Build

```bash
mvn clean compile
```

### Test

```bash
mvn test
```

### Package

```bash
mvn package
```

## Configuration Examples

### Basic Configuration

```bash
# Enable magiclink
curl -X PUT \
  http://localhost:8080/auth/realms/myrealm/magiclink/config \
  -H 'Content-Type: application/json' \
  -d '{
    "enabled": "true",
    "tokenExpiryMinutes": "15",
    "rateLimitEnabled": "true",
    "rateLimitRequests": "10",
    "rateLimitWindow": "60"
  }'
```

### External API Configuration

```bash
curl -X PUT \
  http://localhost:8080/auth/realms/myrealm/magiclink/config \
  -H 'Content-Type: application/json' \
  -d '{
    "externalApiEndpoint": "https://api.example.com/send-magiclink",
    "externalApiToken": "your-secret-token",
    "externalApiType": "bearer"
  }'
```

## Troubleshooting

### Common Issues

1. **Plugin not loading**: Check that the JAR is in the correct providers directory
2. **Authentication flow not working**: Verify the authenticator is properly configured
3. **External API calls failing**: Check network connectivity and API credentials
4. **Rate limiting too restrictive**: Adjust rate limit settings in configuration

### Debug Logging

Enable debug logging for detailed troubleshooting:

```java
MagiclinkLogger.setLogLevel(MagiclinkLogger.LogLevel.DEBUG);
```

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please check the project repository or contact the development team.
