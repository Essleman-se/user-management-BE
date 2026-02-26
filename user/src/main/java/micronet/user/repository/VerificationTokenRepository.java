package micronet.user.repository;

import micronet.user.model.User;
import micronet.user.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.email = :email")
    void deleteByUserEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    Optional<VerificationToken> findByUser(User user);
}

