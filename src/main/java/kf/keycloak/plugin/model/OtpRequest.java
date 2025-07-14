package kf.keycloak.plugin.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Request model for OTP generation
 * Contains all necessary parameters for generating an OTP
 */
public class OtpRequest {
    
    private String email;
    private String userId;
    private String sessionId;
    private Integer otpLength;
    private Integer ttlSeconds;
    private String redirectUrl;
    private LocalDateTime requestTime;
    
    /**
     * Default constructor
     */
    public OtpRequest() {
        this.requestTime = LocalDateTime.now();
    }
    
    /**
     * Constructor with required parameters
     * @param email User's email address
     * @param userId User's ID
     * @param sessionId Session ID
     */
    public OtpRequest(String email, String userId, String sessionId) {
        this();
        this.email = email;
        this.userId = userId;
        this.sessionId = sessionId;
    }
    
    /**
     * Constructor with all parameters
     * @param email User's email address
     * @param userId User's ID
     * @param sessionId Session ID
     * @param otpLength OTP length (optional, will use default if null)
     * @param ttlSeconds OTP TTL in seconds (optional, will use default if null)
     * @param redirectUrl Redirect URL after successful authentication (optional)
     */
    public OtpRequest(String email, String userId, String sessionId, Integer otpLength, Integer ttlSeconds, String redirectUrl) {
        this(email, userId, sessionId);
        this.otpLength = otpLength;
        this.ttlSeconds = ttlSeconds;
        this.redirectUrl = redirectUrl;
    }
    
    /**
     * Get user's email address
     * @return Email address
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Set user's email address
     * @param email Email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * Get user's ID
     * @return User ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Set user's ID
     * @param userId User ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Get session ID
     * @return Session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Set session ID
     * @param sessionId Session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Get OTP length
     * @return OTP length or null if not specified
     */
    public Integer getOtpLength() {
        return otpLength;
    }
    
    /**
     * Set OTP length
     * @param otpLength OTP length
     */
    public void setOtpLength(Integer otpLength) {
        this.otpLength = otpLength;
    }
    
    /**
     * Get OTP TTL in seconds
     * @return OTP TTL in seconds or null if not specified
     */
    public Integer getTtlSeconds() {
        return ttlSeconds;
    }
    
    /**
     * Set OTP TTL in seconds
     * @param ttlSeconds OTP TTL in seconds
     */
    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    
    /**
     * Get redirect URL
     * @return Redirect URL or null if not specified
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    /**
     * Set redirect URL
     * @param redirectUrl Redirect URL
     */
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    /**
     * Get request time
     * @return Request timestamp
     */
    public LocalDateTime getRequestTime() {
        return requestTime;
    }
    
    /**
     * Set request time
     * @param requestTime Request timestamp
     */
    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
    
    /**
     * Validate the request
     * @return true if request is valid, false otherwise
     */
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               sessionId != null && !sessionId.trim().isEmpty() &&
               isValidEmail(email);
    }
    
    /**
     * Get validation error message
     * @return Error message or null if valid
     */
    public String getValidationError() {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }
        if (userId == null || userId.trim().isEmpty()) {
            return "User ID is required";
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return "Session ID is required";
        }
        if (!isValidEmail(email)) {
            return "Invalid email format";
        }
        if (otpLength != null && (otpLength < 4 || otpLength > 10)) {
            return "OTP length must be between 4 and 10";
        }
        if (ttlSeconds != null && (ttlSeconds < 60 || ttlSeconds > 3600)) {
            return "OTP TTL must be between 60 and 3600 seconds";
        }
        return null;
    }
    
    /**
     * Check if email format is valid
     * @param email Email to validate
     * @return true if email format is valid
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Create a copy of this request
     * @return New OtpRequest with same values
     */
    public OtpRequest copy() {
        OtpRequest copy = new OtpRequest(email, userId, sessionId, otpLength, ttlSeconds, redirectUrl);
        copy.setRequestTime(requestTime);
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpRequest that = (OtpRequest) o;
        return Objects.equals(email, that.email) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(otpLength, that.otpLength) &&
               Objects.equals(ttlSeconds, that.ttlSeconds) &&
               Objects.equals(redirectUrl, that.redirectUrl);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(email, userId, sessionId, otpLength, ttlSeconds, redirectUrl);
    }
    
    @Override
    public String toString() {
        return "OtpRequest{" +
                "email='" + email + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", otpLength=" + otpLength +
                ", ttlSeconds=" + ttlSeconds +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", requestTime=" + requestTime +
                '}';
    }
} 