package kf.keycloak.plugin.config;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Configuration service for magiclink plugin settings
 * Manages realm-specific configuration and default values
 */
public class MagiclinkConfig {
    
    // Configuration attribute keys
    private static final String CONFIG_PREFIX = "magiclink.";
    private static final String ATTR_ENABLED = CONFIG_PREFIX + "enabled";
    private static final String ATTR_EXTERNAL_API_ENDPOINT = CONFIG_PREFIX + "external.api.endpoint";
    private static final String ATTR_EXTERNAL_API_TOKEN = CONFIG_PREFIX + "external.api.token";
    private static final String ATTR_EXTERNAL_API_TYPE = CONFIG_PREFIX + "external.api.type";
    private static final String ATTR_BASE_URL = CONFIG_PREFIX + "base.url";
    private static final String ATTR_TOKEN_EXPIRY_MINUTES = CONFIG_PREFIX + "token.expiry.minutes";
    private static final String ATTR_RATE_LIMIT_ENABLED = CONFIG_PREFIX + "rate.limit.enabled";
    private static final String ATTR_RATE_LIMIT_REQUESTS = CONFIG_PREFIX + "rate.limit.requests";
    private static final String ATTR_RATE_LIMIT_WINDOW = CONFIG_PREFIX + "rate.limit.window";
    private static final String ATTR_ALLOWED_REDIRECT_URLS = CONFIG_PREFIX + "allowed.redirect.urls";
    
    // Default values
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_TOKEN_EXPIRY_MINUTES = 15;
    private static final boolean DEFAULT_RATE_LIMIT_ENABLED = true;
    private static final int DEFAULT_RATE_LIMIT_REQUESTS = 10;
    private static final int DEFAULT_RATE_LIMIT_WINDOW = 60; // seconds
    private static final String DEFAULT_AUTH_TYPE = "bearer";
    
    private final RealmModel realm;
    private final KeycloakSession session;
    
    /**
     * Constructor with realm and session
     * @param session Keycloak session
     * @param realm Realm model
     */
    public MagiclinkConfig(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }
    
    /**
     * Check if magiclink is enabled for this realm
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        String value = realm.getAttribute(ATTR_ENABLED);
        return value != null ? Boolean.parseBoolean(value) : DEFAULT_ENABLED;
    }
    
    /**
     * Set magiclink enabled status
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        realm.setAttribute(ATTR_ENABLED, String.valueOf(enabled));
    }
    
    /**
     * Get external API endpoint URL
     * @return API endpoint URL or null if not configured
     */
    public String getExternalApiEndpoint() {
        return realm.getAttribute(ATTR_EXTERNAL_API_ENDPOINT);
    }
    
    /**
     * Set external API endpoint URL
     * @param endpoint API endpoint URL
     */
    public void setExternalApiEndpoint(String endpoint) {
        realm.setAttribute(ATTR_EXTERNAL_API_ENDPOINT, endpoint);
    }
    
    /**
     * Get external API authentication token
     * @return API authentication token or null if not configured
     */
    public String getExternalApiToken() {
        return realm.getAttribute(ATTR_EXTERNAL_API_TOKEN);
    }
    
    /**
     * Set external API authentication token
     * @param token API authentication token
     */
    public void setExternalApiToken(String token) {
        realm.setAttribute(ATTR_EXTERNAL_API_TOKEN, token);
    }
    
    /**
     * Get external API authentication type
     * @return API authentication type (bearer, basic, apikey)
     */
    public String getExternalApiType() {
        String value = realm.getAttribute(ATTR_EXTERNAL_API_TYPE);
        return value != null ? value : DEFAULT_AUTH_TYPE;
    }
    
    /**
     * Set external API authentication type
     * @param type API authentication type
     */
    public void setExternalApiType(String type) {
        realm.setAttribute(ATTR_EXTERNAL_API_TYPE, type);
    }
    
    /**
     * Get base URL for magiclink generation
     * @return Base URL or auto-detected from current request
     */
    public String getBaseUrl() {
        String value = realm.getAttribute(ATTR_BASE_URL);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        // Auto-detect base URL from Keycloak context
        return session.getContext().getUri().getBaseUri().toString();
    }
    
    /**
     * Set base URL for magiclink generation
     * @param baseUrl Base URL
     */
    public void setBaseUrl(String baseUrl) {
        realm.setAttribute(ATTR_BASE_URL, baseUrl);
    }
    
