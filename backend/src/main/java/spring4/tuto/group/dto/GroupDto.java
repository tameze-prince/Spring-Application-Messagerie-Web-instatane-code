package spring4.tuto.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.group.domain.Group;
import spring4.tuto.user.domain.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {
    private UUID id;
    private String title;
    private String description;
    private String avatarFileId;
    private String type;
    private UUID ownerId;
    private Long maxMembers;
    private String joinPolicy;
    private Boolean approvalRequired;
    private Instant createdAt;
    private Instant updatedAt;

    private List<MemberInfo> members;

    public static GroupDto fromEntity(Conversation conv, Group group) {
        return GroupDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .description(conv.getDescription())
                .avatarFileId(conv.getAvatarFileId())
                .type(conv.getType() != null ? conv.getType().name() : "GROUP")
                .ownerId(conv.getOwner() != null ? conv.getOwner().getId() : null)
                .maxMembers(group.getMaxMembers())
                .joinPolicy(group.getJoinPolicy())
                .approvalRequired(group.getApprovalRequired())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
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
    public static class CreateGroupRequest {
        private String name;
        private String description;
        private String avatarFileId;
        private Long maxMembers;
        private String joinPolicy;
        private Boolean approvalRequired;
        private List<UUID> initialMemberIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGroupRequest {
        private String name;
        private String description;
        private String avatarFileId;
        private Long maxMembers;
        private String joinPolicy;
        private Boolean approvalRequired;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddMemberRequest {
        private UUID userId;
    }
}