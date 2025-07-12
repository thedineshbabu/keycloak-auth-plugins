package kf.keycloak.plugin.provider;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import kf.keycloak.plugin.util.MagiclinkLogger;

/**
 * Required Action Provider for handling magiclink redirects
 * This provider handles the final step of magiclink authentication
 */
public class MagiclinkRedirectActionProvider implements RequiredActionProvider {
    private final MagiclinkLogger logger;
    
    public MagiclinkRedirectActionProvider() {
        this.logger = MagiclinkLogger.forComponent("MagiclinkRedirectActionProvider");
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // Check if we need to redirect
        String redirectUrl = context.getAuthenticationSession().getAuthNote("magiclink_redirect_url");
        if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
            logger.info("Found redirect URL in session: " + redirectUrl);
            context.getUser().addRequiredAction("magiclink-redirect");
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        logger.debug("Processing magiclink redirect challenge");
        
        try {
            // Get the user and redirect URL
            UserModel user = context.getUser();
            String redirectUrl = context.getAuthenticationSession().getAuthNote("magiclink_redirect_url");
            
            logger.debug("Processing redirect for user: " + user.getUsername() + " to URL: " + redirectUrl);

            if (redirectUrl != null && !redirectUrl.trim().isEmpty()) {
                // Clear the stored URL
                context.getAuthenticationSession().removeAuthNote("magiclink_redirect_url");
                
                // Set the redirect URL in the response
                context.getAuthenticationSession().setRedirectUri(redirectUrl);
                
                logger.info("Set redirect URL for user: " + user.getUsername() + " to: " + redirectUrl);
            } else {
                logger.warn("No redirect URL found for user: " + user.getUsername());
            }

            // Mark the action as complete
            context.success();
        } catch (Exception e) {
            logger.error("Error during magiclink redirect: " + e.getMessage(), e);
            context.failure();
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // The actual processing is done in requiredActionChallenge
        requiredActionChallenge(context);
    }

    @Override
    public void close() {
        // No resources to clean up
    }
} 