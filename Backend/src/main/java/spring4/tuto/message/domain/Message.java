package spring4.tuto.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring4.tuto.common.domain.BaseEntity;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.user.domain.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Builder.Default
    @Column(name = "type", nullable = false)
    private String type = "TEXT";

    @Column(name = "body")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_message_id")
    private Message forwardedFromMessage;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;
}
