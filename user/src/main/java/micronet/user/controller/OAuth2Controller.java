package micronet.user.controller;

import micronet.user.dto.AuthResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @GetMapping("/success")
    public ResponseEntity<AuthResponseDTO> oauth2Success(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam String role) {
        
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setEmail(email);
        response.setRole(role);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/login")
    public ResponseEntity<String> oauth2Login() {
        return ResponseEntity.ok("""
            <html>
            <body>
                <h2>OAuth2 Login</h2>
                <p>Choose a provider:</p>
                <a href="/oauth2/authorization/google">Login with Google</a>
            </body>
            </html>
            """);
    }

    @GetMapping("/error")
    public ResponseEntity<String> oauth2Error(@RequestParam(required = false) String message) {
        String errorMessage = message != null ? message : "An error occurred during OAuth2 authentication";
        return ResponseEntity.ok("""
            <html>
            <body>
                <h2>OAuth2 Authentication Error</h2>
                <p>%s</p>
                <a href="/oauth2/login">Try again</a>
            </body>
            </html>
            """.formatted(errorMessage));
    }
}

