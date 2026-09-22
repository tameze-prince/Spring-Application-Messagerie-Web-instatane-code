package spring4.tuto.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring4.tuto.common.domain.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "bio")
    private String bio;

    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    @Builder.Default
    @Column(name = "status")
    private String status = "OFFLINE";

    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Version
    @Column(name = "version")
    private Long version;
}
