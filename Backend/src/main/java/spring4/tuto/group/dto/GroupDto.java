package spring4.tuto.group.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private String ownerId;
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
                .avatarFileId(conv.getAvatarFileId() != null ? conv.getAvatarFileId().toString() : null)
                .type(conv.getType() != null ? conv.getType().name() : "GROUP")
                .ownerId(conv.getOwner() != null ? conv.getOwner().getId().toString() : null)
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
        @jakarta.validation.constraints.NotNull(message = "User ID is required")
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
        @NotBlank(message = "Group name is required")
        @Size(max = 80, message = "Group name must not exceed 80 characters")
        @Pattern(regexp = "^(?=.*\\S)[^\\p{Cntrl}]+$", message = "Group name must contain a non-whitespace character and no control characters")
        private String name;

        @Size(max = 500, message = "Group description must not exceed 500 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Group description contains invalid characters")
        private String description;

        @Pattern(regexp = "^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$", message = "Avatar file ID must be a valid UUID")
        private String avatarFileId;

        @Min(value = 2, message = "A group must allow at least 2 members")
        @Max(value = 500, message = "A group cannot allow more than 500 members")
        private Long maxMembers;

        @Pattern(regexp = "^(EVERYONE|ADMINS_ONLY)$", message = "Join policy must be EVERYONE or ADMINS_ONLY")
        private String joinPolicy;
        private Boolean approvalRequired;
        @Size(max = 199, message = "A group can include at most 199 initial members")
        private List<@NotNull UUID> initialMemberIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGroupRequest {
        @Size(min = 1, max = 80, message = "Group name must be between 1 and 80 characters")
        @Pattern(regexp = "^(?=.*\\S)[^\\p{Cntrl}]+$", message = "Group name must contain a non-whitespace character and no control characters")
        private String name;

        @Size(max = 500, message = "Group description must not exceed 500 characters")
        @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "Group description contains invalid characters")
        private String description;

        @Pattern(regexp = "^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$", message = "Avatar file ID must be a valid UUID")
        private String avatarFileId;

        @Min(value = 2, message = "A group must allow at least 2 members")
        @Max(value = 500, message = "A group cannot allow more than 500 members")
        private Long maxMembers;

        @Pattern(regexp = "^(EVERYONE|ADMINS_ONLY)$", message = "Join policy must be EVERYONE or ADMINS_ONLY")
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