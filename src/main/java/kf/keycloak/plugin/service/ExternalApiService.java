package kf.keycloak.plugin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * External API service for sending magiclinks to external endpoints
 * Handles HTTP requests with authentication, retry logic, and error handling
 */
public class ExternalApiService {
    
    private final String endpoint;
    private final String authToken;
    private final String authType;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    /**
     * Constructor with configuration
     * @param endpoint External API endpoint URL
     * @param authToken Authentication token
     * @param authType Authentication type (bearer, basic, apikey)
     */
    public ExternalApiService(String endpoint, String authToken, String authType) {
        this.endpoint = endpoint;
        this.authToken = authToken;
        this.authType = authType != null ? authType : "bearer";
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }
    
    /**
     * Send magiclink to external API
     * @param email User email
     * @param magiclink Generated magiclink URL
     * @param tokenId Token identifier for tracking
     * @param userId User identifier
     * @return ApiResponse with success status and details
     */
    public ApiResponse sendMagiclink(String email, String magiclink, String tokenId, String userId) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return ApiResponse.error("External API endpoint not configured", "CONFIG_ERROR");
        }
        
        // Prepare request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("magiclink", magiclink);
        payload.put("tokenId", tokenId);
        payload.put("userId", userId);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("source", "keycloak-magiclink-plugin");
        
        // Execute request with retry logic
        return executeWithRetry(payload, 3);
    }
    
    /**
     * Execute HTTP request with retry logic
     * @param payload Request payload
     * @param maxRetries Maximum retry attempts
     * @return ApiResponse with result
     */
    private ApiResponse executeWithRetry(Map<String, Object> payload, int maxRetries) {
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            attempt++;
            
            try {
                // Execute HTTP request
                ApiResponse response = executeHttpRequest(payload);
                
                // Return immediately on success
                if (response.isSuccess()) {
                    return response;
                }
                
                // If this is the last attempt, return the error
                if (attempt > maxRetries) {
                    return response;
                }
                
                // Wait before retry (exponential backoff)
                long waitTime = calculateRetryWaitTime(attempt);
                Thread.sleep(waitTime);
                
            } catch (Exception e) {
                // If this is the last attempt, return error
                if (attempt > maxRetries) {
                    return ApiResponse.error("External API call failed after " + maxRetries + " retries: " + e.getMessage(), 
                        "EXTERNAL_API_ERROR");
                }
                
                // Wait before retry
                try {
                    long waitTime = calculateRetryWaitTime(attempt);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ApiResponse.error("Request interrupted", "INTERRUPTED");
                }
            }
        }
        
        return ApiResponse.error("Unexpected error in retry logic", "INTERNAL_ERROR");
    }
    
    /**
     * Execute single HTTP request
     * @param payload Request payload
     * @return ApiResponse with result
     */
    private ApiResponse executeHttpRequest(Map<String, Object> payload) {
        HttpPost request = null;
        
        try {
            // Create HTTP POST request
            request = new HttpPost(endpoint);
            
            // Set headers
            request.setHeader("Content-Type", "application/json");
            request.setHeader("User-Agent", "keycloak-magiclink-plugin/1.0");
            
            // Set authentication header
            setAuthenticationHeader(request);
            
            // Set request body
            String jsonPayload = objectMapper.writeValueAsString(payload);
            request.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
            
            // Execute request
            HttpResponse response = httpClient.execute(request);
            
            // Process response
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = "";
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }
            
            // Check if successful
            if (statusCode >= 200 && statusCode < 300) {
                return ApiResponse.success(responseBody, statusCode);
            } else {
                return ApiResponse.error("API call failed with status " + statusCode + ": " + responseBody, 
                    "HTTP_ERROR_" + statusCode);
            }
            
        } catch (IOException e) {
            return ApiResponse.error("Network error: " + e.getMessage(), "NETWORK_ERROR");
        } catch (Exception e) {
            return ApiResponse.error("Unexpected error: " + e.getMessage(), "INTERNAL_ERROR");
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }
    
    /**
     * Set authentication header based on configuration
     * @param request HTTP request
     */
    private void setAuthenticationHeader(HttpPost request) {
        if (authToken == null || authToken.trim().isEmpty()) {
            return;
        }
        
        switch (authType.toLowerCase()) {
            case "bearer":
                request.setHeader("Authorization", "Bearer " + authToken);
                break;
            case "basic":
                request.setHeader("Authorization", "Basic " + authToken);
                break;
            case "apikey":
                request.setHeader("Authorization", authToken);
                break;
            default:
                request.setHeader("Authorization", authToken);
                break;
        }
    }
    
    /**
     * Calculate retry wait time with exponential backoff
     * @param attempt Current attempt number
     * @return Wait time in milliseconds
     */
    private long calculateRetryWaitTime(int attempt) {
        // Exponential backoff: 1s, 2s, 4s, 8s, etc.
        return Math.min(1000L * (1L << (attempt - 1)), 30000L); // Cap at 30 seconds
    }
    
    /**
     * Create HTTP client with configured timeouts
     * @return CloseableHttpClient
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(30000)
            .setSocketTimeout(30000)
            .setConnectionRequestTimeout(30000)
            .build();
        
        return HttpClients.custom()
            .setDefaultRequestConfig(config)
            .build();
    }
    
    /**
     * Test external API connectivity
     * @return ApiResponse with test result
     */
    public ApiResponse testConnection() {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return ApiResponse.error("External API endpoint not configured", "CONFIG_ERROR");
        }
        
        // Create a simple test payload
        Map<String, Object> testPayload = new HashMap<>();
        testPayload.put("test", true);
        testPayload.put("timestamp", LocalDateTime.now().toString());
        testPayload.put("source", "keycloak-magiclink-plugin-test");
        
        return executeHttpRequest(testPayload);
    }
    
    /**
     * Clean up resources
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            // Log error if needed
        }
    }
    
    /**
     * API response container
     */
    public static class ApiResponse {
        private final boolean success;
        private final String responseBody;
        private final int statusCode;
        private final String errorMessage;
        private final String errorCode;
        private final LocalDateTime timestamp;
        
        private ApiResponse(boolean success, String responseBody, int statusCode, 
                          String errorMessage, String errorCode) {
            this.success = success;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
            this.timestamp = LocalDateTime.now();
        }
        
        public static ApiResponse success(String responseBody, int statusCode) {
            return new ApiResponse(true, responseBody, statusCode, null, null);
        }
        
        public static ApiResponse error(String errorMessage, String errorCode) {
            return new ApiResponse(false, null, -1, errorMessage, errorCode);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getResponseBody() { return responseBody; }
        public int getStatusCode() { return statusCode; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return "ApiResponse{" +
                   "success=" + success +
                   ", statusCode=" + statusCode +
                   ", errorMessage='" + errorMessage + '\'' +
                   ", errorCode='" + errorCode + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
} 