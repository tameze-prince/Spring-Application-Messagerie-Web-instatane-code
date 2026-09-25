package spring4.tuto.channel.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import spring4.tuto.common.dto.ApiResponse;
import spring4.tuto.channel.dto.ChannelDto;
import spring4.tuto.channel.service.ChannelService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelDto>> createChannel(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChannelDto.CreateChannelRequest request) {
        UUID ownerId = UUID.fromString(userDetails.getUsername());
        ChannelDto channel = channelService.createChannel(
                ownerId, request.getName(), request.getUsername(), request.getDescription(),
                request.getAvatarFileId(), request.getVisibility(), request.getPostPermission());
        return ResponseEntity.ok(ApiResponse.success("Channel created successfully", channel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDto>> getChannel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ChannelDto channel = channelService.getChannel(id, userId);
        return ResponseEntity.ok(ApiResponse.success(channel));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<ChannelDto>> getChannelByUsername(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String username) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ChannelDto channel = channelService.getChannelByUsername(username, userId);
        return ResponseEntity.ok(ApiResponse.success(channel));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ChannelDto>>> getPublicChannels() {
        List<ChannelDto> channels = channelService.getPublicChannels();
        return ResponseEntity.ok(ApiResponse.success(channels));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDto>> updateChannel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ChannelDto.UpdateChannelRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ChannelDto channel = channelService.updateChannel(id, userId, request.getName(), request.getDescription(),
                request.getAvatarFileId(), request.getUsername(), request.getVisibility(),
                request.getPostPermission());
        return ResponseEntity.ok(ApiResponse.success("Channel updated successfully", channel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        channelService.deleteChannel(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Channel deleted successfully", null));
    }

    @GetMapping("/{id}/subscribers")
    public ResponseEntity<ApiResponse<List<ChannelDto.SubscriberInfo>>> getSubscribers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<ChannelDto.SubscriberInfo> subscribers = channelService.getSubscribers(id, userId);
        return ResponseEntity.ok(ApiResponse.success(subscribers));
    }

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        channelService.subscribe(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Subscribed successfully", null));
    }

    @DeleteMapping("/{id}/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        channelService.unsubscribe(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed successfully", null));
    }
}