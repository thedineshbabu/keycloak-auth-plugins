# Keycloak Auth Plugins

A collection of custom Keycloak authentication plugins providing enhanced authentication capabilities. Currently includes:

1. **Magic Link Authentication**
   - Secure JWT-based magic links
   - External API integration
   - Rate limiting and security features

2. **Multi-Factor Authentication (MFA)**
   - OTP generation and validation
   - External API integration for OTP delivery
   - Configurable OTP settings

## Project Structure

```
src/main/java/kf/keycloak/
├── mfa/                              # MFA Module
│   ├── config/
│   │   └── MfaConfig.java           # MFA configuration management
│   ├── model/
│   │   ├── OtpRequest.java          # OTP request model
│   │   └── OtpResponse.java         # OTP response model
│   ├── provider/
│   │   ├── MfaAuthenticator.java         # MFA authentication flow
│   │   └── MfaAuthenticatorFactory.java  # MFA provider factory
│   ├── service/
│   │   ├── OtpService.java          # OTP generation and validation
│   │   └── ExternalApiService.java  # External API integration
│   └── util/
│       └── MfaLogger.java           # Structured logging utility
├── magiclink/                        # Magic Link Module
│   ├── config/
│   │   └── MagiclinkConfig.java          # Configuration management
│   ├── model/
│   │   ├── MagiclinkRequest.java         # Request model
│   │   └── MagiclinkResponse.java        # Response model
│   ├── provider/
│   │   ├── MagiclinkResourceProvider.java     # REST endpoints
│   │   ├── MagiclinkResourceProviderFactory.java
│   │   ├── MagiclinkAuthenticator.java        # Authentication flow
│   │   └── MagiclinkAuthenticatorFactory.java
│   ├── service/
│   │   ├── MagiclinkService.java         # Main orchestration service
│   │   ├── TokenService.java             # JWT token management
│   │   └── ExternalApiService.java       # External API integration
│   └── util/
│       └── MagiclinkLogger.java          # Winston-style logging
```

## Installation

### 1. Build the Plugin

```bash
mvn clean package
```

### 2. Deploy to Keycloak

Copy the built JAR to your Keycloak deployment:

```bash
# For standalone deployment
cp target/keycloak-auth-plugins-1.0.0.jar $KEYCLOAK_HOME/providers/

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

## Available Plugins

### 1. Magic Link Authentication
[See Magic Link Documentation](docs/magiclink.md)

### 2. Multi-Factor Authentication (MFA)
[See MFA Documentation](docs/mfa.md)

## Development

### Requirements

- Java 11+
- Maven 3.6+
- Keycloak 25.0.2+

### Adding New Plugins

1. Create a new package under `src/main/java/kf/keycloak/`
2. Implement required interfaces:
   - `AuthenticatorFactory` for authentication flows
   - `RequiredActionFactory` for required actions
   - `RealmResourceProviderFactory` for REST endpoints
3. Register providers in `META-INF/services/`
4. Add configuration and documentation

### Building

```bash
mvn clean compile
```

### Testing

```bash
mvn test
```

### Packaging

```bash
mvn package
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Features

- **Secure JWT-based magiclinks** with HMAC-SHA256 signing
- **External API integration** for sending magiclinks
- **Rate limiting** to prevent abuse
- **One-time use tokens** to prevent replay attacks
- **Configurable expiration** (1-60 minutes)
- **Comprehensive logging** with structured Winston-style output
- **REST API** for magiclink generation and management
- **Keycloak authentication flow** integration

## API Endpoints

### Generate Magiclink
```
POST /realms/{realm}/magiclink/generate
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
GET /realms/{realm}/magiclink/authenticate?token={jwt_token}
```

### Check Token Status
```
GET /realms/{realm}/magiclink/status?token={jwt_token}
```

### Configuration Management
```
GET /realms/{realm}/magiclink/config
PUT /realms/{realm}/magiclink/config
```

### Test External API
```
GET /realms/{realm}/magiclink/test-api
```

### Health Check
```
GET /realms/{realm}/magiclink/health
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

## Usage Examples

### Generate Magiclink

```bash
curl -X POST \
  http://localhost:8080/realms/myrealm/magiclink/generate \
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
  'http://localhost:8080/realms/myrealm/magiclink/authenticate?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
```

### Check Health

```bash
curl -X GET \
  http://localhost:8080/realms/myrealm/magiclink/health
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
  "magiclink": "https://keycloak.example.com/realms/myrealm/magiclink/authenticate?token=...",
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
  http://localhost:8080/realms/myrealm/magiclink/config \
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
  http://localhost:8080/realms/myrealm/magiclink/config \
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

# Multi-Factor Authentication (MFA) Module

A robust MFA implementation that provides One-Time Password (OTP) authentication capabilities through external API integration.

## MFA Features

- **Configurable OTP Generation**
  - Supports both numeric and alphanumeric OTPs
  - Configurable OTP length (4-8 characters)
  - Adjustable expiry time (60-900 seconds)
  - One-time use codes

