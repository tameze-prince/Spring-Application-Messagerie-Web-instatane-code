package spring4.tuto.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.message.domain.MessageRead;

import java.util.UUID;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, MessageRead.MessageReadId> {
    boolean existsById_MessageIdAndId_UserId(UUID messageId, UUID userId);
}
