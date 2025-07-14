package kf.keycloak.plugin.provider;

import kf.keycloak.plugin.config.OtpConfig;
import kf.keycloak.plugin.model.OtpRequest;
import kf.keycloak.plugin.model.OtpResponse;
import kf.keycloak.plugin.model.EligibilityResponse;
import kf.keycloak.plugin.service.EligibilityService;
import kf.keycloak.plugin.service.OtpService;
import kf.keycloak.plugin.util.OtpLogger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Keycloak authenticator for OTP authentication
 * Handles OTP generation, validation, and user authentication in browser flows
 */
public class OtpAuthenticator implements Authenticator {
    
    private final OtpLogger logger;
    
    /**
     * Default constructor
     */
    public OtpAuthenticator() {
        this.logger = OtpLogger.forComponent("OtpAuthenticator");
    }
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("Starting OTP authentication");
        
        // Check if this is a direct grant flow (no web interface)
        boolean isDirectGrant = isDirectGrantFlow(context);
        if (isDirectGrant) {
            logger.info("Direct grant flow detected, handling OTP via parameters");
            
            // Handle OTP authentication for direct grant
            handleDirectGrantOtp(context);
            return;
        }
        
        // Check if OTP is enabled
        OtpConfig config = new OtpConfig(context.getSession(), context.getRealm());
        if (!config.isEnabled()) {
            logger.info("OTP authentication disabled, skipping");
            context.attempted();
            return;
        }
        
        // Check if realm is enabled
        if (!config.isRealmEnabled()) {
            logger.info("OTP authentication not enabled for this realm, skipping");
            context.attempted();
            return;
        }
        
        // Get current user
        UserModel user = context.getUser();
        if (user == null) {
            logger.warn("No user found in authentication context");
            context.attempted();
            return;
        }
        
