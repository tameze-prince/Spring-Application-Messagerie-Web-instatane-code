package spring4.tuto.message.domain;

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
@Table(name = "message_reads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRead {

    @EmbeddedId
    private MessageReadId id;

    @CreationTimestamp
    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageReadId implements Serializable {
        @Column(name = "message_id")
        private UUID messageId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
