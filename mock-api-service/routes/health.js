/**
 * Health Check API Routes
 * Provides health check and status endpoints for the mock API service
 */

const express = require('express');
const router = express.Router();
const config = require('../config/config');
const logger = require('../config/logger');
const mockUtils = require('../utils/mockUtils');

/**
 * GET /health
 * Basic health check endpoint
 */
router.get('/health', async (req, res) => {
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    const healthStatus = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      service: 'keycloak-otp-mock-api',
      version: '1.0.0',
      environment: config.nodeEnv,
      uptime: process.uptime()
    };
    
    logger.info('Health check requested', healthStatus);
    
    return res.status(200).json(healthStatus);
    
  } catch (error) {
    logger.error(`Health check error: ${error.message}`);
    
    return res.status(500).json({
      status: 'unhealthy',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * GET /status
 * Detailed service status endpoint
 */
router.get('/status', async (req, res) => {
  try {
    // Simulate API delay
    await mockUtils.simulateApiDelay();
    
    const status = {
      service: 'keycloak-otp-mock-api',
      version: '1.0.0',
      environment: config.nodeEnv,
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      memory: {
        used: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
        total: Math.round(process.memoryUsage().heapTotal / 1024 / 1024),
        external: Math.round(process.memoryUsage().external / 1024 / 1024)
      },
      configuration: {
        port: config.port,
        logLevel: config.logLevel,
        mockDelayMin: config.mockDelayMin,
        mockDelayMax: config.mockDelayMax,
        mockErrorRate: config.mockErrorRate,
        rateLimitWindowMs: config.rateLimitWindowMs,
        rateLimitMaxRequests: config.rateLimitMaxRequests
      },
      endpoints: {
        health: '/health',
        status: '/status',
        eligibility: '/mfa/enabled',
        mfaStatus: '/mfa/status',
        otpSend: '/otp/send',
        otpValidate: '/otp/validate',
        otpStatus: '/otp/status'
      },
      mockData: {
        eligibleUsersCount: Object.keys(config.mockData.eligibleUsers).length,
        otpConfig: config.mockData.otpConfig
      }
    };
    
    logger.info('Status check requested', { timestamp: status.timestamp });
    
    return res.status(200).json(status);
    
  } catch (error) {
    logger.error(`Status check error: ${error.message}`);
    
    return res.status(500).json({
      status: 'error',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * GET /metrics
 * Basic metrics endpoint for monitoring
 */
router.get('/metrics', async (req, res) => {
  try {
    const metrics = {
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      memory: {
        heapUsed: process.memoryUsage().heapUsed,
        heapTotal: process.memoryUsage().heapTotal,
        external: process.memoryUsage().external,
        rss: process.memoryUsage().rss
      },
      cpu: process.cpuUsage(),
      pid: process.pid,
      nodeVersion: process.version,
      platform: process.platform
    };
    
    logger.debug('Metrics requested', { timestamp: metrics.timestamp });
    
    return res.status(200).json(metrics);
    
  } catch (error) {
    logger.error(`Metrics error: ${error.message}`);
    
    return res.status(500).json({
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

module.exports = router; 