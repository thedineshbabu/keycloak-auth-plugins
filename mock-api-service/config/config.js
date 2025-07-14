/**
 * Configuration Management for Mock API Service
 * Centralizes all configuration settings and environment variables
 */

require('dotenv').config();

const config = {
  // Server Configuration
  port: process.env.PORT || 3001,
  nodeEnv: process.env.NODE_ENV || 'development',
  
  // API Configuration
  apiVersion: process.env.API_VERSION || 'v1',
  apiPrefix: process.env.API_PREFIX || '/api',
  
  // Logging Configuration
  logLevel: process.env.LOG_LEVEL || 'info',
  
  // Mock Configuration
  mockDelayMin: parseInt(process.env.MOCK_DELAY_MIN) || 100,
  mockDelayMax: parseInt(process.env.MOCK_DELAY_MAX) || 500,
  mockErrorRate: parseFloat(process.env.MOCK_ERROR_RATE) || 0.05,
  
  // CORS Configuration
  corsOrigin: process.env.CORS_ORIGIN || 'http://localhost:3000',
  
  // Security Configuration
  rateLimitWindowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 900000, // 15 minutes
  rateLimitMaxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100,
  
  // Mock Data Configuration
  mockData: {
    // Eligible users for OTP (email -> enabled status)
    eligibleUsers: {
      'user1@example.com': true,
      'user2@example.com': true,
      'user3@example.com': false,
      'admin@example.com': true,
      'test@keycloak.com': true
    },
    
    // OTP configuration
    otpConfig: {
      length: 6,
      ttl: 300, // 5 minutes
      maxAttempts: 3
    },
    
    // API endpoints configuration
    endpoints: {
      eligibility: '/mfa/enabled',
      otp: '/otp/send'
    }
  }
};

module.exports = config; 