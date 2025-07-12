package kf.keycloak.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Response model for magiclink generation
 * Contains success status, magiclink URL, and metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MagiclinkResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Success status of the magiclink generation
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * Generated magiclink URL
     * Only present if generation was successful
     */
    @JsonProperty("magiclink")
    private String magiclink;
    
    /**
     * Unique token identifier for tracking
     */
    @JsonProperty("tokenId")
    private String tokenId;
    
    /**
     * Expiration timestamp of the magiclink
     */
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;
    
    /**
     * Error message if generation failed
     */
    @JsonProperty("error")
    private String error;
    
    /**
     * Error code for programmatic error handling
     */
    @JsonProperty("errorCode")
    private String errorCode;
    
    /**
     * Additional message or information
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Timestamp when the response was generated
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Default constructor
    public MagiclinkResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor for successful response
     * @param magiclink Generated magiclink URL
     * @param tokenId Unique token identifier
     * @param expiresAt Expiration timestamp
     */
    public MagiclinkResponse(String magiclink, String tokenId, LocalDateTime expiresAt) {
        this();
        this.success = true;
        this.magiclink = magiclink;
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
        this.message = "Magiclink generated successfully";
    }
    
    /**
     * Constructor for error response
     * @param error Error message
     * @param errorCode Error code
     */
    public MagiclinkResponse(String error, String errorCode) {
        this();
        this.success = false;
        this.error = error;
        this.errorCode = errorCode;
    }
    
    /**
     * Static method to create a successful response
     * @param magiclink Generated magiclink URL
     * @param tokenId Unique token identifier
     * @param expiresAt Expiration timestamp
     * @return MagiclinkResponse instance
     */
    public static MagiclinkResponse success(String magiclink, String tokenId, LocalDateTime expiresAt) {
        return new MagiclinkResponse(magiclink, tokenId, expiresAt);
    }
    
    /**
     * Static method to create an error response
     * @param error Error message
     * @param errorCode Error code
     * @return MagiclinkResponse instance
     */
    public static MagiclinkResponse error(String error, String errorCode) {
        return new MagiclinkResponse(error, errorCode);
    }
    
    /**
     * Static method to create a validation error response
     * @param message Validation error message
     * @return MagiclinkResponse instance
     */
    public static MagiclinkResponse validationError(String message) {
        return new MagiclinkResponse(message, "VALIDATION_ERROR");
    }
    
    /**
     * Static method to create a user not found error response
     * @return MagiclinkResponse instance
     */
    public static MagiclinkResponse userNotFound() {
        return new MagiclinkResponse("User not found", "USER_NOT_FOUND");
    }
    
    /**
     * Static method to create an internal server error response
     * @param message Error message
     * @return MagiclinkResponse instance
     */
    public static MagiclinkResponse internalError(String message) {
        return new MagiclinkResponse("Internal server error: " + message, "INTERNAL_ERROR");
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMagiclink() {
        return magiclink;
    }
    
    public void setMagiclink(String magiclink) {
        this.magiclink = magiclink;
    }
    
    public String getTokenId() {
        return tokenId;
    }
    
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "MagiclinkResponse{" +
               "success=" + success +
               ", magiclink='" + magiclink + '\'' +
               ", tokenId='" + tokenId + '\'' +
               ", expiresAt=" + expiresAt +
               ", error='" + error + '\'' +
               ", errorCode='" + errorCode + '\'' +
               ", message='" + message + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
} 