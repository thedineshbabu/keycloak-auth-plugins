package kf.keycloak.plugin.util;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Environment Variables Logger
 * 
 * Utility class for logging environment variables and system properties.
 * Provides comprehensive logging of the Keycloak runtime environment
 * for debugging and configuration verification purposes.
 * 
 * Features:
 * - Logs all system environment variables
 * - Logs all system properties
 * - Provides filtered logging for specific variables
 * - Supports different logging levels
 * - Formats output for easy reading
 */
public class EnvVarsLogger {
    
    private static final Logger logger = Logger.getLogger(EnvVarsLogger.class.getName());
    private static final String SEPARATOR = "=".repeat(80);
    
    /**
     * Log all environment variables with their values
     */
    public void logAllEnvironmentVariables() {
        logger.info("=== ENVIRONMENT VARIABLES START ===");
        logger.info(SEPARATOR);
        
        Map<String, String> envVars = new TreeMap<>(System.getenv());
        
        if (envVars.isEmpty()) {
            logger.info("No environment variables found");
        } else {
            logger.info("Total environment variables found: " + envVars.size());
            logger.info("");
            
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Mask sensitive environment variables
                String maskedValue = maskSensitiveValue(key, value);
                
                logger.info(String.format("ENV: %s = %s", key, maskedValue));
            }
        }
        
        logger.info(SEPARATOR);
        logger.info("=== ENVIRONMENT VARIABLES END ===");
    }
    
    /**
     * Log all system properties
     */
    public void logAllSystemProperties() {
        logger.info("=== SYSTEM PROPERTIES START ===");
        logger.info(SEPARATOR);
        
        Properties props = System.getProperties();
        
        if (props.isEmpty()) {
            logger.info("No system properties found");
        } else {
            logger.info("Total system properties found: " + props.size());
            logger.info("");
            
            // Convert to TreeMap for sorted output
            Map<String, String> sortedProps = new TreeMap<>();
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                sortedProps.put(key, value);
            }
            
            for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Mask sensitive system properties
                String maskedValue = maskSensitiveValue(key, value);
                
                logger.info(String.format("PROP: %s = %s", key, maskedValue));
            }
        }
        
        logger.info(SEPARATOR);
        logger.info("=== SYSTEM PROPERTIES END ===");
    }
    
    /**
     * Log specific environment variables by pattern
     * 
     * @param patterns Array of patterns to match against environment variable names
     */
    public void logEnvironmentVariablesByPattern(String... patterns) {
        logger.info("=== FILTERED ENVIRONMENT VARIABLES START ===");
        logger.info(SEPARATOR);
        
        Map<String, String> envVars = System.getenv();
        int matchCount = 0;
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Check if key matches any pattern
            boolean matches = false;
            for (String pattern : patterns) {
                if (key.toLowerCase().contains(pattern.toLowerCase())) {
                    matches = true;
                    break;
                }
            }
            
            if (matches) {
                String maskedValue = maskSensitiveValue(key, value);
                logger.info(String.format("ENV: %s = %s", key, maskedValue));
                matchCount++;
            }
        }
        
        logger.info("Matched environment variables: " + matchCount);
        logger.info(SEPARATOR);
        logger.info("=== FILTERED ENVIRONMENT VARIABLES END ===");
    }
    
    /**
     * Log environment variables related to Keycloak
     */
    public void logKeycloakEnvironmentVariables() {
        logger.info("=== KEYCLOAK ENVIRONMENT VARIABLES START ===");
        logger.info(SEPARATOR);
        
        String[] keycloakPatterns = {
            "keycloak", "kc_", "kc-", "java", "jboss", "wildfly", "database", "db_",
            "postgres", "mysql", "oracle", "mongo", "redis", "cache", "session",
            "auth", "ssl", "tls", "cert", "key", "trust", "jvm", "heap", "memory"
        };
        
        Map<String, String> envVars = System.getenv();
        int matchCount = 0;
        
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Check if key matches any Keycloak-related pattern
            boolean matches = false;
            for (String pattern : keycloakPatterns) {
                if (key.toLowerCase().contains(pattern.toLowerCase())) {
                    matches = true;
                    break;
                }
            }
            
            if (matches) {
                String maskedValue = maskSensitiveValue(key, value);
                logger.info(String.format("KC_ENV: %s = %s", key, maskedValue));
                matchCount++;
            }
        }
        
        logger.info("Keycloak-related environment variables: " + matchCount);
        logger.info(SEPARATOR);
        logger.info("=== KEYCLOAK ENVIRONMENT VARIABLES END ===");
    }
    
    /**
     * Mask sensitive values in environment variables
     * 
     * @param key The environment variable name
     * @param value The environment variable value
     * @return Masked value if sensitive, original value otherwise
     */
    private String maskSensitiveValue(String key, String value) {
        if (value == null) {
            return "null";
        }
        
        String lowerKey = key.toLowerCase();
        
        // List of sensitive keywords that should be masked
        String[] sensitiveKeywords = {
            "password", "passwd", "secret", "key", "token", "credential", "auth",
            "private", "certificate", "cert", "keystore", "truststore", "jwt",
            "api_key", "apikey", "access_key", "secret_key", "private_key"
        };
        
        for (String keyword : sensitiveKeywords) {
            if (lowerKey.contains(keyword)) {
                if (value.length() <= 8) {
                    return "***";
                } else {
                    return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
                }
            }
        }
        
        return value;
    }
    
    /**
     * Log a simple info message
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * Log a debug message
     */
    public void debug(String message) {
        logger.fine(message);
    }
    
    /**
     * Log a warning message
     */
    public void warn(String message) {
        logger.warning(message);
    }
    
    /**
     * Log an error message
     */
    public void error(String message) {
        logger.severe(message);
    }
    
    /**
     * Log an error message with exception
     */
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
} 