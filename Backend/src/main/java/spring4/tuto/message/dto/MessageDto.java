package spring4.tuto.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring4.tuto.message.domain.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderUsername;
    private String type;
    private String body;
    private UUID replyToMessageId;
    private UUID forwardedFromMessageId;
    private Instant editedAt;
    private Instant createdAt;
    private Long sequenceNumber;
    private List<String> reactions;

    public static MessageDto fromEntity(Message msg) {
        if (msg == null) return null;
        return MessageDto.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation() != null ? msg.getConversation().getId() : null)
                .senderId(msg.getSender() != null ? msg.getSender().getId() : null)
                .senderUsername(msg.getSender() != null ? msg.getSender().getUsername() : null)
                .type(msg.getType())
                .body(msg.getBody())
                .replyToMessageId(msg.getReplyToMessage() != null ? msg.getReplyToMessage().getId() : null)
                .forwardedFromMessageId(msg.getForwardedFromMessage() != null ? msg.getForwardedFromMessage().getId() : null)
                .editedAt(msg.getEditedAt())
                .createdAt(msg.getCreatedAt())
                .sequenceNumber(msg.getSequenceNumber())
                .build();
    }
}
