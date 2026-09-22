package spring4.tuto.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

    @EmbeddedId
    private ConversationMemberId id;

    @Builder.Default
    @Column(name = "role")
    private String role = "MEMBER";

    @Builder.Default
    @Column(name = "status")
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMemberId implements Serializable {
        @Column(name = "conversation_id")
        private UUID conversationId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
