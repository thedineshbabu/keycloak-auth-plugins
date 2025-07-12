package kf.keycloak.plugin.service;

import io.jsonwebtoken.*;
import kf.keycloak.plugin.model.MagiclinkRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT token service for magiclink authentication
 * Handles token generation, validation, and lifecycle management
 */
public class TokenService {
    
    // Token claim keys
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_REDIRECT_URL = "redirectUrl";
    private static final String CLAIM_CLIENT_ID = "clientId";
    private static final String CLAIM_REALM = "realm";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_ONE_TIME_USE = "oneTimeUse";
    
    private static final String TOKEN_TYPE_MAGICLINK = "magiclink";
    
    // Cache for used tokens to prevent replay attacks
    private static final Map<String, LocalDateTime> usedTokens = new ConcurrentHashMap<>();
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final String secretKey;
    
    /**
     * Constructor with realm-specific configuration
     * @param session Keycloak session
     * @param realm Realm model
     */
    public TokenService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        
        // Generate secret key based on realm
        this.secretKey = generateSecretKey(realm.getName());
    }
    
    /**
     * Generate a magiclink token for the specified user
     * @param user User model
     * @param request Magiclink request
     * @return Generated token string
     */
    public String generateMagiclinkToken(UserModel user, MagiclinkRequest request) {
        try {
            // Generate unique token ID
            String tokenId = UUID.randomUUID().toString();
            
            // Calculate expiration
            int expirationMinutes = request.getExpirationMinutes() != null ? 
                request.getExpirationMinutes() : 15;
            
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
            Date expirationDate = Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant());
            
            // Build JWT claims
            Map<String, Object> claims = new HashMap<>();
            claims.put(CLAIM_USER_ID, user.getId());
            claims.put(CLAIM_EMAIL, user.getEmail());
            claims.put(CLAIM_REDIRECT_URL, request.getRedirectUrl());
            claims.put(CLAIM_CLIENT_ID, request.getClientId());
            claims.put(CLAIM_REALM, realm.getName());
            claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_MAGICLINK);
            claims.put(CLAIM_ONE_TIME_USE, true);
            
            // Generate JWT token using JJWT 0.9.1 API
            String token = Jwts.builder()
                .setClaims(claims)
                .setId(tokenId)
                .setIssuer("keycloak-magiclink-" + realm.getName())
                .setSubject(user.getId())
                .setAudience(realm.getName())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
            
            return token;
            
        } catch (Exception e) {
            throw new TokenGenerationException("Failed to generate magiclink token", e);
        }
    }
    
    /**
     * Validate and parse a magiclink token
     * @param token JWT token string
     * @return TokenValidationResult with validation status and claims
     */
    public TokenValidationResult validateMagiclinkToken(String token) {
        try {
            // Parse and validate JWT using JJWT 0.9.1 API
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes())
                .requireIssuer("keycloak-magiclink-" + realm.getName())
                .requireAudience(realm.getName())
                .parseClaimsJws(token)
                .getBody();
            
            String tokenId = claims.getId();
            
            // Check token type
            String tokenType = (String) claims.get(CLAIM_TOKEN_TYPE);
            if (!TOKEN_TYPE_MAGICLINK.equals(tokenType)) {
                return TokenValidationResult.invalid("Invalid token type");
            }
            
            // Check one-time use
            Boolean oneTimeUse = (Boolean) claims.get(CLAIM_ONE_TIME_USE);
            if (Boolean.TRUE.equals(oneTimeUse) && isTokenUsed(tokenId)) {
                return TokenValidationResult.invalid("Token already used");
            }
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                return TokenValidationResult.invalid("Token expired");
            }
            
            // Extract user information
            String userId = (String) claims.get(CLAIM_USER_ID);
            String email = (String) claims.get(CLAIM_EMAIL);
            String redirectUrl = (String) claims.get(CLAIM_REDIRECT_URL);
            String clientId = (String) claims.get(CLAIM_CLIENT_ID);
            
            // Validate user still exists
            UserModel user = session.users().getUserById(realm, userId);
            if (user == null) {
                return TokenValidationResult.invalid("User not found");
            }
            
            // Mark token as used if it's one-time use
            if (Boolean.TRUE.equals(oneTimeUse)) {
                markTokenAsUsed(tokenId);
            }
            
            return TokenValidationResult.valid(tokenId, userId, email, redirectUrl, clientId, user);
            
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.invalid("Token expired");
        } catch (SignatureException e) {
            return TokenValidationResult.invalid("Invalid token signature");
        } catch (JwtException e) {
            return TokenValidationResult.invalid("Invalid token");
        } catch (Exception e) {
            return TokenValidationResult.invalid("Token validation failed");
        }
    }
    
    /**
     * Generate a complete magiclink URL with token
     * @param user User model
     * @param request Magiclink request
     * @param baseUrl Base URL for magiclink construction
     * @return Complete magiclink URL
     */
    public String generateMagiclinkUrl(UserModel user, MagiclinkRequest request, String baseUrl) {
        try {
            // Generate token
            String token = generateMagiclinkToken(user, request);
            
            // Build magiclink URL that goes through Keycloak authentication flow
            // This will create a proper Keycloak session that the React app can detect
            String clientId = request.getClientId() != null ? request.getClientId() : "magiclink-test-app";
            String redirectUri = request.getRedirectUrl() != null ? request.getRedirectUrl() : "http://localhost:3000/dashboard";
            
            String magiclinkUrl = baseUrl + "/realms/" + realm.getName() + 
                "/protocol/openid-connect/auth" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=openid" +
                "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            
            return magiclinkUrl;
            
        } catch (Exception e) {
            throw new TokenGenerationException("Failed to generate magiclink URL", e);
        }
    }
    
    /**
     * Extract token ID from JWT token without full validation
     * @param token JWT token string
     * @return Token ID or null if extraction fails
     */
    public String extractTokenId(String token) {
        try {
            // Parse without verification to extract ID
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // Simple JSON parsing to extract jti (JWT ID)
            String jtiPattern = "\"jti\":\"";
            int jtiIndex = payload.indexOf(jtiPattern);
            if (jtiIndex != -1) {
                int startIndex = jtiIndex + jtiPattern.length();
                int endIndex = payload.indexOf('"', startIndex);
                if (endIndex != -1) {
                    return payload.substring(startIndex, endIndex);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if a token has been used (for one-time use tokens)
     * @param tokenId Token ID
     * @return true if token has been used
     */
    public boolean isTokenUsed(String tokenId) {
        return usedTokens.containsKey(tokenId);
    }
    
    /**
     * Mark a token as used (for one-time use tokens)
     * @param tokenId Token ID
     */
    public void markTokenAsUsed(String tokenId) {
        usedTokens.put(tokenId, LocalDateTime.now());
    }
    
    /**
     * Generate a secret key based on realm information
     * @param realmName Realm name
     * @return Generated secret key
     */
    private String generateSecretKey(String realmName) {
        // Use realm name and a fixed salt to generate a consistent key
        String baseString = "magiclink-" + realmName + "-secret-key-v1";
        return java.util.Base64.getEncoder().encodeToString(baseString.getBytes());
    }
    
    /**
     * Clean up expired used tokens from memory
     * Should be called periodically to prevent memory leaks
     */
    public static void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // Keep for 24 hours
        usedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
    
    /**
     * Token validation result container
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String error;
        private final String tokenId;
        private final String userId;
        private final String email;
        private final String redirectUrl;
        private final String clientId;
        private final UserModel user;
        
        private TokenValidationResult(boolean valid, String error, String tokenId, 
                                     String userId, String email, String redirectUrl, 
                                     String clientId, UserModel user) {
            this.valid = valid;
            this.error = error;
            this.tokenId = tokenId;
            this.userId = userId;
            this.email = email;
            this.redirectUrl = redirectUrl;
            this.clientId = clientId;
            this.user = user;
        }
        
        public static TokenValidationResult valid(String tokenId, String userId, String email, 
                                                String redirectUrl, String clientId, UserModel user) {
            return new TokenValidationResult(true, null, tokenId, userId, email, redirectUrl, clientId, user);
        }
        
        public static TokenValidationResult invalid(String error) {
            return new TokenValidationResult(false, error, null, null, null, null, null, null);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getError() { return error; }
        public String getTokenId() { return tokenId; }
        public String getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getRedirectUrl() { return redirectUrl; }
        public String getClientId() { return clientId; }
        public UserModel getUser() { return user; }
    }
    
    /**
     * Custom exception for token generation errors
     */
    public static class TokenGenerationException extends RuntimeException {
        public TokenGenerationException(String message) {
            super(message);
        }
        
        public TokenGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 