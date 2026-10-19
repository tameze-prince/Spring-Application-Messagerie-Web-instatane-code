package spring4.tuto.websocket.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import spring4.tuto.common.exception.BadRequestException;
import spring4.tuto.conversation.repository.ConversationMemberRepository;
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.service.MessageService;
import spring4.tuto.user.domain.User;
import spring4.tuto.user.repository.UserRepository;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    @MessageMapping("/message.send")
    public void sendMessage(@Payload WsMessageSendPayload payload, Principal principal) {
        UUID senderId = requirePrincipal(principal);
        UUID conversationId = requireConversationId(payload.getConversationId());

        MessageDto messageDto = messageService.sendMessage(
                conversationId,
                senderId,
                payload.getType(),
                payload.getBody(),
                payload.getReplyToMessageId()
        );

        WsEventResponse<MessageDto> response = WsEventResponse.<MessageDto>builder()
                .event("message.created")
                .requestId(payload.getRequestId())
                .data(messageDto)
                .build();

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
    }

    @MessageMapping("/typing.start")
    public void typingStart(@Payload WsTypingPayload payload, Principal principal) {
        UUID senderId = requirePrincipal(principal);
        UUID conversationId = requireConversationId(payload.getConversationId());
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        requireMembership(conversationId, senderId);

        WsTypingPayload eventPayload = new WsTypingPayload();
        eventPayload.setConversationId(conversationId);
        eventPayload.setUserId(senderId);
        eventPayload.setUsername(sender.getUsername());

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                WsEventResponse.<WsTypingPayload>builder()
                        .event("typing.started")
                        .data(eventPayload)
                        .build()
        );
    }

    @MessageMapping("/typing.stop")
    public void typingStop(@Payload WsTypingPayload payload, Principal principal) {
        UUID senderId = requirePrincipal(principal);
        UUID conversationId = requireConversationId(payload.getConversationId());
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        requireMembership(conversationId, senderId);

        WsTypingPayload eventPayload = new WsTypingPayload();
        eventPayload.setConversationId(conversationId);
        eventPayload.setUserId(senderId);
        eventPayload.setUsername(sender.getUsername());

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                WsEventResponse.<WsTypingPayload>builder()
                        .event("typing.stopped")
                        .data(eventPayload)
                        .build()
        );
    }

    private UUID requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("Authenticated user required");
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Authenticated user required");
        }
    }

    private UUID requireConversationId(UUID conversationId) {
        if (conversationId == null) {
            throw new BadRequestException("Conversation is required");
        }
        return conversationId;
    }

    private void requireMembership(UUID conversationId, UUID userId) {
        if (!memberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new BadRequestException("Access denied to conversation");
        }
    }

    @Data
    public static class WsMessageSendPayload {
        private String requestId;
        private UUID conversationId;
        private String clientMessageId;
        private String type;
        private String body;
        private UUID replyToMessageId;
    }

    @Data
    public static class WsTypingPayload {
        private UUID conversationId;
        private UUID userId;
        private String username;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WsEventResponse<T> {
        private String event;
        private String requestId;
        private T data;
    }
}
