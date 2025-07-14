# 🔧 Keycloak OTP Mock API Service

A Node.js Express application that provides mock external API endpoints for testing the Keycloak OTP plugin. This service simulates the external APIs that the OTP plugin communicates with for user eligibility checks and OTP delivery.

## 🚀 Features

- **User Eligibility API**: Checks if users are eligible for OTP-based MFA
- **OTP Delivery API**: Simulates sending OTPs via external email service
- **OTP Validation API**: Validates OTP codes provided by users
- **Health & Status Endpoints**: Monitoring and debugging endpoints
- **Rate Limiting**: Simulates real-world API rate limiting
- **Error Simulation**: Configurable random API failures
- **Structured Logging**: Winston-based logging with multiple levels
- **Security**: Helmet security headers and CORS configuration

## 📋 Prerequisites

- Node.js 18.0.0 or higher
- npm or yarn package manager

## 🛠️ Installation

1. **Navigate to the mock API service directory:**
   ```bash
   cd mock-api-service
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Copy environment configuration:**
   ```bash
   cp env.example .env
   ```

4. **Start the service:**
   ```bash
   npm start
   ```

   Or for development with auto-restart:
   ```bash
   npm run dev
   ```

## ⚙️ Configuration

The service can be configured via environment variables or the `.env` file:

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3001` | Server port |
| `NODE_ENV` | `development` | Environment mode |
| `LOG_LEVEL` | `info` | Logging level |
| `API_PREFIX` | `/api` | API route prefix |
| `MOCK_DELAY_MIN` | `100` | Minimum API delay (ms) |
| `MOCK_DELAY_MAX` | `500` | Maximum API delay (ms) |
| `MOCK_ERROR_RATE` | `0.1` | Random error rate (0-1) |
| `CORS_ORIGIN` | `http://localhost:3000` | Allowed CORS origin |
| `RATE_LIMIT_WINDOW_MS` | `900000` | Rate limit window (15 min) |
| `RATE_LIMIT_MAX_REQUESTS` | `100` | Max requests per window |

## 📡 API Endpoints

### Health & Status

#### `GET /api/v1/health`
Basic health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "service": "keycloak-otp-mock-api",
  "version": "1.0.0",
  "environment": "development",
  "uptime": 123.45
}
```

#### `GET /api/v1/status`
Detailed service status and configuration.

#### `GET /api/v1/metrics`
System metrics and performance data.

### User Eligibility

#### `GET /api/v1/mfa/enabled?email=user@example.com`
Checks if a user is eligible for OTP-based MFA.

**Query Parameters:**
- `email` (required): User's email address

**Response:**
```json
{
  "enabled": true,
  "email": "user@example.com",
  "checkedAt": "2024-01-15T10:30:00.000Z"
}
```

#### `GET /api/v1/mfa/status?email=user@example.com`
Returns detailed MFA status for a user.

**Response:**
```json
{
  "email": "user@example.com",
  "mfaEnabled": true,
  "mfaType": "OTP",
  "lastUpdated": "2024-01-15T10:30:00.000Z",
  "configuration": {
    "otpLength": 6,
    "otpTtl": 300,
    "maxAttempts": 3
  }
}
```

### OTP Operations

#### `POST /api/v1/otp/send`
Sends OTP to user's email.

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "email": "user@example.com",
    "expiresAt": "2024-01-15T10:35:00.000Z",
    "ttl": 300
  }
}
```

#### `POST /api/v1/otp/validate`
Validates OTP provided by user.

**Request Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP validated successfully",
  "data": {
    "email": "user@example.com",
    "validatedAt": "2024-01-15T10:30:00.000Z"
  }
}
```

#### `GET /api/v1/otp/status?email=user@example.com`
Returns OTP status for debugging.

**Response:**
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "hasOtp": true,
    "isExpired": false,
    "attempts": 1,
    "maxAttempts": 3,
    "expiresAt": "2024-01-15T10:35:00.000Z",
    "createdAt": "2024-01-15T10:30:00.000Z"
  }
}
```

## 🧪 Mock Data

The service includes predefined mock data for testing:

### Eligible Users
- `user1@example.com` - Enabled
- `user2@example.com` - Enabled
- `user3@example.com` - Disabled
- `admin@example.com` - Enabled
- `test@keycloak.com` - Enabled

### OTP Configuration
- Length: 6 digits
- TTL: 300 seconds (5 minutes)
- Max attempts: 3

## 🔍 Testing

### Manual Testing with curl

1. **Health Check:**
   ```bash
   curl http://localhost:3001/api/v1/health
   ```

2. **Check User Eligibility:**
   ```bash
   curl "http://localhost:3001/api/v1/mfa/enabled?email=user1@example.com"
   ```

3. **Send OTP:**
   ```bash
   curl -X POST http://localhost:3001/api/v1/otp/send \
     -H "Content-Type: application/json" \
     -d '{"email": "user1@example.com", "otp": "123456"}'
   ```

4. **Validate OTP:**
   ```bash
   curl -X POST http://localhost:3001/api/v1/otp/validate \
     -H "Content-Type: application/json" \
     -d '{"email": "user1@example.com", "otp": "123456"}'
   ```

### Automated Testing

Run the test suite:
```bash
npm test
```

## 📊 Monitoring

### Logs
Logs are written to:
- Console (colored output)
- `logs/combined.log` (all levels)
- `logs/error.log` (error level only)

### Metrics
Access system metrics at `/api/v1/metrics` for monitoring.

## 🔧 Development

### Project Structure
```
mock-api-service/
├── config/           # Configuration files
│   ├── config.js     # Environment configuration
│   └── logger.js     # Winston logger setup
├── middleware/       # Express middleware
│   └── errorHandler.js
├── routes/           # API route handlers
│   ├── health.js     # Health check endpoints
│   ├── eligibility.js # User eligibility endpoints
│   └── otp.js        # OTP operation endpoints
├── utils/            # Utility functions
│   └── mockUtils.js  # Mock data and simulation utilities
├── logs/             # Log files (auto-created)
├── server.js         # Main application file
├── package.json      # Dependencies and scripts
└── README.md         # This file
```

### Adding New Endpoints

1. Create a new route file in `routes/`
2. Export the router
3. Import and use in `server.js`

### Environment Variables

Create a `.env` file based on `env.example` to customize the service behavior.

## 🚨 Error Handling

The service includes comprehensive error handling:

- **400 Bad Request**: Invalid parameters or request format
- **403 Forbidden**: User not eligible for OTP
- **404 Not Found**: Route or resource not found
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server errors

## 🔒 Security Features

- **Helmet**: Security headers
- **CORS**: Cross-origin request handling
- **Rate Limiting**: Request throttling
- **Input Validation**: Request parameter validation
- **Error Sanitization**: Safe error responses

## 📝 Logging

The service uses Winston for structured logging with different levels:

- **error**: Error conditions
- **warn**: Warning conditions
- **info**: General information
- **http**: HTTP requests
- **debug**: Debug information

## 🔄 Integration with Keycloak OTP Plugin

This mock service is designed to work with the Keycloak OTP plugin:

1. **Configure Keycloak OTP Plugin** to point to this mock service
2. **Set environment variables** in Keycloak for API endpoints
3. **Test the integration** using the provided endpoints

### Keycloak Configuration Example

```properties
# OTP Plugin Configuration
external.otp.api.url=http://localhost:3001/api/v1/otp/send
external.eligibility.api.url=http://localhost:3001/api/v1/mfa/enabled
otp.length=6
otp.ttl=300
failIfEligibilityCheckFails=false
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

MIT License - see LICENSE file for details. 