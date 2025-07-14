package kf.keycloak.plugin.service;

import kf.keycloak.plugin.config.OtpConfig;
import kf.keycloak.plugin.model.OtpRequest;
import kf.keycloak.plugin.model.OtpResponse;
import kf.keycloak.plugin.model.EligibilityResponse;
import kf.keycloak.plugin.util.OtpLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core service for OTP generation, storage, and validation
 * Manages OTP lifecycle and integrates with external APIs
 */
public class OtpService {
    
    // In-memory OTP storage with TTL
    private static final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    // Rate limiting storage
    private static final ConcurrentHashMap<String, RateLimitData> rateLimitData = new ConcurrentHashMap<>();
    
    private final OtpConfig config;
    private final EligibilityService eligibilityService;
    private final ExternalApiService externalApiService;
    private final OtpLogger logger;
    private final SecureRandom secureRandom;
    
    /**
     * Constructor with session and realm
     * @param session Keycloak session
     * @param realm Realm model
     */
    public OtpService(KeycloakSession session, RealmModel realm) {
        this.config = new OtpConfig(session, realm);
        this.logger = OtpLogger.forSession(session, realm, "OtpService");
        this.secureRandom = new SecureRandom();
        
        // Initialize eligibility service
        this.eligibilityService = new EligibilityService(config, logger);
        
        // Initialize external API service if configured
        if (config.isExternalApiConfigured()) {
            this.externalApiService = new ExternalApiService(
                config.getExternalOtpApiUrl(),
                config.getExternalApiToken(),
                config.getExternalApiType()
            );
        } else {
            this.externalApiService = null;
        }
    }
    
    /**
     * Generate OTP for user if eligible
     * @param request OTP generation request
     * @return OtpResponse with generation result
     */
    public OtpResponse generateOtp(OtpRequest request) {
        try {
            // Validate request
            if (!request.isValid()) {
                logger.warn("Invalid OTP request: " + request.getValidationError());
                return OtpResponse.validationError(request.getValidationError());
            }
            
            // Check if OTP is enabled
            if (!config.isEnabled()) {
                logger.warn("OTP generation attempted but feature is disabled");
                return OtpResponse.featureDisabled();
            }
            
            // Check if realm is enabled
            if (!config.isRealmEnabled()) {
                logger.warn("OTP generation attempted but realm is not enabled");
                return OtpResponse.featureDisabled();
            }
            
            // Check eligibility
            EligibilityResponse eligibility = eligibilityService.checkEligibility(request.getEmail());
            if (!eligibility.isEnabled()) {
                logger.info("User not eligible for OTP: " + request.getEmail());
                return OtpResponse.error("User not eligible for OTP", "USER_NOT_ELIGIBLE");
            }
            
            // Apply rate limiting
            String rateLimitKey = getRateLimitKey(request.getEmail(), request.getUserId());
            if (isRateLimited(rateLimitKey)) {
                logger.warn("Rate limit exceeded for user: " + request.getEmail());
                return OtpResponse.rateLimitExceeded();
            }
            
            // Generate OTP
            String otp = generateSecureOtp(request.getOtpLength() != null ? request.getOtpLength() : config.getOtpLength());
            String otpId = generateOtpId();
            
            // Log the generated OTP before sending to external API
            logger.info("Generated OTP for user", createLogContext(request, otp, otpId));
            
            // Calculate expiration
            int ttlSeconds = request.getTtlSeconds() != null ? request.getTtlSeconds() : config.getOtpTtl();
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
            
            // Store OTP data
            OtpData otpData = new OtpData(
                otp,
                request.getEmail(),
                request.getUserId(),
                request.getSessionId(),
                expiresAt,
                request.getRedirectUrl(),
                false // not used yet
            );
            otpStorage.put(otpId, otpData);
            
            // Log successful generation
            logger.logOtpGeneration(request.getUserId(), request.getEmail(), otpId, true);
            
            // Send to external API if configured
            if (externalApiService != null) {
                try {
                    long startTime = System.currentTimeMillis();
                    ExternalApiService.ApiResponse apiResponse = externalApiService.sendOtp(
                        request.getEmail(), otp, otpId, request.getUserId());
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    logger.logExternalApiCall(config.getExternalOtpApiUrl(), 
                        apiResponse.isSuccess(), apiResponse.getStatusCode(), responseTime);
                    
                    if (!apiResponse.isSuccess()) {
                        logger.error("External API call failed", createLogContext(request, apiResponse));
                        return OtpResponse.externalApiError("Failed to send OTP");
                    }
                    
                } catch (Exception e) {
                    logger.error("External API call exception", e);
                    return OtpResponse.externalApiError("External API unavailable");
                }
            }
            
            // Record rate limit usage
            recordRateLimitUsage(rateLimitKey);
            
            // Create success response
            OtpResponse response = OtpResponse.success(otpId, expiresAt, request.getEmail(), request.getUserId());
            
            logger.info("OTP generated successfully", createLogContext(request, response));
            return response;
            
        } catch (Exception e) {
            logger.error("Unexpected error generating OTP", e);
            return OtpResponse.internalError(e.getMessage());
        }
    }
    
