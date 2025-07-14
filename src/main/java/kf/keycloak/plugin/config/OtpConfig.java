package kf.keycloak.plugin.config;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration service for OTP plugin settings
 * Manages realm-specific configuration and default values for OTP functionality
 */
public class OtpConfig {
    
    // Configuration attribute keys
    private static final String CONFIG_PREFIX = "otp.";
    private static final String ATTR_ENABLED = CONFIG_PREFIX + "enabled";
    private static final String ATTR_EXTERNAL_OTP_API_URL = CONFIG_PREFIX + "external.otp.api.url";
    private static final String ATTR_EXTERNAL_ELIGIBILITY_API_URL = CONFIG_PREFIX + "external.eligibility.api.url";
    private static final String ATTR_EXTERNAL_API_TOKEN = CONFIG_PREFIX + "external.api.token";
    private static final String ATTR_EXTERNAL_API_TYPE = CONFIG_PREFIX + "external.api.type";
    private static final String ATTR_OTP_LENGTH = CONFIG_PREFIX + "length";
    private static final String ATTR_OTP_TTL = CONFIG_PREFIX + "ttl";
    private static final String ATTR_FAIL_IF_ELIGIBILITY_FAILS = CONFIG_PREFIX + "fail.if.eligibility.fails";
    private static final String ATTR_ENABLED_REALMS = CONFIG_PREFIX + "enabled.realms";
    private static final String ATTR_MAX_RETRY_ATTEMPTS = CONFIG_PREFIX + "max.retry.attempts";
    private static final String ATTR_RATE_LIMIT_ENABLED = CONFIG_PREFIX + "rate.limit.enabled";
    private static final String ATTR_RATE_LIMIT_REQUESTS = CONFIG_PREFIX + "rate.limit.requests";
    private static final String ATTR_RATE_LIMIT_WINDOW = CONFIG_PREFIX + "rate.limit.window";
    
    // Default values
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_OTP_LENGTH = 6;
    private static final int DEFAULT_OTP_TTL = 300; // 5 minutes
    private static final boolean DEFAULT_FAIL_IF_ELIGIBILITY_FAILS = false;
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final boolean DEFAULT_RATE_LIMIT_ENABLED = true;
    private static final int DEFAULT_RATE_LIMIT_REQUESTS = 5;
    private static final int DEFAULT_RATE_LIMIT_WINDOW = 60; // seconds
    private static final String DEFAULT_AUTH_TYPE = "bearer";
    
    private final RealmModel realm;
    private final KeycloakSession session;
    
    /**
     * Constructor with realm and session
     * @param session Keycloak session
     * @param realm Realm model
     */
    public OtpConfig(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }
    
    /**
     * Check if OTP is enabled for this realm
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        String value = realm.getAttribute(ATTR_ENABLED);
        return value != null ? Boolean.parseBoolean(value) : DEFAULT_ENABLED;
    }
    
    /**
     * Set OTP enabled status
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        realm.setAttribute(ATTR_ENABLED, String.valueOf(enabled));
    }
    
    /**
     * Get external OTP API endpoint URL
     * @return API endpoint URL or null if not configured
     */
    public String getExternalOtpApiUrl() {
        return realm.getAttribute(ATTR_EXTERNAL_OTP_API_URL);
    }
    
    /**
     * Set external OTP API endpoint URL
     * @param url API endpoint URL
     */
    public void setExternalOtpApiUrl(String url) {
        realm.setAttribute(ATTR_EXTERNAL_OTP_API_URL, url);
    }
    
    /**
     * Get external eligibility API endpoint URL
     * @return API endpoint URL or null if not configured
     */
    public String getExternalEligibilityApiUrl() {
        return realm.getAttribute(ATTR_EXTERNAL_ELIGIBILITY_API_URL);
    }
    
