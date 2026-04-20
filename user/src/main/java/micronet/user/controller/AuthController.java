package micronet.user.controller;

import micronet.user.dto.AuthResponseDTO;
import micronet.user.dto.LoginRequestDTO;
import micronet.user.dto.RegisterRequestDTO;
import micronet.user.exception.ResourceNotFoundException;
import micronet.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest, HttpServletRequest request) {
        System.out.println("Register request: " + registerRequest);
        try {
            String frontendBaseUrl = resolveFrontendBaseUrl(request);
            AuthResponseDTO response = authService.register(registerRequest, frontendBaseUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // Check if it's an email sending failure
            if (e.getMessage() != null && e.getMessage().contains("Email address not found")) {
                Map<String, String> errorResponse = new java.util.HashMap<>();
                errorResponse.put("error", "Email address not found or invalid. Please check your email address and try again.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            // Other runtime exceptions (like email already exists)
            Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Email verified successfully. You can now log in.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new java.util.HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(@RequestParam String email, HttpServletRequest request) {
        try {
            String frontendBaseUrl = resolveFrontendBaseUrl(request);
            authService.resendVerificationEmail(email, frontendBaseUrl);
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Verification email sent successfully");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            Map<String, String> response = new java.util.HashMap<>();
            response.put("error", "Email address not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new java.util.HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin.endsWith("/") ? origin + "user-management-UI" : origin + "/user-management-UI";
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            int schemeIdx = referer.indexOf("://");
            if (schemeIdx > 0) {
                int hostEnd = referer.indexOf('/', schemeIdx + 3);
                String base = hostEnd > 0 ? referer.substring(0, hostEnd) : referer;
                return base.endsWith("/") ? base + "user-management-UI" : base + "/user-management-UI";
            }
        }

        // Fallback handled in EmailService via app.frontend.url when null/blank
        return null;
    }
}

