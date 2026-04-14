package micronet.user.service;

import micronet.user.model.User;
import micronet.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persists OAuth2 users in a separate transaction so the row is committed before
 * {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler} runs.
 * Without this, the success handler can run in a new transaction that cannot see an uncommitted insert.
 */
@Service
public class OAuth2AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Creates or updates a Google OAuth2 user and commits immediately (REQUIRES_NEW).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public User upsertGoogleUser(String email, String displayName) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (displayName != null && !displayName.isBlank() && !displayName.equals(user.getName())) {
                user.setName(displayName);
                return userRepository.saveAndFlush(user);
            }
            return user;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(displayName != null && !displayName.isBlank() ? displayName : "OAuth2 User");
        user.setAge(25);
        user.setSex("Unknown");
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
