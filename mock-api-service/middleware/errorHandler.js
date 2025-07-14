/**
 * Error Handling Middleware
 * Provides centralized error handling for the mock API service
 */

const logger = require('../config/logger');
const mockUtils = require('../utils/mockUtils');

/**
 * Global error handler middleware
 * @param {Error} err - Error object
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const errorHandler = (err, req, res, next) => {
  // Log the error
  logger.error('Unhandled error occurred', {
    error: err.message,
    stack: err.stack,
    url: req.url,
    method: req.method,
    ip: req.ip,
    userAgent: req.get('User-Agent')
  });
  
  // Determine error type and create appropriate response
  let statusCode = 500;
  let errorCode = 'INTERNAL_SERVER_ERROR';
  let message = 'Internal server error';
  
  // Handle specific error types
  if (err.name === 'ValidationError') {
    statusCode = 400;
    errorCode = 'VALIDATION_ERROR';
    message = err.message;
  } else if (err.name === 'UnauthorizedError') {
    statusCode = 401;
    errorCode = 'UNAUTHORIZED';
    message = 'Unauthorized access';
  } else if (err.name === 'ForbiddenError') {
    statusCode = 403;
    errorCode = 'FORBIDDEN';
    message = 'Access forbidden';
  } else if (err.name === 'NotFoundError') {
    statusCode = 404;
    errorCode = 'NOT_FOUND';
    message = 'Resource not found';
  } else if (err.name === 'RateLimitError') {
    statusCode = 429;
    errorCode = 'RATE_LIMIT_EXCEEDED';
    message = 'Too many requests';
  }
  
  // Create error response
  const errorResponse = mockUtils.createErrorResponse(message, statusCode, errorCode);
  
  // Send error response
  res.status(statusCode).json(errorResponse);
};

/**
 * 404 handler for undefined routes
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const notFoundHandler = (req, res) => {
  logger.warn(`Route not found: ${req.method} ${req.url}`, {
    ip: req.ip,
    userAgent: req.get('User-Agent')
  });
  
  const errorResponse = mockUtils.createErrorResponse(
    `Route ${req.method} ${req.url} not found`,
    404,
    'ROUTE_NOT_FOUND'
  );
  
  res.status(404).json(errorResponse);
};

/**
 * Request validation middleware
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const validateRequest = (req, res, next) => {
  // Check for required headers
  if (!req.get('Content-Type') && req.method !== 'GET') {
    const error = new Error('Content-Type header is required');
    error.name = 'ValidationError';
    return next(error);
  }
  
  // Check request size (limit to 1MB)
  const contentLength = parseInt(req.get('Content-Length') || '0');
  if (contentLength > 1024 * 1024) {
    const error = new Error('Request body too large');
    error.name = 'ValidationError';
    return next(error);
  }
  
  next();
};

module.exports = {
  errorHandler,
  notFoundHandler,
  validateRequest
}; 