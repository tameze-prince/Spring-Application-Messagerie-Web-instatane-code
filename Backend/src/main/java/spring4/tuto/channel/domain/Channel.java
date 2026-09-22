package spring4.tuto.channel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import spring4.tuto.conversation.domain.Conversation;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Builder.Default
    @Column(name = "visibility")
    private String visibility = "PUBLIC";

    @Builder.Default
    @Column(name = "post_permission")
    private String postPermission = "ADMINS_ONLY";

    @Builder.Default
    @Column(name = "subscriber_count")
    private Long subscriberCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
