package spring4.tuto.user.domain;

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
@Table(name = "blocked_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUser {

    @EmbeddedId
    private BlockedUserId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockedUserId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "blocked_user_id")
        private UUID blockedUserId;
    }
}
