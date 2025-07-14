package kf.keycloak.plugin.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Response model for eligibility checks
 * Contains the result of checking if a user is eligible for OTP MFA
 */
public class EligibilityResponse {
    
    private boolean enabled;
    private String reason;
    private boolean apiSuccess;
    private String email;
    private LocalDateTime checkedAt;
    private String apiError;
    private Integer apiStatusCode;
    
    /**
     * Private constructor for static factory methods
     */
    private EligibilityResponse() {
        this.checkedAt = LocalDateTime.now();
    }
    
    /**
     * Create a successful eligibility response
     * @param enabled Whether user is eligible for OTP
     * @param email User's email address
     * @param reason Reason for eligibility decision (optional)
     * @return EligibilityResponse with eligibility result
     */
    public static EligibilityResponse success(boolean enabled, String email, String reason) {
        EligibilityResponse response = new EligibilityResponse();
        response.enabled = enabled;
        response.email = email;
        response.reason = reason;
        response.apiSuccess = true;
        return response;
    }
    
    /**
     * Create a successful enabled response
     * @param email User's email address
     * @param reason Reason for enabling (optional)
     * @return EligibilityResponse indicating user is eligible
     */
    public static EligibilityResponse enabled(String email, String reason) {
        return success(true, email, reason);
    }
    
    /**
     * Create a successful enabled response without reason
     * @param email User's email address
     * @return EligibilityResponse indicating user is eligible
     */
    public static EligibilityResponse enabled(String email) {
        return enabled(email, null);
    }
    
    /**
     * Create a successful disabled response
     * @param email User's email address
     * @param reason Reason for disabling
     * @return EligibilityResponse indicating user is not eligible
     */
    public static EligibilityResponse disabled(String email, String reason) {
        return success(false, email, reason);
    }
    
    /**
     * Create a successful disabled response without reason
     * @param email User's email address
     * @return EligibilityResponse indicating user is not eligible
     */
    public static EligibilityResponse disabled(String email) {
        return disabled(email, "User not eligible for OTP MFA");
    }
    
    /**
     * Create an API failure response
     * @param email User's email address
     * @param apiError API error message
     * @param apiStatusCode HTTP status code from API
     * @return EligibilityResponse indicating API failure
     */
    public static EligibilityResponse apiFailure(String email, String apiError, Integer apiStatusCode) {
        EligibilityResponse response = new EligibilityResponse();
        response.enabled = false;
        response.email = email;
        response.apiError = apiError;
        response.apiStatusCode = apiStatusCode;
        response.apiSuccess = false;
        return response;
    }
    
    /**
     * Create a timeout response
     * @param email User's email address
     * @return EligibilityResponse indicating API timeout
     */
    public static EligibilityResponse timeout(String email) {
        return apiFailure(email, "API request timed out", null);
    }
    
    /**
     * Create a network error response
     * @param email User's email address
     * @return EligibilityResponse indicating network error
     */
    public static EligibilityResponse networkError(String email) {
        return apiFailure(email, "Network error occurred", null);
    }
    
    /**
     * Create a configuration error response
     * @param email User's email address
     * @return EligibilityResponse indicating configuration error
     */
    public static EligibilityResponse configurationError(String email) {
        return apiFailure(email, "Eligibility API not configured", null);
    }
    
    /**
     * Check if user is eligible for OTP
     * @return true if eligible, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set eligibility status
     * @param enabled Whether user is eligible
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get reason for eligibility decision
     * @return Reason or null if not provided
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Set reason for eligibility decision
     * @param reason Reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    /**
     * Check if API call was successful
     * @return true if API call succeeded, false otherwise
     */
    public boolean isApiSuccess() {
        return apiSuccess;
    }
    
    /**
     * Set API success status
     * @param apiSuccess Whether API call succeeded
     */
    public void setApiSuccess(boolean apiSuccess) {
        this.apiSuccess = apiSuccess;
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
     * Get check timestamp
     * @return Timestamp when check was performed
     */
    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
    
    /**
     * Set check timestamp
     * @param checkedAt Check timestamp
     */
    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
    
    /**
     * Get API error message
     * @return API error message or null if API call succeeded
     */
    public String getApiError() {
        return apiError;
    }
    
    /**
     * Set API error message
     * @param apiError API error message
     */
    public void setApiError(String apiError) {
        this.apiError = apiError;
    }
    
    /**
     * Get API status code
     * @return HTTP status code or null if not available
     */
    public Integer getApiStatusCode() {
        return apiStatusCode;
    }
    
    /**
     * Set API status code
     * @param apiStatusCode HTTP status code
     */
    public void setApiStatusCode(Integer apiStatusCode) {
        this.apiStatusCode = apiStatusCode;
    }
    
    /**
     * Check if this response indicates a fallback scenario
     * @return true if this is a fallback response (API failed but user enabled)
     */
    public boolean isFallback() {
        return !apiSuccess && enabled;
    }
    
    /**
     * Get a human-readable description of the eligibility status
     * @return Description of eligibility status
     */
    public String getStatusDescription() {
        if (apiSuccess) {
            if (enabled) {
                return "User is eligible for OTP MFA" + (reason != null ? " (" + reason + ")" : "");
            } else {
                return "User is not eligible for OTP MFA" + (reason != null ? " (" + reason + ")" : "");
            }
        } else {
            if (enabled) {
                return "API failed, using fallback (user enabled)";
            } else {
                return "API failed, user not eligible" + (apiError != null ? " (" + apiError + ")" : "");
            }
        }
    }
    
    /**
     * Create a copy of this response
     * @return New EligibilityResponse with same values
     */
    public EligibilityResponse copy() {
        EligibilityResponse copy = new EligibilityResponse();
        copy.enabled = this.enabled;
        copy.reason = this.reason;
        copy.apiSuccess = this.apiSuccess;
        copy.email = this.email;
        copy.checkedAt = this.checkedAt;
        copy.apiError = this.apiError;
        copy.apiStatusCode = this.apiStatusCode;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EligibilityResponse that = (EligibilityResponse) o;
        return enabled == that.enabled &&
               apiSuccess == that.apiSuccess &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(email, that.email) &&
               Objects.equals(apiError, that.apiError) &&
               Objects.equals(apiStatusCode, that.apiStatusCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, reason, apiSuccess, email, apiError, apiStatusCode);
    }
    
    @Override
    public String toString() {
        return "EligibilityResponse{" +
                "enabled=" + enabled +
                ", reason='" + reason + '\'' +
                ", apiSuccess=" + apiSuccess +
                ", email='" + email + '\'' +
                ", checkedAt=" + checkedAt +
                ", apiError='" + apiError + '\'' +
                ", apiStatusCode=" + apiStatusCode +
                '}';
    }
} 