package kf.keycloak.plugin.service;

import kf.keycloak.plugin.config.MagiclinkConfig;
import kf.keycloak.plugin.model.MagiclinkRequest;
import kf.keycloak.plugin.model.MagiclinkResponse;
import kf.keycloak.plugin.util.MagiclinkLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main service class that orchestrates magiclink generation and validation
 * Coordinates between TokenService, ExternalApiService, and configuration
 */
public class MagiclinkService {
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final MagiclinkConfig config;
    private final TokenService tokenService;
    private final ExternalApiService externalApiService;
    private final MagiclinkLogger logger;
    
    // Rate limiting storage
    private static final ConcurrentHashMap<String, RateLimitData> rateLimitData = new ConcurrentHashMap<>();
    
    /**
     * Constructor with session and realm
     * @param session Keycloak session
     * @param realm Realm model
     */
    public MagiclinkService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        this.config = new MagiclinkConfig(session, realm);
        this.tokenService = new TokenService(session, realm);
        this.logger = MagiclinkLogger.forSession(session, realm, "MagiclinkService");
        
        // Initialize external API service if configured
        if (config.isExternalApiConfigured()) {
            this.externalApiService = new ExternalApiService(
                config.getExternalApiEndpoint(),
                config.getExternalApiToken(),
                config.getExternalApiType()
            );
        } else {
            this.externalApiService = null;
        }
    }
    
    /**
     * Generate a magiclink for the specified user
     * @param request Magiclink request
     * @return MagiclinkResponse with generation result
     */
    public MagiclinkResponse generateMagiclink(MagiclinkRequest request) {
        try {
            // Validate request
            if (!request.isValid()) {
                logger.warn("Invalid magiclink request", createLogContext(request));
                return MagiclinkResponse.validationError("Invalid request parameters");
            }
            
            // Check if magiclink is enabled
            if (!config.isEnabled()) {
                logger.warn("Magiclink generation attempted but feature is disabled");
                return MagiclinkResponse.error("Magiclink feature is disabled", "FEATURE_DISABLED");
            }
            
            // Validate redirect URL
            if (!config.isRedirectUrlAllowed(request.getRedirectUrl())) {
                logger.warn("Unauthorized redirect URL attempted", createLogContext(request));
                return MagiclinkResponse.error("Redirect URL not allowed", "REDIRECT_URL_NOT_ALLOWED");
            }
            
            // Find user by email
            UserModel user = findUserByEmail(request.getEmail());
            if (user == null) {
                logger.warn("User not found for email: " + request.getEmail());
                return MagiclinkResponse.userNotFound();
            }
            
            // Apply rate limiting
            String rateLimitKey = getRateLimitKey(request.getEmail(), user.getId());
            if (isRateLimited(rateLimitKey)) {
                logger.warn("Rate limit exceeded for user: " + request.getEmail());
                return MagiclinkResponse.error("Rate limit exceeded", "RATE_LIMIT_EXCEEDED");
            }
            
            // Generate magiclink token
            String magiclink = tokenService.generateMagiclinkUrl(user, request, config.getBaseUrl());
            String tokenId = tokenService.extractTokenId(magiclink);
            
            // Calculate expiration
            int expirationMinutes = request.getExpirationMinutes() != null ? 
                request.getExpirationMinutes() : config.getTokenExpiryMinutes();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
            
            // Log successful generation
            logger.logTokenGeneration(user.getId(), request.getEmail(), tokenId, true);
            
            // Send to external API if configured
            if (externalApiService != null) {
                try {
                    long startTime = System.currentTimeMillis();
                    ExternalApiService.ApiResponse apiResponse = externalApiService.sendMagiclink(
                        request.getEmail(), magiclink, tokenId, user.getId());
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    logger.logExternalApiCall(config.getExternalApiEndpoint(), 
                        apiResponse.isSuccess(), apiResponse.getStatusCode(), responseTime);
                    
                    if (!apiResponse.isSuccess()) {
                        logger.error("External API call failed", createLogContext(request, apiResponse));
                        return MagiclinkResponse.error("Failed to send magiclink", "EXTERNAL_API_ERROR");
                    }
                    
                } catch (Exception e) {
                    logger.error("External API call exception", e);
                    return MagiclinkResponse.error("External API unavailable", "EXTERNAL_API_ERROR");
                }
            }
            
            // Record rate limit usage
            recordRateLimitUsage(rateLimitKey);
            
            // Create success response
            MagiclinkResponse response = MagiclinkResponse.success(magiclink, tokenId, expiresAt);
            
            logger.info("Magiclink generated successfully", createLogContext(request, response));
            return response;
            
        } catch (Exception e) {
            logger.error("Unexpected error generating magiclink", e);
            return MagiclinkResponse.internalError(e.getMessage());
        }
    }
    
    /**
     * Authenticate user using magiclink token
     * @param token JWT token from magiclink
     * @return AuthenticationResult with user and redirect information
     */
    public AuthenticationResult authenticateWithMagiclink(String token) {
        try {
            // Validate token
            TokenService.TokenValidationResult validation = tokenService.validateMagiclinkToken(token);
            
            if (!validation.isValid()) {
                logger.logTokenValidation(null, null, false, validation.getError());
                return AuthenticationResult.failure(validation.getError());
            }
            
            // Log successful authentication
            logger.logAuthenticationFlow(validation.getUserId(), validation.getTokenId(), 
                validation.getRedirectUrl(), true);
            
            return AuthenticationResult.success(validation.getUser(), validation.getRedirectUrl());
            
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    /**
     * Get status of a magiclink token
     * @param token JWT token
     * @return Token status information
     */
    public TokenStatus getTokenStatus(String token) {
        try {
            TokenService.TokenValidationResult validation = tokenService.validateMagiclinkToken(token);
            
            if (validation.isValid()) {
                return TokenStatus.valid(validation.getTokenId(), validation.getEmail());
            } else {
                return TokenStatus.invalid(validation.getError());
            }
            
        } catch (Exception e) {
            logger.error("Error checking token status", e);
            return TokenStatus.invalid("Token status check failed");
        }
    }
    
    /**
     * Test external API connectivity
     * @return Test result
     */
    public ExternalApiService.ApiResponse testExternalApi() {
        if (externalApiService == null) {
            return ExternalApiService.ApiResponse.error("External API not configured", "NOT_CONFIGURED");
        }
        
        try {
            return externalApiService.testConnection();
        } catch (Exception e) {
            logger.error("External API test failed", e);
            return ExternalApiService.ApiResponse.error("API test failed: " + e.getMessage(), "TEST_ERROR");
        }
    }
    
    /**
     * Find user by email address
     * @param email User email
     * @return UserModel or null if not found
     */
    private UserModel findUserByEmail(String email) {
        return session.users().getUserByEmail(realm, email);
    }
    
    /**
     * Check if request is rate limited
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
        
        // Check if window has expired
        LocalDateTime windowStart = data.getWindowStart();
        LocalDateTime now = LocalDateTime.now();
        long windowSeconds = config.getRateLimitWindow();
        
        if (windowStart.plusSeconds(windowSeconds).isBefore(now)) {
            // Window expired, reset
            rateLimitData.remove(rateLimitKey);
            return false;
        }
        
        // Check if limit exceeded
        int currentCount = data.getCount().get();
        int limit = config.getRateLimitRequests();
        
        logger.logRateLimit(rateLimitKey, limit, (int) windowSeconds, currentCount, currentCount >= limit);
        
        return currentCount >= limit;
    }
    
    /**
     * Record rate limit usage
     * @param rateLimitKey Rate limit key
     */
    private void recordRateLimitUsage(String rateLimitKey) {
        if (!config.isRateLimitEnabled()) {
            return;
        }
        
        rateLimitData.compute(rateLimitKey, (key, data) -> {
            if (data == null) {
                return new RateLimitData(LocalDateTime.now(), new AtomicInteger(1));
            }
            
            // Check if window has expired
            LocalDateTime windowStart = data.getWindowStart();
            LocalDateTime now = LocalDateTime.now();
            long windowSeconds = config.getRateLimitWindow();
            
            if (windowStart.plusSeconds(windowSeconds).isBefore(now)) {
                // Window expired, reset
                return new RateLimitData(now, new AtomicInteger(1));
            } else {
                // Increment count
                data.getCount().incrementAndGet();
                return data;
            }
        });
    }
    
    /**
     * Generate rate limit key
     * @param email User email
     * @param userId User ID
     * @return Rate limit key
     */
    private String getRateLimitKey(String email, String userId) {
        return "magiclink:" + realm.getName() + ":" + email + ":" + userId;
    }
    
    /**
     * Create log context for request
     * @param request Magiclink request
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(MagiclinkRequest request) {
        java.util.Map<String, Object> context = new java.util.HashMap<>();
        context.put("email", request.getEmail());
        context.put("redirectUrl", request.getRedirectUrl());
        context.put("expirationMinutes", request.getExpirationMinutes());
        context.put("clientId", request.getClientId());
        return context;
    }
    
    /**
     * Create log context for request and response
     * @param request Magiclink request
     * @param response Response object
     * @return Log context map
     */
    private java.util.Map<String, Object> createLogContext(MagiclinkRequest request, Object response) {
        java.util.Map<String, Object> context = createLogContext(request);
        if (response instanceof MagiclinkResponse) {
            MagiclinkResponse magiclinkResponse = (MagiclinkResponse) response;
            context.put("success", magiclinkResponse.isSuccess());
            context.put("tokenId", magiclinkResponse.getTokenId());
        } else if (response instanceof ExternalApiService.ApiResponse) {
            ExternalApiService.ApiResponse apiResponse = (ExternalApiService.ApiResponse) response;
            context.put("apiSuccess", apiResponse.isSuccess());
            context.put("apiStatusCode", apiResponse.getStatusCode());
            context.put("apiError", apiResponse.getErrorMessage());
        }
        return context;
    }
    
    /**
     * Clean up expired rate limit data
     */
    public static void cleanupRateLimitData() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        rateLimitData.entrySet().removeIf(entry -> 
            entry.getValue().getWindowStart().isBefore(cutoff));
    }
    
    /**
     * Get current configuration
     * @return MagiclinkConfig instance
     */
    public MagiclinkConfig getConfig() {
        return config;
    }
    
    /**
     * Rate limit data container
     */
    private static class RateLimitData {
        private final LocalDateTime windowStart;
        private final AtomicInteger count;
        
        public RateLimitData(LocalDateTime windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
        
        public LocalDateTime getWindowStart() {
            return windowStart;
        }
        
        public AtomicInteger getCount() {
            return count;
        }
    }
    
    /**
     * Authentication result container
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final UserModel user;
        private final String redirectUrl;
        private final String error;
        
        private AuthenticationResult(boolean success, UserModel user, String redirectUrl, String error) {
            this.success = success;
            this.user = user;
            this.redirectUrl = redirectUrl;
            this.error = error;
        }
        
        public static AuthenticationResult success(UserModel user, String redirectUrl) {
            return new AuthenticationResult(true, user, redirectUrl, null);
        }
        
        public static AuthenticationResult failure(String error) {
            return new AuthenticationResult(false, null, null, error);
        }
        
        public boolean isSuccess() { return success; }
        public UserModel getUser() { return user; }
        public String getRedirectUrl() { return redirectUrl; }
        public String getError() { return error; }
    }
    
    /**
     * Token status container
     */
    public static class TokenStatus {
        private final boolean valid;
        private final String tokenId;
        private final String email;
        private final String error;
        
        private TokenStatus(boolean valid, String tokenId, String email, String error) {
            this.valid = valid;
            this.tokenId = tokenId;
            this.email = email;
            this.error = error;
        }
        
        public static TokenStatus valid(String tokenId, String email) {
            return new TokenStatus(true, tokenId, email, null);
        }
        
        public static TokenStatus invalid(String error) {
            return new TokenStatus(false, null, null, error);
        }
        
        public boolean isValid() { return valid; }
        public String getTokenId() { return tokenId; }
        public String getEmail() { return email; }
        public String getError() { return error; }
    }
} 