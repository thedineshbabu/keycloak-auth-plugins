package kf.keycloak.plugin.provider;

import kf.keycloak.plugin.service.MagiclinkService;
import kf.keycloak.plugin.util.MagiclinkLogger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Keycloak authenticator for magiclink authentication
 * Handles magiclink token validation and user authentication
 */
public class MagiclinkAuthenticator implements Authenticator {
    
    private final MagiclinkLogger logger;
    
    /**
     * Default constructor
     */
    public MagiclinkAuthenticator() {
        this.logger = MagiclinkLogger.forComponent("MagiclinkAuthenticator");
    }
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("Starting magiclink authentication");
        
        // Get magiclink token or OAuth code from request
        String tokenOrCode = getTokenFromRequest(context);
        
        if (tokenOrCode == null || tokenOrCode.trim().isEmpty()) {
            logger.warn("No magiclink token or OAuth code provided - falling back to next authenticator");
            context.attempted();
            return;
        }
        
        try {
            // Initialize magiclink service
            MagiclinkService magiclinkService = new MagiclinkService(context.getSession(), context.getRealm());
            
            // Log authentication attempt details
            logger.info("Attempting authentication with token/code: " + tokenOrCode.substring(0, Math.min(10, tokenOrCode.length())) + "...");
            
            // Authenticate with magiclink
            MagiclinkService.AuthenticationResult result = magiclinkService.authenticateWithMagiclink(tokenOrCode);
            
            if (result.isSuccess()) {
                // Set authenticated user
                context.setUser(result.getUser());
                
                // Store original redirect URL in authentication session for later use
                if (result.getRedirectUrl() != null && !result.getRedirectUrl().trim().isEmpty()) {
                    logger.info("Setting magiclink redirect URL: " + result.getRedirectUrl());
                    context.getAuthenticationSession().setAuthNote("magiclink_redirect_url", result.getRedirectUrl());
                    
                    // Add required action for redirect
                    logger.info("Adding magiclink-redirect required action for user: " + result.getUser().getEmail());
                    result.getUser().addRequiredAction(MagiclinkRedirectActionProviderFactory.PROVIDER_ID);
                }
                
                logger.info("Magiclink authentication successful for user: " + result.getUser().getEmail());
                context.success();
                
            } else {
                logger.warn("Magiclink authentication failed: " + result.getError());
                // Don't fail completely, let other authenticators try
                context.attempted();
            }
            
        } catch (Exception e) {
            logger.error("Error during magiclink authentication", e);
            context.attempted();
        }
    }
    
    @Override
    public void action(AuthenticationFlowContext context) {
        // Handle form submission or other actions
        authenticate(context);
    }
    
    @Override
    public boolean requiresUser() {
        return false; // We determine the user from the token
    }
    
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // Always available if enabled
        return true;
    }
    
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions needed
    }
    
    @Override
    public void close() {
        // Cleanup resources if needed
    }
    
    /**
     * Extract magiclink token from request
     * @param context Authentication flow context
     * @return Token string or null if not found
     */
    private String getTokenFromRequest(AuthenticationFlowContext context) {
        try {
            logger.info("Checking request for magiclink token or authorization code");
            
            // Check query parameters
            MultivaluedMap<String, String> params = context.getHttpRequest().getUri().getQueryParameters();
            
            // Try magiclink token first
            String token = params.getFirst("token");
            if (token != null && !token.trim().isEmpty()) {
                logger.info("Found magiclink token in query parameters");
                return token;
            }
            
            // Try OAuth code
            String code = params.getFirst("code");
            if (code != null && !code.trim().isEmpty()) {
                logger.info("Found OAuth code in query parameters");
                return code;
            }
            
            // Check hash fragment for code (common in OAuth flows)
            String fragment = context.getHttpRequest().getUri().toString();
            int hashIndex = fragment.indexOf('#');
            if (hashIndex != -1) {
                fragment = fragment.substring(hashIndex + 1);
                logger.info("Checking hash fragment for code");
                String[] pairs = fragment.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("code")) {
                        logger.info("Found OAuth code in hash fragment");
                        return keyValue[1];
                    }
                }
            }
            
            // Check form parameters
            if (context.getHttpRequest().getHttpMethod().equals("POST")) {
                MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
                
                // Try magiclink token
                token = formParams.getFirst("token");
                if (token != null && !token.trim().isEmpty()) {
                    logger.info("Found magiclink token in form parameters");
                    return token;
                }
                
                // Try OAuth code
                code = formParams.getFirst("code");
                if (code != null && !code.trim().isEmpty()) {
                    logger.info("Found OAuth code in form parameters");
                    return code;
                }
            }
            
            // Check authentication session notes
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            if (authSession != null) {
                // Try magiclink token
                token = authSession.getAuthNote("magiclink_token");
                if (token != null && !token.trim().isEmpty()) {
                    logger.info("Found magiclink token in auth session");
                    return token;
                }
                
                // Try OAuth code
                code = authSession.getAuthNote("code");
                if (code != null && !code.trim().isEmpty()) {
                    logger.info("Found OAuth code in auth session");
                    return code;
                }
            }
            
            logger.warn("No magiclink token or OAuth code found in request");
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting token from request", e);
            return null;
        }
    }
} 