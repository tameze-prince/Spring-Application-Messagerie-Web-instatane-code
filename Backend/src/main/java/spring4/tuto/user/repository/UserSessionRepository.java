package spring4.tuto.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.user.domain.UserSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
