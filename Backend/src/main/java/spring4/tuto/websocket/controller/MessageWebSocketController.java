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
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.service.MessageService;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/message.send")
    public void sendMessage(@Payload WsMessageSendPayload payload, Principal principal) {
        if (principal == null) return;
        UUID senderId = UUID.fromString(principal.getName());

        MessageDto messageDto = messageService.sendMessage(
                payload.getConversationId(),
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

        // Broadcast to topic subscribers
        messagingTemplate.convertAndSend("/topic/conversations/" + payload.getConversationId(), response);
    }

    @MessageMapping("/typing.start")
    public void typingStart(@Payload WsTypingPayload payload, Principal principal) {
        if (principal == null) return;
        WsEventResponse<WsTypingPayload> response = WsEventResponse.<WsTypingPayload>builder()
                .event("typing.started")
                .data(payload)
                .build();
        messagingTemplate.convertAndSend("/topic/conversations/" + payload.getConversationId(), response);
    }

    @MessageMapping("/typing.stop")
    public void typingStop(@Payload WsTypingPayload payload, Principal principal) {
        if (principal == null) return;
        WsEventResponse<WsTypingPayload> response = WsEventResponse.<WsTypingPayload>builder()
                .event("typing.stopped")
                .data(payload)
                .build();
        messagingTemplate.convertAndSend("/topic/conversations/" + payload.getConversationId(), response);
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
