package micronet.user.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import micronet.user.model.User;
import micronet.user.repository.UserRepository;
import micronet.user.service.CustomUserDetailsService;
import micronet.user.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        logger.info("OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess called");
        
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            
            logger.info("OAuth2User attributes in success handler: {}", attributes.keySet());
            logger.info("OAuth2User getName(): {}", oAuth2User.getName());
            logger.info("OAuth2User email attribute: {}", attributes.get("email"));
            
            // Extract email from attributes, fallback to principal name (CustomOAuth2User uses email as name).
            // Do NOT use "sub" as email because it's provider user id, not an email.
            String email = (String) attributes.get("email");
            if (email == null || email.isEmpty()) {
                email = oAuth2User.getName();
                logger.info("Tried getName() as fallback, got: {}", email);
            }
            if (email != null) {
                email = email.trim().toLowerCase();
            }

            logger.info("Final extracted email: {}", email);
            
            // Validate email
            if (email == null || email.isEmpty() || !email.contains("@")) {
                logger.error("Email is null or empty! Available attributes: {}", attributes.keySet());
                String errorUrl = UriComponentsBuilder.fromUriString("/oauth2/error")
                        .queryParam("message", "Valid email not found in OAuth2 authentication. Available attributes: " + attributes.keySet())
                        .build().toUriString();
                getRedirectStrategy().sendRedirect(request, response, errorUrl);
                return;
            }
            
            // Get user from database (OAuth2UserService should have created it)
            logger.info("Looking for user in database with email: {}", email);
            Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
            
            // If user doesn't exist, create it here (fallback if OAuth2UserService didn't create it)
            if (userOptional.isEmpty()) {
                logger.warn("User not found in database! Creating user now with email: {}", email);
                try {
                    User newUser = new User();
                    newUser.setEmail(email);
                    Object nameObj = attributes.get("name");
                    String name = nameObj != null ? nameObj.toString() : "OAuth2 User";
                    newUser.setName(name);
                    newUser.setAge(25); // Default age
                    newUser.setSex("Unknown"); // Default sex
                    newUser.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis()));
                    newUser.setRole("USER"); // Default role
                    newUser.setStatus("ACTIVE"); // OAuth2 email is already verified by provider
                    newUser = userRepository.save(newUser);
                    userRepository.flush();
                    logger.info("Created new user with email: {}, ID: {}", newUser.getEmail(), newUser.getId());
                    userOptional = Optional.of(newUser);
                } catch (Exception e) {
                    logger.error("Error creating user: {}", e.getMessage(), e);
                    String errorUrl = UriComponentsBuilder.fromUriString("/oauth2/error")
                            .queryParam("message", "Error creating user: " + e.getMessage())
                            .build().toUriString();
                    getRedirectStrategy().sendRedirect(request, response, errorUrl);
                    return;
                }
            } else {
                logger.info("Found existing user with email: {}", email);
            }
            
            User user = userOptional.get();
            logger.info("User found: ID={}, Email={}, Role={}", user.getId(), user.getEmail(), user.getRole());
            
            // Load user details and generate JWT token
            logger.info("Loading user details for JWT token generation");
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            logger.info("Generating JWT token for user: {}", user.getEmail());
            String token = jwtUtil.generateToken(userDetails, user.getRole());
            logger.info("JWT token generated successfully");
            
            // Redirect to React OAuth2 success endpoint which will redirect to React frontend
            String targetUrl = UriComponentsBuilder.fromUriString("/api/oauth2/success")
                    .queryParam("token", token)
                    .queryParam("email", user.getEmail())
                    .queryParam("role", user.getRole())
                    .build().toUriString();
            
            logger.info("Redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            // Handle any errors during token generation
            String errorUrl = UriComponentsBuilder.fromUriString("/oauth2/error")
                    .queryParam("message", "Error during authentication: " + e.getMessage())
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}

