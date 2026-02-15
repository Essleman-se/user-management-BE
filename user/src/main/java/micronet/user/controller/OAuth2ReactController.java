package micronet.user.controller;

import micronet.user.dto.AuthResponseDTO;
import micronet.user.dto.ErrorResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = "*") // Allow CORS for React frontend
public class OAuth2ReactController {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * GET /api/oauth2/authorization-url/{provider}
     * Returns the OAuth2 authorization URL for the specified provider
     * React frontend should redirect user to this URL
     * 
     * Note: This uses Spring Security's built-in OAuth2 authorization endpoint
     * which handles the OAuth2 flow properly
     */
    @GetMapping("/authorization-url/{provider}")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(@PathVariable String provider) {
        
        try {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider.toLowerCase());
            
            if (registration == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Provider not found");
                error.put("message", "Supported providers: google, github");
                return ResponseEntity.badRequest().body(error);
            }

            // Use Spring Security's OAuth2 authorization endpoint
            // This is the standard endpoint that Spring Security provides
            String authorizationUrl = "/oauth2/authorization/" + provider.toLowerCase();

            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", authorizationUrl);
            response.put("provider", provider);
            response.put("message", "Redirect user to this URL to start OAuth2 flow");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate authorization URL");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/oauth2/providers
     * Returns list of available OAuth2 providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> providers = new HashMap<>();
        
        ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");
        ClientRegistration github = clientRegistrationRepository.findByRegistrationId("github");
        
        if (google != null) {
            Map<String, String> googleInfo = new HashMap<>();
            googleInfo.put("name", "Google");
            googleInfo.put("registrationId", "google");
            googleInfo.put("authorizationUrl", "/api/oauth2/authorization-url/google");
            providers.put("google", googleInfo);
        }
        
        if (github != null) {
            Map<String, String> githubInfo = new HashMap<>();
            githubInfo.put("name", "GitHub");
            githubInfo.put("registrationId", "github");
            githubInfo.put("authorizationUrl", "/api/oauth2/authorization-url/github");
            providers.put("github", githubInfo);
        }
        
        return ResponseEntity.ok(providers);
    }

    /**
     * GET /api/oauth2/success
     * This endpoint is called after successful OAuth2 authentication
     * It redirects to the React frontend with token and email as query parameters
     * This endpoint should be used by OAuth2AuthenticationSuccessHandler to redirect to React app
     */
    @GetMapping("/success")
    public ResponseEntity<Void> oauth2Success(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam(required = false) String role) {
        
        // Build the React frontend callback URL with token and email
        String frontendUrl = UriComponentsBuilder
                //.fromUriString("http://localhost:5173/oauth2/callback")
                .fromUriString("https://essleman-se.github.io/user-management-UI/oauth2/callback")
                .queryParam("token", token)
                .queryParam("email", email)
                .queryParamIfPresent("role", java.util.Optional.ofNullable(role))
                .build()
                .toUriString();
        
        // Return redirect response to React frontend
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", frontendUrl)
                .build();
    }

    /**
     * GET /api/oauth2/callback
     * This endpoint is called after OAuth2 authentication
     * It extracts token from query parameters and returns JSON response
     * Note: This is a fallback endpoint. The actual callback is handled by Spring Security
     * and redirected to /oauth2/success which can be consumed by React
     */
    @GetMapping("/callback")
    public ResponseEntity<?> oauth2Callback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {
        
        if (error != null) {
            ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                    HttpStatus.BAD_REQUEST.value(),
                    "OAuth2 Authentication Error",
                    error_description != null ? error_description : error,
                    "/api/oauth2/callback"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (token == null || token.isEmpty()) {
            ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                    HttpStatus.BAD_REQUEST.value(),
                    "Missing Token",
                    "Authentication token was not provided",
                    "/api/oauth2/callback"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setEmail(email);
        response.setRole(role != null ? role : "USER");
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/oauth2/verify-token
     * Verify if a token is valid (optional endpoint for React to verify tokens)
     */
    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        Map<String, Object> response = new HashMap<>();
        
        if (token == null || token.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Token validation would be done here using JwtUtil
        // For now, just return a placeholder response
        response.put("valid", true);
        response.put("message", "Token verification endpoint - implement JWT validation here");
        
        return ResponseEntity.ok(response);
    }

}

