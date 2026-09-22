package spring4.tuto.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.message.domain.MessageReaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    List<MessageReaction> findByMessageId(UUID messageId);
    Optional<MessageReaction> findByMessageIdAndUserIdAndReaction(UUID messageId, UUID userId, String reaction);
    void deleteByMessageIdAndUserIdAndReaction(UUID messageId, UUID userId, String reaction);
}