        // Get user email
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            logger.warn("User has no email address: " + user.getId());
            context.attempted();
            return;
        }
        
        // Check eligibility
        EligibilityService eligibilityService = new EligibilityService(config, logger);
        EligibilityResponse eligibility = eligibilityService.checkEligibility(email);
        
        if (!eligibility.isEnabled()) {
            logger.info("User not eligible for OTP: " + email);
            // Ensure user is set in context and complete authentication
            context.setUser(user);
            context.success(); // Complete authentication successfully
            return;
        }
        
        // Check if OTP was already generated for this session
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String otpId = authSession.getAuthNote("otp_id");
        
        if (otpId == null || otpId.trim().isEmpty()) {
            // Generate OTP
            logger.info("Generating OTP for user: " + email);
            
            OtpService otpService = new OtpService(context.getSession(), context.getRealm());
            OtpRequest request = new OtpRequest(
                email,
                user.getId(),
                authSession.getParentSession().getId(),
                config.getOtpLength(),
                config.getOtpTtl(),
                null // redirect URL will be handled by Keycloak
            );
            
            OtpResponse response = otpService.generateOtp(request);
            
            if (response.isSuccess()) {
                // Store OTP ID in session
                authSession.setAuthNote("otp_id", response.getOtpId());
                authSession.setAuthNote("otp_email", email);
                authSession.setAuthNote("otp_user_id", user.getId());
                
                // Show OTP input form
                logger.info("OTP generated successfully, showing input form");
                context.challenge(createOtpChallenge(context));
                
            } else {
                logger.error("Failed to generate OTP: " + response.getError());
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("Failed to generate OTP", response.getError()));
            }
            
        } else {
            // OTP already generated, show input form
            logger.info("OTP already generated, showing input form");
            context.challenge(createOtpChallenge(context));
        }
    }
    
    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("Processing OTP form submission");
        
        // Get form parameters
        MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
        String otpInput = formParams.getFirst("otp");
        String resendOtp = formParams.getFirst("resend_otp");
        
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String otpId = authSession.getAuthNote("otp_id");
        String email = authSession.getAuthNote("otp_email");
        String userId = authSession.getAuthNote("otp_user_id");
        
        if (otpId == null || otpId.trim().isEmpty()) {
            logger.warn("No OTP ID found in session");
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                createErrorResponse("Invalid session", "No OTP session found"));
            return;
        }
        
        // Check eligibility again before processing OTP
        OtpConfig config = new OtpConfig(context.getSession(), context.getRealm());
        EligibilityService eligibilityService = new EligibilityService(config, logger);
        EligibilityResponse eligibility = eligibilityService.checkEligibility(email);
        if (!eligibility.isEnabled()) {
            logger.info("User not eligible for OTP (action): " + email);
            // Ensure user is set in context and complete authentication
            UserModel user = context.getSession().users().getUserById(context.getRealm(), userId);
            if (user != null) {
                context.setUser(user);
                context.success(); // Complete authentication successfully
            } else {
                logger.warn("User not found for OTP action: " + userId);
                context.attempted(); // Skip step if user not found
            }
            return;
        }
        
        // Handle resend OTP request
        if ("true".equals(resendOtp)) {
            logger.info("Resending OTP for user: " + email);
            OtpService otpService = new OtpService(context.getSession(), context.getRealm());
            OtpRequest request = new OtpRequest(
                email,
                userId,
                authSession.getParentSession().getId(),
                config.getOtpLength(),
                config.getOtpTtl(),
                null
            );
            OtpResponse response = otpService.generateOtp(request);
            if (response.isSuccess()) {
                // Update OTP ID in session
                authSession.setAuthNote("otp_id", response.getOtpId());
                logger.info("OTP resent successfully");
                context.challenge(createOtpChallenge(context, "OTP resent successfully"));
            } else {
                logger.error("Failed to resend OTP: " + response.getError());
                context.challenge(createOtpChallenge(context, "Failed to resend OTP: " + response.getError()));
            }
            return;
        }
        
        // Validate OTP input
        if (otpInput == null || otpInput.trim().isEmpty()) {
            logger.warn("No OTP input provided");
            context.challenge(createOtpChallenge(context, "Please enter the OTP code"));
            return;
        }
        
        // Validate OTP
        OtpService otpService = new OtpService(context.getSession(), context.getRealm());
        OtpService.OtpValidationResult result = otpService.validateOtp(otpId, otpInput.trim());
        
        if (result.isValid()) {
            // OTP is valid, authenticate user
            UserModel user = context.getSession().users().getUserById(context.getRealm(), userId);
            if (user != null) {
                context.setUser(user);
                
                // Clear OTP session data
                authSession.removeAuthNote("otp_id");
                authSession.removeAuthNote("otp_email");
                authSession.removeAuthNote("otp_user_id");
                
                logger.info("OTP authentication successful for user: " + email);
                context.success();
                
            } else {
                logger.error("User not found after OTP validation: " + userId);
                context.failure(AuthenticationFlowError.INVALID_USER, 
                    createErrorResponse("User not found", "User not found after OTP validation"));
            }
            
        } else {
            // OTP is invalid
            logger.warn("Invalid OTP input for user: " + email);
            context.challenge(createOtpChallenge(context, "Invalid OTP code. Please try again."));
        }
    }
    
    @Override
    public boolean requiresUser() {
        return true; // We need the user to check eligibility and get email
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
     * Check if this is a direct grant flow (no web interface)
     * @param context Authentication flow context
     * @return true if this is a direct grant flow
     */
    private boolean isDirectGrantFlow(AuthenticationFlowContext context) {
        try {
            // Check if there's an HTTP request (browser flows have HTTP requests)
            if (context.getHttpRequest() == null) {
                return true; // No HTTP request means direct grant
            }
            
            // Check the authentication session for direct grant indicators
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            if (authSession != null) {
                String clientId = authSession.getClientNote("client_id");
                String grantType = authSession.getClientNote("grant_type");
                
                // Direct grant flows typically use "password" grant type
                if ("password".equals(grantType)) {
                    return true;
                }
                
                // Check if this is a token endpoint request
                String requestUri = context.getHttpRequest().getUri().getPath();
                if (requestUri != null && requestUri.contains("/protocol/openid-connect/token")) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error detecting direct grant flow, assuming browser flow", e);
            return false;
        }
    }
    
    /**
     * Handle direct grant OTP authentication
     * @param context Authentication flow context
     */
    private void handleDirectGrantOtp(AuthenticationFlowContext context) {
        try {
            // Get parameters from request
            String otpInput = null;
            String requestOtp = null;
            if (context.getHttpRequest() != null) {
                MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
                otpInput = formParams.getFirst("otp");
                requestOtp = formParams.getFirst("request_otp");
            }
            
            // Step 1: Handle OTP request
            if ("true".equals(requestOtp)) {
                handleOtpRequest(context);
                return;
            }
            
            // Step 2: Handle OTP validation
            if (otpInput != null && !otpInput.trim().isEmpty()) {
                handleOtpValidation(context, otpInput);
                return;
            }
            
            // If no OTP parameters provided, skip OTP for direct grant
            logger.info("No OTP parameters provided in direct grant, skipping OTP authentication");
            context.success(); // Complete authentication successfully
            
        } catch (Exception e) {
            logger.error("Error handling direct grant OTP authentication", e);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                createErrorResponse("OTP authentication failed", e.getMessage()));
        }
    }
    
    /**
     * Handle OTP request for direct grant
     * @param context Authentication flow context
     */
    private void handleOtpRequest(AuthenticationFlowContext context) {
        try {
            // Get user and email
            UserModel user = context.getUser();
            if (user == null) {
                logger.warn("No user found in direct grant OTP request");
                context.failure(AuthenticationFlowError.INVALID_USER, 
                    createErrorResponse("User not found", "No user found in OTP request"));
                return;
            }
            
            String email = user.getEmail();
            if (email == null || email.trim().isEmpty()) {
                logger.warn("User has no email address for OTP: " + user.getId());
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("Invalid user", "User has no email address"));
                return;
            }
            
            // Check eligibility
            OtpConfig config = new OtpConfig(context.getSession(), context.getRealm());
            EligibilityService eligibilityService = new EligibilityService(config, logger);
            EligibilityResponse eligibility = eligibilityService.checkEligibility(email);
            
            if (!eligibility.isEnabled()) {
                logger.info("User not eligible for OTP in direct grant: " + email);
                context.success(); // Skip OTP step for direct grant
                return;
            }
            
            // Generate and send OTP
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            OtpService otpService = new OtpService(context.getSession(), context.getRealm());
            
            OtpRequest request = new OtpRequest(
                email,
                user.getId(),
                authSession.getParentSession().getId(),
                config.getOtpLength(),
                config.getOtpTtl(),
                null
            );
            
            OtpResponse response = otpService.generateOtp(request);
            
            if (response.isSuccess()) {
                // Store OTP ID in user attributes for validation in Step 2
                // This ensures persistence across direct grant requests
                user.setAttribute("otp_id", List.of(response.getOtpId()));
                user.setAttribute("otp_email", List.of(email));
                user.setAttribute("otp_timestamp", List.of(String.valueOf(System.currentTimeMillis())));
                
                logger.info("OTP sent successfully for direct grant user: " + email);
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("OTP sent successfully", "Please authenticate with the OTP code"));
            } else {
                logger.error("Failed to send OTP for direct grant: " + response.getError());
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("Failed to send OTP", response.getError()));
            }
            
        } catch (Exception e) {
            logger.error("Error handling OTP request for direct grant", e);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                createErrorResponse("OTP request failed", e.getMessage()));
        }
    }
    
    /**
     * Handle OTP validation for direct grant
     * @param context Authentication flow context
     * @param otpInput The OTP code provided by user
     */
    private void handleOtpValidation(AuthenticationFlowContext context, String otpInput) {
        try {
            // Get user and email
            UserModel user = context.getUser();
            if (user == null) {
                logger.warn("No user found in direct grant OTP validation");
                context.failure(AuthenticationFlowError.INVALID_USER, 
                    createErrorResponse("User not found", "No user found in OTP validation"));
                return;
            }
            
            String email = user.getEmail();
            if (email == null || email.trim().isEmpty()) {
                logger.warn("User has no email address for OTP: " + user.getId());
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("Invalid user", "User has no email address"));
                return;
            }
            
            // Check eligibility
            OtpConfig config = new OtpConfig(context.getSession(), context.getRealm());
            EligibilityService eligibilityService = new EligibilityService(config, logger);
            EligibilityResponse eligibility = eligibilityService.checkEligibility(email);
            
            if (!eligibility.isEnabled()) {
                logger.info("User not eligible for OTP in direct grant: " + email);
                context.success(); // Skip OTP step for direct grant
                return;
            }
            
            // Get the existing OTP ID from user attributes (generated in Step 1)
            String otpId = user.getFirstAttribute("otp_id");
            
            if (otpId == null || otpId.trim().isEmpty()) {
                logger.warn("No OTP ID found in user attributes for direct grant validation");
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("OTP session expired", "Please request a new OTP"));
                return;
            }
            
            // Validate the provided OTP against the existing OTP ID
            OtpService otpService = new OtpService(context.getSession(), context.getRealm());
            OtpService.OtpValidationResult result = otpService.validateOtp(otpId, otpInput.trim());
            
            if (result.isValid()) {
                logger.info("OTP validation successful for direct grant user: " + email);
                context.success(); // Complete authentication successfully
            } else {
                logger.warn("Invalid OTP provided in direct grant for user: " + email);
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                    createErrorResponse("OTP validation failed", "Invalid or missing OTP code"));
            }
            
        } catch (Exception e) {
            logger.error("Error handling OTP validation for direct grant", e);
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, 
                createErrorResponse("OTP validation failed", e.getMessage()));
        }
    }
    
    /**
     * Create OTP challenge response
     * @param context Authentication flow context
     * @return Response with OTP input form
     */
    private Response createOtpChallenge(AuthenticationFlowContext context) {
        return createOtpChallenge(context, null);
    }
    
    /**
     * Create OTP challenge response with message
     * @param context Authentication flow context
     * @param message Optional message to display
     * @return Response with OTP input form
     */
    private Response createOtpChallenge(AuthenticationFlowContext context, String message) {
        String email = context.getAuthenticationSession().getAuthNote("otp_email");
        
        // Generate HTML form directly instead of using FreeMarker template
        // This avoids template dependency issues and provides better control
        String htmlForm = createOtpHtmlForm(
            context.getActionUrl(context.generateAccessCode()).toString(),
            email,
            message
        );
        
        return Response.status(Response.Status.OK)
            .type("text/html; charset=UTF-8")
            .entity(htmlForm)
            .build();
    }
    
    /**
     * Create HTML form for OTP input
     * @param actionUrl Form action URL
     * @param email User's email
     * @param message Optional message
     * @return HTML string
     */
    private String createOtpHtmlForm(String actionUrl, String email, String message) {
        int otpLength = 6; // Number of OTP digits
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        html.append("<title>OTP Verification</title>");
        html.append("<style>");
        html.append("html, body { height: 100%; margin: 0; padding: 0; }");
        html.append("body { min-height: 100vh; display: flex; flex-direction: column; background: linear-gradient(120deg, #f7fafc 60%, #e6f3fa 100%); font-family: 'Inter', Arial, sans-serif; }");
        html.append(".kf-center-wrap { flex: 1; display: flex; align-items: center; justify-content: center; }");
        html.append(".kf-card { background: #fff; border-radius: 14px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); max-width: 340px; width: 100%; padding: 36px 28px 28px 28px; display: flex; flex-direction: column; align-items: center; }");
        html.append(".kf-logo { font-size: 13px; font-weight: 600; color: #222; letter-spacing: 2px; margin-bottom: 8px; }");
        html.append(".kf-title { font-size: 30px; font-weight: 700; letter-spacing: 1px; margin-bottom: 0; background: linear-gradient(90deg, #1dbf73 40%, #3bb3e6 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; text-fill-color: transparent; }");
        html.append(".kf-subtitle { font-size: 15px; color: #222; margin-bottom: 22px; margin-top: 2px; text-align: center; }");
        html.append(".kf-message { width: 100%; padding: 10px 0; margin-bottom: 16px; border-radius: 6px; text-align: center; font-size: 14px; background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }");
        html.append(".kf-email { width: 100%; background: #f7fafc; color: #222; border-radius: 6px; padding: 10px 0; margin-bottom: 18px; text-align: center; font-size: 15px; }");
        html.append("form { width: 100%; display: flex; flex-direction: column; align-items: center; }");
        html.append(".kf-label { width: 100%; text-align: left; font-size: 14px; font-weight: 500; margin-bottom: 8px; color: #222; margin-top: 2px; }");
        html.append(".kf-otp-boxes { display: flex; justify-content: space-between; gap: 10px; width: 100%; margin-bottom: 22px; }");
        html.append(".kf-otp-input { width: 44px; height: 48px; font-size: 24px; text-align: center; border: 1.5px solid #dbe2ea; border-radius: 8px; background: #f9fafb; outline: none; transition: border 0.2s; }");
        html.append(".kf-otp-input:focus { border: 1.5px solid #3bb3e6; background: #fff; }");
        html.append(".kf-btn { width: 100%; padding: 14px 0; background: #3bb3e6; color: #fff; border: none; border-radius: 8px; font-size: 17px; font-weight: 600; cursor: pointer; margin-bottom: 8px; transition: background 0.2s; }");
        html.append(".kf-btn:disabled { background: #dbe2ea; color: #b0b8c1; cursor: not-allowed; }");
        html.append(".kf-btn:not(:disabled):hover { background: #1dbf73; }");
        html.append(".kf-resend { width: 100%; text-align: center; margin-top: 6px; margin-bottom: 0; }");
        html.append(".kf-resend a { color: #3bb3e6; text-decoration: none; font-size: 14px; }");
        html.append(".kf-resend a:hover { text-decoration: underline; }");
        html.append(".kf-footer { width: 100%; text-align: center; margin-top: auto; font-size: 13px; color: #b0b8c1; padding-bottom: 18px; }");
        html.append(".kf-footer a { color: #b0b8c1; text-decoration: none; margin: 0 8px; }");
        html.append(".kf-footer a:hover { text-decoration: underline; }");
        html.append("</style>");
        // Multi-box OTP JS
        html.append("<script>");
        html.append("function kfEnableBtn() {");
        html.append("  var filled = true;");
        html.append("  for (var i = 0; i < " + otpLength + "; i++) {");
        html.append("    var box = document.getElementById(\"otp-\" + i);");
        html.append("    if (!box.value.match(/^[0-9]$/)) { filled = false; break; }");
        html.append("  }");
        html.append("  document.getElementById(\"kf-btn\").disabled = !filled;");
        html.append("}");
        html.append("function kfOtpInput(e, idx) {");
        html.append("  var key = e.key;");
        html.append("  var val = e.target.value;");
        html.append("  if (key === \"Backspace\" || key === \"Delete\") {");
        html.append("    if (val === \"\" && idx > 0) { document.getElementById(\"otp-\" + (idx-1)).focus(); }");
        html.append("    setTimeout(kfEnableBtn, 10);");
        html.append("    return;");
        html.append("  }");
        html.append("  if (val.length > 1) { e.target.value = val.slice(-1); }");
        html.append("  if (val.match(/^[0-9]$/) && idx < " + (otpLength-1) + ") { document.getElementById(\"otp-\" + (idx+1)).focus(); }");
        html.append("  setTimeout(kfEnableBtn, 10);");
        html.append("}");
        html.append("function kfOtpPaste(e) {");
        html.append("  var data = (e.clipboardData || window.clipboardData).getData(\"text\");");
        html.append("  if (data.length === " + otpLength + ") {");
        html.append("    for (var i = 0; i < " + otpLength + "; i++) {");
        html.append("      var box = document.getElementById(\"otp-\" + i);");
        html.append("      box.value = data[i].match(/[0-9]/) ? data[i] : \"\";");
        html.append("    }");
        html.append("    document.getElementById(\"otp-" + (otpLength-1) + "\").focus();");
        html.append("    kfEnableBtn();");
        html.append("    e.preventDefault();");
        html.append("  }");
        html.append("}");
        html.append("function kfOtpSubmit() {");
        html.append("  var otp = \"\";");
        html.append("  for (var i = 0; i < " + otpLength + "; i++) { otp += document.getElementById(\"otp-\" + i).value; }");
        html.append("  document.getElementById(\"otp\").value = otp;");
        html.append("  return true;");
        html.append("}");
        html.append("window.onload = function() {");
        html.append("  for (var i = 0; i < " + otpLength + "; i++) {");
        html.append("    var box = document.getElementById(\"otp-\" + i);");
        html.append("    box.addEventListener(\"input\", function(e) { kfOtpInput(e, parseInt(this.id.split(\"-\")[1])); });");
        html.append("    box.addEventListener(\"keydown\", function(e) { kfOtpInput(e, parseInt(this.id.split(\"-\")[1])); });");
        html.append("    box.addEventListener(\"paste\", kfOtpPaste);");
        html.append("  }");
        html.append("  document.getElementById(\"otp-0\").focus();");
        html.append("  kfEnableBtn();");
        html.append("};");
        html.append("</script>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"kf-center-wrap\">");
        html.append("<div class=\"kf-card\">");
        html.append("<div class=\"kf-logo\">KORN FERRY</div>");
        html.append("<div class=\"kf-title\">TALENT SUITE</div>");
        html.append("<div class=\"kf-subtitle\">Sign in to your account</div>");
        if (message != null && !message.trim().isEmpty()) {
            html.append("<div class=\"kf-message\">").append(message).append("</div>");
        }
        if (email != null && !email.trim().isEmpty()) {
            html.append("<div class=\"kf-email\">").append(email).append("</div>");
        }
        html.append("<form action=\"").append(actionUrl).append("\" method=\"post\" autocomplete=\"off\" onsubmit=\"return kfOtpSubmit();\">");
        html.append("<label class=\"kf-label\" for=\"otp\">Enter OTP Code</label>");
        html.append("<div class=\"kf-otp-boxes\">");
        for (int i = 0; i < otpLength; i++) {
            html.append("<input class=\"kf-otp-input\" type=\"text\" id=\"otp-").append(i).append("\" maxlength=\"1\" inputmode=\"numeric\" pattern=\"[0-9]*\" autocomplete=\"one-time-code\" />");
        }
        html.append("</div>");
        html.append("<input type=\"hidden\" id=\"otp\" name=\"otp\" />");
        html.append("<button id=\"kf-btn\" class=\"kf-btn\" type=\"submit\" disabled>Verify OTP</button>");
        html.append("</form>");
        // Hidden resend form
        html.append("<form id=\"resend-otp-form\" action=\"").append(actionUrl).append("\" method=\"post\" style=\"display:none;\">");
        html.append("<input type=\"hidden\" name=\"resend_otp\" value=\"true\" />");
        html.append("</form>");
        html.append("<div class=\"kf-resend\"><button type=\"button\" style=\"background:none;border:none;color:#3bb3e6;cursor:pointer;font-size:14px;padding:0;\" onclick=\"document.getElementById('resend-otp-form').submit();\">Resend OTP</button></div>");
        html.append("</div>"); // close kf-card
        html.append("</div>"); // close kf-center-wrap
        html.append("<div class=\"kf-footer\">");
        html.append("&copy; Korn Ferry. All rights reserved. <a href=\"https://www.kornferry.com/terms\" target=\"_blank\">Terms</a> <a href=\"https://www.kornferry.com/privacy\" target=\"_blank\">Privacy</a>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }
    
    /**
     * Create error response
     * @param title Error title
     * @param message Error message
     * @return Error response
     */
    private Response createErrorResponse(String title, String message) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("title", title);
        errorData.put("message", message);
        
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(errorData)
            .build();
    }
} 