    /**
     * Validate OTP input
     * @param otpId OTP ID
     * @param otpInput User's OTP input
     * @return Validation result
     */
    public OtpValidationResult validateOtp(String otpId, String otpInput) {
        try {
            // Retrieve stored OTP data
            OtpData otpData = otpStorage.get(otpId);
            if (otpData == null) {
                logger.logOtpValidation(null, otpId, false, "OTP not found");
                return OtpValidationResult.invalid("Invalid OTP");
            }
            
            // Check if already used
            if (otpData.isUsed()) {
                logger.logOtpValidation(otpData.getUserId(), otpId, false, "OTP already used");
                return OtpValidationResult.invalid("OTP already used");
            }
            
            // Check if expired
            if (LocalDateTime.now().isAfter(otpData.getExpiresAt())) {
                logger.logOtpValidation(otpData.getUserId(), otpId, false, "OTP expired");
                return OtpValidationResult.invalid("OTP expired");
            }
            
            // Validate input
            if (!otpData.getOtp().equals(otpInput)) {
                logger.logOtpValidation(otpData.getUserId(), otpId, false, "Invalid OTP input");
                return OtpValidationResult.invalid("Invalid OTP");
            }
            
            // Mark as used
            otpData.setUsed(true);
            
            // Log successful validation
            logger.logOtpValidation(otpData.getUserId(), otpId, true, "Validated successfully");
            
            return OtpValidationResult.success(otpData.getUserId(), otpData.getEmail(), otpData.getRedirectUrl());
            
        } catch (Exception e) {
            logger.error("Error validating OTP", e);
            return OtpValidationResult.invalid("Validation failed");
        }
    }
    
    /**
     * Get OTP status
     * @param otpId OTP ID
     * @return OTP status information
     */
    public OtpStatus getOtpStatus(String otpId) {
        try {
            OtpData otpData = otpStorage.get(otpId);
            if (otpData == null) {
                return OtpStatus.notFound();
            }
            
            boolean expired = LocalDateTime.now().isAfter(otpData.getExpiresAt());
            boolean used = otpData.isUsed();
            
            if (expired) {
                return OtpStatus.expired();
            } else if (used) {
                return OtpStatus.used();
            } else {
                return OtpStatus.valid(otpData.getEmail(), otpData.getExpiresAt());
            }
            
        } catch (Exception e) {
            logger.error("Error checking OTP status", e);
            return OtpStatus.error("Status check failed");
        }
    }
    
