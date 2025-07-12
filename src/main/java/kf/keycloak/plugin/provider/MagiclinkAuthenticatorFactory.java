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
 * Factory for creating MagiclinkAuthenticator instances
 * Registers the magiclink authenticator with Keycloak
 */
public class MagiclinkAuthenticatorFactory implements AuthenticatorFactory {
    
    public static final String PROVIDER_ID = "magiclink-authenticator";
    
    private static final MagiclinkAuthenticator SINGLETON = new MagiclinkAuthenticator();
    
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }
    
    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Initialize with configuration if needed
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Post-initialization tasks if needed
    }
    
    @Override
    public void close() {
        // Cleanup resources if needed
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getDisplayType() {
        return "Magiclink Authenticator";
    }
    
    @Override
    public String getReferenceCategory() {
        return "magiclink";
    }
    
    @Override
    public boolean isConfigurable() {
        return false;
    }
    
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }
    
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }
    
    @Override
    public String getHelpText() {
        return "Validates a magiclink token and authenticates the user";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList();
    }
} 