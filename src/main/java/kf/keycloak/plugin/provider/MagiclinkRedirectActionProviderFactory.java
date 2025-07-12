package kf.keycloak.plugin.provider;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for creating MagiclinkRedirectActionProvider instances
 */
public class MagiclinkRedirectActionProviderFactory implements RequiredActionFactory {

    public static final String PROVIDER_ID = "magiclink-redirect";
    private static final String DISPLAY_TEXT = "Magiclink Redirect";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return DISPLAY_TEXT;
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new MagiclinkRedirectActionProvider();
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to clean up
    }
} 