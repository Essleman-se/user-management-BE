package micronet.user.repository;

import micronet.user.model.LoginVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginVerificationCodeRepository extends JpaRepository<LoginVerificationCode, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM LoginVerificationCode lvc WHERE lvc.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    Optional<LoginVerificationCode> findTopByUserIdOrderByIdDesc(Long userId);
}
