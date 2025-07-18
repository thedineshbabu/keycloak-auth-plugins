package kf.keycloak.plugin.util;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Winston-style logging utility for autologin plugin
 * Provides structured logging with different levels and context information
 */
public class MagiclinkLogger {
    
    // Log levels
    public enum LogLevel {
        ERROR("ERROR", 0),
        WARN("WARN", 1),
        INFO("INFO", 2),
        DEBUG("DEBUG", 3);
        
        private final String name;
        private final int level;
        
        LogLevel(String name, int level) {
            this.name = name;
            this.level = level;
        }
        
        public String getName() {
            return name;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOGGER_NAME = "AutologinPlugin";
    private static LogLevel currentLogLevel = LogLevel.INFO;
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final String component;
    
    /**
     * Constructor with session and realm context
     * @param session Keycloak session
     * @param realm Realm model
     * @param component Component name (service, provider, etc.)
     */
    public MagiclinkLogger(KeycloakSession session, RealmModel realm, String component) {
        this.session = session;
        this.realm = realm;
        this.component = component;
    }
    
    /**
     * Constructor with component only
     * @param component Component name
     */
    public MagiclinkLogger(String component) {
        this.session = null;
        this.realm = null;
        this.component = component;
    }
    
    /**
     * Set global log level
     * @param level Log level
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
    }
    
    /**
     * Log error message with exception
     * @param message Error message
     * @param exception Exception details
     */
    public void error(String message, Exception exception) {
        if (shouldLog(LogLevel.ERROR)) {
            Map<String, Object> context = new HashMap<>();
            context.put("exception", getExceptionDetails(exception));
            log(LogLevel.ERROR, message, context);
        }
    }
    
    /**
     * Log error message with context
     * @param message Error message
     * @param context Additional context
     */
    public void error(String message, Map<String, Object> context) {
        if (shouldLog(LogLevel.ERROR)) {
            log(LogLevel.ERROR, message, context);
        }
    }
    
    /**
     * Log error message
     * @param message Error message
     */
    public void error(String message) {
        if (shouldLog(LogLevel.ERROR)) {
            log(LogLevel.ERROR, message, null);
        }
    }
    
    /**
     * Log warning message
     * @param message Warning message
     */
    public void warn(String message) {
        if (shouldLog(LogLevel.WARN)) {
            log(LogLevel.WARN, message, null);
        }
    }
    
    /**
     * Log warning message with context
     * @param message Warning message
     * @param context Additional context
     */
    public void warn(String message, Map<String, Object> context) {
        if (shouldLog(LogLevel.WARN)) {
            log(LogLevel.WARN, message, context);
        }
    }
    
    /**
     * Log info message
     * @param message Info message
     */
    public void info(String message) {
        if (shouldLog(LogLevel.INFO)) {
            log(LogLevel.INFO, message, null);
        }
    }
    
    /**
     * Log info message with context
     * @param message Info message
     * @param context Additional context
     */
    public void info(String message, Map<String, Object> context) {
        if (shouldLog(LogLevel.INFO)) {
            log(LogLevel.INFO, message, context);
        }
    }
    
    /**
     * Log debug message
     * @param message Debug message
     */
    public void debug(String message) {
        if (shouldLog(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, message, null);
        }
    }
    
    /**
     * Log debug message with context
     * @param message Debug message
     * @param context Additional context
     */
    public void debug(String message, Map<String, Object> context) {
        if (shouldLog(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, message, context);
        }
    }
    
    /**
     * Log token generation activity
     * @param userId User ID
     * @param email User email
     * @param tokenId Token ID
     * @param success Whether generation was successful
     */
    public void logTokenGeneration(String userId, String email, String tokenId, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", email);
        context.put("tokenId", tokenId);
        context.put("success", success);
        context.put("operation", "token_generation");
        
        if (success) {
            info("Autologin token generated successfully", context);
        } else {
            error("Failed to generate autologin token", context);
        }
    }
    
    /**
     * Log token validation activity
     * @param tokenId Token ID
     * @param userId User ID
     * @param success Whether validation was successful
     * @param reason Validation failure reason
     */
    public void logTokenValidation(String tokenId, String userId, boolean success, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("tokenId", tokenId);
        context.put("userId", userId);
        context.put("success", success);
        context.put("operation", "token_validation");
        
        if (!success) {
            context.put("reason", reason);
        }
        
        if (success) {
            info("Autologin token validated successfully", context);
        } else {
            warn("Autologin token validation failed", context);
        }
    }
    
    /**
     * Log external API call activity
     * @param endpoint API endpoint
     * @param success Whether call was successful
     * @param statusCode HTTP status code
     * @param responseTime Response time in milliseconds
     */
    public void logExternalApiCall(String endpoint, boolean success, int statusCode, long responseTime) {
        Map<String, Object> context = new HashMap<>();
        context.put("endpoint", endpoint);
        context.put("success", success);
        context.put("statusCode", statusCode);
        context.put("responseTime", responseTime);
        context.put("operation", "external_api_call");
        
        if (success) {
            info("External API call successful", context);
        } else {
            error("External API call failed", context);
        }
    }
    
    /**
     * Log authentication flow activity
     * @param userId User ID
     * @param tokenId Token ID
     * @param redirectUrl Redirect URL
     * @param success Whether authentication was successful
     */
    public void logAuthenticationFlow(String userId, String tokenId, String redirectUrl, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("tokenId", tokenId);
        context.put("redirectUrl", redirectUrl);
        context.put("success", success);
        context.put("operation", "authentication_flow");
        
        if (success) {
            info("Autologin authentication successful", context);
        } else {
            error("Autologin authentication failed", context);
        }
    }
    
    /**
     * Log rate limiting activity
     * @param identifier Request identifier (IP, user ID, etc.)
     * @param limit Request limit
     * @param window Time window
     * @param currentCount Current request count
     * @param blocked Whether request was blocked
     */
    public void logRateLimit(String identifier, int limit, int window, int currentCount, boolean blocked) {
        Map<String, Object> context = new HashMap<>();
        context.put("identifier", identifier);
        context.put("limit", limit);
        context.put("window", window);
        context.put("currentCount", currentCount);
        context.put("blocked", blocked);
        context.put("operation", "rate_limiting");
        
        if (blocked) {
            warn("Request blocked due to rate limiting", context);
        } else {
            debug("Rate limit check passed", context);
        }
    }
    
    /**
     * Log configuration change
     * @param key Configuration key
     * @param oldValue Old value
     * @param newValue New value
     */
    public void logConfigChange(String key, String oldValue, String newValue) {
        Map<String, Object> context = new HashMap<>();
        context.put("key", key);
        context.put("oldValue", oldValue);
        context.put("newValue", newValue);
        context.put("operation", "config_change");
        
        info("Configuration changed", context);
    }
    
    /**
     * Core logging method
     * @param level Log level
     * @param message Log message
     * @param context Additional context
     */
    private void log(LogLevel level, String message, Map<String, Object> context) {
        try {
            // Build log entry
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            logEntry.put("level", level.getName());
            logEntry.put("logger", LOGGER_NAME);
            logEntry.put("component", component);
            logEntry.put("message", message);
            
            // Add realm context if available
            if (realm != null) {
                logEntry.put("realm", realm.getName());
            }
            
            // Add session context if available
            if (session != null) {
                logEntry.put("sessionId", session.toString());
            }
            
            // Add additional context
            if (context != null) {
                logEntry.putAll(context);
            }
            
            // Format and output log entry
            String logOutput = formatLogEntry(logEntry);
            outputLog(level, logOutput);
            
        } catch (Exception e) {
            // Fallback logging in case of errors
            System.err.println("Error in AutologinLogger: " + e.getMessage());
            System.err.println("Original message: " + message);
        }
    }
    
    /**
     * Format log entry as structured output
     * @param logEntry Log entry map
     * @return Formatted log string
     */
    private String formatLogEntry(Map<String, Object> logEntry) {
        StringBuilder sb = new StringBuilder();
        
        // Basic format: timestamp [level] component: message
        sb.append(logEntry.get("timestamp"));
        sb.append(" [").append(logEntry.get("level")).append("] ");
        sb.append(logEntry.get("component")).append(": ");
        sb.append(logEntry.get("message"));
        
        // Add context fields
        for (Map.Entry<String, Object> entry : logEntry.entrySet()) {
            String key = entry.getKey();
            if (!Arrays.asList("timestamp", "level", "component", "message", "logger").contains(key)) {
                sb.append(" ").append(key).append("=").append(entry.getValue());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Output log to appropriate destination
     * @param level Log level
     * @param logOutput Formatted log output
     */
    private void outputLog(LogLevel level, String logOutput) {
        // For now, output to System.out/System.err
        // In a real implementation, this would integrate with Keycloak's logging system
        if (level == LogLevel.ERROR) {
            System.err.println(logOutput);
        } else {
            System.out.println(logOutput);
        }
    }
    
    /**
     * Check if logging should occur at the specified level
     * @param level Log level to check
     * @return true if logging should occur
     */
    private boolean shouldLog(LogLevel level) {
        return level.getLevel() <= currentLogLevel.getLevel();
    }
    
    /**
     * Get exception details for logging
     * @param exception Exception to process
     * @return Exception details map
     */
    private Map<String, Object> getExceptionDetails(Exception exception) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", exception.getClass().getSimpleName());
        details.put("message", exception.getMessage());
        
        // Get stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        details.put("stackTrace", sw.toString());
        
        return details;
    }
    
    /**
     * Create logger for specific component
     * @param component Component name
     * @return MagiclinkLogger instance
     */
    public static MagiclinkLogger forComponent(String component) {
        return new MagiclinkLogger(component);
    }
    
    /**
     * Create logger with session context
     * @param session Keycloak session
     * @param realm Realm model
     * @param component Component name
     * @return MagiclinkLogger instance
     */
    public static MagiclinkLogger forSession(KeycloakSession session, RealmModel realm, String component) {
        return new MagiclinkLogger(session, realm, component);
    }
} 