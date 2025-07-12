package kf.keycloak.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Request model for magiclink generation
 * Contains user email and redirect URL information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MagiclinkRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * User email address for magiclink generation
     * Must be a valid email format
     */
    @JsonProperty("email")
    private String email;
    
    /**
     * Redirect URL where user will be sent after successful authentication
     * Must be a valid HTTPS URL, or HTTP for localhost/127.0.0.1
     */
    @JsonProperty("redirectUrl")
    private String redirectUrl;
    
    /**
     * Optional expiration time in minutes (default: 15)
     * Must be between 1 and 60 minutes
     */
    @JsonProperty("expirationMinutes")
    private Integer expirationMinutes = 15;
    
    /**
     * Optional client ID for specific application context
     */
    @JsonProperty("clientId")
    private String clientId;
    
    // Default constructor for JSON deserialization
    public MagiclinkRequest() {}
    
    /**
     * Constructor with required fields
     * @param email User email address
     * @param redirectUrl Target redirect URL
     */
    public MagiclinkRequest(String email, String redirectUrl) {
        this.email = email;
        this.redirectUrl = redirectUrl;
    }
    
    /**
     * Full constructor with all fields
     * @param email User email address
     * @param redirectUrl Target redirect URL
     * @param expirationMinutes Token expiration in minutes
     * @param clientId Optional client ID
     */
    public MagiclinkRequest(String email, String redirectUrl, Integer expirationMinutes, String clientId) {
        this.email = email;
        this.redirectUrl = redirectUrl;
        this.expirationMinutes = expirationMinutes;
        this.clientId = clientId;
    }
    
    // Getters and Setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    public Integer getExpirationMinutes() {
        return expirationMinutes;
    }
    
    public void setExpirationMinutes(Integer expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    /**
     * Validates the request data
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               redirectUrl != null && !redirectUrl.trim().isEmpty() &&
               isValidRedirectUrl(redirectUrl) &&
               expirationMinutes != null && expirationMinutes >= 1 && expirationMinutes <= 60;
    }
    
    /**
     * Validates redirect URL format - allows HTTPS everywhere, HTTP only for localhost
     * @param url URL to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidRedirectUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Allow HTTPS for any domain
        if (url.startsWith("https://")) {
            return true;
        }
        
        // Allow HTTP only for localhost and 127.0.0.1
        if (url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1")) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "MagiclinkRequest{" +
               "email='" + email + '\'' +
               ", redirectUrl='" + redirectUrl + '\'' +
               ", expirationMinutes=" + expirationMinutes +
               ", clientId='" + clientId + '\'' +
               '}';
    }
} 