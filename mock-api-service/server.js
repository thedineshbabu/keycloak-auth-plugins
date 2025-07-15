/**
 * Mock API Service for Keycloak OTP Plugin
 * Provides external API endpoints for OTP generation, validation, and user eligibility
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

// Import configuration and utilities
const config = require('./config/config');
const logger = require('./config/logger');
const { errorHandler, notFoundHandler, validateRequest } = require('./middleware/errorHandler');
const { initializeDatabase, closePool } = require('./config/database');

// Import routes
const healthRoutes = require('./routes/health');
const eligibilityRoutes = require('./routes/eligibility');
const otpRoutes = require('./routes/otp');
const magiclinkRoutes = require('./routes/magiclink');
const samlRoutes = require('./routes/saml');

// Create Express application
const app = express();

// Security middleware
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      scriptSrc: ["'self'"],
      imgSrc: ["'self'", "data:", "https:"],
    },
  },
}));

// CORS configuration
app.use(cors({
  origin: config.corsOrigin,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
  credentials: true
}));

// Request parsing middleware with error handling
app.use(express.json({ 
  limit: '1mb',
  verify: (req, res, buf) => {
    try {
      JSON.parse(buf);
    } catch (e) {
      const error = new Error('Invalid JSON format');
      error.name = 'ValidationError';
      throw error;
    }
  }
}));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

// Request validation middleware
app.use(validateRequest);

// Logging middleware
app.use(morgan('combined', {
  stream: {
    write: (message) => logger.http(message.trim())
  }
}));

// Trust proxy for accurate IP addresses
app.set('trust proxy', true);

// API routes
const apiPrefix = config.apiPrefix;
app.use(`${apiPrefix}/v1`, healthRoutes);
app.use(`${apiPrefix}/v1`, eligibilityRoutes);
app.use(`${apiPrefix}/v1`, otpRoutes);
app.use(`${apiPrefix}/v1/magiclink`, magiclinkRoutes);
app.use(`${apiPrefix}/v1/saml`, samlRoutes);

// Root endpoint
app.get('/', (req, res) => {
  res.json({
    service: 'Keycloak OTP Mock API Service',
    version: '1.0.0',
    description: 'Mock external API service for testing Keycloak OTP plugin',
          endpoints: {
        health: `${apiPrefix}/v1/health`,
        status: `${apiPrefix}/v1/status`,
        eligibility: `${apiPrefix}/v1/mfa/enabled`,
        mfaStatus: `${apiPrefix}/v1/mfa/status`,
        otpSend: `${apiPrefix}/v1/otp/send`,
        otpValidate: `${apiPrefix}/v1/otp/validate`,
        otpStatus: `${apiPrefix}/v1/otp/status`,
        magiclinkSend: `${apiPrefix}/v1/magiclink/send`,
        samlHealth: `${apiPrefix}/v1/saml/health`,
        samlMetadata: `${apiPrefix}/v1/saml/metadata`
      },
    documentation: 'See README.md for API documentation'
  });
});

// 404 handler for undefined routes
app.use(notFoundHandler);

// Global error handler (must be last)
app.use(errorHandler);

// Graceful shutdown handling
const gracefulShutdown = async (signal) => {
  logger.info(`Received ${signal}. Starting graceful shutdown...`);
  
  server.close(async () => {
    try {
      await closePool();
      logger.info('Database pool closed');
    } catch (error) {
      logger.error('Error closing database pool:', error);
    }
    
    logger.info('HTTP server closed');
    process.exit(0);
  });
  
  // Force shutdown after 10 seconds
  setTimeout(() => {
    logger.error('Forced shutdown after timeout');
    process.exit(1);
  }, 10000);
};

// Start server
const server = app.listen(config.port, async () => {
  try {
    // Initialize database
    await initializeDatabase();
    
    logger.info(`Mock API Service started successfully`, {
      port: config.port,
      environment: config.nodeEnv,
      version: '1.0.0',
      timestamp: new Date().toISOString()
    });
    
    logger.info(`Server running at http://localhost:${config.port}`);
    logger.info(`Health check available at http://localhost:${config.port}${apiPrefix}/v1/health`);
    logger.info(`SAML metadata health check available at http://localhost:${config.port}${apiPrefix}/v1/saml/health`);
    logger.info(`API documentation available at http://localhost:${config.port}`);
  } catch (error) {
    logger.error('Failed to initialize database:', error);
    process.exit(1);
  }
});

// Handle graceful shutdown
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception:', error);
  process.exit(1);
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
  process.exit(1);
});

module.exports = app; 