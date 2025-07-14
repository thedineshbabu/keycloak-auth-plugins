package kf.keycloak.plugin;

import kf.keycloak.plugin.config.OtpConfig;
import kf.keycloak.plugin.model.OtpRequest;
import kf.keycloak.plugin.model.OtpResponse;
import kf.keycloak.plugin.service.OtpService;
import kf.keycloak.plugin.util.OtpLogger;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OTP service functionality
 */
public class OtpServiceTest {
    
    @Mock
    private KeycloakSession session;
    
    @Mock
    private RealmModel realm;
    
    private OtpService otpService;
    private OtpConfig config;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock realm configuration
        when(realm.getAttribute("otp.enabled")).thenReturn("true");
        when(realm.getAttribute("otp.external.otp.api.url")).thenReturn("https://api.example.com/otp");
        when(realm.getAttribute("otp.external.eligibility.api.url")).thenReturn("https://api.example.com/eligibility");
        when(realm.getAttribute("otp.length")).thenReturn("6");
        when(realm.getAttribute("otp.ttl")).thenReturn("300");
        when(realm.getAttribute("otp.fail.if.eligibility.fails")).thenReturn("false");
        when(realm.getAttribute("otp.rate.limit.enabled")).thenReturn("false");
        
        config = new OtpConfig(session, realm);
        otpService = new OtpService(session, realm);
    }
    
    @Test
    public void testOtpRequestValidation() {
        // Test valid request
        OtpRequest validRequest = new OtpRequest("test@example.com", "user123", "session456");
        assertTrue("Valid request should pass validation", validRequest.isValid());
        assertNull("Valid request should have no validation error", validRequest.getValidationError());
        
        // Test invalid request - missing email
        OtpRequest invalidRequest1 = new OtpRequest(null, "user123", "session456");
        assertFalse("Invalid request should fail validation", invalidRequest1.isValid());
        assertNotNull("Invalid request should have validation error", invalidRequest1.getValidationError());
        
        // Test invalid request - invalid email
        OtpRequest invalidRequest2 = new OtpRequest("invalid-email", "user123", "session456");
        assertFalse("Invalid email should fail validation", invalidRequest2.isValid());
        assertNotNull("Invalid email should have validation error", invalidRequest2.getValidationError());
        
        // Test invalid request - missing user ID
        OtpRequest invalidRequest3 = new OtpRequest("test@example.com", null, "session456");
        assertFalse("Missing user ID should fail validation", invalidRequest3.isValid());
        assertNotNull("Missing user ID should have validation error", invalidRequest3.getValidationError());
    }
    
    @Test
    public void testOtpResponseCreation() {
        // Test success response
        OtpResponse successResponse = OtpResponse.success("otp123", 
            java.time.LocalDateTime.now().plusMinutes(5), "test@example.com", "user123");
        assertTrue("Success response should indicate success", successResponse.isSuccess());
        assertEquals("Success response should have correct OTP ID", "otp123", successResponse.getOtpId());
        assertNull("Success response should have no error", successResponse.getError());
        
        // Test error response
        OtpResponse errorResponse = OtpResponse.error("Test error", "TEST_ERROR");
        assertFalse("Error response should indicate failure", errorResponse.isSuccess());
        assertEquals("Error response should have correct error message", "Test error", errorResponse.getError());
        assertEquals("Error response should have correct error code", "TEST_ERROR", errorResponse.getErrorCode());
        
        // Test specific error types
        OtpResponse validationError = OtpResponse.validationError("Validation failed");
        assertEquals("Validation error should have correct error code", "VALIDATION_ERROR", validationError.getErrorCode());
        
        OtpResponse userNotFound = OtpResponse.userNotFound();
        assertEquals("User not found should have correct error code", "USER_NOT_FOUND", userNotFound.getErrorCode());
        
        OtpResponse rateLimitExceeded = OtpResponse.rateLimitExceeded();
        assertEquals("Rate limit exceeded should have correct error code", "RATE_LIMIT_EXCEEDED", rateLimitExceeded.getErrorCode());
    }
    
    @Test
    public void testOtpConfigDefaults() {
        // Test default values when attributes are not set
        when(realm.getAttribute(anyString())).thenReturn(null);
        
        OtpConfig defaultConfig = new OtpConfig(session, realm);
        
        assertTrue("OTP should be enabled by default", defaultConfig.isEnabled());
        assertEquals("Default OTP length should be 6", 6, defaultConfig.getOtpLength());
        assertEquals("Default OTP TTL should be 300 seconds", 300, defaultConfig.getOtpTtl());
        assertFalse("Default fail-if-eligibility-fails should be false", defaultConfig.shouldFailIfEligibilityFails());
        assertTrue("Default rate limiting should be enabled", defaultConfig.isRateLimitEnabled());
    }
    
    @Test
    public void testOtpConfigValidation() {
        // Test valid configuration
        OtpConfig.ValidationResult validResult = config.validateConfiguration();
        assertTrue("Valid configuration should pass validation", validResult.isValid());
        assertTrue("Valid configuration should have no errors", validResult.getErrors().isEmpty());
        
        // Test invalid configuration - missing API URLs
        when(realm.getAttribute("otp.external.otp.api.url")).thenReturn(null);
        when(realm.getAttribute("otp.external.eligibility.api.url")).thenReturn(null);
        
        OtpConfig invalidConfig = new OtpConfig(session, realm);
        OtpConfig.ValidationResult invalidResult = invalidConfig.validateConfiguration();
        assertFalse("Invalid configuration should fail validation", invalidResult.isValid());
        assertFalse("Invalid configuration should have errors", invalidResult.getErrors().isEmpty());
    }
    
    @Test
    public void testOtpServiceConfiguration() {
        // Test that OTP service can be created with configuration
        assertNotNull("OTP service should be created", otpService);
        assertNotNull("OTP service should have configuration", otpService.getConfig());
        
        // Test configuration values
        assertEquals("OTP length should match configuration", 6, otpService.getConfig().getOtpLength());
        assertEquals("OTP TTL should match configuration", 300, otpService.getConfig().getOtpTtl());
        assertTrue("OTP should be enabled", otpService.getConfig().isEnabled());
    }
} 