    /**
     * Get token expiry time in minutes
     * @return Token expiry in minutes
     */
    public int getTokenExpiryMinutes() {
        String value = realm.getAttribute(ATTR_TOKEN_EXPIRY_MINUTES);
        return value != null ? Integer.parseInt(value) : DEFAULT_TOKEN_EXPIRY_MINUTES;
    }
    
    /**
     * Set token expiry time in minutes
     * @param minutes Token expiry in minutes
     */
    public void setTokenExpiryMinutes(int minutes) {
        realm.setAttribute(ATTR_TOKEN_EXPIRY_MINUTES, String.valueOf(minutes));
    }
    
    /**
     * Check if rate limiting is enabled
     * @return true if rate limiting is enabled
     */
    public boolean isRateLimitEnabled() {
        String value = realm.getAttribute(ATTR_RATE_LIMIT_ENABLED);
        return value != null ? Boolean.parseBoolean(value) : DEFAULT_RATE_LIMIT_ENABLED;
    }
    
    /**
     * Set rate limiting enabled status
     * @param enabled true to enable rate limiting
     */
    public void setRateLimitEnabled(boolean enabled) {
        realm.setAttribute(ATTR_RATE_LIMIT_ENABLED, String.valueOf(enabled));
    }
    
    /**
     * Get rate limit requests per window
     * @return Number of requests allowed per window
     */
    public int getRateLimitRequests() {
        String value = realm.getAttribute(ATTR_RATE_LIMIT_REQUESTS);
        return value != null ? Integer.parseInt(value) : DEFAULT_RATE_LIMIT_REQUESTS;
    }
    
    /**
     * Set rate limit requests per window
     * @param requests Number of requests allowed per window
     */
    public void setRateLimitRequests(int requests) {
        realm.setAttribute(ATTR_RATE_LIMIT_REQUESTS, String.valueOf(requests));
    }
    
    /**
     * Get rate limit window in seconds
     * @return Rate limit window in seconds
     */
    public int getRateLimitWindow() {
        String value = realm.getAttribute(ATTR_RATE_LIMIT_WINDOW);
        return value != null ? Integer.parseInt(value) : DEFAULT_RATE_LIMIT_WINDOW;
    }
    
    /**
     * Set rate limit window in seconds
     * @param seconds Rate limit window in seconds
     */
    public void setRateLimitWindow(int seconds) {
        realm.setAttribute(ATTR_RATE_LIMIT_WINDOW, String.valueOf(seconds));
    }
    
    /**
     * Get allowed redirect URLs (comma-separated)
     * @return Allowed redirect URLs or null if not configured
     */
    public String getAllowedRedirectUrls() {
        return realm.getAttribute(ATTR_ALLOWED_REDIRECT_URLS);
    }
    
    /**
     * Set allowed redirect URLs (comma-separated)
     * @param urls Allowed redirect URLs
     */
    public void setAllowedRedirectUrls(String urls) {
        realm.setAttribute(ATTR_ALLOWED_REDIRECT_URLS, urls);
    }
    
