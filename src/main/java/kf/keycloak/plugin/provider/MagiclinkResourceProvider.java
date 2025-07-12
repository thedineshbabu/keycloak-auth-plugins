package kf.keycloak.plugin.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import kf.keycloak.plugin.config.MagiclinkConfig;
import kf.keycloak.plugin.model.MagiclinkRequest;
import kf.keycloak.plugin.model.MagiclinkResponse;
import kf.keycloak.plugin.service.ExternalApiService;
import kf.keycloak.plugin.service.MagiclinkService;
import kf.keycloak.plugin.util.MagiclinkLogger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * REST resource provider for magiclink endpoints
 * Provides HTTP endpoints for magiclink generation and management
 */
@Path("/magiclink")
public class MagiclinkResourceProvider implements RealmResourceProvider {
    
    private final KeycloakSession session;
    private final MagiclinkLogger logger;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor with session only
     * @param session Keycloak session
     */
    public MagiclinkResourceProvider(KeycloakSession session) {
        this.session = session;
        this.logger = MagiclinkLogger.forComponent("MagiclinkResourceProvider");
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get current realm from session context
     * @return Current realm model
     */
    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
    
    /**
     * Get magiclink service for current realm
     * @return MagiclinkService instance
     */
    private MagiclinkService getMagiclinkService() {
        return new MagiclinkService(session, getRealm());
    }
    
    /**
     * Handle CORS preflight requests for all endpoints
     * OPTIONS /auth/realms/{realm}/magiclink/*
     * @return Response with CORS headers
     */
    @OPTIONS
    @Path("/{path:.*}")
    public Response handleCorsPreFlight() {
        return addCorsHeaders(Response.ok()).build();
    }

    /**
     * Generate a magiclink for a user
     * POST /auth/realms/{realm}/magiclink/generate
     * @param requestBody JSON request body
     * @return Response with magiclink or error
     */
    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateMagiclink(String requestBody) {
        try {
            logger.info("Magiclink generation request received");
            
            // Parse request
            MagiclinkRequest request = objectMapper.readValue(requestBody, MagiclinkRequest.class);
            
            // Generate magiclink
            MagiclinkResponse response = getMagiclinkService().generateMagiclink(request);
            
            // Return appropriate HTTP status with CORS headers
            if (response.isSuccess()) {
                return addCorsHeaders(Response.ok(response)).build();
            } else {
                return addCorsHeaders(Response.status(Response.Status.BAD_REQUEST).entity(response)).build();
            }
            
        } catch (Exception e) {
            logger.error("Error processing magiclink generation request", e);
            MagiclinkResponse errorResponse = MagiclinkResponse.internalError("Invalid request format");
            return addCorsHeaders(Response.status(Response.Status.BAD_REQUEST).entity(errorResponse)).build();
        }
    }
    
    /**
     * Authenticate user with magiclink token
     * GET /auth/realms/{realm}/magiclink/authenticate?token={token}
     * @param token JWT token from magiclink
     * @return Response with redirect or error
     */
    @GET
    @Path("/authenticate")
    public Response authenticateWithMagiclink(@QueryParam("token") String token) {
        try {
            logger.info("Magiclink authentication request received");
            
            if (token == null || token.trim().isEmpty()) {
                logger.warn("Authentication attempted without token");
                return createErrorPage("Token is required", "MISSING_TOKEN");
            }
            
            // Authenticate user
            MagiclinkService.AuthenticationResult result = getMagiclinkService().authenticateWithMagiclink(token);
            
            if (result.isSuccess()) {
                String redirectUrl = result.getRedirectUrl();
                
                // Validate redirect URL is safe
                if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
                    logger.warn("No redirect URL found in token");
                    return createErrorPage("Invalid redirect URL", "INVALID_REDIRECT_URL");
                }
                
                // Validate redirect URL format
                if (!isValidRedirectUrl(redirectUrl)) {
                    logger.warn("Invalid redirect URL format: " + redirectUrl);
                    return createErrorPage("Invalid redirect URL format", "INVALID_REDIRECT_URL");
                }
                
                // Log successful authentication
                logger.info("Magiclink authentication successful for user: " + result.getUser().getEmail() + 
                           ", redirecting to: " + redirectUrl);
                
                // Perform HTTP redirect to the target URL
                return Response.status(Response.Status.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
            } else {
                logger.warn("Magiclink authentication failed: " + result.getError());
                return createErrorPage(result.getError(), "AUTHENTICATION_FAILED");
            }
            
        } catch (Exception e) {
            logger.error("Error processing magiclink authentication", e);
            return createErrorPage("Authentication failed", "INTERNAL_ERROR");
        }
    }
    
    /**
     * Get magiclink token status
     * GET /auth/realms/{realm}/magiclink/status?token={token}
     * @param token JWT token
     * @return Response with token status
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTokenStatus(@QueryParam("token") String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse("Token is required", "MISSING_TOKEN"))
                    .build();
            }
            
            // Get token status
            MagiclinkService.TokenStatus status = getMagiclinkService().getTokenStatus(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", status.isValid());
            response.put("tokenId", status.getTokenId());
            response.put("email", status.getEmail());
            response.put("error", status.getError());
            
            return addCorsHeaders(Response.ok(response)).build();
            
        } catch (Exception e) {
            logger.error("Error checking token status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createErrorResponse("Status check failed", "INTERNAL_ERROR"))
                .build();
        }
    }
    
    /**
     * Get magiclink configuration
     * GET /auth/realms/{realm}/magiclink/config
     * @return Response with configuration
     */
    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfiguration() {
        try {
            // TODO: Add admin authentication check
            
            MagiclinkConfig config = getMagiclinkService().getConfig();
            Map<String, String> configMap = config.getConfigurationMap();
            
            return addCorsHeaders(Response.ok(configMap)).build();
            
        } catch (Exception e) {
            logger.error("Error getting configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createErrorResponse("Configuration retrieval failed", "INTERNAL_ERROR"))
                .build();
        }
    }
    
    /**
     * Update magiclink configuration
     * PUT /auth/realms/{realm}/magiclink/config
     * @param requestBody JSON configuration
     * @return Response with update result
     */
    @PUT
    @Path("/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfiguration(String requestBody) {
        try {
            // TODO: Add admin authentication check
            
            @SuppressWarnings("unchecked")
            Map<String, String> configUpdates = objectMapper.readValue(requestBody, Map.class);
            
            MagiclinkConfig config = getMagiclinkService().getConfig();
            
            // Validate configuration
            config.updateFromMap(configUpdates);
            MagiclinkConfig.ValidationResult validation = config.validateConfiguration();
            
            if (!validation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR"))
                    .build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuration updated successfully");
            response.put("config", config.getConfigurationMap());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Error updating configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createErrorResponse("Configuration update failed", "INTERNAL_ERROR"))
                .build();
        }
    }
    
    /**
     * Test external API connectivity
     * GET /auth/realms/{realm}/magiclink/test-api
     * @return Response with test result
     */
    @GET
    @Path("/test-api")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testExternalApi() {
        try {
            // TODO: Add admin authentication check
            
            ExternalApiService.ApiResponse apiResponse = getMagiclinkService().testExternalApi();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", apiResponse.isSuccess());
            response.put("statusCode", apiResponse.getStatusCode());
            response.put("responseBody", apiResponse.getResponseBody());
            response.put("error", apiResponse.getErrorMessage());
            response.put("timestamp", apiResponse.getTimestamp());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Error testing external API", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createErrorResponse("API test failed", "INTERNAL_ERROR"))
                .build();
        }
    }
    
    /**
     * Get plugin health status
     * GET /auth/realms/{realm}/magiclink/health
     * @return Response with health status
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealth() {
        try {
            MagiclinkConfig config = getMagiclinkService().getConfig();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("enabled", config.isEnabled());
            health.put("externalApiConfigured", config.isExternalApiConfigured());
            health.put("rateLimitEnabled", config.isRateLimitEnabled());
            health.put("realm", getRealm().getName());
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return addCorsHeaders(Response.ok(health)).build();
            
        } catch (Exception e) {
            logger.error("Error checking health", e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(health).build();
        }
    }
    
    /**
     * Create standard error response
     * @param message Error message
     * @param errorCode Error code
     * @return Error response map
     */
    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("errorCode", errorCode);
        error.put("timestamp", java.time.LocalDateTime.now());
        return error;
    }
    
    /**
     * Create error page response for authentication failures
     * @param message Error message
     * @param errorCode Error code
     * @return HTTP response with error page
     */
    private Response createErrorPage(String message, String errorCode) {
        // Create a simple HTML error page
        String htmlContent = createErrorHtml(message, errorCode);
        
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(htmlContent)
            .type(MediaType.TEXT_HTML)
            .build();
    }
    
    /**
     * Create HTML error page content
     * @param message Error message
     * @param errorCode Error code
     * @return HTML content
     */
    private String createErrorHtml(String message, String errorCode) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>Authentication Error</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 40px; }\n" +
               "        .error-container { max-width: 500px; margin: 0 auto; text-align: center; }\n" +
               "        .error-title { color: #d32f2f; margin-bottom: 20px; }\n" +
               "        .error-message { color: #666; margin-bottom: 30px; }\n" +
               "        .error-code { color: #999; font-size: 12px; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"error-container\">\n" +
               "        <h1 class=\"error-title\">Authentication Failed</h1>\n" +
               "        <p class=\"error-message\">" + escapeHtml(message) + "</p>\n" +
               "        <p class=\"error-code\">Error Code: " + escapeHtml(errorCode) + "</p>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Validate redirect URL format and security
     * @param redirectUrl URL to validate
     * @return true if valid and safe
     */
    private boolean isValidRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            URI uri = URI.create(redirectUrl);
            
