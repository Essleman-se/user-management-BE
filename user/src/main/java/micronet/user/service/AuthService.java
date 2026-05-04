package micronet.user.service;

import micronet.user.dto.AuthResponseDTO;
import micronet.user.dto.LoginChallengeResponseDTO;
import micronet.user.dto.LoginRequestDTO;
import micronet.user.dto.RegisterRequestDTO;
import micronet.user.dto.ResetPasswordRequestDTO;
import micronet.user.dto.VerifyLoginCodeRequestDTO;
import micronet.user.exception.ResourceNotFoundException;
import micronet.user.model.LoginVerificationCode;
import micronet.user.model.PasswordResetToken;
import micronet.user.model.User;
import micronet.user.model.VerificationToken;
import micronet.user.repository.LoginVerificationCodeRepository;
import micronet.user.repository.PasswordResetTokenRepository;
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

import java.security.SecureRandom;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private LoginVerificationCodeRepository loginVerificationCodeRepository;

    @Autowired
    private EmailService emailService;

    private static final int LOGIN_CODE_EXPIRY_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthResponseDTO register(RegisterRequestDTO registerRequest, String frontendBaseUrl) {
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Password and re-enter password do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(registerRequest.getFirstName().trim());
        user.setLastName(registerRequest.getLastName().trim());
        user.setEmail(registerRequest.getEmail().trim());
        user.setPhone(registerRequest.getPhone().trim());
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

    public LoginChallengeResponseDTO login(LoginRequestDTO loginRequest) {
        // Get user first to check status
        User user = userRepository.findByEmailIgnoreCase(loginRequest.getEmail())
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
                        user.getEmail(),
                        loginRequest.getPassword()
                )
        );

        loginVerificationCodeRepository.deleteByUserId(user.getId());
        String code = generateLoginCode();
        String channel = normalizeChannel(loginRequest.getChannel());
        LoginVerificationCode verificationCode = new LoginVerificationCode(code, user, channel, LOGIN_CODE_EXPIRY_MINUTES);
        loginVerificationCodeRepository.save(verificationCode);

        if ("PHONE".equals(channel)) {
            emailService.sendLoginVerificationCodeToPhone(user.getPhone(), code);
        } else {
            emailService.sendLoginVerificationCodeEmailAsync(user.getEmail(), code);
        }

        return new LoginChallengeResponseDTO(true, channel, "Verification code sent. Please verify to complete login.");
    }

    public AuthResponseDTO verifyLoginCode(VerifyLoginCodeRequestDTO request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoginVerificationCode latestCode = loginVerificationCodeRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> new RuntimeException("No login verification code found. Please login again."));

        if (latestCode.isUsed()) {
            throw new RuntimeException("This login verification code has already been used");
        }

        if (latestCode.isExpired()) {
            throw new RuntimeException("This login verification code has expired");
        }

        if (!latestCode.getCode().equals(request.getCode().trim())) {
            throw new RuntimeException("Invalid login verification code");
        }

        latestCode.setUsed(true);
        loginVerificationCodeRepository.save(latestCode);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole());
        return new AuthResponseDTO(token, user.getEmail(), user.getRole());
    }

    public void verifyEmail(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        VerificationToken verificationToken = verificationTokenRepository.findByToken(normalizedToken)
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

    /**
     * Issues a reset link only for ACTIVE accounts. Always behaves as success to avoid email enumeration.
     */
    public void forgotPassword(String email, String frontendBaseUrl) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.isEmpty()) {
            return;
        }

        userRepository.findByEmailIgnoreCase(normalized).ifPresent(user -> {
            if (!"ACTIVE".equals(user.getStatus())) {
                return;
            }
            passwordResetTokenRepository.deleteByUserId(user.getId());
            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(rawToken, user);
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmailAsync(user.getEmail(), rawToken, frontendBaseUrl);
        });
    }

    public void resetPassword(ResetPasswordRequestDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password and re-enter password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset link"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("This password reset link has already been used");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("This password reset link has expired");
        }

        User user = resetToken.getUser();
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active; you cannot reset the password");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private String generateLoginCode() {
        int number = 100000 + SECURE_RANDOM.nextInt(900000);
        return Integer.toString(number);
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "EMAIL";
        }
        String normalized = channel.trim().toUpperCase();
        if (!"EMAIL".equals(normalized) && !"PHONE".equals(normalized)) {
            throw new RuntimeException("Channel must be EMAIL or PHONE");
        }
        return normalized;
    }
}

