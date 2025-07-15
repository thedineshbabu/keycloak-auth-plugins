# SAML Metadata API Documentation

## Overview

The SAML Metadata API is a RESTful service that provides dynamic SAML Identity Provider metadata management. It stores and retrieves SAML configuration data (entity ID, SSO URL, certificates, etc.) for different clients, enabling dynamic SAML IDP configuration in Keycloak.

## Features

- **Dynamic Metadata Storage**: Store SAML metadata for multiple clients
- **RESTful API**: Full CRUD operations for SAML metadata
- **PostgreSQL Integration**: Persistent storage with proper indexing
- **Comprehensive Logging**: Winston-based logging with structured data
- **Health Monitoring**: Database connection and service health checks
- **Input Validation**: Certificate format and required field validation
- **Error Handling**: Graceful error responses with meaningful messages

## Database Schema

### SAML Metadata Table

```sql
CREATE TABLE saml_metadata (
  id SERIAL PRIMARY KEY,
  client_id VARCHAR(255) NOT NULL UNIQUE,
  entity_id VARCHAR(500) NOT NULL,
  sso_url VARCHAR(500) NOT NULL,
  x509_certificate TEXT NOT NULL,
  single_logout_url VARCHAR(500),
  name_id_format VARCHAR(255) DEFAULT 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
  signature_algorithm VARCHAR(255) DEFAULT 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
  encryption_algorithm VARCHAR(255) DEFAULT 'http://www.w3.org/2001/04/xmlenc#aes256-cbc',
  enabled BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## API Endpoints

### 1. Get SAML Metadata by Client ID

**GET** `/api/v1/saml/metadata/:clientId`

Retrieves SAML metadata for a specific client.

#### Response Format

```json
{
  "success": true,
  "data": {
    "entityId": "https://saml.client-a.com",
    "ssoUrl": "https://saml.client-a.com/sso",
    "x509Certificate": "-----BEGIN CERTIFICATE-----\n...",
    "singleLogoutUrl": "https://saml.client-a.com/slo",
    "nameIdFormat": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
    "signatureAlgorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
    "encryptionAlgorithm": "http://www.w3.org/2001/04/xmlenc#aes256-cbc"
  },
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

#### Error Responses

- `400` - Invalid client ID
- `404` - Metadata not found
- `500` - Internal server error

### 2. Create/Update SAML Metadata

**POST** `/api/v1/saml/metadata`

Creates or updates SAML metadata for a client.

#### Request Body

```json
{
  "client_id": "client-a",
  "entity_id": "https://saml.client-a.com",
  "sso_url": "https://saml.client-a.com/sso",
  "x509_certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
  "single_logout_url": "https://saml.client-a.com/slo",
  "name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
  "signature_algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
  "encryption_algorithm": "http://www.w3.org/2001/04/xmlenc#aes256-cbc"
}
```

#### Required Fields

- `client_id` - Unique identifier for the client
- `entity_id` - SAML entity ID
- `sso_url` - Single Sign-On URL
- `x509_certificate` - X.509 certificate in PEM format

#### Response Format

```json
{
  "success": true,
  "message": "SAML metadata created/updated successfully",
  "data": {
    "client_id": "client-a",
    "entity_id": "https://saml.client-a.com",
    "sso_url": "https://saml.client-a.com/sso",
    "single_logout_url": "https://saml.client-a.com/slo",
    "name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
    "signature_algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
    "encryption_algorithm": "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
    "enabled": true,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z"
  },
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### 3. Delete SAML Metadata

**DELETE** `/api/v1/saml/metadata/:clientId`

Deletes SAML metadata for a specific client.

#### Response Format

```json
{
  "success": true,
  "message": "SAML metadata deleted successfully",
  "clientId": "client-a",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### 4. Get All SAML Metadata

**GET** `/api/v1/saml/metadata`

Retrieves all SAML metadata (excluding certificates for security).

#### Response Format

```json
{
  "success": true,
  "data": [
    {
      "client_id": "client-a",
      "entity_id": "https://saml.client-a.com",
      "sso_url": "https://saml.client-a.com/sso",
      "single_logout_url": "https://saml.client-a.com/slo",
      "name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
      "signature_algorithm": "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
      "encryption_algorithm": "http://www.w3.org/2001/04/xmlenc#aes256-cbc",
      "enabled": true,
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z"
    }
  ],
  "count": 1,
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### 5. Health Check

**GET** `/api/v1/saml/health`

Checks the health of the SAML metadata service and database connection.

#### Response Format

```json
{
  "success": true,
  "service": "SAML Metadata API",
  "version": "1.0.0",
  "status": "healthy",
  "database": {
    "success": true,
    "message": "Database connection successful",
    "timestamp": "2024-01-15T10:30:00.000Z"
  },
  "metadata": {
    "count": 3,
    "available": ["client-a", "client-b", "test-client"]
  },
  "timestamp": "2024-01-15T10:30:00.000Z",
  "responseTime": "45ms"
}
```

## Setup Instructions

### 1. Database Setup

1. **Create PostgreSQL Database**:
   ```bash
   # Connect to PostgreSQL as superuser
   psql -U postgres
   
   # Create database
   CREATE DATABASE ext_api;
   
   # Create user
   CREATE USER opal_user WITH PASSWORD 'opal_password';
   
   # Grant privileges
   GRANT ALL PRIVILEGES ON DATABASE ext_api TO opal_user;
   ```

2. **Run Database Setup Script**:
   ```bash
   # Connect to the database
   psql -U opal_user -d ext_api -f database-setup.sql
   ```

### 2. Environment Configuration

1. **Copy Environment File**:
   ```bash
   cp env.example .env
   ```

2. **Update Environment Variables**:
   ```bash
   # Database Configuration
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=ext_api
   DB_USERNAME=opal_user
   DB_PASSWORD=opal_password
   DB_SSL=false
   ```

### 3. Install Dependencies

```bash
npm install
```

### 4. Start the Service

```bash
# Development mode
npm run dev

# Production mode
npm start
```

## Testing the API

### 1. Health Check

```bash
curl http://localhost:3001/api/v1/saml/health
```

### 2. Get Metadata for Client

```bash
curl http://localhost:3001/api/v1/saml/metadata/client-a
```

### 3. Create New Metadata

```bash
curl -X POST http://localhost:3001/api/v1/saml/metadata \
  -H "Content-Type: application/json" \
  -d '{
    "client_id": "new-client",
    "entity_id": "https://saml.new-client.com",
    "sso_url": "https://saml.new-client.com/sso",
    "x509_certificate": "-----BEGIN CERTIFICATE-----\nMIIC+zCCAeOgAwIBAgIJAJc1qI+CgYMGMA0GCSqGSIb3DQEBCwUAMIGLMQswCQYD\nVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4g\nVmlldzEQMA4GA1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UE\nAxMHVGVzdCBDQTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTAe\nFw0xNjA1MTAxMjM0NTZaFw0yNjA1MDgxMjM0NTZaMIGLMQswCQYDVQQGEwJVUzET\nMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4G\nA1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UEAxMHVGVzdCBD\nQTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTCBnzANBgkqhkiG\n9w0BAQEFAAOBjQAwgYkCgYEAwU2T1+aLmD6D+QvmEaftyKqZmQqQnyeN5Qfl1/CC\nt6GsK8wcnLcpQOA6ipZJ6u5+P2Tp1lXadNW3N/jLJqBrqOj0Vx/enKHsZ3usE9w\nFFlVBBG2MfD1voexBxUzJQm5NJkny+oWq8OIz2BHWH6Me5/jW9Qe1eGmQfZtCaI=\n-----END CERTIFICATE-----"
  }'
```

### 4. Get All Metadata

```bash
curl http://localhost:3001/api/v1/saml/metadata
```

### 5. Delete Metadata

```bash
curl -X DELETE http://localhost:3001/api/v1/saml/metadata/new-client
```

## Integration with Keycloak Dynamic SAML Plugin

The SAML Metadata API is designed to work with the Keycloak Dynamic SAML IDP plugin. The plugin will:

1. Extract the `client_id` from the authentication session
2. Call `GET /api/v1/saml/metadata/:clientId`
3. Use the returned metadata to configure the SAML Identity Provider dynamically
4. Complete the SAML authentication flow

### Example Integration Flow

```java
// In the Keycloak plugin
String clientId = extractClientIdFromSession(session);
SamlMetadataResponse metadata = externalApiClient.getSamlMetadata(clientId);

// Configure SAML IDP with dynamic metadata
SAMLIdentityProviderConfig config = new SAMLIdentityProviderConfig();
config.setEntityId(metadata.getEntityId());
config.setSingleSignOnServiceUrl(metadata.getSsoUrl());
config.setCertificate(metadata.getX509Certificate());
// ... configure other SAML settings
```

## Error Handling

The API provides comprehensive error handling with:

- **Input Validation**: Required field checks and format validation
- **Database Errors**: Connection and query error handling
- **Security**: Certificate format validation
- **Logging**: Structured logging for debugging and monitoring

### Common Error Codes

- `INVALID_CLIENT_ID` - Client ID is missing or invalid
- `METADATA_NOT_FOUND` - No metadata found for the client
- `MISSING_REQUIRED_FIELDS` - Required fields are missing
- `INVALID_CERTIFICATE_FORMAT` - Certificate is not in PEM format
- `INTERNAL_SERVER_ERROR` - Database or server error

## Monitoring and Logging

### Log Levels

- **INFO**: Normal operations, successful requests
- **WARN**: Validation errors, missing data
- **ERROR**: Database errors, server errors
- **HTTP**: Request/response logging

### Log Format

```json
{
  "level": "info",
  "message": "SAML metadata retrieved successfully",
  "clientId": "client-a",
  "responseTime": "45ms",
  "requestId": "req-123",
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

## Security Considerations

1. **Certificate Validation**: Certificates must be in proper PEM format
2. **Input Sanitization**: All inputs are validated and sanitized
3. **Database Security**: Connection pooling with proper error handling
4. **Logging Security**: Sensitive data is not logged
5. **CORS Configuration**: Proper CORS settings for cross-origin requests

## Performance Considerations

1. **Database Indexing**: Indexes on frequently queried fields
2. **Connection Pooling**: Efficient database connection management
3. **Response Caching**: Consider implementing caching for frequently accessed metadata
4. **Query Optimization**: Optimized SQL queries with proper indexing

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check PostgreSQL service is running
   - Verify database credentials in `.env`
   - Ensure database and user exist

2. **Certificate Format Error**
   - Ensure certificate includes `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----`
   - Check for proper line breaks in certificate

3. **Client Not Found**
   - Verify client ID exists in database
   - Check if client is enabled (`enabled = true`)

### Debug Commands

```bash
# Check database connection
psql -U opal_user -d ext_api -c "SELECT COUNT(*) FROM saml_metadata;"

# Check service logs
tail -f logs/app.log

# Test API endpoints
curl -v http://localhost:3001/api/v1/saml/health
```

## Future Enhancements

1. **Caching Layer**: Redis integration for improved performance
2. **Authentication**: API key or JWT-based authentication
3. **Rate Limiting**: Per-client rate limiting
4. **Audit Trail**: Detailed audit logging for compliance
5. **Bulk Operations**: Batch create/update operations
6. **Metadata Validation**: Enhanced SAML metadata validation
7. **Monitoring**: Prometheus metrics integration 