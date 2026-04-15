package micronet.user.controller;

import micronet.user.dto.AuthResponseDTO;
import micronet.user.dto.ErrorResponseDTO;
import micronet.user.oauth2.OAuth2FrontendReturnUrlSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = "*")
public class OAuth2ReactController {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2FrontendReturnUrlSupport frontendReturnUrlSupport;

    /**
     * GET /api/oauth2/authorization-url/{provider}
     * Returns the path to start OAuth2. Includes return_url so the backend can redirect back to the same frontend (prod vs local).
     * Send Origin (browser) or call from the SPA so Origin matches the deployed site.
     */
    @GetMapping("/authorization-url/{provider}")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(
            @PathVariable String provider,
            HttpServletRequest request) {

        try {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider.toLowerCase());

            if (registration == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Provider not found");
                error.put("message", "Supported providers: google");
                return ResponseEntity.badRequest().body(error);
            }

            String returnBase = frontendReturnUrlSupport.resolveFrontendBaseFromRequest(request);
            String authorizationUrl = frontendReturnUrlSupport.buildOAuth2AuthorizationUrlWithReturnUrl(
                    provider.toLowerCase(), returnBase);

            String portPart = (request.getServerPort() == 80 || request.getServerPort() == 443)
                    ? ""
                    : ":" + request.getServerPort();
            String apiOrigin = request.getScheme() + "://" + request.getServerName() + portPart;

            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", authorizationUrl);
            response.put("authorizationUrlFull", apiOrigin + authorizationUrl);
            response.put("returnUrl", returnBase);
            response.put("provider", provider);
            response.put("message", "Redirect browser to authorizationUrlFull (or apiOrigin + authorizationUrl). return_url selects prod vs local callback.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate authorization URL");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> providers = new HashMap<>();

        ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");
        if (google != null) {
            Map<String, String> googleInfo = new HashMap<>();
            googleInfo.put("name", "Google");
            googleInfo.put("registrationId", "google");
            googleInfo.put("authorizationUrl", "/api/oauth2/authorization-url/google");
            providers.put("google", googleInfo);
        }

        return ResponseEntity.ok(providers);
    }

    /**
     * GET /api/oauth2/success
     * Redirects to frontend /oauth2/callback using cookie set at OAuth2 start, or default app.frontend.url.
     */
    @GetMapping("/success")
    public ResponseEntity<Void> oauth2Success(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam(required = false) String role,
            HttpServletRequest request,
            HttpServletResponse response) {

        String base = frontendReturnUrlSupport.readReturnUrlCookie(request);
        if (base == null || !frontendReturnUrlSupport.isAllowedBase(base)) {
            base = frontendReturnUrlSupport.defaultFrontendBase();
        }

        String location = frontendReturnUrlSupport.buildFrontendCallbackUrl(base, token, email, role);
        frontendReturnUrlSupport.clearReturnUrlCookie(response);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", location)
                .build();
    }

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

        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setToken(token);
        authResponse.setEmail(email);
        authResponse.setRole(role != null ? role : "USER");

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");

        Map<String, Object> response = new HashMap<>();

        if (token == null || token.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("valid", true);
        response.put("message", "Token verification endpoint - implement JWT validation here");

        return ResponseEntity.ok(response);
    }
}
