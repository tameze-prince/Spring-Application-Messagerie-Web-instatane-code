package spring4.tuto.channel.dto;

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
    private UUID ownerId;
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
                .avatarFileId(conv.getAvatarFileId())
                .type(conv.getType() != null ? conv.getType().name() : "CHANNEL")
                .ownerId(conv.getOwner() != null ? conv.getOwner().getId() : null)
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
        private String name;
        private String username;
        private String description;
        private String avatarFileId;
        private String visibility;
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