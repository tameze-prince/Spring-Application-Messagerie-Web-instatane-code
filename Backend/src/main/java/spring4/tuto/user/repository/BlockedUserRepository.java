package spring4.tuto.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.user.domain.BlockedUser;

import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, BlockedUser.BlockedUserId> {

    boolean existsById_UserIdAndId_BlockedUserId(UUID userId, UUID blockedUserId);

    List<BlockedUser> findById_UserId(UUID userId);
}
