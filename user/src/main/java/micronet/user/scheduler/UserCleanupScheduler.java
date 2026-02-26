package micronet.user.scheduler;

import micronet.user.model.User;
import micronet.user.repository.UserRepository;
import micronet.user.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that removes users who registered but did not verify their email
 * within one hour. Runs every 15 minutes.
 */
@Component
public class UserCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UserCleanupScheduler.class);

    private static final int UNVERIFIED_USER_RETENTION_HOURS = 1;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Scheduled(fixedRate = 15 * 60 * 1000) // Every 15 minutes
    @Transactional
    public void cleanupUnverifiedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(UNVERIFIED_USER_RETENTION_HOURS);
        List<User> toRemove = userRepository.findByStatusAndCreatedAtBefore("PENDING", cutoff);

        if (toRemove.isEmpty()) {
            logger.debug("No unverified users to clean up (cutoff: {})", cutoff);
            return;
        }

        int deleted = 0;
        for (User user : toRemove) {
            try {
                verificationTokenRepository.deleteByUserId(user.getId());
                userRepository.delete(user);
                deleted++;
                logger.info("Deleted unverified user: id={}, email={}, createdAt={}",
                        user.getId(), user.getEmail(), user.getCreatedAt());
            } catch (Exception e) {
                logger.error("Failed to delete unverified user id={}, email={}: {}",
                        user.getId(), user.getEmail(), e.getMessage(), e);
            }
        }

        logger.info("User cleanup completed: removed {} unverified user(s) (registered before {})",
                deleted, cutoff);
    }
}
