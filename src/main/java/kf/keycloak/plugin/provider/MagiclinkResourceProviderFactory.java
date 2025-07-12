package kf.keycloak.plugin.provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for creating MagiclinkResourceProvider instances
 * Registers the magiclink REST endpoints with Keycloak
 */
public class MagiclinkResourceProviderFactory implements RealmResourceProviderFactory {
    
    public static final String PROVIDER_ID = "magiclink";
    
    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new MagiclinkResourceProvider(session);
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
} 