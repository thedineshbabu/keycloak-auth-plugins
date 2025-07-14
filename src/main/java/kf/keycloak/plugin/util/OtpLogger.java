package kf.keycloak.plugin.util;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Logger utility for OTP plugin
 * Provides structured logging for OTP-related events
 */
public class OtpLogger {
    
    private static final Logger LOGGER = Logger.getLogger(OtpLogger.class);
    private static final String LOG_PREFIX = "[OTP]";
    
    private final String component;
    private final KeycloakSession session;
    private final RealmModel realm;
    
    /**
     * Private constructor
     * @param component Component name
     * @param session Keycloak session (optional)
     * @param realm Realm model (optional)
     */
    private OtpLogger(String component, KeycloakSession session, RealmModel realm) {
        this.component = component;
        this.session = session;
        this.realm = realm;
    }
    
    /**
     * Create logger for a specific component
     * @param component Component name
     * @return OtpLogger instance
     */
    public static OtpLogger forComponent(String component) {
        return new OtpLogger(component, null, null);
    }
    
    /**
     * Create logger for a session and realm
     * @param session Keycloak session
     * @param realm Realm model
     * @param component Component name
     * @return OtpLogger instance
     */
    public static OtpLogger forSession(KeycloakSession session, RealmModel realm, String component) {
        return new OtpLogger(component, session, realm);
    }
    
    /**
     * Log OTP generation event
     * @param userId User ID
     * @param email User's email
     * @param otpId Generated OTP ID
     * @param success Whether generation was successful
     */
    public void logOtpGeneration(String userId, String email, String otpId, boolean success) {
        Map<String, Object> context = createLogContext();
        context.put("userId", userId);
        context.put("email", email);
        context.put("otpId", otpId);
        context.put("success", success);
        context.put("event", "otp_generation");
        
        if (success) {
            info("OTP generated successfully", context);
        } else {
            warn("OTP generation failed", context);
        }
    }
    
    /**
     * Log OTP validation event
     * @param userId User ID
     * @param otpId OTP ID
     * @param success Whether validation was successful
     * @param reason Reason for success/failure
     */
    public void logOtpValidation(String userId, String otpId, boolean success, String reason) {
        Map<String, Object> context = createLogContext();
        context.put("userId", userId);
        context.put("otpId", otpId);
        context.put("success", success);
        context.put("reason", reason);
        context.put("event", "otp_validation");
        
        if (success) {
            info("OTP validated successfully", context);
        } else {
            warn("OTP validation failed: " + reason, context);
        }
    }
    
    /**
     * Log eligibility check event
     * @param email User's email
     * @param eligible Whether user is eligible
     * @param apiSuccess Whether API call succeeded
     * @param reason Reason for eligibility decision
     */
    public void logEligibilityCheck(String email, boolean eligible, boolean apiSuccess, String reason) {
        Map<String, Object> context = createLogContext();
        context.put("email", email);
        context.put("eligible", eligible);
        context.put("apiSuccess", apiSuccess);
        context.put("reason", reason);
        context.put("event", "eligibility_check");
        
        if (apiSuccess) {
            info("Eligibility check completed: " + (eligible ? "enabled" : "disabled"), context);
        } else {
            warn("Eligibility API call failed, using fallback", context);
        }
    }
    
    /**
     * Log external API call event
     * @param apiUrl API URL
     * @param success Whether API call succeeded
     * @param statusCode HTTP status code
     * @param responseTime Response time in milliseconds
     */
    public void logExternalApiCall(String apiUrl, boolean success, Integer statusCode, long responseTime) {
        Map<String, Object> context = createLogContext();
        context.put("apiUrl", apiUrl);
        context.put("success", success);
        context.put("statusCode", statusCode);
        context.put("responseTime", responseTime);
        context.put("event", "external_api_call");
        
        if (success) {
            info("External API call successful", context);
        } else {
            warn("External API call failed", context);
        }
    }
    
    /**
     * Log authentication flow event
     * @param userId User ID
     * @param otpId OTP ID
     * @param success Whether authentication succeeded
     * @param flowType Type of authentication flow
     */
    public void logAuthenticationFlow(String userId, String otpId, boolean success, String flowType) {
        Map<String, Object> context = createLogContext();
        context.put("userId", userId);
        context.put("otpId", otpId);
        context.put("success", success);
        context.put("flowType", flowType);
        context.put("event", "authentication_flow");
        
        if (success) {
            info("OTP authentication successful", context);
        } else {
            warn("OTP authentication failed", context);
        }
    }
    
