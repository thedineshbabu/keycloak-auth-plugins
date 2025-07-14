package kf.keycloak.plugin.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Response model for OTP generation
 * Contains the result of OTP generation attempt
 */
public class OtpResponse {
    
    private boolean success;
    private String otpId;
    private String error;
    private String errorCode;
    private LocalDateTime expiresAt;
    private String email;
    private String userId;
    private LocalDateTime generatedAt;
    
    /**
     * Private constructor for static factory methods
     */
    private OtpResponse() {
        this.generatedAt = LocalDateTime.now();
    }
    
    /**
     * Create a successful OTP response
     * @param otpId Generated OTP ID
     * @param expiresAt OTP expiration time
     * @param email User's email
     * @param userId User's ID
     * @return OtpResponse with success status
     */
    public static OtpResponse success(String otpId, LocalDateTime expiresAt, String email, String userId) {
        OtpResponse response = new OtpResponse();
        response.success = true;
        response.otpId = otpId;
        response.expiresAt = expiresAt;
        response.email = email;
        response.userId = userId;
        return response;
    }
    
    /**
     * Create an error response
     * @param error Error message
     * @param errorCode Error code for programmatic handling
     * @return OtpResponse with error status
     */
    public static OtpResponse error(String error, String errorCode) {
        OtpResponse response = new OtpResponse();
        response.success = false;
        response.error = error;
        response.errorCode = errorCode;
        return response;
    }
    
    /**
     * Create a validation error response
     * @param error Validation error message
     * @return OtpResponse with validation error
     */
    public static OtpResponse validationError(String error) {
        return error(error, "VALIDATION_ERROR");
    }
    
    /**
     * Create a user not found error response
     * @return OtpResponse for user not found
     */
    public static OtpResponse userNotFound() {
        return error("User not found", "USER_NOT_FOUND");
    }
    
    /**
     * Create a rate limit exceeded error response
     * @return OtpResponse for rate limit exceeded
     */
    public static OtpResponse rateLimitExceeded() {
        return error("Rate limit exceeded", "RATE_LIMIT_EXCEEDED");
    }
    
    /**
     * Create an external API error response
     * @param error API error message
     * @return OtpResponse for external API error
     */
    public static OtpResponse externalApiError(String error) {
        return error(error, "EXTERNAL_API_ERROR");
    }
    
    /**
     * Create an internal error response
     * @param error Internal error message
     * @return OtpResponse for internal error
     */
    public static OtpResponse internalError(String error) {
        return error(error, "INTERNAL_ERROR");
    }
    
    /**
     * Create a feature disabled error response
     * @return OtpResponse for disabled feature
     */
    public static OtpResponse featureDisabled() {
        return error("OTP feature is disabled", "FEATURE_DISABLED");
    }
    
    /**
     * Check if response indicates success
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Get OTP ID
     * @return OTP ID or null if not successful
     */
    public String getOtpId() {
        return otpId;
    }
    
    /**
     * Set OTP ID
     * @param otpId OTP ID
     */
    public void setOtpId(String otpId) {
        this.otpId = otpId;
    }
    
    /**
     * Get error message
     * @return Error message or null if successful
     */
    public String getError() {
        return error;
    }
    
    /**
     * Set error message
     * @param error Error message
     */
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * Get error code
     * @return Error code or null if successful
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Set error code
     * @param errorCode Error code
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * Get OTP expiration time
     * @return Expiration time or null if not successful
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Set OTP expiration time
     * @param expiresAt Expiration time
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    /**
     * Get user's email
     * @return Email address or null if not successful
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Set user's email
     * @param email Email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * Get user's ID
     * @return User ID or null if not successful
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
     * Get generation timestamp
     * @return Generation timestamp
     */
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    /**
     * Set generation timestamp
     * @param generatedAt Generation timestamp
     */
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    /**
     * Check if OTP is expired
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Get remaining time in seconds
     * @return Remaining time in seconds or -1 if expired or no expiration set
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) {
            return -1;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        
        return java.time.Duration.between(now, expiresAt).getSeconds();
    }
    
    /**
     * Create a copy of this response
     * @return New OtpResponse with same values
     */
    public OtpResponse copy() {
        OtpResponse copy = new OtpResponse();
        copy.success = this.success;
        copy.otpId = this.otpId;
        copy.error = this.error;
        copy.errorCode = this.errorCode;
        copy.expiresAt = this.expiresAt;
        copy.email = this.email;
        copy.userId = this.userId;
        copy.generatedAt = this.generatedAt;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpResponse that = (OtpResponse) o;
        return success == that.success &&
               Objects.equals(otpId, that.otpId) &&
               Objects.equals(error, that.error) &&
               Objects.equals(errorCode, that.errorCode) &&
               Objects.equals(expiresAt, that.expiresAt) &&
               Objects.equals(email, that.email) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, otpId, error, errorCode, expiresAt, email, userId);
    }
    
    @Override
    public String toString() {
        return "OtpResponse{" +
                "success=" + success +
                ", otpId='" + otpId + '\'' +
                ", error='" + error + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", expiresAt=" + expiresAt +
                ", email='" + email + '\'' +
                ", userId='" + userId + '\'' +
                ", generatedAt=" + generatedAt +
                '}';
    }
} 