/**
 * API Tests for Mock API Service
 * Tests all endpoints and functionality
 */

const request = require('supertest');
const app = require('../server');

describe('Mock API Service', () => {
  describe('Health Endpoints', () => {
    test('GET /api/v1/health should return healthy status', async () => {
      const response = await request(app)
        .get('/api/v1/health')
        .expect(200);
      
      expect(response.body).toHaveProperty('status', 'healthy');
      expect(response.body).toHaveProperty('service', 'keycloak-otp-mock-api');
      expect(response.body).toHaveProperty('version', '1.0.0');
      expect(response.body).toHaveProperty('timestamp');
      expect(response.body).toHaveProperty('uptime');
    });

    test('GET /api/v1/status should return detailed status', async () => {
      const response = await request(app)
        .get('/api/v1/status')
        .expect(200);
      
      expect(response.body).toHaveProperty('service', 'keycloak-otp-mock-api');
      expect(response.body).toHaveProperty('version', '1.0.0');
      expect(response.body).toHaveProperty('memory');
      expect(response.body).toHaveProperty('configuration');
      expect(response.body).toHaveProperty('endpoints');
      expect(response.body).toHaveProperty('mockData');
    });

    test('GET /api/v1/metrics should return system metrics', async () => {
      const response = await request(app)
        .get('/api/v1/metrics')
        .expect(200);
      
      expect(response.body).toHaveProperty('timestamp');
      expect(response.body).toHaveProperty('uptime');
      expect(response.body).toHaveProperty('memory');
      expect(response.body).toHaveProperty('pid');
      expect(response.body).toHaveProperty('nodeVersion');
    });
  });

  describe('Eligibility Endpoints', () => {
    test('GET /api/v1/mfa/enabled should return enabled for eligible user', async () => {
      const response = await request(app)
        .get('/api/v1/mfa/enabled?email=user1@example.com')
        .expect(200);
      
      expect(response.body).toHaveProperty('enabled', true);
      expect(response.body).toHaveProperty('email', 'user1@example.com');
      expect(response.body).toHaveProperty('checkedAt');
    });

    test('GET /api/v1/mfa/enabled should return disabled for ineligible user', async () => {
      const response = await request(app)
        .get('/api/v1/mfa/enabled?email=user3@example.com')
        .expect(200);
      
      expect(response.body).toHaveProperty('enabled', false);
      expect(response.body).toHaveProperty('email', 'user3@example.com');
    });

    test('GET /api/v1/mfa/enabled should return 400 for missing email', async () => {
      const response = await request(app)
        .get('/api/v1/mfa/enabled')
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'MISSING_EMAIL_PARAMETER');
    });

    test('GET /api/v1/mfa/enabled should return 400 for invalid email', async () => {
      const response = await request(app)
        .get('/api/v1/mfa/enabled?email=invalid-email')
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'INVALID_EMAIL_FORMAT');
    });

    test('GET /api/v1/mfa/status should return detailed MFA status', async () => {
      const response = await request(app)
        .get('/api/v1/mfa/status?email=user1@example.com')
        .expect(200);
      
      expect(response.body).toHaveProperty('email', 'user1@example.com');
      expect(response.body).toHaveProperty('mfaEnabled', true);
      expect(response.body).toHaveProperty('mfaType', 'OTP');
      expect(response.body).toHaveProperty('configuration');
      expect(response.body.configuration).toHaveProperty('otpLength', 6);
      expect(response.body.configuration).toHaveProperty('otpTtl', 300);
      expect(response.body.configuration).toHaveProperty('maxAttempts', 3);
    });
  });

  describe('OTP Endpoints', () => {
    test('POST /api/v1/otp/send should send OTP successfully', async () => {
      const otpData = {
        email: 'user1@example.com',
        otp: '123456'
      };

      const response = await request(app)
        .post('/api/v1/otp/send')
        .send(otpData)
        .expect(200);
      
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('message', 'OTP sent successfully');
      expect(response.body.data).toHaveProperty('email', 'user1@example.com');
      expect(response.body.data).toHaveProperty('expiresAt');
      expect(response.body.data).toHaveProperty('ttl', 300);
    });

    test('POST /api/v1/otp/send should return 400 for missing fields', async () => {
      const response = await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user1@example.com' })
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'MISSING_REQUIRED_FIELDS');
    });

    test('POST /api/v1/otp/send should return 400 for invalid email', async () => {
      const response = await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'invalid-email', otp: '123456' })
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'INVALID_EMAIL_FORMAT');
    });

    test('POST /api/v1/otp/send should return 400 for invalid OTP format', async () => {
      const response = await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user1@example.com', otp: '12345' })
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'INVALID_OTP_FORMAT');
    });

    test('POST /api/v1/otp/send should return 403 for ineligible user', async () => {
      const response = await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user3@example.com', otp: '123456' })
        .expect(403);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'USER_NOT_ELIGIBLE');
    });

    test('POST /api/v1/otp/validate should validate OTP successfully', async () => {
      // First send an OTP
      await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user1@example.com', otp: '123456' })
        .expect(200);

      // Then validate it
      const response = await request(app)
        .post('/api/v1/otp/validate')
        .send({ email: 'user1@example.com', otp: '123456' })
        .expect(200);
      
      expect(response.body).toHaveProperty('success', true);
      expect(response.body).toHaveProperty('message', 'OTP validated successfully');
      expect(response.body.data).toHaveProperty('email', 'user1@example.com');
      expect(response.body.data).toHaveProperty('validatedAt');
    });

    test('POST /api/v1/otp/validate should return 400 for invalid OTP', async () => {
      // First send an OTP
      await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user1@example.com', otp: '123456' })
        .expect(200);

      // Then try to validate with wrong OTP
      const response = await request(app)
        .post('/api/v1/otp/validate')
        .send({ email: 'user1@example.com', otp: '654321' })
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'INVALID_OTP');
    });

    test('POST /api/v1/otp/validate should return 400 for non-existent OTP', async () => {
      const response = await request(app)
        .post('/api/v1/otp/validate')
        .send({ email: 'nonexistent@example.com', otp: '123456' })
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'OTP_NOT_FOUND');
    });

    test('GET /api/v1/otp/status should return OTP status', async () => {
      // First send an OTP
      await request(app)
        .post('/api/v1/otp/send')
        .send({ email: 'user1@example.com', otp: '123456' })
        .expect(200);

      // Then check status
      const response = await request(app)
        .get('/api/v1/otp/status?email=user1@example.com')
        .expect(200);
      
      expect(response.body).toHaveProperty('success', true);
      expect(response.body.data).toHaveProperty('email', 'user1@example.com');
      expect(response.body.data).toHaveProperty('hasOtp', true);
      expect(response.body.data).toHaveProperty('isExpired', false);
      expect(response.body.data).toHaveProperty('attempts', 0);
      expect(response.body.data).toHaveProperty('maxAttempts', 3);
      expect(response.body.data).toHaveProperty('expiresAt');
      expect(response.body.data).toHaveProperty('createdAt');
    });

    test('GET /api/v1/otp/status should return 404 for non-existent OTP', async () => {
      const response = await request(app)
        .get('/api/v1/otp/status?email=nonexistent@example.com')
        .expect(404);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body).toHaveProperty('message', 'No OTP found for this email');
    });
  });

  describe('Error Handling', () => {
    test('Should return 404 for undefined routes', async () => {
      const response = await request(app)
        .get('/api/v1/nonexistent')
        .expect(404);
      
      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('code', 'ROUTE_NOT_FOUND');
    });

    test('Should handle malformed JSON gracefully', async () => {
      const response = await request(app)
        .post('/api/v1/otp/send')
        .set('Content-Type', 'application/json')
        .send('invalid json')
        .expect(400);
      
      expect(response.body).toHaveProperty('success', false);
    });
  });

  describe('Rate Limiting', () => {
    test('Should handle rate limiting after many requests', async () => {
      // Make many requests to trigger rate limiting
      const requests = Array(150).fill().map(() => 
        request(app).get('/api/v1/mfa/enabled?email=user1@example.com')
      );
      
      const responses = await Promise.all(requests);
      const rateLimited = responses.some(r => r.status === 429);
      
      // At least one request should be rate limited
      expect(rateLimited).toBe(true);
    });
  });
}); 