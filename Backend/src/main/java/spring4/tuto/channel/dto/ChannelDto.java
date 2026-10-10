package spring4.tuto.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.channel.domain.Channel;
import spring4.tuto.user.domain.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDto {
    private UUID id;
    private String title;
    private String username;
    private String description;
    private String avatarFileId;
    private String type;
    private String ownerId;
    private String visibility;
    private String postPermission;
    private Long subscriberCount;
    private Instant createdAt;
    private Instant updatedAt;

    private List<SubscriberInfo> subscribers;

    public static ChannelDto fromEntity(Conversation conv, Channel channel) {
        if (channel == null) return null;
        return ChannelDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .username(conv.getUsername())
                .description(conv.getDescription())
                .avatarFileId(conv.getAvatarFileId() != null ? conv.getAvatarFileId().toString() : null)
                .type(conv.getType() != null ? conv.getType().name() : "CHANNEL")
                .ownerId(conv.getOwner() != null ? conv.getOwner().getId().toString() : null)
                .visibility(channel.getVisibility())
                .postPermission(channel.getPostPermission())
                .subscriberCount(channel.getSubscriberCount())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriberInfo {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarFileId;
        private String role;
        private String status;
        private Instant joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChannelRequest {
        @Size(min = 1, max = 80, message = "Channel name must be between 1 and 80 characters")
        private String name;

        @Size(min = 3, max = 50, message = "Channel username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Channel username may contain only letters, numbers, dots, underscores and hyphens")
        private String username;

        @Size(max = 500, message = "Channel description must not exceed 500 characters")
        private String description;

        @Pattern(regexp = "^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$", message = "Avatar file ID must be a valid UUID")
        private String avatarFileId;

        @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "Visibility must be PUBLIC or PRIVATE")
        private String visibility;

        @Pattern(regexp = "^(EVERYONE|ADMINS_ONLY)$", message = "Post permission must be EVERYONE or ADMINS_ONLY")
        private String postPermission;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateChannelRequest {
        private String name;
        private String username;
        private String description;
        private String avatarFileId;
        private String visibility;
        private String postPermission;
    }
}