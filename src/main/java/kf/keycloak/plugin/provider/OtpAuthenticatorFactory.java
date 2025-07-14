package kf.keycloak.plugin.provider;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating OTP authenticator instances
 * Provides configuration options and metadata for the OTP authenticator
 */
public class OtpAuthenticatorFactory implements AuthenticatorFactory {
    
    public static final String PROVIDER_ID = "otp-authenticator";
    public static final String DISPLAY_TYPE = "OTP Authentication";
    public static final String REFERENCE_CATEGORY = null;
    public static final boolean IS_CONFIGURABLE = true;
    public static final boolean IS_USER_SETUP_ALLOWED = false;
    public static final String HELP_TEXT = "Validates OTP codes sent via external API";
    
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.DISABLED
    };
    
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = Arrays.asList(
        new ProviderConfigProperty("otp.external.api.url", "OTP API URL", 
            "External API endpoint for OTP delivery", ProviderConfigProperty.STRING_TYPE, null),
        new ProviderConfigProperty("otp.eligibility.api.url", "Eligibility API URL", 
            "External API endpoint for eligibility checks", ProviderConfigProperty.STRING_TYPE, null),
        new ProviderConfigProperty("otp.length", "OTP Length", 
            "Length of generated OTP", ProviderConfigProperty.STRING_TYPE, "6"),
        new ProviderConfigProperty("otp.ttl", "OTP TTL (seconds)", 
            "OTP validity duration in seconds", ProviderConfigProperty.STRING_TYPE, "300"),
        new ProviderConfigProperty("otp.fail.if.eligibility.fails", "Fail if eligibility check fails", 
            "Whether to fail authentication if eligibility API is unavailable", ProviderConfigProperty.BOOLEAN_TYPE, "false"),
        new ProviderConfigProperty("otp.enabled", "OTP Enabled", 
            "Whether OTP authentication is enabled", ProviderConfigProperty.BOOLEAN_TYPE, "true"),
        new ProviderConfigProperty("otp.enabled.realms", "Enabled Realms", 
            "Comma-separated list of realms where OTP is enabled (empty for all realms)", ProviderConfigProperty.STRING_TYPE, ""),
        new ProviderConfigProperty("otp.max.retry.attempts", "Max Retry Attempts", 
            "Maximum number of OTP validation attempts", ProviderConfigProperty.STRING_TYPE, "3"),
        new ProviderConfigProperty("otp.rate.limit.enabled", "Rate Limiting Enabled", 
            "Whether to enable rate limiting for OTP generation", ProviderConfigProperty.BOOLEAN_TYPE, "true"),
        new ProviderConfigProperty("otp.rate.limit.requests", "Rate Limit Requests", 
            "Number of OTP requests allowed per time window", ProviderConfigProperty.STRING_TYPE, "5"),
        new ProviderConfigProperty("otp.rate.limit.window", "Rate Limit Window (seconds)", 
            "Time window for rate limiting in seconds", ProviderConfigProperty.STRING_TYPE, "60")
    );
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getDisplayType() {
        return DISPLAY_TYPE;
    }
    
    @Override
    public String getReferenceCategory() {
        return REFERENCE_CATEGORY;
    }
    
    @Override
    public boolean isConfigurable() {
        return IS_CONFIGURABLE;
    }
    
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }
    
    @Override
    public boolean isUserSetupAllowed() {
        return IS_USER_SETUP_ALLOWED;
    }
    
    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }
    
    @Override
    public Authenticator create(KeycloakSession session) {
        return new OtpAuthenticator();
    }
    
    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization needed
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }
    
    @Override
    public void close() {
        // No cleanup needed
    }
} 