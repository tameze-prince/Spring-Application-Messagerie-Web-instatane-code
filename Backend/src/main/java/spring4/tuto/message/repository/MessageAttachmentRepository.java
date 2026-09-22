package spring4.tuto.message.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring4.tuto.message.domain.MessageAttachment;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
    List<MessageAttachment> findByMessageId(UUID messageId);
}
