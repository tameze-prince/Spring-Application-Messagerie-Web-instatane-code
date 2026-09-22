package spring4.tuto.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.conversation.domain.Conversation;
import spring4.tuto.conversation.domain.ConversationType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationDto {
    private UUID id;
    private ConversationType type;
    private String title;
    private String username;
    private String description;
    private UUID avatarFileId;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastMessageBody;
    private Instant lastMessageAt;
    private long unreadCount;

    public static ConversationDto fromEntity(Conversation conv) {
        if (conv == null) return null;
        return ConversationDto.builder()
                .id(conv.getId())
                .type(conv.getType())
                .title(conv.getTitle())
                .username(conv.getUsername())
                .description(conv.getDescription())
                .avatarFileId(conv.getAvatarFileId())
                .ownerId(conv.getOwner() != null ? conv.getOwner().getId() : null)
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }
}
