package kf.keycloak.plugin.provider;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Environment Variables Authenticator Factory
 * 
 * Factory class for creating Environment Variables Authenticator instances.
 * This factory registers the authenticator with Keycloak's authentication system
 * and provides configuration options for the authenticator.
 * 
 * Features:
 * - Registers the authenticator with Keycloak
 * - Provides configuration properties
 * - Handles authenticator lifecycle
 * - Supports different authentication flows
 */
public class EnvVarsAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "env-vars-authenticator";
    public static final String DISPLAY_TYPE = "Environment Variables Logger";
    public static final String REFERENCE_CATEGORY = "env-vars";
    
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

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
        return false; // This authenticator doesn't require configuration
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false; // No user setup required
    }

    @Override
    public String getHelpText() {
        return "Logs all environment variables and system properties during authentication. " +
               "Useful for debugging and environment verification. " +
               "This authenticator always succeeds and continues the authentication flow.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // No configuration properties needed for this authenticator
        return null;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new EnvVarsAuthenticator();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Print environment variables at Keycloak startup
        new kf.keycloak.plugin.util.EnvVarsLogger().logAllEnvironmentVariables();
    }

    @Override
    public void close() {
        // No cleanup required
    }
} 