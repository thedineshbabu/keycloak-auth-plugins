/**
 * OTP API Routes
 * Handles OTP generation and sending via external API
 */

const express = require('express');
const router = express.Router();
const config = require('../config/config');
const logger = require('../config/logger');
const mockUtils = require('../utils/mockUtils');

// In-memory storage for OTPs (in production, this would be a database)
const otpStorage = new Map();

/**
 * POST /otp/send
 * Sends OTP to user's email via external API
 * Request Body:
 * {
 *   "email": "user@example.com",
 *   "otp": "123456"
 * }
 */
router.post('/otp/send', async (req, res) => {
  const { email, otp } = req.body;
  
  // Log the request
  mockUtils.logApiRequest(req, '/otp/send');
  
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    // Check for rate limiting
    if (mockUtils.isRateLimited(req.ip)) {
      logger.warn(`Rate limited OTP request from IP: ${req.ip}`);
      return res.status(429).json(mockUtils.createErrorResponse(
        'Too many OTP requests. Please try again later.',
        429,
        'RATE_LIMIT_EXCEEDED'
      ));
    }
    
    // Simulate random API failures
    if (mockUtils.shouldSimulateError()) {
      logger.error(`Simulated OTP API failure for email: ${email}`);
      return res.status(500).json(mockUtils.createErrorResponse(
        'Failed to send OTP. Please try again.',
        500,
        'OTP_SEND_ERROR'
      ));
    }
    
    // Validate request body
    if (!email || !otp) {
      logger.warn('Missing required fields in OTP send request', { email, otp });
      return res.status(400).json(mockUtils.createErrorResponse(
        'Email and OTP are required',
        400,
        'MISSING_REQUIRED_FIELDS'
      ));
    }
    
    // Validate email format
    if (!mockUtils.isValidEmail(email)) {
      logger.warn(`Invalid email format in OTP request: ${email}`);
      return res.status(400).json(mockUtils.createErrorResponse(
        'Invalid email format',
        400,
        'INVALID_EMAIL_FORMAT'
      ));
    }
    
    // Validate OTP format (numeric, 6 digits)
    if (!/^\d{6}$/.test(otp)) {
      logger.warn(`Invalid OTP format: ${otp}`);
      return res.status(400).json(mockUtils.createErrorResponse(
        'Invalid OTP format. Must be 6 digits.',
        400,
        'INVALID_OTP_FORMAT'
      ));
    }
    
    // Check if user is eligible for OTP
    const isEligible = config.mockData.eligibleUsers[email] || false;
    if (!isEligible) {
      logger.warn(`User not eligible for OTP: ${email}`);
      return res.status(403).json(mockUtils.createErrorResponse(
        'User not eligible for OTP-based authentication',
        403,
        'USER_NOT_ELIGIBLE'
      ));
    }
    
    // Store OTP with expiration
    const expiresAt = new Date(Date.now() + config.mockData.otpConfig.ttl * 1000);
    otpStorage.set(email, {
      otp: otp,
      expiresAt: expiresAt,
      attempts: 0,
      createdAt: new Date()
    });
    
    // Simulate email sending
    logger.info(`OTP sent to ${email}: ${otp}`, {
      email: email,
      otp: otp,
      expiresAt: expiresAt,
      ttl: config.mockData.otpConfig.ttl
    });
    
    // Create success response
    const response = {
      success: true,
      message: 'OTP sent successfully',
      data: {
        email: email,
        expiresAt: expiresAt.toISOString(),
        ttl: config.mockData.otpConfig.ttl
      }
    };
    
    // Log the response
    mockUtils.logApiResponse(res, '/otp/send', 200);
    
    return res.status(200).json(response);
    
  } catch (error) {
    logger.error(`Error sending OTP: ${error.message}`, {
      email: email,
      error: error.stack
    });
    
    return res.status(500).json(mockUtils.createErrorResponse(
      'Internal server error while sending OTP',
      500,
      'OTP_SEND_ERROR'
    ));
  }
});

