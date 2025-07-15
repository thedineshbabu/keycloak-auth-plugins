-- Database Setup Script for SAML Metadata API
-- This script creates the database and tables for the mock API service

-- Create database (run this as superuser)
-- CREATE DATABASE ext_api;

-- Connect to the database
-- \c ext_api;

-- Create SAML metadata table
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

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_saml_metadata_client_id ON saml_metadata(client_id);
CREATE INDEX IF NOT EXISTS idx_saml_metadata_enabled ON saml_metadata(enabled);
CREATE INDEX IF NOT EXISTS idx_saml_metadata_created_at ON saml_metadata(created_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
DROP TRIGGER IF EXISTS update_saml_metadata_updated_at ON saml_metadata;
CREATE TRIGGER update_saml_metadata_updated_at
  BEFORE UPDATE ON saml_metadata
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data
INSERT INTO saml_metadata (
  client_id, 
  entity_id, 
  sso_url, 
  x509_certificate, 
  single_logout_url,
  name_id_format,
  signature_algorithm,
  encryption_algorithm
) VALUES 
(
  'client-a',
  'https://saml.client-a.com',
  'https://saml.client-a.com/sso',
  '-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----',
  'https://saml.client-a.com/slo',
  'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
  'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
  'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
),
(
  'client-b',
  'https://saml.client-b.com',
  'https://saml.client-b.com/sso',
  '-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----',
  'https://saml.client-b.com/slo',
  'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
  'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
  'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
),
(
  'test-client',
  'https://saml.test-client.com',
  'https://saml.test-client.com/sso',
  '-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----',
  'https://saml.test-client.com/slo',
  'urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress',
  'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256',
  'http://www.w3.org/2001/04/xmlenc#aes256-cbc'
)
ON CONFLICT (client_id) DO NOTHING;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON TABLE saml_metadata TO opal_user;
GRANT USAGE, SELECT ON SEQUENCE saml_metadata_id_seq TO opal_user;

-- Verify the setup
SELECT 
  'Database setup completed successfully' as status,
  COUNT(*) as total_records,
  COUNT(CASE WHEN enabled = true THEN 1 END) as enabled_records
FROM saml_metadata; 