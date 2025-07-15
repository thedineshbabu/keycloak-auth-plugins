/**
 * Test script for SAML Metadata API
 * Tests database connection and API endpoints
 */

const axios = require('axios');
const { testConnection, getSamlMetadata, getAllSamlMetadata } = require('./config/database');

const API_BASE_URL = 'http://localhost:3001/api/v1/saml';

/**
 * Test database connection
 */
async function testDatabaseConnection() {
  console.log('🔍 Testing database connection...');
  
  try {
    const result = await testConnection();
    if (result.success) {
      console.log('✅ Database connection successful');
      console.log(`   Timestamp: ${result.timestamp}`);
    } else {
      console.log('❌ Database connection failed');
      console.log(`   Error: ${result.message}`);
    }
  } catch (error) {
    console.log('❌ Database connection error:', error.message);
  }
}

/**
 * Test API health endpoint
 */
async function testApiHealth() {
  console.log('\n🔍 Testing API health endpoint...');
  
  try {
    const response = await axios.get(`${API_BASE_URL}/health`);
    console.log('✅ API health check successful');
    console.log(`   Status: ${response.data.status}`);
    console.log(`   Database: ${response.data.database.success ? 'Connected' : 'Failed'}`);
    console.log(`   Metadata count: ${response.data.metadata.count}`);
    console.log(`   Available clients: ${response.data.metadata.available.join(', ')}`);
  } catch (error) {
    console.log('❌ API health check failed:', error.message);
  }
}

/**
 * Test get metadata endpoint
 */
async function testGetMetadata() {
  console.log('\n🔍 Testing get metadata endpoint...');
  
  try {
    const response = await axios.get(`${API_BASE_URL}/metadata/client-a`);
    console.log('✅ Get metadata successful');
    console.log(`   Client ID: ${response.data.data.entityId}`);
    console.log(`   SSO URL: ${response.data.data.ssoUrl}`);
    console.log(`   Certificate: ${response.data.data.x509Certificate.substring(0, 50)}...`);
  } catch (error) {
    console.log('❌ Get metadata failed:', error.response?.data?.message || error.message);
  }
}

/**
 * Test get all metadata endpoint
 */
async function testGetAllMetadata() {
  console.log('\n🔍 Testing get all metadata endpoint...');
  
  try {
    const response = await axios.get(`${API_BASE_URL}/metadata`);
    console.log('✅ Get all metadata successful');
    console.log(`   Total records: ${response.data.count}`);
    response.data.data.forEach((metadata, index) => {
      console.log(`   ${index + 1}. ${metadata.client_id} - ${metadata.entity_id}`);
    });
  } catch (error) {
    console.log('❌ Get all metadata failed:', error.response?.data?.message || error.message);
  }
}

/**
 * Test create metadata endpoint
 */
async function testCreateMetadata() {
  console.log('\n🔍 Testing create metadata endpoint...');
  
  const testMetadata = {
    client_id: 'test-client-api',
    entity_id: 'https://saml.test-client-api.com',
    sso_url: 'https://saml.test-client-api.com/sso',
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
    single_logout_url: 'https://saml.test-client-api.com/slo',
    name_id_format: 'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
    signature_algorithm: 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
    encryption_algorithm: 'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
  };
  
  try {
    const response = await axios.post(`${API_BASE_URL}/metadata`, testMetadata);
    console.log('✅ Create metadata successful');
    console.log(`   Client ID: ${response.data.data.client_id}`);
    console.log(`   Entity ID: ${response.data.data.entity_id}`);
    console.log(`   Created: ${response.data.data.created_at}`);
  } catch (error) {
    console.log('❌ Create metadata failed:', error.response?.data?.message || error.message);
  }
}

/**
 * Test delete metadata endpoint
 */
async function testDeleteMetadata() {
  console.log('\n🔍 Testing delete metadata endpoint...');
  
  try {
    const response = await axios.delete(`${API_BASE_URL}/metadata/test-client-api`);
    console.log('✅ Delete metadata successful');
    console.log(`   Deleted client: ${response.data.clientId}`);
  } catch (error) {
    console.log('❌ Delete metadata failed:', error.response?.data?.message || error.message);
  }
}

/**
 * Test error handling
 */
async function testErrorHandling() {
  console.log('\n🔍 Testing error handling...');
  
  // Test invalid client ID
  try {
    await axios.get(`${API_BASE_URL}/metadata/`);
    console.log('❌ Should have failed for empty client ID');
  } catch (error) {
    if (error.response?.status === 400) {
      console.log('✅ Properly handled empty client ID');
    } else {
      console.log('❌ Unexpected error for empty client ID:', error.response?.status);
    }
  }
  
  // Test non-existent client
  try {
    await axios.get(`${API_BASE_URL}/metadata/non-existent-client`);
    console.log('❌ Should have failed for non-existent client');
  } catch (error) {
    if (error.response?.status === 404) {
      console.log('✅ Properly handled non-existent client');
    } else {
      console.log('❌ Unexpected error for non-existent client:', error.response?.status);
    }
  }
}

/**
 * Run all tests
 */
async function runTests() {
  console.log('🚀 Starting SAML Metadata API Tests\n');
  
  // Test database connection directly
  await testDatabaseConnection();
  
  // Test API endpoints
  await testApiHealth();
  await testGetMetadata();
  await testGetAllMetadata();
  await testCreateMetadata();
  await testDeleteMetadata();
  await testErrorHandling();
  
  console.log('\n✅ All tests completed!');
}

// Run tests if this file is executed directly
if (require.main === module) {
  runTests().catch(console.error);
}

module.exports = {
  runTests,
  testDatabaseConnection,
  testApiHealth,
  testGetMetadata,
  testGetAllMetadata,
  testCreateMetadata,
  testDeleteMetadata,
  testErrorHandling
}; 