    /**
     * Clean up expired OTPs
     * @return Number of OTPs cleaned up
     */
    public int cleanupExpiredOtps() {
        int cleanedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (String otpId : otpStorage.keySet()) {
            OtpData otpData = otpStorage.get(otpId);
            if (otpData != null && now.isAfter(otpData.getExpiresAt())) {
                otpStorage.remove(otpId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            logger.info("Cleaned up " + cleanedCount + " expired OTPs");
        }
        
        return cleanedCount;
    }
    
    /**
     * Generate secure OTP
     * @param length OTP length
     * @return Generated OTP
     */
    private String generateSecureOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
    
    /**
     * Generate unique OTP ID
     * @return Unique OTP ID
     */
    private String generateOtpId() {
        return "otp_" + System.currentTimeMillis() + "_" + secureRandom.nextInt(10000);
    }
    
    /**
     * Check if user is rate limited
     * @param rateLimitKey Rate limit key
     * @return true if rate limited
     */
    private boolean isRateLimited(String rateLimitKey) {
        if (!config.isRateLimitEnabled()) {
            return false;
        }
        
        RateLimitData data = rateLimitData.get(rateLimitKey);
        if (data == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(data.getWindowStart().plusSeconds(config.getRateLimitWindow()))) {
            // Window expired, reset
            rateLimitData.remove(rateLimitKey);
            return false;
        }
        
        return data.getCount().get() >= config.getRateLimitRequests();
    }
    
    /**
     * Record rate limit usage
     * @param rateLimitKey Rate limit key
     */
    private void recordRateLimitUsage(String rateLimitKey) {
        if (!config.isRateLimitEnabled()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        RateLimitData data = rateLimitData.computeIfAbsent(rateLimitKey, 
            k -> new RateLimitData(now, new AtomicInteger(0)));
        
        data.getCount().incrementAndGet();
    }
    
    /**
     * Get rate limit key
     * @param email User's email
     * @param userId User's ID
     * @return Rate limit key
     */
    private String getRateLimitKey(String email, String userId) {
        return email + ":" + userId;
    }
    
    /**
     * Create log context for request
     * @param request OTP request
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(OtpRequest request) {
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        context.put("email", request.getEmail());
        context.put("userId", request.getUserId());
        context.put("sessionId", request.getSessionId());
        return context;
    }
    
    /**
     * Create log context for request and response
     * @param request OTP request
     * @param response OTP response
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(OtpRequest request, OtpResponse response) {
        java.util.Map<String, Object> context = createLogContext(request);
        context.put("success", response.isSuccess());
        context.put("otpId", response.getOtpId());
        context.put("error", response.getError());
        return context;
    }
    
    /**
     * Create log context for request and API response
     * @param request OTP request
     * @param apiResponse API response
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(OtpRequest request, ExternalApiService.ApiResponse apiResponse) {
        java.util.Map<String, Object> context = createLogContext(request);
        context.put("apiSuccess", apiResponse.isSuccess());
        context.put("apiStatusCode", apiResponse.getStatusCode());
        return context;
    }
    
    /**
     * Create log context for request with OTP details
     * @param request OTP request
     * @param otp Generated OTP
     * @param otpId OTP ID
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(OtpRequest request, String otp, String otpId) {
        java.util.Map<String, Object> context = createLogContext(request);
        context.put("otp", otp);
        context.put("otpId", otpId);
        return context;
    }
    
    /**
     * Clean up rate limit data
     */
    public static void cleanupRateLimitData() {
        LocalDateTime now = LocalDateTime.now();
        rateLimitData.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().getWindowStart().plusSeconds(3600))); // 1 hour window
    }
    
    /**
     * Get configuration
     * @return OTP configuration
     */
    public OtpConfig getConfig() {
        return config;
    }
    
    /**
     * OTP data storage class
     */
    private static class OtpData {
        private final String otp;
        private final String email;
        private final String userId;
        private final String sessionId;
        private final LocalDateTime expiresAt;
        private final String redirectUrl;
        private boolean used;
        
        public OtpData(String otp, String email, String userId, String sessionId, 
                      LocalDateTime expiresAt, String redirectUrl, boolean used) {
            this.otp = otp;
            this.email = email;
            this.userId = userId;
            this.sessionId = sessionId;
            this.expiresAt = expiresAt;
            this.redirectUrl = redirectUrl;
            this.used = used;
        }
        
        public String getOtp() { return otp; }
        public String getEmail() { return email; }
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getRedirectUrl() { return redirectUrl; }
        public boolean isUsed() { return used; }
        public void setUsed(boolean used) { this.used = used; }
    }
    
    /**
     * Rate limit data class
     */
    private static class RateLimitData {
        private final LocalDateTime windowStart;
        private final AtomicInteger count;
        
        public RateLimitData(LocalDateTime windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
        
        public LocalDateTime getWindowStart() { return windowStart; }
        public AtomicInteger getCount() { return count; }
    }
    
    /**
     * OTP validation result class
     */
    public static class OtpValidationResult {
        private final boolean valid;
        private final String userId;
        private final String email;
        private final String redirectUrl;
        private final String error;
        
        private OtpValidationResult(boolean valid, String userId, String email, String redirectUrl, String error) {
            this.valid = valid;
            this.userId = userId;
            this.email = email;
            this.redirectUrl = redirectUrl;
            this.error = error;
        }
        
        public static OtpValidationResult success(String userId, String email, String redirectUrl) {
            return new OtpValidationResult(true, userId, email, redirectUrl, null);
        }
        
        public static OtpValidationResult invalid(String error) {
            return new OtpValidationResult(false, null, null, null, error);
        }
        
        public boolean isValid() { return valid; }
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getRedirectUrl() { return redirectUrl; }
        public String getError() { return error; }
    }
    
    /**
     * OTP status class
     */
    public static class OtpStatus {
        private final String status;
        private final String email;
        private final LocalDateTime expiresAt;
        private final String error;
        
        private OtpStatus(String status, String email, LocalDateTime expiresAt, String error) {
            this.status = status;
            this.email = email;
            this.expiresAt = expiresAt;
            this.error = error;
        }
        
        public static OtpStatus valid(String email, LocalDateTime expiresAt) {
            return new OtpStatus("valid", email, expiresAt, null);
        }
        
        public static OtpStatus expired() {
            return new OtpStatus("expired", null, null, "OTP has expired");
        }
        
        public static OtpStatus used() {
            return new OtpStatus("used", null, null, "OTP has already been used");
        }
        
        public static OtpStatus notFound() {
            return new OtpStatus("not_found", null, null, "OTP not found");
        }
        
        public static OtpStatus error(String error) {
            return new OtpStatus("error", null, null, error);
        }
        
        public String getStatus() { return status; }
        public String getEmail() { return email; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getError() { return error; }
    }
} 