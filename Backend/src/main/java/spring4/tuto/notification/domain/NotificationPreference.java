package spring4.tuto.notification.domain;

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
import org.hibernate.annotations.UpdateTimestamp;
import spring4.tuto.user.domain.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "message_notifications")
    private Boolean messageNotifications = true;

    @Builder.Default
    @Column(name = "group_notifications")
    private Boolean groupNotifications = true;

    @Builder.Default
    @Column(name = "channel_notifications")
    private Boolean channelNotifications = true;

    @Builder.Default
    @Column(name = "mention_notifications")
    private Boolean mentionNotifications = true;

    @Builder.Default
    @Column(name = "sound_enabled")
    private Boolean soundEnabled = true;

    @Builder.Default
    @Column(name = "email_notifications")
    private Boolean emailNotifications = false;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
