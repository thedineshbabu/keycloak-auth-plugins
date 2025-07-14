package kf.keycloak.plugin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kf.keycloak.plugin.config.OtpConfig;
import kf.keycloak.plugin.model.EligibilityResponse;
import kf.keycloak.plugin.util.OtpLogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service for checking user eligibility for OTP MFA via external API
 * Handles HTTP calls to external eligibility API and fallback behavior
 */
public class EligibilityService {
    
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    
    private final String eligibilityApiUrl;
    private final String apiToken;
    private final String authType;
    private final boolean failIfApiFails;
    private final OtpLogger logger;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor with configuration
     * @param eligibilityApiUrl External eligibility API URL
     * @param apiToken API authentication token
     * @param authType API authentication type
     * @param failIfApiFails Whether to fail authentication if API fails
     * @param logger Logger instance
     */
    public EligibilityService(String eligibilityApiUrl, String apiToken, String authType, 
                           boolean failIfApiFails, OtpLogger logger) {
        this.eligibilityApiUrl = eligibilityApiUrl;
        this.apiToken = apiToken;
        this.authType = authType;
        this.failIfApiFails = failIfApiFails;
        this.logger = logger;
        this.objectMapper = new ObjectMapper();
        
        // Configure HTTP client with timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(DEFAULT_CONNECT_TIMEOUT_SECONDS))
                .setSocketTimeout((int) TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS))
                .build();
        
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * Constructor with OTP configuration
     * @param config OTP configuration
     * @param logger Logger instance
     */
    public EligibilityService(OtpConfig config, OtpLogger logger) {
        this(
            config.getExternalEligibilityApiUrl(),
            config.getExternalApiToken(),
            config.getExternalApiType(),
            config.shouldFailIfEligibilityFails(),
            logger
        );
    }
    
