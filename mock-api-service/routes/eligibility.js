/**
 * Eligibility API Routes
 * Handles user eligibility checks for OTP-based MFA
 */

const express = require('express');
const router = express.Router();
const config = require('../config/config');
const logger = require('../config/logger');
const mockUtils = require('../utils/mockUtils');

/**
 * GET /mfa/enabled
 * Checks if a user is eligible for OTP-based MFA
 * Query Parameters:
 * - email: User's email address
 */
router.get('/mfa/enabled', async (req, res) => {
  const { email } = req.query;
  
  // Log the request
  mockUtils.logApiRequest(req, '/mfa/enabled');
  
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    // Check for rate limiting
    if (mockUtils.isRateLimited(req.ip)) {
      logger.warn(`Rate limited request from IP: ${req.ip}`);
      return res.status(429).json(mockUtils.createErrorResponse(
        'Too many requests. Please try again later.',
        429,
        'RATE_LIMIT_EXCEEDED'
      ));
    }
    
    // Simulate random API failures
    if (mockUtils.shouldSimulateError()) {
      logger.error(`Simulated API failure for email: ${email}`);
      return res.status(500).json(mockUtils.createErrorResponse(
        'Internal server error. Please try again.',
        500,
        'INTERNAL_SERVER_ERROR'
      ));
    }
    
    // Validate email parameter
    if (!email) {
      logger.warn('Missing email parameter in eligibility check');
      return res.status(400).json(mockUtils.createErrorResponse(
        'Email parameter is required',
        400,
        'MISSING_EMAIL_PARAMETER'
      ));
    }
    
    // Validate email format
    if (!mockUtils.isValidEmail(email)) {
      logger.warn(`Invalid email format: ${email}`);
      return res.status(400).json(mockUtils.createErrorResponse(
        'Invalid email format',
        400,
        'INVALID_EMAIL_FORMAT'
      ));
    }
    
    // Check if user is eligible based on mock data
    const isEnabled = config.mockData.eligibleUsers[email] || false;
    
    // Create response
    const response = {
      enabled: isEnabled,
      email: email,
      checkedAt: new Date().toISOString()
    };
    
    logger.info(`Eligibility check for ${email}: ${isEnabled ? 'ENABLED' : 'DISABLED'}`);
    
    // Log the response
    mockUtils.logApiResponse(res, '/mfa/enabled', 200);
    
    return res.status(200).json(response);
    
  } catch (error) {
    logger.error(`Error in eligibility check: ${error.message}`, {
      email: email,
      error: error.stack
    });
    
    return res.status(500).json(mockUtils.createErrorResponse(
      'Internal server error during eligibility check',
      500,
      'ELIGIBILITY_CHECK_ERROR'
    ));
  }
});

/**
 * GET /mfa/status
 * Returns detailed MFA status for a user
 * Query Parameters:
 * - email: User's email address
 */
router.get('/mfa/status', async (req, res) => {
  const { email } = req.query;
  
  // Log the request
  mockUtils.logApiRequest(req, '/mfa/status');
  
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    // Validate email parameter
    if (!email) {
      return res.status(400).json(mockUtils.createErrorResponse(
        'Email parameter is required',
        400,
        'MISSING_EMAIL_PARAMETER'
      ));
    }
    
    // Validate email format
    if (!mockUtils.isValidEmail(email)) {
      return res.status(400).json(mockUtils.createErrorResponse(
        'Invalid email format',
        400,
        'INVALID_EMAIL_FORMAT'
      ));
    }
    
    // Get eligibility status
    const isEnabled = config.mockData.eligibleUsers[email] || false;
    
    // Create detailed response
    const response = {
      email: email,
      mfaEnabled: isEnabled,
      mfaType: isEnabled ? 'OTP' : 'NONE',
      lastUpdated: new Date().toISOString(),
      configuration: {
        otpLength: config.mockData.otpConfig.length,
        otpTtl: config.mockData.otpConfig.ttl,
        maxAttempts: config.mockData.otpConfig.maxAttempts
      }
    };
    
    logger.info(`MFA status for ${email}: ${isEnabled ? 'ENABLED' : 'DISABLED'}`);
    
    // Log the response
    mockUtils.logApiResponse(res, '/mfa/status', 200);
    
    return res.status(200).json(response);
    
  } catch (error) {
    logger.error(`Error in MFA status check: ${error.message}`, {
      email: email,
      error: error.stack
    });
    
    return res.status(500).json(mockUtils.createErrorResponse(
      'Internal server error during MFA status check',
      500,
      'MFA_STATUS_CHECK_ERROR'
    ));
  }
});

module.exports = router; 