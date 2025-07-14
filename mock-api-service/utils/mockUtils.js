/**
 * Mock Utilities for API Service
 * Provides helper functions for generating mock data and simulating API behavior
 */

const config = require('../config/config');
const logger = require('../config/logger');

/**
 * Generates a random delay to simulate real API response times
 * @returns {Promise} Promise that resolves after random delay
 */
const simulateApiDelay = () => {
  const delay = Math.random() * (config.mockDelayMax - config.mockDelayMin) + config.mockDelayMin;
  return new Promise(resolve => setTimeout(resolve, delay));
};

/**
 * Simulates random API failures based on configured error rate
 * @returns {boolean} True if should simulate error, false otherwise
 */
const shouldSimulateError = () => {
  return Math.random() < config.mockErrorRate;
};

/**
 * Generates a mock OTP code
 * @param {number} length - Length of OTP (default: 6)
 * @returns {string} Generated OTP
 */
const generateMockOtp = (length = 6) => {
  const digits = '0123456789';
  let otp = '';
  for (let i = 0; i < length; i++) {
    otp += digits.charAt(Math.floor(Math.random() * digits.length));
  }
  return otp;
};

/**
 * Validates email format
 * @param {string} email - Email to validate
 * @returns {boolean} True if valid email format
 */
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Creates a mock error response
 * @param {string} message - Error message
 * @param {number} statusCode - HTTP status code
 * @param {string} errorCode - Custom error code
 * @returns {Object} Error response object
 */
const createErrorResponse = (message, statusCode = 400, errorCode = 'BAD_REQUEST') => {
  return {
    success: false,
    error: {
      code: errorCode,
      message: message,
      statusCode: statusCode,
      timestamp: new Date().toISOString()
    }
  };
};

/**
 * Creates a mock success response
 * @param {Object} data - Response data
 * @param {string} message - Success message
 * @returns {Object} Success response object
 */
const createSuccessResponse = (data, message = 'Success') => {
  return {
    success: true,
    message: message,
    data: data,
    timestamp: new Date().toISOString()
  };
};

/**
 * Simulates rate limiting by tracking requests per IP
 */
const requestCounts = new Map();

/**
 * Checks if request should be rate limited
 * @param {string} ip - Client IP address
 * @returns {boolean} True if rate limited
 */
const isRateLimited = (ip) => {
  const now = Date.now();
  const windowStart = now - config.rateLimitWindowMs;
  
  if (!requestCounts.has(ip)) {
    requestCounts.set(ip, []);
  }
  
  const requests = requestCounts.get(ip);
  const recentRequests = requests.filter(timestamp => timestamp > windowStart);
  
  // Update requests for this IP
  requestCounts.set(ip, [...recentRequests, now]);
  
  return recentRequests.length >= config.rateLimitMaxRequests;
};

/**
 * Logs API request for debugging
 * @param {Object} req - Express request object
 * @param {string} endpoint - API endpoint
 */
const logApiRequest = (req, endpoint) => {
  logger.info(`API Request: ${req.method} ${endpoint}`, {
    ip: req.ip,
    userAgent: req.get('User-Agent'),
    timestamp: new Date().toISOString()
  });
};

/**
 * Logs API response for debugging
 * @param {Object} res - Express response object
 * @param {string} endpoint - API endpoint
 * @param {number} statusCode - HTTP status code
 */
const logApiResponse = (res, endpoint, statusCode) => {
  logger.info(`API Response: ${res.req.method} ${endpoint} - ${statusCode}`, {
    ip: res.req.ip,
    timestamp: new Date().toISOString()
  });
};

module.exports = {
  simulateApiDelay,
  shouldSimulateError,
  generateMockOtp,
  isValidEmail,
  createErrorResponse,
  createSuccessResponse,
  isRateLimited,
  logApiRequest,
  logApiResponse
}; 