- **External API Integration**
  - Flexible API authentication (Bearer, Basic, API Key)
  - Configurable endpoints
  - Robust error handling
  - Automatic retries and timeouts

- **User Management**
  - Per-user MFA enablement
  - Contact information management
  - Optional default MFA enforcement
  - Required action for MFA setup

- **Comprehensive Logging**
  - Structured logging with metadata
  - Multiple log levels (TRACE, DEBUG, INFO, WARN, ERROR)
  - Detailed error tracking
  - Security event logging

## MFA Configuration

Configure the MFA module through realm attributes:

| Attribute | Description | Default |
|-----------|-------------|---------|
| `mfa.enabled` | Enable/disable MFA functionality | `true` |
| `mfa.external.api.endpoint` | External API endpoint for OTP delivery | - |
| `mfa.external.api.token` | API authentication token | - |
| `mfa.external.api.type` | Auth type (bearer, basic, apikey) | `bearer` |
| `mfa.otp.length` | Length of generated OTP | `6` |
| `mfa.otp.expiry` | OTP expiry time in seconds | `300` |
| `mfa.otp.type` | OTP type (numeric, alphanumeric) | `numeric` |
| `mfa.enabled.by.default` | Enable MFA by default for new users | `false` |

## OTP Configuration Guide

### 1. Initial Setup

1. Log in to your Keycloak Admin Console
2. Select your realm
3. Go to "Realm Settings" → "Attributes"
4. Add the following required attributes:
   ```
   mfa.enabled = true
   mfa.external.api.endpoint = https://your-api-endpoint/send-otp
   mfa.external.api.token = your-api-token
   ```

### 2. Configure Authentication Flow

1. Go to "Authentication" in the admin console
2. Click "Create flow" or duplicate an existing flow (e.g., "Browser")
3. Name your flow (e.g., "Browser with OTP")
4. Add execution:
   - Click "Add execution"
   - Select "MFA OTP Authentication"
   - Set requirement to "Required"

### 3. Bind Authentication Flow

#### For Browser Login:
1. Go to "Authentication" → "Flows"
2. Select "Browser" from the dropdown at the top
3. Click "Bind flow" and select your new flow

#### For Specific Clients:
1. Go to "Clients" in the admin console
2. Select your client
3. Go to "Advanced" tab
4. Set "Authentication Flow Overrides":
   - Browser Flow = Your new flow
   - Direct Grant Flow = Your new flow (if using Resource Owner Password Credentials)

### 4. Configure OTP Settings

Customize OTP behavior through realm attributes:

```bash
# Set OTP length (4-8 characters)
mfa.otp.length = 6

# Set OTP expiry time (60-900 seconds)
mfa.otp.expiry = 300

# Choose OTP type (numeric or alphanumeric)
mfa.otp.type = numeric
```

### 5. External API Setup

Configure your external API endpoint that will handle OTP delivery:

```bash
# Set API endpoint
mfa.external.api.endpoint = https://your-api-endpoint/send-otp

# Set authentication type (bearer, basic, apikey)
mfa.external.api.type = bearer

# Set authentication token
mfa.external.api.token = your-api-token
```

### 6. Per-User Configuration

Enable/disable MFA for specific users:

1. Go to "Users" in admin console
2. Select a user
3. Go to "Attributes" tab
4. Add attributes:
   ```
   mfa.enabled = true
   mfa.contact = user-contact-info
   ```

### 7. Testing Configuration

1. Create a test user with MFA enabled
2. Configure user's contact information
3. Try logging in with the test user
4. You should be prompted for OTP after password authentication
5. Check external API logs for OTP delivery
6. Verify OTP validation works

### 8. Troubleshooting

Common configuration issues:

1. **OTP not being sent:**
   - Verify `mfa.external.api.endpoint` is correct
   - Check API token validity
   - Ensure user has valid contact information

2. **Authentication flow issues:**
   - Confirm MFA execution is properly configured
   - Check flow binding for realm/client
   - Verify user has MFA enabled

3. **Invalid OTP errors:**
   - Check OTP expiry time configuration
   - Verify OTP length matches configuration
   - Ensure correct OTP type is being used

4. **External API errors:**
   - Verify API endpoint is accessible
   - Check authentication token validity
   - Confirm API response format matches expected structure

### 9. Monitoring

Monitor OTP functionality through logs:

```bash
# Check OTP generation events
grep "Generated new OTP" server.log

# Monitor OTP validation
grep "Validated OTP" server.log

# Track external API calls
grep "External API" server.log
```

### 10. Security Recommendations

1. **OTP Configuration:**
   - Use minimum 6-digit OTPs
   - Set reasonable expiry time (5-15 minutes)
   - Consider alphanumeric OTPs for higher security

2. **API Security:**
   - Use HTTPS for API endpoint
   - Rotate API tokens regularly
   - Implement rate limiting
   - Monitor failed attempts