    /**
     * Check if a redirect URL is allowed
     * @param redirectUrl URL to check
     * @return true if allowed, false otherwise
     */
    public boolean isRedirectUrlAllowed(String redirectUrl) {
        String allowedUrls = getAllowedRedirectUrls();
        if (allowedUrls == null || allowedUrls.trim().isEmpty()) {
            // If no restrictions configured, allow HTTPS URLs and HTTP for localhost/development
            if (redirectUrl != null) {
                if (redirectUrl.startsWith("https://")) {
                    return true;
                }
                // Allow HTTP for localhost, 127.0.0.1, and common development ports
                if (redirectUrl.startsWith("http://localhost") || 
                    redirectUrl.startsWith("http://127.0.0.1") ||
                    redirectUrl.startsWith("http://0.0.0.0")) {
                    return true;
                }
                // Allow HTTP for development environments (ports 3000-9999)
                if (redirectUrl.matches("^http://[^:]+:[3-9]\\d{3,4}.*")) {
                    return true;
                }
            }
            return false;
        }
        
        // Check against configured allowed URLs
        String[] urls = allowedUrls.split(",");
        for (String url : urls) {
            String trimmedUrl = url.trim();
            if (redirectUrl != null && redirectUrl.startsWith(trimmedUrl)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if external API is configured
     * @return true if external API is configured
     */
    public boolean isExternalApiConfigured() {
        String endpoint = getExternalApiEndpoint();
        return endpoint != null && !endpoint.trim().isEmpty();
    }
    
    /**
     * Get all configuration as a map for admin UI
     * @return Configuration map
     */
    public java.util.Map<String, String> getConfigurationMap() {
        java.util.Map<String, String> config = new java.util.HashMap<>();
        
        config.put("enabled", String.valueOf(isEnabled()));
        config.put("externalApiEndpoint", getExternalApiEndpoint());
        config.put("externalApiToken", getExternalApiToken() != null ? "***" : null);
        config.put("externalApiType", getExternalApiType());
        config.put("baseUrl", getBaseUrl());
        config.put("tokenExpiryMinutes", String.valueOf(getTokenExpiryMinutes()));
        config.put("rateLimitEnabled", String.valueOf(isRateLimitEnabled()));
        config.put("rateLimitRequests", String.valueOf(getRateLimitRequests()));
        config.put("rateLimitWindow", String.valueOf(getRateLimitWindow()));
        config.put("allowedRedirectUrls", getAllowedRedirectUrls());
        
        return config;
    }
    
    /**
     * Update configuration from map
     * @param config Configuration map
     */
    public void updateFromMap(java.util.Map<String, String> config) {
        if (config.containsKey("enabled")) {
            setEnabled(Boolean.parseBoolean(config.get("enabled")));
        }
        
        if (config.containsKey("externalApiEndpoint")) {
            setExternalApiEndpoint(config.get("externalApiEndpoint"));
        }
        
        if (config.containsKey("externalApiToken")) {
            String token = config.get("externalApiToken");
            if (token != null && !token.equals("***")) {
                setExternalApiToken(token);
            }
        }
        
        if (config.containsKey("externalApiType")) {
            setExternalApiType(config.get("externalApiType"));
        }
        
        if (config.containsKey("baseUrl")) {
            setBaseUrl(config.get("baseUrl"));
        }
        
        if (config.containsKey("tokenExpiryMinutes")) {
            setTokenExpiryMinutes(Integer.parseInt(config.get("tokenExpiryMinutes")));
        }
        
        if (config.containsKey("rateLimitEnabled")) {
            setRateLimitEnabled(Boolean.parseBoolean(config.get("rateLimitEnabled")));
        }
        
        if (config.containsKey("rateLimitRequests")) {
            setRateLimitRequests(Integer.parseInt(config.get("rateLimitRequests")));
        }
        
        if (config.containsKey("rateLimitWindow")) {
            setRateLimitWindow(Integer.parseInt(config.get("rateLimitWindow")));
        }
        
        if (config.containsKey("allowedRedirectUrls")) {
            setAllowedRedirectUrls(config.get("allowedRedirectUrls"));
        }
    }
    
    /**
     * Validate configuration
     * @return Validation result with any errors
     */
    public ValidationResult validateConfiguration() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        // Validate external API endpoint if configured
        String endpoint = getExternalApiEndpoint();
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                errors.add("External API endpoint must be a valid HTTP/HTTPS URL");
            }
        }
        
        // Validate token expiry
        int expiry = getTokenExpiryMinutes();
        if (expiry < 1 || expiry > 1440) { // 1 minute to 24 hours
            errors.add("Token expiry must be between 1 and 1440 minutes");
        }
        
        // Validate rate limit settings
        if (isRateLimitEnabled()) {
            int requests = getRateLimitRequests();
            int window = getRateLimitWindow();
            
            if (requests < 1 || requests > 1000) {
                errors.add("Rate limit requests must be between 1 and 1000");
            }
            
            if (window < 1 || window > 3600) {
                errors.add("Rate limit window must be between 1 and 3600 seconds");
            }
        }
        
        // Validate allowed redirect URLs
        String allowedUrls = getAllowedRedirectUrls();
        if (allowedUrls != null && !allowedUrls.trim().isEmpty()) {
            String[] urls = allowedUrls.split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.startsWith("https://") && 
                    !trimmedUrl.startsWith("http://localhost") && 
                    !trimmedUrl.startsWith("http://127.0.0.1")) {
                    errors.add("Allowed redirect URLs must use HTTPS (or HTTP for localhost): " + trimmedUrl);
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final java.util.List<String> errors;
        
        public ValidationResult(boolean valid, java.util.List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public java.util.List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return errors.isEmpty() ? null : String.join(", ", errors);
        }
    }
} 