            // Must be absolute URL
            if (!uri.isAbsolute()) {
                return false;
            }
            
            // Check scheme - allow HTTP for localhost/127.0.0.1, require HTTPS for others
            String scheme = uri.getScheme();
            String host = uri.getHost();
            
            if ("https".equals(scheme)) {
                // HTTPS is always allowed
            } else if ("http".equals(scheme)) {
                // HTTP is only allowed for localhost and 127.0.0.1 (development)
                if (!"localhost".equals(host) && !"127.0.0.1".equals(host)) {
                    logger.warn("HTTP redirect URL not allowed for host: " + host + " (use HTTPS for non-localhost)");
                    return false;
                }
            } else {
                logger.warn("Invalid scheme for redirect URL: " + scheme);
                return false;
            }
            
            // Check against realm configuration
            MagiclinkConfig config = getMagiclinkService().getConfig();
            return config.isRedirectUrlAllowed(redirectUrl);
            
        } catch (Exception e) {
            logger.error("Error validating redirect URL: " + redirectUrl, e);
            return false;
        }
    }
    
    /**
     * Escape HTML special characters
     * @param text Text to escape
     * @return Escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Add CORS headers to allow cross-origin requests from React app
     * @param responseBuilder Response builder to add headers to
     * @return Response builder with CORS headers added
     */
    private Response.ResponseBuilder addCorsHeaders(Response.ResponseBuilder responseBuilder) {
        return responseBuilder
            .header("Access-Control-Allow-Origin", "http://localhost:3000")
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
            .header("Access-Control-Allow-Credentials", "true")
            .header("Access-Control-Max-Age", "3600");
    }
    
    @Override
    public Object getResource() {
        return this;
    }
    
    @Override
    public void close() {
        // Cleanup resources if needed
    }
} 