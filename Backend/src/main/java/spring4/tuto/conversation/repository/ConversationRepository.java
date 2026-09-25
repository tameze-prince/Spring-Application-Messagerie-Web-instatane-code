package spring4.tuto.conversation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.domain.ConversationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUsername(String username);

    @Query("SELECT c FROM Conversation c JOIN ConversationMember cm ON c.id = cm.id.conversationId " +
            "WHERE cm.id.userId = :userId AND cm.status = 'ACTIVE' AND c.deletedAt IS NULL " +
            "ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c JOIN ConversationMember cm1 ON c.id = cm1.id.conversationId " +
            "JOIN ConversationMember cm2 ON c.id = cm2.id.conversationId " +
            "WHERE c.type = :type AND cm1.id.userId = :user1Id AND cm2.id.userId = :user2Id AND c.deletedAt IS NULL")
    Optional<Conversation> findPrivateConversationBetweenUsers(
            @Param("type") ConversationType type,
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id);

    List<Conversation> findByType(ConversationType type);
}
