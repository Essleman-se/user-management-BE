package micronet.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;

    public void sendVerificationEmail(String to, String token) {
        sendVerificationEmail(to, token, frontendUrl);
    }

    public void sendVerificationEmail(String to, String token, String frontendBaseUrl) {
        String resolvedFrontendUrl = (frontendBaseUrl == null || frontendBaseUrl.isBlank())
                ? frontendUrl
                : frontendBaseUrl;
        String verificationUrl = buildVerificationUrl(resolvedFrontendUrl, token);

        if (!mailEnabled || mailSender == null) {
            logger.warn("Email service is disabled or not configured. Verification email would be sent to: {}", to);
            logger.info("Verification link: {}", verificationUrl);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Verify Your Email Address");
            message.setText("Please click the following link to verify your email address:\n\n" 
                    + verificationUrl 
                    + "\n\nThis link will expire in 24 hours.");
            
            mailSender.send(message);
            logger.info("Verification email sent to: {}", to);
        } catch (org.springframework.mail.MailException e) {
            logger.error("Failed to send verification email to: {}. Error: {}", to, e.getMessage());
            // Check if it's an invalid email address error
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("550") || 
                                         errorMessage.contains("invalid") || 
                                         errorMessage.contains("not found") ||
                                         errorMessage.contains("does not exist"))) {
                throw new RuntimeException("Email address not found or invalid", e);
            }
            throw new RuntimeException("Failed to send verification email. Please check your email address and try again.", e);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Email address not found or invalid. Please check your email address and try again.", e);
        }
    }

    /**
     * Sends the verification email in a background thread.
     * Use this for registration/resend to avoid blocking the request thread.
     * Failures are logged only; caller is not notified.
     */
    @Async
    public void sendVerificationEmailAsync(String to, String token) {
        sendVerificationEmailAsync(to, token, frontendUrl);
    }

    @Async
    public void sendVerificationEmailAsync(String to, String token, String frontendBaseUrl) {
        try {
            sendVerificationEmail(to, token, frontendBaseUrl);
        } catch (Exception e) {
            logger.error("Background send of verification email failed for: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    private String buildVerificationUrl(String frontendBaseUrl, String token) {
        String base = normalizeBase(frontendBaseUrl);
        return base + "/verify-email?token=" + token;
    }

    public void sendPasswordResetEmail(String to, String token) {
        sendPasswordResetEmail(to, token, frontendUrl);
    }

    public void sendPasswordResetEmail(String to, String token, String frontendBaseUrl) {
        String resolvedFrontendUrl = (frontendBaseUrl == null || frontendBaseUrl.isBlank())
                ? frontendUrl
                : frontendBaseUrl;
        String resetUrl = buildPasswordResetUrl(resolvedFrontendUrl, token);

        if (!mailEnabled || mailSender == null) {
            logger.warn("Email service is disabled or not configured. Password reset email would be sent to: {}", to);
            logger.info("Password reset link: {}", resetUrl);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Reset your password");
            message.setText("You requested a password reset. Click the link below to choose a new password:\n\n"
                    + resetUrl
                    + "\n\nThis link expires in 1 hour. If you did not request this, you can ignore this email.");
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", to);
        } catch (org.springframework.mail.MailException e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", to, e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("550")
                    || errorMessage.contains("invalid")
                    || errorMessage.contains("not found")
                    || errorMessage.contains("does not exist"))) {
                throw new RuntimeException("Email address not found or invalid", e);
            }
            throw new RuntimeException("Failed to send password reset email. Please try again.", e);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Email address not found or invalid. Please check your email and try again.", e);
        }
    }

    @Async
    public void sendPasswordResetEmailAsync(String to, String token, String frontendBaseUrl) {
        try {
            sendPasswordResetEmail(to, token, frontendBaseUrl);
        } catch (Exception e) {
            logger.error("Background send of password reset email failed for: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    private String buildPasswordResetUrl(String frontendBaseUrl, String token) {
        String base = normalizeBase(frontendBaseUrl);
        return base + "/reset-password?token=" + token;
    }

    private static String normalizeBase(String frontendBaseUrl) {
        return frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }
}

