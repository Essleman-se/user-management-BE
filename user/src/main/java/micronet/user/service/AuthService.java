package micronet.user.service;

import micronet.user.dto.AuthResponseDTO;
import micronet.user.dto.LoginRequestDTO;
import micronet.user.dto.RegisterRequestDTO;
import micronet.user.exception.ResourceNotFoundException;
import micronet.user.model.User;
import micronet.user.model.VerificationToken;
import micronet.user.repository.UserRepository;
import micronet.user.repository.VerificationTokenRepository;
import micronet.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private EmailService emailService;

    public AuthResponseDTO register(RegisterRequestDTO registerRequest, String frontendBaseUrl) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user with PENDING status (but don't save yet)
        User user = new User();
        user.setName(registerRequest.getName());
        user.setAge(registerRequest.getAge());
        user.setSex(registerRequest.getSex());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : "USER");
        user.setStatus("PENDING"); // Set status to PENDING until email is verified

        // Generate verification token and save user + token first
        String verificationToken = UUID.randomUUID().toString();
        User savedUser = userRepository.save(user);
        VerificationToken token = new VerificationToken(verificationToken, savedUser);
        verificationTokenRepository.save(token);

        // Send verification email asynchronously (non-blocking)
        emailService.sendVerificationEmailAsync(savedUser.getEmail(), verificationToken, frontendBaseUrl);

        // Return response without JWT token (user needs to verify email first)
        // Token will be null, indicating user needs to verify
        return new AuthResponseDTO(null, savedUser.getEmail(), savedUser.getRole());
    }

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        // Get user first to check status
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user account is active
        if (!"ACTIVE".equals(user.getStatus())) {
            if ("PENDING".equals(user.getStatus())) {
                throw new RuntimeException("Please verify your email address before logging in");
            } else if ("SUSPENDED".equals(user.getStatus())) {
                throw new RuntimeException("Your account has been suspended. Please contact support");
            } else {
                throw new RuntimeException("Your account is not active");
            }
        }

        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

        // Generate JWT token
        String token = jwtUtil.generateToken(userDetails, user.getRole());

        return new AuthResponseDTO(token, user.getEmail(), user.getRole());
    }

    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new RuntimeException("Verification token has already been used");
        }

        if (verificationToken.isExpired()) {
            throw new RuntimeException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setStatus("ACTIVE");
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    public void resendVerificationEmail(String email, String frontendBaseUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email address not found"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Email is already verified");
        }

        // Delete old tokens for this user
        verificationTokenRepository.deleteByUserEmail(email);

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken(verificationToken, user);
        verificationTokenRepository.save(token);

        // Send verification email asynchronously
        emailService.sendVerificationEmailAsync(user.getEmail(), verificationToken, frontendBaseUrl);
    }
}

