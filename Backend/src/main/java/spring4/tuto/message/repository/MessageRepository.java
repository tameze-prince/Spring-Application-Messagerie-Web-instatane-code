package spring4.tuto.message.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring4.tuto.message.domain.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdAndDeletedAtIsNullOrderBySequenceNumberDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM Message m WHERE m.conversation.id = :conversationId")
    Long findMaxSequenceNumberByConversationId(@Param("conversationId") UUID conversationId);

    Optional<Message> findFirstByConversationIdAndDeletedAtIsNullOrderBySequenceNumberDesc(UUID conversationId);

    @Query(value = "SELECT * FROM messages WHERE conversation_id = :conversationId AND deleted_at IS NULL " +
            "AND to_tsvector('english', coalesce(body, '')) @@ plainto_tsquery('english', :query) " +
            "ORDER BY sequence_number DESC", nativeQuery = true)
    List<Message> searchMessagesInConversationFts(@Param("conversationId") UUID conversationId, @Param("query") String query);
}
