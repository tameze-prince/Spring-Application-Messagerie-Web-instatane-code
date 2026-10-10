package spring4.tuto.message.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.message.dto.MessageDto;
import spring4.tuto.message.service.MessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<MessageDto> messages = messageService.getMessagesForConversation(id, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        MessageDto msg = messageService.sendMessage(id, userId, request.getType(), request.getBody(), request.getReplyToMessageId());
        return ResponseEntity.ok(ApiResponse.success("Message sent", msg));
    }

    @PatchMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<MessageDto>> editMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody EditMessageRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        MessageDto updated = messageService.editMessage(id, userId, request.getBody());
        return ResponseEntity.ok(ApiResponse.success("Message edited", updated));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        messageService.deleteMessage(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }

    @PostMapping("/messages/{id}/reaction")
    public ResponseEntity<ApiResponse<Void>> addReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        messageService.addReaction(id, userId, request.getReaction());
        return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
    }

    @DeleteMapping("/messages/{id}/reaction")
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam String reaction) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        messageService.removeReaction(id, userId, reaction);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed", null));
    }

    @Data
    public static class SendMessageRequest {
        @Size(max = 50, message = "Message type must not exceed 50 characters")
        private String type;

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        private String body;
        private UUID replyToMessageId;
    }

    @Data
    public static class EditMessageRequest {
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        private String body;
    }

    @Data
    public static class ReactionRequest {
        @NotBlank(message = "Reaction cannot be empty")
        @Size(max = 32, message = "Reaction must not exceed 32 characters")
        private String reaction;
    }
}
