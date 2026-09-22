package spring4.tuto.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.notification.domain.Notification;
import spring4.tuto.notification.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }
}
