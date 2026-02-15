package micronet.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @GetMapping
    public ResponseEntity<String> login(@RequestParam(required = false) String error) {
        String errorMessage = "";
        if (error != null) {
            errorMessage = "<p style='color: red;'>Error: " + error + "</p>";
        }
        
        return ResponseEntity.ok("""
            <html>
            <head>
                <title>Login</title>
            </head>
            <body>
                <h2>Login</h2>
                %s
                <p>Choose a provider:</p>
                <a href="/oauth2/authorization/google">Login with Google</a><br/><br/>
                <a href="/oauth2/authorization/github">Login with GitHub</a><br/><br/>
                <a href="/oauth2/login">OAuth2 Login Page</a>
            </body>
            </html>
            """.formatted(errorMessage));
    }
}

