package spring4.tuto.user.controller;

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
import spring4.tuto.user.dto.UserDto;
import spring4.tuto.user.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserDto user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        UserDto updated = userService.updateProfile(userId, request.getFirstName(), request.getLastName(), request.getBio(), request.getAvatarFileId());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDto>>> searchUsers(@RequestParam("q") String query) {
        List<UserDto> users = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.blockUser(userId, id);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        userService.unblockUser(userId, id);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String bio;
        private UUID avatarFileId;
    }
}
