package kf.keycloak.plugin.provider;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import kf.keycloak.plugin.util.EnvVarsLogger;

/**
 * Environment Variables Authenticator
 * 
 * This authenticator prints all environment variables and their values during startup.
 * It can be used as a custom authenticator in Keycloak authentication flows to debug
 * environment configuration and understand the Keycloak runtime environment.
 * 
 * Features:
 * - Prints all system environment variables during initialization
 * - Can be configured to show only specific environment variables
 * - Provides detailed logging of environment state
 * - Useful for debugging and environment verification
 */
public class EnvVarsAuthenticator implements Authenticator {

    private static final EnvVarsLogger logger = new EnvVarsLogger();
    private boolean initialized = false;

    /**
     * Initialize the authenticator and print environment variables
     */
    private void initialize() {
        if (!initialized) {
            logger.logAllEnvironmentVariables();
            initialized = true;
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Initialize and log environment variables on first authentication
        initialize();
        
        // Log authentication attempt
        logger.info("Environment Variables Authenticator - Authentication requested for user: " + 
                   (context.getUser() != null ? context.getUser().getUsername() : "unknown"));
        
        // For this authenticator, we always succeed and continue
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No action required for this authenticator
        logger.debug("Environment Variables Authenticator - Action called");
    }

    @Override
    public boolean requiresUser() {
        // This authenticator doesn't require a user to be present
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // This authenticator is always configured
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions for this authenticator
    }

    @Override
    public void close() {
        // Cleanup if needed
        logger.debug("Environment Variables Authenticator - Closing");
    }
} 