/**
 * POST /otp/validate
 * Validates OTP provided by user
 * Request Body:
 * {
 *   "email": "user@example.com",
 *   "otp": "123456"
 * }
 */
router.post('/otp/validate', async (req, res) => {
  const { email, otp } = req.body;
  
  // Log the request
  mockUtils.logApiRequest(req, '/otp/validate');
  
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    // Validate request body
    if (!email || !otp) {
      return res.status(400).json(mockUtils.createErrorResponse(
        'Email and OTP are required',
        400,
        'MISSING_REQUIRED_FIELDS'
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
    
    // Get stored OTP data
    const storedData = otpStorage.get(email);
    
    if (!storedData) {
      logger.warn(`No OTP found for email: ${email}`);
      return res.status(400).json(mockUtils.createErrorResponse(
        'No OTP found for this email. Please request a new OTP.',
        400,
        'OTP_NOT_FOUND'
      ));
    }
    
    // Check if OTP is expired
    if (new Date() > storedData.expiresAt) {
      logger.warn(`Expired OTP for email: ${email}`);
      otpStorage.delete(email);
      return res.status(400).json(mockUtils.createErrorResponse(
        'OTP has expired. Please request a new OTP.',
        400,
        'OTP_EXPIRED'
      ));
    }
    
    // Check if max attempts exceeded
    if (storedData.attempts >= config.mockData.otpConfig.maxAttempts) {
      logger.warn(`Max attempts exceeded for email: ${email}`);
      otpStorage.delete(email);
      return res.status(400).json(mockUtils.createErrorResponse(
        'Maximum OTP attempts exceeded. Please request a new OTP.',
        400,
        'MAX_ATTEMPTS_EXCEEDED'
      ));
    }
    
    // Increment attempts
    storedData.attempts++;
    otpStorage.set(email, storedData);
    
    // Validate OTP
    if (storedData.otp !== otp) {
      logger.warn(`Invalid OTP for email: ${email}`, {
        provided: otp,
        expected: storedData.otp,
        attempts: storedData.attempts
      });
      
      return res.status(400).json(mockUtils.createErrorResponse(
        'Invalid OTP. Please try again.',
        400,
        'INVALID_OTP'
      ));
    }
    
    // OTP is valid - remove it from storage
    otpStorage.delete(email);
    
    logger.info(`OTP validated successfully for email: ${email}`);
    
    // Create success response
    const response = {
      success: true,
      message: 'OTP validated successfully',
      data: {
        email: email,
        validatedAt: new Date().toISOString()
      }
    };
    
    // Log the response
    mockUtils.logApiResponse(res, '/otp/validate', 200);
    
    return res.status(200).json(response);
    
  } catch (error) {
    logger.error(`Error validating OTP: ${error.message}`, {
      email: email,
      error: error.stack
    });
    
    return res.status(500).json(mockUtils.createErrorResponse(
      'Internal server error while validating OTP',
      500,
      'OTP_VALIDATION_ERROR'
    ));
  }
});

/**
 * GET /otp/status
 * Returns OTP status for debugging
 * Query Parameters:
 * - email: User's email address
 */
router.get('/otp/status', async (req, res) => {
  const { email } = req.query;
  
  if (!email) {
    return res.status(400).json(mockUtils.createErrorResponse(
      'Email parameter is required',
      400,
      'MISSING_EMAIL_PARAMETER'
    ));
  }
  
  const storedData = otpStorage.get(email);
  
  if (!storedData) {
    return res.status(404).json({
      success: false,
      message: 'No OTP found for this email'
    });
  }
  
  const isExpired = new Date() > storedData.expiresAt;
  
  return res.status(200).json({
    success: true,
    data: {
      email: email,
      hasOtp: true,
      isExpired: isExpired,
      attempts: storedData.attempts,
      maxAttempts: config.mockData.otpConfig.maxAttempts,
      expiresAt: storedData.expiresAt.toISOString(),
      createdAt: storedData.createdAt.toISOString()
    }
  });
});

module.exports = router; 