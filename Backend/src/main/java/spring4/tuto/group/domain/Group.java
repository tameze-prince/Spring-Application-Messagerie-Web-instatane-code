package spring4.tuto.group.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Builder.Default
    @Column(name = "max_members")
    private Long maxMembers = 200L;

    @Builder.Default
    @Column(name = "join_policy")
    private String joinPolicy = "EVERYONE";

    @Builder.Default
    @Column(name = "approval_required")
    private Boolean approvalRequired = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