    /**
     * Check if user is eligible for OTP MFA
     * @param email User's email address
     * @return EligibilityResponse with eligibility result
     */
    public EligibilityResponse checkEligibility(String email) {
        if (eligibilityApiUrl == null || eligibilityApiUrl.trim().isEmpty()) {
            logger.warn("Eligibility API URL not configured");
            return EligibilityResponse.configurationError(email);
        }
        
        try {
            logger.debug("Checking eligibility for email: " + email);
            
            // Build API URL with email parameter
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            String url = eligibilityApiUrl + "?email=" + encodedEmail;
            
            // Create HTTP request
            HttpGet request = new HttpGet(url);
            
            // Add authentication header if token is provided
            if (apiToken != null && !apiToken.trim().isEmpty()) {
                String authHeader = buildAuthHeader(apiToken, authType);
                request.setHeader("Authorization", authHeader);
            }
            
            // Add standard headers
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("User-Agent", "Keycloak-OTP-Plugin/1.0");
            
            // Execute request
            long startTime = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(request);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log API call
            logger.logExternalApiCall(eligibilityApiUrl, true, response.getStatusLine().getStatusCode(), responseTime);
            
            // Parse response
            return parseEligibilityResponse(response, email);
            
        } catch (IOException e) {
            logger.error("Network error during eligibility check", e);
            return handleApiFailure(email, "Network error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during eligibility check", e);
            return handleApiFailure(email, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Parse eligibility API response
     * @param response HTTP response
     * @param email User's email
     * @return Parsed EligibilityResponse
     * @throws IOException If response parsing fails
     */
    private EligibilityResponse parseEligibilityResponse(HttpResponse response, String email) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        
        // Check for successful response
        if (statusCode >= 200 && statusCode < 300) {
            try {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // Parse enabled field
                boolean enabled = false;
                String reason = null;
                
                if (jsonNode.has("enabled")) {
                    enabled = jsonNode.get("enabled").asBoolean();
                }
                
                if (jsonNode.has("reason")) {
                    reason = jsonNode.get("reason").asText();
                }
                
                // Log eligibility result
                logger.logEligibilityCheck(email, enabled, true, reason);
                
                return EligibilityResponse.success(enabled, email, reason);
                
            } catch (Exception e) {
                logger.error("Failed to parse eligibility response", e);
                return handleApiFailure(email, "Invalid response format: " + e.getMessage());
            }
        } else {
            // Handle error response
            logger.warn("Eligibility API returned error status: " + statusCode);
            return handleApiFailure(email, "API returned status " + statusCode, statusCode);
        }
    }
    
    /**
     * Handle API failure with fallback logic
     * @param email User's email
     * @param errorMessage Error message
     * @return EligibilityResponse with fallback behavior
     */
    private EligibilityResponse handleApiFailure(String email, String errorMessage) {
        return handleApiFailure(email, errorMessage, null);
    }
    
    /**
     * Handle API failure with fallback logic
     * @param email User's email
     * @param errorMessage Error message
     * @param statusCode HTTP status code
     * @return EligibilityResponse with fallback behavior
     */
    private EligibilityResponse handleApiFailure(String email, String errorMessage, Integer statusCode) {
        logger.warn("Eligibility API failed: " + errorMessage);
        
        if (failIfApiFails) {
            // Fail-closed behavior: treat as not eligible
            logger.info("Using fail-closed behavior: treating user as not eligible");
            return EligibilityResponse.apiFailure(email, errorMessage, statusCode);
        } else {
            // Fail-open behavior: treat as eligible
            logger.info("Using fail-open behavior: treating user as eligible");
            return EligibilityResponse.enabled(email, "Fallback: API unavailable");
        }
    }
    
    /**
     * Build authentication header
     * @param token API token
     * @param authType Authentication type
     * @return Formatted authentication header
     */
    private String buildAuthHeader(String token, String authType) {
        if ("bearer".equalsIgnoreCase(authType)) {
            return "Bearer " + token;
        } else if ("apikey".equalsIgnoreCase(authType)) {
            return "ApiKey " + token;
        } else {
            // Default to Bearer
            return "Bearer " + token;
        }
    }
    
    /**
     * Test API connectivity
     * @return Test result
     */
    public ApiTestResult testApiConnectivity() {
        if (eligibilityApiUrl == null || eligibilityApiUrl.trim().isEmpty()) {
            return ApiTestResult.error("Eligibility API URL not configured");
        }
        
        try {
            // Use a test email for connectivity check
            String testEmail = "test@example.com";
            String encodedEmail = URLEncoder.encode(testEmail, StandardCharsets.UTF_8.toString());
            String url = eligibilityApiUrl + "?email=" + encodedEmail;
            
            HttpGet request = new HttpGet(url);
            
            // Add authentication header if token is provided
            if (apiToken != null && !apiToken.trim().isEmpty()) {
                String authHeader = buildAuthHeader(apiToken, authType);
                request.setHeader("Authorization", authHeader);
            }
            
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setHeader("User-Agent", "Keycloak-OTP-Plugin/1.0");
            
            long startTime = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(request);
            long responseTime = System.currentTimeMillis() - startTime;
            
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                return ApiTestResult.success("API connectivity test successful", responseTime);
            } else {
                return ApiTestResult.error("API returned status " + statusCode);
            }
            
        } catch (IOException e) {
            return ApiTestResult.error("Network error: " + e.getMessage());
        } catch (Exception e) {
            return ApiTestResult.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * API test result class
     */
    public static class ApiTestResult {
        private final boolean success;
        private final String message;
        private final long responseTime;
        
        private ApiTestResult(boolean success, String message, long responseTime) {
            this.success = success;
            this.message = message;
            this.responseTime = responseTime;
        }
        
        public static ApiTestResult success(String message, long responseTime) {
            return new ApiTestResult(true, message, responseTime);
        }
        
        public static ApiTestResult error(String message) {
            return new ApiTestResult(false, message, 0);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getResponseTime() {
            return responseTime;
        }
    }
} 