3. **User Management:**
   - Regularly audit MFA-enabled users
   - Monitor failed OTP attempts
   - Implement account lockout policies

## Direct Grant Flow Configuration

The MFA plugin supports direct grant (Resource Owner Password Credentials) authentication flow. This allows you to implement OTP verification in API-based or service-to-service authentication scenarios.

### Setting up Direct Grant Flow

1. Go to "Authentication" in the admin console
2. Select "Direct Grant" from the flows dropdown
3. Click "Copy" to create a new flow (e.g., "Direct Grant with OTP")
4. Add the MFA execution:
   ```
   Click "Add execution"
   Select "MFA OTP Authentication"
   Set requirement to "REQUIRED"
   ```

### Configuring Clients

1. Go to "Clients" in the admin console
2. Select your client
3. Go to "Advanced" tab
4. Under "Authentication Flow Overrides":
   - Set "Direct Grant Flow" to your new flow

### API Usage

When using direct grant with MFA, you'll need to make two API calls:

1. **First Request** - Initiate authentication:
```bash
POST /realms/{realm-name}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&
client_id=your-client&
username=user@example.com&
password=user-password
```

This will return a 401 response with an error indicating OTP is required.

2. **Second Request** - Complete authentication with OTP:
```bash
POST /realms/{realm-name}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&
client_id=your-client&
username=user@example.com&
password=user-password&
otp=123456
```

### Error Handling

Common error responses:

1. **Missing OTP:**
```json
{
    "error": "invalid_grant",
    "error_description": "OTP code required"
}
```

2. **Invalid OTP:**
```json
{
    "error": "invalid_grant",
    "error_description": "Invalid or expired OTP code"
}
```

3. **MFA Not Configured:**
```json
{
    "error": "invalid_grant",
    "error_description": "MFA not configured for user"
}
```

### Client Libraries

When using client libraries, make sure to handle the OTP challenge:

**JavaScript Example:**
```javascript
async function authenticate(username, password, otp = null) {
    const data = new URLSearchParams({
        grant_type: 'password',
        client_id: 'your-client',
        username,
        password,
        ...(otp && { otp })
    });

    try {
        const response = await fetch(
            'https://your-keycloak/realms/your-realm/protocol/openid-connect/token',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: data
            }
        );

        if (response.status === 401) {
            const error = await response.json();
            if (error.error_description === 'OTP code required') {
                // Prompt for OTP and retry
                const otpCode = await promptForOTP();
                return authenticate(username, password, otpCode);
            }
        }

        return response.json();
    } catch (error) {
        console.error('Authentication failed:', error);
        throw error;
    }
}
```

**NestJS Example:**
```typescript
// auth.service.ts
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class AuthService {
  constructor(private readonly httpService: HttpService) {}

  async authenticate(username: string, password: string, otp?: string) {
    const data = new URLSearchParams({
      grant_type: 'password',
      client_id: process.env.KEYCLOAK_CLIENT_ID,
      username,
      password,
      ...(otp && { otp })
    });

    try {
      const response = await firstValueFrom(
        this.httpService.post(
          `${process.env.KEYCLOAK_URL}/realms/${process.env.KEYCLOAK_REALM}/protocol/openid-connect/token`,
          data,
          {
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        )
      );

      return response.data;
    } catch (error) {
      if (error.response?.status === 401) {
        const errorData = error.response.data;
        if (errorData.error_description === 'OTP code required') {
          throw new UnauthorizedException('OTP_REQUIRED');
        }
      }
      throw error;
    }
  }
}

// auth.controller.ts
import { Controller, Post, Body } from '@nestjs/common';
import { AuthService } from './auth.service';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('login')
  async login(@Body() credentials: { username: string; password: string; otp?: string }) {
    return this.authService.authenticate(
      credentials.username,
      credentials.password,
      credentials.otp
    );
  }
}

// auth.module.ts
import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

@Module({
  imports: [
    HttpModule.register({
      timeout: 5000,
      maxRedirects: 5,
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService],
  exports: [AuthService],
})
export class AuthModule {}

// app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AuthModule } from './auth/auth.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    AuthModule,
  ],
})
export class AppModule {}

// Environment variables (.env)
KEYCLOAK_URL=https://your-keycloak
KEYCLOAK_REALM=your-realm
KEYCLOAK_CLIENT_ID=your-client
```

### Security Considerations

1. **Rate Limiting:**
   - Implement rate limiting for token endpoints
   - Consider IP-based and user-based limits
   - Monitor failed attempts

2. **Timeout:**
   - Set appropriate OTP expiry time
   - Consider session timeout settings
   - Implement automatic logout

3. **Audit Logging:**
   - Log all direct grant attempts
   - Monitor failed OTP validations
   - Track unusual patterns

4. **Client Configuration:**
   - Use confidential clients only
   - Enable client authentication
   - Restrict allowed grant types
