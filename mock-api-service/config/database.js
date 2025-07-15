/**
 * Database configuration for PostgreSQL
 * Handles connection pooling and table setup for SAML metadata
 */

const { Pool } = require('pg');
const logger = require('./logger');

// Database configuration
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'ext_api',
  user: process.env.DB_USERNAME || 'opal_user',
  password: process.env.DB_PASSWORD || 'opal_password',
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000, // Close idle clients after 30 seconds
  connectionTimeoutMillis: 2000, // Return an error after 2 seconds if connection could not be established
};

// Create connection pool
const pool = new Pool(dbConfig);

// Handle pool errors
pool.on('error', (err) => {
  logger.error('Unexpected error on idle client', err);
  process.exit(-1);
});

/**
 * Initialize database tables
 */
async function initializeDatabase() {
  const client = await pool.connect();
  
  try {
    logger.info('Initializing database tables...');
    
    // Create SAML metadata table
    const createSamlMetadataTable = `
      CREATE TABLE IF NOT EXISTS saml_metadata (
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
    `;
    
    // Create index for faster lookups
    const createIndex = `
      CREATE INDEX IF NOT EXISTS idx_saml_metadata_client_id ON saml_metadata(client_id);
      CREATE INDEX IF NOT EXISTS idx_saml_metadata_enabled ON saml_metadata(enabled);
    `;
    
    // Create trigger to update updated_at timestamp
    const createTrigger = `
      CREATE OR REPLACE FUNCTION update_updated_at_column()
      RETURNS TRIGGER AS $$
      BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
      END;
      $$ language 'plpgsql';
      
      DROP TRIGGER IF EXISTS update_saml_metadata_updated_at ON saml_metadata;
      CREATE TRIGGER update_saml_metadata_updated_at
        BEFORE UPDATE ON saml_metadata
        FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    `;
    
    await client.query(createSamlMetadataTable);
    await client.query(createIndex);
    await client.query(createTrigger);
    
    logger.info('Database tables initialized successfully');
    
    // Insert sample data if table is empty
    const checkData = await client.query('SELECT COUNT(*) FROM saml_metadata');
    if (parseInt(checkData.rows[0].count) === 0) {
      await insertSampleData(client);
    }
    
  } catch (error) {
    logger.error('Error initializing database:', error);
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Insert sample SAML metadata
 */
async function insertSampleData(client) {
  const sampleData = [
    {
      client_id: 'client-a',
      entity_id: 'https://saml.client-a.com',
      sso_url: 'https://saml.client-a.com/sso',
      x509_certificate: `-----BEGIN CERTIFICATE-----
MIIC+zCCAeOgAwIBAgIJAJc1qI+CgYMGMA0GCSqGSIb3DQEBCwUAMIGLMQswCQYD
VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4g
VmlldzEQMA4GA1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UE
AxMHVGVzdCBDQTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTAe
Fw0xNjA1MTAxMjM0NTZaFw0yNjA1MDgxMjM0NTZaMIGLMQswCQYDVQQGEwJVUzET
MBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4G
A1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UEAxMHVGVzdCBD
QTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTCBnzANBgkqhkiG
9w0BAQEFAAOBjQAwgYkCgYEAwU2T1+aLmD6D+QvmEaftyKqZmQqQnyeN5Qfl1/CC
t6GsK8wcnLcpQOA6ipZJ6u5+P2Tp1lXadNW3N/jLJqBrqOj0Vx/enKHsZ3usE9w
FFlVBBG2MfD1voexBxUzJQm5NJkny+oWq8OIz2BHWH6Me5/jW9Qe1eGmQfZtCaI=
-----END CERTIFICATE-----`,
      single_logout_url: 'https://saml.client-a.com/slo',
      name_id_format: 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
      signature_algorithm: 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
      encryption_algorithm: 'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
    },
    {
      client_id: 'client-b',
      entity_id: 'https://saml.client-b.com',
      sso_url: 'https://saml.client-b.com/sso',
      x509_certificate: `-----BEGIN CERTIFICATE-----
MIIC+zCCAeOgAwIBAgIJAJc1qI+CgYMGMA0GCSqGSIb3DQEBCwUAMIGLMQswCQYD
VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4g
VmlldzEQMA4GA1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UE
AxMHVGVzdCBDQTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTAe
Fw0xNjA1MTAxMjM0NTZaFw0yNjA1MDgxMjM0NTZaMIGLMQswCQYDVQQGEwJVUzET
MBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4G
A1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UEAxMHVGVzdCBD
QTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTCBnzANBgkqhkiG
9w0BAQEFAAOBjQAwgYkCgYEAwU2T1+aLmD6D+QvmEaftyKqZmQqQnyeN5Qfl1/CC
t6GsK8wcnLcpQOA6ipZJ6u5+P2Tp1lXadNW3N/jLJqBrqOj0Vx/enKHsZ3usE9w
FFlVBBG2MfD1voexBxUzJQm5NJkny+oWq8OIz2BHWH6Me5/jW9Qe1eGmQfZtCaI=
-----END CERTIFICATE-----`,
      single_logout_url: 'https://saml.client-b.com/slo',
      name_id_format: 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
      signature_algorithm: 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
      encryption_algorithm: 'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
    },
    {
      client_id: 'test-client',
      entity_id: 'https://saml.test-client.com',
      sso_url: 'https://saml.test-client.com/sso',
      x509_certificate: `-----BEGIN CERTIFICATE-----
MIIC+zCCAeOgAwIBAgIJAJc1qI+CgYMGMA0GCSqGSIb3DQEBCwUAMIGLMQswCQYD
VQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4g
VmlldzEQMA4GA1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UE
AxMHVGVzdCBDQTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTAe
Fw0xNjA1MTAxMjM0NTZaFw0yNjA1MDgxMjM0NTZaMIGLMQswCQYDVQQGEwJVUzET
MBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4G
A1UEChMHVW5pdmVyc2l0eTENMAsGA1UECxMEVGVzdDEQMA4GA1UEAxMHVGVzdCBD
QTEfMB0GCSqGSIb3DQEJARYQdGVzdEB1bml2ZXJzaXR5LmNvbTCBnzANBgkqhkiG
9w0BAQEFAAOBjQAwgYkCgYEAwU2T1+aLmD6D+QvmEaftyKqZmQqQnyeN5Qfl1/CC
t6GsK8wcnLcpQOA6ipZJ6u5+P2Tp1lXadNW3N/jLJqBrqOj0Vx/enKHsZ3usE9w
FFlVBBG2MfD1voexBxUzJQm5NJkny+oWq8OIz2BHWH6Me5/jW9Qe1eGmQfZtCaI=
-----END CERTIFICATE-----`,
      single_logout_url: 'https://saml.test-client.com/slo',
      name_id_format: 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
      signature_algorithm: 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
      encryption_algorithm: 'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
    }
  ];
  
  for (const data of sampleData) {
    await client.query(`
      INSERT INTO saml_metadata (
        client_id, entity_id, sso_url, x509_certificate, single_logout_url,
        name_id_format, signature_algorithm, encryption_algorithm
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      ON CONFLICT (client_id) DO NOTHING
    `, [
      data.client_id, data.entity_id, data.sso_url, data.x509_certificate,
      data.single_logout_url, data.name_id_format, data.signature_algorithm,
      data.encryption_algorithm
    ]);
  }
  
  logger.info('Sample SAML metadata inserted successfully');
}

/**
 * Get SAML metadata by client ID
 */
async function getSamlMetadata(clientId) {
  const client = await pool.connect();
  
  try {
    const query = `
      SELECT 
        client_id, entity_id, sso_url, x509_certificate, single_logout_url,
        name_id_format, signature_algorithm, encryption_algorithm, enabled,
        created_at, updated_at
      FROM saml_metadata 
      WHERE client_id = $1 AND enabled = true
    `;
    
    const result = await client.query(query, [clientId]);
    
    if (result.rows.length === 0) {
      return null;
    }
    
    return result.rows[0];
    
  } catch (error) {
    logger.error('Error getting SAML metadata:', error);
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Create or update SAML metadata
 */
async function upsertSamlMetadata(metadata) {
  const client = await pool.connect();
  
  try {
    const query = `
      INSERT INTO saml_metadata (
        client_id, entity_id, sso_url, x509_certificate, single_logout_url,
        name_id_format, signature_algorithm, encryption_algorithm
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      ON CONFLICT (client_id) 
      DO UPDATE SET
        entity_id = EXCLUDED.entity_id,
        sso_url = EXCLUDED.sso_url,
        x509_certificate = EXCLUDED.x509_certificate,
        single_logout_url = EXCLUDED.single_logout_url,
        name_id_format = EXCLUDED.name_id_format,
        signature_algorithm = EXCLUDED.signature_algorithm,
        encryption_algorithm = EXCLUDED.encryption_algorithm,
        updated_at = CURRENT_TIMESTAMP
      RETURNING *
    `;
    
    const result = await client.query(query, [
      metadata.client_id, metadata.entity_id, metadata.sso_url,
      metadata.x509_certificate, metadata.single_logout_url,
      metadata.name_id_format, metadata.signature_algorithm,
      metadata.encryption_algorithm
    ]);
    
    return result.rows[0];
    
  } catch (error) {
    logger.error('Error upserting SAML metadata:', error);
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Delete SAML metadata by client ID
 */
async function deleteSamlMetadata(clientId) {
  const client = await pool.connect();
  
  try {
    const query = 'DELETE FROM saml_metadata WHERE client_id = $1';
    const result = await client.query(query, [clientId]);
    
    return result.rowCount > 0;
    
  } catch (error) {
    logger.error('Error deleting SAML metadata:', error);
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Get all SAML metadata
 */
async function getAllSamlMetadata() {
  const client = await pool.connect();
  
  try {
    const query = `
      SELECT 
        client_id, entity_id, sso_url, x509_certificate, single_logout_url,
        name_id_format, signature_algorithm, encryption_algorithm, enabled,
        created_at, updated_at
      FROM saml_metadata 
      WHERE enabled = true
      ORDER BY client_id
    `;
    
    const result = await client.query(query);
    return result.rows;
    
  } catch (error) {
    logger.error('Error getting all SAML metadata:', error);
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Test database connection
 */
async function testConnection() {
  const client = await pool.connect();
  
  try {
    const result = await client.query('SELECT NOW() as current_time');
    return {
      success: true,
      message: 'Database connection successful',
      timestamp: result.rows[0].current_time
    };
  } catch (error) {
    logger.error('Database connection test failed:', error);
    return {
      success: false,
      message: 'Database connection failed: ' + error.message
    };
  } finally {
    client.release();
  }
}

/**
 * Close database pool
 */
async function closePool() {
  await pool.end();
  logger.info('Database pool closed');
}

module.exports = {
  pool,
  initializeDatabase,
  getSamlMetadata,
  upsertSamlMetadata,
  deleteSamlMetadata,
  getAllSamlMetadata,
  testConnection,
  closePool
}; 