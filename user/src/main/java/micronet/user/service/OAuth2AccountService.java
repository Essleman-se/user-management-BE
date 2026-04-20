package micronet.user.service;

import micronet.user.model.User;
import micronet.user.repository.UserRepository;
import micronet.user.util.UserProfileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Persists OAuth2 users in a separate transaction so the row is committed before
 * {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler} runs.
 */
@Service
public class OAuth2AccountService {

    private static final String OAUTH2_PLACEHOLDER_PHONE = "0000000000";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public User upsertGoogleUser(String email, String displayName) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (displayName != null && !displayName.isBlank()) {
                User temp = new User();
                UserProfileUtils.applyDisplayName(temp, displayName);
                if (!temp.getFirstName().equals(user.getFirstName())
                        || !Objects.equals(temp.getLastName(), user.getLastName())) {
                    user.setFirstName(temp.getFirstName());
                    user.setLastName(temp.getLastName() != null ? temp.getLastName() : "");
                    return userRepository.saveAndFlush(user);
                }
            }
            return user;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        UserProfileUtils.applyDisplayName(user, displayName != null && !displayName.isBlank() ? displayName : null);
        user.setPhone(OAUTH2_PLACEHOLDER_PHONE);
        user.setPassword(passwordEncoder.encode("OAUTH2_USER_" + System.currentTimeMillis()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        return userRepository.saveAndFlush(user);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