    /**
     * Set external eligibility API endpoint URL
     * @param url API endpoint URL
     */
    public void setExternalEligibilityApiUrl(String url) {
        realm.setAttribute(ATTR_EXTERNAL_ELIGIBILITY_API_URL, url);
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
     * Get OTP length
     * @return OTP length in digits
     */
    public int getOtpLength() {
        String value = realm.getAttribute(ATTR_OTP_LENGTH);
        return value != null ? Integer.parseInt(value) : DEFAULT_OTP_LENGTH;
    }
    
    /**
     * Set OTP length
     * @param length OTP length in digits
     */
    public void setOtpLength(int length) {
        realm.setAttribute(ATTR_OTP_LENGTH, String.valueOf(length));
    }
    
    /**
     * Get OTP TTL in seconds
     * @return OTP TTL in seconds
     */
    public int getOtpTtl() {
        String value = realm.getAttribute(ATTR_OTP_TTL);
        return value != null ? Integer.parseInt(value) : DEFAULT_OTP_TTL;
    }
    
    /**
     * Set OTP TTL in seconds
     * @param ttl OTP TTL in seconds
     */
    public void setOtpTtl(int ttl) {
        realm.setAttribute(ATTR_OTP_TTL, String.valueOf(ttl));
    }
    
    /**
     * Check if authentication should fail when eligibility check fails
     * @return true if should fail, false for fail-open behavior
     */
    public boolean shouldFailIfEligibilityFails() {
        String value = realm.getAttribute(ATTR_FAIL_IF_ELIGIBILITY_FAILS);
        return value != null ? Boolean.parseBoolean(value) : DEFAULT_FAIL_IF_ELIGIBILITY_FAILS;
    }
    
    /**
     * Set fail-if-eligibility-fails behavior
     * @param fail true to fail authentication on eligibility API failure, false for fail-open
     */
    public void setFailIfEligibilityFails(boolean fail) {
        realm.setAttribute(ATTR_FAIL_IF_ELIGIBILITY_FAILS, String.valueOf(fail));
    }
    
    /**
     * Get enabled realms (comma-separated list)
     * @return Enabled realms or null if all realms are enabled
     */
    public String getEnabledRealms() {
        return realm.getAttribute(ATTR_ENABLED_REALMS);
    }
    
    /**
     * Set enabled realms
     * @param realms Comma-separated list of enabled realm names
     */
    public void setEnabledRealms(String realms) {
        realm.setAttribute(ATTR_ENABLED_REALMS, realms);
    }
    
    /**
     * Check if current realm is enabled for OTP
     * @return true if realm is enabled
     */
    public boolean isRealmEnabled() {
        String enabledRealms = getEnabledRealms();
        if (enabledRealms == null || enabledRealms.trim().isEmpty()) {
            return true; // All realms enabled if not specified
        }
        
        String[] realms = enabledRealms.split(",");
        for (String realmName : realms) {
            if (realmName.trim().equals(realm.getName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get maximum retry attempts for OTP validation
     * @return Maximum retry attempts
     */
    public int getMaxRetryAttempts() {
        String value = realm.getAttribute(ATTR_MAX_RETRY_ATTEMPTS);
        return value != null ? Integer.parseInt(value) : DEFAULT_MAX_RETRY_ATTEMPTS;
    }
    
    /**
     * Set maximum retry attempts for OTP validation
     * @param attempts Maximum retry attempts
     */
    public void setMaxRetryAttempts(int attempts) {
        realm.setAttribute(ATTR_MAX_RETRY_ATTEMPTS, String.valueOf(attempts));
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
     * Check if external APIs are configured
     * @return true if both OTP and eligibility APIs are configured
     */
    public boolean isExternalApiConfigured() {
        return getExternalOtpApiUrl() != null && !getExternalOtpApiUrl().trim().isEmpty() &&
               getExternalEligibilityApiUrl() != null && !getExternalEligibilityApiUrl().trim().isEmpty();
    }
    
    /**
     * Get all configuration as a map
     * @return Configuration map
     */
    public Map<String, String> getConfigurationMap() {
        Map<String, String> config = new HashMap<>();
        config.put("enabled", String.valueOf(isEnabled()));
        config.put("external.otp.api.url", getExternalOtpApiUrl());
        config.put("external.eligibility.api.url", getExternalEligibilityApiUrl());
        config.put("external.api.token", getExternalApiToken());
        config.put("external.api.type", getExternalApiType());
        config.put("otp.length", String.valueOf(getOtpLength()));
        config.put("otp.ttl", String.valueOf(getOtpTtl()));
        config.put("fail.if.eligibility.fails", String.valueOf(shouldFailIfEligibilityFails()));
        config.put("enabled.realms", getEnabledRealms());
        config.put("max.retry.attempts", String.valueOf(getMaxRetryAttempts()));
        config.put("rate.limit.enabled", String.valueOf(isRateLimitEnabled()));
        config.put("rate.limit.requests", String.valueOf(getRateLimitRequests()));
        config.put("rate.limit.window", String.valueOf(getRateLimitWindow()));
        return config;
    }
    
    /**
     * Update configuration from map
     * @param config Configuration map
     */
    public void updateFromMap(Map<String, String> config) {
        if (config.containsKey("enabled")) {
            setEnabled(Boolean.parseBoolean(config.get("enabled")));
        }
        if (config.containsKey("external.otp.api.url")) {
            setExternalOtpApiUrl(config.get("external.otp.api.url"));
        }
        if (config.containsKey("external.eligibility.api.url")) {
            setExternalEligibilityApiUrl(config.get("external.eligibility.api.url"));
        }
        if (config.containsKey("external.api.token")) {
            setExternalApiToken(config.get("external.api.token"));
        }
        if (config.containsKey("external.api.type")) {
            setExternalApiType(config.get("external.api.type"));
        }
        if (config.containsKey("otp.length")) {
            setOtpLength(Integer.parseInt(config.get("otp.length")));
        }
        if (config.containsKey("otp.ttl")) {
            setOtpTtl(Integer.parseInt(config.get("otp.ttl")));
        }
        if (config.containsKey("fail.if.eligibility.fails")) {
            setFailIfEligibilityFails(Boolean.parseBoolean(config.get("fail.if.eligibility.fails")));
        }
        if (config.containsKey("enabled.realms")) {
            setEnabledRealms(config.get("enabled.realms"));
        }
        if (config.containsKey("max.retry.attempts")) {
            setMaxRetryAttempts(Integer.parseInt(config.get("max.retry.attempts")));
        }
        if (config.containsKey("rate.limit.enabled")) {
            setRateLimitEnabled(Boolean.parseBoolean(config.get("rate.limit.enabled")));
        }
        if (config.containsKey("rate.limit.requests")) {
            setRateLimitRequests(Integer.parseInt(config.get("rate.limit.requests")));
        }
        if (config.containsKey("rate.limit.window")) {
            setRateLimitWindow(Integer.parseInt(config.get("rate.limit.window")));
        }
    }
    
    /**
     * Validate configuration
     * @return Validation result
     */
    public ValidationResult validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        // Check if OTP is enabled
        if (!isEnabled()) {
            return new ValidationResult(true, errors); // Valid if disabled
        }
        
        // Check if realm is enabled
        if (!isRealmEnabled()) {
            return new ValidationResult(true, errors); // Valid if realm not enabled
        }
        
        // Check external API configuration
        if (!isExternalApiConfigured()) {
            errors.add("External OTP API URL and Eligibility API URL must be configured");
        }
        
        // Validate OTP length
        if (getOtpLength() < 4 || getOtpLength() > 10) {
            errors.add("OTP length must be between 4 and 10 digits");
        }
        
        // Validate OTP TTL
        if (getOtpTtl() < 60 || getOtpTtl() > 3600) {
            errors.add("OTP TTL must be between 60 and 3600 seconds");
        }
        
        // Validate retry attempts
        if (getMaxRetryAttempts() < 1 || getMaxRetryAttempts() > 10) {
            errors.add("Max retry attempts must be between 1 and 10");
        }
        
        // Validate rate limit settings
        if (isRateLimitEnabled()) {
            if (getRateLimitRequests() < 1 || getRateLimitRequests() > 100) {
                errors.add("Rate limit requests must be between 1 and 100");
            }
            if (getRateLimitWindow() < 10 || getRateLimitWindow() > 3600) {
                errors.add("Rate limit window must be between 10 and 3600 seconds");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
} 