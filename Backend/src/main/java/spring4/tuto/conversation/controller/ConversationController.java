package spring4.tuto.conversation.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.conversation.dto.ConversationDto;
import spring4.tuto.conversation.service.ConversationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<ConversationDto> convs = conversationService.getConversationsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(convs));
    }

    @PostMapping("/private")
    public ResponseEntity<ApiResponse<ConversationDto>> createPrivateConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreatePrivateConversationRequest request) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        ConversationDto conv = conversationService.createOrGetPrivateConversation(currentUserId, request.getTargetUserId());
        return ResponseEntity.ok(ApiResponse.success("Private conversation ready", conv));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ConversationDto conv = conversationService.getConversationById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(conv));
    }

    @Data
    public static class CreatePrivateConversationRequest {
        private UUID targetUserId;
    }
}
