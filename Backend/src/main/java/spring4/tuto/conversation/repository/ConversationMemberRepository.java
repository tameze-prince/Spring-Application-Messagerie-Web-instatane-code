package spring4.tuto.conversation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.conversation.domain.ConversationMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.ConversationMemberId> {

    List<ConversationMember> findById_ConversationId(UUID conversationId);

    Optional<ConversationMember> findById_ConversationIdAndId_UserId(UUID conversationId, UUID userId);

    boolean existsById_ConversationIdAndId_UserId(UUID conversationId, UUID userId);

    long countById_ConversationIdAndStatus(UUID conversationId, String status);
}
