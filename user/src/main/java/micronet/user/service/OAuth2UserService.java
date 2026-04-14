package micronet.user.service;

import micronet.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserService.class);

    @Autowired
    private OAuth2AccountService oAuth2AccountService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("OAuth2UserService.loadUser called for registration: {}", userRequest.getClientRegistration().getRegistrationId());
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // Log attributes for debugging
        logger.info("OAuth2 Attributes for {}: {}", registrationId, attributes.keySet());
        logger.info("OAuth2 Attributes values: {}", attributes);
        logger.info("OAuth2 Email attribute: {}", attributes.get("email"));
        logger.info("OAuth2 Sub attribute: {}", attributes.get("sub"));
        logger.info("OAuth2 Name attribute: {}", attributes.get("name"));
        
        String email = extractEmail(oAuth2User, registrationId);
        String name = extractName(oAuth2User, registrationId);
        
        logger.info("Extracted email: {}", email);
        logger.info("Extracted name: {}", name);
        
        // Validate email
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException(
                "Email not found in OAuth2 user attributes. Registration: " + registrationId + 
                ", Available keys: " + attributes.keySet() + 
                ", All attributes: " + attributes);
        }

        // Persist in a new transaction so the row is committed before the success handler runs
        User user = oAuth2AccountService.upsertGoogleUser(email, name);
        
        // Return OAuth2User with authorities (use persisted email so it matches DB)
        return new CustomOAuth2User(
                oAuth2User.getAttributes(),
                oAuth2User.getAuthorities(),
                user.getEmail(),
                user.getRole()
        );
    }

    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if ("google".equals(registrationId)) {
            // Google returns email in "email" field
            Object emailObj = attributes.get("email");
            String email = null;
            
            if (emailObj != null) {
                email = emailObj.toString();
            }
            
            // If email is still null/empty, check for verified_email (legacy field)
            if ((email == null || email.isEmpty()) && attributes.containsKey("verified_email")) {
                Object verifiedEmailObj = attributes.get("verified_email");
                if (verifiedEmailObj != null) {
                    email = verifiedEmailObj.toString();
                }
            }
            
            // Final validation - email must contain @ to be valid
            if (email == null || email.isEmpty() || !email.contains("@")) {
                throw new OAuth2AuthenticationException(
                    "Email not found in Google OAuth2 attributes. " +
                    "Available keys: " + attributes.keySet() + ". " +
                    "Make sure 'email' scope is requested and user has granted access."
                );
            }
            return email.trim().toLowerCase();
        }
        
        // Default: try email field
        String email = (String) attributes.get("email");
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 attributes: " + attributes.keySet());
        }
        return email.trim().toLowerCase();
    }

    private String extractName(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        }
        
        return (String) attributes.get("name");
    }

    // Custom OAuth2User implementation to include role
    private static class CustomOAuth2User implements OAuth2User {
        private final Map<String, Object> attributes;
        private final java.util.Collection<SimpleGrantedAuthority> authorities;
        private final String email;
        private final String role;

        public CustomOAuth2User(Map<String, Object> attributes,
                               java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
                               String email, String role) {
            this.attributes = attributes;
            this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
            this.email = email;
            this.role = role;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getName() {
            return email;
        }

        public String getRole() {
            return role;
        }
    }
}

