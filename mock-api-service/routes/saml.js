/**
 * SAML Metadata API Routes
 * Provides endpoints for managing SAML Identity Provider metadata
 */

const express = require('express');
const router = express.Router();
const logger = require('../config/logger');
const { getSamlMetadata, upsertSamlMetadata, deleteSamlMetadata, getAllSamlMetadata, testConnection } = require('../config/database');

/**
 * @route   GET /api/v1/saml/metadata/:clientId
 * @desc    Get SAML metadata for a specific client
 * @access  Public
 */
router.get('/metadata/:clientId', async (req, res) => {
  const { clientId } = req.params;
  const startTime = Date.now();
  
  try {
    logger.info('SAML metadata request received', {
      clientId,
      requestId: req.id,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
    
    // Validate client ID
    if (!clientId || clientId.trim() === '') {
      logger.warn('Invalid client ID provided', { clientId });
      return res.status(400).json({
        success: false,
        error: 'INVALID_CLIENT_ID',
        message: 'Client ID is required and cannot be empty',
        timestamp: new Date().toISOString()
      });
    }
    
    // Get SAML metadata from database
    const metadata = await getSamlMetadata(clientId);
    
    if (!metadata) {
      logger.warn('SAML metadata not found for client', { clientId });
      return res.status(404).json({
        success: false,
        error: 'METADATA_NOT_FOUND',
        message: `SAML metadata not found for client: ${clientId}`,
        clientId,
        timestamp: new Date().toISOString()
      });
    }
    
    // Prepare response (exclude sensitive fields)
    const response = {
      entityId: metadata.entity_id,
      ssoUrl: metadata.sso_url,
      x509Certificate: metadata.x509_certificate,
      singleLogoutUrl: metadata.single_logout_url,
      nameIdFormat: metadata.name_id_format,
      signatureAlgorithm: metadata.signature_algorithm,
      encryptionAlgorithm: metadata.encryption_algorithm
    };
    
    const responseTime = Date.now() - startTime;
    logger.info('SAML metadata retrieved successfully', {
      clientId,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.json({
      success: true,
      data: response,
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    const responseTime = Date.now() - startTime;
    logger.error('Error retrieving SAML metadata', {
      clientId,
      error: error.message,
      stack: error.stack,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(500).json({
      success: false,
      error: 'INTERNAL_SERVER_ERROR',
      message: 'Failed to retrieve SAML metadata',
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * @route   POST /api/v1/saml/metadata
 * @desc    Create or update SAML metadata for a client
 * @access  Public
 */
router.post('/metadata', async (req, res) => {
  const startTime = Date.now();
  
  try {
    const {
      client_id,
      entity_id,
      sso_url,
      x509_certificate,
      single_logout_url,
      name_id_format,
      signature_algorithm,
      encryption_algorithm
    } = req.body;
    
    logger.info('SAML metadata creation/update request received', {
      clientId: client_id,
      requestId: req.id,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
    
    // Validate required fields
    const requiredFields = ['client_id', 'entity_id', 'sso_url', 'x509_certificate'];
    const missingFields = requiredFields.filter(field => !req.body[field]);
    
    if (missingFields.length > 0) {
      logger.warn('Missing required fields for SAML metadata', {
        missingFields,
        clientId: client_id
      });
      return res.status(400).json({
        success: false,
        error: 'MISSING_REQUIRED_FIELDS',
        message: `Missing required fields: ${missingFields.join(', ')}`,
        missingFields,
        timestamp: new Date().toISOString()
      });
    }
    
    // Validate certificate format
    if (!x509_certificate.includes('-----BEGIN CERTIFICATE-----') || 
        !x509_certificate.includes('-----END CERTIFICATE-----')) {
      logger.warn('Invalid certificate format provided', { clientId: client_id });
      return res.status(400).json({
        success: false,
        error: 'INVALID_CERTIFICATE_FORMAT',
        message: 'Certificate must be in PEM format with BEGIN and END markers',
        timestamp: new Date().toISOString()
      });
    }
    
    // Prepare metadata object
    const metadata = {
      client_id,
      entity_id,
      sso_url,
      x509_certificate,
      single_logout_url: single_logout_url || null,
      name_id_format: name_id_format || 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
      signature_algorithm: signature_algorithm || 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
      encryption_algorithm: encryption_algorithm || 'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
    };
    
    // Upsert metadata
    const result = await upsertSamlMetadata(metadata);
    
    const responseTime = Date.now() - startTime;
    logger.info('SAML metadata created/updated successfully', {
      clientId: client_id,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(201).json({
      success: true,
      message: 'SAML metadata created/updated successfully',
      data: {
        client_id: result.client_id,
        entity_id: result.entity_id,
        sso_url: result.sso_url,
        single_logout_url: result.single_logout_url,
        name_id_format: result.name_id_format,
        signature_algorithm: result.signature_algorithm,
        encryption_algorithm: result.encryption_algorithm,
        enabled: result.enabled,
        created_at: result.created_at,
        updated_at: result.updated_at
      },
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    const responseTime = Date.now() - startTime;
    logger.error('Error creating/updating SAML metadata', {
      error: error.message,
      stack: error.stack,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(500).json({
      success: false,
      error: 'INTERNAL_SERVER_ERROR',
      message: 'Failed to create/update SAML metadata',
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * @route   DELETE /api/v1/saml/metadata/:clientId
 * @desc    Delete SAML metadata for a specific client
 * @access  Public
 */
router.delete('/metadata/:clientId', async (req, res) => {
  const { clientId } = req.params;
  const startTime = Date.now();
  
  try {
    logger.info('SAML metadata deletion request received', {
      clientId,
      requestId: req.id,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
    
    // Validate client ID
    if (!clientId || clientId.trim() === '') {
      logger.warn('Invalid client ID provided for deletion', { clientId });
      return res.status(400).json({
        success: false,
        error: 'INVALID_CLIENT_ID',
        message: 'Client ID is required and cannot be empty',
        timestamp: new Date().toISOString()
      });
    }
    
    // Delete metadata
    const deleted = await deleteSamlMetadata(clientId);
    
    if (!deleted) {
      logger.warn('SAML metadata not found for deletion', { clientId });
      return res.status(404).json({
        success: false,
        error: 'METADATA_NOT_FOUND',
        message: `SAML metadata not found for client: ${clientId}`,
        clientId,
        timestamp: new Date().toISOString()
      });
    }
    
    const responseTime = Date.now() - startTime;
    logger.info('SAML metadata deleted successfully', {
      clientId,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.json({
      success: true,
      message: 'SAML metadata deleted successfully',
      clientId,
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    const responseTime = Date.now() - startTime;
    logger.error('Error deleting SAML metadata', {
      clientId,
      error: error.message,
      stack: error.stack,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(500).json({
      success: false,
      error: 'INTERNAL_SERVER_ERROR',
      message: 'Failed to delete SAML metadata',
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * @route   GET /api/v1/saml/metadata
 * @desc    Get all SAML metadata
 * @access  Public
 */
router.get('/metadata', async (req, res) => {
  const startTime = Date.now();
  
  try {
    logger.info('All SAML metadata request received', {
      requestId: req.id,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
    
    // Get all metadata
    const metadataList = await getAllSamlMetadata();
    
    // Prepare response (exclude sensitive fields)
    const response = metadataList.map(metadata => ({
      client_id: metadata.client_id,
      entity_id: metadata.entity_id,
      sso_url: metadata.sso_url,
      single_logout_url: metadata.single_logout_url,
      name_id_format: metadata.name_id_format,
      signature_algorithm: metadata.signature_algorithm,
      encryption_algorithm: metadata.encryption_algorithm,
      enabled: metadata.enabled,
      created_at: metadata.created_at,
      updated_at: metadata.updated_at
    }));
    
    const responseTime = Date.now() - startTime;
    logger.info('All SAML metadata retrieved successfully', {
      count: response.length,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.json({
      success: true,
      data: response,
      count: response.length,
      timestamp: new Date().toISOString()
    });
    
  } catch (error) {
    const responseTime = Date.now() - startTime;
    logger.error('Error retrieving all SAML metadata', {
      error: error.message,
      stack: error.stack,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(500).json({
      success: false,
      error: 'INTERNAL_SERVER_ERROR',
      message: 'Failed to retrieve SAML metadata',
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * @route   GET /api/v1/saml/health
 * @desc    Health check for SAML metadata service
 * @access  Public
 */
router.get('/health', async (req, res) => {
  const startTime = Date.now();
  
  try {
    logger.info('SAML metadata health check requested', {
      requestId: req.id,
      userAgent: req.get('User-Agent'),
      ip: req.ip
    });
    
    // Test database connection
    const dbTest = await testConnection();
    
    // Get metadata count
    const metadataList = await getAllSamlMetadata();
    
    const responseTime = Date.now() - startTime;
    logger.info('SAML metadata health check completed', {
      dbStatus: dbTest.success,
      metadataCount: metadataList.length,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.json({
      success: true,
      service: 'SAML Metadata API',
      version: '1.0.0',
      status: 'healthy',
      database: dbTest,
      metadata: {
        count: metadataList.length,
        available: metadataList.map(m => m.client_id)
      },
      timestamp: new Date().toISOString(),
      responseTime: `${responseTime}ms`
    });
    
  } catch (error) {
    const responseTime = Date.now() - startTime;
    logger.error('SAML metadata health check failed', {
      error: error.message,
      stack: error.stack,
      responseTime: `${responseTime}ms`,
      requestId: req.id
    });
    
    res.status(500).json({
      success: false,
      service: 'SAML Metadata API',
      version: '1.0.0',
      status: 'unhealthy',
      error: error.message,
      timestamp: new Date().toISOString(),
      responseTime: `${responseTime}ms`
    });
  }
});

module.exports = router; 