    /**
     * Log rate limit event
     * @param email User's email
     * @param userId User ID
     * @param exceeded Whether rate limit was exceeded
     */
    public void logRateLimit(String email, String userId, boolean exceeded) {
        Map<String, Object> context = createLogContext();
        context.put("email", email);
        context.put("userId", userId);
        context.put("exceeded", exceeded);
        context.put("event", "rate_limit");
        
        if (exceeded) {
            warn("Rate limit exceeded for user", context);
        } else {
            debug("Rate limit check passed", context);
        }
    }
    
    /**
     * Log configuration event
     * @param configKey Configuration key
     * @param configValue Configuration value
     * @param valid Whether configuration is valid
     */
    public void logConfiguration(String configKey, String configValue, boolean valid) {
        Map<String, Object> context = createLogContext();
        context.put("configKey", configKey);
        context.put("configValue", configValue != null ? "***" : null);
        context.put("valid", valid);
        context.put("event", "configuration");
        
        if (valid) {
            debug("Configuration validated", context);
        } else {
            warn("Configuration validation failed", context);
        }
    }
    
    /**
     * Log security event
     * @param eventType Type of security event
     * @param userId User ID (optional)
     * @param details Event details
     */
    public void logSecurityEvent(String eventType, String userId, String details) {
        Map<String, Object> context = createLogContext();
        context.put("eventType", eventType);
        context.put("userId", userId);
        context.put("details", details);
        context.put("event", "security");
        
        info("Security event: " + eventType, context);
    }
    
    /**
     * Log debug message
     * @param message Message to log
     */
    public void debug(String message) {
        LOGGER.debug(createLogMessage(message));
    }
    
    /**
     * Log debug message with context
     * @param message Message to log
     * @param context Log context
     */
    public void debug(String message, Map<String, Object> context) {
        LOGGER.debug(createLogMessage(message, context));
    }
    
    /**
     * Log info message
     * @param message Message to log
     */
    public void info(String message) {
        LOGGER.info(createLogMessage(message));
    }
    
    /**
     * Log info message with context
     * @param message Message to log
     * @param context Log context
     */
    public void info(String message, Map<String, Object> context) {
        LOGGER.info(createLogMessage(message, context));
    }
    
    /**
     * Log warning message
     * @param message Message to log
     */
    public void warn(String message) {
        LOGGER.warn(createLogMessage(message));
    }
    
    /**
     * Log warning message with context
     * @param message Message to log
     * @param context Log context
     */
    public void warn(String message, Map<String, Object> context) {
        LOGGER.warn(createLogMessage(message, context));
    }
    
    /**
     * Log error message
     * @param message Message to log
     */
    public void error(String message) {
        LOGGER.error(createLogMessage(message));
    }
    
    /**
     * Log error message with context
     * @param message Message to log
     * @param context Log context
     */
    public void error(String message, Map<String, Object> context) {
        LOGGER.error(createLogMessage(message, context));
    }
    
    /**
     * Log error message with exception
     * @param message Message to log
     * @param throwable Exception
     */
    public void error(String message, Throwable throwable) {
        LOGGER.error(createLogMessage(message), throwable);
    }
    
    /**
     * Log error message with context and exception
     * @param message Message to log
     * @param context Log context
     * @param throwable Exception
     */
    public void error(String message, Map<String, Object> context, Throwable throwable) {
        LOGGER.error(createLogMessage(message, context), throwable);
    }
    
    /**
     * Create base log context
     * @return Log context map
     */
    private Map<String, Object> createLogContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("component", component);
        
        if (realm != null) {
            context.put("realm", realm.getName());
        }
        
        return context;
    }
    
    /**
     * Create log message
     * @param message Base message
     * @return Formatted log message
     */
    private String createLogMessage(String message) {
        return String.format("%s [%s] %s", LOG_PREFIX, component, message);
    }
    
    /**
     * Create log message with context
     * @param message Base message
     * @param context Log context
     * @return Formatted log message
     */
    private String createLogMessage(String message, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append(createLogMessage(message));
        
        if (context != null && !context.isEmpty()) {
            sb.append(" | Context: {");
            boolean first = true;
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        
        return sb.toString();
    }
} 